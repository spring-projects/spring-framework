/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import org.mockito.plugins.MockResolver;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A {@link MockResolver} for testing Spring applications with Mockito.
 *
 * <p>Resolves mocks by walking the Spring AOP proxy chain until the target or a
 * non-static proxy is found.
 *
 * @author Sam Brannen
 * @author Andy Wilkinson
 * @author Juergen Hoeller
 * @since 6.2
 */
public class SpringMockResolver implements MockResolver {

	static final boolean SPRING_AOP_PRESENT = ClassUtils.isPresent(
			"org.springframework.aop.framework.Advised", SpringMockResolver.class.getClassLoader());


	@Override
	public Object resolve(Object instance) {
		if (SPRING_AOP_PRESENT) {
			return getUltimateTargetObject(instance);
		}
		return instance;
	}

	/**
	 * This is a modified version of
	 * {@link org.springframework.test.util.AopTestUtils#getUltimateTargetObject(Object)
	 * AopTestUtils#getUltimateTargetObject()} which only checks static target sources.
	 * @param candidate the instance to check (potentially a Spring AOP proxy;
	 * never {@code null})
	 * @return the target object or the {@code candidate} (never {@code null})
	 * @throws IllegalStateException if an error occurs while unwrapping a proxy
	 * @see Advised#getTargetSource()
	 * @see TargetSource#isStatic()
	 */
	static Object getUltimateTargetObject(Object candidate) {
		Assert.notNull(candidate, "Candidate must not be null");
		try {
			if (AopUtils.isAopProxy(candidate) && candidate instanceof Advised advised) {
				TargetSource targetSource = advised.getTargetSource();
				if (targetSource.isStatic()) {
					Object target = targetSource.getTarget();
					if (target != null) {
						return getUltimateTargetObject(target);
					}
				}
			}
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to unwrap proxied object", ex);
		}
		return candidate;
	}

	/**
	 * Reject the supplied bean if it is not a supported candidate to spy on.
	 * <p>Specifically, this method ensures that the bean is not a Spring AOP proxy
	 * with a non-static {@link TargetSource}.
	 * @param beanName the name of the bean to spy on
	 * @param bean the bean to spy on
	 * @since 7.0
	 * @see #getUltimateTargetObject(Object)
	 */
	static void rejectUnsupportedSpyTarget(String beanName, Object bean) throws IllegalStateException {
		if (SPRING_AOP_PRESENT) {
			if (AopUtils.isAopProxy(bean) && bean instanceof Advised advised &&
					!advised.getTargetSource().isStatic()) {
				throw new IllegalStateException("""
					@MockitoSpyBean cannot be applied to bean '%s', because it is a Spring AOP proxy \
					with a non-static TargetSource. Perhaps you have attempted to spy on a scoped proxy, \
					which is not supported.""".formatted(beanName));
			}
		}
	}

}
