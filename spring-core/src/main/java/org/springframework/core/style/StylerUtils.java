/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.style;

/**
 * Simple utility class to allow for convenient access to value
 * styling logic, mainly to support descriptive logging messages.
 *
 * <p>For more sophisticated needs, use the {@link ValueStyler} abstraction
 * directly. This class simply uses a shared {@link DefaultValueStyler}
 * instance underneath.
 *
 * @author Keith Donald
 * @since 1.2.2
 * @see ValueStyler
 * @see DefaultValueStyler
 */
public abstract class StylerUtils {

	/**
	 * Default ValueStyler instance used by the {@code style} method.
	 * Also available for the {@link ToStringCreator} class in this package.
	 */
	static final ValueStyler DEFAULT_VALUE_STYLER = new DefaultValueStyler();

	/**
	 * Style the specified value according to default conventions.
	 * @param value the Object value to style
	 * @return the styled String
	 * @see DefaultValueStyler
	 */
	public static String style(Object value) {
		return DEFAULT_VALUE_STYLER.style(value);
	}

}
