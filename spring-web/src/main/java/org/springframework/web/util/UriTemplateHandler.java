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

import java.net.URI;
import java.util.Map;

/**
 * Strategy for expanding a URI template.
 *
 * <p>Supported as a property on the {@code RestTemplate} as well as the
 * {@code AsyncRestTemplate}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see DefaultUriBuilderFactory
 */
public interface UriTemplateHandler {

	/**
	 * Expand the given URI template from a map of URI variables.
	 * @param uriTemplate the URI template string
	 * @param uriVariables the URI variables
	 * @return the resulting URI
	 */
	URI expand(String uriTemplate, Map<String, ?> uriVariables);

	/**
	 * Expand the given URI template from an array of URI variables.
	 * @param uriTemplate the URI template string
	 * @param uriVariables the URI variable values
	 * @return the resulting URI
	 */
	URI expand(String uriTemplate, Object... uriVariables);

}
