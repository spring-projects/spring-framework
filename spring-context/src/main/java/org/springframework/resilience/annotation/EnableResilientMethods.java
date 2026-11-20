/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.resilience.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * Enables Spring's core resilience features for method invocations:
 * {@link Retryable @Retryable} as well as {@link ConcurrencyLimit @ConcurrencyLimit}.
 *
 * <p>These annotations can also be individually enabled by
 * defining a {@link RetryAnnotationBeanPostProcessor} or a
 * {@link ConcurrencyLimitBeanPostProcessor}.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see RetryAnnotationBeanPostProcessor
 * @see ConcurrencyLimitBeanPostProcessor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ResilientMethodsConfiguration.class)
public @interface EnableResilientMethods {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies.
	 * <p>The default is {@code false}.
	 * <p>Note that setting this attribute to {@code true} will only affect
	 * {@link RetryAnnotationBeanPostProcessor} and
	 * {@link ConcurrencyLimitBeanPostProcessor}.
	 * <p>It is usually recommendable to rely on a global default proxy configuration
	 * instead, with specific proxy requirements for certain beans expressed through
	 * a {@link org.springframework.context.annotation.Proxyable} annotation on
	 * the affected bean classes.
	 * @see org.springframework.aop.config.AopConfigUtils#forceAutoProxyCreatorToUseClassProxying
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate the order in which the {@link RetryAnnotationBeanPostProcessor}
	 * and {@link ConcurrencyLimitBeanPostProcessor} should be applied.
	 * <p>The default is {@link Ordered#LOWEST_PRECEDENCE - 1} in order to run
	 * after all common post-processors, except for {@code @EnableAsync}.
	 * @see org.springframework.scheduling.annotation.EnableAsync#order()
	 */
	int order() default Ordered.LOWEST_PRECEDENCE - 1;

}
