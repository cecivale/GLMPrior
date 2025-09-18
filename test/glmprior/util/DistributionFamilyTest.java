package glmprior.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the DistributionFamily enum.
 * Tests canonical link functions and domain validation.
 */
public class DistributionFamilyTest {

    @Test
    public void testCanonicalLinks() {
        assertEquals("Normal canonical link", LinkFunction.IDENTITY, 
                DistributionFamily.NORMAL.getCanonicalLink());
        assertEquals("Poisson canonical link", LinkFunction.LOG, 
                DistributionFamily.POISSON.getCanonicalLink());
        assertEquals("Binomial canonical link", LinkFunction.LOGIT, 
                DistributionFamily.BINOMIAL.getCanonicalLink());
        assertEquals("Gamma canonical link", LinkFunction.INVERSE, 
                DistributionFamily.GAMMA.getCanonicalLink());
        assertEquals("Inverse Gaussian canonical link", LinkFunction.INVERSE_SQUARED, 
                DistributionFamily.INVERSE_GAUSSIAN.getCanonicalLink());
        assertEquals("Negative Binomial canonical link", LinkFunction.LOG, 
                DistributionFamily.NEGATIVE_BINOMIAL.getCanonicalLink());
    }

    @Test
    public void testValidLinkCombinations() {
        // Normal distribution
        assertTrue("Normal + Identity", DistributionFamily.NORMAL.isValidLink(LinkFunction.IDENTITY));
        assertTrue("Normal + Log", DistributionFamily.NORMAL.isValidLink(LinkFunction.LOG));
        assertFalse("Normal + Logit", DistributionFamily.NORMAL.isValidLink(LinkFunction.LOGIT));

        // Poisson distribution
        assertTrue("Poisson + Log", DistributionFamily.POISSON.isValidLink(LinkFunction.LOG));
        assertTrue("Poisson + Identity", DistributionFamily.POISSON.isValidLink(LinkFunction.IDENTITY));
        assertTrue("Poisson + Sqrt", DistributionFamily.POISSON.isValidLink(LinkFunction.SQRT));
        assertFalse("Poisson + Logit", DistributionFamily.POISSON.isValidLink(LinkFunction.LOGIT));

        // Binomial distribution
        assertTrue("Binomial + Logit", DistributionFamily.BINOMIAL.isValidLink(LinkFunction.LOGIT));
        assertTrue("Binomial + Probit", DistributionFamily.BINOMIAL.isValidLink(LinkFunction.PROBIT));
        assertTrue("Binomial + Identity", DistributionFamily.BINOMIAL.isValidLink(LinkFunction.IDENTITY));
        assertFalse("Binomial + Log", DistributionFamily.BINOMIAL.isValidLink(LinkFunction.LOG));

        // Gamma distribution
        assertTrue("Gamma + Inverse", DistributionFamily.GAMMA.isValidLink(LinkFunction.INVERSE));
        assertTrue("Gamma + Log", DistributionFamily.GAMMA.isValidLink(LinkFunction.LOG));
        assertTrue("Gamma + Identity", DistributionFamily.GAMMA.isValidLink(LinkFunction.IDENTITY));
        assertFalse("Gamma + Logit", DistributionFamily.GAMMA.isValidLink(LinkFunction.LOGIT));

        // Inverse Gaussian distribution
        assertTrue("InverseGaussian + InverseSquared", 
                DistributionFamily.INVERSE_GAUSSIAN.isValidLink(LinkFunction.INVERSE_SQUARED));
        assertTrue("InverseGaussian + Log", 
                DistributionFamily.INVERSE_GAUSSIAN.isValidLink(LinkFunction.LOG));
        assertTrue("InverseGaussian + Inverse", 
                DistributionFamily.INVERSE_GAUSSIAN.isValidLink(LinkFunction.INVERSE));
        assertFalse("InverseGaussian + Logit", 
                DistributionFamily.INVERSE_GAUSSIAN.isValidLink(LinkFunction.LOGIT));

        // Negative Binomial distribution
        assertTrue("NegativeBinomial + Log", 
                DistributionFamily.NEGATIVE_BINOMIAL.isValidLink(LinkFunction.LOG));
        assertTrue("NegativeBinomial + Identity", 
                DistributionFamily.NEGATIVE_BINOMIAL.isValidLink(LinkFunction.IDENTITY));
        assertTrue("NegativeBinomial + Sqrt", 
                DistributionFamily.NEGATIVE_BINOMIAL.isValidLink(LinkFunction.SQRT));
        assertFalse("NegativeBinomial + Logit", 
                DistributionFamily.NEGATIVE_BINOMIAL.isValidLink(LinkFunction.LOGIT));
    }

    @Test
    public void testMeanValidation() {
        // Normal distribution - any finite value should be valid
        DistributionFamily.NORMAL.validateMean(-100.0);
        DistributionFamily.NORMAL.validateMean(0.0);
        DistributionFamily.NORMAL.validateMean(100.0);
        
        try {
            DistributionFamily.NORMAL.validateMean(Double.POSITIVE_INFINITY);
            fail("Should reject infinite mean for Normal");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("finite"));
        }

        try {
            DistributionFamily.NORMAL.validateMean(Double.NaN);
            fail("Should reject NaN mean for Normal");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("finite"));
        }

        // Poisson distribution - must be > 0
        DistributionFamily.POISSON.validateMean(0.1);
        DistributionFamily.POISSON.validateMean(10.0);
        
        try {
            DistributionFamily.POISSON.validateMean(0.0);
            fail("Should reject zero mean for Poisson");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be > 0"));
        }

        try {
            DistributionFamily.POISSON.validateMean(-1.0);
            fail("Should reject negative mean for Poisson");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be > 0"));
        }

        // Binomial distribution - must be in [0,1]
        DistributionFamily.BINOMIAL.validateMean(0.0);
        DistributionFamily.BINOMIAL.validateMean(0.5);
        DistributionFamily.BINOMIAL.validateMean(1.0);
        
        try {
            DistributionFamily.BINOMIAL.validateMean(-0.1);
            fail("Should reject negative probability for Binomial");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be in [0,1]"));
        }

        try {
            DistributionFamily.BINOMIAL.validateMean(1.1);
            fail("Should reject probability > 1 for Binomial");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be in [0,1]"));
        }

        // Gamma distribution - must be > 0
        DistributionFamily.GAMMA.validateMean(0.1);
        DistributionFamily.GAMMA.validateMean(10.0);
        
        try {
            DistributionFamily.GAMMA.validateMean(0.0);
            fail("Should reject zero mean for Gamma");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be > 0"));
        }

        try {
            DistributionFamily.GAMMA.validateMean(-1.0);
            fail("Should reject negative mean for Gamma");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be > 0"));
        }

        // Inverse Gaussian distribution - must be > 0
        DistributionFamily.INVERSE_GAUSSIAN.validateMean(0.1);
        DistributionFamily.INVERSE_GAUSSIAN.validateMean(10.0);
        
        try {
            DistributionFamily.INVERSE_GAUSSIAN.validateMean(0.0);
            fail("Should reject zero mean for Inverse Gaussian");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be > 0"));
        }

        // Negative Binomial distribution - must be > 0
        DistributionFamily.NEGATIVE_BINOMIAL.validateMean(0.1);
        DistributionFamily.NEGATIVE_BINOMIAL.validateMean(10.0);
        
        try {
            DistributionFamily.NEGATIVE_BINOMIAL.validateMean(0.0);
            fail("Should reject zero mean for Negative Binomial");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be > 0"));
        }
    }

    @Test
    public void testDisplayProperties() {
        // Test that all families have non-empty display properties
        for (DistributionFamily family : DistributionFamily.values()) {
            assertNotNull("Display name should not be null", family.getDisplayName());
            assertFalse("Display name should not be empty", family.getDisplayName().isEmpty());
            
            assertNotNull("Domain should not be null", family.getDomain());
            assertFalse("Domain should not be empty", family.getDomain().isEmpty());
            
            assertNotNull("Additional parameters should not be null", family.getAdditionalParameters());
            assertFalse("Additional parameters should not be empty", family.getAdditionalParameters().isEmpty());
        }

        // Test specific display names
        assertEquals("Normal display name", "Normal", DistributionFamily.NORMAL.getDisplayName());
        assertEquals("Poisson display name", "Poisson", DistributionFamily.POISSON.getDisplayName());
        assertEquals("Binomial display name", "Binomial", DistributionFamily.BINOMIAL.getDisplayName());
        assertEquals("Gamma display name", "Gamma", DistributionFamily.GAMMA.getDisplayName());
        assertEquals("Inverse Gaussian display name", "Inverse Gaussian", 
                DistributionFamily.INVERSE_GAUSSIAN.getDisplayName());
        assertEquals("Negative Binomial display name", "Negative Binomial", 
                DistributionFamily.NEGATIVE_BINOMIAL.getDisplayName());
    }

    @Test
    public void testDomainStrings() {
        // Test that domain strings match expectations
        assertEquals("Normal domain", "μ ∈ ℝ", DistributionFamily.NORMAL.getDomain());
        assertEquals("Poisson domain", "λ > 0", DistributionFamily.POISSON.getDomain());
        assertEquals("Binomial domain", "p ∈ [0,1]", DistributionFamily.BINOMIAL.getDomain());
        assertEquals("Gamma domain", "μ > 0", DistributionFamily.GAMMA.getDomain());
        assertEquals("Inverse Gaussian domain", "μ > 0", DistributionFamily.INVERSE_GAUSSIAN.getDomain());
        assertEquals("Negative Binomial domain", "μ > 0", DistributionFamily.NEGATIVE_BINOMIAL.getDomain());
    }

    @Test
    public void testAdditionalParameterStrings() {
        // Test additional parameter requirements
        assertEquals("Normal additional params", "σ > 0", DistributionFamily.NORMAL.getAdditionalParameters());
        assertEquals("Poisson additional params", "none", DistributionFamily.POISSON.getAdditionalParameters());
        assertEquals("Binomial additional params", "n ≥ 1 (trials)", DistributionFamily.BINOMIAL.getAdditionalParameters());
        assertEquals("Gamma additional params", "shape > 0", DistributionFamily.GAMMA.getAdditionalParameters());
        assertEquals("Inverse Gaussian additional params", "λ > 0 (shape)", 
                DistributionFamily.INVERSE_GAUSSIAN.getAdditionalParameters());
        assertEquals("Negative Binomial additional params", "size > 0", 
                DistributionFamily.NEGATIVE_BINOMIAL.getAdditionalParameters());
    }
}