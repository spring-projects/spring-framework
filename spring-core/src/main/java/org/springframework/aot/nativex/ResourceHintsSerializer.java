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

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.aot.hint.ResourceBundleHint;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.ResourcePatternHint;

/**
 * Serialize a {@link ResourceHints} to the JSON file expected by GraalVM {@code native-image} compiler,
 * typically named {@code resource-config.json}.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/Resources/">Accessing Resources in Native Images</a>
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 */
class ResourceHintsSerializer {

	public String serialize(ResourceHints hints) {
		StringBuilder builder = new StringBuilder();
		builder.append("{\n\"resources\" : {\n");
		serializeInclude(hints, builder);
		serializeExclude(hints, builder);
		builder.append("},\n");
		serializeBundles(hints, builder);
		builder.append("}\n");
		return builder.toString();
	}

	private void serializeInclude(ResourceHints hints, StringBuilder builder) {
		builder.append("\"includes\" : [\n");
		Iterator<ResourcePatternHint> patternIterator = hints.resourcePatterns().iterator();
		while (patternIterator.hasNext()) {
			ResourcePatternHint hint = patternIterator.next();
			Iterator<String> includeIterator = hint.getIncludes().iterator();
			while (includeIterator.hasNext()) {
				String pattern = JsonUtils.escape(patternToRegexp(includeIterator.next()));
				builder.append("{ \"pattern\": \"").append(pattern).append("\" }");
				if (includeIterator.hasNext()) {
					builder.append(", ");
				}
			}
			if (patternIterator.hasNext()) {
				builder.append(",\n");
			}
		}
		builder.append("\n],\n");
	}

	private void serializeExclude(ResourceHints hints, StringBuilder builder) {
		builder.append("\"excludes\" : [\n");
		Iterator<ResourcePatternHint> patternIterator = hints.resourcePatterns().iterator();
		while (patternIterator.hasNext()) {
			ResourcePatternHint hint = patternIterator.next();
			Iterator<String> excludeIterator = hint.getExcludes().iterator();
			while (excludeIterator.hasNext()) {
				String pattern = JsonUtils.escape(patternToRegexp(excludeIterator.next()));
				builder.append("{ \"pattern\": \"").append(pattern).append("\" }");
				if (excludeIterator.hasNext()) {
					builder.append(", ");
				}
			}
			if (patternIterator.hasNext()) {
				builder.append(",\n");
			}
		}
		builder.append("\n]\n");
	}

	private void serializeBundles(ResourceHints hints, StringBuilder builder) {
		builder.append("\"bundles\" : [\n");
		Iterator<ResourceBundleHint> bundleIterator = hints.resourceBundles().iterator();
		while (bundleIterator.hasNext()) {
			String baseName = JsonUtils.escape(bundleIterator.next().getBaseName());
			builder.append("{ \"name\": \"").append(baseName).append("\" }");
			if (bundleIterator.hasNext()) {
				builder.append(",\n");
			}
		}
		builder.append("]\n");
	}

	private String patternToRegexp(String pattern) {
		return Arrays.stream(pattern.split("\\*")).map(Pattern::quote).collect(Collectors.joining(".*"));
	}

}
