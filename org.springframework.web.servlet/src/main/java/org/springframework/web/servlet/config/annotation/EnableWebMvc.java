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
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Enables default Spring MVC configuration and registers Spring MVC infrastructure components expected by the
 * {@link DispatcherServlet}. Add this annotation to an application @{@link Configuration} class. It will in
 * turn import the @{@link Configuration} class {@link DelegatingWebMvcConfiguration}, which provides default Spring MVC
 * configuration.
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebMvc
 * &#064;ComponentScan(
 * 	basePackageClasses = { MyConfiguration.class },
 * 	excludeFilters = { @Filter(type = FilterType.ANNOTATION, value = Configuration.class) }
 * )
 * public class MyConfiguration {
 *
 * }
 * </pre>
 * <p>To customize the imported configuration implement {@link WebMvcConfigurer}, or more conveniently extend
 * {@link WebMvcConfigurerAdapter} overriding specific methods. Your @{@link Configuration} class and any other
 * Spring bean that implements {@link WebMvcConfigurer} will be detected and given an opportunity to customize 
 * the default Spring MVC configuration through the callback methods on the {@link WebMvcConfigurer} interface.
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebMvc
 * &#064;ComponentScan(
 * 	basePackageClasses = { MyConfiguration.class },
 * 	excludeFilters = { @Filter(type = FilterType.ANNOTATION, value = Configuration.class) }
 * )
 * public class MyConfiguration extends WebMvcConfigurerAdapter {
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
 * @see WebMvcConfigurer
 * @see WebMvcConfigurerAdapter
 *
 * @author Dave Syer
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebMvcConfiguration.class)
public @interface EnableWebMvc {
}
