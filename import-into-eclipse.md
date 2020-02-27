# Spring Framework - Eclipse/STS Project Import Guide

This document will guide you through the process of importing the Spring Framework
projects into Eclipse or the Spring Tool Suite (_STS_). It is recommended that you
have a recent version of Eclipse. As a bare minimum you will need Eclipse with full Java
8 support, Eclipse Buildship, the Kotlin plugin, and the Groovy plugin.

The following instructions have been tested against [STS](https://spring.io/tools) 4.3.2
([download](https://github.com/spring-projects/sts4/wiki/Previous-Versions#spring-tools-432-changelog))
(based on Eclipse 4.12) with [Eclipse Buildship](https://projects.eclipse.org/projects/tools.buildship).
The instructions should work with the latest Eclipse distribution as long as you install
[Buildship](https://marketplace.eclipse.org/content/buildship-gradle-integration). Note
that STS 4 comes with Buildship preinstalled.

## Steps

_When instructed to execute `./gradlew` from the command line, be sure to execute it within your locally cloned `spring-framework` working directory._

1. Ensure that Eclipse launches with JDK 8.
   - For example, on Mac OS this can be configured in the `Info.plist` file located in the `Contents` folder of the installed Eclipse or STS application (e.g., the `Eclipse.app` file).
1. Install the [Kotlin Plugin for Eclipse](https://marketplace.eclipse.org/content/kotlin-plugin-eclipse) in Eclipse.
1. Install the [Eclipse Groovy Development Tools](https://github.com/groovy/groovy-eclipse/wiki) in Eclipse.
1. Switch to Groovy 2.5 (Preferences -> Groovy -> Compiler -> Switch to 2.5...) in Eclipse.
1. Change the _Forbidden reference (access rule)_ in Eclipse from Error to Warning
(Preferences -> Java -> Compiler -> Errors/Warnings -> Deprecated and restricted API -> Forbidden reference (access rule)).
1. Optionally install the [AspectJ Development Tools](https://marketplace.eclipse.org/content/aspectj-development-tools) (_AJDT_) if you need to work with the `spring-aspects` project. The AspectJ Development Tools available in the Eclipse Marketplace have been tested with these instructions using STS 4.5 (Eclipse 4.14).
1. Optionally install the [TestNG plugin](https://testng.org/doc/eclipse.html) in Eclipse if you need to execute TestNG tests in the `spring-test` module.
1. Build `spring-oxm` from the command line with `./gradlew :spring-oxm:check`.
1. To apply project specific settings, run `./gradlew eclipseBuildship` from the command line.
1. Import into Eclipse (File -> Import -> Gradle -> Existing Gradle Project -> Navigate to the locally cloned `spring-framework` directory -> Select Finish).
   - If you have not installed AJDT, exclude the `spring-aspects` project from the import, if prompted, or close it after the import.
   - If you run into errors during the import, you may need to set the _Java home_ for Gradle Buildship to the location of your JDK 8 installation in Eclipse (Preferences -> Gradle -> Java home).
1. If you need to execute JAXB-related tests in the `spring-oxm` project and wish to have the generated sources available, add the `build/generated-sources/jaxb` folder to the build path (right click on the `jaxb` folder and select `Build Path -> Use as Source Folder`).
   - If you do not see the `build` folder in the `spring-oxm` project, ensure that the "Gradle build folder" is not filtered out from the view. This setting is available under "Filters" in the configuration of the Package Explorer (available by clicking on the small downward facing arrow in the upper right corner of the Package Explorer).
1. Code away!

## Known Issues

1. `spring-core` and `spring-oxm` should be pre-compiled due to repackaged dependencies.
   - See `*RepackJar` tasks in the build.
1. `spring-aspects` does not compile due to references to aspect types unknown to Eclipse.
   - If you installed _AJDT_ into Eclipse it should work.
1. While JUnit tests pass from the command line with Gradle, some may fail when run from
   the IDE.
   - Resolving this is a work in progress.
   - If attempting to run all JUnit tests from within the IDE, you may need to set the following VM options to avoid out of memory errors: `-XX:MaxPermSize=2048m -Xmx2048m -XX:MaxHeapSize=2048m`

## Tips

In any case, please do not check in your own generated `.classpath` file, `.project`
file, or `.settings` folder. You'll notice these files are already intentionally in
`.gitignore`. The same policy holds for IDEA metadata.
