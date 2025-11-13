# Installation

GLMPrior depends on the following BEAST2 packages:

* [BDMM-Prime](https://github.com/tgvaughan/BDMM-Prime)
* [feast](https://github.com/tgvaughan/feast)

When you install through BEAUti or build from source they will be downloaded automatically. When installing by hand, keep in mind to install the above packages first. 

## From BEAUti

1. Launch BEAUti.
2. Select `File -> Manage packages`.
3. In the new window at the bottom menu, select `Package repositories`.
4. Click `Add URL`, paste the link [https://raw.githubusercontent.com/cecivale/GLMPrior/refs/heads/main/package.xml](https://raw.githubusercontent.com/cecivale/GLMPrior/refs/heads/main/package.xml) and click `OK`.
5. Now close the Package Repository Manager window. You should now see GLMPrior package listed among the other packages.
6. Select the GLMPrior package and click `Install\Upgrade`.
7. In order to use the combined BDMM-Prime and GLMPrior template you will need to restart BEAUti.

## By Hand

Download the ZIP from the [GLMPrior GitHub releases](https://github.com/cecivale/GLMPrior/releases), then unzip and install following the instructions in [BEAST2 Package Management](https://www.beast2.org/managing-packages/#:~:text=If%20for%20some%20reason%20you,zip%20inside%20the%20VSS%20directory).  

Currently, to use the BEAUti interface of GLMPrior with BDMM-Prime, you need to manually clear the BEAUti class path after manual installation of GLMPrior package. To do so:
1. Launch BEAUti
2. Select `File -> Clear class path`
3. Restart BEAUti and you should now be able to use combined GLM and BDMM-Prime template.

## Building from Source Code

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
