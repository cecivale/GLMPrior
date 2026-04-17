package glmprior.util;

import beast.base.inference.parameter.RealParameter;
import beast.base.inference.parameter.BooleanParameter;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for indicator functionality in GLMDistribution.
 * Tests Bayesian variable selection capabilities.
 */
public class GLMIndicatorTest {

    private GLMDistribution glm;
    private RealParameter intercept;
    private RealParameter coefficients;
    private RealParameter predictors;
    private RealParameter sigma;
    private BooleanParameter indicators;

    @Before
    public void setUp() {
        glm = new GLMDistribution();
        
        // Set up GLM components for 3 predictors
        intercept = new RealParameter("1.0");
        coefficients = new RealParameter("2.0 3.0 4.0");  // 3 coefficients
        predictors = new RealParameter("1.0 1.0 1.0");    // 3 predictors (all = 1 for simplicity)
        sigma = new RealParameter("1.0");
        
        glm.interceptInput.setValue(intercept, glm);
        glm.coefficientsInput.setValue(coefficients, glm);
        glm.predictorsInput.setValue(predictors, glm);
        glm.sigmaInput.setValue(sigma, glm);
    }

    @Test
    public void testNoIndicators() {
        // Test without indicators - should use all coefficients
        glm.initAndValidate();
        
        // Expected mean: 1.0 + 2.0*1.0 + 3.0*1.0 + 4.0*1.0 = 10.0
        double expectedMean = 10.0;
        assertEquals("GLM without indicators", expectedMean, glm.getMean(), 1e-10);
    }

    @Test
    public void testAllIndicatorsTrue() {
        // Test with all indicators set to true - should behave same as no indicators
        indicators = new BooleanParameter("true true true");
        glm.indicatorsInput.setValue(indicators, glm);
        
        glm.initAndValidate();
        
        // Expected mean: 1.0 + 2.0*1.0 + 3.0*1.0 + 4.0*1.0 = 10.0
        double expectedMean = 10.0;
        assertEquals("GLM with all indicators true", expectedMean, glm.getMean(), 1e-10);
    }

    @Test
    public void testAllIndicatorsFalse() {
        // Test with all indicators set to false - should use only intercept
        indicators = new BooleanParameter("false false false");
        glm.indicatorsInput.setValue(indicators, glm);
        
        glm.initAndValidate();
        
        // Expected mean: 1.0 + 0*2.0*1.0 + 0*3.0*1.0 + 0*4.0*1.0 = 1.0
        double expectedMean = 1.0;
        assertEquals("GLM with all indicators false", expectedMean, glm.getMean(), 1e-10);
    }

    @Test
    public void testSelectiveIndicators() {
        // Test with selective indicators - only first and third variables active
        indicators = new BooleanParameter("true false true");
        glm.indicatorsInput.setValue(indicators, glm);
        
        glm.initAndValidate();
        
        // Expected mean: 1.0 + 2.0*1.0 + 0*3.0*1.0 + 4.0*1.0 = 7.0
        double expectedMean = 7.0;
        assertEquals("GLM with selective indicators", expectedMean, glm.getMean(), 1e-10);
    }

    @Test
    public void testIndicatorDimensionMismatch() {
        // Test dimension mismatch between indicators and coefficients
        indicators = new BooleanParameter("true false"); // Only 2 indicators for 3 coefficients
        glm.indicatorsInput.setValue(indicators, glm);
        
        try {
            glm.initAndValidate();
            fail("Should have thrown exception for dimension mismatch");
        } catch (IllegalArgumentException e) {
            assertTrue("Expected dimension mismatch message", 
                    e.getMessage().contains("Dimension mismatch: indicators"));
        }
    }

    @Test
    public void testIndicatorsWithDifferentLinkFunction() {
        // Test indicators work with different link functions
        indicators = new BooleanParameter("true false true");
        glm.indicatorsInput.setValue(indicators, glm);
        glm.linkInput.setValue(LinkFunction.LOG, glm);
        
        glm.initAndValidate();
        
        // Linear predictor: η = 1.0 + 2.0*1.0 + 0*3.0*1.0 + 4.0*1.0 = 7.0
        // Mean with log link: μ = exp(7.0)
        double expectedMean = Math.exp(7.0);
        assertEquals("GLM with indicators and log link", expectedMean, glm.getMean(), 1e-8);
    }

    @Test
    public void testIndicatorsWithDifferentDistribution() {
        // Test indicators work with different distribution families
        indicators = new BooleanParameter("false true false");
        glm.indicatorsInput.setValue(indicators, glm);
        glm.familyInput.setValue(DistributionFamily.GAMMA, glm);
        glm.sigmaInput.setValue(null, glm);
        glm.shapeInput.setValue(new RealParameter("2.0"), glm);
        
        glm.initAndValidate();
        
        assertEquals("Should use Gamma family", DistributionFamily.GAMMA, glm.getFamily());
        assertEquals("Should use canonical inverse link", LinkFunction.INVERSE, glm.getLink());
        
        // Linear predictor: η = 1.0 + 0*2.0*1.0 + 3.0*1.0 + 0*4.0*1.0 = 4.0
        // Mean with inverse link: μ = 1/4.0 = 0.25
        double expectedMean = 0.25;
        assertEquals("GLM with indicators and Gamma distribution", expectedMean, glm.getMean(), 1e-10);
    }

    @Test
    public void testSingleActiveIndicator() {
        // Test with only one indicator active
        indicators = new BooleanParameter("false true false");
        glm.indicatorsInput.setValue(indicators, glm);
        
        glm.initAndValidate();
        
        // Expected mean: 1.0 + 0*2.0*1.0 + 3.0*1.0 + 0*4.0*1.0 = 4.0
        double expectedMean = 4.0;
        assertEquals("GLM with single active indicator", expectedMean, glm.getMean(), 1e-10);
    }

    @Test
    public void testIndicatorsWithVaryingPredictors() {
        // Test indicators with different predictor values
        predictors = new RealParameter("2.0 -1.5 0.5");  // Different predictor values
        indicators = new BooleanParameter("true false true");
        
        glm.predictorsInput.setValue(predictors, glm);
        glm.indicatorsInput.setValue(indicators, glm);
        
        glm.initAndValidate();
        
        // Expected mean: 1.0 + 2.0*2.0 + 0*3.0*(-1.5) + 4.0*0.5 = 1.0 + 4.0 + 0 + 2.0 = 7.0
        double expectedMean = 7.0;
        assertEquals("GLM with indicators and varying predictors", expectedMean, glm.getMean(), 1e-10);
    }

    @Test
    public void testIndicatorsSwitchingBehavior() {
        // Test that changing indicators changes the result appropriately
        indicators = new BooleanParameter("true true true");
        glm.indicatorsInput.setValue(indicators, glm);
        glm.initAndValidate();
        
        double allActiveMean = glm.getMean();
        assertEquals("All variables active", 10.0, allActiveMean, 1e-10);
        
        // Now deactivate middle variable
        indicators.setValue(1, false);
        double partialActiveMean = glm.getMean();
        assertEquals("Middle variable deactivated", 7.0, partialActiveMean, 1e-10);
        
        // Deactivate all variables
        indicators.setValue(0, false);
        indicators.setValue(2, false);
        double interceptOnlyMean = glm.getMean();
        assertEquals("Only intercept active", 1.0, interceptOnlyMean, 1e-10);
    }

    @Test
    public void testIndicatorsWithStandardization() {
        // Test that indicators work with predictor standardization
        predictors = new RealParameter("10.0 20.0 30.0");  // Values that will be standardized
        indicators = new BooleanParameter("true false true");
        
        glm.predictorsInput.setValue(predictors, glm);
        glm.indicatorsInput.setValue(indicators, glm);
        glm.standardizeInput.setValue(true, glm);
        
        glm.initAndValidate();
        
        // After standardization, predictors should have mean ≈ 0
        // The exact value depends on standardization, but the logic should work
        double mean = glm.getMean();
        assertTrue("Should compute finite mean with standardization", Double.isFinite(mean));
        
        // Test that deactivated predictor doesn't contribute
        // Set all indicators to false and check we get intercept only
        indicators.setValue(0, false);
        indicators.setValue(2, false);
        double interceptOnly = glm.getMean();
        assertEquals("Should get intercept only when all indicators false", 1.0, interceptOnly, 1e-10);
    }
}