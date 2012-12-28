/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj;

import org.springframework.aop.framework.AopConfigException;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * Implementation of {@link AspectInstanceFactory} that creates a new instance
 * of the specified aspect class for every {@link #getAspectInstance()} call.
 *
 * @author Juergen Hoeller
 * @since 2.0.4
 */
public class SimpleAspectInstanceFactory implements AspectInstanceFactory {

	private final Class aspectClass;


	/**
	 * Create a new SimpleAspectInstanceFactory for the given aspect class.
	 * @param aspectClass the aspect class
	 */
	public SimpleAspectInstanceFactory(Class aspectClass) {
		Assert.notNull(aspectClass, "Aspect class must not be null");
		this.aspectClass = aspectClass;
	}

	/**
	 * Return the specified aspect class (never {@code null}).
	 */
	public final Class getAspectClass() {
		return this.aspectClass;
	}


	@Override
	public final Object getAspectInstance() {
		try {
			return this.aspectClass.newInstance();
		}
		catch (InstantiationException ex) {
			throw new AopConfigException("Unable to instantiate aspect class [" + this.aspectClass.getName() + "]", ex);
		}
		catch (IllegalAccessException ex) {
			throw new AopConfigException("Cannot access element class [" + this.aspectClass.getName() + "]", ex);
		}
	}

	@Override
	public ClassLoader getAspectClassLoader() {
		return this.aspectClass.getClassLoader();
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
		return getOrderForAspectClass(this.aspectClass);
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
