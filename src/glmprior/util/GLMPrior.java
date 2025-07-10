package glmprior.util;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Loggable;
import beast.base.inference.CalculationNode;
import beast.base.core.Function;
import beast.base.core.BEASTObject;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;

import java.io.PrintStream;
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
public class GLMPrior extends CalculationNode implements Function, Loggable {

    public Input<String> linkFunctionInput = new Input<>("linkFunction",
            "Link function to use: log, logit, or identity. Default is log.",
            "log", Input.Validate.OPTIONAL);
    public  Input<List<Function>>  predictorsInput = new Input<>("predictor",
            "One or more predictor for the GLM, e.g. numbers of flights between different locations",
            new ArrayList<>(), Input.Validate.REQUIRED);

    public Input<RealParameter> coefficientsInput = new Input<>("coefficients",
            "GLM coefficients.", Input.Validate.REQUIRED);

    public Input<BooleanParameter> indicatorsInput = new Input<>("indicators",
            "Indicators for predictor inclusion/exclusion in GLM.", Input.Validate.REQUIRED);

    public Input<RealParameter> baselineValueInput = new Input<>("baselineValue",
            "GLM intercept, baseline value of the parameter when all coefficients or indicators are 0.", new RealParameter("1.0"), Input.Validate.REQUIRED);

    public Input<RealParameter> errorInput = new Input<>("error",
            "Error terms.", Input.Validate.OPTIONAL);

    public Input<Boolean> logTransformInput = new Input<>("logTransform",
            "Whether to log-transform the predictors using log(x + 1). Default false.", false,
            Input.Validate.OPTIONAL);

    public Input<Boolean> standardizeInput = new Input<>("standardize",
            "Whether to standardize predictors (mean 0, sd 1) after transformation. Default false.", false,
            Input.Validate.OPTIONAL);

    String link;
    List<Function> predictors;
    RealParameter coefficients, baselineValue, error;
    BooleanParameter indicators;
    int parameterSize, numPredictors;
    double intercept;  // internal intercept after transformation of baseline Value
    @Override
    public void initAndValidate() {
        coefficients = coefficientsInput.get();
        indicators = indicatorsInput.get();
        baselineValue = baselineValueInput.get();
        predictors = predictorsInput.get();

        numPredictors = predictors.size();
        parameterSize = predictors.get(0).getDimension();

        link = linkFunctionInput.get().toLowerCase();

        switch (link) {
            case "log":
                if (baselineValue.getValue() <= 0) {
                    throw new IllegalArgumentException("Baseline value must be positive for log link.");
                }
                break;

            case "logit":
                if (baselineValue.getValue() <= 0 || baselineValue.getValue() >= 1) {
                    throw new IllegalArgumentException("Baseline probability must be in (0,1) for logit link.");
                }
                break;

            case "identity":
                break;

            default:
                throw new IllegalArgumentException("Unknown link function: " + link + " Only log, logit or identity functions are allowed.");
        }


        for (Function pred : predictors)
            if (parameterSize != pred.getDimension())
                throw new IllegalArgumentException("GLM Predictors do not have the same dimension " +
                        parameterSize + "!=" +  pred.getDimension());

        coefficients.setDimension(numPredictors);
        indicators.setDimension(numPredictors);

        if (baselineValue.getDimension() != 1)
            throw new IllegalArgumentException("Dimension of GLM scale factor should be 1.");



        if (logTransformInput.get() || standardizeInput.get()) {
            Double[] predT;
            List<Function> transformedPredictors = new ArrayList<>();
            double pred, mean, sd;

            for (int j = 0; j < numPredictors; j++) {
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

            predictors = transformedPredictors;
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


    }

    @Override
    public int getDimension() {
        return parameterSize;
    }

    @Override
    public double getArrayValue(int i) {

        double linearPredictor = 0;
        for (int j = 0; j < coefficients.getDimension(); j++) {
            if (indicators.getArrayValue(j) > 0.0) {
                linearPredictor += coefficients.getArrayValue(j) * predictors.get(j).getArrayValue(i);
            }
        }

        if (error != null) {
            linearPredictor += error.getArrayValue(i % error.getDimension());
        }

        switch (link) {
            case "log":
                intercept = Math.log(baselineValue.getValue());
                linearPredictor += intercept;
                return Math.exp(linearPredictor);

            case "logit":
                intercept = Math.log(baselineValue.getValue() / (1 - baselineValue.getValue())); // logit transform
                linearPredictor += intercept;
                return 1.0 / (1.0 + Math.exp(-linearPredictor));

            case "identity":
                intercept = baselineValue.getValue();
                linearPredictor += intercept;
                return linearPredictor;

            default:
                throw new IllegalArgumentException("Unknown link function: " + link);
        }
    }

    private String getPredictorID(Input<List<Function>> p, int i) {
        Function predictor = p.get().get(i);
        String predID = ((BEASTObject) predictor).getID();
        return (predID == null || predID.equals(predictor.getClass().getSimpleName())) ? String.valueOf(i + 1) : predID;
    }

    /*
     * Loggable implementation
     */
    @Override
    public void init(PrintStream out) {

        // baseline
        out.print(getID() + "_baseline\t");

//        // coefficients all
//        for (int i = 0; i < coefficients.getDimension(); i++) {
//            out.print(getID() + "_coefficient." + i + "\t");
//        }
        // coefficients filtered by indicators (only those with indicator == 1)
        for (int i = 0; i < coefficients.getDimension(); i++) {
            out.print(getID() + "_coefficientON." + getPredictorID(predictorsInput, i) + "\t");
        }

        // indicators
        for (int i = 0; i < indicators.getDimension(); i++) {
            out.print(getID() + "_indicator." + getPredictorID(predictorsInput, i) + "\t");
        }

        // error terms if defined
        if (error != null) {
            for (int i = 0; i < error.getDimension(); i++) {
                out.print(getID() + "_error." + String.valueOf(i + 1) + "\t");
            }
        }

        // GLM values for each dimension
        for (int i = 0; i < getDimension(); i++) {
            out.print(getID() + "_value." + String.valueOf(i + 1) + "\t");
        }
    }

    @Override
    public void log(long sample, PrintStream out) {

        // baseline (intercept)
        out.print(baselineValue.getValue() + "\t");
//
//        // all coefficients
//        for (int i = 0; i < coefficients.getDimension(); i++) {
//            out.print(coefficients.getArrayValue(i) + "\t");
//        }

        // coefficients where indicators == 1, 0 otherwise
        for (int i = 0; i < coefficients.getDimension(); i++) {
            double coefSelected = (indicators.getArrayValue(i) > 0.0) ? coefficients.getArrayValue(i) : 0.0;
            out.print(coefSelected + "\t");
        }

        // indicators
        for (int i = 0; i < indicators.getDimension(); i++) {
            out.print(indicators.getArrayValue(i) + "\t");
        }

        // error terms (if present)
        if (error != null) {
            for (int i = 0; i < error.getDimension(); i++) {
                out.print(error.getArrayValue(i) + "\t");
            }
        }

        // GLM values for each dimension (predicted values)
        for (int i = 0; i < getDimension(); i++) {
            out.print(getArrayValue(i)+ "\t");

        }
    }

    @Override
    public void close(PrintStream out) {
    }

}