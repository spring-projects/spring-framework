# <img src="framework-docs/src/docs/spring-framework.png" width="80" height="80"> Spring Framework [![Build Status](https://ci.spring.io/api/v1/teams/spring-framework/pipelines/spring-framework-6.0.x/jobs/build/badge)](https://ci.spring.io/teams/spring-framework/pipelines/spring-framework-6.0.x?groups=Build") [![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.spring.io/scans?search.rootProjectNames=spring)

## 关于本工程
1. fork自spring官方，方便看源码用。因为平时构建主要用Maven，所以为了随时能自己编译源码并打包，将工程改造成pom构建。
有同样需求的同学欢迎clone ^_^
 
2. 所有本人改造的工程，都会依赖自己打出来的parent父pom。可移步bf-parent这个项目。里面的README.md
也详细记录了本人是如何一步步改造spring成Maven的。有兴趣自己动手也可以参考

3. main分支严格跟踪官方分支，不做任何改动，方便看diff。所以clone其他分支。

4. 只改造一些里程碑版本，并保证一定能本地源码编译通过，打包成功。版本号（分支名，tag名）和官方一一对应。名字上都看得出来

5. 如果你觉得有用，可加星星。


## 重要改动点记录
1. 修改了spring-core.gradle 去除了java版本21。
2. 修改了.gitignore,增加了更多的忽略项
3. 修改了framework-bom.gradle，用于生成根pom。
4. 修改了根目录下的build.gradle,用于生成所有子工程的pom

