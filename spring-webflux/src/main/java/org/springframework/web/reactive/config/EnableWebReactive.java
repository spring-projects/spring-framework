/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Adding this annotation to an {@code @Configuration} class imports the Spring Web
 * Reactive configuration from {@link WebReactiveConfigurationSupport}, e.g.:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebReactive
 * &#064;ComponentScan(basePackageClasses = MyConfiguration.class)
 * public class MyConfiguration {
 *
 * }
 * </pre>
 *
 * <p>To customize the imported configuration implement
 * {@link WebReactiveConfigurer} and override individual methods as shown below:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebReactive
 * &#064;ComponentScan(basePackageClasses = MyConfiguration.class)
 * public class MyConfiguration implements WebReactiveConfigurer {
 *
 * 	   &#064;Override
 * 	   public void addFormatters(FormatterRegistry formatterRegistry) {
 *         formatterRegistry.addConverter(new MyConverter());
 * 	   }
 *
 * 	   &#064;Override
 * 	   public void configureMessageWriters(List&lt;HttpMessageWriter&lt;?&gt&gt messageWriters) {
 *         messageWriters.add(new MyHttpMessageWriter());
 * 	   }
 *
 * }
 * </pre>
 *
 * <p><strong>Note:</strong> only one {@code @Configuration} class may have the
 * {@code @EnableWebReactive} annotation to import the Spring Web Reactive
 * configuration. There can however be multiple {@code @Configuration} classes
 * implementing {@code WebReactiveConfigurer} in order to customize the provided
 * configuration.
 *
 * <p>If {@link WebReactiveConfigurer} does not expose some more advanced setting
 * that needs to be configured consider removing the {@code @EnableWebReactive}
 * annotation and extending directly from {@link WebReactiveConfigurationSupport}
 * or {@link DelegatingWebReactiveConfiguration} if you still want to allow
 * {@link WebReactiveConfigurer} instances to customize the configuration.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebReactiveConfigurer
 * @see WebReactiveConfigurationSupport
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebReactiveConfiguration.class)
public @interface EnableWebReactive {
}
