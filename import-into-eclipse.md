# Spring Framework - Eclipse/STS Project Import Guide

This document will guide you through the process of importing the Spring Framework
projects into Eclipse or the Spring Tool Suite (STS). It is recommended that you have a
recent version of Eclipse or STS. As a bare minimum you will need Eclipse with full Java
8 support, the AspectJ Development Tools (AJDT), and the Groovy Compiler.

The following instructions have been tested against
[Spring Tool Suite](https://spring.io/tools) (_STS_) 3.9.4 and 4.0.0.M11 with
[Eclipse Buildship](http://projects.eclipse.org/projects/tools.buildship) (Eclipse
Plug-ins for Gradle). The instructions should work with the latest Eclipse distribution
as long as you install
[Buildship](https://marketplace.eclipse.org/content/buildship-gradle-integration). Note
that STS 4 comes with Buildship preinstalled.

## Steps

_Within your locally cloned `spring-framework` working directory:_

1. Precompile `spring-oxm` with `./gradlew :spring-oxm:compileTestJava`
2. Import into Eclipse (File -> Import -> Gradle -> Existing Gradle Project -> Navigate
   to directory -> Select Finish)
3. If prompted, exclude the `spring-aspects` module (or after the import by closing or
   deleting the project)
4. In the `spring-oxm` project, add the `jaxb` folder in
   `build/generated-sources` to the build path (right click on them and select
   `Build Path -> Use as Source Folder`)
5. To apply project specific settings run `./gradlew eclipseBuildship`
7. Code away

## Known Issues

1. `spring-core` and `spring-oxm` should be pre-compiled due to repackaged dependencies.
  - See `*RepackJar` tasks in the build.
2. `spring-aspects` does not compile due to references to aspect types unknown to Eclipse.
  - If you install [AJDT](https://www.eclipse.org/ajdt/downloads/) into Eclipse it should
    work.
3. While JUnit tests pass from the command line with Gradle, some may fail when run from
   the IDE.
  - Resolving this is a work in progress.
  - If attempting to run all JUnit tests from within the IDE, you will likely need to set
    the following VM options to avoid out of memory errors:
	`-XX:MaxPermSize=2048m -Xmx2048m -XX:MaxHeapSize=2048m`

## Tips

In any case, please do not check in your own generated `.classpath` file, `.project`
file, or `.settings` folder. You'll notice these files are already intentionally in
`.gitignore`. The same policy holds for IDEA metadata.
