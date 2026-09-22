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
 *   η = g(baselineValue) + sum_j beta[j] * X[j]  (linear predictor)
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
    private RealParameter baselineValue;
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
     * @param baselineValue Baseline value on response scale (value when all predictors are 0)
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
    public GLMDistribution(RealParameter baselineValue,
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

        this.baselineValue = baselineValue;
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
     * Computes the linear predictor η = g(baseline) + Σ(γⱼ * βⱼ * xⱼ)
     * where γⱼ are the binary indicators (if provided) for variable selection,
     * and g() is the link function applied to the baseline value.
     *
     * The baselineValue parameter is treated as the baseline value on the response scale,
     * so the link function is applied to convert it to the linear predictor scale.
     */
    private double computeLinearPredictor() {
        // Apply link function to baseline value to get intercept on linear predictor scale
        double eta = LinkFunctions.apply(link, baselineValue.getValue());
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

    private double computeMean(Double baselineValue, Double[] coefficients, Boolean[] indicators) {
        // Apply link function to baseline value to get intercept on linear predictor scale
        double interceptOnEtaScale = LinkFunctions.apply(link, baselineValue);
        double eta = computeLinearPredictor(interceptOnEtaScale, coefficients, indicators);
        double mu = LinkFunctions.inverse(link, eta);

        // Validate that μ is in the valid domain for this distribution family
        family.validateMean(mu);

        return mu;
    }

    /**
     * Creates the appropriate Apache Commons Math distribution object.
     * Returns the proper Distribution type (continuous or discrete).
     *
     * Note: LogNormal is not supported via this method - use logDensity() directly.
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

            case LOGNORMAL:
                // LogNormal doesn't have a direct Commons Math impl in the old package.
                // Return a Normal distribution on the log scale for compatibility,
                // but logDensity() should be used for proper calculations.
                double logMu = computeLinearPredictor(); // η = log-scale mean
                return new NormalDistributionImpl(logMu, getSigmaValue());

            case LOGITNORMAL:
                // LogitNormal doesn't have a Commons Math impl.
                // Return a Normal distribution on the logit scale for compatibility,
                // but logDensity() should be used for proper calculations.
                double logitMu = computeLinearPredictor(); // η = logit-scale mean
                return new NormalDistributionImpl(logitMu, getSigmaValue());

            default:
                throw new IllegalStateException("Distribution creation not implemented for " + family);
        }
    }

    /**
     * Computes the log density for the given value.
     * Overridden to provide proper LogNormal and LogitNormal support.
     */
    @Override
    public double logDensity(double x) {
        if (family == DistributionFamily.LOGNORMAL) {
            return logNormalLogDensity(x);
        }
        if (family == DistributionFamily.LOGITNORMAL) {
            return logitNormalLogDensity(x);
        }
        // For other families, use the parent implementation
        return super.logDensity(x);
    }

    /**
     * Computes log density for LogNormal distribution.
     * LogNormal(μ, σ) where log(Y) ~ Normal(μ, σ).
     *
     * For our GLM with log link:
     *   η = log(baseline) + β·X  (linear predictor = log-scale mean)
     *   log(Y) ~ Normal(η, σ)
     *
     * log f(y) = -log(y) - log(σ) - 0.5*log(2π) - 0.5*((log(y) - η)/σ)²
     */
    private double logNormalLogDensity(double y) {
        if (y <= 0) {
            return Double.NEGATIVE_INFINITY;
        }

        double eta = computeLinearPredictor(); // log-scale mean
        double sigmaValue = getSigmaValue();
        double logY = Math.log(y);
        double z = (logY - eta) / sigmaValue;

        // log f(y) = -log(y) - log(σ) - 0.5*log(2π) - 0.5*z²
        return -logY - Math.log(sigmaValue) - 0.5 * Math.log(2 * Math.PI) - 0.5 * z * z;
    }

    /**
     * Computes log density for LogitNormal distribution.
     * LogitNormal(μ, σ) where logit(Y) ~ Normal(μ, σ).
     *
     * For our GLM with logit link:
     *   η = logit(baseline) + β·X  (linear predictor = logit-scale mean)
     *   logit(Y) ~ Normal(η, σ)
     *
     * The PDF of LogitNormal is:
     *   f(y) = (1 / (σ * sqrt(2π))) * (1 / (y * (1-y))) * exp(-0.5 * ((logit(y) - η) / σ)²)
     *
     * log f(y) = -log(σ) - 0.5*log(2π) - log(y) - log(1-y) - 0.5*((logit(y) - η)/σ)²
     */
    private double logitNormalLogDensity(double y) {
        if (y <= 0 || y >= 1) {
            return Double.NEGATIVE_INFINITY;
        }

        double eta = computeLinearPredictor(); // logit-scale mean
        double sigmaValue = getSigmaValue();
        double logitY = Math.log(y / (1.0 - y)); // logit(y)
        double z = (logitY - eta) / sigmaValue;

        // log f(y) = -log(σ) - 0.5*log(2π) - log(y) - log(1-y) - 0.5*z²
        // The Jacobian term is 1/(y*(1-y)) which gives -log(y) - log(1-y) in log space
        return -Math.log(sigmaValue) - 0.5 * Math.log(2 * Math.PI)
               - Math.log(y) - Math.log(1.0 - y) - 0.5 * z * z;
    }

    /**
     * Computes the density for the given value.
     * Overridden to provide proper LogNormal and LogitNormal support.
     */
    @Override
    public double density(double x) {
        if (family == DistributionFamily.LOGNORMAL) {
            return Math.exp(logNormalLogDensity(x));
        }
        if (family == DistributionFamily.LOGITNORMAL) {
            return Math.exp(logitNormalLogDensity(x));
        }
        return super.density(x);
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
        return computeMean(baselineValue.getStoredValues()[0], coefficients.getStoredValues(), indicators.getStoredValues());
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
            case LOGNORMAL:
                // For LogNormal where log(Y) ~ Normal(η, σ):
                // Var(Y) = (exp(σ²) - 1) * exp(2η + σ²)
                double etaLN = computeLinearPredictor();
                double sigmaLN = getSigmaValue();
                double sigma2LN = sigmaLN * sigmaLN;
                return (Math.exp(sigma2LN) - 1) * Math.exp(2 * etaLN + sigma2LN);
            case LOGITNORMAL:
                // For LogitNormal where logit(Y) ~ Normal(η, σ):
                // No closed-form variance. Using delta method approximation:
                // Var(Y) ≈ (dμ/dη)² * σ² where μ = logit⁻¹(η)
                // dμ/dη = exp(η)/(1+exp(η))² = μ(1-μ)
                double etaLGN = computeLinearPredictor();
                double sigmaLGN = getSigmaValue();
                double muLGN = 1.0 / (1.0 + Math.exp(-etaLGN)); // logit⁻¹(η)
                double derivLGN = muLGN * (1.0 - muLGN); // Jacobian
                return derivLGN * derivLGN * sigmaLGN * sigmaLGN;
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