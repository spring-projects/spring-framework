/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.support;

import java.io.Serializable;

import org.aopalliance.aop.Advice;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.PointcutAdvisor;
import org.springframework.core.Ordered;
import org.springframework.util.ObjectUtils;

/**
 * Abstract base class for {@link org.springframework.aop.PointcutAdvisor}
 * implementations. Can be subclassed for returning a specific pointcut/advice
 * or a freely configurable pointcut/advice.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1.2
 * @see AbstractGenericPointcutAdvisor
 */
@SuppressWarnings("serial")
public abstract class AbstractPointcutAdvisor implements PointcutAdvisor, Ordered, Serializable {

	private @Nullable Integer order;


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		if (this.order != null) {
			return this.order;
		}
		Advice advice = getAdvice();
		if (advice instanceof Ordered ordered) {
			return ordered.getOrder();
		}
		return Ordered.LOWEST_PRECEDENCE;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof PointcutAdvisor otherAdvisor &&
				ObjectUtils.nullSafeEquals(getAdvice(), otherAdvisor.getAdvice()) &&
				ObjectUtils.nullSafeEquals(getPointcut(), otherAdvisor.getPointcut())));
	}

	@Override
	public int hashCode() {
		return PointcutAdvisor.class.hashCode();
	}

}
