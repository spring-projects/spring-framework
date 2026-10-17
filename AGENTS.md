# Spring Framework agent guidelines

Concise extract of [CONTRIBUTING.md](CONTRIBUTING.md), which remains the reference for humans.

## Build

- Gradle build, JDK from `.sdkmanrc` (currently JDK 25, `sdk env install` / `sdk env`); artifacts target Java 17+.
- Scope builds to the modules you touched: `./gradlew :spring-webmvc:test`, a single test with `--tests <fully.qualified.ClassTests>`.
- Run `./gradlew :<module>:check` before finishing: it runs tests and Checkstyle (`src/checkstyle/checkstyle.xml`), which enforces most of the code style below.
- NullAway null-safety checks run during compilation; fix reported errors instead of suppressing them.
- Do not run `clean` unless needed; the build is incremental and cached.
- Reference docs: `./gradlew antora`, output in `framework-docs/build/site/index.html`.

## Code style

- Match surrounding code; do not reformat unrelated code; a file should look like it was written by a single author.
- Tabs, LF, UTF-8, no trailing whitespace.
- Aim for 90 characters per line for code (105 acceptable, 120 max) and ~80 for Javadoc.
- Wrap lines after separators (`,` `+` `?` `:` `&&` `||`), never before.
- K&R braces, with `else`, `catch`, and `finally` on a new line.
- Two blank lines before fields, constructors, `static {}` blocks, and inner classes; one blank line after a multiline method signature.
- Import order, groups separated by a blank line: `java.*`, then `javax.*` + `jakarta.*`, then others, then `org.springframework.*`, then static imports.
- No wildcard imports. No static imports in production code except constants/enum constants and third-party DSL factory methods; use them in tests (e.g. `assertThat`).
- Every source file: Apache 2.0 license header (`Copyright 2002-present the original author or authors.`, copy from an existing file), package, imports, exactly one top-level class.
- Always reference fields with `this.`, never methods. Always add `@Override`.
- No `var` in production code. No single-character variable names. Wrap ternaries in parentheses with the non-null condition first: `(foo != null ? foo : "default")`.
- Argument checks: `Assert.notNull(event, "Event must not be null")`; state checks: `Assert.state(...)`.
- Null-safety with JSpecify: packages are `@NullMarked` in `package-info.java`, use `org.jspecify.annotations.Nullable` explicitly (e.g. `private @Nullable String name;`), repeat super method nullness on overrides, `@Contract` where useful.
- Static utility classes: `abstract`, `Utils` suffix, private constructor.
- No `System.out`/`System.err` or `printStackTrace()`.

## Javadoc

- First sentence in imperative style ("Return", not "Returns"); `<p>` to start extra paragraphs; `{@code}` for code and `null`.
- No blank line between method description and tags; do not indent wrapped tag descriptions.
- Add `@since` to new classes and new public/protected methods; omit a `.0` patch version (`@since 7.1`, not `7.1.0`).
- Tag order for types: `@author`, `@since`, `@param`, `@see`, `@deprecated`; for members: `@param`, `@return`, `@throws`, `@since`, `@see`, `@deprecated`.

## Tests

- Add or update tests for any code change.
- JUnit Jupiter, AssertJ (including `assertThatIllegalArgumentException()` and similar), Mockito. No JUnit 4/Jupiter/TestNG assertions, no Hamcrest.
- Test class names end with `Tests`.
- Use the standard Mockito API (`when(...).thenReturn(...)`), not `BDDMockito` (`given(...)`), in new test code; do not migrate existing `BDDMockito` usage.

## Docs

- Reference docs are AsciiDoc in `framework-docs/modules/ROOT`; code snippets for `include-code::` live in `framework-docs/src/main/{java,kotlin}`.

## Commits and pull requests

- Pull requests target `main`.
- Subject: imperative, capitalized verb, at most 55 characters, no `fix:`/`docs:` prefixes, no issue/PR number, no trailing period.
- Body wrapped at 72 characters explaining the motivation, followed by `Closes gh-123` (or `See gh-123`), then a `Signed-off-by: Name <email>` trailer (DCO, use `git commit -s`).
- Never write annotations verbatim in commit messages or PR titles (it mentions GitHub users): enclose them in backticks, as in `` `@Override` ``.
- Changes must be reviewed by a human who remains accountable for them before being submitted.
