# GLM Distribution with Indicator Variables for Variable Selection

This document provides examples of using the GLM framework with `BooleanParameter` indicators for Bayesian variable selection (BSSVS - Bayesian Stochastic Search Variable Selection).

## 1. Basic Variable Selection with Normal Distribution

```xml
<distribution spec="glmprior.util.GLMDistribution" family="NORMAL">
    <intercept spec="RealParameter" value="0.0"/>
    <coefficients spec="RealParameter" value="1.0 2.0 -0.5 0.8"/>
    <predictors spec="RealParameter" value="1.2 0.8 -1.1 2.3"/>
    <indicators spec="BooleanParameter" value="true false true true"/>
    <sigma spec="RealParameter" value="1.0"/>
</distribution>
```

In this example:
- 4 predictors with coefficients [1.0, 2.0, -0.5, 0.8]
- Indicators [true, false, true, true] mean variables 1, 3, and 4 are active
- Variable 2 is excluded (coefficient effectively becomes 0)
- Linear predictor: η = 0.0 + 1.0×1.2 + 0×2.0×0.8 + (-0.5)×(-1.1) + 0.8×2.3 = 4.89

## 2. Complete MCMC Setup for Variable Selection

```xml
<run spec="MCMC" chainLength="1000000">
    
    <!-- GLM Distribution with indicators -->
    <distribution spec="glmprior.util.GLMDistribution" family="NORMAL" id="glm">
        <intercept spec="RealParameter" id="intercept" value="0.0"/>
        <coefficients spec="RealParameter" id="coefficients" 
                     value="0.0 0.0 0.0 0.0 0.0" dimension="5"/>
        <predictors spec="RealParameter" id="predictors" 
                   value="1.2 -0.5 2.1 0.8 -1.3"/>
        <indicators spec="BooleanParameter" id="indicators" 
                   value="false false false false false" dimension="5"/>
        <sigma spec="RealParameter" id="sigma" value="1.0" lower="0.0"/>
    </distribution>

    <!-- Priors -->
    <distribution spec="CompoundDistribution" id="prior">
        
        <!-- Prior on intercept -->
        <distribution spec="Prior">
            <x idref="intercept"/>
            <distr spec="Normal" mean="0.0" sigma="10.0"/>
        </distribution>
        
        <!-- Prior on coefficients -->
        <distribution spec="Prior">
            <x idref="coefficients"/>
            <distr spec="Normal" mean="0.0" sigma="1.0"/>
        </distribution>
        
        <!-- Prior on sigma -->
        <distribution spec="Prior">
            <x idref="sigma"/>
            <distr spec="LogNormal" meanInRealSpace="false" M="0.0" S="1.0"/>
        </distribution>
        
        <!-- Prior on indicators (Bernoulli with p=0.5) -->
        <distribution spec="Prior">
            <x idref="indicators"/>
            <distr spec="Bernoulli" p="0.5"/>
        </distribution>
        
    </distribution>

    <!-- Operators -->
    <operator spec="ScaleOperator" parameter="@coefficients" weight="2.0"/>
    <operator spec="ScaleOperator" parameter="@sigma" weight="1.0"/>
    <operator spec="RealRandomWalkOperator" parameter="@intercept" weight="1.0"/>
    
    <!-- BitFlipOperator for indicator variables -->
    <operator spec="BitFlipOperator" parameter="@indicators" weight="5.0"/>

    <!-- Loggers -->
    <logger spec="Logger" logEvery="1000" fileName="glm_variable_selection.log">
        <log idref="prior"/>
        <log idref="likelihood"/>
        <log idref="intercept"/>
        <log idref="coefficients"/>
        <log idref="sigma"/>
        <log idref="indicators"/>
    </logger>

    <logger spec="Logger" logEvery="10000">
        <log idref="prior"/>
        <log idref="likelihood"/>
    </logger>
    
</run>
```

## 3. Poisson GLM with Variable Selection

```xml
<distribution spec="glmprior.util.GLMDistribution" family="POISSON">
    <intercept spec="RealParameter" value="1.0"/>
    <coefficients spec="RealParameter" value="0.5 -0.3 0.8"/>
    <predictors spec="RealParameter" value="2.0 1.5 -1.0"/>
    <indicators spec="BooleanParameter" value="true true false"/>
</distribution>
```

This gives:
- Linear predictor: η = 1.0 + 0.5×2.0 + (-0.3)×1.5 + 0×0.8×(-1.0) = 1.55
- Mean with log link: λ = exp(1.55) ≈ 4.71

## 4. Binomial GLM with Variable Selection

```xml
<distribution spec="glmprior.util.GLMDistribution" family="BINOMIAL" link="LOGIT">
    <intercept spec="RealParameter" value="0.0"/>
    <coefficients spec="RealParameter" value="1.2 -0.8 0.6"/>
    <predictors spec="RealParameter" value="0.5 1.0 -0.3"/>
    <indicators spec="BooleanParameter" value="true false true"/>
    <nTrials spec="RealParameter" value="10"/>
</distribution>
```

This gives:
- Linear predictor: η = 0.0 + 1.2×0.5 + 0×(-0.8)×1.0 + 0.6×(-0.3) = 0.42
- Probability with logit link: p = exp(0.42)/(1+exp(0.42)) ≈ 0.603

## 5. Advanced: Hierarchical Variable Selection

```xml
<run spec="MCMC" chainLength="2000000">
    
    <!-- GLM with hierarchical priors on indicators -->
    <distribution spec="glmprior.util.GLMDistribution" family="NORMAL" id="glm">
        <intercept spec="RealParameter" id="intercept" value="0.0"/>
        <coefficients spec="RealParameter" id="coefficients" dimension="10"
                     value="0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0"/>
        <predictors spec="RealParameter" id="predictors" dimension="10"
                   value="1.0 -0.5 2.0 0.3 -1.2 0.8 1.5 -0.7 0.9 -1.8"/>
        <indicators spec="BooleanParameter" id="indicators" dimension="10"
                   value="false false false false false false false false false false"/>
        <sigma spec="RealParameter" id="sigma" value="1.0" lower="0.0"/>
    </distribution>

    <distribution spec="CompoundDistribution" id="prior">
        
        <!-- Hierarchical prior: inclusion probability -->
        <distribution spec="Prior">
            <x spec="RealParameter" id="inclusionProb" value="0.3" lower="0.0" upper="1.0"/>
            <distr spec="Beta" alpha="1.0" beta="2.0"/>
        </distribution>
        
        <!-- Prior on indicators depends on inclusion probability -->
        <distribution spec="Prior">
            <x idref="indicators"/>
            <distr spec="Bernoulli" p="@inclusionProb"/>
        </distribution>
        
        <!-- Spike-and-slab prior on coefficients -->
        <!-- When indicator = true: Normal(0, tau_large) -->
        <!-- When indicator = false: coefficients should be 0 (handled by indicators) -->
        <distribution spec="Prior">
            <x idref="coefficients"/>
            <distr spec="Normal" mean="0.0" sigma="2.0"/>
        </distribution>
        
        <!-- Other priors... -->
        <distribution spec="Prior">
            <x idref="intercept"/>
            <distr spec="Normal" mean="0.0" sigma="10.0"/>
        </distribution>
        
        <distribution spec="Prior">
            <x idref="sigma"/>
            <distr spec="LogNormal" meanInRealSpace="false" M="0.0" S="1.0"/>
        </distribution>
        
    </distribution>

    <!-- Operators -->
    <operator spec="ScaleOperator" parameter="@coefficients" weight="3.0"/>
    <operator spec="ScaleOperator" parameter="@sigma" weight="1.0"/>
    <operator spec="RealRandomWalkOperator" parameter="@intercept" weight="1.0"/>
    <operator spec="BitFlipOperator" parameter="@indicators" weight="8.0"/>
    <operator spec="ScaleOperator" parameter="@inclusionProb" weight="2.0"/>

    <!-- Loggers -->
    <logger spec="Logger" logEvery="1000" fileName="hierarchical_selection.log">
        <log idref="prior"/>
        <log idref="likelihood"/>
        <log idref="intercept"/>
        <log idref="coefficients"/>
        <log idref="sigma"/>
        <log idref="indicators"/>
        <log idref="inclusionProb"/>
        
        <!-- Log number of active variables -->
        <log spec="Sum" arg="@indicators" id="numActiveVars"/>
    </logger>
    
</run>
```

## 6. Variable Selection with Different Link Functions

### Gamma GLM with Inverse Link
```xml
<distribution spec="glmprior.util.GLMDistribution" family="GAMMA">
    <intercept spec="RealParameter" value="2.0"/>
    <coefficients spec="RealParameter" value="0.5 -0.3 1.0"/>
    <predictors spec="RealParameter" value="1.0 2.0 0.5"/>
    <indicators spec="BooleanParameter" value="true false true"/>
    <shape spec="RealParameter" value="3.0"/>
</distribution>
```

### Gamma GLM with Log Link (Non-canonical)
```xml
<distribution spec="glmprior.util.GLMDistribution" family="GAMMA" link="LOG">
    <intercept spec="RealParameter" value="1.0"/>
    <coefficients spec="RealParameter" value="0.2 0.3 -0.1"/>
    <predictors spec="RealParameter" value="2.0 1.0 3.0"/>
    <indicators spec="BooleanParameter" value="false true true"/>
    <shape spec="RealParameter" value="2.5"/>
</distribution>
```

## 7. Practical Tips for Variable Selection

### A. Initialization Strategy
```xml
<!-- Start with sparse model (most indicators false) -->
<indicators spec="BooleanParameter" value="false false false true false true false"/>

<!-- Or start with dense model (most indicators true) -->
<indicators spec="BooleanParameter" value="true true true false true false true"/>
```

### B. Operator Weights
```xml
<!-- Give higher weight to indicator flipping for good mixing -->
<operator spec="BitFlipOperator" parameter="@indicators" weight="10.0"/>

<!-- Moderate weights for coefficient updates -->
<operator spec="ScaleOperator" parameter="@coefficients" weight="3.0"/>
```

### C. Prior Considerations
```xml
<!-- Informative prior favoring sparsity -->
<distribution spec="Prior">
    <x idref="indicators"/>
    <distr spec="Bernoulli" p="0.2"/>  <!-- 20% inclusion probability -->
</distribution>

<!-- Or neutral prior -->
<distribution spec="Prior">
    <x idref="indicators"/>
    <distr spec="Bernoulli" p="0.5"/>  <!-- 50% inclusion probability -->
</distribution>
```

## 8. Post-Processing Variable Selection Results

After running MCMC, you can analyze the results:

```R
# Read BEAST2 log file
log_data <- read.table("glm_variable_selection.log", header=TRUE)

# Calculate inclusion probabilities for each variable
inclusion_probs <- sapply(1:5, function(i) {
    col_name <- paste0("indicators.", i-1)  # BEAST2 uses 0-based indexing in logs
    mean(log_data[[col_name]])
})

print("Inclusion probabilities:")
print(inclusion_probs)

# Variables with >50% inclusion probability are "selected"
selected_vars <- which(inclusion_probs > 0.5)
print(paste("Selected variables:", paste(selected_vars, collapse=", ")))

# Plot inclusion probabilities
barplot(inclusion_probs, names.arg=paste("Var", 1:5), 
        xlab="Variable", ylab="Inclusion Probability",
        main="Bayesian Variable Selection Results")
abline(h=0.5, col="red", lty=2)  # 50% threshold line
```

## Key Features of GLM Indicator Framework

1. **Type Safety**: Uses `BooleanParameter` instead of `RealParameter` for indicators
2. **Automatic Integration**: Works seamlessly with existing BEAST2 operators (`BitFlipOperator`)
3. **Multiple Distributions**: Compatible with all GLM families (Normal, Poisson, Binomial, Gamma, etc.)
4. **Multiple Link Functions**: Works with any valid family-link combination
5. **Flexible Priors**: Supports both fixed and hierarchical priors on inclusion probabilities
6. **Efficient Computation**: Variables with false indicators are excluded from linear predictor calculation

This framework provides a powerful and flexible approach to Bayesian variable selection within the BEAST2 ecosystem!