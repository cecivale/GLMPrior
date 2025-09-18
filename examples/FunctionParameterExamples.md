# FunctionParameter Usage Examples

The `FunctionParameter` class allows you to use `GLMPrior` and other `Function` implementations wherever a `RealParameter` is expected in BEAST2. This adapter pattern provides seamless integration with existing packages.

## 1. Basic GLM Prior as RealParameter

```xml
<!-- GLM Prior that computes values -->
<glmPrior spec="glmprior.util.GLMPrior" id="myGLM">
    <distribution spec="glmprior.util.GLMDistribution" family="NORMAL">
        <intercept spec="RealParameter" value="2.0"/>
        <coefficients spec="RealParameter" value="1.5 -0.8"/>
        <predictors spec="RealParameter" value="1.0 2.0"/>
        <sigma spec="RealParameter" value="1.0"/>
    </distribution>
</glmPrior>

<!-- Wrap it as a RealParameter -->
<parameter spec="glmprior.util.FunctionParameter" id="glmParameter">
    <function idref="myGLM"/>
</parameter>

<!-- Now use it anywhere a RealParameter is expected -->
<someDistribution spec="SomePackage.SomeDistribution">
    <data idref="glmParameter"/>
</someDistribution>
```

## 2. GLM with Variable Selection as RealParameter

```xml
<run spec="MCMC" chainLength="1000000">

    <!-- GLM with indicator variables -->
    <glmPrior spec="glmprior.util.GLMPrior" id="variableSelectionGLM">
        <distribution spec="glmprior.util.GLMDistribution" family="POISSON">
            <intercept spec="RealParameter" id="intercept" value="1.0"/>
            <coefficients spec="RealParameter" id="coefficients" 
                         value="0.0 0.0 0.0 0.0" dimension="4"/>
            <predictors spec="RealParameter" id="predictors" 
                       value="1.2 -0.5 2.1 0.8"/>
            <indicators spec="BooleanParameter" id="indicators" 
                       value="true false true false" dimension="4"/>
        </distribution>
    </glmPrior>

    <!-- Wrap as RealParameter for use in likelihood -->
    <parameter spec="glmprior.util.FunctionParameter" id="glmRates">
        <function idref="variableSelectionGLM"/>
    </parameter>

    <!-- Use in any likelihood that expects RealParameter -->
    <distribution spec="beast.base.evolution.likelihood.GenericTreeLikelihood">
        <data idref="alignment"/>
        <tree idref="tree"/>
        <siteModel spec="beast.base.evolution.sitemodel.SiteModel">
            <substModel spec="beast.base.evolution.substitutionmodel.JukesCantor"/>
            <shape idref="glmRates"/>  <!-- GLM-computed rates! -->
        </siteModel>
    </distribution>

    <!-- Priors and operators for GLM parameters -->
    <distribution spec="CompoundDistribution" id="prior">
        <distribution spec="Prior">
            <x idref="intercept"/>
            <distr spec="Normal" mean="0.0" sigma="10.0"/>
        </distribution>
        <distribution spec="Prior">
            <x idref="coefficients"/>
            <distr spec="Normal" mean="0.0" sigma="1.0"/>
        </distribution>
        <distribution spec="Prior">
            <x idref="indicators"/>
            <distr spec="Bernoulli" p="0.5"/>
        </distribution>
    </distribution>

    <!-- Operators -->
    <operator spec="ScaleOperator" parameter="@coefficients" weight="3.0"/>
    <operator spec="RealRandomWalkOperator" parameter="@intercept" weight="1.0"/>
    <operator spec="BitFlipOperator" parameter="@indicators" weight="5.0"/>

</run>
```

## 3. Multi-dimensional GLM Output

```xml
<!-- GLM that outputs multiple values (e.g., site-specific rates) -->
<glmPrior spec="glmprior.util.GLMPrior" id="siteRatesGLM">
    <distribution spec="glmprior.util.GLMDistribution" family="GAMMA">
        <intercept spec="RealParameter" value="0.0"/>
        <coefficients spec="RealParameter" value="0.2 -0.1 0.3"/>
        <predictors spec="RealParameter" value="1.0 -0.5 2.0 0.8 1.2 -1.0 1.5 0.3 -0.8"/>
        <shape spec="RealParameter" value="2.0"/>
    </distribution>
</glmPrior>

<!-- This will have dimension = predictors.length / coefficients.length = 9/3 = 3 -->
<parameter spec="glmprior.util.FunctionParameter" id="siteRates">
    <function idref="siteRatesGLM"/>
</parameter>

<!-- Use as site-specific rates -->
<distribution spec="beast.base.evolution.likelihood.GenericTreeLikelihood">
    <data idref="alignment"/>
    <tree idref="tree"/>
    <siteModel spec="beast.base.evolution.sitemodel.SiteModel">
        <substModel spec="beast.base.evolution.substitutionmodel.HKY">
            <frequencies spec="Frequencies" frequencies="0.25 0.25 0.25 0.25"/>
        </substModel>
        <shape idref="siteRates"/>  <!-- Site-specific rates from GLM -->
    </siteModel>
</distribution>
```

## 4. Conditional GLM (Function depending on other parameters)

```xml
<!-- GLM where predictors depend on other model parameters -->
<parameter spec="RealParameter" id="phyloFeatures" value="1.0 0.5 -0.3" dimension="3"/>

<glmPrior spec="glmprior.util.GLMPrior" id="conditionalGLM">
    <distribution spec="glmprior.util.GLMDistribution" family="NORMAL">
        <intercept spec="RealParameter" value="0.0"/>
        <coefficients spec="RealParameter" value="1.2 -0.8"/>
        <predictors idref="phyloFeatures"/>  <!-- Dynamic predictors -->
        <sigma spec="RealParameter" value="0.5"/>
    </distribution>
</glmPrior>

<parameter spec="glmprior.util.FunctionParameter" id="adaptiveParameter">
    <function idref="conditionalGLM"/>
</parameter>

<!-- When phyloFeatures changes, adaptiveParameter automatically updates -->
<distribution spec="SomeDistribution">
    <parameter idref="adaptiveParameter"/>
</distribution>
```

## 5. Hierarchical Model with GLM Components

```xml
<run spec="MCMC" chainLength="2000000">

    <!-- Level 1: Environmental predictors -->
    <parameter spec="RealParameter" id="environment" 
               value="0.5 -1.2 0.8 1.1 -0.3" dimension="5"/>

    <!-- Level 2: GLM for population parameters -->
    <glmPrior spec="glmprior.util.GLMPrior" id="populationGLM">
        <distribution spec="glmprior.util.GLMDistribution" family="NORMAL">
            <intercept spec="RealParameter" id="popIntercept" value="2.0"/>
            <coefficients spec="RealParameter" id="popCoeffs" value="0.3 -0.2"/>
            <predictors idref="environment"/>
            <sigma spec="RealParameter" id="popSigma" value="0.8"/>
        </distribution>
    </glmPrior>

    <!-- Convert to RealParameter -->
    <parameter spec="glmprior.util.FunctionParameter" id="populationParams">
        <function idref="populationGLM"/>
    </parameter>

    <!-- Level 3: Use in demographic model -->
    <distribution spec="beast.base.evolution.tree.coalescent.Coalescent">
        <populationModel spec="beast.base.evolution.tree.coalescent.ConstantPopulation">
            <popSize idref="populationParams"/>
        </populationModel>
        <treeIntervals idref="treeIntervals"/>
    </distribution>

    <!-- Priors -->
    <distribution spec="CompoundDistribution" id="prior">
        <distribution spec="Prior">
            <x idref="environment"/>
            <distr spec="Normal" mean="0.0" sigma="2.0"/>
        </distribution>
        <distribution spec="Prior">
            <x idref="popIntercept"/>
            <distr spec="Normal" mean="0.0" sigma="5.0"/>
        </distribution>
        <distribution spec="Prior">
            <x idref="popCoeffs"/>
            <distr spec="Normal" mean="0.0" sigma="1.0"/>
        </distribution>
        <distribution spec="Prior">
            <x idref="popSigma"/>
            <distr spec="LogNormal" meanInRealSpace="false" M="0.0" S="1.0"/>
        </distribution>
    </distribution>

    <!-- Operators -->
    <operator spec="ScaleOperator" parameter="@environment" weight="3.0"/>
    <operator spec="RealRandomWalkOperator" parameter="@popIntercept" weight="1.0"/>
    <operator spec="ScaleOperator" parameter="@popCoeffs" weight="2.0"/>
    <operator spec="ScaleOperator" parameter="@popSigma" weight="1.0"/>

</run>
```

## 6. Multiple GLM Components

```xml
<!-- GLM for substitution rates -->
<glmPrior spec="glmprior.util.GLMPrior" id="rateGLM">
    <distribution spec="glmprior.util.GLMDistribution" family="GAMMA">
        <intercept spec="RealParameter" value="0.0"/>
        <coefficients spec="RealParameter" value="0.5"/>
        <predictors spec="RealParameter" value="1.0"/>
        <shape spec="RealParameter" value="2.0"/>
    </distribution>
</glmPrior>

<!-- GLM for population sizes -->
<glmPrior spec="glmprior.util.GLMPrior" id="popGLM">
    <distribution spec="glmprior.util.GLMDistribution" family="NORMAL">
        <intercept spec="RealParameter" value="5.0"/>
        <coefficients spec="RealParameter" value="-0.3"/>
        <predictors spec="RealParameter" value="2.0"/>
        <sigma spec="RealParameter" value="1.0"/>
    </distribution>
</glmPrior>

<!-- Convert both to RealParameters -->
<parameter spec="glmprior.util.FunctionParameter" id="substitutionRate">
    <function idref="rateGLM"/>
</parameter>

<parameter spec="glmprior.util.FunctionParameter" id="populationSize">
    <function idref="popGLM"/>
</parameter>

<!-- Use in different parts of the model -->
<distribution spec="beast.base.evolution.likelihood.GenericTreeLikelihood">
    <clockModel spec="beast.base.evolution.branchratemodel.StrictClockModel">
        <clock.rate idref="substitutionRate"/>
    </clockModel>
    <!-- ... other components -->
</distribution>

<distribution spec="beast.base.evolution.tree.coalescent.Coalescent">
    <populationModel spec="beast.base.evolution.tree.coalescent.ConstantPopulation">
        <popSize idref="populationSize"/>
    </populationModel>
    <!-- ... other components -->
</distribution>
```

## Key Benefits

1. **Seamless Integration**: Use GLM computations anywhere `RealParameter` is expected
2. **Automatic Updates**: Values update automatically when GLM inputs change
3. **Type Safety**: Maintains BEAST2's type system while adding computed parameters
4. **No Code Changes**: Existing packages work without modification
5. **Performance**: Values computed on-demand, no unnecessary storage

## Important Notes

- **Read-Only**: `FunctionParameter` values cannot be set directly (they're computed)
- **Dynamic**: Values automatically update when underlying GLM parameters change
- **Dimension**: Dimension is determined by the underlying `Function`
- **Operators**: Apply operators to the underlying GLM parameters, not the `FunctionParameter`
- **Initialization**: The wrapped `Function` must be properly initialized

## Error Handling

```xml
<!-- This will throw an error - cannot set computed values -->
<operator spec="ScaleOperator" parameter="@glmParameter" weight="1.0"/>  <!-- WRONG -->

<!-- Instead, operate on the underlying GLM parameters -->
<operator spec="ScaleOperator" parameter="@coefficients" weight="1.0"/>  <!-- CORRECT -->
```

This adapter pattern provides a clean, type-safe way to integrate GLM computations into any BEAST2 model component that expects `RealParameter` inputs!