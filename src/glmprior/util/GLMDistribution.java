package glmprior.util;

import beast.base.core.Description;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.parameter.BooleanParameter;

import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.Distribution;
import org.apache.commons.math.distribution.IntegerDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.distribution.PoissonDistributionImpl;
import org.apache.commons.math.distribution.GammaDistributionImpl;
import org.apache.commons.math.distribution.BinomialDistributionImpl;

/**
 * A generalized GLM-driven parametric distribution that supports multiple distribution families
 * and link functions. The mean parameter is modeled as:
 *   eta = g(baselineValue) + sum_j beta[j] * X[j]  (linear predictor)
 *   mu = g^(-1)(eta)  (mean via inverse link function)
 *   y | mu, theta ~ Family(mu, theta)  (response from specified family with additional parameters theta)
 *
 * Supports: Normal, Poisson, Binomial, and Gamma distributions
 * with various link functions (identity, log, logit, probit, inverse, sqrt).
 *
 * This class is designed for programmatic use (via constructor) rather than XML initialization.
 */
@Description("General GLM-driven parametric distribution supporting multiple families and link functions")
public class GLMDistribution extends ParametricDistribution {

    // Core GLM fields
    private final RealParameter baselineValue;
    private final RealParameter coefficients;
    private final Double[] predictorValues;
    private final BooleanParameter indicators;

    // Distribution family and link function
    private final DistributionFamily family;
    private final LinkFunction link;

    // Distribution-specific parameters
    private final RealParameter sigma;
    private final RealParameter sigma2;
    private final RealParameter nTrials;
    private final RealParameter shape;

    // Cached values
    private final int p; // number of predictors

    /**
     * Programmatic constructor for creating GLMDistribution instances.
     * Validation of parameters should be done in MultiGLMDistribution before calling this constructor.
     *
     * @param baselineValue Baseline value on response scale (value when all predictors are 0)
     * @param coefficients GLM coefficients
     * @param predictorValues Predictor values (one value per coefficient)
     * @param indicators Optional indicators for variable selection
     * @param family Distribution family
     * @param link Link function
     * @param sigma Standard deviation for Normal distribution (optional)
     * @param sigma2 Variance for Normal distribution (optional)
     * @param nTrials Number of trials for Binomial distribution (optional)
     * @param shape Shape parameter for Gamma distribution (optional)
     */
    public GLMDistribution(RealParameter baselineValue,
                          RealParameter coefficients,
                          Double[] predictorValues,
                          BooleanParameter indicators,
                          DistributionFamily family,
                          LinkFunction link,
                          RealParameter sigma,
                          RealParameter sigma2,
                          RealParameter nTrials,
                          RealParameter shape) {
        super();

        this.baselineValue = baselineValue;
        this.coefficients = coefficients;
        this.predictorValues = predictorValues;
        this.indicators = indicators;
        this.family = family;
        this.link = link;
        this.sigma = sigma;
        this.sigma2 = sigma2;
        this.nTrials = nTrials;
        this.shape = shape;

        this.p = predictorValues.length;
    }

    @Override
    public void initAndValidate() {
        // Minimal validation - just dimension checks
        if (coefficients.getDimension() != p) {
            coefficients.setDimension(p);
        }

        if (indicators != null && indicators.getDimension() != p) {
            indicators.setDimension(p);
        }
    }

    /**
     * Computes the linear predictor eta = g(baseline) + sum_j(gamma_j * beta_j * x_j)
     * where gamma_j are the binary indicators (if provided) for variable selection,
     * and g() is the link function applied to the baseline value.
     *
     * The baselineValue parameter is treated as the baseline value on the response scale,
     * so the link function is applied to convert it to the linear predictor scale.
     */
    private double computeLinearPredictor() {
        return computeLinearPredictor(baselineValue.getValue(), coefficients.getValues(),
                indicators != null ? indicators.getValues() : null);
    }

    /**
     * Computes the linear predictor from explicit values.
     * Returns NaN if the baseline value lies outside the domain of the link function
     * (e.g. a non-positive baseline with a log link), so that callers can reject the
     * state instead of throwing.
     */
    private double computeLinearPredictor(double baseline, Double[] beta, Boolean[] indicators) {
        if (!LinkFunctions.isInDomain(link, baseline)) {
            return Double.NaN;
        }
        double eta = LinkFunctions.apply(link, baseline);
        return accumulateLinearPredictor(eta, beta, indicators);
    }

    private double accumulateLinearPredictor(double eta, Double[] beta, Boolean[] indicators) {
        for (int j = 0; j < p; j++) {
            double coefficient = beta[j];

            // Apply indicator variable if provided (for variable selection)
            if (indicators != null) {
                boolean indicator = indicators[j];
                if (!indicator) {
                    coefficient = 0.0; // Exclude this variable if indicator is false
                }
            }

            eta += coefficient * predictorValues[j];
        }

        return eta;
    }

    /**
     * Computes the mean parameter mu = g^(-1)(eta) using the inverse link function.
     * Returns NaN if the current state gives no valid mean: the baseline is outside the
     * link domain, the linear predictor cannot be inverted (e.g. negative eta with an
     * inverse link), or the resulting mean is outside the family's domain.
     */
    private double computeMean() {
        return meanFromLinearPredictor(computeLinearPredictor());
    }

    private double computeMean(Double baselineValue, Double[] coefficients, Boolean[] indicators) {
        return meanFromLinearPredictor(computeLinearPredictor(baselineValue, coefficients, indicators));
    }

    private double meanFromLinearPredictor(double eta) {
        if (!LinkFunctions.isValidLinearPredictor(link, eta)) {
            return Double.NaN;
        }
        double mu = LinkFunctions.inverse(link, eta);
        return family.isValidMean(mu) ? mu : Double.NaN;
    }

    /**
     * Creates the appropriate Apache Commons Math distribution object.
     * Returns the proper Distribution type (continuous or discrete).
     *
     * Note: LogNormal is not supported via this method - use logDensity() directly.
     *
     * @throws IllegalStateException if the current state gives no valid mean; use
     *         logDensity() for evaluations that must not throw during MCMC.
     */
    @Override
    public Distribution getDistribution() {
        double mu = computeMean();
        if (Double.isNaN(mu)) {
            throw new IllegalStateException(family.getDisplayName() + " GLM with " + link.getDisplayName() +
                    " link has no valid mean for the current parameter values");
        }
        return getDistribution(mu);
    }

    private Distribution getDistribution(double mu) {
        switch (family) {
            case NORMAL:
                double sigmaValue = getSigmaValue();
                return new NormalDistributionImpl(mu, sigmaValue);

            case POISSON:
                return new PoissonDistributionImpl(mu);

            case BINOMIAL:
                int n = (int) Math.round(nTrials.getValue());
                // For binomial, mu is the probability p
                return new BinomialDistributionImpl(n, mu);

            case GAMMA:
                double shapeValue = shape.getValue();
                double rate = shapeValue / mu; // rate = shape / mean
                return new GammaDistributionImpl(shapeValue, 1.0 / rate); // Commons Math uses scale = 1/rate

            case LOGNORMAL:
                // LogNormal doesn't have a direct Commons Math impl in the old package.
                // Return a Normal distribution on the log scale for compatibility,
                // but logDensity() should be used for proper calculations.
                double logMu = computeLinearPredictor(); // eta = log-scale mean
                return new NormalDistributionImpl(logMu, getSigmaValue());

            case LOGITNORMAL:
                // LogitNormal doesn't have a Commons Math impl.
                // Return a Normal distribution on the logit scale for compatibility,
                // but logDensity() should be used for proper calculations.
                double logitMu = computeLinearPredictor(); // eta = logit-scale mean
                return new NormalDistributionImpl(logitMu, getSigmaValue());

            default:
                throw new IllegalStateException("Distribution creation not implemented for " + family);
        }
    }

    /**
     * Computes the log density for the given value.
     *
     * Never throws for an invalid GLM state: if the current parameter values give no valid
     * mean (baseline outside the link domain, non-invertible linear predictor, or mean outside
     * the family domain), the result is -Infinity so the MCMC rejects the proposal.
     *
     * Note: the offset input of ParametricDistribution is ignored, consistent with
     * MultiGLMDistribution.calcLogP.
     */
    @Override
    public double logDensity(double y) {
        if (family == DistributionFamily.LOGNORMAL) {
            return logNormalLogDensity(y);
        }
        if (family == DistributionFamily.LOGITNORMAL) {
            return logitNormalLogDensity(y);
        }

        double mu = computeMean();
        if (Double.isNaN(mu)) {
            return Double.NEGATIVE_INFINITY;
        }

        Distribution dist = getDistribution(mu);
        if (dist instanceof ContinuousDistribution) {
            return ((ContinuousDistribution) dist).logDensity(y);
        }
        if (dist instanceof IntegerDistribution) {
            double probability = ((IntegerDistribution) dist).probability(y);
            return probability > 0 ? Math.log(probability) : Double.NEGATIVE_INFINITY;
        }
        return Double.NEGATIVE_INFINITY;
    }

    /**
     * Computes log density for LogNormal distribution.
     * LogNormal(mu, sigma) where log(Y) ~ Normal(mu, sigma).
     *
     * For our GLM with log link:
     *   eta = log(baseline) + beta*X  (linear predictor = log-scale mean)
     *   log(Y) ~ Normal(eta, sigma)
     *
     * log f(y) = -log(y) - log(sigma) - 0.5*log(2*pi) - 0.5*((log(y) - eta)/sigma)^2
     */
    private double logNormalLogDensity(double y) {
        if (y <= 0) {
            return Double.NEGATIVE_INFINITY;
        }

        double eta = computeLinearPredictor(); // log-scale mean
        if (!Double.isFinite(eta)) {
            return Double.NEGATIVE_INFINITY; // baseline outside link domain
        }
        double sigmaValue = getSigmaValue();
        double logY = Math.log(y);
        double z = (logY - eta) / sigmaValue;

        // log f(y) = -log(y) - log(sigma) - 0.5*log(2*pi) - 0.5*z^2
        return -logY - Math.log(sigmaValue) - 0.5 * Math.log(2 * Math.PI) - 0.5 * z * z;
    }

    /**
     * Computes log density for LogitNormal distribution.
     * LogitNormal(mu, sigma) where logit(Y) ~ Normal(mu, sigma).
     *
     * For our GLM with logit link:
     *   eta = logit(baseline) + beta*X  (linear predictor = logit-scale mean)
     *   logit(Y) ~ Normal(eta, sigma)
     *
     * The PDF of LogitNormal is:
     *   f(y) = (1 / (sigma * sqrt(2*pi))) * (1 / (y * (1-y))) * exp(-0.5 * ((logit(y) - eta) / sigma)^2)
     *
     * log f(y) = -log(sigma) - 0.5*log(2*pi) - log(y) - log(1-y) - 0.5*((logit(y) - eta)/sigma)^2
     */
    private double logitNormalLogDensity(double y) {
        if (y <= 0 || y >= 1) {
            return Double.NEGATIVE_INFINITY;
        }

        double eta = computeLinearPredictor(); // logit-scale mean
        if (!Double.isFinite(eta)) {
            return Double.NEGATIVE_INFINITY; // baseline outside link domain
        }
        double sigmaValue = getSigmaValue();
        double logitY = Math.log(y / (1.0 - y)); // logit(y)
        double z = (logitY - eta) / sigmaValue;

        // log f(y) = -log(sigma) - 0.5*log(2*pi) - log(y) - log(1-y) - 0.5*z^2
        // The Jacobian term is 1/(y*(1-y)) which gives -log(y) - log(1-y) in log space
        return -Math.log(sigmaValue) - 0.5 * Math.log(2 * Math.PI)
               - Math.log(y) - Math.log(1.0 - y) - 0.5 * z * z;
    }

    /**
     * Computes the density for the given value. Returns 0 for an invalid GLM state.
     */
    @Override
    public double density(double x) {
        return Math.exp(logDensity(x));
    }

    /**
     * Gets the standard deviation for Normal distribution from either sigma or sigma2 field.
     */
    private double getSigmaValue() {
        if (sigma != null) {
            return sigma.getValue();
        } else if (sigma2 != null) {
            return Math.sqrt(sigma2.getValue());
        } else {
            throw new IllegalStateException("No sigma or sigma2 parameter available for Normal distribution");
        }
    }

    // Convenience accessors

    /**
     * Mean (location) for the current parameter values, or NaN if the current state gives
     * no valid mean. Callers such as operators must treat NaN as a reason to reject.
     */
    public double getMean() {
        return computeMean();
    }

    /**
     * Mean (location) computed from the stored (pre-proposal) parameter values, or NaN if
     * that state gives no valid mean.
     */
    public double getStoredMean() {
        return computeMean(baselineValue.getStoredValues()[0],
                coefficients.getStoredValues(),
                indicators != null ? indicators.getStoredValues() : null);
    }

    public double getVariance() {
        switch (family) {
            case NORMAL:
                double sigmaValue = getSigmaValue();
                return sigmaValue * sigmaValue;
            case POISSON:
                return computeMean(); // For Poisson, variance = mean
            case BINOMIAL:
                int n = (int) Math.round(nTrials.getValue());
                double p = computeMean();
                return n * p * (1 - p);
            case GAMMA:
                double mu = computeMean();
                double shapeValue = shape.getValue();
                return mu * mu / shapeValue; // For Gamma: var = mu^2/shape
            case LOGNORMAL:
                // For LogNormal where log(Y) ~ Normal(eta, sigma):
                // Var(Y) = (exp(sigma^2) - 1) * exp(2*eta + sigma^2)
                double etaLN = computeLinearPredictor();
                double sigmaLN = getSigmaValue();
                double sigma2LN = sigmaLN * sigmaLN;
                return (Math.exp(sigma2LN) - 1) * Math.exp(2 * etaLN + sigma2LN);
            case LOGITNORMAL:
                // For LogitNormal where logit(Y) ~ Normal(eta, sigma):
                // No closed-form variance. Using delta method approximation:
                // Var(Y) ~= (d(mu)/d(eta))^2 * sigma^2 where mu = logit^(-1)(eta)
                // d(mu)/d(eta) = exp(eta)/(1+exp(eta))^2 = mu(1-mu)
                double etaLGN = computeLinearPredictor();
                double sigmaLGN = getSigmaValue();
                double muLGN = 1.0 / (1.0 + Math.exp(-etaLGN)); // logit^(-1)(eta)
                double derivLGN = muLGN * (1.0 - muLGN); // Jacobian
                return derivLGN * derivLGN * sigmaLGN * sigmaLGN;
            default:
                throw new UnsupportedOperationException("Variance calculation not implemented for " + family);
        }
    }

    public DistributionFamily getFamily() {
        return family;
    }

    public LinkFunction getLink() {
        return link;
    }
}