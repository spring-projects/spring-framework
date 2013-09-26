@echo off
set STS_TEST_VERSION='2.9.2.RELEASE'

set CURRENT_DIR=%~dp0
cd %CURRENT_DIR%

cls

echo.
echo -----------------------------------------------------------------------
echo Spring Framework Eclipse/STS project import guide
echo.
echo This script will guide you through the process of importing the
echo Spring Framework sources into Eclipse/STS. It is recommended that you
echo have a recent version of the SpringSource Tool Suite (this script has
echo been tested against STS %STS_TEST_VERSION%), but at the minimum you will
echo need Eclipse + AJDT.
echo.
echo If you need to download and install STS, please do that now by
echo visiting http://spring.io/tools/sts/all 
echo.
echo Otherwise, press enter and we'll begin.

pause

REM this command:
REM - wipes out any existing Eclipse metadata
REM - generates OXM test classes to avoid errors on import into Eclipse
REM - generates metadata for all subprojects
REM - skips metadata gen for the root project (-x :eclipse) to work
REM   around Eclipse's inability to import hierarchical project structures
REM SET COMMAND="./gradlew cleanEclipse :spring-oxm:compileTestJava eclipse -x :eclipse"
SET COMMAND=gradlew cleanEclipse :spring-oxm:compileTestJava eclipse -x :eclipse

echo.
echo -----------------------------------------------------------------------
echo STEP 1: Generate subproject Eclipse metadata
echo. 
echo The first step will be to generate Eclipse project metadata for each
echo of the spring-* subprojects. This happens via the built-in
echo "Gradle wrapper" script (./gradlew in this directory). If this is your
echo first time using the Gradle wrapper, this step may take a few minutes
echo while a Gradle distribution is downloaded for you.
echo. 
echo The command run will be:
echo. 
echo     %COMMAND%
echo. 
echo Press enter when ready.

pause

call %COMMAND%
if not "%ERRORLEVEL%" == "0" exit /B %ERRORLEVEL%

echo.
echo -----------------------------------------------------------------------
echo STEP 2: Import subprojects into Eclipse/STS
echo.
echo Within Eclipse/STS, do the following:
echo. 
echo File ^> Import... ^> Existing Projects into Workspace
echo      ^> When prompted for the 'root directory', provide %CURRENT_DIR%
echo      ^> Press enter. You will see the modules show up under "Projects"
echo      ^> All projects should be selected/checked. Click Finish.
echo      ^> When the project import is complete, you should have no errors.
echo.
echo When the above is complete, return here and press the enter key.

pause

set COMMAND=gradlew :eclipse

echo.
echo -----------------------------------------------------------------------
echo STEP 3: generate root project Eclipse metadata
echo. 
echo Unfortunately, Eclipse does not allow for importing project
echo hierarchies, so we had to skip root project metadata generation in the
echo during step 1. In this step we simply generate root project metadata
echo so you can import it in the next step.
echo. 
echo The command run will be:
echo. 
echo     %COMMAND%
echo. 
echo Press the enter key when ready.
pause

call %COMMAND%
if not "%ERRORLEVEL%" == "0" exit /B %ERRORLEVEL%

echo.
echo -----------------------------------------------------------------------
echo STEP 4: Import root project into Eclipse/STS
echo. 
echo Follow the project import steps listed in step 2 above to import the
echo root project.
echo. 
echo Press enter when complete, and move on to the final step.

pause

echo.
echo -----------------------------------------------------------------------
echo STEP 5: Enable Git support for all projects
echo. 
echo - In the Eclipse/STS Package Explorer, select all spring* projects.
echo - Right-click to open the context menu and select Team ^> Share Project...
echo - In the Share Project dialog that appears, select Git and press Next
echo - Check "Use or create repository in parent folder of project"
echo - Click Finish
echo. 
echo When complete, you'll have Git support enabled for all projects.
echo. 
echo You're ready to code! Goodbye!

