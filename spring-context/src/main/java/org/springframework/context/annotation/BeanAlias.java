package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation that declares a single bean alias.
 *
 * <p>This annotation can be used on a configuration class to declare an alias for a specific bean.
 * It is processed by the {@link BeanAliasRegistrar} which registers the specified alias with the
 * application context. Multiple aliases can be declared using the {@link BeanAliases} container
 * annotation.
 *
 * @author Tiger Zhao
 * @since 7.0.0
 */
@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(BeanAliases.class)
@Documented
@Import(BeanAliasRegistrar.class)
public @interface BeanAlias {

	/**
	 * The name of the bean for which this alias is being declared.
	 *
	 * @return the name of the bean
	 */
	String name();

	/**
	 * An array of alias names for the specified bean.
	 *
	 * @return an array of alias names
	 */
	String[] alias();
}
