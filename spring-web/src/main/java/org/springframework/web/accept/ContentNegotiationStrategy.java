/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * A strategy for resolving the requested media types for a request.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
@FunctionalInterface
public interface ContentNegotiationStrategy {

	/**
	 * A singleton list with {@link MediaType#ALL} that is returned from
	 * {@link #resolveMediaTypes} when no specific media types are requested.
	 * @since 5.0.5
	 */
	List<MediaType> MEDIA_TYPE_ALL_LIST = Collections.singletonList(MediaType.ALL);


	/**
	 * Resolve the given request to a list of media types. The returned list is
	 * ordered by specificity first and by quality parameter second.
	 * @param webRequest the current request
	 * @return the requested media types, or {@link #MEDIA_TYPE_ALL_LIST} if none
	 * were requested.
	 * @throws HttpMediaTypeNotAcceptableException if the requested media
	 * types cannot be parsed
	 */
	List<MediaType> resolveMediaTypes(NativeWebRequest webRequest)
			throws HttpMediaTypeNotAcceptableException;

}
