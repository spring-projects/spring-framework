STS_TEST_VERSION='2.9.2.RELEASE'

cd `dirname $0`
clear
cat <<EOM

-----------------------------------------------------------------------
Spring Framework Eclipse/STS project import guide

This script will guide you through the process of importing the
Spring Framework sources into Eclipse/STS. It is recommended that you
have a recent version of the SpringSource Tool Suite (this script has
been tested against STS $STS_TEST_VERSION), but at the minimum you will
need Eclipse + AJDT.

If you need to download and install STS, please do that now by
visiting http://springsource.org/downloads/sts

Otherwise, press enter and we'll begin.
EOM

read

# this command:
# - wipes out any existing Eclipse metadata
# - generates OXM test classes to avoid errors on import into Eclipse
# - generates metadata for all subprojects
# - skips metadata gen for the root project (-x :eclipse) to work
#   around Eclipse's inability to import hierarchical project structures
COMMAND="./gradlew cleanEclipse :spring-oxm:compileTestJava eclipse -x :eclipse"

cat <<EOM

-----------------------------------------------------------------------
STEP 1: Generate subproject Eclipse metadata

The first step will be to generate Eclipse project metadata for each
of the spring-* subprojects. This happens via the built-in
"Gradle wrapper" script (./gradlew in this directory). If this is your
first time using the Gradle wrapper, this step may take a few minutes
while a Gradle distribution is downloaded for you.

The command run will be:

    $COMMAND

Press enter when ready.
EOM

read

$COMMAND || exit

cat <<EOM

-----------------------------------------------------------------------
STEP 2: Import subprojects into Eclipse/STS

Within Eclipse/STS, do the following:

File > Import... > Existing Projects into Workspace
   > When prompted for the 'root directory', provide $PWD
   > Press enter. You will see the modules show up under "Projects"
   > All projects should be selected/checked. Click Finish.
   > When the project import is complete, you should have no errors.

When the above is complete, return here and press the enter key.
EOM

read

COMMAND="./gradlew :eclipse"

cat <<EOM

-----------------------------------------------------------------------
STEP 3: generate root project Eclipse metadata

Unfortunately, Eclipse does not allow for importing project
hierarchies, so we had to skip root project metadata generation in the
during step 1. In this step we simply generate root project metadata
so you can import it in the next step.

The command run will be:

    $COMMAND

Press the enter key when ready.
EOM

read

$COMMAND || exit

cat <<EOM
-----------------------------------------------------------------------
STEP 4: Import root project into Eclipse/STS

Follow the project import steps listed in step 2 above to import the
root project.

Press enter when complete, and move on to the final step.
EOM

read

cat <<EOM
-----------------------------------------------------------------------
STEP 5: Enable Git support for all projects

- In the Eclipse/STS Package Explorer, select all spring* projects.
- Right-click to open the context menu and select Team > Share Project...
- In the Share Project dialog that appears, select Git and press Next
- Check "Use or create repository in parent folder of project"
- Click Finish

When complete, you'll have Git support enabled for all projects.

You're ready to code! Goodbye!
EOM
