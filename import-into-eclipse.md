# Spring Framework - Eclipse/STS Project Import Guide

This document will guide you through the process of importing the Spring Framework
projects into Eclipse or the Spring Tool Suite (_STS_). It is recommended that you
have a recent version of Eclipse. As a bare minimum you will need Eclipse with full Java
8 support, Eclipse Buildship, and the Groovy plugin.

The following instructions have been tested against [STS](https://spring.io/tools) 4.12.0
([download](https://github.com/spring-projects/sts4/wiki/Previous-Versions#spring-tools-4120-changelog))
(based on Eclipse 4.21) with [Eclipse Buildship](https://projects.eclipse.org/projects/tools.buildship).
The instructions should work with the latest Eclipse distribution as long as you install
[Buildship](https://marketplace.eclipse.org/content/buildship-gradle-integration). Note
that STS 4 comes with Buildship preinstalled.

## Steps

_When instructed to execute `./gradlew` from the command line, be sure to execute it within your locally cloned `spring-framework` working directory._

1. Install the [Groovy Development Tools](https://marketplace.eclipse.org/content/groovy-development-tools).
1. Switch to Groovy 3.0 in Eclipse (Preferences &#8594; Groovy &#8594; Compiler &#8594; Switch to 3.0...).
   - If you encounter build errors stating something similar to _"Groovy: compiler mismatch: project level is 2.5, workspace level is 3.0"_, change the Groovy compiler version to 3.0 for each affected project.
1. Ensure that the _Forbidden reference (access rule)_ in Eclipse is set to `Info`
(Preferences &#8594; Java &#8594; Compiler &#8594; Errors/Warnings &#8594; Deprecated and restricted API &#8594; Forbidden reference (access rule)).
1. Optionally install the [Kotlin Plugin for Eclipse](https://marketplace.eclipse.org/content/kotlin-plugin-eclipse) if you need to execute Kotlin-based tests or develop Kotlin extensions.
   - **NOTE**: As of September 21, 2021, it appears that the Kotlin Plugin for Eclipse does not yet work with Eclipse 4.21.
1. Optionally install the [AspectJ Development Tools](https://marketplace.eclipse.org/content/aspectj-development-tools) (_AJDT_) if you need to work with the `spring-aspects` project.
   - **NOTE**: As of September 21, 2021, it appears that the AspectJ Development Tools do not yet work with Eclipse 4.21.
1. Optionally install the [TestNG plugin](https://testng.org/doc/eclipse.html) in Eclipse if you need to execute individual TestNG test classes or tests in the `spring-test` module.
   - As an alternative to installing the TestNG plugin, you can execute the `org.springframework.test.context.testng.TestNGTestSuite` class as a "JUnit 5" test class in Eclipse.
1. Build `spring-oxm` from the command line with `./gradlew :spring-oxm:check`.
1. To apply Spring Framework specific settings, run `./gradlew cleanEclipse eclipse` from the command line.
1. Import all projects into Eclipse (File &#8594; Import &#8594; Gradle &#8594; Existing Gradle Project &#8594; Navigate to the locally cloned `spring-framework` directory &#8594; Select Finish).
   - If you have not installed AJDT, exclude the `spring-aspects` project from the import, if prompted, or close it after the import.
   - If you run into errors during the import, you may need to set the _Java home_ for Gradle Buildship to the location of your JDK 8 installation in Eclipse (Preferences &#8594; Gradle &#8594; Java home).
1. If you need to execute JAXB-related tests in the `spring-oxm` project and wish to have the generated sources available, add the `build/generated-sources/jaxb` folder to the build path (right click on the `jaxb` folder and select "Build Path &#8594; Use as Source Folder").
   - If you do not see the `build` folder in the `spring-oxm` project, ensure that the "Gradle build folder" is not filtered out from the view. This setting is available under "Filters" in the configuration of the Package Explorer (available by clicking on the _three vertical dots_ in the upper right corner of the Package Explorer).
1. Code away!

## Known Issues

1. `spring-core` should be pre-compiled due to repackaged dependencies.
   - See `*RepackJar` tasks in the `spring-core.gradle` build file.
1. `spring-oxm` should be pre-compiled due to JAXB types generated for tests.
   - Note that executing `./gradlew :spring-oxm:check` as explained in the _Steps_ above will compile `spring-core` and generate JAXB types for `spring-oxm`.
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
