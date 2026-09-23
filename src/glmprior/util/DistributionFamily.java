package glmprior.util;

/**
 * Enumeration of distribution families supported by the GLM framework.
 * Each family has specific domain constraints and canonical link functions.
 */
public enum DistributionFamily {
    /**
     * Normal (Gaussian) distribution.
     * Domain: real numbers
     * Canonical link: Identity
     * Additional parameters: sigma (standard deviation)
 */
    NORMAL("Normal", "mu: (-inf, inf)", "sigma > 0"),
    
    /**
     * Poisson distribution.
     * Domain: lambda > 0
     * Canonical link: Log
     * Additional parameters: none
     */
    POISSON("Poisson", "lambda > 0", "none"),
    
    /**
     * Binomial distribution.
     * Domain: p in [0,1]
     * Canonical link: Logit
     * Additional parameters: n (number of trials)
     */
    BINOMIAL("Binomial", "p in [0,1]", "n >= 1 (trials)"),
    
    /**
     * Gamma distribution.
     * Domain: mu > 0
     * Canonical link: Inverse
     * Additional parameters: shape parameter
     */
    GAMMA("Gamma", "mu > 0", "shape > 0"),

    /**
     * Log-Normal distribution.
     * Domain: mu > 0 (the mean of the underlying normal is log(mu))
     * Canonical link: Log (since we model log(Y) ~ Normal)
     * Additional parameters: sigma (standard deviation on log scale)
     *
     * This models: log(Y) ~ Normal(eta, sigma) where eta is the linear predictor.
     * Equivalently: Y ~ LogNormal(eta, sigma)
     *
     * This matches the error structure in GLMPrior when using log link with error terms.
     */
    LOGNORMAL("LogNormal", "mu > 0", "sigma > 0 (on log scale)"),

    /**
     * Logit-Normal distribution.
     * Domain: mu in (0, 1) (the mean of the underlying normal is logit(mu))
     * Canonical link: Logit (since we model logit(Y) ~ Normal)
     * Additional parameters: sigma (standard deviation on logit scale)
     *
     * This models: logit(Y) ~ Normal(eta, sigma) where eta is the linear predictor.
     * The output Y is constrained to (0, 1).
     *
     * This matches the error structure in GLMPrior when using logit link with error terms.
     */
    LOGITNORMAL("LogitNormal", "mu in (0,1)", "sigma > 0 (on logit scale)");
    
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
            case LOGNORMAL:
                return LinkFunction.LOG;
            case LOGITNORMAL:
                return LinkFunction.LOGIT;
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
            case LOGNORMAL:
                // LogNormal naturally uses log link (models log(Y) ~ Normal)
                // Identity link would mean Y ~ LogNormal(mu, sigma) directly
                return link == LinkFunction.LOG || link == LinkFunction.IDENTITY;
            case LOGITNORMAL:
                // LogitNormal naturally uses logit link (models logit(Y) ~ Normal)
                // Identity link would mean Y ~ LogitNormal(mu, sigma) directly
                return link == LinkFunction.LOGIT || link == LinkFunction.IDENTITY;
            default:
                return false;
        }
    }
    
    /**
     * Returns true if the mean parameter mu is finite and inside the valid domain for this family.
     */
    public boolean isValidMean(double mu) {
        if (!Double.isFinite(mu)) {
            return false;
        }
        return switch (this) {
            case NORMAL -> true;                        // mu in (-inf, inf)
            case POISSON, GAMMA, LOGNORMAL -> mu > 0.0;                    // mu > 0
            case BINOMIAL -> mu >= 0.0 && mu <= 1.0;      // probability in [0,1]
            case LOGITNORMAL -> mu > 0.0 && mu < 1.0;        // logit undefined at 0 and 1
            default -> false;
        };
    }

    /**
     * Validates that the mean parameter mu is in the valid domain for this distribution family.
     * @param mu the mean parameter to validate
     * @throws IllegalArgumentException if mu is outside the valid domain
     */
    public void validateMean(double mu) {
        if (!isValidMean(mu)) {
            throw new IllegalArgumentException(getDisplayName() + " distribution mean must be in " +
                    getDomain() + ", got: " + mu);
        }
    }
}