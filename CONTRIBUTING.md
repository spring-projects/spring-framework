# Contributing  to the Spring Framework

First off, thank you for taking the time to contribute! :+1: :tada: 

### Table of Contents

* [Code of Conduct](#code-of-conduct)
* [How to Contribute](#how-to-contribute)
  * [Discuss](#discuss)
  * [Create a Ticket](#create-a-ticket)
  * [Ticket Lifecycle](#ticket-lifecycle)
  * [Submit a Pull Request](#submit-a-pull-request)
* [Build from Source](#build-from-source)
* [Source Code Style](#source-code-style)
* [Reference Docs](#reference-docs)

### Code of Conduct

This project is governed by the [Spring Code of Conduct](CODE_OF_CONDUCT.adoc).
By participating you are expected to uphold this code.
Please report unacceptable behavior to spring-code-of-conduct@pivotal.io.

### How to Contribute

#### Discuss

If you have a question, check StackOverflow using
[this list of tags](https://spring.io/questions), organized by Spring project.
Find an existing discussion or start a new one if necessary.

If you suspect an issue, perform a search in the
[JIRA issue tracker](https://jira.spring.io/browse/SPR), using a few different keywords.
When you find related issues and discussions, prior or current, it helps you to learn and
it helps us to make a decision.

#### Create a Ticket

Reporting an issue or making a feature request is a great way to contribute. Your feedback
and the conversations that result from it provide a continuous flow of ideas. 

Before you create a ticket, please take the time to [research first](#discuss).

If creating a ticket after a discussion on StackOverflow, please provide a self-sufficient description in the ticket, independent of the details on StackOverview. We understand this is extra work but the issue tracker is an important place of record for design discussions and decisions that can often be referenced long after the fix version, for example to revisit decisions, to understand the origin of a feature, and so on.

When ready create a ticket in the [JIRA issue tracker](https://jira.spring.io/browse/SPR).

#### Ticket Lifecycle

When an issue is first created, it may not be assigned and will not have a fix version.
Within a day or two, the issue is assigned to a specific committer and the target
version is set to "Waiting for Triage". The committer will then review the issue, ask for
further information if needed, and based on the findings, the issue is either assigned a fix
version or rejected.

When a fix is ready, the issue is marked "Resolved" and may still be re-opened. Once a fix
is released, the issue is permanently "Closed". If necessary, you will need to create a new,
related ticket with a fresh description.

#### Submit a Pull Request

You can contribute a source code change by submitting a pull request.

1. If you have not previously done so, please sign the
[Contributor License Agreement](https://cla.pivotal.io/sign/spring). You will also be reminded
automatically when you submit a pull request.

1. For all but the most trivial of contributions, please [create a ticket](#create-a-ticket).
The purpose of the ticket is to understand and discuss the underlying issue or feature.
We use the JIRA issue tracker as the preferred place of record for conversations and
conclusions. In that sense discussions directly under a PR are more implementation detail
oriented and transient in nature.

1. Always check out the `master` branch and submit pull requests against it
(for target version see [settings.gradle](settings.gradle)).
Backports to prior versions will be considered on a case-by-case basis and reflected as
the fix version in the issue tracker.

1. Use short branch names, preferably based on the JIRA issue (e.g. `SPR-1234`), or
otherwise using succinct, lower-case, dash (-) delimited names, such as `fix-warnings`.

1. Choose the granularity of your commits consciously and squash commits that represent
multiple edits or corrections of the same logical change. See
[Rewriting History section of Pro Git](http://git-scm.com/book/en/Git-Tools-Rewriting-History)
for an overview of streamlining commit history.

1. Format commit messages using 55 characters for the subject line, 72 lines for the
description, followed by related issues, e.g. `Issues: SPR-1234, SPR-1235`.
See the
[Commit Guidelines section of Pro Git](http://git-scm.com/book/en/Distributed-Git-Contributing-to-a-Project#Commit-Guidelines)
for best practices around commit messages and use `git log` to see some examples.

1. List the JIRA issue number in the PR description.

If accepted, your contribution may be heavily modified as needed prior to merging.
You will likely retain author attribution for your Git commits granted that the bulk of
your changes remain intact. You may also be asked to rework the submission.

If asked to make corrections, simply push the changes against the same branch, and your
pull request will be updated. In other words, you do not need to create a new pull request
when asked to make changes.

### Build from Source

See the [Build from Source](https://github.com/spring-projects/spring-framework/wiki/Build-from-Source)
wiki page for instructions on how to check out, build, and import the Spring Framework
source code into your IDE.

### Source Code Style

The wiki pages
[Code Style](https://github.com/spring-projects/spring-framework/wiki/Code-Style) and
[IntelliJ IDEA Editor Settings](https://github.com/spring-projects/spring-framework/wiki/IntelliJ-IDEA-Editor-Settings)
defines the source file coding standards we use along with some IDEA editor settings we customize.

### Reference Docs

The reference documentation is in the [src/docs/asciidoc](src/docs/asciidoc) directory and, in
[Asciidoctor](http://asciidoctor.org/) format. For trivial changes, you may be able to browse,
edit source files, and submit directly from Github.

When making changes locally, use `./gradlew asciidoctor` and then browse the result under
`build/asciidoc/html5/index.html`.

Asciidoctor also supports live editing. For more details read
[Editing AsciiDoc with Live Preview](http://asciidoctor.org/docs/editing-asciidoc-with-live-preview/).
Note that if you choose the
[System Monitor](http://asciidoctor.org/docs/editing-asciidoc-with-live-preview/#using-a-system-monitor)
option, you can find a Guardfile under `src/docs/asciidoc`.
