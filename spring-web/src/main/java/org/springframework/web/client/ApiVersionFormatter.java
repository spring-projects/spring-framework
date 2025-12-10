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

package org.springframework.web.client;

/**
 * Contract to format the API version for a request.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @see DefaultApiVersionInserter.Builder#withVersionFormatter(ApiVersionFormatter)
 */
@FunctionalInterface
public interface ApiVersionFormatter {

	/**
	 * Format the given version Object into a String value.
	 * @param version the version to format
	 * @return the final String version to use
	 */
	String formatVersion(Object version);

}
