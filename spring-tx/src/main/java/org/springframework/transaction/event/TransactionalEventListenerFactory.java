/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.event;

import java.lang.reflect.Method;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListenerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * {@link EventListenerFactory} implementation that handles {@link TransactionalEventListener}
 * annotated methods.
 *
 * @author Stephane Nicoll
 * @since 4.2
 * @see TransactionalApplicationListenerMethodAdapter
 */
public class TransactionalEventListenerFactory implements EventListenerFactory, Ordered {

	private int order = 50;


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	public boolean supportsMethod(Method method) {
		return AnnotatedElementUtils.hasAnnotation(method, TransactionalEventListener.class);
	}

	@Override
	public ApplicationListener<?> createApplicationListener(String beanName, Class<?> type, Method method) {
		return new TransactionalApplicationListenerMethodAdapter(beanName, type, method);
	}

}
