/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.web.util;

/**
 * Factory to create {@link UriBuilder} instances pre-configured in a specific
 * way such as sharing a common base URI across all builders.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface UriBuilderFactory extends UriTemplateHandler {

	/**
	 * Create a builder from the given URI template string.
	 * Implementations may further combine the URI template with a base URI.
	 * @param uriTemplate the URI template to use
	 * @return the builder instance
	 */
	UriBuilder uriString(String uriTemplate);

	/**
	 * Create a builder with default settings.
	 * @return the builder instance
	 */
	UriBuilder builder();

}
