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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

import rx.Single;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;


/**
 * Manages the {@link CacheResultWrapper} instances to apply only the appropriate.
 * @author Pablo Diaz-Lopez
 */
public class CacheResultWrapperManager {
	private Map<Class<?>, CacheResultWrapper> unwrapperByClass;

	public CacheResultWrapperManager() {
		unwrapperByClass = new HashMap<>();

		unwrapperByClass.put(Optional.class, new OptionalUnWrapper());
		registerReactiveWrappers();
	}

	private void registerReactiveWrappers() {
		if(tryRegisterResultWrapper("reactor.core.publisher.Mono", MonoReactiveWrapper::new)) {
			ReactiveAdapterRegistry adapterRegistry = new ReactiveAdapterRegistry();

			tryRegisterResultWrapper("rx.Single", () -> new SingleReactiveWrapper(adapterRegistry));
		}
	}

	private boolean tryRegisterResultWrapper(String className, Supplier<CacheResultWrapper> cacheResultWrapperSupplier) {
		try {
			Class<?> clazz = ClassUtils.forName(className, CacheAspectSupport.class.getClassLoader());
			CacheResultWrapper cacheResultWrapper = cacheResultWrapperSupplier.get();
			unwrapperByClass.put(clazz, cacheResultWrapper);
			return true;
		} catch (ClassNotFoundException e) {
			// Cannot register wrapper
			return false;
		}
	}

	/**
	 * Wraps a value
	 *
	 * @param clazz the target class wanted
	 * @param value the value to be wrapped
	 * @return the value wrapped if it can, or the same value if it cannot handle it
	 */
	public Object wrap(Class<?> clazz, Object value) {
		if (value != null) {
			CacheResultWrapper unwrapper = unwrapperByClass.get(clazz);

			if (unwrapper != null) {
				return unwrapper.wrap(value);
			}
		}

		return value;
	}

	/**
	 * Unwraps the value asynchronously
	 *
	 * @param valueWrapped the value wrapped to be unwrapped
	 * @param asyncResult  where the result will be notified
	 * @return the same value wrapped or decorated in order to notify when it finish.
	 */
	public Object asyncUnwrap(Object valueWrapped, Class<?> classWrapped, AsyncWrapResult asyncResult) {
		if (valueWrapped != null) {
			CacheResultWrapper unwrapper = unwrapperByClass.get(classWrapped);

			if (unwrapper != null) {
				return unwrapper.notifyResult(valueWrapped, asyncResult);
			}
		}

		asyncResult.complete(valueWrapped);

		return valueWrapped;
	}

	private class SingleReactiveWrapper extends MonoReactiveAdapterWrapper {
		public SingleReactiveWrapper(ReactiveAdapterRegistry registry) {
			super(registry, Single.class);
		}
	}

	private class MonoReactiveWrapper implements CacheResultWrapper {
		@Override
		public Mono<?> wrap(Object value) {
			return Mono.justOrEmpty(value);
		}

		@Override
		public Mono<?> notifyResult(Object objectWrapped, AsyncWrapResult asyncResult) {
			Mono<?> monoWrapped = (Mono<?>) objectWrapped;

			return monoWrapped
					.doOnSuccess(asyncResult::complete)
					.doOnError(asyncResult::error);
		}
	}


	private abstract class MonoReactiveAdapterWrapper implements CacheResultWrapper {
		private ReactiveAdapter adapter;
		private MonoReactiveWrapper monoReactiveWrapper;

		MonoReactiveAdapterWrapper(ReactiveAdapterRegistry registry, Class<?> wrapperClass) {
			this.adapter = registry.getAdapterFrom(wrapperClass);
			this.monoReactiveWrapper = new MonoReactiveWrapper();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object wrap(Object value) {
			return adapter.fromPublisher(monoReactiveWrapper.wrap(value));
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object notifyResult(Object valueWrapped, AsyncWrapResult asyncResult) {
			Mono<?> monoWrapped = adapter.toMono(valueWrapped);
			Mono<?> monoCacheWrapped = monoReactiveWrapper.notifyResult(monoWrapped, asyncResult);

			return adapter.fromPublisher(monoCacheWrapped);
		}
	}

	/**
	 * Inner class to avoid a hard dependency on Java 8.
	 */
	private class OptionalUnWrapper implements CacheResultWrapper {

		@Override
		public Optional<?> notifyResult(Object optionalObject, AsyncWrapResult asyncResult) {
			Optional<?> optional = (Optional<?>) optionalObject;

			Object value = ObjectUtils.unwrapOptional(optional);

			asyncResult.complete(value);

			return optional;
		}

		@Override
		public Optional<?> wrap(Object value) {
			return Optional.ofNullable(value);
		}
	}
}
