package glmprior.util;

import beast.base.core.Description;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.parameter.BooleanParameter;

import org.apache.commons.math.distribution.Distribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.distribution.PoissonDistributionImpl;
import org.apache.commons.math.distribution.GammaDistributionImpl;
import org.apache.commons.math.distribution.BinomialDistributionImpl;

/**
 * A generalized GLM-driven parametric distribution that supports multiple distribution families
 * and link functions. The mean parameter is modeled as:
 *   η = intercept + sum_j beta[j] * X[j]  (linear predictor)
 *   μ = g^(-1)(η)  (mean via inverse link function)
 *   y | μ, θ ~ Family(μ, θ)  (response from specified family with additional parameters θ)
 *
 * Supports: Normal, Poisson, Binomial, and Gamma distributions
 * with various link functions (identity, log, logit, probit, inverse, sqrt).
 *
 * This class is designed for programmatic use (via constructor) rather than XML initialization.
 */
@Description("General GLM-driven parametric distribution supporting multiple families and link functions")
public class GLMDistribution extends ParametricDistribution {

    // Core GLM fields
    private RealParameter intercept;
    private RealParameter coefficients;
    private Double[] predictorValues;
    private BooleanParameter indicators;

    // Distribution family and link function
    private DistributionFamily family;
    private LinkFunction link;

    // Distribution-specific parameters
    private RealParameter sigma;
    private RealParameter sigma2;
    private RealParameter nTrials;
    private RealParameter shape;

    // Cached values
    private int p; // number of predictors

    /**
     * Programmatic constructor for creating GLMDistribution instances.
     * Validation of parameters should be done in MultiGLMDistribution before calling this constructor.
     *
     * @param intercept GLM intercept parameter
     * @param coefficients GLM coefficients
     * @param predictorValues Predictor values (one value per coefficient)
     * @param indicators Optional indicators for variable selection
     * @param family Distribution family
     * @param link Link function
     * @param sigma Standard deviation for Normal distribution (optional)
     * @param sigma2 Variance for Normal distribution (optional)
     * @param nTrials Number of trials for Binomial distribution (optional)
     * @param shape Shape parameter for Gamma distribution (optional)
     */
    public GLMDistribution(RealParameter intercept,
                          RealParameter coefficients,
                          Double[] predictorValues,
                          BooleanParameter indicators,
                          DistributionFamily family,
                          LinkFunction link,
                          RealParameter sigma,
                          RealParameter sigma2,
                          RealParameter nTrials,
                          RealParameter shape) {
        super();

        this.intercept = intercept;
        this.coefficients = coefficients;
        this.predictorValues = predictorValues;
        this.indicators = indicators;
        this.family = family;
        this.link = link;
        this.sigma = sigma;
        this.sigma2 = sigma2;
        this.nTrials = nTrials;
        this.shape = shape;

        this.p = predictorValues.length;
    }

    @Override
    public void initAndValidate() {
        // Minimal validation - just dimension checks
        if (coefficients.getDimension() != p) {
            coefficients.setDimension(p);
        }

        if (indicators != null && indicators.getDimension() != p) {
            indicators.setDimension(p);
        }
    }

    /**
     * Computes the linear predictor η = α + Σ(γⱼ * βⱼ * xⱼ)
     * where γⱼ are the binary indicators (if provided) for variable selection.
     */
    private double computeLinearPredictor() {
        double eta = intercept.getValue();
        final Double[] beta = coefficients.getValues();

        if (indicators !=null)
            return computeLinearPredictor(eta, beta, indicators.getValues());

        return computeLinearPredictor(eta, beta, null);

//        for (int j = 0; j < p; j++) {
//            double coefficient = beta[j];
//
//            // Apply indicator variable if provided (for variable selection)
//            if (indicators != null) {
//                boolean indicator = indicators.getValue(j);
//                if (!indicator) {
//                    coefficient = 0.0; // Exclude this variable if indicator is false
//                }
//            }
//
//            eta += coefficient * predictorValues[j];
//        }
//
//        return eta;
    }

    private double computeLinearPredictor(double eta, Double[] beta, Boolean[] indicators) {
        for (int j = 0; j < p; j++) {
            double coefficient = beta[j];

            // Apply indicator variable if provided (for variable selection)
            if (indicators != null) {
                boolean indicator = indicators[j];
                if (!indicator) {
                    coefficient = 0.0; // Exclude this variable if indicator is false
                }
            }

            eta += coefficient * predictorValues[j];
        }

        return eta;
    }

    /**
     * Computes the mean parameter μ = g^(-1)(η) using the inverse link function
     */
    private double computeMean() {
        double eta = computeLinearPredictor();
        double mu = LinkFunctions.inverse(link, eta);

        // Validate that μ is in the valid domain for this distribution family
        family.validateMean(mu);

        return mu;
    }

    private double computeMean(Double intercept, Double[] coefficients, Boolean[] indicators) {
        double eta = computeLinearPredictor(intercept, coefficients, indicators);
        double mu = LinkFunctions.inverse(link, eta);

        // Validate that μ is in the valid domain for this distribution family
        family.validateMean(mu);

        return mu;
    }

    /**
     * Creates the appropriate Apache Commons Math distribution object.
     * Returns the proper Distribution type (continuous or discrete).
     */
    @Override
    public Distribution getDistribution() {
        double mu = computeMean();

        switch (family) {
            case NORMAL:
                double sigmaValue = getSigmaValue();
                return new NormalDistributionImpl(mu, sigmaValue);

            case POISSON:
                return new PoissonDistributionImpl(mu);

            case BINOMIAL:
                int n = (int) Math.round(nTrials.getValue());
                // For binomial, mu is the probability p
                return new BinomialDistributionImpl(n, mu);

            case GAMMA:
                double shapeValue = shape.getValue();
                double rate = shapeValue / mu; // rate = shape / mean
                return new GammaDistributionImpl(shapeValue, 1.0 / rate); // Commons Math uses scale = 1/rate

            default:
                throw new IllegalStateException("Distribution creation not implemented for " + family);
        }
    }

    /**
     * Gets the standard deviation for Normal distribution from either sigma or sigma2 field.
     */
    private double getSigmaValue() {
        if (sigma != null) {
            return sigma.getValue();
        } else if (sigma2 != null) {
            return Math.sqrt(sigma2.getValue());
        } else {
            throw new IllegalStateException("No sigma or sigma2 parameter available for Normal distribution");
        }
    }

    /**
     * Returns a string listing the valid link functions for the current distribution family.
     */
    private String getValidLinksString() {
        StringBuilder sb = new StringBuilder();
        for (LinkFunction lf : LinkFunction.values()) {
            if (family.isValidLink(lf)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(lf.getDisplayName());
            }
        }
        return sb.toString();
    }

    // Convenience accessors
    public double getMean() {
        return computeMean();
    }

    public double getStoredMean() {
        return computeMean(intercept.getStoredValues()[0], coefficients.getStoredValues(), indicators.getStoredValues());
    }

    public double getVariance() {
        switch (family) {
            case NORMAL:
                double sigmaValue = getSigmaValue();
                return sigmaValue * sigmaValue;
            case POISSON:
                return computeMean(); // For Poisson, variance = mean
            case BINOMIAL:
                int n = (int) Math.round(nTrials.getValue());
                double p = computeMean();
                return n * p * (1 - p);
            case GAMMA:
                double mu = computeMean();
                double shapeValue = shape.getValue();
                return mu * mu / shapeValue; // For Gamma: var = μ²/shape
            default:
                throw new UnsupportedOperationException("Variance calculation not implemented for " + family);
        }
    }

    public DistributionFamily getFamily() {
        return family;
    }

    public LinkFunction getLink() {
        return link;
    }
}