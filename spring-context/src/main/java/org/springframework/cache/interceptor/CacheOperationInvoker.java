/*
 * Copyright 2002-2014 the original author or authors.
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

/**
 * Abstract the invocation of a cache operation.
 *
 * <p>Provide a special exception that can be used to indicate that the
 * underlying invocation has thrown a checked exception, allowing the
 * callers to threat these in a different manner if necessary.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface CacheOperationInvoker {

	/**
	 * Invoke the cache operation defined by this instance. Can throw a
	 * {@link ThrowableWrapper} if that operation wants to explicitly
	 * indicate that a checked exception has occurred.
	 * @return the result of the operation
	 * @throws ThrowableWrapper if a checked exception has been thrown
	 */
	Object invoke() throws ThrowableWrapper;

	/**
	 * Wrap any exception thrown while invoking {@link #invoke()}
	 */
	@SuppressWarnings("serial")
	public static class ThrowableWrapper extends RuntimeException {

		private final Throwable original;

		public ThrowableWrapper(Throwable original) {
			super(original.getMessage(), original);
			this.original = original;
		}

		public Throwable getOriginal() {
			return original;
		}
	}

}
