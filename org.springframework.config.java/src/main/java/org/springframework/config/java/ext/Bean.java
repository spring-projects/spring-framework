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
package org.springframework.config.java.ext;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.config.java.Configuration;
import org.springframework.config.java.StandardScopes;


/**
 * Annotation to be applied to methods that create beans in a Spring context. The name of
 * the bean is the method name. (It is also possible to specify aliases using the aliases
 * array on this annotation.)
 * 
 * <p>
 * Contains information similar to that held in Spring's internal BeanDefinition metadata.
 * </p>
 * 
 * <p>
 * Bean creation methods must be non-private (default, public or protected). Bean creation
 * methods may throw any exception, which will be caught and handled by the Spring container
 * on processing of the configuration class.<br>
 * Bean creation methods must return an object type. The decision to return a class or an
 * interface will be significant in the event of proxying. Bean methods that return
 * interfaces will be proxied using dynamic proxies; those that return a class will require
 * CGLIB or other subclass-based proxying. It is recommended to return an interface where
 * possible, as this is also consistent with best practice around loose coupling.
 * </p>
 * 
 * <p>
 * Bean creation methods may reference other bean creation methods by calling them directly,
 * as follows. This ensures that references between beans are strongly typed:
 * </p>
 * 
 * @see Configuration
 * 
 * @author Rod Johnson
 * @author Costin Leau
 * @author Chris Beams
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Bean {

	/**
	 * Role this bean plays in the overall application configuration.
	 * 
	 * @see BeanDefinition#ROLE_APPLICATION
	 * @see BeanDefinition#ROLE_INFRASTRUCTURE
	 * @see BeanDefinition#ROLE_SUPPORT
	 * 
	 * @see AbstractBeanDefinition the 'role' field is assigned by default to
	 *      ROLE_APPLICATION
	 */
	int role() default BeanDefinition.ROLE_APPLICATION;

	/**
	 * Bean aliases.
	 */
	String[] aliases() default {};

	/**
	 * Scope: whether the bean is a singleton, prototype or custom scope. Default is
	 * singleton.
	 */
	String scope() default StandardScopes.SINGLETON;

	/**
	 * Bean autowire strategy.
	 */
	Autowire autowire() default Autowire.INHERITED;

	// /**
	// * Bean lazy strategy.
	// */
	// Lazy lazy() default Lazy.UNSPECIFIED;
	//
	// /**
	// * A bean may be marked as primary, useful for disambiguation when looking
	// * up beans by type.
	// *
	// * @see
	// org.springframework.config.java.context.JavaConfigApplicationContext#getBean(Class);
	// */
	// Primary primary() default Primary.UNSPECIFIED;

	/**
	 * Bean init method name. Normally this is not needed, as the initialization (with
	 * parameterization) can be done directly through java code.
	 */
	String initMethodName() default "";

	/**
	 * Bean destroy method name.
	 */
	String destroyMethodName() default "";

	// /**
	// * Bean dependency check strategy.
	// */
	// DependencyCheck dependencyCheck() default DependencyCheck.UNSPECIFIED;

	/**
	 * Beans on which the current bean depends on.
	 */
	String[] dependsOn() default {};

	// /**
	// * Metadata for the current bean.
	// */
	// Meta[] meta() default { };

	/**
	 * Allow the bean to be overridden in another JavaConfig, XML or other non-Java
	 * configuration. This is consistent with DefaultListableBeanFactory's
	 * allowBeanDefinitionOverriding property, which defaults to true.
	 * 
	 * @return whether overriding of this bean is allowed
	 */
	boolean allowOverriding() default true;

}

