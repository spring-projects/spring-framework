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

package org.springframework.aot.nativex;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.core.SpringVersion;

/**
 * Write a {@link RuntimeHints} instance to the JSON output expected by the
 * GraalVM {@code native-image} compiler, typically named {@code reachability-metadata.json}.
 *
 * @author Brian Clozel
 * @since 7.0
 * @see <a href="https://www.graalvm.org/jdk23/reference-manual/native-image/metadata/#specifying-metadata-with-json">GraalVM Reachability Metadata</a>
 */
class RuntimeHintsWriter {

	public void write(BasicJsonWriter writer, RuntimeHints hints) {
		Map<String, Object> document = new LinkedHashMap<>();
		String springVersion = SpringVersion.getVersion();
		if (springVersion != null) {
			document.put("comment", "Spring Framework " + springVersion);
		}
		List<Map<String, Object>> reflection = new ReflectionHintsAttributes().reflection(hints);
		if (!reflection.isEmpty()) {
			document.put("reflection", reflection);
		}
		List<Map<String, Object>> jni = new ReflectionHintsAttributes().jni(hints);
		if (!jni.isEmpty()) {
			document.put("jni", jni);
		}
		List<Map<String, Object>> resourceHints = new ResourceHintsAttributes().resources(hints.resources());
		if (!resourceHints.isEmpty()) {
			document.put("resources", resourceHints);
		}
		List<Map<String, Object>> resourceBundles = new ResourceHintsAttributes().resourceBundles(hints.resources());
		if (!resourceBundles.isEmpty()) {
			document.put("bundles", resourceBundles);
		}
		List<Map<String, Object>> serialization = new SerializationHintsAttributes().toAttributes(hints.serialization());
		if (!serialization.isEmpty()) {
			document.put("serialization", serialization);
		}

		writer.writeObject(document);
	}

}
