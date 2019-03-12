package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * {@code @PrototypeScope} is a specialization of {@link Scope @Scope} for a
 * standard prototype scope: "prototype".
 *
 * <p>Specifically, {@code @PrototypeScope} is a <em>composed annotation</em> that
 * acts as a shortcut for {@code @Scope("prototype")}.
 *
 * <p>{@code @PrototypeScope} may be used as a meta-annotation to create custom
 * composed annotations.
 *
 * @author Bojan Vukasovic
 * @since 5.1
 * @see SingletonScope
 * @see org.springframework.context.annotation.Scope
 * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE
 * @see org.springframework.stereotype.Component
 * @see org.springframework.context.annotation.Bean
 */
@Target({ ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public @interface PrototypeScope {
}
