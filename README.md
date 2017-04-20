# Spring源码中文翻译
## Spring Framework
Spring 提供了 a comprehensive programming and configuration
model for modern Java-based enterprise applications - on any kind of deployment
platform. A key element of Spring is infrastructural support at the application
level: Spring focuses on the "plumbing" of enterprise applications so that teams
can focus on application-level business logic, without unnecessary ties to
specific deployment environments.

The framework also serves as the foundation for [Spring Integration][], [Spring
Batch][] and the rest of the Spring [family of projects][]. Browse the repositories
under the [Spring organization][] on GitHub for a full list.

## 下载
See [downloading Spring artifacts][] for Maven repository information. Unable to
use Maven or other transitive dependency management tools? See [building a
distribution with dependencies][].

## 文档
See the current [Javadoc][] and [reference docs][].

## 获得支持
Check out the [Spring forums][] and the [spring][spring tag] and
[spring-mvc][spring-mvc tag] tags on [Stack Overflow][]. [Commercial support][]
is available too.

## Issue Tracking
Report issues via the [Spring Framework JIRA]. Understand our issue management
process by reading about [the lifecycle of an issue][]. Think you've found a
bug? Please consider submitting a reproduction project via the
[spring-framework-issues][] GitHub repository. The [readme][] there provides
simple step-by-step instructions.

## Building from Source
The Spring Framework uses a [Gradle][]-based build system. In the instructions
below, [`./gradlew`][] is invoked from the root of the source tree and serves as
a cross-platform, self-contained bootstrap mechanism for the build.

### 前提

[Git][] and [JDK 8 update 20 or later][JDK8 build]

Be sure that your `JAVA_HOME` environment variable points to the `jdk1.8.0` folder
extracted from the JDK download.

### 下载源码
`git clone git@github.com:spring-projects/spring-framework.git`

### 导入源文件到IDE
Run `./import-into-eclipse.sh` or read `import-into-idea.md` as appropriate.
> **Note:** Per the prerequisites above, ensure that you have JDK 8 configured properly in your IDE.

### Install all spring-\* jars into your local Maven cache
`./gradlew install`

### 编译和测试; build all jars, distribution zips, and docs
`./gradlew build`

... and discover more commands with `./gradlew tasks`. See also the [Gradle
build and release FAQ][].


## Staying in Touch
Follow [@SpringCentral][] as well as [@SpringFramework][] and its [team members][]
on Twitter. In-depth articles can be found at [The Spring Blog][], and releases
are announced via our [news feed][].

## 其他Spring
* Spring Integration: https://github.com/spring-projects/spring-integration
* Spring Batch: https://github.com/spring-projects/spring-batch
* family of projects: http://spring.io/projects
* Spring organization: https://github.com/spring-projects
* downloading Spring artifacts: https://github.com/spring-projects/spring-framework/wiki/Downloading-Spring-artifacts
 * building a distribution with dependencies: https://github.com/spring-projects/spring-framework/wiki/Building-a-distribution-with-dependencies
* Javadoc: http://docs.spring.io/spring-framework/docs/current/javadoc-api/
* reference docs: http://docs.spring.io/spring-framework/docs/current/spring-framework-reference/
* Spring forums: http://forum.spring.io/
* Stack Overflow: http://stackoverflow.com/faq
* Commercial support: http://spring.io/services
* Spring Framework JIRA: https://jira.spring.io/browse/SPR
* the lifecycle of an issue: https://github.com/spring-projects/spring-framework/wiki/The-Lifecycle-of-an-Issue
* spring-framework-issues: https://github.com/spring-projects/spring-framework-issues#readme
* readme: https://github.com/spring-projects/spring-framework-issues#readme
* Gradle: http://gradle.org
* `./gradlew`: http://vimeo.com/34436402
* Git]: http://help.github.com/set-up-git-redirect
* JDK8 build: http://www.oracle.com/technetwork/java/javase/downloads
* Gradle build and release FAQ: https://github.com/spring-projects/spring-framework/wiki/Gradle-build-and-release-FAQ
* Pull requests: http://help.github.com/send-pull-requests
* contributor guidelines: https://github.com/spring-projects/spring-framework/blob/master/CONTRIBUTING.md
* @SpringFramework: https://twitter.com/springframework
* @SpringCentral: https://twitter.com/springcentral
* team members: https://twitter.com/springframework/lists/team/members
* The Spring Blog: http://spring.io/blog/
* news feed: http://spring.io/blog/category/news
* Apache License: http://www.apache.org/licenses/LICENSE-2.0

## 目录说明
