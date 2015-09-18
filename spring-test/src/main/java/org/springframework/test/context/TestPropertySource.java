/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.core.annotation.AliasFor;

/**
 * {@code @TestPropertySource} is a class-level annotation that is used to
 * configure the {@link #locations} of properties files and inlined
 * {@link #properties} to be added to the {@code Environment}'s set of
 * {@code PropertySources} for an
 * {@link org.springframework.context.ApplicationContext ApplicationContext}
 * for integration tests.
 *
 * <h3>Precedence</h3>
 * <p>Test property sources have higher precedence than those loaded from the
 * operating system's environment or Java system properties as well as property
 * sources added by the application declaratively via
 * {@link org.springframework.context.annotation.PropertySource @PropertySource}
 * or programmatically (e.g., via an
 * {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}
 * or some other means). Thus, test property sources can be used to selectively
 * override properties defined in system and application property sources.
 * Furthermore, inlined {@link #properties} have higher precedence than
 * properties loaded from resource {@link #locations}.
 *
 * <h3>Default Properties File Detection</h3>
 * <p>If {@code @TestPropertySource} is declared as an <em>empty</em> annotation
 * (i.e., without explicit values for {@link #locations} or {@link #properties}),
 * an attempt will be made to detect a <em>default</em> properties file relative
 * to the class that declared the annotation. For example, if the annotated test
 * class is {@code com.example.MyTest}, the corresponding default properties file
 * is {@code "classpath:com/example/MyTest.properties"}. If the default cannot be
 * detected, an {@link IllegalStateException} will be thrown.
 *
 * <h3>Enabling &#064;TestPropertySource</h3>
 * <p>{@code @TestPropertySource} is enabled if the configured
 * {@linkplain ContextConfiguration#loader context loader} honors it. Every
 * {@code SmartContextLoader} that is a subclass of either
 * {@link org.springframework.test.context.support.AbstractGenericContextLoader AbstractGenericContextLoader} or
 * {@link org.springframework.test.context.web.AbstractGenericWebContextLoader AbstractGenericWebContextLoader}
 * provides automatic support for {@code @TestPropertySource}, and this includes
 * every {@code SmartContextLoader} provided by the Spring TestContext Framework.
 *
 * <h3>Miscellaneous</h3>
 * <ul>
 * <li>Typically, {@code @TestPropertySource} will be used in conjunction with
 * {@link ContextConfiguration @ContextConfiguration}.</li>
 * <li>This annotation may be used as a <em>meta-annotation</em> to create
 * custom <em>composed annotations</em>; however, caution should be taken if
 * this annotation and {@code @ContextConfiguration} are combined on a composed
 * annotation since the {@code locations} and {@code inheritLocations} attributes
 * of both annotations can lead to ambiguity during the attribute resolution
 * process.</li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 4.1
 * @see ContextConfiguration
 * @see org.springframework.core.env.Environment
 * @see org.springframework.core.env.PropertySource
 * @see org.springframework.context.annotation.PropertySource
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TestPropertySource {

	/**
	 * Alias for {@link #locations}.
	 *
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #locations}, but it may be used <em>instead</em> of {@link #locations}.
	 *
	 * @see #locations
	 */
	@AliasFor("locations")
	String[] value() default {};

	/**
	 * The resource locations of properties files to be loaded into the
	 * {@code Environment}'s set of {@code PropertySources}. Each location
	 * will be added to the enclosing {@code Environment} as its own property
	 * source, in the order declared.
	 *
	 * <h3>Supported File Formats</h3>
	 * <p>Both traditional and XML-based properties file formats are supported
	 * &mdash; for example, {@code "classpath:/com/example/test.properties"}
	 * or {@code "file:/path/to/file.xml"}.
	 *
	 * <h3>Path Resource Semantics</h3>
	 * <p>Each path will be interpreted as a Spring
	 * {@link org.springframework.core.io.Resource Resource}. A plain path
	 * &mdash; for example, {@code "test.properties"} &mdash; will be treated as a
	 * classpath resource that is <em>relative</em> to the package in which the
	 * test class is defined. A path starting with a slash will be treated as an
	 * <em>absolute</em> classpath resource, for example:
	 * {@code "/org/example/test.xml"}. A path which references a
	 * URL (e.g., a path prefixed with
	 * {@link org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX classpath:},
	 * {@link org.springframework.util.ResourceUtils#FILE_URL_PREFIX file:},
	 * {@code http:}, etc.) will be loaded using the specified resource protocol.
	 * Resource location wildcards (e.g. <code>*&#42;/*.properties</code>)
	 * are not permitted: each location must evaluate to exactly one
	 * {@code .properties} or {@code .xml} resource. Property placeholders
	 * in paths (i.e., <code>${...}</code>) will be
	 * {@linkplain org.springframework.core.env.Environment#resolveRequiredPlaceholders(String) resolved}
	 * against the {@code Environment}.
	 *
	 * <h3>Default Properties File Detection</h3>
	 * <p>See the class-level Javadoc for a discussion on detection of defaults.
	 *
	 * <h3>Precedence</h3>
	 * <p>Properties loaded from resource locations have lower precedence than
	 * inlined {@link #properties}.
	 *
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #value}, but it may be used <em>instead</em> of {@link #value}.
	 *
	 * @see #inheritLocations
	 * @see #value
	 * @see #properties
	 * @see org.springframework.core.env.PropertySource
	 */
	@AliasFor("value")
	String[] locations() default {};

	/**
	 * Whether or not test property source {@link #locations} from superclasses
	 * should be <em>inherited</em>.
	 *
	 * <p>The default value is {@code true}, which means that a test class will
	 * <em>inherit</em> property source locations defined by a superclass.
	 * Specifically, the property source locations for a test class will be
	 * appended to the list of property source locations defined by a superclass.
	 * Thus, subclasses have the option of <em>extending</em> the list of test
	 * property source locations.
	 *
	 * <p>If {@code inheritLocations} is set to {@code false}, the property
	 * source locations for the test class will <em>shadow</em> and effectively
	 * replace any property source locations defined by a superclass.
	 *
	 * <p>In the following example, the {@code ApplicationContext} for
	 * {@code BaseTest} will be loaded using only the {@code "base.properties"}
	 * file as a test property source. In contrast, the {@code ApplicationContext}
	 * for {@code ExtendedTest} will be loaded using the {@code "base.properties"}
	 * <strong>and</strong> {@code "extended.properties"} files as test property
	 * source locations.
	 * <pre class="code">
	 * &#064;TestPropertySource(&quot;base.properties&quot;)
	 * &#064;ContextConfiguration
	 * public class BaseTest {
	 *     // ...
	 * }
	 *
	 * &#064;TestPropertySource(&quot;extended.properties&quot;)
	 * &#064;ContextConfiguration
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }
	 * </pre>
	 *
	 * @see #locations
	 */
	boolean inheritLocations() default true;

	/**
	 * <em>Inlined properties</em> in the form of <em>key-value</em> pairs that
	 * should be added to the Spring
	 * {@link org.springframework.core.env.Environment Environment} before the
	 * {@code ApplicationContext} is loaded for the test. All key-value pairs
	 * will be added to the enclosing {@code Environment} as a single test
	 * {@code PropertySource} with the highest precedence.
	 *
	 * <h3>Supported Syntax</h3>
	 * <p>The supported syntax for key-value pairs is the same as the
	 * syntax defined for entries in a Java
	 * {@linkplain java.util.Properties#load(java.io.Reader) properties file}:
	 * <ul>
	 * <li>{@code "key=value"}</li>
	 * <li>{@code "key:value"}</li>
	 * <li>{@code "key value"}</li>
	 * </ul>
	 *
	 * <h3>Precedence</h3>
	 * <p>Properties declared via this attribute have higher precedence than
	 * properties loaded from resource {@link #locations}.
	 *
	 * <p>This attribute may be used in conjunction with {@link #value}
	 * <em>or</em> {@link #locations}.
	 *
	 * @see #inheritProperties
	 * @see #locations
	 * @see org.springframework.core.env.PropertySource
	 */
	String[] properties() default {};

	/**
	 * Whether or not inlined test {@link #properties} from superclasses should
	 * be <em>inherited</em>.
	 *
	 * <p>The default value is {@code true}, which means that a test class will
	 * <em>inherit</em> inlined properties defined by a superclass. Specifically,
	 * the inlined properties for a test class will be appended to the list of
	 * inlined properties defined by a superclass. Thus, subclasses have the
	 * option of <em>extending</em> the list of inlined test properties.
	 *
	 * <p>If {@code inheritProperties} is set to {@code false}, the inlined
	 * properties for the test class will <em>shadow</em> and effectively
	 * replace any inlined properties defined by a superclass.
	 *
	 * <p>In the following example, the {@code ApplicationContext} for
	 * {@code BaseTest} will be loaded using only the inlined {@code key1}
	 * property. In contrast, the {@code ApplicationContext} for
	 * {@code ExtendedTest} will be loaded using the inlined {@code key1}
	 * <strong>and</strong> {@code key2} properties.
	 * <pre class="code">
	 * &#064;TestPropertySource(properties = &quot;key1 = value1&quot;)
	 * &#064;ContextConfiguration
	 * public class BaseTest {
	 *     // ...
	 * }
	 *
	 * &#064;TestPropertySource(properties = &quot;key2 = value2&quot;)
	 * &#064;ContextConfiguration
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }
	 * </pre>
	 *
	 * @see #properties
	 */
	boolean inheritProperties() default true;

}
