/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * The root interface for accessing a Spring bean container.
 *
 * <p>This is the basic client view of a bean container;
 * further interfaces such as {@link ListableBeanFactory} and
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * are available for specific purposes.
 *
 * <p>This interface is implemented by objects that hold a number of bean definitions,
 * each uniquely identified by a String name. Depending on the bean definition,
 * the factory will return either an independent instance of a contained object
 * (the Prototype design pattern), or a single shared instance (a superior
 * alternative to the Singleton design pattern, in which the instance is a
 * singleton in the scope of the factory). Which type of instance will be returned
 * depends on the bean factory configuration: the API is the same. Since Spring
 * 2.0, further scopes are available depending on the concrete application
 * context (e.g. "request" and "session" scopes in a web environment).
 *
 * <p>The point of this approach is that the BeanFactory is a central registry
 * of application components, and centralizes configuration of application
 * components (no more do individual objects need to read properties files,
 * for example). See chapters 4 and 11 of "Expert One-on-One J2EE Design and
 * Development" for a discussion of the benefits of this approach.
 *
 * <p>Note that it is generally better to rely on Dependency Injection
 * ("push" configuration) to configure application objects through setters
 * or constructors, rather than use any form of "pull" configuration like a
 * BeanFactory lookup. Spring's Dependency Injection functionality is
 * implemented using this BeanFactory interface and its subinterfaces.
 *
 * <p>Normally a BeanFactory will load bean definitions stored in a configuration
 * source (such as an XML document), and use the {@code org.springframework.beans}
 * package to configure the beans. However, an implementation could simply return
 * Java objects it creates as necessary directly in Java code. There are no
 * constraints on how the definitions could be stored: LDAP, RDBMS, XML,
 * properties file, etc. Implementations are encouraged to support references
 * amongst beans (Dependency Injection).
 *
 * <p>In contrast to the methods in {@link ListableBeanFactory}, all of the
 * operations in this interface will also check parent factories if this is a
 * {@link HierarchicalBeanFactory}. If a bean is not found in this factory instance,
 * the immediate parent factory will be asked. Beans in this factory instance
 * are supposed to override beans of the same name in any parent factory.
 *
 * <p>Bean factory implementations should support the standard bean lifecycle interfaces
 * as far as possible. The full set of initialization methods and their standard order is:
 * <ol>
 * <li>BeanNameAware's {@code setBeanName}
 * <li>BeanClassLoaderAware's {@code setBeanClassLoader}
 * <li>BeanFactoryAware's {@code setBeanFactory}
 * <li>EnvironmentAware's {@code setEnvironment}
 * <li>EmbeddedValueResolverAware's {@code setEmbeddedValueResolver}
 * <li>ResourceLoaderAware's {@code setResourceLoader}
 * (only applicable when running in an application context)
 * <li>ApplicationEventPublisherAware's {@code setApplicationEventPublisher}
 * (only applicable when running in an application context)
 * <li>MessageSourceAware's {@code setMessageSource}
 * (only applicable when running in an application context)
 * <li>ApplicationContextAware's {@code setApplicationContext}
 * (only applicable when running in an application context)
 * <li>ServletContextAware's {@code setServletContext}
 * (only applicable when running in a web application context)
 * <li>{@code postProcessBeforeInitialization} methods of BeanPostProcessors
 * <li>InitializingBean's {@code afterPropertiesSet}
 * <li>a custom {@code init-method} definition
 * <li>{@code postProcessAfterInitialization} methods of BeanPostProcessors
 * </ol>
 *
 * <p>On shutdown of a bean factory, the following lifecycle methods apply:
 * <ol>
 * <li>{@code postProcessBeforeDestruction} methods of DestructionAwareBeanPostProcessors
 * <li>DisposableBean's {@code destroy}
 * <li>a custom {@code destroy-method} definition
 * </ol>
 *
 * <p>bean工厂(BeanFactory)
 * <p>用于访问Spring bean容器的根接口, 获取bean及bean的各种属性
 * <p>这是bean容器的基本客户端视图；
 * 其他接口, 如{@link ListableBeanFactory}和{@link org.springframework.beans.factory.config.ConfigurableBeanFactory},
 * 可用于特定目的
 * <p> 此接口由包含多个bean定义的对象实现, 每个bean定义都由一个String名称唯一标识。
 * 根据bean定义, 工厂将返回包含对象的独立实例（原型设计模式）或单个共享实例（Singleton设计模式的高级替代方案, 其中实例是工厂范围内的单例）。
 * 返回哪种类型的实例取决于bean工厂配置：API是相同的。
 * 自Spring 2.0以来, 根据具体的应用程序上下文, 可以使用更多的作用域（例如web环境中的“请求”和“会话”作用域）
 * <p> 这种方法的要点是BeanFactory是应用程序组件的中央注册表, 并集中配置应用程序组件（例如, 单个对象不再需要读取属性文件）。
 * 关于这种方法的好处的讨论, 请参阅“专家一对一J2EE设计和开发”的第4章和第11章
 * <p> 请注意, 通常最好依靠依赖注入（“推送”配置）通过setter或构造函数配置应用程序对象, 而不是使用任何形式的“拉取”配置, 如BeanFactory查找。
 * Spring的依赖注入功能是使用此BeanFactory接口及其子接口实现的
 * <p> 通常, BeanFactory会加载存储在配置源（如XML文档）中的bean定义, 并使用{@code org.springframework.beans}包来配置bean。
 * 然而, 实现可以简单地返回它在必要时直接在Java代码中创建的Java对象。对于定义的存储方式没有限制：LDAP、RDBMS、XML、属性文件等。
 * 鼓励实现支持bean之间的引用（依赖注入）<p> 与{@link ListableBeanFactory}中的方法不同, 如果这是{@link HierarchicalBeanFactory},
 * 则此接口中的所有操作也将检查父工厂。如果在此工厂实例中找不到bean, 将询问直接父工厂。此工厂实例中的Bean应该覆盖任何父工厂中同名的Bean
 * <p> Bean工厂实现应尽可能支持标准Bean生命周期接口。完整的初始化方法及其标准顺序是：
 * <ol>
 * <li>BeanNameAware的{@code setBeanName}
 * <li>BeanClassLoaderAware的{@code setBeanClassLoader}
 * <li>BeanFactory Aware的{1@code setBean Factory}
 * <li]EnvironmentAware的{0@code setEnvironment}
 * <li>EmbeddedValueResolverWare的{@code setEmbeddedValueResolver}
 * <li>ResourceLoaderAware的{@code setResourceLoader}(仅适用于在应用程序上下文中运行时)
 * <li>ApplicationEventPublisherAware
 * <li>MessageSourceAware的{@code setMessageSource}(仅适用于在应用程序上下文中运行时)
 * <li>ApplicationContextAware的{@code setApplicationContext}
 * <li>BeanPostProcessors的自定义{@code init方法}定义
 * <li>{@code postProcessAfterInitialization}方法
 * </ol>
 * <p>关闭bean工厂时, 应用以下生命周期方法：
 * <ol>
 * <li>{@code postProcessBeforeDestruction}销毁方法WealthBeanPostProcessers
 * <li>DisposableBean的{@code destruction}
 * <li>自定义{@code Destruction方法}定义
 * </ol>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see BeanNameAware#setBeanName
 * @see BeanClassLoaderAware#setBeanClassLoader
 * @see BeanFactoryAware#setBeanFactory
 * @see org.springframework.context.EnvironmentAware#setEnvironment
 * @see org.springframework.context.EmbeddedValueResolverAware#setEmbeddedValueResolver
 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader
 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher
 * @see org.springframework.context.MessageSourceAware#setMessageSource
 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
 * @see org.springframework.web.context.ServletContextAware#setServletContext
 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization
 * @see InitializingBean#afterPropertiesSet
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getInitMethodName
 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization
 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor#postProcessBeforeDestruction
 * @see DisposableBean#destroy
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getDestroyMethodName
 * @since 13 April 2001
 */
public interface BeanFactory {

	/**
	 * Used to dereference a {@link FactoryBean} instance and distinguish it from
	 * beans <i>created</i> by the FactoryBean. For example, if the bean named
	 * {@code myJndiObject} is a FactoryBean, getting {@code &myJndiObject}
	 * will return the factory, not the instance returned by the factory.
	 *
	 * <p>用于区分和FactoryBean创建的bean
	 * <p>为了区分“FactoryBean” 和 “FactoryBean 创建的 bean实例”, Spring 使用了 “&” 前缀
	 * 假设我们的 beanName 为 apple, 则 getBean("apple") 获得的是 AppleFactoryBean 通过 getObject()方法创建的 bean 实例
	 * 而getBean("&apple") 获得的是 AppleFactoryBean 本身
	 */
	String FACTORY_BEAN_PREFIX = "&";


	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * <p>This method allows a Spring BeanFactory to be used as a replacement for the
	 * Singleton or Prototype design pattern. Callers may retain references to
	 * returned objects in the case of Singleton beans.
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 *
	 * <p>从容器中返回指定名称的Bean
	 *
	 * @param name the name of the bean to retrieve
	 * @return an instance of the bean.
	 * Note that the return value will never be {@code null} but possibly a stub for
	 * {@code null} returned from a factory method, to be checked via {@code equals(null)}.
	 * Consider using {@link #getBeanProvider(Class)} for resolving optional dependencies.
	 * @throws NoSuchBeanDefinitionException if there is no bean with the specified name
	 * @throws BeansException                if the bean could not be obtained
	 */
	Object getBean(String name) throws BeansException;

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * <p>Behaves the same as {@link #getBean(String)}, but provides a measure of type
	 * safety by throwing a BeanNotOfRequiredTypeException if the bean is not of the
	 * required type. This means that ClassCastException can't be thrown on casting
	 * the result correctly, as can happen with {@link #getBean(String)}.
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 *
	 * <p>从容器中返回指定名称并类型为指定类型的Bean
	 *
	 * @param name         the name of the bean to retrieve
	 * @param requiredType type the bean must match; can be an interface or superclass
	 * @return an instance of the bean.
	 * Note that the return value will never be {@code null}. In case of a stub for
	 * {@code null} from a factory method having been resolved for the requested bean, a
	 * {@code BeanNotOfRequiredTypeException} against the NullBean stub will be raised.
	 * Consider using {@link #getBeanProvider(Class)} for resolving optional dependencies.
	 * @throws NoSuchBeanDefinitionException  if there is no such bean definition
	 * @throws BeanNotOfRequiredTypeException if the bean is not of the required type
	 * @throws BeansException                 if the bean could not be created
	 */
	<T> T getBean(String name, Class<T> requiredType) throws BeansException;

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * <p>Allows for specifying explicit constructor arguments / factory method arguments,
	 * overriding the specified default arguments (if any) in the bean definition.
	 *
	 * <p>指定名称 && 指定形参的构造器或工厂方法
	 *
	 * @param name the name of the bean to retrieve
	 * @param args arguments to use when creating a bean instance using explicit arguments
	 *             (only applied when creating a new instance as opposed to retrieving an existing one)
	 * @return an instance of the bean
	 * @throws NoSuchBeanDefinitionException if there is no such bean definition
	 * @throws BeanDefinitionStoreException  if arguments have been given but
	 *                                       the affected bean isn't a prototype
	 * @throws BeansException                if the bean could not be created
	 * @since 2.5
	 */
	Object getBean(String name, Object... args) throws BeansException;

	/**
	 * Return the bean instance that uniquely matches the given object type, if any.
	 * <p>This method goes into {@link ListableBeanFactory} by-type lookup territory
	 * but may also be translated into a conventional by-name lookup based on the name
	 * of the given type. For more extensive retrieval operations across sets of beans,
	 * use {@link ListableBeanFactory} and/or {@link BeanFactoryUtils}.
	 *
	 * <p>从容器中获取指定类型的Bean
	 *
	 * @param requiredType type the bean must match; can be an interface or superclass
	 * @return an instance of the single bean matching the required type
	 * @throws NoSuchBeanDefinitionException   if no bean of the given type was found
	 * @throws NoUniqueBeanDefinitionException if more than one bean of the given type was found
	 * @throws BeansException                  if the bean could not be created
	 * @see ListableBeanFactory
	 * @since 3.0
	 */
	<T> T getBean(Class<T> requiredType) throws BeansException;

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * <p>Allows for specifying explicit constructor arguments / factory method arguments,
	 * overriding the specified default arguments (if any) in the bean definition.
	 * <p>This method goes into {@link ListableBeanFactory} by-type lookup territory
	 * but may also be translated into a conventional by-name lookup based on the name
	 * of the given type. For more extensive retrieval operations across sets of beans,
	 * use {@link ListableBeanFactory} and/or {@link BeanFactoryUtils}.
	 *
	 * <p>指定类型 && 指定形参的构造器或工厂方法
	 *
	 * @param requiredType type the bean must match; can be an interface or superclass
	 * @param args         arguments to use when creating a bean instance using explicit arguments
	 *                     (only applied when creating a new instance as opposed to retrieving an existing one)
	 * @return an instance of the bean
	 * @throws NoSuchBeanDefinitionException if there is no such bean definition
	 * @throws BeanDefinitionStoreException  if arguments have been given but
	 *                                       the affected bean isn't a prototype
	 * @throws BeansException                if the bean could not be created
	 * @since 4.1
	 */
	<T> T getBean(Class<T> requiredType, Object... args) throws BeansException;

	/**
	 * Return a provider for the specified bean, allowing for lazy on-demand retrieval
	 * of instances, including availability and uniqueness options.
	 * <p>For matching a generic type, consider {@link #getBeanProvider(ResolvableType)}.
	 *
	 * <p>返回对应的 ObjectProvider, 这是一种可以延迟获取 bean 实例的模式
	 * <p>比如基于 ObjectProvider#getObject 方法获取实例
	 *
	 * @param requiredType type the bean must match; can be an interface or superclass
	 * @return a corresponding provider handle
	 * @see #getBeanProvider(ResolvableType)
	 * @since 5.1
	 */
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType);

	/**
	 * Return a provider for the specified bean, allowing for lazy on-demand retrieval
	 * of instances, including availability and uniqueness options. This variant allows
	 * for specifying a generic type to match, similar to reflective injection points
	 * with generic type declarations in method/constructor parameters.
	 * <p>Note that collections of beans are not supported here, in contrast to reflective
	 * injection points. For programmatically retrieving a list of beans matching a
	 * specific type, specify the actual bean type as an argument here and subsequently
	 * use {@link ObjectProvider#orderedStream()} or its lazy streaming/iteration options.
	 * <p>Also, generics matching is strict here, as per the Java assignment rules.
	 * For lenient fallback matching with unchecked semantics (similar to the ´unchecked´
	 * Java compiler warning), consider calling {@link #getBeanProvider(Class)} with the
	 * raw type as a second step if no full generic match is
	 * {@link ObjectProvider#getIfAvailable() available} with this variant.
	 *
	 * <p>返回对应的 ObjectProvider, 这是一种可以延迟获取 bean 实例的模式
	 *
	 * @param requiredType type the bean must match; can be a generic type declaration
	 * @return a corresponding provider handle
	 * @see ObjectProvider#iterator()
	 * @see ObjectProvider#stream()
	 * @see ObjectProvider#orderedStream()
	 * @since 5.1
	 */
	<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType);

	/**
	 * Does this bean factory contain a bean definition or externally registered singleton
	 * instance with the given name?
	 * <p>If the given name is an alias, it will be translated back to the corresponding
	 * canonical bean name.
	 * <p>If this factory is hierarchical, will ask any parent factory if the bean cannot
	 * be found in this factory instance.
	 * <p>If a bean definition or singleton instance matching the given name is found,
	 * this method will return {@code true} whether the named bean definition is concrete
	 * or abstract, lazy or eager, in scope or not. Therefore, note that a {@code true}
	 * return value from this method does not necessarily indicate that {@link #getBean}
	 * will be able to obtain an instance for the same name.
	 * <p>
	 * <p>是否包含指定 name 的 bean definition 或 单例
	 * <p>此处返回 true 并不意味着 getBean 能返回对应实例
	 * 1）支持别名解析
	 * 2）支持层级查找
	 *
	 * @param name the name of the bean to query
	 * @return whether a bean with the given name is present
	 */
	boolean containsBean(String name);

	/**
	 * Is this bean a shared singleton? That is, will {@link #getBean} always
	 * return the same instance?
	 * <p>Note: This method returning {@code false} does not clearly indicate
	 * independent instances. It indicates non-singleton instances, which may correspond
	 * to a scoped bean as well. Use the {@link #isPrototype} operation to explicitly
	 * check for independent instances.
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 *
	 * <p>name 对应的 bean 是否单例（singleton）
	 *
	 * @param name the name of the bean to query
	 * @return whether this bean corresponds to a singleton instance
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @see #getBean
	 * @see #isPrototype
	 */
	boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

	/**
	 * Is this bean a prototype? That is, will {@link #getBean} always return
	 * independent instances?
	 * <p>Note: This method returning {@code false} does not clearly indicate
	 * a singleton object. It indicates non-independent instances, which may correspond
	 * to a scoped bean as well. Use the {@link #isSingleton} operation to explicitly
	 * check for a shared singleton instance.
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 *
	 * <p>name 对应的 bean 是否原型（prototype）
	 *
	 * @param name the name of the bean to query
	 * @return whether this bean will always deliver independent instances
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @see #getBean
	 * @see #isSingleton
	 * @since 2.0.3
	 */
	boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

	/**
	 * Check whether the bean with the given name matches the specified type.
	 * More specifically, check whether a {@link #getBean} call for the given name
	 * would return an object that is assignable to the specified target type.
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 *
	 * <p>指定 name 的 bean 是否匹配对应类型
	 *
	 * @param name        the name of the bean to query
	 * @param typeToMatch the type to match against (as a {@code ResolvableType})
	 * @return {@code true} if the bean type matches,
	 * {@code false} if it doesn't match or cannot be determined yet
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @see #getBean
	 * @see #getType
	 * @since 4.2
	 */
	boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * Check whether the bean with the given name matches the specified type.
	 * More specifically, check whether a {@link #getBean} call for the given name
	 * would return an object that is assignable to the specified target type.
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 *
	 * <p>指定 name 的 bean 是否匹配对应类型
	 *
	 * @param name        the name of the bean to query
	 * @param typeToMatch the type to match against (as a {@code Class})
	 * @return {@code true} if the bean type matches,
	 * {@code false} if it doesn't match or cannot be determined yet
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @see #getBean
	 * @see #getType
	 * @since 2.0.1
	 */
	boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * Determine the type of the bean with the given name. More specifically,
	 * determine the type of object that {@link #getBean} would return for the given name.
	 * <p>For a {@link FactoryBean}, return the type of object that the FactoryBean creates,
	 * as exposed by {@link FactoryBean#getObjectType()}. This may lead to the initialization
	 * of a previously uninitialized {@code FactoryBean} (see {@link #getType(String, boolean)}).
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 *
	 * <p>获取指定 name 的 bean 实例的类型, 如果目标 bean 实例是一个 FactoryBean
	 * 则返回的使其创建实例的类型, 即 FactoryBean#getObjectType
	 * 这会造成 FactoryBean 的提前实例化
	 * 1）支持别名解析
	 * 2）支持层级查找
	 *
	 * @param name the name of the bean to query
	 * @return the type of the bean, or {@code null} if not determinable
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @see #getBean
	 * @see #isTypeMatch
	 * @since 1.1.2
	 */
	@Nullable
	Class<?> getType(String name) throws NoSuchBeanDefinitionException;

	/**
	 * Determine the type of the bean with the given name. More specifically,
	 * determine the type of object that {@link #getBean} would return for the given name.
	 * <p>For a {@link FactoryBean}, return the type of object that the FactoryBean creates,
	 * as exposed by {@link FactoryBean#getObjectType()}. Depending on the
	 * {@code allowFactoryBeanInit} flag, this may lead to the initialization of a previously
	 * uninitialized {@code FactoryBean} if no early type information is available.
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 *
	 * <p>在 getType(String name) 的基础上由参数 allowFactoryBeanInit
	 * <p>决定是否允许 FactoryBean 的提前初始化
	 *
	 * @param name                 the name of the bean to query
	 * @param allowFactoryBeanInit whether a {@code FactoryBean} may get initialized
	 *                             just for the purpose of determining its object type
	 * @return the type of the bean, or {@code null} if not determinable
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @see #getBean
	 * @see #isTypeMatch
	 * @since 5.2
	 */
	@Nullable
	Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException;

	/**
	 * Return the aliases for the given bean name, if any.
	 * <p>All of those aliases point to the same bean when used in a {@link #getBean} call.
	 * <p>If the given name is an alias, the corresponding original bean name
	 * and other aliases (if any) will be returned, with the original bean name
	 * being the first element in the array.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 *
	 * <p>返回指定 name 的所有别名, 这意味这基于这些 name 获取的 bean 实例都是同一个
	 * <p>如果这个地方传入的是一个别名, 那么会返回 bean 原始名称和别名组成的数组
	 * <p>支持层级查找
	 *
	 * @param name the bean name to check for aliases
	 * @return the aliases, or an empty array if none
	 * @see #getBean
	 */
	String[] getAliases(String name);

}
