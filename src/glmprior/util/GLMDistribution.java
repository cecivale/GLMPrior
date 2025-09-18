package glmprior.util;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.parameter.BooleanParameter;

import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.Distribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.distribution.PoissonDistributionImpl;
import org.apache.commons.math.distribution.GammaDistributionImpl;
import org.apache.commons.math.distribution.BinomialDistributionImpl;
import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * A generalized GLM-driven parametric distribution that supports multiple distribution families
 * and link functions. The mean parameter is modeled as:
 *   η = intercept + sum_j beta[j] * X[j]  (linear predictor)
 *   μ = g^(-1)(η)  (mean via inverse link function)
 *   y | μ, θ ~ Family(μ, θ)  (response from specified family with additional parameters θ)
 *
 * Supports: Normal, Poisson, Binomial, and Gamma distributions
 * with various link functions (identity, log, logit, probit, inverse, sqrt).
 */
@Description("General GLM-driven parametric distribution supporting multiple families and link functions")
public class GLMDistribution extends ParametricDistribution {

    // Core GLM inputs (from original GLMNormalDistribution)
    public Input<RealParameter> interceptInput = new Input<>(
            "intercept", "GLM intercept (α)", Validate.REQUIRED);

    public Input<RealParameter> coefficientsInput = new Input<>(
            "coefficients", "GLM coefficients β (dimension must match predictors)", 
            Validate.REQUIRED);

    public Input<RealParameter> predictorsInput = new Input<>(
            "predictors", "Predictor vector X (fixed values, dimension p)", 
            Validate.REQUIRED);

    // Variable selection inputs
    public Input<BooleanParameter> indicatorsInput = new Input<>(
            "indicators", "Boolean indicators for variable selection (true/false for each coefficient)", 
            Validate.OPTIONAL);

    // Distribution family and link function specification
    public Input<DistributionFamily> familyInput = new Input<>(
            "family", "Distribution family", DistributionFamily.NORMAL, Validate.OPTIONAL);

    public Input<LinkFunction> linkInput = new Input<>(
            "link", "Link function (if not specified, uses canonical link for family)", 
            Validate.OPTIONAL);

    // Distribution-specific parameters
    public Input<RealParameter> sigmaInput = new Input<>(
            "sigma", "Standard deviation σ for Normal distribution (>0)", Validate.OPTIONAL);
    
    public Input<RealParameter> sigma2Input = new Input<>(
            "sigma2", "Variance σ² for Normal distribution (>0)", Validate.OPTIONAL);

    public Input<RealParameter> nTrialsInput = new Input<>(
            "nTrials", "Number of trials n for Binomial distribution (≥1)", Validate.OPTIONAL);

    public Input<RealParameter> shapeInput = new Input<>(
            "shape", "Shape parameter for Gamma distribution (>0)", 
            Validate.OPTIONAL);

    // Optional standardization (from original)
    public Input<Boolean> standardizeInput = new Input<>(
            "standardize", "Z-score standardize predictors at initialization", 
            false, Validate.OPTIONAL);

    // Cached values
    private DistributionFamily family;
    private LinkFunction link;
    private int p; // number of predictors

    @Override
    public void initAndValidate() {
        // Validate basic GLM components
        final RealParameter beta = coefficientsInput.get();
        final RealParameter X = predictorsInput.get();

        if (beta == null || X == null) {
            throw new IllegalArgumentException("Both coefficients and predictors must be provided");
        }

        if (beta.getDimension() != X.getDimension()) {
            throw new IllegalArgumentException("Dimension mismatch: coefficients ("
                    + beta.getDimension() + ") vs predictors (" + X.getDimension() + ")");
        }
        p = beta.getDimension();

        // Validate indicators if provided
        final BooleanParameter indicators = indicatorsInput.get();
        if (indicators != null) {
            if (indicators.getDimension() != p) {
                throw new IllegalArgumentException("Dimension mismatch: indicators ("
                        + indicators.getDimension() + ") vs coefficients (" + p + ")");
            }
            // No additional validation needed for BooleanParameter - values are guaranteed to be true/false
        }

        // Set distribution family and link function
        family = familyInput.get();
        if (family == null) {
            family = DistributionFamily.NORMAL;
        }

        link = linkInput.get();
        if (link == null) {
            link = family.getCanonicalLink();
        }

        // Validate family-link combination
        validateFamilyLinkCombination();

        // Validate distribution-specific parameters
        validateDistributionParameters();

        // Optional standardization (applied once at init)
        if (Boolean.TRUE.equals(standardizeInput.get())) {
            standardizePredictors();
        }
    }

    /**
     * Validates that the specified link function is appropriate for the distribution family.
     */
    private void validateFamilyLinkCombination() {
        if (!family.isValidLink(link)) {
            throw new IllegalArgumentException("Link function " + link.getDisplayName() + 
                    " is not valid for " + family.getDisplayName() + " distribution. " +
                    "Valid links: " + getValidLinksString());
        }
    }

    /**
     * Validates that required parameters are provided for the specified distribution family.
     */
    private void validateDistributionParameters() {
        switch (family) {
            case NORMAL:
                // Need either sigma or sigma2
                boolean hasSigma = sigmaInput.get() != null;
                boolean hasSigma2 = sigma2Input.get() != null;
                if (!hasSigma && !hasSigma2) {
                    throw new IllegalArgumentException("Normal distribution requires either 'sigma' or 'sigma2' parameter");
                }
                if (hasSigma && hasSigma2) {
                    throw new IllegalArgumentException("Normal distribution: specify either 'sigma' or 'sigma2', not both");
                }
                if (hasSigma && sigmaInput.get().getValue() <= 0.0) {
                    throw new IllegalArgumentException("Normal distribution: sigma must be > 0");
                }
                if (hasSigma2 && sigma2Input.get().getValue() <= 0.0) {
                    throw new IllegalArgumentException("Normal distribution: sigma2 must be > 0");
                }
                break;

            case POISSON:
                // No additional parameters needed
                break;

            case BINOMIAL:
                if (nTrialsInput.get() == null) {
                    throw new IllegalArgumentException("Binomial distribution requires 'nTrials' parameter");
                }
                if (nTrialsInput.get().getValue() < 1) {
                    throw new IllegalArgumentException("Binomial distribution: nTrials must be ≥ 1");
                }
                break;

            case GAMMA:
                if (shapeInput.get() == null) {
                    throw new IllegalArgumentException("Gamma distribution requires 'shape' parameter");
                }
                if (shapeInput.get().getValue() <= 0.0) {
                    throw new IllegalArgumentException("Gamma distribution: shape must be > 0");
                }
                break;


            default:
                throw new IllegalStateException("Parameter validation not implemented for " + family);
        }
    }

    /**
     * Standardizes predictor variables (z-score normalization).
     */
    private void standardizePredictors() {
        RealParameter X = predictorsInput.get();
        Double[] vals = X.getValues();
        
        // Calculate mean
        double mean = 0.0;
        for (double v : vals) mean += v;
        mean /= vals.length;
        
        // Calculate standard deviation
        double var = 0.0;
        for (double v : vals) {
            double d = v - mean;
            var += d * d;
        }
        var /= Math.max(1, vals.length - 1);
        double sd = Math.sqrt(var);
        
        // Apply standardization
        if (sd > 0) {
            for (int i = 0; i < vals.length; i++) {
                X.setValue(i, (vals[i] - mean) / sd);
            }
        }
    }

    /**
     * Computes the linear predictor η = α + Σ(γⱼ * βⱼ * xⱼ)
     * where γⱼ are the binary indicators (if provided) for variable selection.
     */
    private double computeLinearPredictor() {
        double eta = interceptInput.get().getValue();
        final Double[] beta = coefficientsInput.get().getValues();
        final Double[] x = predictorsInput.get().getValues();
        final BooleanParameter indicators = indicatorsInput.get();
        
        for (int j = 0; j < p; j++) {
            double coefficient = beta[j];
            
            // Apply indicator variable if provided (for variable selection)
            if (indicators != null) {
                boolean indicator = indicators.getValue(j);
                if (!indicator) {
                    coefficient = 0.0; // Exclude this variable if indicator is false
                }
            }
            
            eta += coefficient * x[j];
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

    /**
     * Creates the appropriate Apache Commons Math distribution object.
     * Returns the proper Distribution type (continuous or discrete).
     */
    @Override
    public Distribution getDistribution() {
        double mu = computeMean();
        
        switch (family) {
            case NORMAL:
                double sigma = getSigmaValue();
                return new NormalDistributionImpl(mu, sigma);

            case POISSON:
                // Now we can use the actual Poisson distribution!
                return new PoissonDistributionImpl(mu);

            case BINOMIAL:
                int n = (int) Math.round(nTrialsInput.get().getValue());
                // For binomial, mu is the probability p
                return new BinomialDistributionImpl(n, mu);

            case GAMMA:
                double shape = shapeInput.get().getValue();
                double rate = shape / mu; // rate = shape / mean
                return new GammaDistributionImpl(shape, 1.0 / rate); // Commons Math uses scale = 1/rate

            default:
                throw new IllegalStateException("Distribution creation not implemented for " + family);
        }
    }


    /**
     * Gets the standard deviation for Normal distribution from either sigma or sigma2 input.
     */
    private double getSigmaValue() {
        if (sigmaInput.get() != null) {
            return sigmaInput.get().getValue();
        } else if (sigma2Input.get() != null) {
            return Math.sqrt(sigma2Input.get().getValue());
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

    public double getVariance() {
        switch (family) {
            case NORMAL:
                double sigma = getSigmaValue();
                return sigma * sigma;
            case POISSON:
                return computeMean(); // For Poisson, variance = mean
            case BINOMIAL:
                int n = (int) Math.round(nTrialsInput.get().getValue());
                double p = computeMean();
                return n * p * (1 - p);
            case GAMMA:
                double mu = computeMean();
                double shape = shapeInput.get().getValue();
                return mu * mu / shape; // For Gamma: var = μ²/shape
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