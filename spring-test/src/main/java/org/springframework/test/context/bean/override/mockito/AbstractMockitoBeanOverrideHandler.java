/*
 * Copyright 2002-2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.style.ToStringCreator;
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


	protected AbstractMockitoBeanOverrideHandler(@Nullable Field field, ResolvableType beanType,
			@Nullable String beanName, String contextName, BeanOverrideStrategy strategy,
			MockReset reset) {

		super(field, beanType, beanName, contextName, strategy);
		this.reset = (reset != null ? reset : MockReset.AFTER);
	}


	/**
	 * Return the mock reset mode.
	 * @return the reset mode
	 */
	MockReset getReset() {
		return this.reset;
	}

	@Override
	protected void trackOverrideInstance(Object mock, SingletonBeanRegistry trackingBeanRegistry) {
		getMockBeans(trackingBeanRegistry).add(mock);
	}

	private static MockBeans getMockBeans(SingletonBeanRegistry trackingBeanRegistry) {
		String beanName = MockBeans.class.getName();
		MockBeans mockBeans = null;
		if (trackingBeanRegistry.containsSingleton(beanName)) {
			mockBeans = (MockBeans) trackingBeanRegistry.getSingleton(beanName);
		}
		if (mockBeans == null) {
			mockBeans = new MockBeans();
			trackingBeanRegistry.registerSingleton(beanName, mockBeans);
		}
		return mockBeans;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		return (other instanceof AbstractMockitoBeanOverrideHandler that && super.equals(that) &&
				this.reset == that.reset);
	}

	@Override
	public int hashCode() {
		return super.hashCode() + this.reset.hashCode();
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("field", getField())
				.append("beanType", getBeanType())
				.append("beanName", getBeanName())
				.append("contextName", getContextName())
				.append("strategy", getStrategy())
				.append("reset", getReset())
				.toString();
	}

}
