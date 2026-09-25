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

package org.springframework.beans.factory;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

/**
 * A {@code bean} to locate an {@code Adapter} based on the method called and
 * the parameters passed in.
 *
 * <p>For use when multiple implementations of an {@code interface} (Adapters) exist
 * to handle functionality in various ways such as sending and monitoring shipments
 * from different providers.  The {@link AdapterFinderBean} can look at the method
 * parameters and determine which shipping provider {@code Adapter} to use.
 *
 * <p>If the {@code AdapterFinderBean} cannot find an implementation appropriate for
 * the parameters, then it will return {@code null}.
 *
 * @author Joe Chambers
 * @param <T> the service type the finder returns
 */
public interface AdapterFinderBean<T> {

	/**
	 * Lookup the adapter appropriate for the {@link Method} and {@code Arguments}
	 * passed to the implementation.
	 * @param method the {@link Method} being called
	 * @param args the {@code Arguments} being passed to the invocation
	 * @return the implementation of the {@code Adapter} that is appropriate or {@code null}
	 */
	@Nullable
	T findAdapter(Method method, @Nullable Object[] args);
}
