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

package org.springframework.http.converter.protobuf;

import kotlinx.serialization.protobuf.ProtoBuf;

import org.springframework.http.MediaType;
import org.springframework.http.converter.KotlinSerializationBinaryHttpMessageConverter;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter}
 * that can read and write Protocol Buffers using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This converter can be used to bind {@code @Serializable} Kotlin classes,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic serialization</a>
 * is not supported.
 * It supports {@code application/x-protobuf}, {@code application/octet-stream}, and {@code application/vnd.google.protobuf}.
 *
 * @author Iain Henderson
 * @since 6.0
 */
public class KotlinSerializationProtobufHttpMessageConverter extends
		KotlinSerializationBinaryHttpMessageConverter<ProtoBuf> {

	public KotlinSerializationProtobufHttpMessageConverter() {
		this(ProtoBuf.Default);
	}

	public KotlinSerializationProtobufHttpMessageConverter(ProtoBuf protobuf) {
		super(protobuf, MediaType.APPLICATION_PROTOBUF, MediaType.APPLICATION_OCTET_STREAM,
				new MediaType("application", "vnd.google.protobuf"));
	}

}
