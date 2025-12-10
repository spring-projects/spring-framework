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

package org.springframework.web.accept;

/**
 * Contract to parse a version into an Object representation.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <V> the parsed object type
 */
@FunctionalInterface
public interface ApiVersionParser<V extends Comparable<V>> {

	/**
	 * Parse the version into an Object.
	 * @param version the value to parse
	 * @return an Object that represents the version
	 */
	V parseVersion(String version);

}
