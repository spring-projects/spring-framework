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
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.util.MimeType;

/**
 * Abstract base class for {@link Decoder} implementations.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class AbstractEncoder<T> implements Encoder<T> {

	private final List<MimeType> encodableMimeTypes;


	protected AbstractEncoder(MimeType... supportedMimeTypes) {
		this.encodableMimeTypes = Arrays.asList(supportedMimeTypes);
	}


	@Override
	public List<MimeType> getEncodableMimeTypes() {
		return this.encodableMimeTypes;
	}

	@Override
	public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
		if (mimeType == null) {
			return true;
		}
		return this.encodableMimeTypes.stream().anyMatch(candidate -> candidate.isCompatibleWith(mimeType));
	}

}
