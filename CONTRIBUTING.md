# Contributing to the Spring Framework

First off, thank you for taking the time to contribute! :+1: :tada:

The Spring Framework is released under the Apache 2.0 license.
If you would like to contribute something, or want to hack on the code, this document should help you get started.

### Table of Contents

* [Code of Conduct](#code-of-conduct)
* [Reporting Security Vulnerabilities](#reporting-security-vulnerabilities)
* [How to Contribute](#how-to-contribute)
  * [Ask Questions](#ask-questions)
  * [Create an Issue](#create-an-issue)
  * [Issue Lifecycle](#issue-lifecycle)
  * [Submit a Pull Request](#submit-a-pull-request)
  * [Commit Messages](#commit-messages)
  * [Participate in Reviews](#participate-in-reviews)
* [Build from Source](#build-from-source)
* [Source Code Style](#source-code-style)
* [Tests](#tests)
* [Reference Docs](#reference-docs)

### Code of Conduct

This project is governed by the [Spring Code of Conduct](https://github.com/spring-projects/spring-framework#coc-ov-file).
By participating you are expected to uphold this code.
Please report unacceptable behavior to spring-code-of-conduct@spring.io.

**We accept contributions created with the help of AI coding agents, but they must be carefully reviewed by a human who remains accountable for the quality of the contribution.**
Contributions submitted by GitHub accounts controlled by autonomous AI bots are forbidden and will result in permanent bans.

### Reporting Security Vulnerabilities

If you think you have found a security vulnerability in the Spring Framework, please **DO NOT** disclose it publicly until we've had a chance to fix it.
Please do not report security vulnerabilities using GitHub issues or pull requests.
Instead, see [SECURITY.md](SECURITY.md) and https://spring.io/security-policy to learn how to disclose them responsibly.

### How to Contribute

#### Ask Questions

If you have a question, check Stack Overflow using
[this list of tags](https://stackoverflow.com/questions/tagged/spring+or+spring-mvc+or+spring-aop+or+spring-jdbc+or+spring-transactions+or+spring-annotations+or+spring-jms+or+spring-el+or+spring-test+or+spring+or+spring-remoting+or+spring-orm+or+spring-jmx+or+spring-cache+or+spring-webflux?tab=Newest).
Find an existing discussion, or start a new one if necessary.

If you believe there is an issue, search through [existing issues](https://github.com/spring-projects/spring-framework/issues) and [pull requests](https://github.com/spring-projects/spring-framework/pulls), trying a few different ways to find discussions, past or current, that are related to the issue.
Reading those discussions helps you to learn about the issue, and helps us to make a decision.

#### Create an Issue

Reporting an issue or making a feature request is a great way to contribute.
Your feedback and the conversations that result from it provide a continuous flow of ideas.
However, before creating a ticket, please take the time to [ask and research](#ask-questions) first.

If you create an issue after a discussion on Stack Overflow, please provide a description in the issue instead of simply referring to Stack Overflow.
The issue tracker is an important place of record for design discussions and should be self-sufficient.

Once you're ready, create an issue on [GitHub](https://github.com/spring-projects/spring-framework/issues).
Please submit issues against [supported versions](https://spring.io/projects/spring-framework#support).

Many issues are caused by subtle behavior, typos, and unintended configuration.
Creating a [Minimal Reproducible Example](https://stackoverflow.com/help/minimal-reproducible-example) (starting with https://start.spring.io for example) of the problem helps the team quickly triage your issue and get to the core of the problem.
Share it as a link to a source repository or attach it as an archive, along with instructions on how to reproduce the problem.

For enhancement requests, before explaining how you would like things to work, please describe a concrete use case for the feature and how you have tried to solve it so far.

#### Issue Lifecycle

When an issue is first created, it is flagged `waiting-for-triage` waiting for a team member to triage it.
Once the issue has been reviewed, the team may ask for further information if needed, and based on the findings, the issue is either assigned a target milestone or is closed with a specific status.

When a fix is ready, the issue is closed and may still be re-opened until the fix is released.
After that the issue will typically no longer be reopened.
In rare cases if the issue was not at all fixed, the issue may be re-opened.
In most cases however any follow-up reports will need to be created as new issues with a fresh description.

#### Submit a Pull Request

1. Should you create an issue first? No, just create the pull request and use the description to provide context and motivation, as you would for an issue.
If you want to start a discussion first or have already created an issue, once a pull request is created, we will close the issue as superseded by the pull request, and the discussion about the issue will continue under the pull request.

1. Always check out the `main` branch and submit pull requests against it (for the target version see [gradle.properties](gradle.properties)).
Backports to prior versions will be considered on a case-by-case basis and reflected as the fix version in the issue tracker.

1. Please do not submit pull requests:
   * With GitHub accounts managed by autonomous AI bots.
   * For issues with the label `status: waiting-for-triage`, which indicates that the team has not yet triaged or decided on the issue.
   * For issues already assigned to someone else, since the assignee is working on it or plans to.
   * For straightforward or polish-style changes.

1. Before submitting a pull request:
   * Add new tests, or update existing ones, that exercise the code you have added or modified.
   * Run the build and tests for the affected modules locally (see [Build from Source](#build-from-source)), including the `checkstyle` checks.
   * Carefully review the changes yourself, especially when they were generated by a coding agent.

1. Choose the granularity of your commits consciously and squash commits that represent multiple edits or corrections of the same logical change.
See [Rewriting History section of Pro Git](https://git-scm.com/book/en/Git-Tools-Rewriting-History) for an overview of streamlining the commit history.

1. All commits must include a _Signed-off-by_ trailer at the end of each commit message to indicate that the contributor agrees to the Developer Certificate of Origin.
For additional details, please refer to the blog post [Hello DCO, Goodbye CLA: Simplifying Contributions to Spring](https://spring.io/blog/2025/01/06/hello-dco-goodbye-cla-simplifying-contributions-to-spring).

1. Format commit messages as described in [Commit Messages](#commit-messages).

1. If there is a prior issue, reference the GitHub issue number in the description of the pull request, not in its title.

If accepted, your contribution may be heavily modified as needed prior to merging.
You will likely retain author attribution for your Git commits granted that the bulk of your changes remain intact.
You may also be asked to rework the submission.

If asked to make changes, simply push the changes against the same branch, and your pull request will be updated.
In other words, you do not need to create a new pull request when asked to make changes.

#### Commit Messages

A commit message consists of a subject line, a blank line, a description, and trailers.

* The subject line should be at most 55 characters, start with a capitalized verb in the imperative mood, and not end with a period.
It should not include prefixes like `fix:`, `feat:` or `docs:`, nor the issue or pull request number.
* The description should be wrapped at 72 characters and explain the motivation for the change, for example with "Prior to this commit, ..." followed by "This commit ...".
* The description is followed by a reference to the related issue, for example `Closes gh-22276` (which closes the issue once the commit is merged) or `See gh-22276` (which only references it).
* The last line is the `Signed-off-by` trailer, which `git commit -s` adds for you.

For example:

```text
Fix single-value adaptation for primitive array types

Prior to this commit, a single non-array value provided for a primitive
array attribute was wrapped in a boxed array, which then failed the
compatibility check.

This commit derives the component type from the declared attribute
type so that all primitive array types accept a single value.

Closes gh-22276

Signed-off-by: Firstname Lastname <username@users.noreply.github.com>
```

To avoid mentioning unrelated GitHub users, annotation names such as `@Override` should not appear verbatim in commit messages or pull request titles.
Enclose them in backticks instead, as in `` `@Override` ``.

See the [Commit Guidelines section of Pro Git](https://git-scm.com/book/en/Distributed-Git-Contributing-to-a-Project#Commit-Guidelines) for best practices around commit messages, and use `git log` to see some examples.

#### Participate in Reviews

Helping to review pull requests is another great way to contribute.
Your feedback can help to shape the implementation of new features.
When reviewing pull requests, however, please refrain from approving or rejecting a PR unless you are a core committer for the Spring Framework.

### Build from Source

The Spring Framework uses a [Gradle](https://gradle.org) build.
The instructions below use the Gradle Wrapper from the root of the source tree.
The wrapper script serves as a cross-platform, self-contained bootstrap mechanism for the build system.

#### Before You Start

To build you will need [Git](https://docs.github.com/en/get-started/quickstart/set-up-git) and the JDK specified in the [.sdkmanrc](.sdkmanrc) file (currently JDK 25, for example [Liberica JDK 25](https://bell-sw.com/pages/downloads/#jdk-25)).
The build produces artifacts compatible with Java 17+.

If you use [SDKMAN!](https://sdkman.io/), run `sdk env install` to install the JDK specified in `.sdkmanrc`, and `sdk env` to use it in your current shell.

#### Get the Source Code

```shell
git clone git@github.com:spring-projects/spring-framework.git
cd spring-framework
```

#### Build from the Command Line

To compile, test, and build all jars, distribution zips, and docs use:

```shell
./gradlew build
```

The first time you run the build it may take a while to download Gradle and all build dependencies, as well as to run all tests.
Once you've bootstrapped a Gradle distribution and downloaded dependencies, those are cached in your `$HOME/.gradle` directory.

Gradle has good incremental build support, so run without `clean` to keep things snappy.
You can also use the `:<project>` prefix to only run the tasks of a given module and the modules it depends on.
For example, if iterating over changes in `spring-webmvc`, run the following to build and test that module:

```shell
./gradlew :spring-webmvc:test
```

To run a single test class or method, use the `--tests` option:

```shell
./gradlew :spring-core:test --tests org.springframework.util.StringUtilsTests
```

To run all checks for a module, including tests and `checkstyle`, use:

```shell
./gradlew :spring-core:check
```

#### Install in Local Maven Repository

If you need to publish Spring Framework artifacts locally for testing, you can do the following:

```shell
./gradlew pTML -PskipDocs
```

`pTML` is an abbreviation for the `publishToMavenLocal` task.
The `skipDocs` property will skip the "documentation" and "distribution" tasks (typically, the javadoc, kdoc and zip artifacts for docs in general).
This can be useful for local iterations, but it is advised to run the full build before submitting a pull request.

To install all Spring Framework jars in your local Maven repository, use the following:

```shell
./gradlew publishToMavenLocal
```

#### Import into your IDE

Ensure that the JDK specified in `.sdkmanrc` is configured in your IDE.
Then follow the instructions for [Eclipse](import-into-eclipse.md) or [IntelliJ IDEA](import-into-intellij-idea.md).

### Source Code Style

This section defines the coding standards for source files in the Spring Framework.
Its structure is based on the [Google Java Style](https://google.github.io/styleguide/javaguide.html) reference.
Many of these rules are enforced at build time by [Checkstyle](src/checkstyle/checkstyle.xml).

Above all, please carefully follow the whitespace and formatting conventions already present in the code you are modifying, and do not reformat code for its own sake.

#### Source File Basics

* Source files must be encoded using `UTF-8`.
* Indentation uses _tabs_, not spaces.
* Use Unix (LF), not DOS (CRLF), line endings.
* Eliminate all trailing whitespace.

#### Source File Structure

A source file consists of the following, in this exact order:

* License
* Package statement
* Import statements
* Exactly one top-level class

Exactly one blank line separates each of the above sections.

##### License

Each source file must specify the following license at the very top of the file:

```java
/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

##### Import Statements

The import statements are structured as follows:

* import `java.*`
* blank line
* import `javax.*`
* import `jakarta.*`
* blank line
* import all other imports
* blank line
* import `org.springframework.*`
* blank line
* import static all other imports

Static imports should not be used in production code, but they should be used in test code, especially for things like `import static org.assertj.core.api.Assertions.assertThat;`.

Although static imports are generally forbidden in production code, the following are use cases for which static imports are permissible:

* constants (including enum constants): such as those in `java.nio.charset.StandardCharsets` or `org.springframework.core.annotation.MergedAnnotations.SearchStrategy`
* static factory methods for third-party DSLs: such as the methods in `org.junit.platform.engine.discovery.DiscoverySelectors` when used with the JUnit Platform `Launcher` API

Wildcard imports such as `import java.util.*` or `import static org.assertj.core.api.Assertions.*` are forbidden, even in test code.

##### Java Source File Organization

The following governs how the elements of a source file are organized:

1. static fields
1. normal fields
1. constructors
1. (private) methods called from constructors
1. static factory methods
1. JavaBean properties (i.e., getters and setters)
1. method implementations coming from interfaces
1. private or protected templates that get called from method implementations coming from interfaces
1. other methods
1. `equals`, `hashCode`, and `toString`

Note that private or protected methods called from method implementations should be placed immediately below the methods where they're used.
In other words, if there are 3 interface method implementations with 3 private methods (one used from each), then the order of methods should include 1 interface and 1 private method in sequence, not 3 interface and then 3 private methods at the bottom.

Above all, the organization of the code should feel _natural_.

#### Formatting

##### Braces

Braces mostly follow the _Kernighan and Ritchie style_ (a.k.a., "Egyptian brackets") for nonempty blocks and block-like constructs:

* No line break before the opening brace but prefixed by a single space
* Line break after the opening brace
* Line break before the closing brace
* Line break after the closing brace if that brace terminates a statement or the body of a method, constructor, or named class
* Line break before `else`, `catch`, and `finally` statements

Example:

```java
return new MyClass() {
	@Override
	public void method() {
		if (condition()) {
			something();
		}
		else {
			try {
				alternative();
			}
			catch (ProblemException ex) {
				recover();
			}
		}
	}
};
```

##### Line Wrapping

90 characters is the *preferred* line length we aim for.
In some cases the preferred length can be achieved by refactoring code slightly.
In other cases it's just not possible.

90 is not a hard limit.
Lines between 90-105 are perfectly acceptable in many cases where it aids readability and where wrapping has the opposite effect of reducing readability.
This is a judgement call and it's also important to seek consistency.
Many times you can learn by looking at how specific situations are handled in other parts of the code.

Lines between 105-120 are allowed but discouraged and should be few.

No lines should exceed 120 characters.

The one big exception to the above line wrapping rules is Javadoc where we aim to wrap around 80 characters for maximum readability in all kinds of contexts, for example, reading on GitHub, on your phone, etc.

When wrapping a lengthy expression, 90 characters is the length at which we aim to wrap.
Place separator symbols at the end of the current line rather than at the beginning of the next line.
In this context, the following are considered separator symbols: `,`, `+`, `?`, `:`, `&&`, `||`.
For example:

```java
if (thisLengthyMethodCall(param1, param2) && anotherCheck() &&
		yetAnotherCheck()) {

	// ....
}
```

##### Blank Lines

Add two blank lines before the following elements:

* `static {}` block
* Fields
* Constructors
* Inner classes

Add one blank line after a method signature that is multiline, i.e.

```java
@Override
protected Object invoke(FooBarOperationContext context,
		AnotherSuperLongName name) {

	// code here
}
```

For inner classes, extra blank lines around fields and constructors are typically not added as the inner class is already separated by 2 lines, unless the inner class is more substantial in which case the 2 extra lines could still help with readability.

##### IDE Settings

The [src/idea/spring-framework.xml](src/idea/spring-framework.xml) file provides an IntelliJ IDEA code style scheme with the settings we use, and `./gradlew cleanEclipse eclipse` applies the [Eclipse settings](src/eclipse) we use.
Notable differences from the IntelliJ IDEA defaults are:

* Use tab character for indentation
* Add a space before the left brace of an array initializer
* Keep when reformatting: _multiple expressions in one line_, _simple blocks in one line_
* `else`, `catch`, and `finally` on new line
* Method declaration parameters: do not align when multiline
* Javadoc formatting disabled
* High thresholds for _"Class count to use import with `*`"_ and _"Names count to use static imports with `*`"_ so that imports are always listed individually
* Import layout as described in [Import Statements](#import-statements)

#### Class Declaration

Try as much as possible to put the `implements`, `extends` section of a class declaration on the same line as the class itself.

Order the classes so that the most important comes first.

#### Naming

##### Constant Names

Constant names use `CONSTANT_CASE`: all uppercase letters, with words separated by underscores.

Every constant is a `static final` field, but not all `static final` fields are constants.
Constant case should therefore be chosen only if the field **is really** a constant.

Example:

```java
// Constants
private static final Object NULL_HOLDER = new NullHolder();
public static final int DEFAULT_PORT = -1;

// Not constants
private static final ThreadLocal<Executor> executorHolder = new ThreadLocal<>();
private static final Set<String> internalAnnotationAttributes = new HashSet<>();
```

##### Variable Names

Avoid using single characters as variable names.
For instance, prefer `Method method` to `Method m`.

#### Programming Practices

##### File History

* A file should look like it was crafted by a single author, not like a history of changes.
* Don't artificially spread things out that belong together.

##### Organization of Setter Methods

Choose wisely where to add a new setter method; it should not be simply added at the end of the list.
Perhaps the setter is related to another setter or relates to a group.
In that case it should be placed near related methods.

* Setter order should reflect order of importance, not historical order.
* Ordering of _fields_ and _setters_ should be **consistent**.

##### Ternary Operator

Wrap the ternary operator within parentheses, for example, `return (foo != null ? foo : "default");`.

Also make sure that the _not null_ condition comes first.

##### Null Checks

Use the `org.springframework.util.Assert.notNull` static method to check that a method argument is not `null` and throw an `IllegalArgumentException` otherwise.
Format the exception message so that the name of the parameter comes first with its first character capitalized, followed by "_must not be null_".
For instance:

```java
public void handle(Event event) {
	Assert.notNull(event, "Event must not be null");
	//...
}
```

For other use cases, use the `org.springframework.util.Assert.state` static method to ensure a variable is not `null` and throw an `IllegalStateException` otherwise.
Format the exception message so that the identifier related to the variable (name, type or description) comes first with its first character capitalized, followed by "_must not be null_".
For instance:

```java
//...
Event event = ...
Assert.state(event != null, "Event must not be null");
//...
```

##### Null Safety of APIs and Fields

The null-safety of Spring Framework APIs and fields must be specified using [JSpecify](https://jspecify.dev/) annotations, and consistency is enforced at build time via [NullAway](https://github.com/uber/NullAway).
For details, see the [Null-safety](https://docs.spring.io/spring-framework/reference/core/null-safety.html) chapter of the reference manual.

Packages are expected to define non-null as the default in `package-info.java`:

```java
@NullMarked
package org.springframework.mypackage;

import org.jspecify.annotations.NullMarked;
```

Nullable fields, return values, and parameters are expected to be specified explicitly using `@Nullable`:

```java
import org.jspecify.annotations.Nullable;

public class MyClass {

	private @Nullable Class<?> nullableType;

	public void setNullableType(@Nullable Class<?> nullableType) {
		this.nullableType = nullableType;
	}

	public @Nullable Class<?> getNullableType() {
		return this.nullableType;
	}
}
```

Spring's [`@Contract`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/lang/Contract.html) annotation can be used to specify some aspects of the method behavior depending on the arguments that are then [taken into account by NullAway](https://github.com/uber/NullAway/wiki/Supported-Annotations#contracts).
See for example how `@Contract` is used within the Spring Framework codebase in the `Assert` class:

```java
public abstract class Assert {
	// ...

	@Contract("false, _ -> fail")
	public static void state(boolean expression, String message) {
		if (!expression) {
			throw new IllegalStateException(message);
		}
	}

	// ...

	@Contract("null, _ -> fail")
	public static void notNull(@Nullable Object object, String message) {
		if (object == null) {
			throw new IllegalArgumentException(message);
		}
	}

	// ...
}
```

Those contracts allow NullAway's static analysis to understand that after an invocation of `Assert.state(value != null, "Value must not be null")` or `Assert.notNull(value, "Value must not be null")`, a nullable `value` can be considered as non-nullable.

Related guidelines:

* When overriding a method, null-safety annotations of the super method need to be specified on the overridden method unless you want to override null-safety.
* Use `@SuppressWarnings("NullAway")` when NullAway triggers irrelevant errors, which can happen due to [NullAway bugs or missing features](https://github.com/uber/NullAway/issues) or when the analysis is not able to prove null-safety (for example, with lambdas).
* Only use the `@Nullable` and `@NonNull` annotations from the `org.jspecify.annotations` package.

##### Use of @Override

Always add `@Override` on methods overriding or implementing a method declared in a super type.

##### Use of @since

* `@since` should be added to every new class with the version of the framework in which it was introduced.
* `@since` should be added to any *new* **public** and **protected** methods of an existing class.
* `@since` omits a `.0` patch version (for example, `@since 7.1` rather than `@since 7.1.0`), but includes it otherwise (for example, `@since 7.0.3` for an addition in a maintenance release).

##### Utility Classes

A class that is only a collection of static utility methods must be named with a `Utils` suffix, must have a `private` default constructor, and must be `abstract`.
Making the class `abstract` and providing a `private` _default_ constructor prevent anyone from instantiating it.
For example:

```java
public abstract class MyUtils {

	private MyUtils() {
		/* prevent instantiation */
	}

	// static utility methods
}
```

##### Field and Method References

A field of a class should *always* be referenced using `this`.
A method of a class, however, should never be referenced using `this`.

##### Local Variable Type Inference

The use of `var` for variable declarations (_local variable type inference_) is not permitted.
Instead, declare variables using the concrete type or interface (where applicable).
Note, however, that `var` is permitted within test methods if used consistently within the test and preferably within the entire test class.

##### Logging and Console Output

Printing to `System.out` or `System.err`, and calling `printStackTrace()`, are forbidden in the main codebase.

#### Javadoc

##### Javadoc Formatting

The following template summarizes typical Javadoc usage for a method.

```java
/**
 * Parse the specified {@link Element} and register the resulting
 * {@link BeanDefinition BeanDefinition(s)}.
 * <p>Implementations must return the primary {@link BeanDefinition} that results
 * from the parsing if they will ever be used in a nested fashion (for example as
 * an inner tag in a {@code <property/>} tag). Implementations may return
 * {@code null} if they will <strong>not</strong> be used in a nested fashion.
 * @param element the element that is to be parsed into one or more {@link BeanDefinition BeanDefinitions}
 * @param parserContext the object encapsulating the current state of the parsing process;
 * provides access to a {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
 * @return the primary {@link BeanDefinition}
 */
BeanDefinition parse(Element element, ParserContext parserContext);
```

In particular, please note:

* Use an imperative style (i.e. _Return_ and not _Returns_) for the first sentence.
* No blank lines between the description and the parameter descriptions.
* If the description is defined with multiple paragraphs, start each of them with `<p>`.
* If a parameter description needs to be wrapped, do not indent subsequent lines (see `parserContext`).

The Javadoc of a class has some extra rules that are illustrated by the sample below:

```java
/**
 * Interface used by the {@link DefaultBeanDefinitionDocumentReader} to handle custom,
 * top-level (directly under {@code <beans/>}) tags.
 *
 * <p>Implementations are free to turn the metadata in the custom tag into as many
 * {@link BeanDefinition BeanDefinitions} as required.
 *
 * <p>The parser locates a {@link BeanDefinitionParser} from the associated
 * {@link NamespaceHandler} for the namespace in which the custom tag resides.
 *
 * @author Rob Harrop
 * @since 2.0
 * @see NamespaceHandler
 * @see AbstractBeanDefinitionParser
 */
```

* Each class must have a `@since` tag with the version in which the class was introduced.
* The order of tags for type-level Javadoc is: `@author`, `@since`, `@param`, `@see`, `@deprecated`.
* The order of tags for constructor-level, method-level, and field-level Javadoc is: `@param`, `@return`, `@throws`, `@since`, `@see`, `@deprecated`.
* In contrast to constructor-level, method-level, and field-level Javadoc, the paragraphs of a class description *are* separated by blank lines.

The following are additional general rules to apply when writing Javadoc:

* Use `{@code}` to wrap code statements or values such as `null`.
* If a type is only referenced by a `{@link}` element, use the fully qualified name in order to avoid an unnecessary `import` declaration.

### Tests

Any code change should come with new or updated tests.

* Tests must be written using JUnit Jupiter.
The only exceptions to this rule are test classes in the `spring-test` module that specifically test Spring's integration with JUnit 4 and TestNG.
* Each test class name must end with a `Tests` suffix.
* Use [AssertJ](https://assertj.github.io/doc/) for assertions and assumptions, including specialized assertions such as `assertThatIllegalArgumentException()` instead of `assertThatExceptionOfType(IllegalArgumentException.class)`.
* Use [Mockito](https://site.mockito.org/) for mocks and spies.

### Reference Docs

The reference documentation is authored in [AsciiDoc](https://asciidoc.org/) format using [Antora](https://docs.antora.org/antora/latest/).
The source files for the documentation reside in the [framework-docs/modules/ROOT](framework-docs/modules/ROOT) directory.
Java and Kotlin code snippets included via `include-code::` reside in [framework-docs/src/main](framework-docs/src/main) and are compiled as part of the build.
For trivial changes, you may be able to browse, edit source files, and submit directly from GitHub.

When making changes locally, execute `./gradlew antora` and then browse the results under `framework-docs/build/site/index.html`.

Asciidoctor also supports live editing.
For more details see [AsciiDoc Tooling](https://docs.asciidoctor.org/asciidoctor/latest/tooling/).
