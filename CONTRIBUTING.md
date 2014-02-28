_Have something you'd like to contribute to the framework?  We welcome pull
requests but ask that you carefully read this document first to understand how
best to submit them; what kind of changes are likely to be accepted; and what
to expect from the Spring team when evaluating your submission._

_Please refer back to this document as a checklist before issuing any pull
request; this will save time for everyone!_

## Take Your First Steps

### Understand the basics

Not sure what a pull request is, or how to submit one? Take a look at GitHub's
excellent [help documentation][] first.


### Search JIRA first; create an issue if necessary

Is there already an issue that addresses your concern? Do a bit of searching
in our [JIRA issue tracker][] to see if you can find something similar. If you
do not find something similar, please create a new JIRA issue before submitting
a pull request unless the change is truly trivial -- for example: typo fixes,
removing compiler warnings, etc.

### Discuss non-trivial contribution ideas with committers

If you're considering anything more than correcting a typo or fixing a minor
bug, please discuss it on the [spring-framework-contrib][] mailing list before
submitting a pull request. We're happy to provide guidance, but please spend an
hour or two researching the subject on your own, including searching the mailing
list for prior discussions.

### Sign the Contributor License Agreement

If you have not previously done so, please fill out and submit the
[SpringSource CLA form][]. You'll receive a token when this process is complete.
Keep track of this; you may be asked for it later!

Note that emailing/postal mailing a signed copy is _not_ necessary. Submission
of the web form is all that is required.

Once you've completed the web form, simply add the following in a comment on
your pull request:

    I have signed and agree to the terms of the SpringSource Individual
    Contributor License Agreement.

You do not need to include your token/id. Please add the above statement to all
future pull requests as well, simply so that the Spring Framework team knows
immediately that this process is complete.

## Create a Branch

### Branch from `master`

Master currently represents work toward Spring Framework 4.0. Please submit
all pull requests there, even bug fixes and minor improvements. Backports to
`3.2.x` will be considered on a case-by-case basis.


### Use short branch names

Branches used when submitting pull requests should preferably be named
according to JIRA issues, e.g. 'SPR-1234'. Otherwise, use succinct, lower-case,
dash (-) delimited names, such as 'fix-warnings', 'fix-typo', etc. In
[fork-and-edit][] cases, the GitHub default 'patch-1' is fine as well. This is
important, because branch names show up in the merge commits that result from
accepting pull requests and should be as expressive and concise as possible.

## Use Spring Code Style

The complete [Spring Code Style][] reference is available on the wiki, but
here's a quick summary:

### Mind the whitespace

Please carefully follow the whitespace and formatting conventions already
present in the framework.

1. Tabs, not spaces
1. Unix (LF), not DOS (CRLF) line endings
1. Eliminate all trailing whitespace
1. Wrap Javadoc at 90 characters
1. Aim to wrap code at 90 characters, but favor readability over wrapping
1. Preserve existing formatting; i.e. do not reformat code for its own sake
1. Search the codebase using `git grep` and other tools to discover common
    naming conventions, etc.
1. Latin-1 (ISO-8859-1) encoding for Java sources; use `native2ascii` to convert
    if necessary


### Add Apache license header to all new classes

```java
/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ...;
```

### Update Apache license header in modified files as necessary

Always check the date range in the license header. For example, if you've
modified a file in 2014 whose header still reads:

```java
 * Copyright 2002-2011 the original author or authors.
```

Then be sure to update it to 2014 accordingly:

```java
 * Copyright 2002-2014 the original author or authors.
```

### Use @since tags for newly-added public API types and methods

For example:

```java
/**
 * ...
 *
 * @author First Last
 * @since 4.0
 * @see ...
 */
```

## Prepare Your Commit

### Submit JUnit test cases for all behavior changes

Search the codebase to find related tests and add additional `@Test` methods
as appropriate. It is also acceptable to submit test cases on a per JIRA issue
basis, for example:

```java
package org.springframework.beans.factory.support;

/**
 * Unit tests for SPR-8954, in which a custom {@link InstantiationAwareBeanPostProcessor}
 * forces the predicted type of a FactoryBean, effectively preventing retrieval of the
 * bean from calls to #getBeansOfType(FactoryBean.class). The implementation of
 * {@link AbstractBeanFactory#isFactoryBean(String, RootBeanDefinition)} now ensures
 * that not only the predicted bean type is considered, but also the original bean
 * definition's beanClass.
 *
 * @author Chris Beams
 */
public class Spr8954Tests {

    @Test
    public void cornerSpr8954() {
        // ...
    }
}
```


### Squash commits

Use `git rebase --interactive --autosquash`, `git add --patch` and other tools
to "squash" multiple commits into atomic changes. In addition to the man pages
for git, there are many resources online to help you understand how these tools
work. Here is one: http://book.git-scm.com/4_interactive_rebasing.html.


### Use real name in git commits

Please configure git to use your real first and last name for any commits you
intend to submit as pull requests. For example, this is not acceptable:

    Author: Nickname <user@mail.com>

Rather, please include your first and last name, properly capitalized, as
submitted against the SpringSource contributor license agreement:

    Author: First Last <user@mail.com>

This helps ensure traceability against the CLA and also goes a long way to
ensuring useful output from tools like `git shortlog` and others.

You can configure this via the account admin area in GitHub (useful for
fork-and-edit cases); _globally_ on your machine with

    git config --global user.name "First Last"
    git config --global user.email user@mail.com

or _locally_ for the `spring-framework` repository only by omitting the
'--global' flag:

    cd spring-framework
    git config user.name "First Last"
    git config user.email user@mail.com


### Format commit messages

Please read and follow the [commit guidelines section of Pro Git][].

Most importantly, please format your commit messages in the following way
(adapted from the commit template in the link above):

    Short (50 chars or less) summary of changes

    More detailed explanatory text, if necessary. Wrap it to about 72
    characters or so. In some contexts, the first line is treated as the
    subject of an email and the rest of the text as the body. The blank
    line separating the summary from the body is critical (unless you omit
    the body entirely); tools like rebase can get confused if you run the
    two together.

    Further paragraphs come after blank lines.

     - Bullet points are okay, too

     - Typically a hyphen or asterisk is used for the bullet, preceded by a
       single space, with blank lines in between, but conventions vary here

    Issue: SPR-1234, SPR-1235


1. Use imperative statements in the subject line, e.g. "Fix broken Javadoc link"
1. Begin the subject line with a capitalized verb, e.g. "Add, Prune, Fix,
    Introduce, Avoid, etc."
1. Do not end the subject line with a period
1. Restrict the subject line to 50 characters or less if possible
1. Wrap lines in the body at 72 characters or less
1. Mention associated JIRA issue(s) at the end of the commit comment, prefixed
    with "Issue: " as above
1. In the body of the commit message, explain how things worked before this
    commit, what has changed, and how things work now

For examples of this style, issue a `git log --author=cbeams` in the
`spring-framework` git repository. For convenience, here are several such commits:

- https://github.com/SpringSource/spring-framework/commit/08e2669b84ec0faa2f7904441fe39ac70b65b078
- https://github.com/SpringSource/spring-framework/commit/1d9d3e6ff79ce9f0eca03b02cd1df705925575da
- https://github.com/SpringSource/spring-framework/commit/8e0b1c3a5f957af3049cfa0438317177e16d6de6
- https://github.com/SpringSource/spring-framework/commit/b787a68f2050df179f7036b209aa741230a02477

## Run the Final Checklist

### Run all tests prior to submission

See the [building from source][] section of the `README` for instructions. Make
sure that all tests pass prior to submitting your pull request.


### Submit your pull request

Subject line:

Follow the same conventions for pull request subject lines as mentioned above
for commit message subject lines.

In the body:

1. Explain your use case. What led you to submit this change? Why were existing
    mechanisms in the framework insufficient? Make a case that this is a
    general-purpose problem and that yours is a general-purpose solution, etc.
1. Add any additional information and ask questions; start a conversation or
    continue one from JIRA
1. Mention the JIRA issue ID
1. Also mention that you have submitted the CLA as described above

Note that for pull requests containing a single commit, GitHub will default the
subject line and body of the pull request to match the subject line and body of
the commit message. This is fine, but please also include the items above in the
body of the request.


### Mention your pull request on the associated JIRA issue

Add a comment to the associated JIRA issue(s) linking to your new pull request.


### Expect discussion and rework

The Spring team takes a very conservative approach to accepting contributions to
the framework. This is to keep code quality and stability as high as possible,
and to keep complexity at a minimum. Your changes, if accepted, may be heavily
modified prior to merging. You will retain "Author:" attribution for your Git
commits granted that the bulk of your changes remain intact. You may be asked to
rework the submission for style (as explained above) and/or substance. Again, we
strongly recommend discussing any serious submissions with the Spring Framework 
team _prior_ to engaging in serious development work.

Note that you can always force push (`git push -f`) reworked / rebased commits
against the branch used to submit your pull request. In other words, you do not
need to issue a new pull request when asked to make changes.

[help documentation]: http://help.github.com/send-pull-requests
[JIRA issue tracker]: https://jira.springsource.org/browse/SPR
[spring-framework-contrib]: https://groups.google.com/forum/#!forum/spring-framework-contrib
[SpringSource CLA form]: https://support.springsource.com/spring_committer_signup
[fork-and-edit]: https://github.com/blog/844-forking-with-the-edit-button
[Spring Code Style]: https://github.com/spring-projects/spring-framework/wiki/Spring-Code-Style
[commit guidelines section of Pro Git]: http://progit.org/book/ch5-2.html#commit_guidelines
[building from source]: https://github.com/SpringSource/spring-framework#building-from-source
