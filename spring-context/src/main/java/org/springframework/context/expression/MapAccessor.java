/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.context.expression;

import org.springframework.expression.PropertyAccessor;

/**
 * SpEL {@link PropertyAccessor} that knows how to access the keys of a standard
 * {@link java.util.Map}.
 *
 * @author Juergen Hoeller
 * @author Andy Clement
 * @since 3.0
 * @deprecated as of Spring Framework 7.0 in favor of {@link org.springframework.expression.spel.support.MapAccessor}.
 */
@Deprecated(since = "7.0", forRemoval = true)
public class MapAccessor extends org.springframework.expression.spel.support.MapAccessor {

	/**
	 * Create a new {@code MapAccessor} for reading as well as writing.
	 * @see #MapAccessor(boolean)
	 */
	public MapAccessor() {
		this(true);
	}

	/**
	 * Create a new {@code MapAccessor} for reading and possibly also writing.
	 * @param allowWrite whether to allow write operations on a target instance
	 * @since 6.2
	 * @see #canWrite
	 */
	public MapAccessor(boolean allowWrite) {
		super(allowWrite);
	}

}
