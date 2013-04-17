
## Maven Snapshots

Maven snapshots of this branch are available through the Spring snapshot repository:

    <repository>
        <id>spring-snapshots</id>
        <url>http://repo.springsource.org/snapshot</url>
        <snapshots><enabled>true</enabled></snapshots>
        <releases><enabled>false</enabled></releases>
    </repository>

Use version `4.0.0.WEBSOCKET-SNAPSHOT`, for example: 

    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>4.0.0.WEBSOCKET-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-web</artifactId>
        <version>4.0.0.WEBSOCKET-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-websocket</artifactId>
        <version>4.0.0.WEBSOCKET-SNAPSHOT</version>
    </dependency>



### Tomcat

Tomcat provides early JSR-356 support. You'll need to build the latest source, which is relatively easy to do.

Check out Tomcat trunk:
    mkdir tomcat
    cd tomcat
    svn co http://svn.apache.org/repos/asf/tomcat/trunk/
    cd trunk

Create `build.properties` in the trunk directory with content similar to the one below:
    # ----- Default Base Path for Dependent Packages -----
    # Replace this path with the path where dependencies binaries should be downloaded
    base.path=~/dev/sources/apache/tomcat/download

Run the ant build:
    ant clean
    ant

A usable Tomcat installation can be found in `output/build`

### Glassfish

Glassfish also provides JSR-356 support based on Tyrus (the reference implementation).

Download a [Glassfish 4 build](http://dlc.sun.com.edgesuite.net/glassfish/4.0/) (e.g. glassfish-4.0-b84.zip from the promoted builds)

Unzip the downloaded file.

Start the server:
    cd <unzip_dir>/glassfish4
    bin/asadmin start-domain

Deploy a WAR file. Here is [a sample script](https://github.com/rstoyanchev/spring-websocket-test/blob/master/redeploy-glassfish.sh).

Watch the logs:
    cd <unzip_dir>/glassfish4
    less `glassfish/domains/domain1/logs/server.log`


