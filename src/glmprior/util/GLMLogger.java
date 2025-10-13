package glmprior.util;

import beast.base.core.BEASTObject;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Loggable;
import beast.base.inference.CalculationNode;
import beast.base.inference.parameter.RealParameter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Logger for MultiGLMDistribution that logs intercept, coefficients (filtered by indicators),
 * indicators, and errors from the mean.
 */
public class GLMLogger extends CalculationNode implements Loggable {
    public Input<MultiGLMDistribution> distributionInput = new Input<>(
            "multiGLMDistributions", "Multi-GLM distributions",  Input.Validate.REQUIRED);

    public Input<String> prefixInput = new Input<>("prefix", "Parameter name or other prefix to use. Default: none",
            "", Input.Validate.OPTIONAL);

    public Input<Function> paramInput = new Input<>(
            "parameter", "Parameter that GLM distribution is used on.",
            Input.Validate.OPTIONAL);

    private int coeffDim;
    private int numDistributions;

    /**
     * Gets the predictor ID from the MultiGLMDistribution's predictor input.
     */
    private String getPredictorID(MultiGLMDistribution multiGLM, int i) {
        List<Function> predictors = multiGLM.predictorsInput.get();
        if (i >= 0 && i < predictors.size()) {
            Function predictor = predictors.get(i);
            if (predictor instanceof BEASTObject) {
                String predID = ((BEASTObject) predictor).getID();
                if (predID != null && !predID.equals(predictor.getClass().getSimpleName())) {
                    return predID;
                }
            }
        }
        return String.valueOf(i + 1);
    }

    @Override
    public void initAndValidate() {
        // Cache dimensions
        coeffDim = distributionInput.get().coefficientsInput.get().getDimension();

        // Calculate total number of dimensions across all MultiGLMDistributions
        numDistributions = distributionInput.get().getNumDimensions();
    }

    @Override
    public void init(PrintStream out) {
        String prefix = prefixInput.get();
        if (prefix == null || prefix.isEmpty()) {
            if (paramInput.get() instanceof RealParameter)
                prefix = ((RealParameter) paramInput.get()).getID();
        }
        prefix = prefix + "_";

        // baseline (intercept)
        out.print(prefix + "baseline\t");

        // coefficients filtered by indicators (only those with indicator == 1)
        for (int i = 0; i < coeffDim; i++) {
            out.print(prefix + "coefficientON." + getPredictorID(distributionInput.get(), i) + "\t");
        }

        // indicators
        if (distributionInput.get().indicatorsInput.get() != null) {
            for (int i = 0; i < coeffDim; i++) {
                out.print(prefix + "indicator." + getPredictorID(distributionInput.get(), i) + "\t");
            }
        }

        // error from mean for each dimension
        if (paramInput.get() != null) {
            for (int i = 0; i < numDistributions; i++) {
                out.print(prefix + "error." + (i + 1) + "\t");
            }
        }
    }

    @Override
    public void log(long sample, PrintStream out) {

        MultiGLMDistribution distr = distributionInput.get();

        // baseline (intercept)
        out.print(LinkFunctions.inverse(LinkFunction.valueOf(distr.linkInput.get()), distr.interceptInput.get().getValue()) + "\t");

        // coefficients where indicators == 1, 0 otherwise
        for (int i = 0; i < coeffDim; i++) {
            double coefficient = distr.coefficientsInput.get().getArrayValue(i);

            if (distr.indicatorsInput.get() != null) {
                boolean isActive = distr.indicatorsInput.get().getValue(i);
                coefficient = isActive ? coefficient : 0.0;
            }

            out.print(coefficient + "\t");
        }

        // indicators (if present)
        if (distr.indicatorsInput.get() != null) {
            for (int i = 0; i < coeffDim; i++) {
                out.print(distr.indicatorsInput.get().getArrayValue(i) + "\t");
            }
        }

        // error terms (difference between parameter value and GLM mean)
        if (paramInput.get() != null) {
            int paramIdx = 0;
            for (int i = 0; i < numDistributions; i++) {
                double glmMean = distr.getMean(i);
                double paramValue = paramInput.get().getArrayValue(paramIdx);
                double error = glmMean - paramValue;
                out.print(error + "\t");
                paramIdx++;
            }
        }
    }

    @Override
    public void close(PrintStream out) {
        // Nothing to close
    }
}