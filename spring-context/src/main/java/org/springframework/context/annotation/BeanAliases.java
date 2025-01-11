package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation that aggregates multiple {@link BeanAlias} declarations.
 *
 * <p>This annotation can be used on a configuration class to declare multiple bean aliases
 * in a single place. It is processed by the {@link BeanAliasRegistrar} which registers the
 * specified aliases with the application context.
 *
 * @author Tiger Zhao
 * @since 7.0.0
 */
@Retention(RUNTIME)
@Target(TYPE)
@Documented
@Import(BeanAliasRegistrar.class)
public @interface BeanAliases {

	/**
	 * Returns an array of {@link BeanAlias} declarations.
	 *
	 * @return an array of {@code @BeanAlias} annotations
	 */
	BeanAlias[] value();
}
