=========================================
== Spring PetClinic sample application ==
=========================================

@author Ken Krebs
@author Juergen Hoeller
@author Rob Harrop
@author Costin Leau
@author Sam Brannen
@author Scott Andrews

--------------------------------------------------------------------------------


1. DATA ACCESS STRATEGIES

PetClinic features alternative DAO implementations and application
configurations for JDBC, Hibernate and JPA, with HSQLDB and MySQL as target
databases. The default PetClinic configuration is JDBC on HSQLDB. See
"src/jdbc.properties", "war/WEB-INF/web.xml" and
"war/WEB-INF/applicationContext-*.xml" for details. A simple comment change in
"web.xml" switches between the data access strategies.

The JDBC and Hibernate versions of PetClinic also demonstrate JMX support via
the use of "<context:mbean-export/>" for exporting MBeans. SimpleJdbcClinic
exposes the SimpleJdbcClinicMBean management interface via JMX through the use
of the @ManagedResource and @ManagedOperation annotations; whereas, the
HibernateStatistics service is exposed via JMX through auto-detection of the
service MBean. You can start up the JDK's JConsole to manage the exported bean.

The Spring distribution comes with all required Hibernate and TopLink Essentials
(JPA RI) JAR files to be able to build and run PetClinic on those two ORM tools.

All data access strategies can work with JTA for transaction management, by
activating the JtaTransactionManager and a JndiObjectFactoryBean that refers to
a transactional container DataSource. The default for JDBC is
DataSourceTransactionManager; for Hibernate, HibernateTransactionManager; for
JPA, JpaTransactionManager. Those local strategies allow for working with any
locally defined DataSource.

Note that in the default case, the sample configurations for Hibernate and JPA
specify Spring's non-pooling DriverManagerDataSource as a local DataSource.
You can change the DataSource definition to a Commons DBCP BasicDataSource to
get proper connection pooling. See "war/WEB-INF/applicationContext-jdbc.xml" for
an example.

--------------------------------------------------------------------------------


2. BUILD AND DEPLOYMENT

This directory contains the web app source. For deployment, it needs to be built
with Apache Ant 1.6 or higher.

Run "build.bat" in this directory for available targets (e.g. "build.bat build",
"build.bat warfile"). You can use "warfile.bat" as a shortcut for WAR file
creation. The WAR file will be created in the "dist" directory.

You can also invoke an existing installation of Ant with this directory as the
execution directory. Note that you must do this in order to execute the "tests"
target, as you need the JUnit task from Ant's optional.jar, which is not
included in this sample application.

By default, an embedded HSQL instance in configured.  No other steps are 
necessary to get the data source up and running.

For MySQL, you'll need to use the corresponding schema and SQL scripts in the
"db/mysql" subdirectory. Follow the steps outlined in
"db/mysql/petclinic_db_setup_mysql.txt" for explicit details.

In you intend to use a local DataSource, the JDBC settings can be adapted in
"src/jdbc.properties". To use a JTA DataSource, you need to set up corresponding
DataSources in your Java EE container.

Notes on enabling Log4J:
 - Log4J is disabled by default, due to JBoss issues
 - Uncomment the Log4J listener in "WEB-INF/web.xml"

--------------------------------------------------------------------------------


3. JPA ON TOMCAT

Notes on using the Java Persistence API (JPA) on Apache Tomcat 4.x or higher,
with a persistence provider that requires class instrumentation (such as TopLink
Essentials):

To use JPA class instrumentation, Tomcat has to be instructed to use a custom
class loader which supports instrumentation. See the JPA section of the Spring
reference manual for complete details.

The basic steps are:
 - Copy "spring-tomcat-weaver.jar" from the Spring distribution to
   "TOMCAT_HOME/server/lib".
 - If you're running on Tomcat 5.x, modify "TOMCAT_HOME/conf/server.xml"
   and add a new "<Context>" element for 'petclinic' (see below). You can 
   alternatively deploy the WAR including "META-INF/context.xml" from this 
   sample application's "war" directory, in which case you will need to
   uncomment the Loader element in that file to enable the use of the
   TomcatInstrumentableClassLoader.

<Context path="/petclinic" docBase="/petclinic/location" ...>
  <!-- please note that useSystemClassLoaderAsParent is available since Tomcat 5.5.20; remove it if previous versions are being used -->
  <Loader loaderClass="org.springframework.instrument.classloading.tomcat.TomcatInstrumentableClassLoader" useSystemClassLoaderAsParent="false"/>
  ...
</Context>
