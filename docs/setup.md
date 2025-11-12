# Model Specification using BEAUti

    ⚠️ Note:
    The GLMPrior package and its integration in BEAUti are under active development and not yet fully stable. Errors may occur, and unexpected behavior is possible.
    Please follow the setup instructions exactly in the order provided, and avoid navigating back and forth between steps in BEAUti. 
    Doing so may break the setup or result in an invalid XML file. 
    If issues arise, restarting the BEAUti setup from scratch is recommended.

## Setup Instructions

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


## Format Requirements for Predictor Files in BEAUTi (only for BDMM-Prime)

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

