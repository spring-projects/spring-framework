The following has been tested against Intellij IDEA 11.0.1

## Steps

_Within your locally cloned spring-framework working directory:_

1. Generate IDEA metadata with `./gradlew cleanIdea idea`
2. Import into IDEA as usual
3. Set the Project JDK as appropriate
4. Add git support
5. Code away

## Known issues

1. MockServletContext and friends will fail to compile in spring-web. To fix this, uncheck the 'export' setting for all servlet-api and tomcat-servlet-api jars. The problem is that spring-web needs Servlet 2.5, but it's picking up Servlet 3.0 from projects that it depends on.
2. spring-context will fail to build because there's a duplicate instance of GroovyMessenger in spring-context/src/test/java/org/springframework/scripting/groovy/Messenger.groovy. The solution to this is not known.  It's not a problem on Eclipse, because Eclipse doesn't automatically compile .groovy files like IDEA (apparently) does.

There are no other known problems at this time.  Please add to this list, and if you're ambitious, consider playing with the Gradle IDEA generation DSL to fix these problems automatically, e.g.:

* http://gradle.org/docs/current/dsl/org.gradle.plugins.ide.idea.model.IdeaProject.html
* http://gradle.org/docs/current/dsl/org.gradle.plugins.ide.idea.model.IdeaModule.html
* http://gradle.org/docs/current/groovydoc/org/gradle/plugins/ide/idea/model/IdeaModule.html

## Tips

In any case, please do not check in your own generated .iml, .ipr, or .iws files. You'll notice these files are already intentionally in .gitignore. The same policy goes for eclipse metadata.

## FAQ

Q. What about IDEA's own [Gradle support](http://confluence.jetbrains.net/display/IDEADEV/Gradle+integration)?

A. Unknown. Please report back if you try it and it goes well for you.
