# Spring Framework build

This folder contains the custom plugins and conventions for the Spring Framework build.
They are declared in the `build.gradle` file in this folder.

## Build Conventions

### Compiler conventions

The `org.springframework.build.compile` applies the Java compiler conventions to the build.
By default, the build is compiling sources with the `1.8` source and target compatibility.
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

## Test sources

The `org.springframework.build.test-sources` updates `testCompile` dependencies to include
the test source sets of `project()` dependencies. This plugin is used in the Spring Framework build 
to share test utilities and fixtures amongst modules.