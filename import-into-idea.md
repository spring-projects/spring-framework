The following has been tested against Intellij IDEA 12.0

## Steps

_Within your locally cloned spring-framework working directory:_

1. Generate IDEA metadata with `./gradlew :spring-oxm:compileTestJava cleanIdea idea`
2. Import into IDEA as usual
3. Set the Project JDK as appropriate
4. Add git support
5. Code away

## Known issues

1. Those steps don't work currently for Intellij IDEA 13+
2. `spring-aspects` does not compile out of the box due to references to aspect types unknown to IDEA.
See http://youtrack.jetbrains.com/issue/IDEA-64446 for details. In the meantime, the 'spring-aspects'
module has been excluded from the overall project to avoid compilation errors.
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
