/*
 * Copyright 2002-2015 the original author or authors.
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
 * A strategy for expanding a URI template with URI variables into a {@link URI}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface UriTemplateHandler {

	/**
	 * Expand the give URI template with a map of URI variables.
	 * @param uriTemplate the URI template string
	 * @param uriVariables the URI variables
	 * @return the resulting URI
	 */
	URI expand(String uriTemplate, Map<String, ?> uriVariables);

	/**
	 * Expand the give URI template with an array of URI variable values.
	 * @param uriTemplate the URI template string
	 * @param uriVariableValues the URI variable values
	 * @return the resulting URI
	 */
	URI expand(String uriTemplate, Object... uriVariableValues);

}
