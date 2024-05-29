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
import java.lang.reflect.Proxy;
import java.util.Objects;

import org.mockito.AdditionalAnswers;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.listeners.VerificationStartedEvent;
import org.mockito.listeners.VerificationStartedListener;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.BeanOverrideStrategy;
import org.springframework.test.context.bean.override.OverrideMetadata;
import org.springframework.test.util.AopTestUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.mockito.Mockito.mock;

/**
 * {@link OverrideMetadata} for Mockito {@code spy} support.
 *
 * @author Phillip Webb
 * @author Simon Baslé
 * @since 6.2
 */
class MockitoSpyBeanMetadata extends MockitoMetadata {

	MockitoSpyBeanMetadata(MockitoSpyBean spyAnnotation, Field field, ResolvableType typeToSpy) {
		this(spyAnnotation.name(), spyAnnotation.reset(), spyAnnotation.proxyTargetAware(),
				field, typeToSpy);
	}

	MockitoSpyBeanMetadata(String name, MockReset reset, boolean proxyTargetAware, Field field, ResolvableType typeToSpy) {
		super(name, reset, proxyTargetAware, field, typeToSpy, BeanOverrideStrategy.WRAP_BEAN);
		Assert.notNull(typeToSpy, "typeToSpy must not be null");
	}


	@Override
	protected Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition,
			@Nullable Object existingBeanInstance) {

		Assert.notNull(existingBeanInstance,
				() -> "MockitoSpyBean requires an existing bean instance for bean " + beanName);
		return createSpy(beanName, existingBeanInstance);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == this) {
			return true;
		}
		// For SpyBean we want the class to be exactly the same.
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		MockitoSpyBeanMetadata that = (MockitoSpyBeanMetadata) obj;
		return (super.equals(obj) && ObjectUtils.nullSafeEquals(getBeanType(), that.getBeanType()));
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getBeanType());
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("beanName", getBeanName())
				.append("beanType", getBeanType())
				.append("reset", getReset())
				.toString();
	}

	@SuppressWarnings("unchecked")
	<T> T createSpy(String name, Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		Class<?> resolvedTypeToOverride = getBeanType().resolve();
		Assert.notNull(resolvedTypeToOverride, "Failed to resolve type to override");
		Assert.isInstanceOf(resolvedTypeToOverride, instance);
		if (Mockito.mockingDetails(instance).isSpy()) {
			return (T) instance;
		}
		MockSettings settings = MockReset.withSettings(getReset());
		if (StringUtils.hasLength(name)) {
			settings.name(name);
		}
		if (isProxyTargetAware()) {
			settings.verificationStartedListeners(new SpringAopBypassingVerificationStartedListener());
		}
		Class<?> toSpy;
		if (Proxy.isProxyClass(instance.getClass())) {
			settings.defaultAnswer(AdditionalAnswers.delegatesTo(instance));
			toSpy = getBeanType().toClass();
		}
		else {
			settings.defaultAnswer(Mockito.CALLS_REAL_METHODS);
			settings.spiedInstance(instance);
			toSpy = instance.getClass();
		}
		return (T) mock(toSpy, settings);
	}

	/**
	 * A {@link VerificationStartedListener} that bypasses any proxy created by
	 * Spring AOP when the verification of a spy starts.
	 */
	private static final class SpringAopBypassingVerificationStartedListener implements VerificationStartedListener {

		@Override
		public void onVerificationStarted(VerificationStartedEvent event) {
			event.setMock(AopTestUtils.getUltimateTargetObject(event.getMock()));
		}

	}

}
