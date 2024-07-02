/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

import java.beans.PropertyEditor;
import java.security.AccessControlContext;

/**
 * Configuration interface to be implemented by most bean factories. Provides
 * facilities to configure a bean factory, in addition to the bean factory
 * client methods in the {@link org.springframework.beans.factory.BeanFactory}
 * interface.
 *
 * <p>This bean factory interface is not meant to be used in normal application
 * code: Stick to {@link org.springframework.beans.factory.BeanFactory} or
 * {@link org.springframework.beans.factory.ListableBeanFactory} for typical
 * needs. This extended interface is just meant to allow for framework-internal
 * plug'n'play and for special access to bean factory configuration methods.
 *
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.beans.factory.ListableBeanFactory
 * @see ConfigurableListableBeanFactory
 * @since 03.11.2003
 */
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {

	/**
	 * Scope identifier for the standard singleton scope: {@value}.
	 * <p>Custom scopes can be added via {@code registerScope}.
	 * 标准单例作用域的作用域标识符：{@value}
	 * 自定义作用域可以通过{@code registerScope}添加。
	 *
	 * @see #registerScope
	 */
	String SCOPE_SINGLETON = "singleton";

	/**
	 * Scope identifier for the standard prototype scope: {@value}.
	 * <p>Custom scopes can be added via {@code registerScope}.
	 * 标准原型作用域的作用域标识符: {@value}
	 * 自定义作用域可以通过{@code registerScope}添加。
	 *
	 * @see #registerScope
	 */
	String SCOPE_PROTOTYPE = "prototype";


	/**
	 * Set the parent of this bean factory.
	 * <p>Note that the parent cannot be changed: It should only be set outside
	 * a constructor if it isn't available at the time of factory instantiation.
	 * 设置这个bean工厂的父级
	 * 请注意，不能更改父级：只有在工厂实例化时父级不可用时，才应在构造函数外部设置它
	 *
	 * @param parentBeanFactory the parent BeanFactory
	 * @throws IllegalStateException if this factory is already associated with
	 *                               a parent BeanFactory
	 * @see #getParentBeanFactory()
	 */
	void setParentBeanFactory(BeanFactory parentBeanFactory) throws IllegalStateException;

	/**
	 * Set the class loader to use for loading bean classes.
	 * Default is the thread context class loader.
	 * <p>Note that this class loader will only apply to bean definitions
	 * that do not carry a resolved bean class yet. This is the case as of
	 * Spring 2.0 by default: Bean definitions only carry bean class names,
	 * to be resolved once the factory processes the bean definition.设置用于加载bean类的类加载器。默认为线程上下文类加载器
	 * 请注意，这个类加载器将仅适用于尚未携带已解析bean类的bean定义。
	 * 默认情况下，这是Spring 2.0的情况：Bean定义只携带Bean类名，一旦工厂处理了Bean定义，就会解析。
	 *
	 * @param beanClassLoader the class loader to use,
	 *                        or {@code null} to suggest the default class loader
	 */
	void setBeanClassLoader(@Nullable ClassLoader beanClassLoader);

	/**
	 * Return this factory's class loader for loading bean classes
	 * (only {@code null} if even the system ClassLoader isn't accessible).
	 * 返回此工厂的类加载器以加载bean类
	 * (如果系统ClassLoader不可访问，则仅{@code null})。
	 *
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getBeanClassLoader();

	/**
	 * Specify a temporary ClassLoader to use for type matching purposes.
	 * Default is none, simply using the standard bean ClassLoader.
	 * <p>A temporary ClassLoader is usually just specified if
	 * <i>load-time weaving</i> is involved, to make sure that actual bean
	 * classes are loaded as lazily as possible. The temporary loader is
	 * then removed once the BeanFactory completes its bootstrap phase.
	 * 指定用于类型匹配目的的临时ClassLoader。默认值为none，只需使用标准的bean ClassLoader
	 * 如果涉及＜i＞加载时编织＜i＞，通常只指定临时ClassLoader，以确保尽可能延迟地加载实际的bean类。
	 * 一旦BeanFactory完成其引导阶段，就会移除临时加载程序。
	 *
	 * @since 2.5
	 */
	void setTempClassLoader(@Nullable ClassLoader tempClassLoader);

	/**
	 * Return the temporary ClassLoader to use for type matching purposes,
	 * if any.
	 * 返回临时ClassLoader以用于类型匹配目的（如果有的话）。
	 *
	 * @since 2.5
	 */
	@Nullable
	ClassLoader getTempClassLoader();

	/**
	 * Set whether to cache bean metadata such as given bean definitions
	 * (in merged fashion) and resolved bean classes. Default is on.
	 * <p>Turn this flag off to enable hot-refreshing of bean definition objects
	 * and in particular bean classes. If this flag is off, any creation of a bean
	 * instance will re-query the bean class loader for newly resolved classes.
	 * 设置是否缓存bean元数据，例如给定的bean定义（以合并的方式）和已解析的bean类。默认值为on。
	 * 关闭此标志可以启用bean定义对象的热刷新，尤其是bean类。如果该标志处于关闭状态，则任何创建bean实例的操作都将重新查询bean类加载器中新解析的类。
	 */
	void setCacheBeanMetadata(boolean cacheBeanMetadata);

	/**
	 * Return whether to cache bean metadata such as given bean definitions
	 * (in merged fashion) and resolved bean classes.
	 * 返回是否缓存bean元数据，例如给定的bean定义（以合并的方式）和已解析的bean类。
	 */
	boolean isCacheBeanMetadata();

	/**
	 * Specify the resolution strategy for expressions in bean definition values.
	 * <p>There is no expression support active in a BeanFactory by default.
	 * An ApplicationContext will typically set a standard expression strategy
	 * here, supporting "#{...}" expressions in a Unified EL compatible style.
	 * 为bean定义值中的表达式指定解析策略
	 * 默认情况下，BeanFactory中没有活动的表达式支持。ApplicationContext通常会在此处设置标准表达式策略，支持统一EL兼容样式的"#{...}" 表达式
	 *
	 * @since 3.0
	 */
	void setBeanExpressionResolver(@Nullable BeanExpressionResolver resolver);

	/**
	 * Return the resolution strategy for expressions in bean definition values.
	 * 返回bean定义值中表达式的解析策略
	 *
	 * @since 3.0
	 */
	@Nullable
	BeanExpressionResolver getBeanExpressionResolver();

	/**
	 * Specify a Spring 3.0 ConversionService to use for converting
	 * property values, as an alternative to JavaBeans PropertyEditors.
	 * 指定用于转换属性值的Spring 3.0 ConversionService，作为JavaBeans PropertyEditors的替代方案。
	 *
	 * @since 3.0
	 */
	void setConversionService(@Nullable ConversionService conversionService);

	/**
	 * Return the associated ConversionService, if any.
	 * 返回关联的ConversionService（如果有）
	 *
	 * @since 3.0
	 */
	@Nullable
	ConversionService getConversionService();

	/**
	 * Add a PropertyEditorRegistrar to be applied to all bean creation processes.
	 * <p>Such a registrar creates new PropertyEditor instances and registers them
	 * on the given registry, fresh for each bean creation attempt. This avoids
	 * the need for synchronization on custom editors; hence, it is generally
	 * preferable to use this method instead of {@link #registerCustomEditor}.
	 * 添加一个PropertyEditorRegister以应用于所有bean创建过程
	 * 这样的注册器创建新的PropertyEditor实例，并将它们注册到给定的注册表中，对于每次bean创建尝试都是新鲜的。
	 * 这避免了在自定义编辑器上进行同步的需要；因此，通常最好使用此方法，而不是 {@link #registerCustomEditor}
	 *
	 * @param registrar the PropertyEditorRegistrar to register
	 */
	void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar);

	/**
	 * Register the given custom property editor for all properties of the
	 * given type. To be invoked during factory configuration.
	 * <p>Note that this method will register a shared custom editor instance;
	 * access to that instance will be synchronized for thread-safety. It is
	 * generally preferable to use {@link #addPropertyEditorRegistrar} instead
	 * of this method, to avoid for the need for synchronization on custom editors.
	 * 为给定类型的所有属性注册给定的自定义属性编辑器。在工厂配置期间调用
	 * 请注意，此方法将注册一个共享的自定义编辑器实例；为了线程安全，将同步访问该实例。通常最好使用{@link #addPropertyEditorRegistrar} 而不是此方法，
	 * 以避免在自定义编辑器上进行同步。
	 *
	 * @param requiredType        type of the property
	 * @param propertyEditorClass the {@link PropertyEditor} class to register
	 */
	void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass);

	/**
	 * Initialize the given PropertyEditorRegistry with the custom editors
	 * that have been registered with this BeanFactory.
	 * 使用已在此BeanFactory中注册的自定义编辑器初始化给定的PropertyEditorRegistry。
	 *
	 * @param registry the PropertyEditorRegistry to initialize
	 */
	void copyRegisteredEditorsTo(PropertyEditorRegistry registry);

	/**
	 * Set a custom type converter that this BeanFactory should use for converting
	 * bean property values, constructor argument values, etc.
	 * <p>This will override the default PropertyEditor mechanism and hence make
	 * any custom editors or custom editor registrars irrelevant.
	 * 设置一个自定义类型转换器，此BeanFactory应用于转换bean属性值、构造函数参数值等
	 * 这将覆盖默认的PropertyEditor机制，从而使任何自定义编辑器或自定义编辑器注册器都不相关。
	 *
	 * @see #addPropertyEditorRegistrar
	 * @see #registerCustomEditor
	 * @since 2.5
	 */
	void setTypeConverter(TypeConverter typeConverter);

	/**
	 * Obtain a type converter as used by this BeanFactory. This may be a fresh
	 * instance for each call, since TypeConverters are usually <i>not</i> thread-safe.
	 * <p>If the default PropertyEditor mechanism is active, the returned
	 * TypeConverter will be aware of all custom editors that have been registered.
	 * 获取此BeanFactory使用的类型转换器。这可能是每个调用的一个新实例，因为TypeConverter通常＜i＞不是＜i＞线程安全的
	 * 如果默认的PropertyEditor机制处于活动状态，则返回的TypeConverter将知道已注册的所有自定义编辑器。
	 *
	 * @since 2.5
	 */
	TypeConverter getTypeConverter();

	/**
	 * Add a String resolver for embedded values such as annotation attributes.
	 * 为嵌入值（如注释属性）添加字符串解析程序
	 *
	 * @param valueResolver the String resolver to apply to embedded values
	 * @since 3.0
	 */
	void addEmbeddedValueResolver(StringValueResolver valueResolver);

	/**
	 * Determine whether an embedded value resolver has been registered with this
	 * bean factory, to be applied through {@link #resolveEmbeddedValue(String)}.
	 * 确定嵌入值解析器是否已向该bean工厂注册，将通过{@link #resolveEmbeddedValue(String)}应用。
	 *
	 * @since 4.3
	 */
	boolean hasEmbeddedValueResolver();

	/**
	 * Resolve the given embedded value, e.g. an annotation attribute.
	 * 解析给定的嵌入值，例如注释属性
	 *
	 * @param value the value to resolve
	 * @return the resolved value (may be the original value as-is)
	 * @since 3.0
	 */
	@Nullable
	String resolveEmbeddedValue(String value);

	/**
	 * Add a new BeanPostProcessor that will get applied to beans created
	 * by this factory. To be invoked during factory configuration.
	 * <p>Note: Post-processors submitted here will be applied in the order of
	 * registration; any ordering semantics expressed through implementing the
	 * {@link org.springframework.core.Ordered} interface will be ignored. Note
	 * that autodetected post-processors (e.g. as beans in an ApplicationContext)
	 * will always be applied after programmatically registered ones.
	 * 添加一个新的BeanPostProcessor，该处理器将应用于此工厂创建的bean。在工厂配置期间调用
	 * 注：此处提交的后处理者将按注册顺序申请；通过实现{@link org.springframework.core.Ordered} 接口表达的任何排序语义都将被忽略。
	 * 请注意，自动检测的后处理器（例如ApplicationContext中的bean）将始终在编程注册后应用。
	 *
	 * @param beanPostProcessor the post-processor to register
	 */
	void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

	/**
	 * Return the current number of registered BeanPostProcessors, if any.
	 * 返回当前注册的BeanPostProcessors数量（如果有）。
	 */
	int getBeanPostProcessorCount();

	/**
	 * Register the given scope, backed by the given Scope implementation.
	 * 注册给定范围，并由给定范围实现支持。
	 *
	 * @param scopeName the scope identifier
	 * @param scope     the backing Scope implementation
	 */
	void registerScope(String scopeName, Scope scope);

	/**
	 * Return the names of all currently registered scopes.
	 * <p>This will only return the names of explicitly registered scopes.
	 * Built-in scopes such as "singleton" and "prototype" won't be exposed.
	 * 返回当前注册的所有作用域的名称
	 * 这将只返回显式注册的作用域的名称。诸如“singleton”和“prototype”之类的内置作用域不会被公开。
	 *
	 * @return the array of scope names, or an empty array if none
	 * @see #registerScope
	 */
	String[] getRegisteredScopeNames();

	/**
	 * Return the Scope implementation for the given scope name, if any.
	 * <p>This will only return explicitly registered scopes.
	 * Built-in scopes such as "singleton" and "prototype" won't be exposed.
	 * 返回给定作用域名称的作用域实现（如果有的话）
	 * 这将只返回显式注册的作用域。诸如“singleton”和“prototype”之类的内置作用域不会被公开
	 *
	 * @param scopeName the name of the scope
	 * @return the registered Scope implementation, or {@code null} if none
	 * @see #registerScope
	 */
	@Nullable
	Scope getRegisteredScope(String scopeName);

	/**
	 * Set the {@code ApplicationStartup} for this bean factory.
	 * <p>This allows the application context to record metrics during application startup.
	 * 为这个bean工厂设置 {@code ApplicationStartup}
	 * 这允许应用程序上下文在应用程序启动期间记录度量。
	 *
	 * @param applicationStartup the new application startup
	 * @since 5.3
	 */
	void setApplicationStartup(ApplicationStartup applicationStartup);

	/**
	 * Return the {@code ApplicationStartup} for this bean factory.
	 * 返回此bean工厂的 {@code ApplicationStartup}
	 *
	 * @since 5.3
	 */
	ApplicationStartup getApplicationStartup();

	/**
	 * Provides a security access control context relevant to this factory.
	 * 提供与此工厂相关的安全访问控制上下文
	 *
	 * @return the applicable AccessControlContext (never {@code null})
	 * @since 3.0
	 */
	AccessControlContext getAccessControlContext();

	/**
	 * Copy all relevant configuration from the given other factory.
	 * <p>Should include all standard configuration settings as well as
	 * BeanPostProcessors, Scopes, and factory-specific internal settings.
	 * Should not include any metadata of actual bean definitions,
	 * such as BeanDefinition objects and bean name aliases.
	 * 从给定的其他工厂复制所有相关配置
	 * 应包括所有标准配置设置以及BeanPostProcessors、Scopes和工厂特定的内部设置。不应包括任何实际bean定义的元数据
	 * 例如BeanDefinition对象和bean名称别名
	 *
	 * @param otherFactory the other BeanFactory to copy from
	 */
	void copyConfigurationFrom(ConfigurableBeanFactory otherFactory);

	/**
	 * Given a bean name, create an alias. We typically use this method to
	 * support names that are illegal within XML ids (used for bean names).
	 * <p>Typically invoked during factory configuration, but can also be
	 * used for runtime registration of aliases. Therefore, a factory
	 * implementation should synchronize alias access.
	 * 给定一个bean名称，创建一个别名。我们通常使用此方法来支持XML ID中非法的名称（用于bean名称）
	 * 通常在工厂配置期间调用，但也可用于别名的运行时注册。因此，工厂实现应该同步别名访问
	 *
	 * @param beanName the canonical name of the target bean
	 * @param alias    the alias to be registered for the bean
	 * @throws BeanDefinitionStoreException if the alias is already in use
	 */
	void registerAlias(String beanName, String alias) throws BeanDefinitionStoreException;

	/**
	 * Resolve all alias target names and aliases registered in this
	 * factory, applying the given StringValueResolver to them.
	 * <p>The value resolver may for example resolve placeholders
	 * in target bean names and even in alias names.
	 * 解析此工厂中注册的所有别名目标名称和别名，并对其应用给定的StringValueResolver
	 * 例如，值解析器可以解析目标bean名称甚至别名中的占位符
	 *
	 * @param valueResolver the StringValueResolver to apply
	 * @since 2.5
	 */
	void resolveAliases(StringValueResolver valueResolver);

	/**
	 * Return a merged BeanDefinition for the given bean name,
	 * merging a child bean definition with its parent if necessary.
	 * Considers bean definitions in ancestor factories as well.
	 * 为给定的bean名称返回一个合并的BeanDefinition，必要时将子bean定义与其父bean定义合并。还考虑了祖先工厂中的bean定义
	 *
	 * @param beanName the name of the bean to retrieve the merged definition for
	 * @return a (potentially merged) BeanDefinition for the given bean
	 * @throws NoSuchBeanDefinitionException if there is no bean definition with the given name
	 * @since 2.5
	 */
	BeanDefinition getMergedBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * Determine whether the bean with the given name is a FactoryBean.
	 * 确定具有给定名称的bean是否是FactoryBean
	 *
	 * @param name the name of the bean to check
	 * @return whether the bean is a FactoryBean
	 * ({@code false} means the bean exists but is not a FactoryBean)
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @since 2.5
	 */
	boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException;

	/**
	 * Explicitly control the current in-creation status of the specified bean.
	 * For container-internal use only.
	 * 显式控制指定bean的当前创建状态。仅供容器内部使用。
	 *
	 * @param beanName   the name of the bean
	 * @param inCreation whether the bean is currently in creation
	 * @since 3.1
	 */
	void setCurrentlyInCreation(String beanName, boolean inCreation);

	/**
	 * Determine whether the specified bean is currently in creation.
	 * 确定指定的bean当前是否正在创建中
	 *
	 * @param beanName the name of the bean
	 * @return whether the bean is currently in creation
	 * @since 2.5
	 */
	boolean isCurrentlyInCreation(String beanName);

	/**
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 * 为给定的bean注册一个依赖bean，在给定bean被销毁之前进行销毁。
	 *
	 * @param beanName          the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 * @since 2.5
	 */
	void registerDependentBean(String beanName, String dependentBeanName);

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 * 返回依赖于指定bean的所有bean的名称（如果有的话）
	 *
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 * @since 2.5
	 */
	String[] getDependentBeans(String beanName);

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 * 返回指定bean所依赖的所有bean的名称（如果有的话）
	 *
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 * @since 2.5
	 */
	String[] getDependenciesForBean(String beanName);

	/**
	 * Destroy the given bean instance (usually a prototype instance
	 * obtained from this factory) according to its bean definition.
	 * <p>Any exception that arises during destruction should be caught
	 * and logged instead of propagated to the caller of this method.
	 * 根据给定的bean实例（通常是从该工厂获得的原型实例）的bean定义销毁该实例
	 * 销毁过程中出现的任何异常都应该被捕获并记录，而不是传播到此方法的调用方。
	 *
	 * @param beanName     the name of the bean definition
	 * @param beanInstance the bean instance to destroy
	 */
	void destroyBean(String beanName, Object beanInstance);

	/**
	 * Destroy the specified scoped bean in the current target scope, if any.
	 * <p>Any exception that arises during destruction should be caught
	 * and logged instead of propagated to the caller of this method.
	 * 销毁当前目标作用域中指定的作用域bean（如果有的话）
	 * 销毁过程中出现的任何异常都应该被捕获并记录，而不是传播到此方法的调用方
	 *
	 * @param beanName the name of the scoped bean
	 */
	void destroyScopedBean(String beanName);

	/**
	 * Destroy all singleton beans in this factory, including inner beans that have
	 * been registered as disposable. To be called on shutdown of a factory.
	 * <p>Any exception that arises during destruction should be caught
	 * and logged instead of propagated to the caller of this method.
	 * 销毁此工厂中的所有单例bean，包括已注册为一次性的内部bean。被要求关闭工厂
	 * 销毁过程中出现的任何异常都应该被捕获并记录，而不是传播到此方法的调用方
	 */
	void destroySingletons();

}
