# GLMPrior (in development)

`GLMPrior` is a package designed to define flexible generalized linear model (GLM) prior distributions for any parameter in a BEAST2 phylodynamic analysis. Due to some interface limitations in BEAST2, it currently only works with packages that accept parameters of class `Function`. For example, [BDMM-Prime](https://github.com/tgvaughan/BDMM-Prime/tree/master) package for (structured) birth-dath-sampling analyses with constant rates or rates changing over time. 

Below you will find information on building GLMPrior from source, citation and license. For general information on the package, installation and set-up instructions, visit project web page:

[cecivale.github.io/GLMPrior/](https://cecivale.github.io/GLMPrior/)

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

