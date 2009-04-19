/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowire;

/**
 * Indicates that a method produces a bean to be managed by the Spring container. The
 * names and semantics of the attributes to this annotation are intentionally similar
 * to those of the {@literal <bean/>} element in the Spring XML schema. Deviations are
 * as follows:
 * 
 * <p>The Bean annotation does not provide attributes for scope, primary or lazy. Rather,
 * it should be used in conjunction with {@link Scope}, {@link Primary} and {@link Lazy}
 * annotations to acheive the same semantics.
 * 
 * <p>While a {@link #name()} attribute is available, the default strategy for determining
 * the name of a bean is to use the name of the Bean method. This is convenient and
 * intuitive, but if explicit naming is desired, the {@link #name()} attribute may be used.
 * Also note that {@link #name()} accepts an array of strings. This is in order to allow
 * for specifying multiple names (aka aliases) for a single bean.
 * 
 * <h3>Constraints</h3>
 * <ul>
 *     <li>Bean methods are valid only when declared within a {@link Configuration}-annotated class
 * 	   <li>Bean methods must be non-void, non-final, non-private
 * 	   <li>Bean methods may not accept any arguments
 *     <li>Bean methods may throw any exception, which will be caught and handled
 *  by the Spring container on processing of the declaring {@link Configuration} class.
 * </ul>
 * 
 * <h3>Usage</h3>
 * <p>Bean methods may reference other Bean methods by calling them directly. This ensures
 * that references between beans are strongly typed and navigable. So called 'inter-bean
 * references' are guaranteed to respect scoping and AOP semantics.
 * 
 * @author Rod Johnson
 * @author Costin Leau
 * @author Chris Beams
 * @since 3.0
 * @see Configuration
 * @see Lazy
 * @see Primary
 * @see org.springframework.context.annotation.Scope
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {

	/**
	 * The name of this bean, or if plural, aliases for this bean. If left unspecified
	 * the name of the bean is the name of the annotated method. If specified, the method
	 * name is ignored.
	 */
	String[] name() default {};

	/**
	 * Are dependencies to be injected via autowiring?
	 */
	Autowire autowire() default Autowire.NO;

	/**
	 * The optional name of a method to call on the bean instance during initialization.
	 * Not commonly used, given that the method may be called programmatically directly
	 * within the body of a Bean-annotated method.
	 */
	String initMethod() default "";

	/**
	 * The optional name of a method to call on the bean instance during upon closing the
	 * application context, for example a {@literal close()} method on a {@literal DataSource}.
	 * The method must have no arguments, but may throw any exception.
	 * <p>Note: Only invoked on beans whose lifecycle is under the full control of the
	 * factory which is always the case for singletons, but not guaranteed 
     * for any other scope.
	 * see {@link org.springframework.context.ConfigurableApplicationContext#close()}
	 */
	String destroyMethod() default "";

}
