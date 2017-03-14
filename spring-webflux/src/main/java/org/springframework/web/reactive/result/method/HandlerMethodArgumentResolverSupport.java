/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.web.reactive.result.method;

import java.lang.annotation.Annotation;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.Assert;

/**
 * Base class for {@link HandlerMethodArgumentResolver} implementations with
 * access to a {@code ReactiveAdapterRegistry} and methods to check for
 * method parameter support.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class HandlerMethodArgumentResolverSupport {

	private final ReactiveAdapterRegistry adapterRegistry;


	protected HandlerMethodArgumentResolverSupport(ReactiveAdapterRegistry adapterRegistry) {
		Assert.notNull(adapterRegistry, "ReactiveAdapterRegistry is required");
		this.adapterRegistry = adapterRegistry;
	}


	/**
	 * Return the configured {@link ReactiveAdapterRegistry}.
	 */
	public ReactiveAdapterRegistry getAdapterRegistry() {
		return this.adapterRegistry;
	}


	/**
	 * Evaluate the {@code Predicate} on the the method parameter type or on
	 * the generic type within a reactive type wrapper.
	 */
	protected boolean checkParamType(MethodParameter param, Predicate<Class<?>> predicate) {
		Class<?> type = param.getParameterType();
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(type);
		if (adapter != null) {
			assertHasValues(adapter, param);
			type = param.nested().getNestedParameterType();
		}
		return predicate.test(type);
	}

	private void assertHasValues(ReactiveAdapter adapter, MethodParameter param) {
		if (adapter.isNoValue()) {
			throw new IllegalArgumentException(
					"No value reactive types not supported: " + param.getGenericParameterType());
		}
	}

	/**
	 * Evaluate the {@code Predicate} on the method parameter type but raise an
	 * {@code IllegalStateException} if the same matches the generic type
	 * within a reactive type wrapper.
	 */
	protected boolean checkParamTypeNoReactiveWrapper(MethodParameter param, Predicate<Class<?>> predicate) {
		Class<?> type = param.getParameterType();
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(type);
		if (adapter != null) {
			assertHasValues(adapter, param);
			type = param.nested().getNestedParameterType();
		}
		if (predicate.test(type)) {
			if (adapter == null) {
				return true;
			}
			throw getReactiveWrapperError(param);
		}
		return false;
	}

	private IllegalStateException getReactiveWrapperError(MethodParameter param) {
		return new IllegalStateException(getClass().getSimpleName() +
				" doesn't support reactive type wrapper: " + param.getGenericParameterType());
	}

	/**
	 * Evaluate the {@code Predicate} on the method parameter type if it has the
	 * given annotation, nesting within {@link java.util.Optional} if necessary,
	 * but raise an {@code IllegalStateException} if the same matches the generic
	 * type within a reactive type wrapper.
	 */
	protected <A extends Annotation> boolean checkAnnotatedParamNoReactiveWrapper(
			MethodParameter param, Class<A> annotationType,
			BiPredicate<A, Class<?>> typePredicate) {

		A annotation = param.getParameterAnnotation(annotationType);
		if (annotation == null) {
			return false;
		}

		param = param.nestedIfOptional();
		Class<?> type = param.getNestedParameterType();

		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(type);
		if (adapter != null) {
			assertHasValues(adapter, param);
			param = param.nested();
			type = param.getNestedParameterType();
		}

		if (typePredicate.test(annotation, type)) {
			if (adapter == null) {
				return true;
			}
			throw getReactiveWrapperError(param);
		}

		return false;
	}

}
