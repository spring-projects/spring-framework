/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.test.bean.override.mockito;

import java.lang.annotation.Annotation;
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
import org.springframework.test.bean.override.BeanOverrideStrategy;
import org.springframework.test.util.AopTestUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.mockito.Mockito.mock;

/**
 * A complete definition that can be used to create a Mockito spy.
 *
 * @author Phillip Webb
 */
class SpyDefinition extends Definition {

	SpyDefinition(MockitoSpyBean spyAnnotation, Field field, ResolvableType typeToSpy) {
		this(spyAnnotation.name(), spyAnnotation.reset(), spyAnnotation.proxyTargetAware(), field,
				spyAnnotation, typeToSpy);
	}

	SpyDefinition(String name, MockReset reset, boolean proxyTargetAware, Field field, Annotation annotation,
			ResolvableType typeToSpy) {
		super(name, reset, proxyTargetAware, field, annotation, typeToSpy, BeanOverrideStrategy.WRAP_EARLY_BEAN);
		Assert.notNull(typeToSpy, "typeToSpy must not be null");
	}

	@Override
	public String getBeanOverrideDescription() {
		return "spy";
	}

	@Override
	protected Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition, @Nullable Object existingBeanInstance) {
		return createSpy(beanName, Objects.requireNonNull(existingBeanInstance,
				"MockitoSpyBean requires an existing bean instance for bean " + beanName));
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		//for SpyBean we want the class to be exactly the same
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		SpyDefinition other = (SpyDefinition) obj;
		boolean result = super.equals(obj);
		result = result && ObjectUtils.nullSafeEquals(this.typeToOverride(), other.typeToOverride());
		return result;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.typeToOverride());
		return result;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("name", this.name)
			.append("typeToSpy", typeToOverride())
			.append("reset", getReset())
			.toString();
	}

	<T> T createSpy(Object instance) {
		return createSpy(this.name, instance);
	}

	@SuppressWarnings("unchecked")
	<T> T createSpy(String name, Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		Assert.isInstanceOf(Objects.requireNonNull(this.typeToOverride().resolve()), instance);
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
			toSpy = this.typeToOverride().toClass();
		}
		else {
			settings.defaultAnswer(Mockito.CALLS_REAL_METHODS);
			settings.spiedInstance(instance);
			toSpy = instance.getClass();
		}
		return (T) mock(toSpy, settings);
	}

	/**
	 * A {@link VerificationStartedListener} that bypasses any proxy created by Spring AOP
	 * when the verification of a spy starts.
	 */
	private static final class SpringAopBypassingVerificationStartedListener implements VerificationStartedListener {

		@Override
		public void onVerificationStarted(VerificationStartedEvent event) {
			event.setMock(AopTestUtils.getUltimateTargetObject(event.getMock()));
		}

	}

}
