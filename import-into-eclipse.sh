cd `dirname $0`
clear
cat <<EOM

------------------------------------------------------------------------
Spring Framework - Eclipse/STS Project Import Guide

This script will guide you through the process of importing the Spring
Framework projects into Eclipse or the Spring Tool Suite (STS). It is
recommended that you have a recent version of Eclipse or STS. As a bare
minimum you will need Eclipse with full Java 8 support, the AspectJ
Development Tools (AJDT), and the Groovy Compiler.

This script has been tested against:

- STS:     3.8.3.RELEASE (Eclipse Neon.2 4.6.2)
- AJDT:    2.2.4.201612122115 (Eclipse Neon 4.6)

If you need to download and install Eclipse or STS, please do that now
by visiting one of the following sites:

- Eclipse downloads: http://download.eclipse.org/eclipse/downloads
- STS downloads: http://spring.io/tools/sts/all
- STS nightly builds: http://dist.springsource.com/snapshot/STS/nightly-distributions.html
- ADJT: http://www.eclipse.org/ajdt/downloads/
- Groovy Eclipse: https://github.com/groovy/groovy-eclipse/wiki

Once Eclipse/STS is installed, press enter, and we'll begin.
EOM

read

# this command:
# - wipes out any existing Eclipse metadata
# - generates OXM test classes to avoid errors on import into Eclipse
# - generates metadata for all subprojects
# - skips metadata gen for the root project (-x :eclipse) to work
#   around Eclipse's inability to import hierarchical project structures
COMMAND="./gradlew --no-daemon cleanEclipse :spring-oxm:compileTestJava eclipse -x :eclipse"

cat <<EOM

------------------------------------------------------------------------
STEP 1: Generate subproject Eclipse metadata

The first step will be to generate Eclipse project metadata for each of
the spring-* subprojects. This happens via the built-in "Gradle wrapper"
script (./gradlew in this directory). If this is your first time using
the Gradle wrapper, this step may take a few minutes while a Gradle
distribution is downloaded for you.

The command run will be:

    $COMMAND

Press enter when ready.
EOM

read

$COMMAND || exit

cat <<EOM

------------------------------------------------------------------------
STEP 2: Import subprojects into Eclipse/STS

Within Eclipse/STS, do the following:

- File > Import... > Existing Projects into Workspace
- When prompted for the 'root directory', provide $PWD.
- Press enter. You will see the modules show up under "Projects".
- All projects should be selected/checked. Click Finish.
- When the project import is complete, you should have no errors.

When the above is complete, return here and press the enter key.
EOM

read

COMMAND="./gradlew --no-daemon :eclipse"

cat <<EOM

------------------------------------------------------------------------
STEP 3: Generate root project Eclipse metadata

Unfortunately, Eclipse does not support importing project hierarchies,
so we had to skip root project metadata generation during step 1. In
this step we simply generate root project metadata so that you can
import it in the next step.

The command run will be:

    $COMMAND

Press the enter key when ready.
EOM

read

$COMMAND || exit

cat <<EOM
------------------------------------------------------------------------
STEP 4: Import root project into Eclipse/STS

Follow the project import steps listed in step 2 above to import the
root "spring" project.

Press enter when complete, and move on to the final step.
EOM

read

cat <<EOM
------------------------------------------------------------------------
STEP 5: Enable Git support for all projects

- In the Eclipse/STS Package Explorer, select all spring* projects.
- Right-click to open the context menu and select Team > Share Project...
- In the Share Project dialog that appears, select Git and press Next.
- Check "Use or create repository in parent folder of project".
- Click Finish.

When complete, you'll have Git support enabled for all projects.

You're ready to code! Goodbye!
EOM
