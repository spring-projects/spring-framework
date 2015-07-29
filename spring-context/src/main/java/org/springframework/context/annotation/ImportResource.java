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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.core.annotation.AliasFor;

/**
 * Indicates one or more resources containing bean definitions to import.
 *
 * <p>Like {@link Import @Import}, this annotation provides functionality similar to
 * the {@code <import/>} element in Spring XML. It is typically used when designing
 * {@link Configuration @Configuration} classes to be bootstrapped by an
 * {@link AnnotationConfigApplicationContext}, but where some XML functionality such
 * as namespaces is still necessary.
 *
 * <p>By default, arguments to the {@link #value} attribute will be processed using a
 * {@link org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader GroovyBeanDefinitionReader}
 * if ending in {@code ".groovy"}; otherwise, an
 * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader XmlBeanDefinitionReader}
 * will be used to parse Spring {@code <beans/>} XML files. Optionally, the {@link #reader}
 * attribute may be declared, allowing the user to choose a custom {@link BeanDefinitionReader}
 * implementation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see Configuration
 * @see Import
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ImportResource {

	/**
	 * Alias for {@link #locations}.
	 * @see #locations
	 * @see #reader
	 */
	@AliasFor("locations")
	String[] value() default {};

	/**
	 * Resource locations from which to import.
	 * <p>Supports resource-loading prefixes such as {@code classpath:},
	 * {@code file:}, etc.
	 * <p>Consult the Javadoc for {@link #reader} for details on how resources
	 * will be processed.
	 * @since 4.2
	 * @see #value
	 * @see #reader
	 */
	@AliasFor("value")
	String[] locations() default {};

	/**
	 * {@link BeanDefinitionReader} implementation to use when processing
	 * resources specified via the {@link #value} attribute.
	 * <p>By default, the reader will be adapted to the resource path specified:
	 * {@code ".groovy"} files will be processed with a
	 * {@link org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader GroovyBeanDefinitionReader};
	 * whereas, all other resources will be processed with an
	 * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader XmlBeanDefinitionReader}.
	 * @see #value
	 */
	Class<? extends BeanDefinitionReader> reader() default BeanDefinitionReader.class;

}
