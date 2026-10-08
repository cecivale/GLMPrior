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
    }

    @Test
    public void testIsValidMean() {
        assertTrue(DistributionFamily.NORMAL.isValidMean(-100.0));
        assertFalse(DistributionFamily.NORMAL.isValidMean(Double.POSITIVE_INFINITY));
        assertFalse(DistributionFamily.NORMAL.isValidMean(Double.NaN));

        assertTrue(DistributionFamily.POISSON.isValidMean(0.1));
        assertFalse(DistributionFamily.POISSON.isValidMean(0.0));
        assertFalse(DistributionFamily.POISSON.isValidMean(-1.0));

        assertTrue(DistributionFamily.BINOMIAL.isValidMean(0.0));
        assertTrue(DistributionFamily.BINOMIAL.isValidMean(1.0));
        assertFalse(DistributionFamily.BINOMIAL.isValidMean(-0.1));
        assertFalse(DistributionFamily.BINOMIAL.isValidMean(1.1));

        assertTrue(DistributionFamily.GAMMA.isValidMean(0.1));
        assertFalse(DistributionFamily.GAMMA.isValidMean(0.0));
        assertFalse(DistributionFamily.GAMMA.isValidMean(-1.0));
    }
}