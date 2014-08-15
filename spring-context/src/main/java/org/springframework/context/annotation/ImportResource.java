/*
 * Copyright 2002-2014 the original author or authors.
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

/**
 * Indicates one or more resources containing bean definitions to import.
 *
 * <p>Like {@link Import @Import}, this annotation provides functionality similar to
 * the {@code <import/>} element in Spring XML. It is typically used when designing
 * {@link Configuration @Configuration} classes to be bootstrapped by
 * {@link AnnotationConfigApplicationContext}, but where some XML functionality such
 * as namespaces is still necessary.
 *
 * <p>By default, arguments to the {@link #value()} attribute will be processed using
 * {@link org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader} if ending in
 * ".groovy"; otherwise, {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}
 * will be used to parse Spring {@code <beans/>} XML files. Optionally, the {@link #reader()}
 * attribute may be supplied, allowing the user to choose a custom {@link BeanDefinitionReader}
 * implementation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Configuration
 * @see Import
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ImportResource {

	/**
	 * Resource paths to import. Resource-loading prefixes such as {@code classpath:}
	 * and {@code file:}, etc may be used.
	 * <p>Out of the box, ".groovy" files are going to be specifically parsed with
	 * {@link org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader};
	 * others with {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}.
	 */
	String[] value();

	/**
	 * {@link BeanDefinitionReader} implementation to use when processing resources
	 * specified by the {@link #value()} attribute.
	 * <p>By default, the reader will be adapted to the resource path specified:
	 * ".groovy" files are going to be specifically parsed with
	 * {@link org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader};
	 * others with {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}.
	 */
	Class<? extends BeanDefinitionReader> reader() default BeanDefinitionReader.class;

}
