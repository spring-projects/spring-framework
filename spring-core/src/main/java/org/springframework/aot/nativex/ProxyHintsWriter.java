/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.aot.hint.JdkProxyHint;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.TypeReference;

/**
 * Write {@link JdkProxyHint}s contained in a {@link ProxyHints} to the JSON
 * output expected by the GraalVM {@code native-image} compiler, typically named
 * {@code proxy-config.json}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.0
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/DynamicProxy/">Dynamic Proxy in Native Image</a>
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 */
class ProxyHintsWriter {

	public static final ProxyHintsWriter INSTANCE = new ProxyHintsWriter();

	private static final Comparator<JdkProxyHint> JDK_PROXY_HINT_COMPARATOR =
			(left, right) -> {
				String leftSignature = left.getProxiedInterfaces().stream()
						.map(TypeReference::getCanonicalName).collect(Collectors.joining(","));
				String rightSignature = right.getProxiedInterfaces().stream()
						.map(TypeReference::getCanonicalName).collect(Collectors.joining(","));
				return leftSignature.compareTo(rightSignature);
			};

	public void write(BasicJsonWriter writer, ProxyHints hints) {
		writer.writeArray(hints.jdkProxyHints().sorted(JDK_PROXY_HINT_COMPARATOR)
				.map(this::toAttributes).toList());
	}

	private Map<String, Object> toAttributes(JdkProxyHint hint) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		handleCondition(attributes, hint);
		attributes.put("interfaces", hint.getProxiedInterfaces());
		return attributes;
	}

	private void handleCondition(Map<String, Object> attributes, JdkProxyHint hint) {
		if (hint.getReachableType() != null) {
			Map<String, Object> conditionAttributes = new LinkedHashMap<>();
			conditionAttributes.put("typeReachable", hint.getReachableType());
			attributes.put("condition", conditionAttributes);
		}
	}

}
