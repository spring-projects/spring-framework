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

import kotlinx.serialization.json.Json;

import org.springframework.http.MediaType;
import org.springframework.http.codec.KotlinSerializationStringDecoder;

/**
 * Decode a byte stream into JSON and convert to Object's with
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This decoder can be used to bind {@code @Serializable} Kotlin classes,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic serialization</a>
 * is not supported.
 * It supports {@code application/json} and {@code application/*+json} with
 * various character sets, {@code UTF-8} being the default.
 *
 * <p>Decoding streams is not supported yet, see
 * <a href="https://github.com/Kotlin/kotlinx.serialization/issues/1073">kotlinx.serialization/issues/1073</a>
 * related issue.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 * @since 5.3
 */
public class KotlinSerializationJsonDecoder extends KotlinSerializationStringDecoder<Json> {

	public KotlinSerializationJsonDecoder() {
		this(Json.Default);
	}

	public KotlinSerializationJsonDecoder(Json json) {
		super(json, MediaType.APPLICATION_JSON, new MediaType("application", "*+json"),
				MediaType.APPLICATION_NDJSON);
	}

}
