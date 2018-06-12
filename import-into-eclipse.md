The following has been tested against STS 4.0.0.M11, which ships with Buildship (the Gradle tooling from Gradleware). It should work with the latest Eclipse distro as long as you install Buildship.

## Steps

_Within your locally cloned spring-framework working directory:_

1. Precompile `spring-oxm` with `./gradlew :spring-oxm:compileTestJava`
2. Import into Eclipse (File -> Import -> Gradle -> Existing Gradle Project -> Navigate to directory -> Select Finish)
3. If prompted, exclude the `spring-aspects` module (or after the import by deleting the project)
4. In the `spring-oxm` project, add the two folders (`castor` and `jaxb`) in `build/generated-sources` to the build path (right click on them and select `Build Path -> Use as Source Folder`)
5. If your workspace default JDK is Java 8, add a Java 9 (or 10) JDK, and update the build path in `spring-beans` and `spring-core` to use it instead of the default
6. Code away

## Known issues

1. `spring-core` and `spring-oxm` should be pre-compiled due to repackaged dependencies.
See `*RepackJar` tasks in the build).
2. `spring-aspects` does not compile due to references to aspect types unknown to
Eclipse. If you install AJDT into Eclipse it should work.
3. While JUnit tests pass from the command line with Gradle, some may fail when run from
the IDE. Resolving this is a work in progress. If attempting to run all JUnit tests from within
the IDE, you will likely need to set the following VM options to avoid out of memory errors:
    -XX:MaxPermSize=2048m -Xmx2048m -XX:MaxHeapSize=2048m

## Tips

In any case, please do not check in your own generated .classpath, .project or .settings.
You'll notice these files are already intentionally in .gitignore. The same policy goes for IDEA metadata.
