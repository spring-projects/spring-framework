package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * {@code @SingletonScope} is a specialization of {@link Scope @Scope} for a
 * standard singleton scope: "singleton".
 *
 * <p>Specifically, {@code @SingletonScope} is a <em>composed annotation</em> that
 * acts as a shortcut for {@code @Scope("singleton")}.
 *
 * <p>{@code @SingletonScope} may be used as a meta-annotation to create custom
 * composed annotations.
 *
 * @author Bojan Vukasovic
 * @since 5.1
 * @see PrototypeScope
 * @see org.springframework.context.annotation.Scope
 * @see ConfigurableBeanFactory#SCOPE_SINGLETON
 * @see org.springframework.stereotype.Component
 * @see org.springframework.context.annotation.Bean
 */
@Target({ ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public @interface SingletonScope {
}
