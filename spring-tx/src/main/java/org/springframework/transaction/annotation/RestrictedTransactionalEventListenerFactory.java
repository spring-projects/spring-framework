/*
 * Copyright 2002-2024 the original author or authors.
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
		Transactional txAnn = AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);
		if (txAnn != null) {
			Propagation propagation = txAnn.propagation();
			if (propagation != Propagation.REQUIRES_NEW && propagation != Propagation.NOT_SUPPORTED) {
				throw new IllegalStateException("@TransactionalEventListener method must not be annotated with " +
						"@Transactional unless when declared as REQUIRES_NEW or NOT_SUPPORTED: " + method);
			}
		}
		return super.createApplicationListener(beanName, type, method);
	}

}
