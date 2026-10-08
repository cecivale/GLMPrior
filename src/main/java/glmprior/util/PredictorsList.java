package glmprior.util;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.CalculationNode;
import beast.base.inference.parameter.RealParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that manages a list of predictor functions with optional log transformation
 * and standardization. This class transforms predictors at initialization and provides
 * access to the transformed predictor list.
 *
 * Transformations are applied in the following order:
 * 1. Log transformation: log(x + 1) if logTransform is true
 * 2. Standardization: (x - mean) / sd if standardize is true
 *
 * @author Cecilia Valenzuela Agui
 */
@Description("Manages a list of predictors with optional log transformation and standardization")
public class PredictorsList extends CalculationNode implements Function {

    public Input<List<Function>> predictorsInput = new Input<>(
            "predictor",
            "One or more predictor for the GLM, e.g. numbers of flights between different locations",
            new ArrayList<>(),
            Input.Validate.REQUIRED);

    public Input<Boolean> logTransformInput = new Input<>(
            "logTransform",
            "Whether to log-transform the predictors using log(x + 1). Default false.",
            false,
            Input.Validate.OPTIONAL);

    public Input<Boolean> standardizeInput = new Input<>(
            "standardize",
            "Whether to standardize predictors (mean 0, sd 1) after transformation. Default false.",
            false,
            Input.Validate.OPTIONAL);

    // Cached transformed predictors
    private List<Function> transformedPredictors;
    private int numPredictors;
    private int parameterSize;

    @Override
    public void initAndValidate() {
        List<Function> originalPredictors = predictorsInput.get();

        if (originalPredictors.isEmpty()) {
            throw new IllegalArgumentException("At least one predictor must be provided");
        }

        numPredictors = originalPredictors.size();
        parameterSize = originalPredictors.get(0).getDimension();

        // Validate that all predictors have the same dimension
        for (Function pred : originalPredictors) {
            if (parameterSize != pred.getDimension()) {
                throw new IllegalArgumentException("All predictors must have the same dimension. " +
                        "Expected: " + parameterSize + ", found: " + pred.getDimension());
            }
        }

        // Apply transformations if requested
        if (logTransformInput.get() || standardizeInput.get()) {
            transformedPredictors = applyTransformations(originalPredictors);
        } else {
            // No transformations - use original predictors
            transformedPredictors = new ArrayList<>(originalPredictors);
        }
    }

    /**
     * Applies log transformation and/or standardization to the predictors.
     */
    private List<Function> applyTransformations(List<Function> originalPredictors) {
        List<Function> result = new ArrayList<>();
        boolean logTransform = logTransformInput.get();
        boolean standardize = standardizeInput.get();

        for (int j = 0; j < numPredictors; j++) {
            Double[] transformedValues = new Double[parameterSize];
            double mean = 0.0;
            double sd = 0.0;

            // Step 1: Log transformation (if requested)
            for (int i = 0; i < parameterSize; i++) {
                double value = originalPredictors.get(j).getArrayValue(i);

                if (logTransform) {
                    if (value < 0.0) {
                        throw new IllegalArgumentException(
                                "Predictor " + j + " contains negative value (" + value + "). " +
                                "Cannot apply log transformation to negative values.");
                    }
                    value = Math.log(value + 1.0);
                }

                transformedValues[i] = value;
                mean += value;
            }

            // Step 2: Standardization (if requested)
            if (standardize) {
                mean /= parameterSize;

                // Calculate standard deviation
                for (int i = 0; i < parameterSize; i++) {
                    double deviation = transformedValues[i] - mean;
                    sd += deviation * deviation;
                }
                sd = Math.sqrt(sd / parameterSize);

                if (sd == 0.0) {
                    throw new IllegalArgumentException(
                            "Standard deviation of predictor " + j + " is zero. " +
                            "Cannot standardize constant predictor.");
                }

                // Apply standardization
                for (int i = 0; i < parameterSize; i++) {
                    transformedValues[i] = (transformedValues[i] - mean) / sd;
                }
            }

            result.add(new RealParameter(transformedValues));
        }

        return result;
    }

    /**
     * Returns the list of (transformed) predictors.
     */
    public List<Function> getPredictors() {
        return new ArrayList<>(transformedPredictors);
    }

    /**
     * Returns a specific predictor by index.
     */
    public Function getPredictor(int index) {
        if (index < 0 || index >= numPredictors) {
            throw new IndexOutOfBoundsException("Invalid predictor index: " + index);
        }
        return transformedPredictors.get(index);
    }

    /**
     * Returns the number of predictors.
     */
    public int getNumPredictors() {
        return numPredictors;
    }

    /**
     * Returns the dimension of each predictor (i.e., the parameter size).
     */
    @Override
    public int getDimension() {
        return parameterSize;
    }

    /**
     * Returns the value at index i from the first predictor.
     * This is primarily for Function interface compatibility.
     */
    @Override
    public double getArrayValue(int i) {
        if (transformedPredictors.isEmpty()) {
            throw new IllegalStateException("No predictors available");
        }
        return transformedPredictors.get(0).getArrayValue(i);
    }

    /**
     * Returns the value at index i from a specific predictor.
     */
    public double getArrayValue(int predictorIndex, int valueIndex) {
        if (predictorIndex < 0 || predictorIndex >= numPredictors) {
            throw new IndexOutOfBoundsException("Invalid predictor index: " + predictorIndex);
        }
        return transformedPredictors.get(predictorIndex).getArrayValue(valueIndex);
    }

    /**
     * Returns whether log transformation was applied.
     */
    public boolean isLogTransformed() {
        return logTransformInput.get();
    }

    /**
     * Returns whether standardization was applied.
     */
    public boolean isStandardized() {
        return standardizeInput.get();
    }
}