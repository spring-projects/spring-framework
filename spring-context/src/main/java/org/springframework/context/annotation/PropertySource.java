/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.io.support.PropertySourceFactory;

/**
 * Annotation providing a convenient and declarative mechanism for adding a
 * {@link org.springframework.core.env.PropertySource PropertySource} to Spring's
 * {@link org.springframework.core.env.Environment Environment}. To be used in
 * conjunction with @{@link Configuration} classes.
 *
 * Annotation提供了一种方便的声明机制，用于向Spring的
 * {@link org.springframework.core.env.Environment Environment}
 * 添加{@link org.springframework.core.env.PropertySource PropertySource}。
 * 与@ {@link Configuration}类一起使用。
 *
 * <h3>Example usage</h3>
 *
 * <h3>用法示例</ h3>
 *
 * <p>Given a file {@code app.properties} containing the key/value pair
 * {@code testbean.name=myTestBean}, the following {@code @Configuration} class
 * uses {@code @PropertySource} to contribute {@code app.properties} to the
 * {@code Environment}'s set of {@code PropertySources}.
 *
 * 给定一个包含关键字/值对{@code testbean.name = myTestBean}的文件{@code app.properties}，
 * 以下{@code @Configuration}类使用{@code @PropertySource}来贡献{@code app.properties}添加到{@code Environment}的
 * {@Code PropertySources}集合。
 *
 * <pre class="code">
 * @Configuration
 * @PropertySource("classpath:/com/myco/app.properties")
 * public class AppConfig {
 *
 *     @Autowired
 *     Environment env;
 *
 *     @Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }</pre>
 *
 * Notice that the {@code Environment} object is
 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired} into the
 * configuration class and then used when populating the {@code TestBean} object. Given
 * the configuration above, a call to {@code testBean.getName()} will return "myTestBean".
 *
 * <h3>Resolving ${...} placeholders in {@code <bean>} and {@code @Value} annotations</h3>
 *
 * In order to resolve ${...} placeholders in {@code <bean>} definitions or {@code @Value}
 * annotations using properties from a {@code PropertySource}, one must register
 * a {@code PropertySourcesPlaceholderConfigurer}. This happens automatically when using
 * {@code <context:property-placeholder>} in XML, but must be explicitly registered using
 * a {@code static} {@code @Bean} method when using {@code @Configuration} classes. See
 * the "Working with externalized values" section of @{@link Configuration}'s javadoc and
 * "a note on BeanFactoryPostProcessor-returning @Bean methods" of @{@link Bean}'s javadoc
 * for details and examples.
 *
 * <h3>Resolving ${...} placeholders within {@code @PropertySource} resource locations</h3>
 *
 * Any ${...} placeholders present in a {@code @PropertySource} {@linkplain #value()
 * resource location} will be resolved against the set of property sources already
 * registered against the environment. For example:
 *
 * 注意，{@code Environment}对象是{@link org.springframework.beans.factory.annotation.Autowired @Autowired}
 * 到配置类中，然后在填充TestBean对象时使用。根据上面的配置，调用{@code testBean.getName（）}将返回“myTestBean”。
 * <h3>解决{@code <bean>}和{@code @Value}注释中的$ {...}占位符</ h3>
 * 为了使用来自PropertySource的属性来解析{@code <bean>}定义或{@code @Value}注释中的$ {...}占位符，
 * 必须注册一个PropertySourcesPlaceholderConfigurer。这在使用XML中的{@code <context：property-placeholder>}时会自动发生，
 * 但在使用@Configuration}类时必须使用静态{@code @Bean}方法显式注册。有关详细信息和示例，
 * 请参阅@ {@link Bean}的javadoc中的“{{@link Configuration}”的javadoc的“使用外部化值”部分和
 * “返回BeanFactoryPostProcessor返回@Bean方法的说明”。
 * <h3>解决{@code @PropertySource}资源位置中的$ {...}占位符</ h3> {@code @PropertySource}
 * {@linkplain #value（）资源位置中存在的任何$ {...}占位符}将针对已经针对环境注册的一组财产来源进行解决。例如：
 * <pre class="code">
 *
 * @Configuration
 * @PropertySource("classpath:/com/${my.placeholder:default/path}/app.properties")
 * public class AppConfig {
 *
 *     @Autowired
 *     Environment env;
 *
 *     @Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }</pre>
 *
 * Assuming that "my.placeholder" is present in one of the property sources already
 * registered, e.g. system properties or environment variables, the placeholder will
 * be resolved to the corresponding value. If not, then "default/path" will be used as a
 * default. Expressing a default value (delimited by colon ":") is optional.  If no
 * default is specified and a property cannot be resolved, an {@code
 * IllegalArgumentException} will be thrown.
 *
 * <h3>A note on property overriding with @PropertySource</h3>
 *
 * In cases where a given property key exists in more than one {@code .properties}
 * file, the last {@code @PropertySource} annotation processed will 'win' and override.
 *
 * For example, given two properties files {@code a.properties} and
 * {@code b.properties}, consider the following two configuration classes
 * that reference them with {@code @PropertySource} annotations:
 *
 * 假设“my.placeholder”存在于已经注册的其中一个财产来源中，
 * 例如系统属性或环境变量，占位符将被解析为相应的值。 如果不是，则默认使用“默认/路径”。
 * 表示默认值（用冒号“：”分隔）是可选的。 如果未指定默认值并且属性无法解析，
 * 则会抛出{Icode IllegalArgumentException}。
 * <h3>有关使用@PropertySource </ h3>进行属性覆盖的说明
 * 如果给定的属性键存在于多个{@code .properties}
 * 文件，处理的最后一个{@code @PropertySource}注释将'取胜'并覆盖。
 * 例如，给定两个属性文件{code a.properties}和
 * {@code b.properties}，请考虑以下两个配置类
 * 使用{@code @PropertySource}注释引用它们：
 *
 * <pre class="code">
 * @Configuration
 * @PropertySource("classpath:/com/myco/a.properties")
 * public class ConfigA { }
 *
 * @Configuration
 * @PropertySource("classpath:/com/myco/b.properties")
 * public class ConfigB { }
 * </pre>
 *
 * The override ordering depends on the order in which these classes are registered
 * with the application context.
 *
 * 覆盖顺序取决于这些类在应用程序上下文中注册的顺序。
 *
 * <pre class="code">
 * AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 * ctx.register(ConfigA.class);
 * ctx.register(ConfigB.class);
 * ctx.refresh();
 * </pre>
 *
 * In the scenario above, the properties in {@code b.properties} will override any
 * duplicates that exist in {@code a.properties}, because {@code ConfigB} was registered
 * last.
 *
 * <p>In certain situations, it may not be possible or practical to tightly control
 * property source ordering when using {@code @ProperySource} annotations. For example,
 * if the {@code @Configuration} classes above were registered via component-scanning,
 * the ordering is difficult to predict. In such cases - and if overriding is important -
 * it is recommended that the user fall back to using the programmatic PropertySource API.
 * See {@link org.springframework.core.env.ConfigurableEnvironment ConfigurableEnvironment}
 * and {@link org.springframework.core.env.MutablePropertySources MutablePropertySources}
 * javadocs for details.
 *
 * <p><b>NOTE: This annotation is repeatable according to Java 8 conventions.</b>
 * However, all such {@code @PropertySource} annotations need to be declared at the same
 * level: either directly on the configuration class or as meta-annotations within the
 * same custom annotation. Mixing of direct annotations and meta-annotations is not
 * recommended since direct annotations will effectively override meta-annotations.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.1
 * @see PropertySources
 * @see Configuration
 * @see org.springframework.core.env.PropertySource
 * @see org.springframework.core.env.ConfigurableEnvironment#getPropertySources()
 * @see org.springframework.core.env.MutablePropertySources
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(PropertySources.class)
public @interface PropertySource {

	/**
	 * Indicate the name of this property source. If omitted, a name will
	 * be generated based on the description of the underlying resource.
	 * 指明此属性来源的名称。 如果省略，将根据底层资源的描述生成名称。
	 *
	 * @see org.springframework.core.env.PropertySource#getName()
	 * @see org.springframework.core.io.Resource#getDescription()
	 */
	String name() default "";

	/**
	 * Indicate the resource location(s) of the properties file to be loaded.
	 * <p>Both traditional and XML-based properties file formats are supported
	 * &mdash; for example, {@code "classpath:/com/myco/app.properties"}
	 * or {@code "file:/path/to/file.xml"}.
	 * <p>Resource location wildcards (e.g. *&#42;/*.properties) are not permitted;
	 * each location must evaluate to exactly one {@code .properties} resource.
	 * <p>${...} placeholders will be resolved against any/all property sources already
	 * registered with the {@code Environment}. See {@linkplain PropertySource above}
	 * for examples.
	 * <p>Each location will be added to the enclosing {@code Environment} as its own
	 * property source, and in the order declared.
	 *
	 * 指示要加载的属性文件的资源位置。
	 * <p>传统和基于XML的属性文件格式均受支持 - ＆mdash;
	 * 例如，{code“classpath：/com/myco/app.properties”}或{@code“file：/path/to/file.xml”}。
	 * 资源位置通配符（例如** / *。properties）是不允许的; 每个位置都必须评估一个{
	 * {@code .properties}资源。</ p>
	 * $ {...}占位符将针对已在“环境”中注册的任何/所有属性源进行解析。
	 * 有关示例，请参见上面的{@linkplain PropertySource}。
	 * <p>每个位置都将作为其自己的属性来源并按声明的顺序添加到封闭的环境中。
	 */
	String[] value();

	/**
	 * Indicate if failure to find the a {@link #value() property resource} should be
	 * ignored.
	 * 指示是否应该忽略找不到{@link #value（）属性资源}的失败。
	 *
	 * <p>{@code true} is appropriate if the properties file is completely optional.
	 * Default is {@code false}.
	 * @since 4.0
	 */
	boolean ignoreResourceNotFound() default false;

	/**
	 * A specific character encoding for the given resources, e.g. "UTF-8".
	 * @since 4.3
	 */
	String encoding() default "";

	/**
	 * Specify a custom {@link PropertySourceFactory}, if any.
	 * <p>By default, a default factory for standard resource files will be used.
	 * @since 4.3
	 * @see org.springframework.core.io.support.DefaultPropertySourceFactory
	 * @see org.springframework.core.io.support.ResourcePropertySource
	 */
	Class<? extends PropertySourceFactory> factory() default PropertySourceFactory.class;

}
