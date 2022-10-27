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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.aot.hint.ConditionalHint;
import org.springframework.aot.hint.ResourceBundleHint;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.ResourcePatternHint;
import org.springframework.aot.hint.ResourcePatternHints;
import org.springframework.lang.Nullable;

/**
 * Write a {@link ResourceHints} to the JSON output expected by the GraalVM
 * {@code native-image} compiler, typically named {@code resource-config.json}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.0
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/Resources/">Accessing Resources in Native Images</a>
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 */
class ResourceHintsWriter {

	public static final ResourceHintsWriter INSTANCE = new ResourceHintsWriter();

	public void write(BasicJsonWriter writer, ResourceHints hints) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		addIfNotEmpty(attributes, "resources", toAttributes(hints));
		handleResourceBundles(attributes, hints.resourceBundleHints());
		writer.writeObject(attributes);
	}

	private Map<String, Object> toAttributes(ResourceHints hint) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		addIfNotEmpty(attributes, "includes", hint.resourcePatternHints().map(ResourcePatternHints::getIncludes)
				.flatMap(List::stream).distinct().map(this::toAttributes).toList());
		addIfNotEmpty(attributes, "excludes", hint.resourcePatternHints().map(ResourcePatternHints::getExcludes)
				.flatMap(List::stream).distinct().map(this::toAttributes).toList());
		return attributes;
	}

	private void handleResourceBundles(Map<String, Object> attributes, Stream<ResourceBundleHint> ressourceBundles) {
		addIfNotEmpty(attributes, "bundles", ressourceBundles.map(this::toAttributes).toList());
	}

	private Map<String, Object> toAttributes(ResourceBundleHint hint) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		handleCondition(attributes, hint);
		attributes.put("name", hint.getBaseName());
		return attributes;
	}

	private Map<String, Object> toAttributes(ResourcePatternHint hint) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		handleCondition(attributes, hint);
		attributes.put("pattern", hint.toRegex().toString());
		return attributes;
	}

	private void addIfNotEmpty(Map<String, Object> attributes, String name, @Nullable Object value) {
		if (value instanceof Collection<?> collection) {
			if (!collection.isEmpty()) {
				attributes.put(name, value);
			}
		}
		else if (value instanceof Map<?, ?> map) {
			if (!map.isEmpty()) {
				attributes.put(name, value);
			}
		}
		else if (value != null) {
			attributes.put(name, value);
		}
	}

	private void handleCondition(Map<String, Object> attributes, ConditionalHint hint) {
		if (hint.getReachableType() != null) {
			Map<String, Object> conditionAttributes = new LinkedHashMap<>();
			conditionAttributes.put("typeReachable", hint.getReachableType());
			attributes.put("condition", conditionAttributes);
		}
	}

}
