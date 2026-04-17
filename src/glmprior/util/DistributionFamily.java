package glmprior.util;

/**
 * Enumeration of distribution families supported by the GLM framework.
 * Each family has specific domain constraints and canonical link functions.
 */
public enum DistributionFamily {
    /**
     * Normal (Gaussian) distribution.
     * Domain: μ ∈ ℝ
     * Canonical link: Identity
     * Additional parameters: σ (standard deviation)
     */
    NORMAL("Normal", "μ ∈ ℝ", "σ > 0"),
    
    /**
     * Poisson distribution.
     * Domain: λ > 0
     * Canonical link: Log
     * Additional parameters: none
     */
    POISSON("Poisson", "λ > 0", "none"),
    
    /**
     * Binomial distribution.
     * Domain: p ∈ [0,1]
     * Canonical link: Logit
     * Additional parameters: n (number of trials)
     */
    BINOMIAL("Binomial", "p ∈ [0,1]", "n ≥ 1 (trials)"),
    
    /**
     * Gamma distribution.
     * Domain: μ > 0
     * Canonical link: Inverse
     * Additional parameters: shape parameter
     */
    GAMMA("Gamma", "μ > 0", "shape > 0"),
    
    /**
     * Note: Additional distribution families like Inverse Gaussian and Negative Binomial
     * can be added in the future with custom implementations.
     */;
    
    private final String displayName;
    private final String domain;
    private final String additionalParams;
    
    DistributionFamily(String displayName, String domain, String additionalParams) {
        this.displayName = displayName;
        this.domain = domain;
        this.additionalParams = additionalParams;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public String getAdditionalParameters() {
        return additionalParams;
    }
    
    /**
     * Returns the canonical link function for this distribution family.
     */
    public LinkFunction getCanonicalLink() {
        switch (this) {
            case NORMAL:
                return LinkFunction.IDENTITY;
            case POISSON:
                return LinkFunction.LOG;
            case BINOMIAL:
                return LinkFunction.LOGIT;
            case GAMMA:
                return LinkFunction.INVERSE;
            default:
                throw new IllegalStateException("No canonical link defined for " + this);
        }
    }
    
    /**
     * Returns true if the given link function is valid for this distribution family.
     */
    public boolean isValidLink(LinkFunction link) {
        switch (this) {
            case NORMAL:
                return link == LinkFunction.IDENTITY || link == LinkFunction.LOG;
            case POISSON:
                return link == LinkFunction.LOG || link == LinkFunction.IDENTITY || link == LinkFunction.SQRT;
            case BINOMIAL:
                return link == LinkFunction.LOGIT || link == LinkFunction.PROBIT || link == LinkFunction.IDENTITY;
            case GAMMA:
                return link == LinkFunction.INVERSE || link == LinkFunction.LOG || link == LinkFunction.IDENTITY;
            default:
                return false;
        }
    }
    
    /**
     * Validates that the mean parameter μ is in the valid domain for this distribution family.
     * @param mu the mean parameter to validate
     * @throws IllegalArgumentException if μ is outside the valid domain
     */
    public void validateMean(double mu) {
        switch (this) {
            case NORMAL:
                // μ ∈ ℝ - no constraints
                if (!Double.isFinite(mu)) {
                    throw new IllegalArgumentException("Normal distribution mean must be finite, got: " + mu);
                }
                break;
            case POISSON:
            case GAMMA:
                // μ > 0
                if (mu <= 0.0 || !Double.isFinite(mu)) {
                    throw new IllegalArgumentException(getDisplayName() + " distribution mean must be > 0, got: " + mu);
                }
                break;
            case BINOMIAL:
                // μ ∈ [0,1] (interpreted as probability p)
                if (mu < 0.0 || mu > 1.0 || !Double.isFinite(mu)) {
                    throw new IllegalArgumentException("Binomial distribution probability must be in [0,1], got: " + mu);
                }
                break;
            default:
                throw new IllegalStateException("Domain validation not implemented for " + this);
        }
    }
}