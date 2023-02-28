/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.codec.cbor;

import kotlinx.serialization.cbor.Cbor;

import org.springframework.http.MediaType;
import org.springframework.http.codec.KotlinSerializationBinaryDecoder;

/**
 * Decode a byte stream into CBOR and convert to Objects with
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This decoder can be used to bind {@code @Serializable} Kotlin classes,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic serialization</a>
 * is not supported.
 * It supports {@code application/cbor}.
 *
 * <p>Decoding streams is not supported yet, see
 * <a href="https://github.com/Kotlin/kotlinx.serialization/issues/1073">kotlinx.serialization/issues/1073</a>
 * related issue.
 *
 * @author Iain Henderson
 * @since 6.0
 */
public class KotlinSerializationCborDecoder extends KotlinSerializationBinaryDecoder<Cbor> {

	public KotlinSerializationCborDecoder() {
		this(Cbor.Default);
	}

	public KotlinSerializationCborDecoder(Cbor cbor) {
		super(cbor, MediaType.APPLICATION_CBOR);
	}
}
