Spring Reactive is a sandbox for experimenting on the reactive support intended to be part
of Spring Framework 5. For more information about this topic, you can have a look to
[Intro to Reactive programming][] and [Reactive Web Applications][] talks.

## Downloading Artifacts
Spring Reactive JAR dependency is available from Spring snapshot repository:
 - Repository URL: `https://repo.spring.io/snapshot/`
 - GroupId: `org.springframework.reactive`
 - ArtifactId: `spring-reactive`
 - Version: `0.1.0.BUILD-SNAPSHOT`
 
## Documentation
See the current [Javadoc][] and [reference docs][].
 
## Sample application
[Spring Reactive Playground] is a sample application based on Spring Reactive and on MongoDB,
Couchbase and PostgreSQL Reactive database drivers.

## Building from Source
Spring Reactive uses a [Gradle][]-based build system. In the instructions
below, `./gradlew` is invoked from the root of the source tree and serves as
a cross-platform, self-contained bootstrap mechanism for the build.

You can check the current build status on this [Bamboo Spring Reactive build][].

### Prerequisites

[Git][] and [JDK 8 update 20 or later][JDK8 build]

Be sure that your `JAVA_HOME` environment variable points to the `jdk1.8.0` folder
extracted from the JDK download.

### Install all spring-\* jars into your local Maven cache
`./gradlew install`

### Compile and test; build all jars, distribution zips, and docs
`./gradlew build`

## Contributing
Feel free to send us your feedback on the [issue tracker][]; [Pull requests][] are welcome.

## License
The Spring Reactive is released under version 2.0 of the [Apache License][].


[Spring Reactive Playground]: https://github.com/sdeleuze/spring-reactive-playground
[Gradle]: http://gradle.org
[Bamboo Spring Reactive build]: https://build.spring.io/browse/SR-PUB
[Git]: http://help.github.com/set-up-git-redirect
[JDK8 build]: http://www.oracle.com/technetwork/java/javase/downloads
[Intro to Reactive programming]: http://fr.slideshare.net/StphaneMaldini/intro-to-reactive-programming-52821416
[Reactive Web Applications]: http://fr.slideshare.net/rstoya05/reactive-web-applications
[issue tracker]: https://github.com/spring-projects/spring-reactive/issues
[Pull requests]: http://help.github.com/send-pull-requests
[Apache License]: http://www.apache.org/licenses/LICENSE-2.0
