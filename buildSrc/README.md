# Spring Framework Build

This folder contains the custom plugins and conventions for the Spring Framework build.
They are declared in the `build.gradle` file in this folder.

## Build Conventions

The `org.springframework.build.conventions` plugin applies all conventions to the Framework build:

* Configuring the Java compiler, see `JavaConventions`
* Configuring the Kotlin compiler, see `KotlinConventions`
* Configuring testing in the build with `TestConventions` 


## Build Plugins

### Optional dependencies

The `org.springframework.build.optional-dependencies` plugin creates a new `optional`
Gradle configuration - it adds the dependencies to the project's compile and runtime classpath
but doesn't affect the classpath of dependent projects.
This plugin does not provide a `provided` configuration, as the native `compileOnly` and `testCompileOnly`
configurations are preferred.


### RuntimeHints Java Agent

The `spring-core-test` project module contributes the `RuntimeHintsAgent` Java agent.

The `RuntimeHintsAgentPlugin` Gradle plugin creates a dedicated `"runtimeHintsTest"` test task for each project.
This task will detect and execute [tests tagged](https://junit.org/junit5/docs/current/user-guide/#running-tests-build-gradle)
with the `"RuntimeHintsTests"` [JUnit tag](https://junit.org/junit5/docs/current/user-guide/#running-tests-tags).
In the Spring Framework test suite, those are usually annotated with the `@EnabledIfRuntimeHintsAgent` annotation.

By default, the agent will instrument all classes located in the `"org.springframework"` package, as they are loaded.
The `RuntimeHintsAgentExtension` allows to customize this using a DSL:

```groovy
// this applies the `RuntimeHintsAgentPlugin` to the project
plugins {
	id 'org.springframework.build.runtimehints-agent'
}

// You can configure the agent to include and exclude packages from the instrumentation process.
runtimeHintsAgent {
	includedPackages = ["org.springframework", "io.spring"]
	excludedPackages = ["org.example"]
}

dependencies {
    // to use the test infrastructure, the project should also depend on the "spring-core-test" module
	testImplementation(project(":spring-core-test"))
}
```

With this configuration, `./gradlew runtimeHintsTest` will run all tests instrumented by this java agent.
The global `./gradlew check` task depends on `runtimeHintsTest`.            

NOTE: the "spring-core-test" module doesn't shade "spring-core" by design, so the agent should never instrument
code that doesn't have "spring-core" on its classpath.