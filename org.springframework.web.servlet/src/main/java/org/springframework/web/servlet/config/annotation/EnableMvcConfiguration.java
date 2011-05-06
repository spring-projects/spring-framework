/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.web.servlet.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Enables default Spring MVC configuration and registers Spring MVC infrastructure components expected by the
 * {@link DispatcherServlet}. To use this annotation simply place it on an application @{@link Configuration} class
 * and that will in turn import default Spring MVC configuration including support for annotated methods
 * in @{@link Controller} classes.
 * <pre>
 * &#064;Configuration
 * &#064;EnableMvcConfiguration
 * &#064;ComponentScan(
 *	basePackageClasses = { MyMvcConfiguration.class },
 * 	excludeFilters = { @Filter(type = FilterType.ANNOTATION, value = Configuration.class) }
 * )
 * public class MyMvcConfiguration {
 *
 * }
 * </pre>
 * <p>To customize the imported configuration you simply implement {@link MvcConfigurer}, or more likely extend
 * {@link MvcConfigurerSupport} overriding selected methods only. The most obvious place to do this is
 * the @{@link Configuration} class that enabled the Spring MVC configuration via @{@link EnableMvcConfiguration}.
 * However any @{@link Configuration} class and more generally any Spring bean can implement {@link MvcConfigurer}
 * to be detected and given an opportunity to customize Spring MVC configuration at startup.
 * <pre>
 * &#064;Configuration
 * &#064;EnableMvcConfiguration
 * &#064;ComponentScan(
 * 	basePackageClasses = { MyMvcConfiguration.class },
 * 	excludeFilters = { @Filter(type = FilterType.ANNOTATION, value = Configuration.class) }
 * )
 * public class MyMvcConfiguration extends MvcConfigurerSupport {
 *
 * 	&#064;Override
 * 	public void registerFormatters(FormatterRegistry formatterRegistry) {
 * 		formatterRegistry.addConverter(new MyConverter());
 * 	}
 *
 * 	&#064;Override
 * 	public void configureMessageConverters(List&lt;HttpMessageConverter&lt;?&gt;&gt; converters) {
 * 		converters.add(new MyHttpMessageConverter());
 * 	}
 *
 * 	...
 *
 * }
 * </pre>
 *
 * @see MvcConfigurer
 * @see MvcConfigurerSupport
 *
 * @author Dave Syer
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(MvcConfiguration.class)
public @interface EnableMvcConfiguration {
}
