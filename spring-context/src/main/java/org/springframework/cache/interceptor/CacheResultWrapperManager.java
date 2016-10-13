/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.cache.interceptor;


import java.util.*;
import java.util.function.Consumer;

import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.ClassUtils;


/**
 * Manages the {@link ReactiveResultWrapper} instances to apply only the appropriate.
 * @author Pablo Diaz-Lopez
 */
public class CacheResultWrapperManager {
	private ReactiveResultWrapper reactiveResultWrapper;

	public CacheResultWrapperManager() {
		tryRegisterReactiveAdapterResultWrapper();
	}

	private void tryRegisterReactiveAdapterResultWrapper() {
		boolean monoIsPresent = ClassUtils.isPresent("reactor.core.publisher.Mono", CacheAspectSupport.class.getClassLoader());

		if (monoIsPresent) {
			reactiveResultWrapper = new ReactiveAdapterResultWrapper();
		} else {
			reactiveResultWrapper = new DoNothingReactiveResultWrapper();
		}
	}

	/**
	 * Wraps a value
	 *
	 * @param value the value to be wrapped
	 * @param clazz the target class wanted
	 * @return the value wrapped if it can, or the same value if it cannot handle it
	 */
	public Object wrap(Object value, Class<?> clazz) {
		if (clazz.equals(Optional.class)) {
			return Optional.ofNullable(value);
		}

		return reactiveResultWrapper.tryToWrap(value, clazz);
	}

	/**
	 * Unwraps the value asynchronously, if it can (or needs to) be unwrapped
	 *
	 * @param value the value wrapped to be unwrapped
	 * @param asyncResult  where the result will be notified
	 * @return the same value wrapped or decorated in order to notify when it finish.
	 */
	public Object asyncUnwrap(Object value, Class<?> clazz, Consumer<Object> asyncResult) {
		if (clazz.equals(Optional.class)) {
			asyncResult.accept(ObjectUtils.unwrapOptional(value));

			return value;
		}

		return reactiveResultWrapper.notifyResult(value, clazz, asyncResult);
	}

	/**
	 * Wrapper/Unwrapper, it allows to notifyResult values to be cached and wraps it back
	 * in order to be consumed.
	 * @author Pablo Diaz-Lopez
	 */
	interface ReactiveResultWrapper {
		/**
		 * Wraps to the wrapping target class
		 * @param value the value to wrap
		 * @return the value wrapped
		 */
		Object tryToWrap(Object value, Class<?> clazz);

		/**
		 * Unwraps a value and returns it decorated if it needs in order to
		 * notify the result, this will be the case if the wrapped value is not
		 * available at the moment (it is calculated asynchronously)
		 * @param value the value wrapped
		 * @param clazz class to be wrapped into
		 * @param asyncResult it will call it when the value it's available
		 * @return the same value wrapped or a version decorated.
		 */
		Object notifyResult(Object value, Class<?> clazz, Consumer<Object> asyncResult);

	}

	private class DoNothingReactiveResultWrapper implements ReactiveResultWrapper {
		@Override
		public Object tryToWrap(Object value, Class<?> clazz) {
			return value;
		}

		@Override
		public Object notifyResult(Object value, Class<?> valueClass, Consumer<Object> asyncResult) {
			asyncResult.accept(value);
			return value;
		}
	}

	private class ReactiveAdapterResultWrapper implements ReactiveResultWrapper {
		private ReactiveAdapterRegistry registry;

		public ReactiveAdapterResultWrapper() {
			this.registry = new ReactiveAdapterRegistry();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object tryToWrap(Object value, Class<?> clazz) {
			ReactiveAdapter adapterToWrapped = registry.getAdapterTo(clazz);

			if (isValid(adapterToWrapped)) {
				Mono<?> monoWrapped = Mono.justOrEmpty(value);
				return adapterToWrapped.fromPublisher(monoWrapped);

			} else {
				return value;
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object notifyResult(Object value, Class<?> valueClass, Consumer<Object> asyncResult) {
			ReactiveAdapter adapter = registry.getAdapterFrom(valueClass);

			if (isValid(adapter)) {
				Mono<?> monoWrapped = adapter.toMono(value)
						.doOnSuccess(asyncResult);

				return adapter.fromPublisher(monoWrapped);
			} else {
				asyncResult.accept(value);

				return value;
			}

		}

		private boolean isValid(ReactiveAdapter adapter) {
			return adapter != null && !adapter.getDescriptor().isMultiValue();
		}
	}
}
