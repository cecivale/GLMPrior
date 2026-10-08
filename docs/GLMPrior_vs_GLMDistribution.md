# GLMPrior vs MultiGLMDistribution: Equivalence Guide

This document explains how to configure `GLMPrior` and `MultiGLMDistribution` to achieve equivalent models, and provides guidance on when to use each approach.

## Overview

- **GLMPrior**: A `Function` that computes parameter values directly using a GLM formula. The output is deterministic given the GLM parameters.
- **MultiGLMDistribution**: A `ParametricDistribution` that defines a probability distribution whose mean is determined by a GLM. Used as a prior on estimated parameters.

## Equivalence Table

### With Error Terms (Probabilistic Models)

For GLMPrior with error terms to be equivalent to MultiGLMDistribution, use the following mappings:

| GLMPrior Config | MultiGLMDistribution Config | Mathematical Model |
|-----------------|-----------------------------|--------------------|
| `linkFunction="log"` + `error` | `family="LOGNORMAL"` `link="LOG"` | log(Y) ~ Normal(η, σ) |
| `linkFunction="identity"` + `error` | `family="NORMAL"` `link="IDENTITY"` | Y ~ Normal(η, σ) |
| `linkFunction="logit"` + `error` | `family="LOGITNORMAL"` `link="LOGIT"` | logit(Y) ~ Normal(η, σ) |

Where η = g(baseline) + β·X is the linear predictor.

### Without Error Terms (Deterministic)

| GLMPrior Config | MultiGLMDistribution Equivalent |
|-----------------|--------------------------------|
| `linkFunction="log"`, no error | **Not applicable** - use GLMPrior directly |
| `linkFunction="identity"`, no error | **Not applicable** - use GLMPrior directly |
| `linkFunction="logit"`, no error | **Not applicable** - use GLMPrior directly |

GLMPrior without error terms is a deterministic function. There is no equivalent distribution-based representation.

## Parameter Mapping

| GLMPrior Parameter | MultiGLMDistribution Parameter | Description |
|-------------------|-------------------------------|-------------|
| `baselineValue` | `baselineValue` | Value on response scale when all predictors are 0 |
| `coefficients` | `coefficients` | GLM coefficients (β) |
| `indicators` | `indicators` | Binary indicators for variable selection |
| `error` prior: Normal(0, σ) | `sigma` | Standard deviation on link scale |
| `predictor` | `predictor` | Predictor functions |
| `logTransform` | `logTransform` | Apply log(x+1) transform to predictors |
| `standardize` | `standardize` | Standardize predictors to mean=0, sd=1 |

## XML Configuration Examples

### Example 1: Log Link with Error (Rates, Positive Values)

**GLMPrior setup:**
```xml
<!-- GLMPrior computes values directly -->
<skylineValues id="samplingRate" spec="glmprior.util.GLMPrior"
               linkFunction="log"
               logTransform="true"
               standardize="true">
    <predictor spec="RealParameterFromLabelledXSV" .../>
    <parameter id="coefficients" spec="RealParameter" name="coefficients" value="0.1"/>
    <parameter id="baseline" spec="RealParameter" name="baselineValue" value="0.1"/>
    <parameter id="indicators" spec="BooleanParameter" name="indicators" value="1"/>
    <parameter id="errorTerms" spec="RealParameter" name="error" value="0.0" dimension="8"/>
</skylineValues>

<!-- Prior on error terms -->
<prior x="@errorTerms">
    <Normal mean="0" sigma="@error_sigma"/>
</prior>
```

**Equivalent MultiGLMDistribution setup:**
```xml
<!-- samplingRate is a free parameter -->
<parameter id="samplingRate" spec="RealParameter" dimension="8" value="0.1"/>

<!-- GLM prior on samplingRate -->
<prior x="@samplingRate">
    <distr spec="glmprior.util.MultiGLMDistribution"
           family="LOGNORMAL"
           link="LOG"
           logTransform="true"
           standardize="true"
           baselineValue="@baseline"
           coefficients="@coefficients"
           indicators="@indicators"
           sigma="@error_sigma">
        <predictor spec="RealParameterFromLabelledXSV" .../>
    </distr>
</prior>
```

### Example 2: Logit Link with Error (Probabilities)

**GLMPrior setup:**
```xml
<skylineValues id="probability" spec="glmprior.util.GLMPrior"
               linkFunction="logit"
               logTransform="true"
               standardize="true">
    <predictor .../>
    <parameter id="coefficients" name="coefficients" .../>
    <parameter id="baseline" name="baselineValue" value="0.5"/>  <!-- probability scale -->
    <parameter id="indicators" name="indicators" .../>
    <parameter id="errorTerms" name="error" dimension="8"/>
</skylineValues>

<prior x="@errorTerms">
    <Normal mean="0" sigma="@error_sigma"/>
</prior>
```

**Equivalent MultiGLMDistribution setup:**
```xml
<parameter id="probability" spec="RealParameter" dimension="8" value="0.5" lower="0" upper="1"/>

<prior x="@probability">
    <distr spec="glmprior.util.MultiGLMDistribution"
           family="LOGITNORMAL"
           link="LOGIT"
           logTransform="true"
           standardize="true"
           baselineValue="@baseline"
           coefficients="@coefficients"
           indicators="@indicators"
           sigma="@error_sigma">
        <predictor .../>
    </distr>
</prior>
```

### Example 3: Identity Link with Error (Unbounded Values)

**GLMPrior setup:**
```xml
<skylineValues id="value" spec="glmprior.util.GLMPrior"
               linkFunction="identity">
    <predictor .../>
    <parameter id="coefficients" name="coefficients" .../>
    <parameter id="baseline" name="baselineValue" value="1.0"/>
    <parameter id="indicators" name="indicators" .../>
    <parameter id="errorTerms" name="error" dimension="8"/>
</skylineValues>

<prior x="@errorTerms">
    <Normal mean="0" sigma="@error_sigma"/>
</prior>
```

**Equivalent MultiGLMDistribution setup:**
```xml
<parameter id="value" spec="RealParameter" dimension="8" value="1.0"/>

<prior x="@value">
    <distr spec="glmprior.util.MultiGLMDistribution"
           family="NORMAL"
           link="IDENTITY"
           baselineValue="@baseline"
           coefficients="@coefficients"
           indicators="@indicators"
           sigma="@error_sigma">
        <predictor .../>
    </distr>
</prior>
```

## When to Use Each Approach

### Use GLMPrior When:

1. **Deterministic relationships**: You want the parameter values to be exactly determined by the GLM formula without additional noise.

2. **Direct computation**: The GLM output is used directly as a model parameter (e.g., sampling rate in a birth-death model).

3. **Explicit error terms**: You want to estimate individual error terms for each dimension, allowing per-dimension deviations from the GLM prediction.

4. **Simpler model structure**: Fewer parameters when error terms are not needed.

### Use MultiGLMDistribution When:

1. **Standard prior specification**: You want to specify a prior distribution on a parameter, where the mean of the prior follows a GLM.

2. **Shared variance**: All dimensions share the same error variance (σ), rather than having individual error terms.

3. **Familiar GLM framework**: The distribution-based approach is more aligned with standard GLM theory.

4. **Integration with BEAST priors**: Fits naturally into BEAST's prior specification framework.

## Error Structure Comparison

| Link Function | Error Location | GLMPrior Formula | Distribution |
|---------------|----------------|------------------|--------------|
| log | Log scale (multiplicative) | Y = baseline × exp(β·X + ε) | LogNormal |
| identity | Original scale (additive) | Y = baseline + β·X + ε | Normal |
| logit | Logit scale | Y = logit⁻¹(logit(baseline) + β·X + ε) | LogitNormal |

### Why Link-Scale Error?

Putting error on the link scale (as GLMPrior does) has important properties:

- **Log link**: Error is multiplicative on the original scale. This is appropriate when you expect constant coefficient of variation (CV), common for rates and concentrations.

- **Logit link**: Error is on the log-odds scale. The output is automatically constrained to (0, 1), appropriate for probabilities.

- **Identity link**: Error is additive, appropriate for unbounded continuous variables.

## Distribution Family Summary

| Family | Domain | Canonical Link | Use Case |
|--------|--------|----------------|----------|
| NORMAL | (-∞, +∞) | identity | Unbounded continuous data |
| LOGNORMAL | (0, +∞) | log | Positive continuous data (rates, concentrations) |
| LOGITNORMAL | (0, 1) | logit | Proportions, probabilities |
| POISSON | {0, 1, 2, ...} | log | Count data |
| BINOMIAL | {0, ..., n} | logit | Binary/binomial outcomes |
| GAMMA | (0, +∞) | inverse | Positive continuous with right skew |

## Important Notes

1. **Initial values must match**: For equivalent results, ensure initial values of baseline, coefficients, indicators, and the target parameter are consistent between the two approaches.

2. **Predictor transformations**: Always set `logTransform` and `standardize` identically in both approaches.

3. **Dimension consistency**: Ensure the dimension of the target parameter matches the dimension of the predictors.

4. **Sigma interpretation**: In both approaches, sigma represents standard deviation on the link scale (log scale for LOGNORMAL, logit scale for LOGITNORMAL).

## Mathematical Details

In all cases, the linear predictor is:
```
η = g(baselineValue) + Σ(βⱼ × Xⱼ × γⱼ)
```
where g() is the link function, βⱼ are coefficients, Xⱼ are predictors, and γⱼ are indicators.

---

### NORMAL Family

**Model**: Y ~ Normal(μ, σ)

**Link functions**:
- Identity (canonical): μ = η
- Log: μ = exp(η)

**Properties**:
- Mean: E[Y] = μ
- Variance: Var(Y) = σ²
- Domain: Y ∈ (-∞, +∞)

**Use case**: Unbounded continuous data with additive error structure.

**GLMPrior equivalent**: `linkFunction="identity"` with error terms.

---

### LOGNORMAL Family

**Model**: log(Y) ~ Normal(η, σ), equivalently Y ~ LogNormal(η, σ)

**Link functions**:
- Log (canonical): η = log(baselineValue) + β·X
- Identity: η = baselineValue + β·X (less common)

**Properties**:
- Median: exp(η)
- Mean: E[Y] = exp(η + σ²/2)
- Variance: Var(Y) = (exp(σ²) - 1) × exp(2η + σ²)
- Domain: Y ∈ (0, +∞)
- Error is multiplicative on original scale

**Use case**: Positive continuous data (rates, concentrations) where coefficient of variation is approximately constant.

**GLMPrior equivalent**: `linkFunction="log"` with error terms.

---

### LOGITNORMAL Family

**Model**: logit(Y) ~ Normal(η, σ), where logit(y) = log(y/(1-y))

**Link functions**:
- Logit (canonical): η = logit(baselineValue) + β·X
- Identity: η = baselineValue + β·X (less common)

**Properties**:
- Mode ≈ logit⁻¹(η) for small σ
- Mean: No closed form (requires numerical integration)
- Variance: Approximated using delta method: Var(Y) ≈ [μ(1-μ)]² × σ²
- Domain: Y ∈ (0, 1)
- Error is on log-odds scale

**Use case**: Proportions or probabilities with overdispersion or individual-level variation.

**GLMPrior equivalent**: `linkFunction="logit"` with error terms.

---

### POISSON Family

**Model**: Y ~ Poisson(λ) where λ = μ

**Link functions**:
- Log (canonical): μ = exp(η)
- Identity: μ = η
- Sqrt: μ = η²

**Properties**:
- Mean: E[Y] = μ
- Variance: Var(Y) = μ (variance equals mean)
- Domain: Y ∈ {0, 1, 2, ...}

**Use case**: Count data where variance approximately equals mean.

**Note**: Poisson does not model error on the link scale like GLMPrior. It assumes the variance-mean relationship inherent to the Poisson distribution. Not directly equivalent to GLMPrior with error terms.

---

### BINOMIAL Family

**Model**: Y ~ Binomial(n, p) where p = μ

**Link functions**:
- Logit (canonical): μ = logit⁻¹(η) = 1/(1 + exp(-η))
- Probit: μ = Φ(η) where Φ is the standard normal CDF
- Identity: μ = η (only valid if η ∈ [0,1])

**Properties**:
- Mean: E[Y] = n × p
- Variance: Var(Y) = n × p × (1 - p)
- Domain: Y ∈ {0, 1, ..., n}
- Requires `nTrials` parameter

**Use case**: Binary outcomes or counts out of a fixed number of trials.

**Note**: Binomial does not model error on the link scale. For overdispersed proportions, use LOGITNORMAL instead.

---

### GAMMA Family

**Model**: Y ~ Gamma(shape, rate) where rate = shape/μ

**Link functions**:
- Inverse (canonical): μ = 1/η
- Log: μ = exp(η)
- Identity: μ = η

**Properties**:
- Mean: E[Y] = μ
- Variance: Var(Y) = μ²/shape (coefficient of variation is constant)
- Domain: Y ∈ (0, +∞)
- Requires `shape` parameter

**Use case**: Positive continuous data with right skew, where variance is proportional to the square of the mean.

**Note**: Gamma models a specific variance-mean relationship. For multiplicative error with normal distribution on log scale, use LOGNORMAL instead.

---

## GLMPrior Error Equivalence Summary

GLMPrior with `error ~ Normal(0, σ)` is equivalent to:

| Link Function | Equivalent Family | Model |
|---------------|-------------------|-------|
| `log` | LOGNORMAL | log(Y) ~ Normal(η, σ) |
| `identity` | NORMAL | Y ~ Normal(η, σ) |
| `logit` | LOGITNORMAL | logit(Y) ~ Normal(η, σ) |

Where η = g(baselineValue) + β·X is the linear predictor.

**Important**: POISSON, BINOMIAL, and GAMMA families model specific variance-mean relationships from their respective distributions. They are NOT equivalent to GLMPrior with error terms, which assumes normal error on the link scale.