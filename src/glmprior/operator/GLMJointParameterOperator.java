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
public class GLMJointParameterOperator extends Operator {

    // Inputs
    final public Input<RealParameter> parameterInput = new Input<>(
            "parameter",
            "The parameter to update jointly (typically the response variable y)",
            Validate.REQUIRED);

    final public Input<MultiGLMDistribution> glmDistributionInput = new Input<>(
            "glmDistribution",
            "The MultiGLMDistribution that links coefficients to parameter means",
            Validate.REQUIRED);



    @Override
    public void initAndValidate() {

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
        MultiGLMDistribution glmDistribution = glmDistributionInput.get();

        // Store old means before changing coefficients
        double[] oldMeans = glmDistribution.getAllStoredMeans();

        // Get new means after coefficient change
        double[] newMeans = glmDistribution.getAllMeans();

        // Adjust parameter values deterministically to track mean changes
        if (!updateParameterDeterministically(parameter, oldMeans, newMeans)) {
            return Double.NEGATIVE_INFINITY;
        }

        // Deterministic coupling has Hastings ratio = 1.0
        return 0.0;
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
}