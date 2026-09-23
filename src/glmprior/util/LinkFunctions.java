package glmprior.util;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.special.Erf;

/**
 * Utility class for applying link functions and their inverses in GLM computations.
 * 
 * Link functions map the mean parameter mu to the linear predictor eta via g(mu) = eta.
 * Inverse link functions map the linear predictor eta back to the mean mu via mu = g^(-1)(eta).
 */
public class LinkFunctions {
    
    // Standard normal distribution for probit calculations
    private static final NormalDistribution STANDARD_NORMAL = new NormalDistribution(0.0, 1.0);
    
    // Constants for numerical stability
    private static final double LOG_EPSILON = 1e-15;
    private static final double LOGIT_EPSILON = 1e-15;
    private static final double MAX_EXP_ARG = 700.0; // exp(700) is approx 1e304, close to Double.MAX_VALUE
    
    /**
     * Apply the link function g(mu) to get the linear predictor eta.
     * 
     * @param link the link function to apply
     * @param mu the mean parameter mu
     * @return the linear predictor eta = g(mu)
     * @throws IllegalArgumentException if mu is outside the valid domain for the link function
     */
    public static double apply(LinkFunction link, double mu) {
        validateDomain(link, mu);
        
        switch (link) {
            case IDENTITY:
                return mu;
                
            case LOG:
                return Math.log(Math.max(mu, LOG_EPSILON));
                
            case LOGIT:
                // Clamp to avoid numerical issues
                double p = Math.max(LOGIT_EPSILON, Math.min(1.0 - LOGIT_EPSILON, mu));
                return Math.log(p / (1.0 - p));
                
            case PROBIT:
                // Clamp to avoid numerical issues
                double pProbit = Math.max(LOGIT_EPSILON, Math.min(1.0 - LOGIT_EPSILON, mu));
                return STANDARD_NORMAL.inverseCumulativeProbability(pProbit);
                
            case INVERSE:
                return 1.0 / mu;
                
            case SQRT:
                return Math.sqrt(mu);
                
                
            default:
                throw new IllegalArgumentException("Unsupported link function: " + link);
        }
    }
    
    /**
     * Apply the inverse link function g^(-1)(eta) to get the mean parameter mu.
     * 
     * @param link the link function whose inverse to apply
     * @param eta the linear predictor eta
     * @return the mean parameter mu = g^(-1)(eta)
     * @throws IllegalArgumentException if the computation would result in an invalid mu
     */
    public static double inverse(LinkFunction link, double eta) {
        if (!Double.isFinite(eta)) {
            throw new IllegalArgumentException("Linear predictor eta must be finite, got: " + eta);
        }
        
        switch (link) {
            case IDENTITY:
                return eta;
                
            case LOG:
                // Clamp eta to avoid overflow
                double clampedEta = Math.min(eta, MAX_EXP_ARG);
                return Math.exp(clampedEta);
                
            case LOGIT:
                // Use numerically stable computation
                if (eta > MAX_EXP_ARG) {
                    return 1.0 - LOGIT_EPSILON;
                } else if (eta < -MAX_EXP_ARG) {
                    return LOGIT_EPSILON;
                } else {
                    double expEta = Math.exp(eta);
                    return expEta / (1.0 + expEta);
                }
                
            case PROBIT:
                return STANDARD_NORMAL.cumulativeProbability(eta);
                
            case INVERSE:
                if (Math.abs(eta) < LOG_EPSILON) {
                    throw new IllegalArgumentException("Cannot compute 1/eta when eta ~= 0, got eta = " + eta);
                }
                double result = 1.0 / eta;
                if (result <= 0.0) {
                    throw new IllegalArgumentException("Inverse link resulted in non-positive mean: mu = " + result + " (eta = " + eta + ")");
                }
                return result;
                
            case SQRT:
                if (eta < 0.0) {
                    throw new IllegalArgumentException("Square root link requires eta >= 0, got eta = " + eta);
                }
                return eta * eta;
                
                
            default:
                throw new IllegalArgumentException("Unsupported link function: " + link);
        }
    }
    
    /**
     * Returns true if the mean parameter mu is finite and inside the domain of the link function.
     * Non-throwing counterpart of {@link #validateDomain(LinkFunction, double)}.
     */
    public static boolean isInDomain(LinkFunction link, double mu) {
        if (!Double.isFinite(mu)) {
            return false;
        }
        return switch (link) {
            case IDENTITY -> true;
            case LOG, INVERSE -> mu > 0.0;
            case LOGIT, PROBIT -> mu > 0.0 && mu < 1.0;
            case SQRT -> mu >= 0.0;
            default -> false;
        };
    }

    /**
     * Returns true if the linear predictor eta is finite and the inverse link can be applied to it
     * without leaving the domain of the mean. Non-throwing counterpart of the checks in
     * {@link #inverse(LinkFunction, double)}.
     */
    public static boolean isValidLinearPredictor(LinkFunction link, double eta) {
        if (!Double.isFinite(eta)) {
            return false;
        }
        return switch (link) {
            case INVERSE -> eta >= LOG_EPSILON; // 1/eta must be positive and finite
            case SQRT -> eta >= 0.0;
            default -> true;
        };
    }

    /**
     * Validate that the mean parameter mu is in the valid domain for the given link function.
     * 
     * @param link the link function
     * @param mu the mean parameter to validate
     * @throws IllegalArgumentException if mu is outside the valid domain
     */
    public static void validateDomain(LinkFunction link, double mu) {
        if (!Double.isFinite(mu)) {
            throw new IllegalArgumentException("Mean parameter mu must be finite, got: " + mu);
        }
        
        switch (link) {
            case IDENTITY:
                // No domain restrictions for identity link
                break;
                
            case LOG:
            case INVERSE:
                if (mu <= 0.0) {
                    throw new IllegalArgumentException(link.getDisplayName() + " link requires mu > 0, got mu = " + mu);
                }
                break;
                
            case LOGIT:
            case PROBIT:
                if (mu <= 0.0 || mu >= 1.0) {
                    throw new IllegalArgumentException(link.getDisplayName() + " link requires mu in (0,1), got mu = " + mu);
                }
                break;
                
            case SQRT:
                if (mu < 0.0) {
                    throw new IllegalArgumentException("Square root link requires mu >= 0, got mu = " + mu);
                }
                break;
                
            default:
                throw new IllegalArgumentException("Domain validation not implemented for link: " + link);
        }
    }
    
    /**
     * Get the derivative of the inverse link function d(mu)/d(eta).
     * This is useful for computing standard errors and confidence intervals.
     * 
     * @param link the link function
     * @param eta the linear predictor eta
     * @return the derivative d(mu)/d(eta) at eta
     */
    public static double inverseLinkDerivative(LinkFunction link, double eta) {
        switch (link) {
            case IDENTITY:
                return 1.0;
                
            case LOG:
                double clampedEta = Math.min(eta, MAX_EXP_ARG);
                return Math.exp(clampedEta);
                
            case LOGIT:
                if (Math.abs(eta) > MAX_EXP_ARG) {
                    return 0.0; // Derivative approaches 0 at extremes
                }
                double expEta = Math.exp(eta);
                double denom = 1.0 + expEta;
                return expEta / (denom * denom);
                
            case PROBIT:
                double phi = Math.exp(-0.5 * eta * eta) / Math.sqrt(2.0 * Math.PI);
                return phi;
                
            case INVERSE:
                return -1.0 / (eta * eta);
                
            case SQRT:
                return 2.0 * eta;
                
                
            default:
                throw new IllegalArgumentException("Derivative not implemented for link: " + link);
        }
    }
}