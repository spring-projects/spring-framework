This is the home of the Spring Framework that underlies all
[Spring projects](https://spring.io/projects). Collectively the Spring Framework and the
family of related Spring projects make up what we call "Spring".

Spring provides everything you need beyond the Java language to create enterprise
applications in a wide range of scenarios and architectures. Please read the
[Overview](https://docs.spring.io/spring/docs/current/spring-framework-reference/overview.html#spring-introduction)
section in the reference for a more complete introduction.

## Code of Conduct

This project is governed by the [Spring Code of Conduct](CODE_OF_CONDUCT.adoc).
By participating you are expected to uphold this code.
Please report unacceptable behavior to spring-code-of-conduct@pivotal.io.

## Artifacts

For Maven repository information see
[downloading Spring artifacts](https://github.com/spring-projects/spring-framework/wiki/Downloading-Spring-artifacts),
or if unable to use Maven or other transitive dependency management tools, see
[building a distribution with dependencies](https://github.com/spring-projects/spring-framework/wiki/Building-a-distribution-with-dependencies).

## Learn

The Spring Frameworks maintains
[reference documentation](http://docs.spring.io/spring-framework/docs/current/spring-framework-reference/),
Github [wiki pages](https://github.com/spring-projects/spring-framework/wiki), and an
[API reference](http://docs.spring.io/spring-framework/docs/current/javadoc-api/).

You can find guides and tutorials on [https://spring.io](https://spring.io/guides).

## Build from Source

The Spring Framework uses a [Gradle](http://gradle.org) build. In the instructions below,
the [Gradle Wrapper](http://vimeo.com/34436402) is invoked from the root of the source
tree and serves as a cross-platform, self-contained bootstrap mechanism for the build.

To build you will need [Git](http://help.github.com/set-up-git-redirect) and
[JDK 8 update 20 or later](http://www.oracle.com/technetwork/java/javase/downloads).
Be sure that your `JAVA_HOME` environment variable points to the `jdk1.8.0` folder
extracted from the JDK download.

Start by checking out the sources:
```
git clone git@github.com:spring-projects/spring-framework.git
```

To import into an IDE, ensure JDK 8 is configured. Then run `./import-into-eclipse.sh`
or read [import-into-idea.md](import-into-idea.md). For IntelliJ please do read the
instructions as a straight-up import will not work.

To compile, test, build all jars, distribution zips, and docs use:
```
./gradlew build
```

To install all spring-\* jars into your local Maven cache:
```
./gradlew install
```

Discover more commands:
```
./gradlew tasks
```

See also [CONTRIBUTING](CONTRIBUTING.md) and the
[Gradle build and release FAQ](https://github.com/spring-projects/spring-framework/wiki/Gradle-build-and-release-FAQ).

## Stay in Touch

Follow [@SpringCentral](https://twitter.com/springcentral),
[@SpringFramework](https://twitter.com/springframework), and its
[team members](https://twitter.com/springframework/lists/team/members) on Twitter.
In-depth articles can be found at [The Spring Blog](http://spring.io/blog/),
and releases are announced via our [news feed](http://spring.io/blog/category/news).

## License

The Spring Framework is released under version 2.0 of the
[Apache License](http://www.apache.org/licenses/LICENSE-2.0).
