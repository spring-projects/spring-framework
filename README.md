## Spring Framework
The Spring Framework provides a comprehensive programming and configuration
model for modern Java-based enterprise applications - on any kind of deployment
platform. A key element of Spring is infrastructural support at the application
level: Spring focuses on the "plumbing" of enterprise applications so that teams
can focus on application-level business logic, without unnecessary ties to
specific deployment environments.

The framework also serves as the foundation for [Spring Integration][], [Spring
Batch][] and the rest of the Spring [family of projects][]. Browse the
repositories under the [SpringSource organization][] on GitHub for a full list.

## Downloading artifacts
See [downloading Spring artifacts][] for Maven repository information. Unable to
use Maven or other transitive dependency management tools? See [building a
distribution with dependencies][].

## Documentation
See the current [Javadoc][] and [reference docs][].

## Getting support
Check out the [Spring forums][] and the [spring][spring tag] and
[spring-mvc][spring-mvc tag] tags on [Stack Overflow][]. [Commercial support][]
is available too.

## Issue Tracking
Report issues via the [Spring Framework JIRA]. Understand our issue management
process by reading about [the lifecycle of an issue][]. Think you've found a
bug? Please consider submitting a reproduction project via the
[spring-framework-issues][] GitHub repository. The [readme][] there provides
simple step-by-step instructions.

## Building from source
The Spring Framework uses a [Gradle][]-based build system. In the instructions
below, [`./gradlew`][] is invoked from the root of the source tree and serves as
a cross-platform, self-contained bootstrap mechanism for the build.

### prerequisites

[Git][] and [Early Access build of OpenJDK 1.8 build 88][JDK18 build 88].

### check out sources
`git clone git://github.com/SpringSource/spring-framework.git`

### import sources into your IDE
Run `./import-into-eclipse.sh` or read `import-into-idea.md` as appropriate.

### install all spring-\* jars into your local Maven cache
`./gradlew install`

### compile and test, build all jars, distribution zips and docs
`./gradlew build`

... and discover more commands with `./gradlew tasks`. See also the [Gradle
build and release FAQ][].

## Contributing
[Pull requests][] are welcome; see the [contributor guidelines][] for details.

## Staying in touch
Follow [@springframework][] and its [team members][] on Twitter. In-depth
articles can be found at the SpringSource [team blog][], and releases are
announced via our [news feed][].

## License
The Spring Framework is released under version 2.0 of the [Apache License][].

[Spring Integration]: https://github.com/SpringSource/spring-integration
[Spring Batch]: https://github.com/SpringSource/spring-batch
[family of projects]: http://springsource.org/projects
[SpringSource organization]: https://github.com/SpringSource
[downloading Spring artifacts]: https://github.com/SpringSource/spring-framework/wiki/Downloading-Spring-artifacts
[building a distribution with dependencies]: https://github.com/SpringSource/spring-framework/wiki/Building-a-distribution-with-dependencies
[Javadoc]: http://static.springsource.org/spring-framework/docs/current/javadoc-api
[reference docs]: http://static.springsource.org/spring-framework/docs/current/spring-framework-reference
[Spring forums]: http://forum.springsource.org
[spring tag]: http://stackoverflow.com/questions/tagged/spring
[spring-mvc tag]: http://stackoverflow.com/questions/tagged/spring-mvc
[Stack Overflow]: http://stackoverflow.com/faq
[Commercial support]: http://springsource.com/support/springsupport
[Spring Framework JIRA]: http://jira.springsource.org/browse/SPR
[the lifecycle of an issue]: https://github.com/cbeams/spring-framework/wiki/The-Lifecycle-of-an-Issue
[spring-framework-issues]: https://github.com/SpringSource/spring-framework-issues#readme
[readme]: https://github.com/SpringSource/spring-framework-issues#readme
[Gradle]: http://gradle.org
[`./gradlew`]: http://vimeo.com/34436402
[Git]: http://help.github.com/set-up-git-redirect
[JDK18 build 88]: https://jdk8.java.net/archive/8-b88.html
[Gradle build and release FAQ]: https://github.com/SpringSource/spring-framework/wiki/Gradle-build-and-release-FAQ
[Pull requests]: http://help.github.com/send-pull-requests
[contributor guidelines]: https://github.com/SpringSource/spring-framework/blob/master/CONTRIBUTING.md
[@springframework]: http://twitter.com/springframework
[team members]: http://twitter.com/springframework/team/members
[team blog]: http://blog.springsource.org
[news feed]: http://www.springsource.org/news-events
[Apache License]: http://www.apache.org/licenses/LICENSE-2.0
