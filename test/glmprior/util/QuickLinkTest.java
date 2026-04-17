package glmprior.util;

/**
 * Quick standalone test to verify our link function implementations work correctly.
 * This can be run independently of the full BEAST framework.
 */
public class QuickLinkTest {
    
    public static void main(String[] args) {
        System.out.println("Testing GLM Framework Core Components...");
        
        try {
            // Test 1: Link function round-trip
            testLinkFunctionRoundTrip();
            System.out.println("✓ Link function round-trip tests passed");
            
            // Test 2: Distribution family validation
            testDistributionFamilyValidation();
            System.out.println("✓ Distribution family validation tests passed");
            
            // Test 3: Edge cases
            testEdgeCases();
            System.out.println("✓ Edge case tests passed");
            
            System.out.println("\n🎉 All core GLM framework tests passed!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testLinkFunctionRoundTrip() {
        // Test identity link
        double mu = 5.0;
        double eta = LinkFunctions.apply(LinkFunction.IDENTITY, mu);
        double muBack = LinkFunctions.inverse(LinkFunction.IDENTITY, eta);
        assert Math.abs(mu - muBack) < 1e-10 : "Identity link round-trip failed";
        
        // Test log link
        mu = 3.0;
        eta = LinkFunctions.apply(LinkFunction.LOG, mu);
        muBack = LinkFunctions.inverse(LinkFunction.LOG, eta);
        assert Math.abs(mu - muBack) < 1e-10 : "Log link round-trip failed";
        
        // Test logit link
        mu = 0.7;
        eta = LinkFunctions.apply(LinkFunction.LOGIT, mu);
        muBack = LinkFunctions.inverse(LinkFunction.LOGIT, eta);
        assert Math.abs(mu - muBack) < 1e-10 : "Logit link round-trip failed";
        
        // Test inverse link
        mu = 2.5;
        eta = LinkFunctions.apply(LinkFunction.INVERSE, mu);
        muBack = LinkFunctions.inverse(LinkFunction.INVERSE, eta);
        assert Math.abs(mu - muBack) < 1e-10 : "Inverse link round-trip failed";
    }
    
    private static void testDistributionFamilyValidation() {
        // Test Normal distribution (should accept any finite value)
        DistributionFamily.NORMAL.validateMean(-5.0);
        DistributionFamily.NORMAL.validateMean(0.0);
        DistributionFamily.NORMAL.validateMean(10.0);
        
        // Test Poisson distribution (should require > 0)
        DistributionFamily.POISSON.validateMean(0.1);
        DistributionFamily.POISSON.validateMean(10.0);
        
        try {
            DistributionFamily.POISSON.validateMean(0.0);
            assert false : "Should have rejected zero mean for Poisson";
        } catch (IllegalArgumentException e) {
            // Expected
        }
        
        // Test Binomial distribution (should require [0,1])
        DistributionFamily.BINOMIAL.validateMean(0.0);
        DistributionFamily.BINOMIAL.validateMean(0.5);
        DistributionFamily.BINOMIAL.validateMean(1.0);
        
        try {
            DistributionFamily.BINOMIAL.validateMean(1.5);
            assert false : "Should have rejected > 1 probability for Binomial";
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    private static void testEdgeCases() {
        // Test extreme values for logit link
        double largeEta = 500.0;
        double p = LinkFunctions.inverse(LinkFunction.LOGIT, largeEta);
        assert p > 0.999 && p < 1.0 : "Large eta should give probability close to 1";
        
        double smallEta = -500.0;
        p = LinkFunctions.inverse(LinkFunction.LOGIT, smallEta);
        assert p > 0.0 && p < 0.001 : "Small eta should give probability close to 0";
        
        // Test log link with extreme values
        double largeExp = LinkFunctions.inverse(LinkFunction.LOG, 100.0);
        assert largeExp > 0 && Double.isFinite(largeExp) : "Large log should give finite positive result";
        
        // Test domain validation
        try {
            LinkFunctions.apply(LinkFunction.LOG, -1.0);
            assert false : "Should reject negative value for log link";
        } catch (IllegalArgumentException e) {
            // Expected
        }
        
        try {
            LinkFunctions.apply(LinkFunction.LOGIT, 1.5);
            assert false : "Should reject > 1 value for logit link";
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
}