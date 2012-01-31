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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as being eligible for Spring-driven configuration.
 * 
 * <p>Typically used with the AspectJ <code>AnnotationBeanConfigurerAspect</code>.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Adrian Colyer
 * @author Ramnivas Laddad
 * @since 2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Configurable {

	/**
	 * The name of the bean definition that serves as the configuration template.
	 */
	String value() default "";

	/**
	 * Are dependencies to be injected via autowiring?
	 */
	Autowire autowire() default Autowire.NO;

	/**
	 * Is dependency checking to be performed for configured objects?
	 */
	boolean dependencyCheck() default false;
	
	/**
	 * Are dependencies to be injected prior to the construction of an object?
	 */
	boolean preConstruction() default false;

}
