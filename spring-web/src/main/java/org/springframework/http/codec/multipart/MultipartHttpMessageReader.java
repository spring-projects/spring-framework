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

package org.springframework.http.codec.multipart;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.util.MultiValueMap;

/**
 * Interface for reading multipart HTML forms with {@code "multipart/form-data"} media
 * type in accordance with <a href="https://tools.ietf.org/html/rfc7578">RFC 7578</a>.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface MultipartHttpMessageReader extends HttpMessageReader<MultiValueMap<String, Part>> {

	ResolvableType MULTIPART_VALUE_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class);

	@Override
	default List<MediaType> getReadableMediaTypes() {
		return Collections.singletonList(MediaType.MULTIPART_FORM_DATA);
	}

	@Override
	default boolean canRead(ResolvableType elementType, MediaType mediaType) {
		return MULTIPART_VALUE_TYPE.isAssignableFrom(elementType) &&
				(mediaType == null || MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType));
	}

	@Override
	default Flux<MultiValueMap<String, Part>> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
		return Flux.from(readMono(elementType, message, hints));
	}
}
