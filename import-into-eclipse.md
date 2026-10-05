# Spring Framework - Eclipse Project Import Guide

This document will guide you through the process of importing the Spring Framework
projects into Eclipse IDE or the Spring Tools for Eclipse. It is recommended that you
have a recent version of Eclipse. The build uses JDK 25 (see `.sdkmanrc`), so as a bare
minimum you will need Eclipse with full Java 25 support.

The following instructions have been tested against
[Spring Tools for Eclipse](https://spring.io/tools#eclipse) 5.4.0 (based on Eclipse IDE
4.41). The instructions should also work with the latest release of the
[Eclipse IDE for Java Developers](https://www.eclipse.org/downloads/packages/); Spring
Tools is not required.

## Steps

_When instructed to execute `./gradlew` from the command line, be sure to execute it
within your locally cloned `spring-framework` working directory._

1. Ensure that the _Forbidden reference (access rule)_ in Eclipse is set to `Info`
   (Preferences &#8594; Java &#8594; Compiler &#8594; Errors/Warnings &#8594; Deprecated
   and restricted API &#8594; Forbidden reference (access rule)).
1. Optionally install the
   [Kotlin Plugin for Eclipse](https://marketplace.eclipse.org/content/kotlin-plugin-eclipse)
   if you need to execute Kotlin-based tests or develop Kotlin extensions.
1. Optionally install the
   [AspectJ Development Tools](https://marketplace.eclipse.org/content/aspectj-development-tools)
   (_AJDT_) if you need to work with the `spring-aspects` project.
1. Optionally install the
   [TestNG plugin](https://marketplace.eclipse.org/content/testng-eclipse) in Eclipse if
   you need to execute individual TestNG test classes or tests in the `spring-test`
   module.
   - As an alternative to installing the TestNG plugin, you can execute the
     `org.springframework.test.context.testng.TestNGTestSuite` class as a "JUnit 6" test
     class in Eclipse.
1. Compile all main and test classes from the command line first with
   `./gradlew testClasses`. This pre-compiles `spring-core` and generates the JAXB types
   for `spring-oxm` (see _Known Issues_ below).
1. To apply Spring Framework specific settings, run `./gradlew cleanEclipse eclipse`
   from the command line.
1. Import all projects into Eclipse (File &#8594; Import &#8594; General &#8594; Existing
   Projects into Workspace &#8594; Navigate to the locally cloned `spring-framework`
   directory &#8594; Select Finish).
   - If you have not installed AJDT, exclude the `spring-aspects` project from the
     import, if prompted, or close it after the import.
1. Code away!

## Known Issues

1. `spring-core` should be pre-compiled due to repackaged dependencies.
   - See `*RepackJar` tasks in the `spring-core.gradle` build file.
1. `spring-oxm` should be pre-compiled due to JAXB types generated for tests.
   - Note that executing `./gradlew testClasses` as explained in the _Steps_ above will
     compile `spring-core` and generate JAXB types for `spring-oxm`.
1. `spring-aspects` does not compile due to references to aspect types unknown to Eclipse.
   - If you installed _AJDT_ into Eclipse it should work.
1. Since an Eclipse project supports only a single Java compliance level, `spring-core`
   is configured with a Java 24 baseline in Eclipse, which includes the Java 21 and
   Java 24 multi-release sources (`src/main/java21`, `src/main/java24`, and
   `src/test/java21`). Types overridden by those sources are excluded from
   `src/main/java`. Eclipse does not report usage of Java features newer than the
   project-wide Java 17 baseline; however, such usage will be caught by the Gradle build.
   - To work on the Java 17 or Java 21 variants instead, run
     `./gradlew eclipse -PeclipseJavaBaseline=17` (or `21`) and refresh the projects in
     Eclipse (or close and reopen them if the compiler settings are not updated).
1. While JUnit tests pass from the command line with Gradle, some may fail when run from
   the IDE.
   - Resolving this is a work in progress.
   - If attempting to run all JUnit tests from within the IDE, you may need to set the
     following VM option to avoid out of memory errors: `-Xmx2048m`
   - Tests run via Gradle are also configured with the following VM options (see
     `TestConventions` in `buildSrc`), which you may need to set in your Eclipse launch
     configuration as well: `--add-opens=java.base/java.lang=ALL-UNNAMED
     --add-opens=java.base/java.util=ALL-UNNAMED -Xshare:off`

## Tips

In any case, please do not check in your own generated `.classpath` file, `.project`
file, or `.settings` folder. You'll notice these files are already intentionally in
`.gitignore`. The same policy holds for IntelliJ IDEA metadata.
