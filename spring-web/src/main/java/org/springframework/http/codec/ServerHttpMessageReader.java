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

package org.springframework.http.codec;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * Server and annotation based controller specific {@link HttpMessageReader} that allows to
 * resolve hints using annotations or request based information.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ServerHttpMessageReader<T> extends HttpMessageReader<T> {

	/**
	 * Return hints that can be used to customize how the body should be read
	 * @param streamType the original type used in the method parameter. For annotation
	 * based controllers, the {@link MethodParameter} is available via {@link ResolvableType#getSource()}.
	 * @param elementType the stream element type to return
	 * @param mediaType the media type to read, can be {@code null} if not specified.
	 * Typically the value of a {@code Content-Type} header.
	 * @param request the current HTTP request
	 * @return Additional information about how to read the body
	 */
	Map<String, Object> resolveReadHints(ResolvableType streamType, ResolvableType elementType,
			MediaType mediaType, ServerHttpRequest request);

}
