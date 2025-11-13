# Introduction

This page provides a brief guide on how to use the `GLMPrior` class and how to set it up within BEAUti for BDMM-Prime analyses in BEAST2.

    ⚠️ Note:
    This package is still under active development and considered experimental. Some features may not work as expected, and errors can occur. 
    Please use with caution and report any issues to the developers.

## Overview

`GLMPrior` is a package designed to define flexible generalized linear model (GLM) prior distributions for any parameter in a BEAST2 phylodynamic analysis. The GLM has the following general form:

**μ = g⁻¹( g(μ₀) + δ₁·β₁·X₁ + δ₂·β₂·X₂ + ... + δₚ·βₚ·Xₚ + ε )**

Where:

- **μ** is the parameter being modeled (e.g., a rate or probability).
- **μ₀** is the baseline value on the natural scale, it can be estimated by the MCMC.
- **g(·)** is the link function: one of `log`, `logit`, or `identity` (default is `log`).
- **g⁻¹(·)** is its inverse.
- **X₁, X₂, ..., Xₚ** are **predictors**  (explanatory variables). These should be carefully selected and appropriately transformed. Scaling is generally recommended. Built-in options for log-transformation and standardization are available in the class (but disabled by default). The order of the elements in each predictor must match the order of the elements in the parameter (package dependent).
- **β₁, ..., βₚ** are **coefficients** describing the effect of each predictor. Estimated by the MCMC. They can be estimated by the MCMC.
- **δ₁, ..., δₚ** are optional **indicator variables** (0 or 1), enabling variable selection. If included, they can be estimated by the MCMC.
- **ε** is an optional **error term**, modeling residual variation. If included, they can be estimated by the MCMC.

> **Important:** The `GLMPrior` class is implemented as a **function** within the BEAST2 framework. This means that to use it for your parameter, the BEAST2 package you are applying it to must accept **functions** as inputs (not only `RealParameter` objects). Ensure compatibility before setup.
