/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.scheduling.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.context.config.AdviceMode;
import org.springframework.core.Ordered;

/**
 * Enables Spring's asynchronous method execution capability. To be used
 * on @{@link Configuration} classes as follows:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAsync
 * public class AppConfig {
 *     &#064;Bean
 *     public MyAsyncBean asyncBean() {
 *         return new MyAsyncBean();
 *     }
 * }</pre>
 *
 * <p>The various attributes of the annotation control how advice
 * is applied ({@link #mode()}), and if the mode is {@link AdviceMode#PROXY}
 * (the default), the other attributes control the behavior of the proxying.
 *
 * <p>Note that if the {@linkplain #mode} is set to {@link AdviceMode#ASPECTJ}
 * the {@code org.springframework.aspects} module must be present on the classpath.
 *
 * <p>By default, a {@link org.springframework.core.task.SimpleAsyncTaskExecutor
 * SimpleAsyncTaskExecutor} will be used to process async method invocations. To
 * customize this behavior, implement {@link AsyncConfigurer} and
 * provide your own {@link java.util.concurrent.Executor Executor} through the
 * {@link AsyncConfigurer#getExecutor() getExecutor()} method.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAsync
 * public class AppConfig implements AsyncConfigurer {
 *
 *     &#064;Bean
 *     public MyAsyncBean asyncBean() {
 *         return new MyAsyncBean();
 *     }
 *
 *     public Executor getExecutor() {
 *         ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 *         executor.setThreadNamePrefix("Custom-");
 *         executor.initialize();
 *         return executor;
 *     }
 * }</pre>
 *
 * @author Chris Beams
 * @since 3.1
 * @see Async
 * @see AsyncConfigurer
 * @see AsyncConfigurationSelector
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(AsyncConfigurationSelector.class)
@Documented
public @interface EnableAsync {

	/**
	 * Indicate the 'async' annotation type to be detected at either class
	 * or method level. By default, both the {@link Async} annotation and
	 * the EJB 3.1 <code>javax.ejb.Asynchronous</code> annotation will be
	 * detected. <p>This setter property exists so that developers can provide
	 * their own (non-Spring-specific) annotation type to indicate that a method
	 * (or all methods of a given class) should be invoked asynchronously.
	 */
	Class<? extends Annotation> annotation() default Annotation.class;

	/**
	 * Indicate whether class-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies. The default is {@code false}
	 *
	 * <p>Note: Class-based proxies require the async {@link #annotation()}
	 * to be defined on the concrete class. Annotations in interfaces will
	 * not work in that case (they will rather only work with interface-based proxies)!
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate how async advice should be applied.
	 * The default is {@link AdviceMode#PROXY}.
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * Indicate the order in which the
	 * {@link org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor}
	 * should be applied. Defaults to Order.LOWEST_PRIORITY in order to run
	 * after all other post-processors, so that it can add an advisor to
	 * existing proxies rather than double-proxy.
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;
}
