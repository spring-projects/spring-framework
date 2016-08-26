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

package org.springframework.core.codec;

import java.util.Arrays;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MimeType;

/**
 * Abstract base class for {@link Decoder} implementations.
 * <p> Decoder接口的虚拟实现类
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class AbstractDecoder<T> implements Decoder<T> {

	private final List<MimeType> decodableMimeTypes;


	protected AbstractDecoder(MimeType... supportedMimeTypes) {
		this.decodableMimeTypes = Arrays.asList(supportedMimeTypes);
	}


	@Override
	public List<MimeType> getDecodableMimeTypes() {
		return this.decodableMimeTypes;
	}

	@Override
	public boolean canDecode(ResolvableType elementType, MimeType mimeType, Object... hints) {
		if (mimeType == null) {
			return true;
		}
		return this.decodableMimeTypes.stream().anyMatch(candidate -> candidate.isCompatibleWith(mimeType));
	}

	@Override
	public Mono<T> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			MimeType mimeType, Object... hints) {

		throw new UnsupportedOperationException();
	}

}
