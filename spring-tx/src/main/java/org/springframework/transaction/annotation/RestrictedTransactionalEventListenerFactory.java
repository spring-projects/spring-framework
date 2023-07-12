/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.annotation;

import java.lang.reflect.Method;

import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListenerFactory;

/**
 * Extension of {@link TransactionalEventListenerFactory},
 * detecting invalid transaction configuration for transactional event listeners:
 * {@link Transactional} only supported with {@link Propagation#REQUIRES_NEW} or
 * {@link Async}.
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see org.springframework.transaction.event.TransactionalEventListener
 * @see Transactional
 */
public class RestrictedTransactionalEventListenerFactory extends TransactionalEventListenerFactory {

	@Override
	public ApplicationListener<?> createApplicationListener(String beanName, Class<?> type, Method method) {
		Transactional txAnn = AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);
		if (txAnn != null && txAnn.propagation() != Propagation.REQUIRES_NEW &&
				!AnnotatedElementUtils.hasAnnotation(method, Async.class)) {
			throw new IllegalStateException("@TransactionalEventListener method must not be annotated with " +
					"@Transactional unless when marked as REQUIRES_NEW or declared as @Async: " + method);
		}
		return super.createApplicationListener(beanName, type, method);
	}

}
