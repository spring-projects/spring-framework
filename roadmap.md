# Spring 启动路线图

This document outlines the execution order of the Spring Framework startup process, based on `AbstractApplicationContext.refresh()`. It maps user-added comments to the corresponding steps.

## 1. 准备刷新 (Prepare Refresh)
**Location:** `AbstractApplicationContext.java`
- [Line 587](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L587): `// 1. 准备阶段：记录启动时间，设置启动标志等`

## 2. 获取 BeanFactory (Obtain Fresh BeanFactory)
**Location:** `AbstractApplicationContext.java`
- [Line 591](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L591): `// 2.【关键步骤】获取 BeanFactory：加载XML文件，解析成 BeanDefinition，并创建 BeanFactory`

## 3. 准备 BeanFactory (Prepare BeanFactory)
**Location:** `AbstractApplicationContext.java`
- [Line 595](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L595): `// 3. 准备 BeanFactory：配置 BeanFactory，比如设置类加载器、添加一些默认的 BeanPostProcessor`

## 4. BeanFactory 后置处理 (Post Process BeanFactory)
**Location:** `AbstractApplicationContext.java`
- [Line 600](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L600): `// 4. BeanFactory 的后置处理：允许子类对 BeanFactory 进行扩展处理`

## 5. 调用 BeanFactoryPostProcessor
**Location:** `AbstractApplicationContext.java`
- [Line 605](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L605): `// 5.【关键步骤】执行 BeanFactoryPostProcessor：在所有 BeanDefinition 加载完成，但 Bean 实例还未创建时，`
- [Line 606](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L606): `// 对 BeanDefinition 进行修改或增强。`

### ConfigurationClassPostProcessor (配置类后置处理器)
**Location:** `ConfigurationClassPostProcessor.java`
- [Line 327](spring-context/src/main/java/org/springframework/context/annotation/ConfigurationClassPostProcessor.java#L327): `// 增强 @Configuration 类`
- [Line 329](spring-context/src/main/java/org/springframework/context/annotation/ConfigurationClassPostProcessor.java#L329): `// 注册 ImportAwareBeanPostProcessor，以便支持 @Import 以及 ImportAware 的语义`

## 6. 注册 BeanPostProcessor
**Location:** `AbstractApplicationContext.java`
- [Line 610](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L610): `// 6.【关键步骤】注册 BeanPostProcessor：将 BeanPostProcessor 注册到 BeanFactory 中，`
- [Line 611](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L611): `// 它们将在 Bean 实例化过程中被调用。`

## 7. 初始化 MessageSource
**Location:** `AbstractApplicationContext.java`
- [Line 616](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L616): `// 7. 初始化 MessageSource (国际化相关)`

## 8. 初始化事件广播器
**Location:** `AbstractApplicationContext.java`
- [Line 620](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L620): `// 8. 初始化事件广播器`

## 9. 刷新回调 (On Refresh)
**Location:** `AbstractApplicationContext.java`
- [Line 624](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L624): `// 9. onRefresh()：留给子类实现的扩展点`

## 10. 注册监听器
**Location:** `AbstractApplicationContext.java`
- [Line 628](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L628): `// 10. 注册监听器`

## 11. 完成 BeanFactory 初始化
**Location:** `AbstractApplicationContext.java`
- [Line 632](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L632): `// 11.【关键步骤】完成 BeanFactory 的初始化：实例化所有剩余的非懒加载的单例 Bean`
- [Line 994](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L994): `// 创建所有非懒加载的单例 Bean 实例`

### Bean 实例化 (doGetBean)
**Location:** `AbstractBeanFactory.java`
- [Line 244](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractBeanFactory.java#L244): `// 检查单例缓存`
- [Line 256](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractBeanFactory.java#L256): `// 缓存中获取到的 sharedInstance 可能是一个普通的 Bean，也可能是一个 FactoryBean。`
- [Line 266](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractBeanFactory.java#L266): `// 检查原型 Bean 的循环依赖`
- [Line 274](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractBeanFactory.java#L274): `// 如果当前容器中没有这个 Bean 的定义，尝试从父容器中获取`
- [Line 297](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractBeanFactory.java#L297): `// 将当前 Bean 标记为“已创建”（实际上是“开始创建”），用于循环依赖检查`
- [Line 312](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractBeanFactory.java#L312): `// 处理 depends-on 依赖`
- [Line 348](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractBeanFactory.java#L348): `// 根据不同的 scope 创建 Bean 实例`
- [Line 350](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractBeanFactory.java#L350): `// 单例 (Singleton)`
- [Line 363](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractBeanFactory.java#L363): `// 如果创建失败，需要清理缓存，因为在解决循环依赖时可能已经提前放入了不完整的 Bean`
- [Line 373](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractBeanFactory.java#L373): `// 原型 (Prototype)`

### 单例注册表 (循环依赖)
**Location:** `DefaultSingletonBeanRegistry.java`
- [Line 210](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultSingletonBeanRegistry.java#L210): `// 1. 快速路径检查：首先检查一级缓存 (singletonObjects)`
- [Line 213](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultSingletonBeanRegistry.java#L213): `// 2. 如果一级缓存中没有，并且当前 Bean 正在创建中（这是循环依赖的典型特征）`
- [Line 215](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultSingletonBeanRegistry.java#L215): `// 3. 接着检查二级缓存 (earlySingletonObjects)`
- [Line 218](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultSingletonBeanRegistry.java#L218): `// 4. 如果二级缓存中也没有，并且允许早期引用`
- [Line 227](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultSingletonBeanRegistry.java#L227): `// 5. 【双重检查锁定】进入同步代码块后，再次检查一、二级缓存。`
- [Line 233](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultSingletonBeanRegistry.java#L233): `// 6. 如果一、二级缓存都没有，尝试从三级缓存 (singletonFactories) 中获取 ObjectFactory。`
- [Line 237](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultSingletonBeanRegistry.java#L237): `// 7. 【核心】如果工厂存在，则调用 getObject() 方法创建早期引用。`
- [Line 241](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultSingletonBeanRegistry.java#L241): `// 8. 【升级】将创建出的早期引用从三级缓存移动到二级缓存。`
- [Line 257](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultSingletonBeanRegistry.java#L257): `// 9. 释放锁。`
- [Line 262](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultSingletonBeanRegistry.java#L262): `// 10. 返回找到的 Bean 实例`

### Bean 创建详情 (createBean & doCreateBean)
**Location:** `AbstractAutowireCapableBeanFactory.java`
- [Line 500](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory.java#L500): `// 1. 解析 Bean 的 Class 类型`
- [Line 523](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory.java#L523): `// 2. 【重要】实例化前的后置处理（AOP 关键切入点）`
- [Line 538](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory.java#L538): `// 3. 核心创建逻辑的委派`
- [Line 576](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory.java#L576): `// 1. 【实例化】`
- [Line 613](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory.java#L613): `// 2. 【解决循环依赖的关键】提前曝光单例`
- [Line 629](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory.java#L629): `// 3. 【初始化 Bean】`
- [Line 633](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory.java#L633): `// 3.1 【属性填充】`
- [Line 637](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory.java#L637): `// 3.2 【初始化】`
- [Line 656](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory.java#L656): `// 4. 【循环依赖的最终检查】`
- [Line 693](spring-beans/src/main/java/org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory.java#L693): `// 5. 【注册销毁逻辑】`

### 预实例化单例
**Location:** `DefaultListableBeanFactory.java`
- [Line 1124](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultListableBeanFactory.java#L1124): `// 等待所有后台初始化结束`
- [Line 1133](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultListableBeanFactory.java#L1133): `// 触发 SmartInitializingSingleton 回调`
- [Line 1135](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultListableBeanFactory.java#L1135): `// 从单例池 getSingleton(beanName, false) 拿到已创建好的实例`
- [Line 1140](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultListableBeanFactory.java#L1140): `// 在单例预实例化阶段的最后被调用`
- [Line 1157](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultListableBeanFactory.java#L1157): `// 使用提供的线程池 executor 异步执行 bean 的实例化逻辑`
- [Line 1161](spring-beans/src/main/java/org/springframework/beans/factory/support/DefaultListableBeanFactory.java#L1161): `// 早期暴露`

### AOP 代理创建
**Location:** `AbstractAutoProxyCreator.java`
- [Line 291](spring-aop/src/main/java/org/springframework/aop/framework/autoproxy/AbstractAutoProxyCreator.java#L291): `// 2. --- 核心判断逻辑 ---`
- [Line 364](spring-aop/src/main/java/org/springframework/aop/framework/autoproxy/AbstractAutoProxyCreator.java#L364): `// --- 第一部分：快速失败与跳过检查 ---`
- [Line 383](spring-aop/src/main/java/org/springframework/aop/framework/autoproxy/AbstractAutoProxyCreator.java#L383): `// --- 第二部分：核心代理创建逻辑 ---`
- [Line 386](spring-aop/src/main/java/org/springframework/aop/framework/autoproxy/AbstractAutoProxyCreator.java#L386): `// 1. 【核心】为当前 Bean 查找匹配的通知（Advices）和顾问（Advisors）`
- [Line 395](spring-aop/src/main/java/org/springframework/aop/framework/autoproxy/AbstractAutoProxyCreator.java#L395): `// 3. 【执行】创建代理对象`
- [Line 508](spring-aop/src/main/java/org/springframework/aop/framework/autoproxy/AbstractAutoProxyCreator.java#L508): `// --- 准备阶段：配置装配工具 (ProxyFactory) ---`
- [Line 523](spring-aop/src/main/java/org/springframework/aop/framework/autoproxy/AbstractAutoProxyCreator.java#L523): `// --- 决策阶段：选择代理策略（用 CGLIB 还是 JDK 动态代理？）---`
- [Line 563](spring-aop/src/main/java/org/springframework/aop/framework/autoproxy/AbstractAutoProxyCreator.java#L563): `// --- 组装阶段：将 AOP 组件装配到工厂 ---`
- [Line 575](spring-aop/src/main/java/org/springframework/aop/framework/autoproxy/AbstractAutoProxyCreator.java#L575): `// --- 生产阶段：生成最终的代理对象 ---`

### AOP 代理执行 (运行时)
**Location:** `JdkDynamicAopProxy.java`
- [Line 174](spring-aop/src/main/java/org/springframework/aop/framework/JdkDynamicAopProxy.java#L174): `// --- 第一部分：特殊方法的快速通道处理 ---`
- [Line 202](spring-aop/src/main/java/org/springframework/aop/framework/JdkDynamicAopProxy.java#L202): `// --- 第二部分：准备工作与获取“调用链” ---`
- [Line 220](spring-aop/src/main/java/org/springframework/aop/framework/JdkDynamicAopProxy.java#L220): `// 7. 【核心步骤】获取将要应用于此方法的“拦截器链”。`
- [Line 226](spring-aop/src/main/java/org/springframework/aop/framework/JdkDynamicAopProxy.java#L226): `// --- 第三部分：执行调用链与目标方法 ---`
- [Line 230](spring-aop/src/main/java/org/springframework/aop/framework/JdkDynamicAopProxy.java#L230): `// 8. 如果拦截器链为空，说明没有任何通知需要应用到此方法上。`
- [Line 241](spring-aop/src/main/java/org/springframework/aop/framework/JdkDynamicAopProxy.java#L241): `// 9. 【核心步骤】如果拦截器链不为空，就需要执行 AOP 逻辑。`
- [Line 246](spring-aop/src/main/java/org/springframework/aop/framework/JdkDynamicAopProxy.java#L246): `//    【启动调用链】调用 proceed() 方法，这会像多米诺骨牌一样，`
- [Line 252](spring-aop/src/main/java/org/springframework/aop/framework/JdkDynamicAopProxy.java#L252): `// --- 第四部分：返回值处理 ---`
- [Line 255](spring-aop/src/main/java/org/springframework/aop/framework/JdkDynamicAopProxy.java#L255): `// 10. 对返回值进行一些特殊处理。`
- [Line 280](spring-aop/src/main/java/org/springframework/aop/framework/JdkDynamicAopProxy.java#L280): `// --- 第五部分：清理工作 ---`

**Location:** `ReflectiveMethodInvocation.java`
- [Line 157](spring-aop/src/main/java/org/springframework/aop/framework/ReflectiveMethodInvocation.java#L157): `// 1. 判断是否已执行完所有拦截器 (递归的终止条件)`
- [Line 166](spring-aop/src/main/java/org/springframework/aop/framework/ReflectiveMethodInvocation.java#L166): `// 2. 获取下一个要执行的拦截器`
- [Line 172](spring-aop/src/main/java/org/springframework/aop/framework/ReflectiveMethodInvocation.java#L172): `// 3. 判断拦截器的类型`
- [Line 176](spring-aop/src/main/java/org/springframework/aop/framework/ReflectiveMethodInvocation.java#L176): `// 3.1 类型一：动态匹配的拦截器`
- [Line 199](spring-aop/src/main/java/org/springframework/aop/framework/ReflectiveMethodInvocation.java#L199): `// 3.2 类型二：静态匹配的拦截器`

## 12. 完成刷新
**Location:** `AbstractApplicationContext.java`
- [Line 636](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L636): `// 12. 完成刷新过程：发布容器刷新完成事件`

### 清理与生命周期
**Location:** `AbstractApplicationContext.java`
- [Line 1005](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L1005): `// 1. 清理临时缓存`
- [Line 1016](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L1016): `// 2. 初始化生命周期处理器`
- [Line 1022](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L1022): `// 3. 触发 Lifecycle Bean 的启动`
- [Line 1029](spring-context/src/main/java/org/springframework/context/support/AbstractApplicationContext.java#L1029): `// 4. 发布“容器刷新完成”事件`
