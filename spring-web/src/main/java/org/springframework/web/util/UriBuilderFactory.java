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
 * Factory for instances of {@link UriBuilder}.
 *
 * <p>A single {@link UriBuilderFactory} may be created once, configured with
 * common properties such as a base URI, and then used to create many URIs.
 *
 * <p>Extends {@link UriTemplateHandler} which has a similar purpose but only
 * provides shortcuts for expanding URI templates, not builder style methods.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface UriBuilderFactory extends UriTemplateHandler {

	/**
	 * Return a builder that is initialized with the given URI string which may
	 * be a URI template and represent full URI or just a path.
	 * <p>Depending on the factory implementation and configuration, the builder
	 * may merge the given URI string with a base URI and apply other operations.
	 * Refer to the specific factory implementation for details.
	 * @param uriTemplate the URI template to create the builder with
	 * @return the UriBuilder
	 */
	UriBuilder uriString(String uriTemplate);

}
