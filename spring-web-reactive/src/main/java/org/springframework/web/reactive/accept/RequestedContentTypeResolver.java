/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.reactive.accept;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Strategy for resolving the requested media types for a {@code ServerWebExchange}.
 *
 * @author Rossen Stoyanchev
 */
public interface RequestedContentTypeResolver {

	/**
	 * Resolve the given request to a list of requested media types. The returned
	 * list is ordered by specificity first and by quality parameter second.
	 *
	 * @param exchange the current exchange
	 * @return the requested media types or an empty list
	 *
	 * @throws NotAcceptableStatusException if the requested media types is invalid
	 */
	List<MediaType> resolveMediaTypes(ServerWebExchange exchange)
			throws NotAcceptableStatusException;

}
