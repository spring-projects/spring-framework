/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.codec.protobuf;

import kotlinx.serialization.protobuf.ProtoBuf;

import org.springframework.http.codec.KotlinSerializationBinaryEncoder;

/**
 * Decode a byte stream into a Protocol Buffer and convert to Objects with
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 * It supports {@code application/x-protobuf}, {@code application/octet-stream}, and {@code application/vnd.google.protobuf}.
 *
 * <p>As of Spring Framework 7.0,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphism</a>
 * is supported.
 *
 * <p>Decoding streams is not supported yet, see
 * <a href="https://github.com/Kotlin/kotlinx.serialization/issues/1073">kotlinx.serialization/issues/1073</a>
 * related issue.
 *
 * @author Iain Henderson
 * @since 6.0
 */
public class KotlinSerializationProtobufEncoder extends KotlinSerializationBinaryEncoder<ProtoBuf> {

	public KotlinSerializationProtobufEncoder() {
		this(ProtoBuf.Default);
	}

	public KotlinSerializationProtobufEncoder(ProtoBuf protobuf) {
		super(protobuf, ProtobufCodecSupport.MIME_TYPES);
	}
}
