package glmprior.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the LinkFunctions utility class.
 * Tests both forward and inverse transformations for all supported link functions.
 */
public class LinkFunctionsTest {

    private static final double TOLERANCE = 1e-12;
    private static final double LOOSE_TOLERANCE = 1e-8;

    @Test
    public void testIdentityLink() {
        // Test forward and inverse for identity link
        double[] testValues = {-10.0, -1.0, 0.0, 1.0, 10.0, 100.0};
        
        for (double mu : testValues) {
            double eta = LinkFunctions.apply(LinkFunction.IDENTITY, mu);
            assertEquals("Identity link forward", mu, eta, TOLERANCE);
            
            double muInverse = LinkFunctions.inverse(LinkFunction.IDENTITY, eta);
            assertEquals("Identity link inverse", mu, muInverse, TOLERANCE);
        }
    }

    @Test
    public void testLogLink() {
        double[] testValues = {0.001, 0.1, 1.0, 10.0, 100.0};
        
        for (double mu : testValues) {
            double eta = LinkFunctions.apply(LinkFunction.LOG, mu);
            assertEquals("Log link forward", Math.log(mu), eta, TOLERANCE);
            
            double muInverse = LinkFunctions.inverse(LinkFunction.LOG, eta);
            assertEquals("Log link inverse", mu, muInverse, TOLERANCE);
        }
    }

    @Test
    public void testLogitLink() {
        double[] testValues = {0.01, 0.1, 0.3, 0.5, 0.7, 0.9, 0.99};
        
        for (double mu : testValues) {
            double eta = LinkFunctions.apply(LinkFunction.LOGIT, mu);
            double expectedEta = Math.log(mu / (1.0 - mu));
            assertEquals("Logit link forward", expectedEta, eta, TOLERANCE);
            
            double muInverse = LinkFunctions.inverse(LinkFunction.LOGIT, eta);
            assertEquals("Logit link inverse", mu, muInverse, TOLERANCE);
        }
    }

    @Test
    public void testProbitLink() {
        double[] testValues = {0.01, 0.1, 0.3, 0.5, 0.7, 0.9, 0.99};
        
        for (double mu : testValues) {
            double eta = LinkFunctions.apply(LinkFunction.PROBIT, mu);
            double muInverse = LinkFunctions.inverse(LinkFunction.PROBIT, eta);
            assertEquals("Probit link round-trip", mu, muInverse, LOOSE_TOLERANCE);
        }
    }

    @Test
    public void testInverseLink() {
        double[] testValues = {0.1, 0.5, 1.0, 2.0, 10.0};
        
        for (double mu : testValues) {
            double eta = LinkFunctions.apply(LinkFunction.INVERSE, mu);
            assertEquals("Inverse link forward", 1.0 / mu, eta, TOLERANCE);
            
            double muInverse = LinkFunctions.inverse(LinkFunction.INVERSE, eta);
            assertEquals("Inverse link inverse", mu, muInverse, TOLERANCE);
        }
    }

    @Test
    public void testSqrtLink() {
        double[] testValues = {0.0, 0.1, 1.0, 4.0, 9.0, 25.0};
        
        for (double mu : testValues) {
            double eta = LinkFunctions.apply(LinkFunction.SQRT, mu);
            assertEquals("Sqrt link forward", Math.sqrt(mu), eta, TOLERANCE);
            
            double muInverse = LinkFunctions.inverse(LinkFunction.SQRT, eta);
            assertEquals("Sqrt link inverse", mu, muInverse, TOLERANCE);
        }
    }

    @Test
    public void testInverseSquaredLink() {
        double[] testValues = {0.1, 0.5, 1.0, 2.0, 4.0};
        
        for (double mu : testValues) {
            double eta = LinkFunctions.apply(LinkFunction.INVERSE_SQUARED, mu);
            assertEquals("Inverse squared link forward", 1.0 / (mu * mu), eta, TOLERANCE);
            
            double muInverse = LinkFunctions.inverse(LinkFunction.INVERSE_SQUARED, eta);
            assertEquals("Inverse squared link inverse", mu, muInverse, TOLERANCE);
        }
    }

    @Test
    public void testDomainValidation() {
        // Test domain validation for log link (mu > 0)
        try {
            LinkFunctions.apply(LinkFunction.LOG, 0.0);
            fail("Should have thrown exception for mu = 0 with log link");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Log link requires μ > 0"));
        }

        try {
            LinkFunctions.apply(LinkFunction.LOG, -1.0);
            fail("Should have thrown exception for mu < 0 with log link");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Log link requires μ > 0"));
        }

        // Test domain validation for logit link (0 < mu < 1)
        try {
            LinkFunctions.apply(LinkFunction.LOGIT, 0.0);
            fail("Should have thrown exception for mu = 0 with logit link");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Logit link requires μ ∈ (0,1)"));
        }

        try {
            LinkFunctions.apply(LinkFunction.LOGIT, 1.0);
            fail("Should have thrown exception for mu = 1 with logit link");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Logit link requires μ ∈ (0,1)"));
        }

        try {
            LinkFunctions.apply(LinkFunction.LOGIT, 1.5);
            fail("Should have thrown exception for mu > 1 with logit link");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Logit link requires μ ∈ (0,1)"));
        }
    }

    @Test
    public void testNumericalStability() {
        // Test extreme values for logit link
        double eta1 = LinkFunctions.inverse(LinkFunction.LOGIT, 500.0); // Very large eta
        assertTrue("Large eta should give probability close to 1", eta1 > 0.999);
        
        double eta2 = LinkFunctions.inverse(LinkFunction.LOGIT, -500.0); // Very small eta  
        assertTrue("Small eta should give probability close to 0", eta2 < 0.001);

        // Test extreme values for log link
        double mu1 = LinkFunctions.inverse(LinkFunction.LOG, 500.0); // Large eta
        assertTrue("Large eta should give large mu", mu1 > 1e200);
        assertTrue("Result should be finite", Double.isFinite(mu1));

        // Test near-zero values for log link
        double mu2 = LinkFunctions.inverse(LinkFunction.LOG, -500.0); // Very negative eta
        assertTrue("Very negative eta should give small positive mu", mu2 > 0 && mu2 < 1e-200);
    }

    @Test
    public void testInverseLinkDerivatives() {
        // Test identity link derivative
        assertEquals("Identity derivative", 1.0, 
                LinkFunctions.inverseLinkDerivative(LinkFunction.IDENTITY, 2.0), TOLERANCE);

        // Test log link derivative (should equal exp(eta))
        double eta = 1.0;
        assertEquals("Log derivative", Math.exp(eta), 
                LinkFunctions.inverseLinkDerivative(LinkFunction.LOG, eta), TOLERANCE);

        // Test logit link derivative at eta = 0 (should be 0.25)
        assertEquals("Logit derivative at eta=0", 0.25, 
                LinkFunctions.inverseLinkDerivative(LinkFunction.LOGIT, 0.0), TOLERANCE);

        // Test inverse link derivative
        eta = 2.0;
        assertEquals("Inverse derivative", -1.0 / (eta * eta), 
                LinkFunctions.inverseLinkDerivative(LinkFunction.INVERSE, eta), TOLERANCE);

        // Test sqrt link derivative
        eta = 3.0;
        assertEquals("Sqrt derivative", 2.0 * eta, 
                LinkFunctions.inverseLinkDerivative(LinkFunction.SQRT, eta), TOLERANCE);
    }

    @Test
    public void testInvalidInputs() {
        // Test infinite inputs
        try {
            LinkFunctions.apply(LinkFunction.IDENTITY, Double.POSITIVE_INFINITY);
            fail("Should reject infinite mu");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("finite"));
        }

        try {
            LinkFunctions.inverse(LinkFunction.IDENTITY, Double.NaN);
            fail("Should reject NaN eta");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("finite"));
        }

        // Test division by zero in inverse link
        try {
            LinkFunctions.inverse(LinkFunction.INVERSE, 0.0);
            fail("Should reject eta = 0 for inverse link");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Cannot compute 1/η"));
        }

        // Test negative eta for sqrt inverse
        try {
            LinkFunctions.inverse(LinkFunction.SQRT, -1.0);
            fail("Should reject negative eta for sqrt link");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Square root link requires η ≥ 0"));
        }
    }
}