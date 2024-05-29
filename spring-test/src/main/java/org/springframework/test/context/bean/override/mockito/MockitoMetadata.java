/*
 * Copyright 2012-2024 the original author or authors.
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
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.BeanOverrideStrategy;
import org.springframework.test.context.bean.override.OverrideMetadata;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for Mockito override metadata.
 *
 * @author Phillip Webb
 * @since 6.2
 */
abstract class MockitoMetadata extends OverrideMetadata {

	protected final String name;

	private final MockReset reset;

	private final boolean proxyTargetAware;


	MockitoMetadata(String name, @Nullable MockReset reset, boolean proxyTargetAware, Field field,
			ResolvableType typeToOverride, BeanOverrideStrategy strategy) {

		super(field, typeToOverride, strategy);
		this.name = name;
		this.reset = (reset != null) ? reset : MockReset.AFTER;
		this.proxyTargetAware = proxyTargetAware;
	}


	@Override
	@Nullable
	protected String getBeanName() {
		return StringUtils.hasText(this.name) ? this.name : super.getBeanName();
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
			tracker= new MockitoBeans();
			trackingBeanRegistry.registerSingleton(MockitoBeans.class.getName(), tracker);
		}
		tracker.add(mock);
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
	public boolean equals(@Nullable Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		MockitoMetadata other = (MockitoMetadata) obj;
		boolean result = super.equals(obj);
		result = result && ObjectUtils.nullSafeEquals(this.name, other.name);
		result = result && ObjectUtils.nullSafeEquals(this.reset, other.reset);
		result = result && ObjectUtils.nullSafeEquals(this.proxyTargetAware, other.proxyTargetAware);
		return result;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), this.name, this.reset, this.proxyTargetAware);
	}

}
