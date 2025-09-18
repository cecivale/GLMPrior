package glmprior.util;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;

/**
 * A RealParameter that wraps a Function, allowing Function-based computations
 * to be used wherever a RealParameter is expected in BEAST2.
 * 
 * This adapter pattern enables GLMPrior and other Function implementations
 * to work seamlessly with packages that expect RealParameter inputs.
 * 
 * The parameter values are computed on-demand from the underlying Function.
 * Setting values directly is not supported since the values are computed.
 */
@Description("RealParameter adapter that wraps a Function for computed parameter values")
public class FunctionParameter extends RealParameter {

    public Input<Function> functionInput = new Input<>(
            "function", 
            "Function that computes the parameter values", 
            Input.Validate.REQUIRED);

    private Function function;
    private boolean initialized = false;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        function = functionInput.get();
        if (function == null) {
            throw new IllegalArgumentException("Function input is required");
        }

        // Initialize the underlying function if it hasn't been initialized yet
        if (function instanceof beast.base.core.BEASTObject beastFunction) {
            beastFunction.initAndValidate();
        }
        
        initialized = true;

    }

    @Override
    public int getDimension() {
        if (!initialized || function == null) {
            return 0;
        }
        return function.getDimension();
    }

    @Override
    public Double getValue() {
        return getValue(0);
    }

    @Override
    public Double getValue(int index) {
        if (!initialized || function == null) {
            throw new IllegalStateException("FunctionParameter not properly initialized");
        }
        
        if (index < 0 || index >= function.getDimension()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for dimension " + function.getDimension());
        }
        
        return function.getArrayValue(index);
    }

    @Override
    public Double[] getValues() {
        if (!initialized || function == null) {
            throw new IllegalStateException("FunctionParameter not properly initialized");
        }
        
        int dim = function.getDimension();
        Double[] values = new Double[dim];
        for (int i = 0; i < dim; i++) {
            values[i] = function.getArrayValue(i);
        }
        return values;
    }

    @Override
    public double getArrayValue() {
        return getValue(0);
    }

    @Override
    public double getArrayValue(int index) {
        if (!initialized || function == null) {
            throw new IllegalStateException("FunctionParameter not properly initialized");
        }
        return function.getArrayValue(index);
    }

    /**
     * Setting values is not supported since values are computed from the underlying Function.
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setValue(int index, Double value) {
        throw new UnsupportedOperationException("Cannot set values on FunctionParameter - values are computed from underlying Function");
    }

    /**
     * Setting values is not supported since values are computed from the underlying Function.
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setValue(Double value) {
        throw new UnsupportedOperationException("Cannot set values on FunctionParameter - values are computed from underlying Function");
    }

    @Override
    public String toString() {
        if (!initialized || function == null) {
            return "FunctionParameter[uninitialized]";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("FunctionParameter[");
        int dim = function.getDimension();
        for (int i = 0; i < dim; i++) {
            if (i > 0) sb.append(", ");
            sb.append(function.getArrayValue(i));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Returns the underlying Function that provides the computed values.
     */
    public Function getFunction() {
        return function;
    }

}