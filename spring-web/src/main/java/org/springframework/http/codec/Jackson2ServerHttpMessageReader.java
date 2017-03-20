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

package org.springframework.http.codec;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.http.codec.json.AbstractJackson2Codec;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * {@link ServerHttpMessageReader} that resolves those annotation or request based Jackson 2 hints:
 * <ul>
 *   <li>{@code @JsonView} + {@code @RequestBody} annotated handler method parameter</li>
 * </ul>
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see com.fasterxml.jackson.annotation.JsonView
 */
public class Jackson2ServerHttpMessageReader extends DecoderHttpMessageReader<Object> {


	public Jackson2ServerHttpMessageReader(Decoder<Object> decoder) {
		super(decoder);
	}


	@Override
	protected Map<String, Object> resolveReadHints(ResolvableType streamType,
			ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response) {

		Object source = streamType.getSource();
		MethodParameter parameter = (source instanceof MethodParameter ? (MethodParameter)source : null);
		if (parameter != null) {
			JsonView annotation = parameter.getParameterAnnotation(JsonView.class);
			if (annotation != null) {
				Class<?>[] classes = annotation.value();
				if (classes.length != 1) {
					throw new IllegalArgumentException(
							"@JsonView only supported for read hints with exactly 1 class argument: " + parameter);
				}
				return Collections.singletonMap(AbstractJackson2Codec.JSON_VIEW_HINT, classes[0]);
			}
		}
		return Collections.emptyMap();
	}

}
