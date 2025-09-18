# GLM Distribution Examples

This document provides examples of how to use the new generalized GLM framework in BEAST2 XML configurations.

## 1. Normal Distribution with Identity Link (equivalent to original GLMNormalDistribution)

```xml
<distribution spec="glmprior.util.GLMDistribution" family="NORMAL" link="IDENTITY">
    <intercept spec="RealParameter" value="2.0"/>
    <coefficients spec="RealParameter" value="1.5 -0.5"/>
    <predictors spec="RealParameter" value="1.0 2.0"/>
    <sigma spec="RealParameter" value="1.0"/>
</distribution>
```

This gives: μ = 2.0 + 1.5×1.0 + (-0.5)×2.0 = 2.5, with σ = 1.0

## 2. Normal Distribution with Log Link

```xml
<distribution spec="glmprior.util.GLMDistribution" family="NORMAL" link="LOG">
    <intercept spec="RealParameter" value="1.0"/>
    <coefficients spec="RealParameter" value="0.5 0.3"/>
    <predictors spec="RealParameter" value="2.0 1.0"/>
    <sigma spec="RealParameter" value="0.5"/>
</distribution>
```

This gives: η = 1.0 + 0.5×2.0 + 0.3×1.0 = 2.3, μ = exp(2.3) ≈ 9.97

## 3. Poisson Distribution with Log Link (canonical)

```xml
<distribution spec="glmprior.util.GLMDistribution" family="POISSON">
    <intercept spec="RealParameter" value="1.5"/>
    <coefficients spec="RealParameter" value="0.8 -0.2"/>
    <predictors spec="RealParameter" value="1.0 3.0"/>
</distribution>
```

This gives: η = 1.5 + 0.8×1.0 + (-0.2)×3.0 = 1.7, λ = exp(1.7) ≈ 5.47

## 4. Binomial Distribution with Logit Link (canonical)

```xml
<distribution spec="glmprior.util.GLMDistribution" family="BINOMIAL">
    <intercept spec="RealParameter" value="0.0"/>
    <coefficients spec="RealParameter" value="1.2 -0.8"/>
    <predictors spec="RealParameter" value="0.5 0.3"/>
    <nTrials spec="RealParameter" value="20"/>
</distribution>
```

This gives: η = 0.0 + 1.2×0.5 + (-0.8)×0.3 = 0.36, p = exp(0.36)/(1+exp(0.36)) ≈ 0.59

## 5. Binomial Distribution with Probit Link

```xml
<distribution spec="glmprior.util.GLMDistribution" family="BINOMIAL" link="PROBIT">
    <intercept spec="RealParameter" value="0.2"/>
    <coefficients spec="RealParameter" value="0.5"/>
    <predictors spec="RealParameter" value="1.0"/>
    <nTrials spec="RealParameter" value="10"/>
</distribution>
```

This gives: η = 0.2 + 0.5×1.0 = 0.7, p = Φ(0.7) ≈ 0.758

## 6. Gamma Distribution with Inverse Link (canonical)

```xml
<distribution spec="glmprior.util.GLMDistribution" family="GAMMA">
    <intercept spec="RealParameter" value="2.0"/>
    <coefficients spec="RealParameter" value="0.5"/>
    <predictors spec="RealParameter" value="1.0"/>
    <shape spec="RealParameter" value="3.0"/>
</distribution>
```

This gives: η = 2.0 + 0.5×1.0 = 2.5, μ = 1/2.5 = 0.4, with shape = 3.0

## 7. Gamma Distribution with Log Link

```xml
<distribution spec="glmprior.util.GLMDistribution" family="GAMMA" link="LOG">
    <intercept spec="RealParameter" value="1.0"/>
    <coefficients spec="RealParameter" value="0.3 0.2"/>
    <predictors spec="RealParameter" value="2.0 1.5"/>
    <shape spec="RealParameter" value="2.5"/>
</distribution>
```

This gives: η = 1.0 + 0.3×2.0 + 0.2×1.5 = 1.9, μ = exp(1.9) ≈ 6.69

## 8. Using Variance Parameter (σ²) Instead of Standard Deviation

```xml
<distribution spec="glmprior.util.GLMDistribution" family="NORMAL">
    <intercept spec="RealParameter" value="0.0"/>
    <coefficients spec="RealParameter" value="1.0"/>
    <predictors spec="RealParameter" value="3.0"/>
    <sigma2 spec="RealParameter" value="4.0"/>
</distribution>
```

This gives: μ = 0.0 + 1.0×3.0 = 3.0, with σ² = 4.0 (so σ = 2.0)

## 9. Predictor Standardization

```xml
<distribution spec="glmprior.util.GLMDistribution" family="NORMAL" standardize="true">
    <intercept spec="RealParameter" value="5.0"/>
    <coefficients spec="RealParameter" value="2.0 1.5 0.8"/>
    <predictors spec="RealParameter" value="100.0 200.0 500.0"/>
    <sigma spec="RealParameter" value="1.5"/>
</distribution>
```

The predictors will be z-score standardized at initialization, then the GLM will be applied.

## 10. Default Behavior (Uses Canonical Links)

```xml
<!-- This automatically uses LOG link for Poisson -->
<distribution spec="glmprior.util.GLMDistribution" family="POISSON">
    <intercept spec="RealParameter" value="1.0"/>
    <coefficients spec="RealParameter" value="0.5"/>
    <predictors spec="RealParameter" value="2.0"/>
</distribution>

<!-- This automatically uses LOGIT link for Binomial -->
<distribution spec="glmprior.util.GLMDistribution" family="BINOMIAL">
    <intercept spec="RealParameter" value="0.0"/>
    <coefficients spec="RealParameter" value="1.0"/>
    <predictors spec="RealParameter" value="0.5"/>
    <nTrials spec="RealParameter" value="15"/>
</distribution>
```

## Valid Family-Link Combinations

| Family | Valid Links |
|--------|-------------|
| NORMAL | IDENTITY, LOG |
| POISSON | LOG, IDENTITY, SQRT |
| BINOMIAL | LOGIT, PROBIT, IDENTITY |
| GAMMA | INVERSE, LOG, IDENTITY |
| INVERSE_GAUSSIAN | INVERSE_SQUARED, LOG, INVERSE |
| NEGATIVE_BINOMIAL | LOG, IDENTITY, SQRT |

## Migration from GLMNormalDistribution

To migrate from the old `GLMNormalDistribution` to the new framework:

**Old:**
```xml
<distribution spec="glmprior.util.GLMNormalDistribution">
    <intercept spec="RealParameter" value="2.0"/>
    <coefficients spec="RealParameter" value="1.0 -0.5"/>
    <predictors spec="RealParameter" value="1.5 2.0"/>
    <sigma spec="RealParameter" value="1.2"/>
</distribution>
```

**New:**
```xml
<distribution spec="glmprior.util.GLMDistribution" family="NORMAL" link="IDENTITY">
    <intercept spec="RealParameter" value="2.0"/>
    <coefficients spec="RealParameter" value="1.0 -0.5"/>
    <predictors spec="RealParameter" value="1.5 2.0"/>
    <sigma spec="RealParameter" value="1.2"/>
</distribution>
```

Or simply:
```xml
<distribution spec="glmprior.util.GLMDistribution">
    <intercept spec="RealParameter" value="2.0"/>
    <coefficients spec="RealParameter" value="1.0 -0.5"/>
    <predictors spec="RealParameter" value="1.5 2.0"/>
    <sigma spec="RealParameter" value="1.2"/>
</distribution>
```

Since NORMAL with IDENTITY link is the default.