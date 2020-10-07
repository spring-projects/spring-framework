## Spring Boot - Custom Application Properties

**Problem:** 

1. Need the app to be configurable, no hard coding of the values
2. Need to read app configuration from a properties file

**Solution:** Application Properties File **(application.properties)**

- By default, Spring Boot reads information from a standard properties file
  - Located at: **src/main/resources/application.properties** (standard spring boot file name)
- We can define any custome properties in this file
- Spring Boot app can access properties using **@Value** 
- No additional coding or configuration required



#### Development Process:

- **Step 1:** Define custom properties in **application.properties**

- **Step 2:** Inject properties into Spring Boot application using **@Value**

  

  **Step 1:**

  > **application.properties**
  >
  > ```properties
  > #
  > # Define custom properties
  > #
  > coach.name = Micky mouse
  > team.name = Khulna Club
  > ```

  **Step 2:**

  ```java
  @RestController
  public class FunRestController {
  	
      // inject properties for: coach.name, team.name
      @Value("$(coach.name)")
      private String coachName;
      
      @Value("$(team.name)")
      private String teamName;
      ....
  }
  ```



#### Spring Boot Properties

- Don't let the 1000+ properties overwhelm you

- The properties are roughly grouped into the following catagories

  a

**Core Properties**

```properties
# Log levels severity mapping

# Set logging level based on package name
logging.level.org.springframwork = DEBUG
logging.level.org.hibernate = TRACE
logging.level.org.touhidjisan = INFO

# Log file name
logging.file = my-crazy-staff.log
```

**Web Properties**

```properties
# HTTP server port
server.port = 7070 

# Context path of the application
# default context path /
server.servlet.context-path = /my-silly-app 
#from last to props => "http://localhost:7070/my-silly-app"


# Default HTTP Session time out
server.servlet.session.timeout = 15m
```

**Actuator Properties**

```properties
# Use wildcards "*" to expose all endpoints
# Can also expose individual endpoints with a comma-delimited list
management.endpoints.web.exposure.include=*

# Exclude individual endpoints with a comma-delimited list
management.endpoints.web.exposure.exclude = health, info

# Base path for actuator endpoints
management.endpoints.web.base-path = /actuator
```

**Security Properties**

```properties
# Default user name 
spring.security.user.name = admin

# Password for default user
spring.security.user.password = topsecret
```

**Data Properties**

```properties
# JDBC URL of the database
spring.datasource.url = "jdbc:mysql://localhost3306/ecommerce"

# Login usename of the database
spring.datasource.username = hbstudent

# Login password of the database
spring.datasource.password = hbstudent
```

