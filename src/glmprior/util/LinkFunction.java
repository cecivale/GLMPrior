package glmprior.util;

/**
 * Enumeration of link functions supported by the GLM framework.
 * Link functions map the linear predictor eta to the mean parameter mu of the distribution.
 * 
 * The relationship is: eta = g(mu), where g is the link function.
 * The inverse link gives: mu = g^(-1)(eta).
 */
public enum LinkFunction {
    /**
     * Identity link: g(mu) = mu
     * Inverse: mu = eta
     * Domain: mu in (-inf, inf)
     */
    IDENTITY("Identity", "g(mu) = mu", "mu in (-inf, inf)"),
    
    /**
     * Log link: g(mu) = log(mu)
     * Inverse: mu = exp(eta)
     * Domain: mu > 0
     */
    LOG("Log", "g(mu) = log(mu)", "mu > 0"),
    
    /**
     * Logit link: g(mu) = log(mu/(1-mu))
     * Inverse: mu = exp(eta)/(1+exp(eta))
     * Domain: mu in (0,1)
     */
    LOGIT("Logit", "g(mu) = log(mu/(1-mu))", "mu in (0,1)"),
    
    /**
     * Probit link: g(mu) = Phi^(-1)(mu)
     * Inverse: mu = Phi(eta)
     * Domain: mu in (0,1)
     */
    PROBIT("Probit", "g(mu) = Phi^(-1)(mu)", "mu in (0,1)"),
    
    /**
     * Inverse link: g(mu) = 1/mu
     * Inverse: mu = 1/eta
     * Domain: mu > 0, eta != 0
     */
    INVERSE("Inverse", "g(mu) = 1/mu", "mu > 0"),
    
    /**
     * Square root link: g(mu) = sqrt(mu)
     * Inverse: mu = eta^2
     * Domain: mu >= 0
     */
    SQRT("Square Root", "g(mu) = sqrt(mu)", "mu >= 0"),
    
    /**
     * Note: Additional link functions can be added as needed for future distributions.
     */;
    
    private final String displayName;
    private final String formula;
    private final String domain;
    
    LinkFunction(String displayName, String formula, String domain) {
        this.displayName = displayName;
        this.formula = formula;
        this.domain = domain;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getFormula() {
        return formula;
    }
    
    public String getDomain() {
        return domain;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}