/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.rsocket.service;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;

/**
 * Base class for {@link RSocketServiceArgumentResolver} test fixtures.
 * @author Rossen Stoyanchev
 */
public abstract class RSocketServiceArgumentResolverTestSupport {

	private @Nullable RSocketServiceArgumentResolver resolver;

	private final RSocketRequestValues.Builder requestValuesBuilder = RSocketRequestValues.builder(null);

	private @Nullable RSocketRequestValues requestValues;


	protected RSocketServiceArgumentResolverTestSupport() {
		this.resolver = initResolver();
	}

	protected abstract RSocketServiceArgumentResolver initResolver();

	protected static MethodParameter initMethodParameter(Class<?> serviceClass, String methodName, int index) {
		Method method = ClassUtils.getMethod(serviceClass, methodName, (Class<?>[]) null);
		return new MethodParameter(method, index);
	}

	protected boolean execute(Object payload, MethodParameter parameter) {
		return this.resolver.resolve(payload, parameter, this.requestValuesBuilder);
	}

	protected RSocketRequestValues getRequestValues() {
		this.requestValues = (this.requestValues != null ? this.requestValues : this.requestValuesBuilder.build());
		return this.requestValues;
	}

}
