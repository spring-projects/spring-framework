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

import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.BeanOverrideHandler;
import org.springframework.test.context.bean.override.BeanOverrideStrategy;

/**
 * Abstract base {@link BeanOverrideHandler} implementation for Mockito.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
abstract class AbstractMockitoBeanOverrideHandler extends BeanOverrideHandler {

	private final MockReset reset;

	private final boolean proxyTargetAware;


	protected AbstractMockitoBeanOverrideHandler(Field field, ResolvableType beanType, @Nullable String beanName,
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
	protected void trackOverrideInstance(Object mock, SingletonBeanRegistry trackingBeanRegistry) {
		getMockitoBeans(trackingBeanRegistry).add(mock);
	}

	private static MockitoBeans getMockitoBeans(SingletonBeanRegistry trackingBeanRegistry) {
		String beanName = MockitoBeans.class.getName();
		MockitoBeans mockitoBeans = null;
		if (trackingBeanRegistry.containsSingleton(beanName)) {
			mockitoBeans = (MockitoBeans) trackingBeanRegistry.getSingleton(beanName);
		}
		if (mockitoBeans == null) {
			mockitoBeans = new MockitoBeans();
			trackingBeanRegistry.registerSingleton(beanName, mockitoBeans);
		}
		return mockitoBeans;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		return (other instanceof AbstractMockitoBeanOverrideHandler that && super.equals(that) &&
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
