This is where the master build that creates releases of Spring Framework resides.  The build system is based on spring-build, which is linked in using an SVN external to https://src.springframework.org/svn/spring-build.

USERS
- To build all Spring Framework projects, including samples:

    1. From this directory, run:
       ant
        
Build Pre-requisites:
- javac 1.6 or > must be in your system path
- ant 1.7 or > must be in your system path

DEVELOPERS
- To build a new Spring Framework distribution for release:

  1. Update the files containing the version number to reflect the new release version, if necessary.
	
         build.properties
         build-spring-framework/resources/readme.txt
         spring-framework/src/spring-framework-reference.xml

  2. From this directory, run:
	
         ant jar package
		
     The release archive will be created and placed in:
         target/artifacts