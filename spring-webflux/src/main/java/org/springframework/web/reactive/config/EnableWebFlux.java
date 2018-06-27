/*
 * Copyright 2002-2017 the original author or authors.
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
 * Adding this annotation to an {@code @Configuration} class imports the Spring
 * WebFlux configuration from {@link WebFluxConfigurationSupport} that enables
 * use of annotated controllers and functional endpoints.
 *
 * <p>For example:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebFlux
 * &#064;ComponentScan(basePackageClasses = MyConfiguration.class)
 * public class MyConfiguration {
 * }
 * </pre>
 *
 * <p>To customize the imported configuration, implement
 * {@link WebFluxConfigurer} and one or more of its methods:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebFlux
 * &#064;ComponentScan(basePackageClasses = MyConfiguration.class)
 * public class MyConfiguration implements WebFluxConfigurer {
 *
 * 	   &#064;Override
 * 	   public void configureMessageWriters(List&lt;HttpMessageWriter&lt;?&gt&gt messageWriters) {
 *         messageWriters.add(new MyHttpMessageWriter());
 * 	   }
 *
 * 	   // ...
 * }
 * </pre>
 *
 * <p>Only one {@code @Configuration} class should have the {@code @EnableWebFlux}
 * annotation in order to import the Spring WebFlux configuration. There can
 * however be multiple {@code @Configuration} classes that implement
 * {@code WebFluxConfigurer} that customize the provided configuration.
 *
 * <p>If {@code WebFluxConfigurer} does not expose some setting that needs to be
 * configured, consider switching to an advanced mode by removing the
 * {@code @EnableWebFlux} annotation and extending directly from
 * {@link WebFluxConfigurationSupport} or {@link DelegatingWebFluxConfiguration} --
 * the latter allows detecting and delegating to one or more
 * {@code WebFluxConfigurer} configuration classes.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebFluxConfigurer
 * @see WebFluxConfigurationSupport
 * @see DelegatingWebFluxConfiguration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebFluxConfiguration.class)
public @interface EnableWebFlux {
}
