package glmprior.util;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.parameter.RealParameter;

// Use Apache Commons Math for CDF/ICDF like many BEAST distributions do
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.Distribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

/**
 * A Normal parametric distribution whose mean is a fixed-time GLM:
 *   mu = intercept + sum_j beta[j] * X[j]
 * and x | mu, sigma ~ Normal(mu, sigma^2).
 *
 * Intended to be plugged into BEAUTi at the parametricDistributions merge point.
 */
@Description("GLM-driven Normal distribution: mean = intercept + sum_j beta_j * X_j; x ~ N(mean, sigma^2)")
public class GLMNormalDistribution extends ParametricDistribution {

    // Intercept (alpha)
    public Input<RealParameter> interceptInput = new Input<>(
            "intercept", "GLM intercept (alpha).", Validate.REQUIRED);

    // Coefficients (beta vector, dimension p)
    public Input<RealParameter> coefficientsInput = new Input<>(
            "coefficients", "GLM coefficients beta (dimension must match predictors).",
            Validate.REQUIRED);

    // Predictors (X vector, fixed values, dimension p)
    // Marked with a custom editor so users can load from file in BEAUTi (see editor below).
    public Input<RealParameter> predictorsInput = new Input<>(
            "predictors", "Predictor vector X (fixed; usually populated via BEAUTi file loader).",
            Validate.REQUIRED);

    // Sigma (std. dev.) > 0
    public Input<RealParameter> sigmaInput = new Input<>(
            "sigma", "Standard deviation sigma (>0).", Validate.REQUIRED);

    // Optional: whether to z-score predictors on init (off by default)
    public Input<Boolean> standardizeInput = new Input<>(
            "standardize", "Z-score standardize predictors at init.",
            false, Validate.OPTIONAL);

    private int p;

    @Override
    public void initAndValidate() {
        final RealParameter beta = coefficientsInput.get();
        final RealParameter X = predictorsInput.get();

        if (beta == null || X == null)
            throw new IllegalArgumentException("Both coefficients and predictors must be provided.");

        if (beta.getDimension() != X.getDimension()) {
            throw new IllegalArgumentException("Dimension mismatch: coefficients ("
                    + beta.getDimension() + ") vs predictors (" + X.getDimension() + ").");
        }
        p = beta.getDimension();

        final double sigma = sigmaInput.get().getValue();
        if (!(sigma > 0.0)) {
            throw new IllegalArgumentException("sigma must be > 0");
        }

        // Optional standardization (applied once at init)
        if (Boolean.TRUE.equals(standardizeInput.get())) {
            Double[] vals = X.getValues();
            double mean = 0.0;
            for (double v : vals) mean += v;
            mean /= vals.length;
            double var = 0.0;
            for (double v : vals) { double d = v - mean; var += d*d; }
            var /= Math.max(1, vals.length - 1);
            double sd = Math.sqrt(var);
            if (sd > 0) {
                for (int i = 0; i < vals.length; i++)
                    X.setValue(i, (vals[i] - mean) / sd);
            }
        }
    }

    /** Current GLM mean mu(alpha, beta, X) */
    private double currentMean() {
        double mu = interceptInput.get().getValue();
        final Double[] b = coefficientsInput.get().getValues();
        final Double[] x = predictorsInput.get().getValues();
        for (int j = 0; j < p; j++) mu += b[j] * x[j];
        return mu;
    }

    /** REQUIRED by ParametricDistribution: supply the current Commons-Math distribution. */
    @Override
    public ContinuousDistribution getDistribution() {
        final double mu    = currentMean();
        final double sigma = sigmaInput.get().getValue();
        // ParametricDistribution will call density/CDF/ICDF on this object.
        return new NormalDistributionImpl(mu, sigma);
    }

    // Convenience accessors other BEAST utilities sometimes use.
    public double getMean() { return currentMean(); }
    public double getVariance() {
        final double sigma = sigmaInput.get().getValue();
        return sigma * sigma;
    }
}
