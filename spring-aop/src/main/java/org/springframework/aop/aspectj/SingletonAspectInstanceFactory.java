/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop.aspectj;

import java.io.Serializable;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of {@link AspectInstanceFactory} that is backed by a
 * specified singleton object, returning the same instance for every
 * {@link #getAspectInstance()} call.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see SimpleAspectInstanceFactory
 */
@SuppressWarnings("serial")
public class SingletonAspectInstanceFactory implements AspectInstanceFactory, Serializable {

	private final Object aspectInstance;


	/**
	 * Create a new SingletonAspectInstanceFactory for the given aspect instance.
	 * @param aspectInstance the singleton aspect instance
	 */
	public SingletonAspectInstanceFactory(Object aspectInstance) {
		Assert.notNull(aspectInstance, "Aspect instance must not be null");
		this.aspectInstance = aspectInstance;
	}


	@Override
	public final Object getAspectInstance() {
		return this.aspectInstance;
	}

	@Override
	@Nullable
	public ClassLoader getAspectClassLoader() {
		return this.aspectInstance.getClass().getClassLoader();
	}

	/**
	 * Determine the order for this factory's aspect instance,
	 * either an instance-specific order expressed through implementing
	 * the {@link org.springframework.core.Ordered} interface,
	 * or a fallback order.
	 * @see org.springframework.core.Ordered
	 * @see #getOrderForAspectClass
	 */
	@Override
	public int getOrder() {
		if (this.aspectInstance instanceof Ordered) {
			return ((Ordered) this.aspectInstance).getOrder();
		}
		return getOrderForAspectClass(this.aspectInstance.getClass());
	}

	/**
	 * Determine a fallback order for the case that the aspect instance
	 * does not express an instance-specific order through implementing
	 * the {@link org.springframework.core.Ordered} interface.
	 * <p>The default implementation simply returns {@code Ordered.LOWEST_PRECEDENCE}.
	 * @param aspectClass the aspect class
	 */
	protected int getOrderForAspectClass(Class<?> aspectClass) {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
