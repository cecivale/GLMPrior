# GLMPrior (in development)

This document provides a brief guide on how to use the `GLMPrior` class and how to set it up within BEAUti for BDMM-Prime analyses in BEAST2.

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


## Installation

### From BEAUTi

1. Launch BEAUTi.
2. Select `File -> Manage packages`.
3. In the new window at the bottom menu, select `Package repositories`.
4. Click `Add URL`, paste the link [https://raw.githubusercontent.com/jugne/stratigraphic-ranges/master/package.xml](https://raw.githubusercontent.com/cecivale/GLMPrior/refs/heads/main/package.xml) and click `OK`.
5. Now close the Package Repository Manager window. You should now see GLMPrior package listed among the other packages.
6. Select the GLMPrior package and click `Install\Upgrade`.
7. In order to use the combined BDMM-Prime and GLMPrior template you will need to restart BEAUTi.

### By hand

Download the ZIP from the [GLMPrior GitHub releases](https://github.com/cecivale/GLMPrior/releases), then unzip and install following the instructions in [BEAST2 Package Management](https://www.beast2.org/managing-packages/) — see the **Install by hand** section.  

Currently, to use the BEAUTi interface of GLMPrior with BDMM-Prime, you need to manually clear the BEAUTi class path after manual installation of GLMPrior package. To do so:
    1. Launch BEAUTi
    2. Select `File -> Clear class path`
    3. Restart BEAUTi and you should now be able to use combined GLM and BDMM-Prime template.

## Setup Instructions in BEAUTi (only for BDMM-Prime )

    ⚠️ Note:
    The GLMPrior package and its integration in BEAUti are under active development and not yet fully stable. Errors may occur, and unexpected behavior is possible.
    Please follow the setup instructions exactly in the order provided, and avoid navigating back and forth between steps in BEAUti. 
    Doing so may break the setup or result in an invalid XML file. 
    If issues arise, restarting the BEAUti setup from scratch is recommended.


1. Begin setting up your BDMM-Prime analysis as usual: load your alignment, set tip dates, substitution and clock models.
2. In the **Priors** panel, select the **BDMMPrime** tree prior, and set the tip types at the bottom.
3. Choose the parameterization:
   - **GLMEpiParameterization** (reproductive number, becoming uninfectious rate, sampling proportion), or
   - **GLMCanonicalParameterization** (birth, death, and sampling rates)  
   from the **Parameterization** dropdown.
4. For parameters without GLM prior, proceed as usual.
5. For parameters with GLM prior, follow these steps:  
   - Specify if the parameter has the same value across all types (“scalar values”, default) or varies (deselect scalar).  
   - Specify the number of change times; define change times and related specs.  
   - Tick **Use GLM** next to the parameter.  
   - Click **Load GLM predictors CSV** to select your predictor file(s).  
   - Click **Show Table** to check your predictors.  
   - Select/deselect **Estimate indicators** as appropriate. 
   - Select/deselect **Estimate error** as appropriate.  
   - Select/deselect **Log transform** and **Standardize** depending on your predictors.  
   - Define a sensible prior for **μ₀** (baseline on natural scale).  
   - Leave default priors for coefficients, indicators, and error unless you have reasons to change them.


### Format Requirements for Predictor Files in BEAUTi (only for BDMM-Prime )

- CSV format.
- One predictor per row (you may load one or multiple files; they append predictors).
- The order of predictor values must match the order of the parameter elements.  
  For BDMM-Prime: elements are ordered alphabetically by type, and if multiple epochs exist, order is by epoch then by type within each epoch.  See [BDMM-Prime documentation](https://tgvaughan.github.io/BDMM-Prime/#id-2-Model-specification-using-BEAUti-Skyline-parameters). Note this order varies for other BEAST2 packages.
- Rows must be named; columns may be named or unnamed.

*Examples*

- **1D parameter (e.g., multi-type transmission rate):**

```csv
example 1,6,4,3,5
```

- **2D parameter (e.g., skyline multi-type sampling rate):**

```csv
Predictor,Epoch 1,,,,,Epoch 3,,,,
,Type1,Type2,Type3,Type4,Type5,Type1,Type2,Type3,Type4,Type5
example,1,6,4,3,5,3,2,8,3,1
```

- **3D parameter (e.g., skyline migration rate):**
```csv
Predictor,Epoch 1,,,,,,,,,,,,,,,,,,,,Epoch 2,,,,,,,,,,,,,,,,,,,,Epoch 3,,,,,,,,,,,,,,,,,,,,Epoch 4,,,,,,,,,,,,,,,,,,,
,T1,,,,T2,,,,T3,,,,T4,,,,T5,,,,T1,,,,T2,,,,T3,,,,T4,,,,T5,,,,T1,,,,T2,,,,T3,,,,T4,,,,T5,,,,T1,,,,T2,,,,T3,,,,T4,,,,T5,,,
,T2,T3,T4,T5,T1,T3,T4,T5,T1,T2,T4,T5,T1,T2,T3,T5,T1,T2,T3,T4,T2,T3,T4,T5,T1,T3,T4,T5,T1,T2,T4,T5,T1,T2,T3,T5,T1,T2,T3,T4,T2,T3,T4,T5,T1,T3,T4,T5,T1,T2,T4,T5,T1,T2,T3,T5,T1,T2,T3,T4,T2,T3,T4,T5,T1,T3,T4,T5,T1,T2,T4,T5,T1,T2,T3,T5,T1,T2,T3,T4
example,-1.34,-1.35,-1.35,-1.34,-1.04,-0.09,-0.46,1.07,-1.04,-0.23,-0.45,1.22,-1.35,-0.36,-0.24,0.53,-1.24,0.22,0.53,-0.38,-1.31,-1.32,-1.35,-1.31,-0.94,0.40,0.52,1.56,-0.95,0.25,0.46,1.76,-1.34,0.62,0.68,1.69,-1.15,0.62,0.92,0.65,-1.23,-1.22,-1.28,-1.05,-0.33,0.41,0.56,1.53,-0.45,0.23,0.47,1.71,-0.57,0.66,0.72,1.73,-0.57,0.75,1.03,0.83,-1.24,-1.26,-1.30,-1.12,-0.28,0.41,0.60,1.58,-0.38,0.28,0.55,1.82,-0.51,0.68,0.74,1.79,-0.50,0.68,0.98,0.76
```


## Building from Source

The below information is largely copied from [BDMM-Prime repo](https://github.com/tgvaughan/BDMM-Prime).

To build GLMPrior from source you'll need the following to be installed:
- OpenJDK version 17 or greater
- A recent version of OpenJFX
- the Apache Ant build system

Once these are installed and in your execution path, issue the following
command from the root directory of this repository:

```sh
JAVA_FX_HOME=/path/to/openjfx/ ant
```
The package archive will be left in the `dist/` subdirectory.

Note that unless you already have a local copy of the latest
[BEAST 2 source](https://github.com/CompEvol/beast2)
in the directory `../beast2` and the latest
[BeastFX source](https://github.com/CompEvol/beastfx)
in the directory `../beastfx` relative to the GLMPrior root, the build
script will attempt to download them automatically. Also, other BEAST2 
packages that GLMPrior depends on will be downloaded. Thus, most builds
will require a network connection.


## Acknowledgements and Citations
> TODO

## License
GLMPrior is free software. It is distributed under the terms of version 3 of the GNU General Public License. A copy of this license should be found in the file COPYING located in the root directory of this repository. If this file is absent for some reason, it can also be retrieved from https://www.gnu.org/licenses.

