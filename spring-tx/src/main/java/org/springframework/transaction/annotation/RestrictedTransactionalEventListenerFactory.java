/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.annotation;

import java.lang.reflect.Method;

import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalApplicationListenerMethodAdapter;
import org.springframework.transaction.event.TransactionalEventListenerFactory;

/**
 * Extension of {@link TransactionalEventListenerFactory},
 * detecting invalid transaction configuration for transactional event listeners:
 * {@link Transactional} only supported with {@link Propagation#REQUIRES_NEW}
 * and {@link Propagation#NOT_SUPPORTED}.
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see org.springframework.transaction.event.TransactionalEventListener
 * @see Transactional
 */
public class RestrictedTransactionalEventListenerFactory extends TransactionalEventListenerFactory {

	@Override
	public ApplicationListener<?> createApplicationListener(String beanName, Class<?> type, Method method) {
		TransactionalApplicationListenerMethodAdapter adapter =
				new TransactionalApplicationListenerMethodAdapter(beanName, type, method);
		if (adapter.getTransactionPhase() != TransactionPhase.BEFORE_COMMIT) {
			Transactional txAnn = AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);
			if (txAnn == null) {
				txAnn = AnnotatedElementUtils.findMergedAnnotation(type, Transactional.class);
			}
			if (txAnn != null) {
				Propagation propagation = txAnn.propagation();
				if (propagation != Propagation.REQUIRES_NEW && propagation != Propagation.NOT_SUPPORTED) {
					throw new IllegalStateException("@TransactionalEventListener method must not be annotated with " +
							"@Transactional unless when declared as REQUIRES_NEW or NOT_SUPPORTED: " + method);
				}
			}
		}
		return adapter;
	}

}
