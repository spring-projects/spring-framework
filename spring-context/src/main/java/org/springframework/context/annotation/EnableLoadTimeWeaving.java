/*
 * Copyright 2002-2013 the original author or authors.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.weaving.DefaultContextLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;

/**
 * Activates a Spring {@link LoadTimeWeaver} for this application context, available as
 * a bean with the name "loadTimeWeaver", similar to the {@code <context:load-time-weaver>}
 * element in Spring XML.
 * To be used
 * on @{@link org.springframework.context.annotation.Configuration Configuration} classes;
 * the simplest possible example of which follows:
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableLoadTimeWeaving
 * public class AppConfig {
 *     // application-specific &#064;Bean definitions ...
 * }</pre>
 *
 * The example above is equivalent to the following Spring XML configuration:
 * <pre class="code">
 * {@code
 * <beans>
 *     <context:load-time-weaver/>
 *     <!-- application-specific <bean> definitions -->
 * </beans>
 * }</pre>
 *
 * <h2>The {@code LoadTimeWeaverAware} interface</h2>
 * Any bean that implements the {@link
 * org.springframework.context.weaving.LoadTimeWeaverAware LoadTimeWeaverAware} interface
 * will then receive the {@code LoadTimeWeaver} reference automatically; for example,
 * Spring's JPA bootstrap support.
 *
 * <h2>Customizing the {@code LoadTimeWeaver}</h2>
 * The default weaver is determined automatically: see {@link DefaultContextLoadTimeWeaver}.
 *
 * <p>To customize the weaver used, the {@code @Configuration} class annotated with
 * {@code @EnableLoadTimeWeaving} may also implement the {@link LoadTimeWeavingConfigurer}
 * interface and return a custom {@code LoadTimeWeaver} instance through the
 * {@code #getLoadTimeWeaver} method:
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableLoadTimeWeaving
 * public class AppConfig implements LoadTimeWeavingConfigurer {
 *     &#064;Override
 *     public LoadTimeWeaver getLoadTimeWeaver() {
 *         MyLoadTimeWeaver ltw = new MyLoadTimeWeaver();
 *         ltw.addClassTransformer(myClassFileTransformer);
 *         // ...
 *         return ltw;
 *     }
 * }</pre>
 *
 * <p>The example above can be compared to the following Spring XML configuration:
 * <pre class="code">
 * {@code
 * <beans>
 *     <context:load-time-weaver weaverClass="com.acme.MyLoadTimeWeaver"/>
 * </beans>
 * }</pre>
 *
 * <p>The code example differs from the XML example in that it actually instantiates the
 * {@code MyLoadTimeWeaver} type, meaning that it can also configure the instance, e.g.
 * calling the {@code #addClassTransformer} method. This demonstrates how the code-based
 * configuration approach is more flexible through direct programmatic access.
 *
 * <h2>Enabling AspectJ-based weaving</h2>
 * AspectJ load-time weaving may be enabled with the {@link #aspectjWeaving()}
 * attribute, which will cause the {@linkplain
 * org.aspectj.weaver.loadtime.ClassPreProcessorAgentAdapter AspectJ class transformer} to
 * be registered through {@link LoadTimeWeaver#addTransformer}. AspectJ weaving will be
 * activated by default if a "META-INF/aop.xml" resource is present on the classpath.
 * Example:
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableLoadTimeWeaving(aspectjWeaving=ENABLED)
 * public class AppConfig {
 * }</pre>
 *
 * <p>The example above can be compared to the following Spring XML configuration:
 * <pre class="code">
 * {@code
 * <beans>
 *     <context:load-time-weaver aspectj-weaving="on"/>
 * </beans>
 * }</pre>
 *
 * <p>The two examples are equivalent with one significant exception: in the XML case,
 * the functionality of {@code <context:spring-configured>} is implicitly enabled when
 * {@code aspectj-weaving} is "on".  This does not occur when using
 * {@code @EnableLoadTimeWeaving(aspectjWeaving=ENABLED)}. Instead you must explicitly add
 * {@code @EnableSpringConfigured} (included in the {@code spring-aspects} module)
 *
 * @author Chris Beams
 * @since 3.1
 * @see LoadTimeWeaver
 * @see DefaultContextLoadTimeWeaver
 * @see org.aspectj.weaver.loadtime.ClassPreProcessorAgentAdapter
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(LoadTimeWeavingConfiguration.class)
public @interface EnableLoadTimeWeaving {

	/**
	 * Whether AspectJ weaving should be enabled.
	 */
	AspectJWeaving aspectjWeaving() default AspectJWeaving.AUTODETECT;

	public enum AspectJWeaving {

		/**
		 * Switches on Spring-based AspectJ load-time weaving.
		 */
		ENABLED,

		/**
		 * Switches off Spring-based AspectJ load-time weaving (even if a
		 * "META-INF/aop.xml" resource is present on the classpath).
		 */
		DISABLED,

		/**
		 * Switches on AspectJ load-time weaving if a "META-INF/aop.xml" resource
		 * is present in the classpath. If there is no such resource, then AspectJ
		 * load-time weaving will be switched off.
		 */
		AUTODETECT;
	}
}
