/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.util.function;

import java.util.function.Supplier;

import org.springframework.lang.Nullable;

/**
 * Convenience utilities for {@link java.util.function.Supplier} handling.
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @see SingletonSupplier
 */
public abstract class SupplierUtils {

	/**
	 * Resolve the given {@code Supplier}, getting its result or immediately
	 * returning {@code null} if the supplier itself was {@code null}.
	 * @param supplier the supplier to resolve
	 * @return the supplier's result, or {@code null} if none
	 */
	@Nullable
	public static <T> T resolve(@Nullable Supplier<T> supplier) {
		return (supplier != null ? supplier.get() : null);
	}

}
