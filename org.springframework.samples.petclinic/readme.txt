==========================================================================
=== Spring PetClinic Sample Application
==========================================================================

@author Ken Krebs
@author Juergen Hoeller
@author Rob Harrop
@author Costin Leau
@author Sam Brannen
@author Scott Andrews

==========================================================================
=== Data Access Strategies
==========================================================================

PetClinic features alternative DAO implementations and application
configurations for JDBC, Hibernate, and JPA, with HSQLDB and MySQL as
target databases. The default PetClinic configuration is JDBC on HSQLDB.
See "src/main/resources/jdbc.properties" as well as web.xml and
applicationContext-*.xml in the "src/main/webapp/WEB-INF" folder for
details. A simple comment change in web.xml switches between the data
access strategies.

The JDBC and Hibernate versions of PetClinic also demonstrate JMX support
via the use of <context:mbean-export/> for exporting MBeans.
SimpleJdbcClinic exposes the SimpleJdbcClinicMBean management interface
via JMX through the use of the @ManagedResource and @ManagedOperation
annotations; whereas, the HibernateStatistics service is exposed via JMX
through auto-detection of the service MBean. You can start up the JDK's
JConsole to manage the exported bean.

All data access strategies can work with JTA for transaction management by
activating the JtaTransactionManager and a JndiObjectFactoryBean that
refers to a transactional container DataSource. The default for JDBC is
DataSourceTransactionManager; for Hibernate, HibernateTransactionManager;
for JPA, JpaTransactionManager. Those local strategies allow for working
with any locally defined DataSource.

Note that the sample configurations for JDBC, Hibernate, and JPA configure
a BasicDataSource from the Apache Commons DBCP project for connection
pooling.

==========================================================================
=== Build and Deployment
==========================================================================

The Spring PetClinic sample application is built using Spring Build, which
is a custom build solution based on Ant and Ivy for dependency management.
For deployment, the web application needs to be built with Apache Ant 1.6
or higher. When the project is first built, Spring Build will use Ivy to
automatically download all required dependencies. Thus the initial build
may take a few minutes depending on the speed of your Internet connection,
but subsequent builds will be much faster.

Available build commands:

- ant clean        --> cleans the project
- ant clean test   --> cleans the project and runs all tests
- ant clean jar    --> cleans the project and builds the WAR

After building the project with "ant clean jar", you will find the
resulting WAR file in the "target/artifacts" directory. By default, an
embedded HSQLDB instance in configured. No other steps are necessary to
get the data source up and running: you can simply deploy the built WAR
file directly to your Servlet container.

For MySQL, you'll need to use the corresponding schema and SQL scripts in
the "db/mysql" subdirectory. Follow the steps outlined in
"db/mysql/petclinic_db_setup_mysql.txt" for explicit details.

In you intend to use a local DataSource, the JDBC settings can be adapted
in "src/main/resources/jdbc.properties". To use a JTA DataSource, you need
to set up corresponding DataSources in your Java EE container.

Notes on enabling Log4J:
 - Log4J is disabled by default due to issues with JBoss.
 - Uncomment the Log4J listener in "WEB-INF/web.xml" to enable logging.

==========================================================================
=== JPA on Tomcat
==========================================================================

This section provides tips on using the Java Persistence API (JPA) on
Apache Tomcat 4.x or higher with a persistence provider that requires
class instrumentation (such as TopLink Essentials).

To use JPA class instrumentation, Tomcat has to be instructed to use a
custom class loader which supports instrumentation. See the JPA section of
the Spring reference manual for complete details.

The basic steps are:
 - Copy "org.springframework.instrument.tomcat-3.0.0.RELEASE.jar" from the
   Spring distribution to "TOMCAT_HOME/server/lib".
 - If you're running on Tomcat 5.x, modify "TOMCAT_HOME/conf/server.xml"
   and add a new "<Context>" element for 'petclinic' (see below). You can 
   alternatively deploy the WAR including "META-INF/context.xml" from this 
   sample application's "src/main/webapp" directory, in which case you
   will need to uncomment the Loader element in that file to enable the
   use of the TomcatInstrumentableClassLoader.

<Context path="/petclinic" docBase="/petclinic/location" ...>
  <!-- please note that useSystemClassLoaderAsParent is available since Tomcat 5.5.20; remove it if previous versions are being used -->
  <Loader loaderClass="org.springframework.instrument.classloading.tomcat.TomcatInstrumentableClassLoader" useSystemClassLoaderAsParent="false"/>
  ...
</Context>
