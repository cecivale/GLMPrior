package glmprior.util;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.CalculationNode;
import beast.base.core.Function;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Cecilia Valenzuela Agui
 */
@Description(
        "A function that implements a GLM Log Linear model from a set of predictors, "
                + "coefficients, indicator variables for predictor selection, and optionally "
                + "a global scale factor and error terms. Predictors are log transform and scale by default."
)
public class GLMLogLinear extends CalculationNode implements Function {

    public  Input<List<Function>>  predictorsInput = new Input<>("predictor",
            "One or more predictor for the GLM, e.g. numbers of flights between different locations",
            new ArrayList<>(), Input.Validate.REQUIRED);

    public Input<RealParameter> coefficientsInput = new Input<>("coefficients",
            "GLM coefficients.", Input.Validate.REQUIRED);

    public Input<BooleanParameter> indicatorsInput = new Input<>("indicators",
            "Indicators for predictor inclusion/exclusion in GLM.", Input.Validate.REQUIRED);

    public Input<RealParameter> baselineValueInput = new Input<>("baselineValue",
            "GLM intercept, baseline value of the parameter when all coefficients or indicators are 0.", new RealParameter("1.0"), Input.Validate.OPTIONAL);

    public Input<RealParameter> errorInput = new Input<>("error",
            "Error terms.", Input.Validate.OPTIONAL);

    public Input<Boolean> logTransformInput = new Input<>("logTransform",
            "Whether to log-transform the predictors using log(x + 1). Default false.", false,
            Input.Validate.OPTIONAL);

    public Input<Boolean> standardizeInput = new Input<>("standardize",
            "Whether to standardize predictors (mean 0, sd 1) after transformation. Default false.", false,
            Input.Validate.OPTIONAL);
    public  Input<List<Function>>  predictorsTInput = new Input<>("predictorT",
            "Predictor transformed, internal to the class",
            new ArrayList<>(), Input.Validate.OPTIONAL);

    List<Function> predictors;
    RealParameter coefficients, baselineValue, error;
    BooleanParameter indicators;
    int parameterSize, predictorN;

    @Override
    public void initAndValidate() {
        coefficients = coefficientsInput.get();
        indicators = indicatorsInput.get();
        baselineValue = baselineValueInput.get();
        predictors = predictorsInput.get();

        predictorN = predictors.size();
        parameterSize = predictors.get(0).getDimension();
        for (Function pred : predictors)
            if (parameterSize != pred.getDimension())
                throw new IllegalArgumentException("GLM Predictors do not have the same dimension " +
                        parameterSize + "!=" +  pred.getDimension());

        coefficients.setDimension(predictorN);
        indicators.setDimension(predictorN);

        if (baselineValue.getDimension() != 1)
            throw new IllegalArgumentException("Dimension of GLM scale factor should be 1.");

        if (baselineValue.getArrayValue() <= 0.0) {
            throw new IllegalArgumentException("Baseline value must be positive.");
        }

        if (errorInput.get() != null) {
            error = errorInput.get();
            if (error.getDimension() == 1) {
               error.setDimension(parameterSize);
            }

            if (parameterSize % error.getDimension() != 0)
                throw new IllegalArgumentException("GLM error term has an incorrect number "
                        + "of elements.");
        }

        if (logTransformInput.get() || standardizeInput.get()) {
            Double[] predT;
            List<Function> transformedPredictors = new ArrayList<>();
            double pred, mean, sd;

            for (int j = 0; j < predictorN; j++) {
                predT = new Double[parameterSize];
                mean = 0;
                sd = 0;

                for (int i = 0; i < parameterSize; i++) {
                    pred = predictors.get(j).getArrayValue(i);
                    if (logTransformInput.get()) {
                        if (pred < 0.0)
                            throw new IllegalArgumentException("Predictor should not be smaller than 0 to be log transformed.");
                        pred = Math.log(pred + 1);
                    }
                    predT[i] = pred;
                    mean += pred;
                }

                if (standardizeInput.get()) {
                    mean /= parameterSize;
                    for (int i = 0; i < parameterSize; i++) {
                        sd += (predT[i] - mean) * (predT[i] - mean);
                    }
                    sd = Math.sqrt(sd / parameterSize);
                    if (sd == 0.0) {
                        throw new IllegalArgumentException("Standard deviation of predictor " + j + " is zero, cannot standardize.");
                    }
                    for (int i = 0; i < parameterSize; i++) {
                        predT[i] = (predT[i] - mean) / sd;
                    }
                }

                transformedPredictors.add(new RealParameter(predT));
            }
            predictorsTInput.setValue(transformedPredictors, this);
            predictors = predictorsTInput.get();
        }

    }

    @Override
    public int getDimension() {
        return parameterSize;
    }

    @Override
    public double getArrayValue(int i) {
        double lograte = 0;
        for (int j = 0; j < coefficients.getDimension(); j++) {
            if (indicators.getArrayValue(j) > 0.0) {
                lograte += coefficients.getArrayValue(j) * predictors.get(j).getArrayValue(i);
            }
        }

        if (error != null)
            lograte += error.getArrayValue(i % error.getDimension());

        return baselineValue.getArrayValue() * Math.exp(lograte);
    }
}