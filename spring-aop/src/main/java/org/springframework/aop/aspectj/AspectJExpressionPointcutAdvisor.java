/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractGenericPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

/**
 * Spring AOP Advisor that can be used for any AspectJ pointcut expression.
 *
 * @author Rob Harrop
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AspectJExpressionPointcutAdvisor extends AbstractGenericPointcutAdvisor implements BeanFactoryAware {

	private final AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();


	public void setExpression(@Nullable String expression) {
		this.pointcut.setExpression(expression);
	}

	public @Nullable String getExpression() {
		return this.pointcut.getExpression();
	}

	public void setLocation(@Nullable String location) {
		this.pointcut.setLocation(location);
	}

	public @Nullable String getLocation() {
		return this.pointcut.getLocation();
	}

	public void setParameterNames(String... names) {
		this.pointcut.setParameterNames(names);
	}

	public void setParameterTypes(Class<?>... types) {
		this.pointcut.setParameterTypes(types);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.pointcut.setBeanFactory(beanFactory);
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
