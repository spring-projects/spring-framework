/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.BeanRegistrar;

/**
 * Indicates one or more <em>component classes</em> to import &mdash; typically
 * {@link Configuration @Configuration} classes.
 *
 * <p>Provides functionality equivalent to the {@code <import/>} element in Spring XML.
 *
 * <p>Allows for importing {@code @Configuration} classes, {@link ImportSelector},
 * {@link ImportBeanDefinitionRegistrar}, and {@link BeanRegistrar} implementations,
 * as well as regular component classes (analogous to
 * {@link AnnotationConfigApplicationContext#register}).
 *
 * <p>{@code @Bean} definitions declared in imported {@code @Configuration} classes should be
 * accessed by using {@link org.springframework.beans.factory.annotation.Autowired @Autowired}
 * injection. Either the bean itself can be autowired, or the configuration class instance
 * declaring the bean can be autowired. The latter approach allows for explicit, IDE-friendly
 * navigation between {@code @Configuration} class methods.
 *
 * <p>May be declared directly at the class level or as a meta-annotation.
 * {@code @Import} annotations declared directly at the class level are processed
 * after {@code @Import} annotations declared as meta-annotations, which allows
 * directly declared imports to override beans registered via {@code @Import}
 * meta-annotations.
 *
 * <p>As of Spring Framework 7.0, {@code @Import} annotations declared on interfaces
 * implemented by {@code @Configuration} classes are also supported. Locally declared
 * {@code @Import} annotations are processed after {@code @Import} annotations on
 * interfaces, which allows local imports to override beans registered via
 * {@code @Import} annotations inherited from interfaces.
 *
 * <p>If XML or other non-{@code @Configuration} bean definition resources need to be
 * imported, use the {@link ImportResource @ImportResource} annotation instead.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Configuration
 * @see ImportSelector
 * @see ImportBeanDefinitionRegistrar
 * @see ImportResource
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Import {

	/**
	 * {@link Configuration @Configuration}, {@link ImportSelector},
	 * {@link ImportBeanDefinitionRegistrar}, {@link BeanRegistrar},
	 * or regular component classes to import.
	 */
	Class<?>[] value();

}
