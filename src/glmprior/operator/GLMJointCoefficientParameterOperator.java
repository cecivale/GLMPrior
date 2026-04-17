package glmprior.operator;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Operator;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;
import glmprior.util.MultiGLMDistribution;

import java.text.DecimalFormat;

/**
 * A joint operator that updates both GLM coefficients and the dependent parameter
 * using deterministic coupling to maintain the GLM relationship.
 *
 * This operator solves the problem of poor mixing when the parameter y is tightly
 * constrained by the GLM prior (e.g., Normal with small sigma). By updating both
 * the coefficient and y together, it maintains y ≈ μ where μ = g^(-1)(α + Σ(β*X)).
 *
 * The coupling is deterministic and reversible:
 *   - Propose Δβ for coefficient β[i]
 *   - Compute induced change in means: Δμ[j] = μ_new[j] - μ_old[j]
 *   - Update parameter: y[j] := y[j] + Δμ[j]
 *
 * This maintains proper MCMC reversibility (Hastings ratio = 1.0) and dramatically
 * improves acceptance rates when sigma is small.
 */
@Description("Joint operator that updates GLM coefficients and parameter together using deterministic coupling")
public class GLMJointCoefficientParameterOperator extends Operator {

    // Inputs
    final public Input<RealParameter> parameterInput = new Input<>(
            "parameter",
            "The parameter to update jointly (typically the response variable y)",
            Validate.REQUIRED);

    final public Input<RealParameter> coefficientsInput = new Input<>(
            "coefficients",
            "The GLM coefficients to update",
            Validate.REQUIRED);

    final public Input<MultiGLMDistribution> glmDistributionInput = new Input<>(
            "glmDistribution",
            "The MultiGLMDistribution that links coefficients to parameter means",
            Validate.REQUIRED);

    final public Input<IntegerParameter> coefficientIndexInput = new Input<>(
            "coefficientIndex",
            "Index of the coefficient to update (if not provided, selects randomly)",
            Validate.OPTIONAL);

    final public Input<Double> windowSizeInput = new Input<>(
            "windowSize",
            "Window size for coefficient proposal (Gaussian std dev)",
            0.1);

    final public Input<Boolean> useGaussianInput = new Input<>(
            "useGaussian",
            "Use Gaussian proposal (if false, uses uniform). Default true.",
            true);

    final public Input<Boolean> optimiseInput = new Input<>(
            "optimise",
            "Automatically tune window size for target acceptance rate (default true)",
            true);

    final public Input<Boolean> updateAllCoefficientsInput = new Input<>(
            "updateAllCoefficients",
            "If true, updates all coefficients simultaneously (default false = update one)",
            false);

    // Cached values
    private double windowSize;
    private boolean useGaussian;
    private boolean updateAllCoefficients;
    private Integer fixedCoefficientIndex = null;

    @Override
    public void initAndValidate() {
        windowSize = windowSizeInput.get();
        useGaussian = useGaussianInput.get();
        updateAllCoefficients = updateAllCoefficientsInput.get();

        // If coefficient index is provided, use it; otherwise select randomly in proposal()
        if (coefficientIndexInput.get() != null) {
            fixedCoefficientIndex = coefficientIndexInput.get().getValue();

            // Validate index
            RealParameter coeffs = coefficientsInput.get();
            if (fixedCoefficientIndex < 0 || fixedCoefficientIndex >= coeffs.getDimension()) {
                throw new IllegalArgumentException(
                    "coefficientIndex must be between 0 and " + (coeffs.getDimension() - 1) +
                    " (found " + fixedCoefficientIndex + ")");
            }
        }

        // Validate that parameter and GLM distribution dimensions match
        RealParameter param = parameterInput.get();
        MultiGLMDistribution glmDist = glmDistributionInput.get();

        if (param.getDimension() != glmDist.getNumDimensions()) {
            throw new IllegalArgumentException(
                "Parameter dimension (" + param.getDimension() +
                ") must match GLM distribution dimensions (" + glmDist.getNumDimensions() + ")");
        }
    }

    @Override
    public double proposal() {
        RealParameter parameter = parameterInput.get();
        RealParameter coefficients = coefficientsInput.get();
        MultiGLMDistribution glmDistribution = glmDistributionInput.get();

        // Store old means before changing coefficients
        double[] oldMeans = glmDistribution.getAllMeans();

        if (updateAllCoefficients) {
            // Update all coefficients simultaneously
            if (!updateAllCoefficientsProposal(coefficients, parameter, oldMeans, glmDistribution)) {
                return Double.NEGATIVE_INFINITY;
            }
        } else {
            // Update single coefficient
            if (!updateSingleCoefficientProposal(coefficients, parameter, oldMeans, glmDistribution)) {
                return Double.NEGATIVE_INFINITY;
            }
        }

        // Deterministic coupling has Hastings ratio = 1.0
        return 0.0;
    }

    /**
     * Updates a single coefficient and adjusts parameter accordingly.
     */
    private boolean updateSingleCoefficientProposal(
            RealParameter coefficients,
            RealParameter parameter,
            double[] oldMeans,
            MultiGLMDistribution glmDistribution) {

        // Select which coefficient to update
        int coeffIndex = (fixedCoefficientIndex != null)
            ? fixedCoefficientIndex
            : Randomizer.nextInt(coefficients.getDimension());

        // Store old coefficient value
        double oldCoeff = coefficients.getValue(coeffIndex);

        // Propose new coefficient value
        double delta = useGaussian
            ? Randomizer.nextGaussian() * windowSize
            : (Randomizer.nextDouble() * 2.0 - 1.0) * windowSize;

        double newCoeff = oldCoeff + delta;

        // Check bounds
        if (newCoeff < coefficients.getLower() || newCoeff > coefficients.getUpper()) {
            return false; // Reject
        }

        // Check for no-op
        if (newCoeff == oldCoeff) {
            return false; // Reject (no change)
        }

        // Update coefficient
        coefficients.setValue(coeffIndex, newCoeff);

        // Get new means after coefficient change
        double[] newMeans = glmDistribution.getAllMeans();

        // Adjust parameter values deterministically to track mean changes
        if (!updateParameterDeterministically(parameter, oldMeans, newMeans)) {
            // Revert coefficient if parameter update failed
            coefficients.setValue(coeffIndex, oldCoeff);
            return false;
        }

        return true;
    }

    /**
     * Updates all coefficients and adjusts parameter accordingly.
     */
    private boolean updateAllCoefficientsProposal(
            RealParameter coefficients,
            RealParameter parameter,
            double[] oldMeans,
            MultiGLMDistribution glmDistribution) {

        // Store old coefficient values
        double[] oldCoeffs = new double[coefficients.getDimension()];
        for (int i = 0; i < coefficients.getDimension(); i++) {
            oldCoeffs[i] = coefficients.getValue(i);
        }

        // Propose new coefficient values
        for (int i = 0; i < coefficients.getDimension(); i++) {
            double delta = useGaussian
                ? Randomizer.nextGaussian() * windowSize
                : (Randomizer.nextDouble() * 2.0 - 1.0) * windowSize;

            double newCoeff = oldCoeffs[i] + delta;

            // Check bounds
            if (newCoeff < coefficients.getLower() || newCoeff > coefficients.getUpper()) {
                // Revert all changes
                for (int j = 0; j < i; j++) {
                    coefficients.setValue(j, oldCoeffs[j]);
                }
                return false;
            }

            coefficients.setValue(i, newCoeff);
        }

        // Get new means after coefficient changes
        double[] newMeans = glmDistribution.getAllMeans();

        // Adjust parameter values deterministically
        if (!updateParameterDeterministically(parameter, oldMeans, newMeans)) {
            // Revert all coefficients
            for (int i = 0; i < coefficients.getDimension(); i++) {
                coefficients.setValue(i, oldCoeffs[i]);
            }
            return false;
        }

        return true;
    }

    /**
     * Updates parameter deterministically to track mean changes.
     * Returns false if any parameter value goes out of bounds.
     */
    private boolean updateParameterDeterministically(
            RealParameter parameter,
            double[] oldMeans,
            double[] newMeans) {

        int dimension = parameter.getDimension();

        // Store old parameter values in case we need to revert
        double[] oldParams = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            oldParams[i] = parameter.getValue(i);
        }

        // Update each parameter dimension
        for (int i = 0; i < dimension; i++) {
            double deltaMu = newMeans[i] - oldMeans[i];
            double newParamValue = oldParams[i] + deltaMu;

            // Check bounds
            if (newParamValue < parameter.getLower() || newParamValue > parameter.getUpper()) {
                // Revert all parameter changes made so far
                for (int j = 0; j < i; j++) {
                    parameter.setValue(j, oldParams[j]);
                }
                return false;
            }

            parameter.setValue(i, newParamValue);
        }

        return true;
    }

    @Override
    public double getCoercableParameterValue() {
        return windowSize;
    }

    @Override
    public void setCoercableParameterValue(double value) {
        windowSize = value;
    }

    @Override
    public void optimize(double logAlpha) {
        if (optimiseInput.get()) {
            double delta = calcDelta(logAlpha);
            delta += Math.log(windowSize);
            windowSize = Math.exp(delta);
        }
    }

    @Override
    public double getTargetAcceptanceProbability() {
        // Target acceptance rate for random walk operators
        // Typical values: 0.234 for multivariate, 0.44 for univariate
        return updateAllCoefficients ? 0.234 : 0.44;
    }

    @Override
    public String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        double newWindowSize = windowSize * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10 || prob > 0.70) {
            return "Try setting windowSize to about " + formatter.format(newWindowSize);
        }
        return "";
    }

} // class GLMJointCoefficientParameterOperator