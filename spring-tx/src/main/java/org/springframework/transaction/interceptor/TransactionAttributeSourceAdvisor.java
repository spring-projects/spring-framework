/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.interceptor;

import org.aopalliance.aop.Advice;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.util.Assert;

/**
 * Advisor driven by a {@link TransactionAttributeSource}, used to include
 * a {@link TransactionInterceptor} only for methods that are transactional.
 *
 * <p>Because the AOP framework caches advice calculations, this is normally
 * faster than just letting the TransactionInterceptor run and find out
 * itself that it has no work to do.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setTransactionInterceptor
 * @see TransactionProxyFactoryBean
 */
@SuppressWarnings("serial")
public class TransactionAttributeSourceAdvisor extends AbstractPointcutAdvisor {

	private @Nullable TransactionInterceptor transactionInterceptor;

	private final TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();


	/**
	 * Create a new TransactionAttributeSourceAdvisor.
	 */
	public TransactionAttributeSourceAdvisor() {
	}

	/**
	 * Create a new TransactionAttributeSourceAdvisor.
	 * @param interceptor the transaction interceptor to use for this advisor
	 */
	public TransactionAttributeSourceAdvisor(TransactionInterceptor interceptor) {
		setTransactionInterceptor(interceptor);
	}


	/**
	 * Set the transaction interceptor to use for this advisor.
	 */
	public void setTransactionInterceptor(TransactionInterceptor interceptor) {
		Assert.notNull(interceptor, "TransactionInterceptor must not be null");
		this.transactionInterceptor = interceptor;
		this.pointcut.setTransactionAttributeSource(interceptor.getTransactionAttributeSource());
	}

	/**
	 * Set the {@link ClassFilter} to use for this pointcut.
	 * Default is {@link ClassFilter#TRUE}.
	 */
	public void setClassFilter(ClassFilter classFilter) {
		this.pointcut.setClassFilter(classFilter);
	}


	@Override
	public Advice getAdvice() {
		Assert.state(this.transactionInterceptor != null, "No TransactionInterceptor set");
		return this.transactionInterceptor;
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
