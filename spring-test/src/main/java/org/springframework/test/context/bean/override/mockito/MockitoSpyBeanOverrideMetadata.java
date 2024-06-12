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
import java.lang.reflect.Proxy;

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
import org.springframework.util.StringUtils;

import static org.mockito.Mockito.mock;

/**
 * {@link OverrideMetadata} implementation for Mockito {@code spy} support.
 *
 * @author Phillip Webb
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @since 6.2
 */
class MockitoSpyBeanOverrideMetadata extends MockitoOverrideMetadata {

	MockitoSpyBeanOverrideMetadata(Field field, ResolvableType typeToSpy, MockitoSpyBean spyAnnotation) {
		this(field, typeToSpy, (StringUtils.hasText(spyAnnotation.name()) ? spyAnnotation.name() : null),
				spyAnnotation.reset(), spyAnnotation.proxyTargetAware());
	}

	MockitoSpyBeanOverrideMetadata(Field field, ResolvableType typeToSpy, @Nullable String beanName,
			MockReset reset, boolean proxyTargetAware) {

		super(field, typeToSpy, beanName, BeanOverrideStrategy.WRAP_BEAN, reset, proxyTargetAware);
		Assert.notNull(typeToSpy, "typeToSpy must not be null");
	}


	@Override
	protected Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition,
			@Nullable Object existingBeanInstance) {

		Assert.notNull(existingBeanInstance,
				() -> "MockitoSpyBean requires an existing bean instance for bean " + beanName);
		return createSpy(beanName, existingBeanInstance);
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

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("beanName", getBeanName())
				.append("beanType", getBeanType())
				.append("reset", getReset())
				.toString();
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
