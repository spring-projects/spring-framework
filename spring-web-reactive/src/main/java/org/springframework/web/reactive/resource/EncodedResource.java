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

package org.springframework.web.reactive.resource;

import org.springframework.core.io.Resource;

/**
 * Interface for a resource descriptor that describes the encoding
 * applied to the entire resource content.
 *
 * <p>This information is required if the client consuming that resource
 * needs additional decoding capabilities to retrieve the resource's content.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see <a href="http://tools.ietf.org/html/rfc7231#section-3.1.2.2">
 *     HTTP/1.1: Semantics and Content, section 3.1.2.2</a>
 */
public interface EncodedResource extends Resource {

	/**
	 * The content coding value, as defined in the IANA registry
	 * @return the content encoding
	 * @see <a href="http://tools.ietf.org/html/rfc7231#section-3.1.2.1">HTTP/1.1: Semantics
	 * and Content, section 3.1.2.1</a>
	 */
	String getContentEncoding();

}
