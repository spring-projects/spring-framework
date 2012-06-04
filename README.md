## Spring Framework
The Spring Framework provides a comprehensive programming and configuration model for modern
Java-based enterprise applications - on any kind of deployment platform. A key element of Spring is
infrastructural support at the application level: Spring focuses on the "plumbing" of enterprise
applications so that teams can focus on application-level business logic, without unnecessary ties
to specific deployment environments.

The framework also serves as the foundation for
[Spring Integration](https://github.com/SpringSource/spring-integration),
[Spring Batch](https://github.com/SpringSource/spring-batch) and the rest of the Spring
[family of projects](http://springsource.org/projects). Browse the repositories under the
[SpringSource organization](https://github.com/SpringSource) on GitHub for a full list.

[.NET](https://github.com/SpringSource/spring-net) and
[Python](https://github.com/SpringSource/spring-python) variants are available as well.

## Downloading artifacts
Instructions on
[downloading Spring artifacts](https://github.com/SpringSource/spring-framework/wiki/Downloading-Spring-artifacts)
via Maven and other build systems are available via the project wiki.

## Documentation
See the current [Javadoc](http://static.springsource.org/spring-framework/docs/current/api)
and [Reference docs](http://static.springsource.org/spring-framework/docs/current/reference).

## Getting support
Check out the [Spring forums](http://forum.springsource.org) and the
[Spring tag](http://stackoverflow.com/questions/tagged/spring) on StackOverflow.
[Commercial support](http://springsource.com/support/springsupport) is available too.

## Issue Tracking
Spring's JIRA issue tracker can be found [here](http://jira.springsource.org/browse/SPR). Think
you've found a bug? Please consider submitting a reproduction project via the
[spring-framework-issues](https://github.com/SpringSource/spring-framework-issues) repository. The
[readme](https://github.com/SpringSource/spring-framework-issues#readme) provides simple
step-by-step instructions.  <a name="building_from_source"/>

## Building from source
The Spring Framework uses a [Gradle](http://gradle.org)-based build system. In the instructions
below, [`./gradlew`](http://vimeo.com/34436402) is invoked from the root of the source tree and
serves as a cross-platform, self-contained bootstrap mechanism for the build. The only
prerequisites are [git](http://help.github.com/set-up-git-redirect) and JDK 1.6+.

### check out sources
`git clone git://github.com/SpringSource/spring-framework.git`

### compile and test, build all jars, distribution zips and docs
`./gradlew build`

### install all spring-\* jars into your local Maven cache
`./gradlew install`

### import sources into your IDE
Run `./import-into-eclipse.sh` or read `import-into-idea.md` as appropriate.

... and discover more commands with `./gradlew tasks`. See also the
[Gradle build and release FAQ](https://github.com/SpringSource/spring-framework/wiki/Gradle-build-and-release-FAQ).

## Contributing
[Pull requests](http://help.github.com/send-pull-requests) are welcome; see the
[contributor guidelines](https://github.com/SpringSource/spring-framework/wiki/Contributor-guidelines).

## Staying in touch
Follow [@springframework](http://twitter.com/springframework) and its
[team members](http://twitter.com/springframework/team/members) on Twitter. In-depth articles can be
found at the SpringSource [team blog](http://blog.springsource.org), and releases are announced via
our [news feed](http://www.springsource.org/news-events).

## License
The Spring Framework is released under version 2.0 of the
[Apache License](http://www.apache.org/licenses/LICENSE-2.0).
