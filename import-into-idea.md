The following has been tested against IntelliJ IDEA 2016.2.2

## Steps

_Within your locally cloned spring-framework working directory:_

1. Import into IntelliJ IDEA (File -> New -> Project from Existing Sources -> Select directory -> Select Gradle -> Use gradle wrapper task configuration 
2. Set the Project JDK as appropriate (1.8+)
3. Code away

## Known issues for IntelliJ IDEA 13.1

1. `spring-oxm` should be pre-compiled since it's using repackaged dependencies (see *RepackJar tasks). You can precompile `spring-oxm` with `./gradlew cleanIdea :spring-oxm:compileTestJava`
2. `spring-aspects` does not compile out of the box due to references to aspect types unknown to
IntelliJ IDEA. See http://youtrack.jetbrains.com/issue/IDEA-64446 for details. In the meantime, the
'spring-aspects' should be excluded from the overall project to avoid compilation errors. To exclude go to File-> Project Structure -> Modules
3. While all JUnit tests pass from the command line with Gradle, many will fail when run from
IntelliJ IDEA. Resolving this is a work in progress. If attempting to run all JUnit tests from within
IntelliJ IDEA, you will likely need to set the following VM options to avoid out of memory errors:
    -XX:MaxPermSize=2048m -Xmx2048m -XX:MaxHeapSize=2048m


## Tips

In any case, please do not check in your own generated .iml, .ipr, or .iws files.
You'll notice these files are already intentionally in .gitignore. The same policy goes for eclipse metadata.

## FAQ

Q. What about IntelliJ IDEA's own [Gradle support](http://confluence.jetbrains.net/display/IDEADEV/Gradle+integration)?

A. Keep an eye on http://youtrack.jetbrains.com/issue/IDEA-53476
