/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.http.codec.json;

import java.util.List;

import kotlinx.serialization.json.Json;

import org.springframework.http.MediaType;
import org.springframework.http.codec.KotlinSerializationStringEncoder;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This encoder can be used to bind {@code @Serializable} Kotlin classes,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic serialization</a>
 * is not supported.
 * It supports {@code application/json}, {@code application/x-ndjson} and {@code application/*+json} with
 * various character sets, {@code UTF-8} being the default.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 * @since 5.3
 */
public class KotlinSerializationJsonEncoder extends KotlinSerializationStringEncoder<Json> {

	public KotlinSerializationJsonEncoder() {
		this(Json.Default);
	}

	public KotlinSerializationJsonEncoder(Json json) {
		super(json, MediaType.APPLICATION_JSON, new MediaType("application", "*+json"),
				MediaType.APPLICATION_NDJSON);
		setStreamingMediaTypes(List.of(MediaType.APPLICATION_NDJSON));
	}

}
