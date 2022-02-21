package com.pengbin.spring;

import com.pengbin.spring.config.SysConfig;
import com.pengbin.spring.pojo.SysUser;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 1. Core Container
 * Core Container(核心容器)包含有Core、Beans、Context和Expression Language模块
 * Core和Beans模块是框架的基础部分，提供IoC(转控制)和依赖注入特性。这里的基础概念是BeanFactory，它提供对Factory模式的经典实现来消除对程序性单例模式的需要，并真正地允许你从程序逻辑中分离出依赖关系和配置。
 *
 * Core模块主要包含Spring框架基本的核心工具类
 * Beans模块是所有应用都要用到的，它包含访问配置文件、创建和管理bean以及进行Inversion of Control/Dependency Injection(Ioc/DI)操作相关的所有类
 * Context模块构建于Core和Beans模块基础之上，提供了一种类似于JNDI注册器的框架式的对象访问方法。Context模块继承了Beans的特性，为Spring核心提供了大量扩展，添加了对国际化(如资源绑定)、事件传播、资源加载和对Context的透明创建的支持。ApplicationContext接口是Context模块的关键
 * Expression Language模块提供了一个强大的表达式语言用于在运行时查询和操纵对象，该语言支持设置/获取属性的值，属性的分配，方法的调用，访问数组上下文、容器和索引器、逻辑和算术运算符、命名变量以及从Spring的IoC容器中根据名称检索对象
 *
 * 2. Data Access/Integration
 * JDBC模块提供了一个JDBC抽象层，它可以消除冗长的JDBC编码和解析数据库厂商特有的错误代码，这个模块包含了Spring对JDBC数据访问进行封装的所有类
 * ORM模块为流行的对象-关系映射API，如JPA、JDO、Hibernate、iBatis等，提供了一个交互层，利用ORM封装包，可以混合使用所有Spring提供的特性进行O/R映射，如前边提到的简单声明性事务管理
 * OXM模块提供了一个Object/XML映射实现的抽象层，Object/XML映射实现抽象层包括JAXB，Castor，XMLBeans，JiBX和XStream
 * JMS（java Message Service）模块主要包含了一些制造和消费消息的特性
 * Transaction模块支持编程和声明式事物管理，这些事务类必须实现特定的接口，并且对所有POJO都适用
 *
 * 3. Web
 * Web上下文模块建立在应用程序上下文模块之上，为基于Web的应用程序提供了上下文，所以Spring框架支持与Jakarta Struts的集成。Web模块还简化了处理多部分请求以及将请求参数绑定到域对象的工作。Web层包含了Web、Web-Servlet、Web-Struts和Web、Porlet模块
 * Web模块：提供了基础的面向Web的集成特性，例如，多文件上传、使用Servlet
 * listeners初始化IoC容器以及一个面向Web的应用上下文，它还包含了Spring远程支持中Web的相关部分
 * Web-Servlet模块web.servlet.jar：该模块包含Spring的model-view-controller(MVC)实现，Spring的MVC框架使得模型范围内的代码和web forms之间能够清楚地分离开来，并与Spring框架的其他特性基础在一起
 * Web-Struts模块：该模块提供了对Struts的支持，使得类在Spring应用中能够与一个典型的Struts Web层集成在一起
 * Web-Porlet模块：提供了用于Portlet环境和Web-Servlet模块的MVC的实现
 *
 * 4. AOP
 * AOP模块提供了一个符合AOP联盟标准的面向切面编程的实现，它让你可以定义例如方法拦截器和切点，从而将逻辑代码分开，降低它们之间的耦合性，利用source-level的元数据功能，还可以将各种行为信息合并到你的代码中
 * Spring AOP模块为基于Spring的应用程序中的对象提供了事务管理服务，通过使用Spring AOP，不用依赖EJB组件，就可以将声明性事务管理集成到应用程序中
 *
 * 5. Test
 * Test模块支持使用Junit和TestNG对Spring组件进行测试
 *
 * 了解下spring-bean最核心的两个类：DefaultListableBeanFactory和XmlBeanDefinitionReader
 * DefaultListableBeanFactory
 * XmlBeanFactory继承自DefaultListableBeanFactory，而DefaultListableBeanFactory是整个bean加载的核心部分，
 * 是Spring注册及加载bean的默认实现，而对于XmlBeanFactory与DefaultListableBeanFactory不同的地方其实是在XmlBeanFactory中使用了自定义的XML读取器XmlBeanDefinitionReader，
 * 实现了个性化的BeanDefinitionReader读取，DefaultListableBeanFactory继承了AbstractAutowireCapableBeanFactory并实现了ConfigurableListableBeanFactory以及BeanDefinitionRegistry接口。
 *
 * - AliasRegistry：定义对alias的简单增删改等操作
 * - SimpleAliasRegistry：主要使用map作为alias的缓存，并对接口AliasRegistry进行实现
 * - SingletonBeanRegistry：定义对单例的注册及获取
 * - BeanFactory：定义获取bean及bean的各种属性
 * - DefaultSingletonBeanRegistry：默认对接口SingletonBeanRegistry各函数的实现
 * - HierarchicalBeanFactory：继承BeanFactory，也就是在BeanFactory定义的功能的基础上增加了对parentFactory的支持
 * - BeanDefinitionRegistry：定义对BeanDefinition的各种增删改操作
 * - FactoryBeanRegistrySupport：在DefaultSingletonBeanRegistry基础上增加了对FactoryBean的特殊处理功能
 * - ConfigurableBeanFactory：提供配置Factory的各种方法
 * - ListableBeanFactory：根据各种条件获取bean的配置清单
 * - AbstractBeanFactory：综合FactoryBeanRegistrySupport和ConfigurationBeanFactory的功能
 * - AutowireCapableBeanFactory：提供创建bean、自动注入、初始化以及应用bean的后处理器
 * - AbstractAutowireCapableBeanFactory：综合AbstractBeanFactory并对接口AutowireCapableBeanFactory进行实现
 * - ConfigurableListableBeanFactory：BeanFactory配置清单，指定忽略类型及接口等
 * - DefaultListableBeanFactory：综合上面所有功能，主要是对Bean注册后的处理
 * XmlBeanFactory对DefaultListableBeanFactory类进行了扩展，主要用于从XML文档中读取BeanDefinition，对于注册及获取Bean都是使用从父类DefaultListableBeanFactory继承的方法去实现，而唯独与父类不同的个性化实现就是增加了XmlBeanDefinitionReader类型的reader属性。在XmlBeanFactory中主要使用reader属性对资源文件进行读取和注册
 *
 * XmlBeanDefinitionReader
 * XML配置文件的读取是Spring中重要的功能，因为Spring的大部分功能都是以配置作为切入点的，可以从XmlBeanDefinitionReader中梳理一下资源文件读取、解析及注册的大致脉络，首先看看各个类的功能
 *
 * ResourceLoader：定义资源加载器，主要应用于根据给定的资源文件地址返回对应的Resource
 * BeanDefinitionReader：主要定义资源文件读取并转换为BeanDefinition的各个功能
 * EnvironmentCapable：定义获取Environment方法
 * DocumentLoader：定义从资源文件加载到转换为Document的功能
 * AbstractBeanDefinitionReader：对EnvironmentCapable、BeanDefinitionReader类定义的功能进行实现
 * BeanDefinitionDocumentReader：定义读取Document并注册BeanDefinition功能
 * BeanDefinitionParserDelegate：定义解析Element的各种方法
 *
 *
 * 整个XML配置文件读取的大致流程
 * （1）通过继承自AbstractBeanDefinitionReader中的方法，来使用ResourceLoader将资源文件路径转换为对应的Resource文件
 * （2）通过DocumentLoader对Resource文件进行转换，将Resource文件转换为Document文件
 * （3）通过实现接口BeanDefinitionDocumentReader的DefaultBeanDefinitionDocumentReader类对Document进行解析，并使用BeanDefinitionParserDelegate对Element进行解析
 *
 * profile的用法
 * 通过profile标记不同的环境，可以通过设置spring.profiles.active和spring.profiles.default激活指定profile环境。
 * 如果设置了active，default便失去了作用。如果两个都没有设置，那么带有profiles的bean都不会生成。
 */
public class SpringDemoApplication {
	public static void main(String[] args) {
		// 获取容器
		ApplicationContext ac =new AnnotationConfigApplicationContext(SysConfig.class);
		// 获取 bean
		SysUser user = (SysUser) ac.getBean("sysUser");
		System.out.println(user.toString());
	}
}
