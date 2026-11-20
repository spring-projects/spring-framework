/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * @author Stephane Nicoll
 * @since 7.0
 * @see <a href="https://www.graalvm.org/jdk25/reference-manual/native-image/metadata">GraalVM Reachability Metadata</a>
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

		writer.writeObject(document);
	}

}
