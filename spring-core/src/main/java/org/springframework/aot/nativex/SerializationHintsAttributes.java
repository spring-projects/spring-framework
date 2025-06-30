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

package org.springframework.aot.nativex;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.aot.hint.ConditionalHint;
import org.springframework.aot.hint.JavaSerializationHint;
import org.springframework.aot.hint.SerializationHints;

/**
 * Collect {@link SerializationHints} as map attributes ready for JSON serialization for the GraalVM
 * {@code native-image} compiler.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @see <a href="https://www.graalvm.org/jdk23/reference-manual/native-image/overview/BuildConfiguration/">Native Image Build Configuration</a>
 */
class SerializationHintsAttributes {

	private static final Comparator<JavaSerializationHint> JAVA_SERIALIZATION_HINT_COMPARATOR =
			Comparator.comparing(JavaSerializationHint::getType);

	public List<Map<String, Object>> toAttributes(SerializationHints hints) {
		return hints.javaSerializationHints()
				.sorted(JAVA_SERIALIZATION_HINT_COMPARATOR)
				.map(this::toAttributes).toList();
	}

	private Map<String, Object> toAttributes(JavaSerializationHint serializationHint) {
		LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
		handleCondition(attributes, serializationHint);
		attributes.put("type", serializationHint.getType());
		return attributes;
	}

	private void handleCondition(Map<String, Object> attributes, ConditionalHint hint) {
		if (hint.getReachableType() != null) {
			Map<String, Object> conditionAttributes = new LinkedHashMap<>();
			conditionAttributes.put("typeReached", hint.getReachableType());
			attributes.put("condition", conditionAttributes);
		}
	}

}
