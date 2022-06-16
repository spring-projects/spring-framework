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

package org.springframework.aot.nativex;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.aot.hint.JavaSerializationHints;
import org.springframework.aot.hint.TypeReference;

/**
 * Write a {@link JavaSerializationHints} to the JSON output expected by the
 * GraalVM {@code native-image} compiler, typically named
 * {@code serialization-config.json}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @since 6.0
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 */
class JavaSerializationHintsWriter {

	public static final JavaSerializationHintsWriter INSTANCE = new JavaSerializationHintsWriter();

	public void write(BasicJsonWriter writer, JavaSerializationHints hints) {
		writer.writeArray(hints.types().map(this::toAttributes).toList());
	}

	private Map<String, Object> toAttributes(TypeReference typeReference) {
		LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("name", typeReference);
		return attributes;
	}

}
