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

import java.util.Iterator;

import org.springframework.aot.hint.JavaSerializationHints;
import org.springframework.aot.hint.TypeReference;

/**
 * Serialize a {@link JavaSerializationHints} to the JSON file expected by GraalVM {@code native-image} compiler,
 * typically named {@code serialization-config.json}.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 */
class JavaSerializationHintsSerializer {

	public String serialize(JavaSerializationHints hints) {
		StringBuilder builder = new StringBuilder();
		builder.append("[\n");
		Iterator<TypeReference> typeIterator = hints.types().iterator();
		while (typeIterator.hasNext()) {
			TypeReference type = typeIterator.next();
			String name = JsonUtils.escape(type.getCanonicalName());
			builder.append("{ \"name\": \"").append(name).append("\" }");
			if (typeIterator.hasNext()) {
				builder.append(",\n");
			}
		}
		builder.append("\n]\n");
		return builder.toString();
	}

}
