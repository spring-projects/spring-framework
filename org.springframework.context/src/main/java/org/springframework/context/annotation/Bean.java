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
 * to those of the {@literal <bean/>} element in the Spring XML schema.
 *
 * <p>Note that the <code>@Bean</code> annotation does not provide attributes for scope,
 * primary or lazy. Rather, it should be used in conjunction with {@link Scope &#064;Scope},
 * {@link Primary &#064;Primary}, and {@link Lazy &#064;Lazy} annotations to achieve
 * those semantics. The same annotations can also be used at the type level, e.g. for
 * component scanning.
 *
 * <p>While a {@link #name()} attribute is available, the default strategy for determining
 * the name of a bean is to use the name of the Bean method. This is convenient and
 * intuitive, but if explicit naming is desired, the {@link #name()} attribute may be used.
 * Also note that {@link #name()} accepts an array of Strings. This is in order to allow
 * for specifying multiple names (i.e., aliases) for a single bean.
 *
 * <p>The <code>@Bean</code> annotation may be used on any methods in an <code>@Component</code>
 * class, in which case they will get processed in a configuration class 'lite' mode where
 * they will simply be called as plain factory methods from the container (similar to
 * <code>factory-method</code> declarations in XML). The containing component classes remain
 * unmodified in this case, and there are no unusual constraints for factory methods.
 *
 * <p>As an advanced mode, <code>@Bean</code> may also be used within <code>@Configuration</code>
 * component classes. In this case, bean methods may reference other <code>@Bean</code> methods
 * on the same class by calling them <i>directly</i>. This ensures that references between beans
 * are strongly typed and navigable. Such so-called 'inter-bean references' are guaranteed to
 * respect scoping and AOP semantics, just like <code>getBean</code> lookups would. These are
 * the semantics known from the original 'Spring JavaConfig' project which require CGLIB
 * subclassing of each such configuration class at runtime. As a consequence, configuration
 * classes and their factory methods must not be marked as final or private in this mode.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.stereotype.Component
 * @see Configuration
 * @see Scope
 * @see DependsOn
 * @see Lazy
 * @see Primary
 * @see org.springframework.beans.factory.annotation.Autowired
 * @see org.springframework.beans.factory.annotation.Value
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
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
	 * The optional name of a method to call on the bean instance upon closing the
	 * application context, for example a {@literal close()} method on a {@literal DataSource}.
	 * The method must have no arguments but may throw any exception.
	 * <p>Note: Only invoked on beans whose lifecycle is under the full control of the
	 * factory, which is always the case for singletons but not guaranteed 
	 * for any other scope.
	 * @see {@link org.springframework.context.ConfigurableApplicationContext#close()}
	 */
	String destroyMethod() default "";

}
