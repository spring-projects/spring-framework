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
import org.springframework.aot.hint.ResourceBundleHint;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.ResourcePatternHint;
import org.springframework.aot.hint.ResourcePatternHints;

/**
 * Collect {@link ResourceHints} as map attributes ready for JSON serialization for the GraalVM
 * {@code native-image} compiler.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.0
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/Resources/">Accessing Resources in Native Images</a>
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 */
class ResourceHintsAttributes {

	private static final Comparator<ResourcePatternHint> RESOURCE_PATTERN_HINT_COMPARATOR =
			Comparator.comparing(ResourcePatternHint::getPattern);

	private static final Comparator<ResourceBundleHint> RESOURCE_BUNDLE_HINT_COMPARATOR =
			Comparator.comparing(ResourceBundleHint::getBaseName);


	public List<Map<String, Object>> resources(ResourceHints hint) {
		return hint.resourcePatternHints()
				.map(ResourcePatternHints::getIncludes).flatMap(List::stream).distinct()
				.sorted(RESOURCE_PATTERN_HINT_COMPARATOR)
				.map(this::toAttributes).toList();
	}

	public List<Map<String, Object>> resourceBundles(ResourceHints hint) {
		return hint.resourceBundleHints()
				.sorted(RESOURCE_BUNDLE_HINT_COMPARATOR)
				.map(this::toAttributes).toList();
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
		attributes.put("glob", hint.getPattern());
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
