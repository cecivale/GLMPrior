package glmprior.util;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.parameter.BooleanParameter;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.Distribution;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper class that creates and manages multiple GLMDistribution instances,
 * one for each dimension of the parameter being modeled.
 *
 * For each dimension i, creates a GLMDistribution where each predictor provides
 * its value at index i. All other parameters (intercept, coefficients, indicators,
 * family, link, etc.) are shared across all dimensions.
 *
 * Example: If predictors have dimension 10, this creates 10 GLMDistribution instances
 * where GLMDistribution[i] uses predictor[j].getArrayValue(i) for all predictors j.
 */
@Description("Creates multiple GLM distributions, one per parameter dimension, with shared parameters")
public class MultiGLMDistribution extends ParametricDistribution {

    // Core GLM inputs
    public Input<RealParameter> interceptInput = new Input<>(
            "intercept", "GLM intercept (α) shared across all dimensions", Validate.REQUIRED);

    public Input<RealParameter> coefficientsInput = new Input<>(
            "coefficients", "GLM coefficients β (dimension must match number of predictors)",
            Validate.REQUIRED);

    public Input<List<Function>> predictorsInput = new Input<>(
            "predictor",
            "Predictor functions (each with dimension matching the number of parameter dimensions)",
            new ArrayList<>(),
            Validate.REQUIRED);

    // Variable selection inputs
    public Input<BooleanParameter> indicatorsInput = new Input<>(
            "indicators", "Boolean indicators for variable selection (true/false for each coefficient)",
            Validate.OPTIONAL);

    // Distribution family and link function specification
    public Input<String> familyInput = new Input<>(
            "family", "Distribution family (NORMAL, POISSON, BINOMIAL, GAMMA)", "NORMAL", Validate.OPTIONAL);

    public Input<String> linkInput = new Input<>(
            "link", "Link function (IDENTITY, LOG, LOGIT, PROBIT, INVERSE, SQRT)", "IDENTITY",
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

    // Cached GLM distributions (one per parameter dimension)
    private List<GLMDistribution> glmDistributions;
    private int numDimensions;
    private int numPredictors;
    private DistributionFamily family;
    private LinkFunction link;

    @Override
    public void initAndValidate() {
        List<Function> predictors = predictorsInput.get();

        if (predictors.isEmpty()) {
            throw new IllegalArgumentException("At least one predictor must be provided");
        }

        numPredictors = predictors.size();
        numDimensions = predictors.get(0).getDimension();

        // Validate that all predictors have the same dimension
        for (int j = 0; j < numPredictors; j++) {
            if (predictors.get(j).getDimension() != numDimensions) {
                throw new IllegalArgumentException(
                        "All predictors must have the same dimension. " +
                        "Predictor 0 has dimension " + numDimensions + ", " +
                        "but predictor " + j + " has dimension " + predictors.get(j).getDimension());
            }
        }

        // Validate coefficients dimension
        if (coefficientsInput.get().getDimension() != numPredictors) {
            coefficientsInput.get().setDimension(numPredictors);
        }

        // Validate indicators dimension if provided
        if (indicatorsInput.get() != null) {
            if (indicatorsInput.get().getDimension() != numPredictors) {
                indicatorsInput.get().setDimension(numPredictors);
            }
        }

        // Parse and validate distribution family
        family = parseFamily(familyInput.get());

        // Parse and validate link function
        link = parseLink(linkInput.get(), family);

        // Validate family-link combination
        validateFamilyLinkCombination();

        // Validate distribution-specific parameters
        validateDistributionParameters();

        // Create GLMDistribution instances - one for each parameter dimension
        glmDistributions = new ArrayList<>(numDimensions);

        for (int i = 0; i < numDimensions; i++) {
            // Extract predictor values for this dimension
            Double[] predictorValues = new Double[numPredictors];
            for (int j = 0; j < numPredictors; j++) {
                predictorValues[j] = predictors.get(j).getArrayValue(i);
            }


            // Create GLMDistribution for this dimension
            GLMDistribution dist = new GLMDistribution(
                    interceptInput.get(),
                    coefficientsInput.get(),
                    predictorValues,
                    indicatorsInput.get(),
                    family,
                    link,
                    sigmaInput.get(),
                    sigma2Input.get(),
                    nTrialsInput.get(),
                    shapeInput.get()
            );

            // Initialize the distribution
            dist.initAndValidate();

            glmDistributions.add(dist);
        }
    }

    /**
     * Parses the family string and returns the DistributionFamily enum.
     */
    private DistributionFamily parseFamily(String familyStr) {
        if (familyStr == null || familyStr.isEmpty()) {
            return DistributionFamily.NORMAL;
        }
        try {
            return DistributionFamily.valueOf(familyStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid family name: " + familyStr +
                    ". Valid options: NORMAL, POISSON, BINOMIAL, GAMMA");
        }
    }

    /**
     * Parses the link string and returns the LinkFunction enum.
     * If not specified, returns the canonical link for the family.
     */
    private LinkFunction parseLink(String linkStr, DistributionFamily family) {
        if (linkStr == null || linkStr.isEmpty() || linkStr.equals("IDENTITY")) {
            return family.getCanonicalLink();
        }
        try {
            return LinkFunction.valueOf(linkStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid link function name: " + linkStr +
                    ". Valid options: IDENTITY, LOG, LOGIT, PROBIT, INVERSE, SQRT");
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
                validateNormalParameters();
                break;

            case POISSON:
                // No additional parameters needed
                if (sigmaInput.get() != null || sigma2Input.get() != null ||
                        nTrialsInput.get() != null || shapeInput.get() != null) {
                    throw new IllegalArgumentException("Poisson distribution does not use sigma, sigma2, nTrials, or shape parameters");
                }
                break;

            case BINOMIAL:
                validateBinomialParameters();
                break;

            case GAMMA:
                validateGammaParameters();
                break;

            default:
                throw new IllegalStateException("Parameter validation not implemented for " + family);
        }
    }

    /**
     * Validates parameters for Normal distribution.
     */
    private void validateNormalParameters() {
        boolean hasSigma = sigmaInput.get() != null;
        boolean hasSigma2 = sigma2Input.get() != null;

        if (!hasSigma && !hasSigma2) {
            throw new IllegalArgumentException("Normal distribution requires either 'sigma' or 'sigma2' parameter");
        }
        if (hasSigma && hasSigma2) {
            throw new IllegalArgumentException("Normal distribution: specify either 'sigma' or 'sigma2', not both");
        }
        if (nTrialsInput.get() != null || shapeInput.get() != null) {
            throw new IllegalArgumentException("Normal distribution does not use nTrials or shape parameters");
        }

        // Validate values
        if (hasSigma) {
            RealParameter sigma = sigmaInput.get();
            for (int i = 0; i < sigma.getDimension(); i++) {
                if (sigma.getValue(i) <= 0.0) {
                    throw new IllegalArgumentException("Normal distribution: sigma must be > 0 (found " +
                            sigma.getValue(i) + " at index " + i + ")");
                }
            }
        }
        if (hasSigma2) {
            RealParameter sigma2 = sigma2Input.get();
            for (int i = 0; i < sigma2.getDimension(); i++) {
                if (sigma2.getValue(i) <= 0.0) {
                    throw new IllegalArgumentException("Normal distribution: sigma2 must be > 0 (found " +
                            sigma2.getValue(i) + " at index " + i + ")");
                }
            }
        }
    }

    /**
     * Validates parameters for Binomial distribution.
     */
    private void validateBinomialParameters() {
        if (nTrialsInput.get() == null) {
            throw new IllegalArgumentException("Binomial distribution requires 'nTrials' parameter");
        }
        if (sigmaInput.get() != null || sigma2Input.get() != null || shapeInput.get() != null) {
            throw new IllegalArgumentException("Binomial distribution does not use sigma, sigma2, or shape parameters");
        }

        RealParameter nTrials = nTrialsInput.get();
        for (int i = 0; i < nTrials.getDimension(); i++) {
            if (nTrials.getValue(i) < 1) {
                throw new IllegalArgumentException("Binomial distribution: nTrials must be ≥ 1 (found " +
                        nTrials.getValue(i) + " at index " + i + ")");
            }
        }
    }

    /**
     * Validates parameters for Gamma distribution.
     */
    private void validateGammaParameters() {
        if (shapeInput.get() == null) {
            throw new IllegalArgumentException("Gamma distribution requires 'shape' parameter");
        }
        if (sigmaInput.get() != null || sigma2Input.get() != null || nTrialsInput.get() != null) {
            throw new IllegalArgumentException("Gamma distribution does not use sigma, sigma2, or nTrials parameters");
        }

        RealParameter shape = shapeInput.get();
        for (int i = 0; i < shape.getDimension(); i++) {
            if (shape.getValue(i) <= 0.0) {
                throw new IllegalArgumentException("Gamma distribution: shape must be > 0 (found " +
                        shape.getValue(i) + " at index " + i + ")");
            }
        }
    }

    /**
     * Returns a string listing the valid link functions for the current distribution family.
     */
    private String getValidLinksString() {
        StringBuilder sb = new StringBuilder();
        for (LinkFunction lf : LinkFunction.values()) {
            if (family.isValidLink(lf)) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(lf.getDisplayName());
            }
        }
        return sb.toString();
    }

    /**
     * Calculate log probability across all dimensions.
     * If the function has the same dimension as the number of GLM distributions,
     * applies each distribution to the corresponding dimension.
     *
     * @param fun Function containing the data values
     */
    @Override
    public double calcLogP(Function fun) {
        if (fun.getDimension() != numDimensions) {
            throw new IllegalArgumentException(
                    "Function dimension (" + fun.getDimension() + ") does not match " +
                    "number of GLM dimensions (" + numDimensions + ")");
        }

        double totalLogP = 0.0;
        for (int i = 0; i < numDimensions; i++) {
            double x = fun.getArrayValue(i);
            totalLogP += glmDistributions.get(i).logDensity(x);
        }

        return totalLogP;
    }

    /**
     * Returns the first GLMDistribution's underlying distribution.
     * Note: This is for compatibility with ParametricDistribution interface.
     */
    @Override
    public Distribution getDistribution() {
        if (glmDistributions.isEmpty()) {
            throw new IllegalStateException("No GLM distributions available");
        }
        return glmDistributions.get(0).getDistribution();
    }

    /**
     * Gets a specific GLMDistribution by index.
     */
    public GLMDistribution getGLMDistribution(int index) {
        if (index < 0 || index >= numDimensions) {
            throw new IndexOutOfBoundsException("Invalid distribution index: " + index);
        }
        return glmDistributions.get(index);
    }

    /**
     * Gets all GLMDistributions.
     */
    public List<GLMDistribution> getAllDistributions() {
        return new ArrayList<>(glmDistributions);
    }

    /**
     * Gets the number of parameter dimensions (= number of GLMDistributions).
     */
    public int getNumDimensions() {
        return numDimensions;
    }

    /**
     * Gets the number of predictors.
     */
    public int getNumPredictors() {
        return numPredictors;
    }

    /**
     * Gets the mean from a specific dimension's GLMDistribution.
     */
    public double getMean(int index) {
        return getGLMDistribution(index).getMean();
    }

    /**
     * Gets means from all dimensions.
     */
    public double[] getAllMeans() {
        double[] means = new double[numDimensions];
        for (int i = 0; i < numDimensions; i++) {
            means[i] = glmDistributions.get(i).getMean();
        }
        return means;
    }

    /**
     * Gets the variance from a specific dimension's GLMDistribution.
     */
    public double getVariance(int index) {
        return getGLMDistribution(index).getVariance();
    }

    /**
     * Gets variances from all dimensions.
     */
    public double[] getAllVariances() {
        double[] variances = new double[numDimensions];
        for (int i = 0; i < numDimensions; i++) {
            variances[i] = glmDistributions.get(i).getVariance();
        }
        return variances;
    }

    /**
     * Returns the mean of the first distribution (for compatibility with ParametricDistribution).
     */
    @Override
    public double getMean() {
        return glmDistributions.get(0).getMean();
    }

    /**
     * Samples from all distributions.
     * Returns a 2D array where each row contains one sample, and each column
     * corresponds to one dimension.
     */
    @Override
    public Double[][] sample(int size) throws MathException {
        Double[][] samples = new Double[size][numDimensions];

        for (int j = 0; j < numDimensions; j++) {
            Double[][] distSamples = glmDistributions.get(j).sample(size);
            for (int i = 0; i < size; i++) {
                samples[i][j] = distSamples[i][0];
            }
        }

        return samples;
    }

    /**
     * Returns density for the first distribution (for compatibility).
     */
    @Override
    public double density(double x) {
        return glmDistributions.get(0).density(x);
    }

    /**
     * Returns log density for the first distribution (for compatibility).
     */
    @Override
    public double logDensity(double x) {
        return glmDistributions.get(0).logDensity(x);
    }

    /**
     * Calculates cumulative probability using the first distribution.
     */
    @Override
    public double cumulativeProbability(double x) throws MathException {
        return glmDistributions.get(0).cumulativeProbability(x);
    }

    /**
     * Calculates cumulative probability between bounds using the first distribution.
     */
    @Override
    public double cumulativeProbability(double x0, double x1) throws MathException {
        return glmDistributions.get(0).cumulativeProbability(x0, x1);
    }

    /**
     * Calculates inverse cumulative probability using the first distribution.
     */
    @Override
    public double inverseCumulativeProbability(double p) throws MathException {
        return glmDistributions.get(0).inverseCumulativeProbability(p);
    }
}