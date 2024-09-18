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

package org.springframework.test.context.bean.override.mockito;

import java.lang.reflect.Field;
import java.util.Objects;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.BeanOverrideStrategy;
import org.springframework.test.context.bean.override.OverrideMetadata;

/**
 * Base {@link OverrideMetadata} implementation for Mockito.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
abstract class MockitoOverrideMetadata extends OverrideMetadata {

	private final MockReset reset;

	private final boolean proxyTargetAware;


	protected MockitoOverrideMetadata(Field field, ResolvableType beanType, @Nullable String beanName,
			BeanOverrideStrategy strategy, @Nullable MockReset reset, boolean proxyTargetAware) {

		super(field, beanType, beanName, strategy);
		this.reset = (reset != null ? reset : MockReset.AFTER);
		this.proxyTargetAware = proxyTargetAware;
	}


	/**
	 * Return the mock reset mode.
	 * @return the reset mode
	 */
	MockReset getReset() {
		return this.reset;
	}

	/**
	 * Return if AOP advised beans should be proxy target aware.
	 * @return if proxy target aware
	 */
	boolean isProxyTargetAware() {
		return this.proxyTargetAware;
	}

	@Override
	protected void track(Object mock, SingletonBeanRegistry trackingBeanRegistry) {
		MockitoBeans tracker = null;
		try {
			tracker = (MockitoBeans) trackingBeanRegistry.getSingleton(MockitoBeans.class.getName());
		}
		catch (NoSuchBeanDefinitionException ignored) {
		}
		if (tracker == null) {
			tracker = new MockitoBeans();
			trackingBeanRegistry.registerSingleton(MockitoBeans.class.getName(), tracker);
		}
		tracker.add(mock);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		return (other instanceof MockitoOverrideMetadata that && super.equals(that) &&
				(this.reset == that.reset) && (this.proxyTargetAware == that.proxyTargetAware));
	}

	@Override
	public int hashCode() {
		return super.hashCode() + Objects.hash(this.reset, this.proxyTargetAware);
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("field", getField())
				.append("beanType", getBeanType())
				.append("beanName", getBeanName())
				.append("strategy", getStrategy())
				.append("reset", getReset())
				.append("proxyTargetAware", isProxyTargetAware())
				.toString();
	}

}
