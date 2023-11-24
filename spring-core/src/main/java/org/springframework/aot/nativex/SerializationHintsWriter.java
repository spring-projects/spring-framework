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

import org.springframework.aot.hint.ConditionalHint;
import org.springframework.aot.hint.JavaSerializationHint;
import org.springframework.aot.hint.SerializationHints;

/**
 * Write a {@link SerializationHints} to the JSON output expected by the
 * GraalVM {@code native-image} compiler, typically named
 * {@code serialization-config.json}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.0
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 */
class SerializationHintsWriter {

	public static final SerializationHintsWriter INSTANCE = new SerializationHintsWriter();

	public void write(BasicJsonWriter writer, SerializationHints hints) {
		writer.writeArray(hints.javaSerializationHints().map(this::toAttributes).toList());
	}

	private Map<String, Object> toAttributes(JavaSerializationHint serializationHint) {
		LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
		handleCondition(attributes, serializationHint);
		attributes.put("name", serializationHint.getType());
		return attributes;
	}

	private void handleCondition(Map<String, Object> attributes, ConditionalHint hint) {
		if (hint.getReachableType() != null) {
			Map<String, Object> conditionAttributes = new LinkedHashMap<>();
			conditionAttributes.put("typeReachable", hint.getReachableType());
			attributes.put("condition", conditionAttributes);
		}
	}

}
