Building from Source
--------------------

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

