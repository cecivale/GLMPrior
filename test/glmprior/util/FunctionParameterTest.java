package glmprior.util;

import beast.base.core.Function;
import beast.base.inference.parameter.RealParameter;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for FunctionParameter class.
 * Tests the adapter pattern for wrapping Functions as RealParameters.
 */
public class FunctionParameterTest {

    private FunctionParameter functionParam;
    private MockFunction mockFunction;

    /**
     * Simple mock Function for testing.
     */
    private static class MockFunction implements Function {
        private double[] values;
        
        public MockFunction(double... values) {
            this.values = values;
        }
        
        public void setValues(double... values) {
            this.values = values;
        }
        
        @Override
        public int getDimension() {
            return values.length;
        }
        
        @Override
        public double getArrayValue() {
            return values[0];
        }
        
        @Override
        public double getArrayValue(int index) {
            return values[index];
        }
    }

    @Before
    public void setUp() {
        mockFunction = new MockFunction(1.0, 2.0, 3.0);
        functionParam = new FunctionParameter();
        functionParam.functionInput.setValue(mockFunction, functionParam);
        functionParam.initAndValidate();
    }

    @Test
    public void testDimension() {
        assertEquals("Dimension should match function dimension", 3, functionParam.getDimension());
        
        // Test dimension changes when function changes
        mockFunction.setValues(1.0, 2.0, 3.0, 4.0, 5.0);
        assertEquals("Dimension should update when function changes", 5, functionParam.getDimension());
    }

    @Test
    public void testGetValue() {
        assertEquals("First value should be 1.0", (Double) 1.0, functionParam.getValue());
        assertEquals("getValue() should equal getValue(0)", functionParam.getValue(), functionParam.getValue(0));
    }

    @Test
    public void testGetValueByIndex() {
        assertEquals("Value at index 0", (Double) 1.0, functionParam.getValue(0));
        assertEquals("Value at index 1", (Double) 2.0, functionParam.getValue(1));
        assertEquals("Value at index 2", (Double) 3.0, functionParam.getValue(2));
    }

    @Test
    public void testGetValues() {
        Double[] values = functionParam.getValues();
        assertArrayEquals("All values should match", 
                new Double[]{1.0, 2.0, 3.0}, values);
    }

    @Test
    public void testGetArrayValue() {
        assertEquals("Array value at index 0", 1.0, functionParam.getArrayValue(), 1e-15);
        assertEquals("Array value at index 1", 2.0, functionParam.getArrayValue(1), 1e-15);
        assertEquals("Array value at index 2", 3.0, functionParam.getArrayValue(2), 1e-15);
    }

    @Test
    public void testDynamicUpdates() {
        // Change the underlying function values
        mockFunction.setValues(10.0, 20.0, 30.0);
        
        // FunctionParameter should reflect the changes
        assertEquals("Updated value at index 0", (Double) 10.0, functionParam.getValue(0));
        assertEquals("Updated value at index 1", (Double) 20.0, functionParam.getValue(1));
        assertEquals("Updated value at index 2", (Double) 30.0, functionParam.getValue(2));
        
        Double[] values = functionParam.getValues();
        assertArrayEquals("All updated values should match", 
                new Double[]{10.0, 20.0, 30.0}, values);
    }

    @Test
    public void testIndexOutOfBounds() {
        try {
            functionParam.getValue(-1);
            fail("Should throw IndexOutOfBoundsException for negative index");
        } catch (IndexOutOfBoundsException e) {
            assertTrue("Expected index out of bounds message", 
                    e.getMessage().contains("out of bounds"));
        }

        try {
            functionParam.getValue(3);
            fail("Should throw IndexOutOfBoundsException for index >= dimension");
        } catch (IndexOutOfBoundsException e) {
            assertTrue("Expected index out of bounds message", 
                    e.getMessage().contains("out of bounds"));
        }
    }

    @Test
    public void testSetValueNotSupported() {
        try {
            functionParam.setValue(0, 99.0);
            fail("Should throw UnsupportedOperationException for setValue");
        } catch (UnsupportedOperationException e) {
            assertTrue("Expected unsupported operation message", 
                    e.getMessage().contains("Cannot set values"));
        }

        try {
            functionParam.setValue(99.0);
            fail("Should throw UnsupportedOperationException for setValue");
        } catch (UnsupportedOperationException e) {
            assertTrue("Expected unsupported operation message", 
                    e.getMessage().contains("Cannot set values"));
        }

        try {
            functionParam.setValueQuietly(0, 99.0);
            fail("Should throw UnsupportedOperationException for setValueQuietly");
        } catch (UnsupportedOperationException e) {
            assertTrue("Expected unsupported operation message", 
                    e.getMessage().contains("Cannot set values"));
        }
    }

    @Test
    public void testAssignNotSupported() {
        try {
            functionParam.assignFrom("1.0 2.0 3.0");
            fail("Should throw UnsupportedOperationException for assignFrom");
        } catch (UnsupportedOperationException e) {
            assertTrue("Expected unsupported operation message", 
                    e.getMessage().contains("Cannot assign values"));
        }

        try {
            functionParam.assignFromWithoutID("1.0 2.0 3.0");
            fail("Should throw UnsupportedOperationException for assignFromWithoutID");
        } catch (UnsupportedOperationException e) {
            assertTrue("Expected unsupported operation message", 
                    e.getMessage().contains("Cannot assign values"));
        }
    }

    @Test
    public void testGetFunction() {
        assertEquals("Should return the wrapped function", mockFunction, functionParam.getFunction());
    }

    @Test
    public void testIsComputed() {
        assertTrue("FunctionParameter should always be computed", functionParam.isComputed());
    }

    @Test
    public void testToString() {
        String str = functionParam.toString();
        assertTrue("ToString should contain values", str.contains("1.0"));
        assertTrue("ToString should contain values", str.contains("2.0"));
        assertTrue("ToString should contain values", str.contains("3.0"));
        assertTrue("ToString should identify as FunctionParameter", str.contains("FunctionParameter"));
    }

    @Test
    public void testEmptyFunction() {
        MockFunction emptyFunction = new MockFunction(); // No values
        FunctionParameter emptyParam = new FunctionParameter();
        emptyParam.functionInput.setValue(emptyFunction, emptyParam);
        emptyParam.initAndValidate();
        
        assertEquals("Empty function should have dimension 0", 0, emptyParam.getDimension());
        
        try {
            emptyParam.getValue(0);
            fail("Should throw IndexOutOfBoundsException for empty function");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
    }

    @Test
    public void testSingleValueFunction() {
        MockFunction singleFunction = new MockFunction(42.0);
        FunctionParameter singleParam = new FunctionParameter();
        singleParam.functionInput.setValue(singleFunction, singleParam);
        singleParam.initAndValidate();
        
        assertEquals("Single value function should have dimension 1", 1, singleParam.getDimension());
        assertEquals("Single value should be correct", (Double) 42.0, singleParam.getValue());
        assertEquals("Single value should be correct at index 0", (Double) 42.0, singleParam.getValue(0));
        
        Double[] values = singleParam.getValues();
        assertEquals("Single value array should have length 1", 1, values.length);
        assertEquals("Single value should be correct in array", (Double) 42.0, values[0]);
    }

    @Test
    public void testRequiresRecalculation() {
        assertTrue("FunctionParameter should always require recalculation", 
                functionParam.requiresRecalculation());
    }

    @Test
    public void testWithGLMDistribution() {
        // Test with actual GLMDistribution
        GLMDistribution glm = new GLMDistribution();
        glm.interceptInput.setValue(new RealParameter("2.0"), glm);
        glm.coefficientsInput.setValue(new RealParameter("1.5 -0.5"), glm);
        glm.predictorsInput.setValue(new RealParameter("1.0 2.0"), glm);
        glm.sigmaInput.setValue(new RealParameter("1.0"), glm);
        glm.initAndValidate();
        
        FunctionParameter glmParam = new FunctionParameter();
        glmParam.functionInput.setValue(glm, glmParam);
        glmParam.initAndValidate();
        
        assertEquals("GLM should have dimension 1", 1, glmParam.getDimension());
        
        // Expected: intercept + coefficients * predictors = 2.0 + 1.5*1.0 + (-0.5)*2.0 = 2.5
        double expectedValue = 2.5;
        assertEquals("GLM computed value should be correct", expectedValue, glmParam.getArrayValue(), 1e-10);
    }

    @Test
    public void testNullFunction() {
        FunctionParameter nullParam = new FunctionParameter();
        // Don't set function input
        
        try {
            nullParam.initAndValidate();
            fail("Should throw exception for null function");
        } catch (IllegalArgumentException e) {
            assertTrue("Expected null function message", 
                    e.getMessage().contains("Function input is required"));
        }
    }
}