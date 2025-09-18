package glmprior.util;

/**
 * Enumeration of link functions supported by the GLM framework.
 * Link functions map the linear predictor η to the mean parameter μ of the distribution.
 * 
 * The relationship is: η = g(μ), where g is the link function.
 * The inverse link gives: μ = g^(-1)(η).
 */
public enum LinkFunction {
    /**
     * Identity link: g(μ) = μ
     * Inverse: μ = η
     * Domain: μ ∈ ℝ
     */
    IDENTITY("Identity", "g(μ) = μ", "μ ∈ ℝ"),
    
    /**
     * Log link: g(μ) = log(μ)
     * Inverse: μ = exp(η)
     * Domain: μ > 0
     */
    LOG("Log", "g(μ) = log(μ)", "μ > 0"),
    
    /**
     * Logit link: g(μ) = log(μ/(1-μ))
     * Inverse: μ = exp(η)/(1+exp(η))
     * Domain: μ ∈ (0,1)
     */
    LOGIT("Logit", "g(μ) = log(μ/(1-μ))", "μ ∈ (0,1)"),
    
    /**
     * Probit link: g(μ) = Φ^(-1)(μ)
     * Inverse: μ = Φ(η)
     * Domain: μ ∈ (0,1)
     */
    PROBIT("Probit", "g(μ) = Φ⁻¹(μ)", "μ ∈ (0,1)"),
    
    /**
     * Inverse link: g(μ) = 1/μ
     * Inverse: μ = 1/η
     * Domain: μ > 0, η ≠ 0
     */
    INVERSE("Inverse", "g(μ) = 1/μ", "μ > 0"),
    
    /**
     * Square root link: g(μ) = √μ
     * Inverse: μ = η²
     * Domain: μ ≥ 0
     */
    SQRT("Square Root", "g(μ) = √μ", "μ ≥ 0"),
    
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