# Spring Framework Build

This folder contains the custom plugins and conventions for the Spring Framework build.
They are declared in the `build.gradle` file in this folder.

## Build Conventions

### Compiler conventions

The `org.springframework.build.compile` plubin applies the Java compiler conventions to the build.
By default, the build compiles sources with Java `1.8` source and target compatibility.
You can test a different source compatibility version on the CLI with a project property like:

```
./gradlew test -PjavaSourceVersion=11
```

## Build Plugins

## Optional dependencies

The `org.springframework.build.optional-dependencies` plugin creates a new `optional`
Gradle configuration - it adds the dependencies to the project's compile and runtime classpath
but doesn't affect the classpath of dependent projects.
This plugin does not provide a `provided` configuration, as the native `compileOnly` and `testCompileOnly`
configurations are preferred.

## API Diff

This plugin uses the [Gradle JApiCmp](https://github.com/melix/japicmp-gradle-plugin) plugin
to generate API Diff reports for each Spring Framework module. This plugin is applied once on the root
project and creates tasks in each framework module. Unlike previous versions of this part of the build,
there is no need for checking out a specific tag. The plugin will fetch the JARs we want to compare the
current working version with. You can generate the reports for all modules or a single module:

```
./gradlew apiDiff -PbaselineVersion=5.1.0.RELEASE
./gradlew :spring-core:apiDiff -PbaselineVersion=5.1.0.RELEASE
```      

The reports are located under `build/reports/api-diff/$OLDVERSION_to_$NEWVERSION/`.
