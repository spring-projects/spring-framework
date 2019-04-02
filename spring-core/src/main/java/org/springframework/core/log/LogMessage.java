/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.log;

import java.util.function.Supplier;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A simple log message type for use with Commons Logging, allowing
 * for convenient late resolution of a given {@link Supplier} instance
 * (typically bound to a Java 8 lambda expression) in {@link #toString()}.
 *
 * @author Juergen Hoeller
 * @since 5.2
 * @see org.apache.commons.logging.Log#fatal(Object)
 * @see org.apache.commons.logging.Log#error(Object)
 * @see org.apache.commons.logging.Log#warn(Object)
 * @see org.apache.commons.logging.Log#info(Object)
 * @see org.apache.commons.logging.Log#debug(Object)
 * @see org.apache.commons.logging.Log#trace(Object)
 */
public class LogMessage {

	private final Supplier<? extends CharSequence> supplier;

	@Nullable
	private String result;


	/**
	 * Construct a new {@code LogMessage} for the given supplier.
	 * @param supplier the lazily resolving supplier
	 * (typically bound to a Java 8 lambda expression)
	 */
	public LogMessage(Supplier<? extends CharSequence> supplier) {
		Assert.notNull(supplier, "Supplier must not be null");
		this.supplier = supplier;
	}


	/**
	 * This will be called by the logging provider, potentially once
	 * per log target (therefore locally caching the result here).
	 */
	@Override
	public String toString() {
		if (this.result == null) {
			this.result = this.supplier.get().toString();
		}
		return this.result;
	}

}
