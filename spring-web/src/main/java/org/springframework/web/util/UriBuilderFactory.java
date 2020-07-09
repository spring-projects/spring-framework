/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.util;

/**
 * Factory to create {@link UriBuilder} instances with shared configuration
 * such as a base URI, an encoding mode strategy, and others across all URI
 * builder instances created through a factory.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see DefaultUriBuilderFactory
 */
public interface UriBuilderFactory extends UriTemplateHandler {

	/**
	 * Initialize a builder with the given URI template.
	 * @param uriTemplate the URI template to use
	 * @return the builder instance
	 */
	UriBuilder uriString(String uriTemplate);

	/**
	 * Create a URI builder with default settings.
	 * @return the builder instance
	 */
	UriBuilder builder();

}
