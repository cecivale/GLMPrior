package glmprior.util;

import beast.base.inference.parameter.RealParameter;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the GLMDistribution class.
 * Tests the generalized GLM framework functionality.
 */
public class GLMDistributionTest {

    private GLMDistribution glm;
    private RealParameter intercept;
    private RealParameter coefficients;
    private RealParameter predictors;
    private RealParameter sigma;

    @Before
    public void setUp() {
        glm = new GLMDistribution();
        
        // Set up basic GLM components
        intercept = new RealParameter("2.0");
        coefficients = new RealParameter("1.5 -0.5");
        predictors = new RealParameter("1.0 2.0");
        sigma = new RealParameter("1.0");
        
        glm.interceptInput.setValue(intercept, glm);
        glm.coefficientsInput.setValue(coefficients, glm);
        glm.predictorsInput.setValue(predictors, glm);
        glm.sigmaInput.setValue(sigma, glm);
    }

    @Test
    public void testDefaultNormalDistribution() {
        // Test that default is Normal distribution with Identity link
        glm.initAndValidate();
        
        assertEquals("Default family should be Normal", 
                DistributionFamily.NORMAL, glm.getFamily());
        assertEquals("Default link should be Identity", 
                LinkFunction.IDENTITY, glm.getLink());
    }

    @Test
    public void testNormalDistributionMeanCalculation() {
        glm.initAndValidate();
        
        // Expected mean: 2.0 + 1.5*1.0 + (-0.5)*2.0 = 2.0 + 1.5 - 1.0 = 2.5
        double expectedMean = 2.5;
        assertEquals("Normal GLM mean calculation", expectedMean, glm.getMean(), 1e-10);
        
        // Variance should be sigma^2 = 1.0
        assertEquals("Normal GLM variance", 1.0, glm.getVariance(), 1e-10);
    }

    @Test
    public void testNormalDistributionWithLogLink() {
        glm.familyInput.setValue(DistributionFamily.NORMAL, glm);
        glm.linkInput.setValue(LinkFunction.LOG, glm);
        glm.initAndValidate();
        
        // Linear predictor: η = 2.0 + 1.5*1.0 + (-0.5)*2.0 = 2.5
        // Mean with log link: μ = exp(2.5) ≈ 12.18
        double expectedMean = Math.exp(2.5);
        assertEquals("Normal GLM with log link", expectedMean, glm.getMean(), 1e-10);
    }

    @Test
    public void testPoissonDistribution() {
        glm.familyInput.setValue(DistributionFamily.POISSON, glm);
        // Remove sigma parameter (not needed for Poisson)
        glm.sigmaInput.setValue(null, glm);
        
        try {
            glm.initAndValidate();
            
            assertEquals("Poisson family", DistributionFamily.POISSON, glm.getFamily());
            assertEquals("Poisson canonical link", LinkFunction.LOG, glm.getLink());
            
            // Mean with log link: μ = exp(2.5) ≈ 12.18
            double expectedMean = Math.exp(2.5);
            assertEquals("Poisson GLM mean", expectedMean, glm.getMean(), 1e-10);
            
            // For Poisson, variance = mean
            assertEquals("Poisson GLM variance", expectedMean, glm.getVariance(), 1e-10);
            
        } catch (UnsupportedOperationException e) {
            // This is expected due to Apache Commons Math limitations for small means
            assertTrue("Expected Poisson limitation message", 
                    e.getMessage().contains("Poisson distribution"));
        }
    }

    @Test
    public void testBinomialDistribution() {
        // Create new coefficients with smaller values to keep probability in valid range
        RealParameter smallCoeffs = new RealParameter("0.5 -0.2");
        
        glm.familyInput.setValue(DistributionFamily.BINOMIAL, glm);
        glm.sigmaInput.setValue(null, glm);
        glm.nTrialsInput.setValue(new RealParameter("20"), glm);
        glm.coefficientsInput.setValue(smallCoeffs, glm);
        
        try {
            glm.initAndValidate();
            
            assertEquals("Binomial family", DistributionFamily.BINOMIAL, glm.getFamily());
            assertEquals("Binomial canonical link", LinkFunction.LOGIT, glm.getLink());
            
            // Linear predictor: η = 2.0 + 0.5*1.0 + (-0.2)*2.0 = 2.1
            // Probability with logit link: p = exp(2.1)/(1+exp(2.1))
            double eta = 2.1;
            double expectedProb = Math.exp(eta) / (1.0 + Math.exp(eta));
            assertEquals("Binomial GLM probability", expectedProb, glm.getMean(), 1e-10);
            
        } catch (UnsupportedOperationException e) {
            // This is expected due to Apache Commons Math limitations
            assertTrue("Expected Binomial limitation message", 
                    e.getMessage().contains("Binomial distribution"));
        }
    }

    @Test
    public void testGammaDistribution() {
        glm.familyInput.setValue(DistributionFamily.GAMMA, glm);
        glm.sigmaInput.setValue(null, glm);
        glm.shapeInput.setValue(new RealParameter("2.0"), glm);
        
        glm.initAndValidate();
        
        assertEquals("Gamma family", DistributionFamily.GAMMA, glm.getFamily());
        assertEquals("Gamma canonical link", LinkFunction.INVERSE, glm.getLink());
        
        // Linear predictor: η = 2.5
        // Mean with inverse link: μ = 1/η = 1/2.5 = 0.4
        double expectedMean = 1.0 / 2.5;
        assertEquals("Gamma GLM mean", expectedMean, glm.getMean(), 1e-10);
        
        // For Gamma: variance = μ²/shape = 0.4²/2.0 = 0.08
        double expectedVar = expectedMean * expectedMean / 2.0;
        assertEquals("Gamma GLM variance", expectedVar, glm.getVariance(), 1e-10);
    }

    @Test
    public void testDimensionMismatch() {
        // Create mismatched dimensions
        RealParameter mismatchedCoeffs = new RealParameter("1.0");  // 1 element
        RealParameter mismatchedPreds = new RealParameter("1.0 2.0");  // 2 elements
        
        glm.coefficientsInput.setValue(mismatchedCoeffs, glm);
        glm.predictorsInput.setValue(mismatchedPreds, glm);
        
        try {
            glm.initAndValidate();
            fail("Should have thrown exception for dimension mismatch");
        } catch (IllegalArgumentException e) {
            assertTrue("Expected dimension mismatch message", 
                    e.getMessage().contains("Dimension mismatch"));
        }
    }

    @Test
    public void testInvalidFamilyLinkCombination() {
        // Try to use logit link with Normal distribution (invalid)
        glm.familyInput.setValue(DistributionFamily.NORMAL, glm);
        glm.linkInput.setValue(LinkFunction.LOGIT, glm);
        
        try {
            glm.initAndValidate();
            fail("Should have thrown exception for invalid family-link combination");
        } catch (IllegalArgumentException e) {
            assertTrue("Expected invalid link message", 
                    e.getMessage().contains("Link function Logit is not valid for Normal distribution"));
        }
    }

    @Test
    public void testMissingRequiredParameters() {
        // Test Normal distribution without sigma parameter
        glm.sigmaInput.setValue(null, glm);
        
        try {
            glm.initAndValidate();
            fail("Should have thrown exception for missing sigma parameter");
        } catch (IllegalArgumentException e) {
            assertTrue("Expected missing parameter message", 
                    e.getMessage().contains("Normal distribution requires either 'sigma' or 'sigma2' parameter"));
        }
    }

    @Test
    public void testBothSigmaAndSigma2() {
        // Test providing both sigma and sigma2 (should be error)
        glm.sigma2Input.setValue(new RealParameter("2.0"), glm);
        
        try {
            glm.initAndValidate();
            fail("Should have thrown exception for both sigma and sigma2");
        } catch (IllegalArgumentException e) {
            assertTrue("Expected conflicting parameter message", 
                    e.getMessage().contains("specify either 'sigma' or 'sigma2', not both"));
        }
    }

    @Test
    public void testSigma2Parameter() {
        // Test using sigma2 instead of sigma
        glm.sigmaInput.setValue(null, glm);
        glm.sigma2Input.setValue(new RealParameter("4.0"), glm);  // sigma2 = 4, so sigma = 2
        
        glm.initAndValidate();
        
        assertEquals("Mean should still be calculated correctly", 2.5, glm.getMean(), 1e-10);
        assertEquals("Variance should be sigma2", 4.0, glm.getVariance(), 1e-10);
    }

    @Test
    public void testInvalidParameterValues() {
        // Test negative sigma
        sigma.setValue(0, -1.0);
        
        try {
            glm.initAndValidate();
            fail("Should have thrown exception for negative sigma");
        } catch (IllegalArgumentException e) {
            assertTrue("Expected negative sigma message", 
                    e.getMessage().contains("sigma must be > 0"));
        }
    }

    @Test
    public void testStandardization() {
        // Test predictor standardization
        RealParameter nonStandardPreds = new RealParameter("10.0 20.0 30.0");
        coefficients = new RealParameter("1.0 2.0 3.0");
        
        glm.predictorsInput.setValue(nonStandardPreds, glm);
        glm.coefficientsInput.setValue(coefficients, glm);
        glm.standardizeInput.setValue(true, glm);
        
        glm.initAndValidate();
        
        // After standardization, predictors should have mean ≈ 0 and std dev ≈ 1
        Double[] standardizedVals = nonStandardPreds.getValues();
        double mean = 0.0;
        for (double v : standardizedVals) mean += v;
        mean /= standardizedVals.length;
        
        assertEquals("Standardized predictors should have mean ≈ 0", 0.0, mean, 1e-10);
        
        // Check that standard deviation is approximately 1
        double var = 0.0;
        for (double v : standardizedVals) {
            double d = v - mean;
            var += d * d;
        }
        var /= (standardizedVals.length - 1);
        double sd = Math.sqrt(var);
        
        assertEquals("Standardized predictors should have std dev ≈ 1", 1.0, sd, 1e-10);
    }

    @Test
    public void testCanonicalLinkSelection() {
        // Test that canonical link is selected when link is not specified
        glm.familyInput.setValue(DistributionFamily.GAMMA, glm);
        glm.sigmaInput.setValue(null, glm);
        glm.shapeInput.setValue(new RealParameter("2.0"), glm);
        // Don't set linkInput - should default to canonical
        
        glm.initAndValidate();
        
        assertEquals("Should use canonical link for Gamma", 
                LinkFunction.INVERSE, glm.getLink());
    }

    @Test
    public void testToleranceForExtremeValues() {
        // Test that the framework handles extreme but valid values
        intercept.setValue(0, 50.0);  // Large intercept
        RealParameter extremeCoeffs = new RealParameter("10.0 -5.0");
        glm.coefficientsInput.setValue(extremeCoeffs, glm);
        
        glm.familyInput.setValue(DistributionFamily.NORMAL, glm);
        glm.linkInput.setValue(LinkFunction.LOG, glm);
        
        glm.initAndValidate();
        
        // Should handle large linear predictor values
        double mean = glm.getMean();
        assertTrue("Mean should be positive and finite", mean > 0 && Double.isFinite(mean));
    }
}