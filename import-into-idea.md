The following has been tested against Intellij IDEA 13.1

## Steps

_Within your locally cloned spring-framework working directory:_

1. Pre-compile `spring-oxm` with `./gradlew cleanIdea :spring-oxm:compileTestJava`
2. Import into IDEA (File->import project->import from external model->Gradle)
3. Set the Project JDK as appropriate (1.8+)
4. Exclude the `spring-aspects` module (Go to File->Project Structure->Modules)
5. Code away

## Known issues

1. `spring-oxm` should be pre-compiled since it's using repackaged dependencies (see *RepackJar tasks)
2. `spring-aspects` does not compile out of the box due to references to aspect types unknown to IDEA.
See http://youtrack.jetbrains.com/issue/IDEA-64446 for details. In the meantime, the 'spring-aspects'
should be excluded from the overall project to avoid compilation errors.
3. While all JUnit tests pass from the command line with Gradle, many will fail when run from IDEA.
Resolving this is a work in progress. If attempting to run all JUnit tests from within IDEA, you will
likely need to set the following VM options to avoid out of memory errors:
    -XX:MaxPermSize=2048m -Xmx2048m -XX:MaxHeapSize=2048m

## Tips

In any case, please do not check in your own generated .iml, .ipr, or .iws files.
You'll notice these files are already intentionally in .gitignore. The same policy goes for eclipse metadata.

## FAQ

Q. What about IDEA's own [Gradle support](http://confluence.jetbrains.net/display/IDEADEV/Gradle+integration)?

A. Keep an eye on http://youtrack.jetbrains.com/issue/IDEA-53476
