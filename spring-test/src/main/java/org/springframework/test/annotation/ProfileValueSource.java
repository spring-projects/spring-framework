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

package org.springframework.test.annotation;

/**
 * <p>
 * Strategy interface for retrieving <em>profile values</em> for a given
 * testing environment.
 * </p>
 * <p>
 * Concrete implementations must provide a {@code public} no-args
 * constructor.
 * </p>
 * <p>
 * Spring provides the following out-of-the-box implementations:
 * </p>
 * <ul>
 * <li>{@link SystemProfileValueSource}</li>
 * </ul>
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 * @see ProfileValueSourceConfiguration
 * @see IfProfileValue
 * @see ProfileValueUtils
 */
public interface ProfileValueSource {

	/**
	 * Get the <em>profile value</em> indicated by the specified key.
	 * @param key the name of the <em>profile value</em>
	 * @return the String value of the <em>profile value</em>, or {@code null}
	 * if there is no <em>profile value</em> with that key
	 */
	String get(String key);

}
