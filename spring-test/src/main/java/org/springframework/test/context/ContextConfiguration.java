/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@code @ContextConfiguration} defines class-level metadata that is
 * used to determine how to load and configure an
 * {@link org.springframework.context.ApplicationContext ApplicationContext}
 * for integration tests.
 *
 * <h3>Supported Resource Types</h3>
 *
 * <p>Prior to Spring 3.1, only path-based resource locations were supported.
 * As of Spring 3.1, {@linkplain #loader context loaders} may choose to support
 * either path-based or class-based resources (but not both). Consequently
 * {@code @ContextConfiguration} can be used to declare either path-based
 * resource locations (via the {@link #locations} or {@link #value}
 * attribute) <i>or</i> annotated classes (via the {@link #classes}
 * attribute).
 *
 * <h3>Annotated Classes</h3>
 *
 * <p>The term <em>annotated class</em> can refer to any of the following.
 *
 * <ul>
 * <li>A class annotated with
 * {@link org.springframework.context.annotation.Configuration @Configuration}</li>
 * <li>A component (i.e., a class annotated with
 * {@link org.springframework.stereotype.Component @Component},
 * {@link org.springframework.stereotype.Service @Service},
 * {@link org.springframework.stereotype.Repository @Repository}, etc.)</li>
 * <li>A JSR-330 compliant class that is annotated with {@code javax.inject} annotations</li>
 * <li>Any other class that contains
 * {@link org.springframework.context.annotation.Bean @Bean}-methods</li>
 * </ul>
 *
 * Consult the Javadoc for
 * {@link org.springframework.context.annotation.Configuration @Configuration} and
 * {@link org.springframework.context.annotation.Bean @Bean}
 * for further information regarding the configuration and semantics of
 * <em>annotated classes</em>.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see ContextLoader
 * @see SmartContextLoader
 * @see ContextConfigurationAttributes
 * @see MergedContextConfiguration
 * @see ActiveProfiles
 * @see org.springframework.context.ApplicationContext
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ContextConfiguration {

	/**
	 * Alias for {@link #locations}.
	 *
	 * <p>This attribute may <strong>not</strong> be used in conjunction
	 * with {@link #locations} or {@link #classes}, but it may be used
	 * instead of {@link #locations}.
	 * @since 3.0
	 * @see #inheritLocations
	 */
	String[] value() default {};

	/**
	 * The resource locations to use for loading an
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
	 *
	 * <p>Check out the Javadoc for
	 * {@link org.springframework.test.context.support.AbstractContextLoader#modifyLocations
	 * AbstractContextLoader.modifyLocations()} for details on how a location
	 * String will be interpreted at runtime, in particular in case of a relative
	 * path. Also, check out the documentation on
	 * {@link org.springframework.test.context.support.AbstractContextLoader#generateDefaultLocations
	 * AbstractContextLoader.generateDefaultLocations()} for details on the default
	 * locations that are going to be used if none are specified.
	 *
	 * <p>Note that the above-mentioned default rules only apply for a standard
	 * {@link org.springframework.test.context.support.AbstractContextLoader
	 * AbstractContextLoader} subclass such as
	 * {@link org.springframework.test.context.support.GenericXmlContextLoader
	 * GenericXmlContextLoader} which is the effective default implementation
	 * used at runtime if <code>locations</code> are configured. See the
	 * documentation for {@link #loader} for further details regarding default
	 * loaders.
	 *
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #value} or {@link #classes}, but it may be used instead of
	 * {@link #value}.
	 * @since 2.5
	 * @see #inheritLocations
	 */
	String[] locations() default {};

	/**
	 * The <em>annotated classes</em> to use for loading an
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
	 *
	 * <p>Check out the Javadoc for
	 * {@link org.springframework.test.context.support.AnnotationConfigContextLoader#detectDefaultConfigurationClasses
	 * AnnotationConfigContextLoader.detectDefaultConfigurationClasses()} for details
	 * on how default configuration classes will be detected if no
	 * <em>annotated classes</em> are specified. See the documentation for
	 * {@link #loader} for further details regarding default loaders.
	 *
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #locations} or {@link #value}.
	 *
	 * @since 3.1
	 * @see org.springframework.context.annotation.Configuration
	 * @see org.springframework.test.context.support.AnnotationConfigContextLoader
	 * @see #inheritLocations
	 */
	Class<?>[] classes() default {};

	/**
	 * The application context <em>initializer classes</em> to use for initializing
	 * a {@link ConfigurableApplicationContext}.
	 * 
	 * <p>The concrete {@code ConfigurableApplicationContext} type supported by each
	 * declared initializer must be compatible with the type of {@code ApplicationContext}
	 * created by the {@link SmartContextLoader} in use.
	 *
	 * <p>{@code SmartContextLoader} implementations typically detect whether
	 * Spring's {@link org.springframework.core.Ordered Ordered} interface has been
	 * implemented or if the @{@link org.springframework.core.annotation.Order Order}
	 * annotation is present and sort instances accordingly prior to invoking them.
	 *
	 * @since 3.2
	 * @see org.springframework.context.ApplicationContextInitializer
	 * @see org.springframework.context.ConfigurableApplicationContext
	 * @see #inheritInitializers
	 * @see #loader
	 */
	Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[] initializers() default {};

	/**
	 * Whether or not {@link #locations resource locations} or <em>annotated
	 * classes</em> from test superclasses should be <em>inherited</em>.
	 *
	 * <p>The default value is <code>true</code>. This means that an annotated
	 * class will <em>inherit</em> the resource locations or annotated classes
	 * defined by test superclasses. Specifically, the resource locations or
	 * annotated classes for a given test class will be appended to the list of
	 * resource locations or annotated classes defined by test superclasses.
	 * Thus, subclasses have the option of <em>extending</em> the list of resource
	 * locations or annotated classes.
	 *
	 * <p>If <code>inheritLocations</code> is set to <code>false</code>, the
	 * resource locations or annotated classes for the annotated class
	 * will <em>shadow</em> and effectively replace any resource locations
	 * or annotated classes defined by superclasses.
	 *
	 * <p>In the following example that uses path-based resource locations, the
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}
	 * for {@code ExtendedTest} will be loaded from
	 * &quot;base-context.xml&quot; <strong>and</strong>
	 * &quot;extended-context.xml&quot;, in that order. Beans defined in
	 * &quot;extended-context.xml&quot; may therefore override those defined in
	 * &quot;base-context.xml&quot;.
	 * <pre class="code">
	 * &#064;ContextConfiguration(&quot;base-context.xml&quot;)
	 * public class BaseTest {
	 *     // ...
	 * }
	 *
	 * &#064;ContextConfiguration(&quot;extended-context.xml&quot;)
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }
	 * </pre>
	 *
	 * <p>Similarly, in the following example that uses annotated
	 * classes, the
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}
	 * for {@code ExtendedTest} will be loaded from the
	 * {@code BaseConfig} <strong>and</strong> {@code ExtendedConfig}
	 * configuration classes, in that order. Beans defined in
	 * {@code ExtendedConfig} may therefore override those defined in
	 * {@code BaseConfig}.
	 * <pre class="code">
	 * &#064;ContextConfiguration(classes=BaseConfig.class)
	 * public class BaseTest {
	 *     // ...
	 * }
	 *
	 * &#064;ContextConfiguration(classes=ExtendedConfig.class)
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }
	 * </pre>
	 * @since 2.5
	 */
	boolean inheritLocations() default true;

	/**
	 * Whether or not {@linkplain #initializers context initializers} from test
	 * superclasses should be <em>inherited</em>.
	 *
	 * <p>The default value is <code>true</code>. This means that an annotated
	 * class will <em>inherit</em> the application context initializers defined
	 * by test superclasses. Specifically, the initializers for a given test
	 * class will be added to the set of initializers defined by test
	 * superclasses. Thus, subclasses have the option of <em>extending</em> the
	 * set of initializers.
	 *
	 * <p>If <code>inheritInitializers</code> is set to <code>false</code>, the
	 * initializers for the annotated class will <em>shadow</em> and effectively
	 * replace any initializers defined by superclasses.
	 *
	 * <p>In the following example, the
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}
	 * for {@code ExtendedTest} will be initialized using
	 * {@code BaseInitializer} <strong>and</strong> {@code ExtendedInitializer}.
	 * Note, however, that the order in which the initializers are invoked
	 * depends on whether they implement {@link org.springframework.core.Ordered
	 * Ordered} or are annotated with {@link org.springframework.core.annotation.Order
	 * &#064;Order}.
	 * <pre class="code">
	 * &#064;ContextConfiguration(initializers = BaseInitializer.class)
	 * public class BaseTest {
	 *     // ...
	 * }
	 * 
	 * &#064;ContextConfiguration(initializers = ExtendedInitializer.class)
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }
	 * </pre>
	 * @since 3.2
	 */
	boolean inheritInitializers() default true;

	/**
	 * The type of {@link SmartContextLoader} (or {@link ContextLoader}) to use
	 * for loading an {@link org.springframework.context.ApplicationContext
	 * ApplicationContext}.
	 *
	 * <p>If not specified, the loader will be inherited from the first superclass
	 * that is annotated with {@code @ContextConfiguration} and specifies an
	 * explicit loader. If no class in the hierarchy specifies an explicit
	 * loader, a default loader will be used instead.
	 *
	 * <p>The default concrete implementation chosen at runtime will be
	 * {@link org.springframework.test.context.support.DelegatingSmartContextLoader
	 * DelegatingSmartContextLoader}. For further details on the default behavior
	 * of various concrete {@code ContextLoaders}, check out the Javadoc for
	 * {@link org.springframework.test.context.support.AbstractContextLoader
	 * AbstractContextLoader},
	 * {@link org.springframework.test.context.support.GenericXmlContextLoader
	 * GenericXmlContextLoader}, and
	 * {@link org.springframework.test.context.support.AnnotationConfigContextLoader
	 * AnnotationConfigContextLoader}.
	 * @since 2.5
	 */
	Class<? extends ContextLoader> loader() default ContextLoader.class;

}
