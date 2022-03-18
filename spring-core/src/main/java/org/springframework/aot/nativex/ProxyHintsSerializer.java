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

import org.springframework.aot.hint.JdkProxyHint;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.TypeReference;

/**
 * Serialize {@link JdkProxyHint}s contained in a {@link ProxyHints} to the JSON file expected by GraalVM
 * {@code native-image} compiler, typically named {@code proxy-config.json}.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/DynamicProxy/">Dynamic Proxy in Native Image</a>
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 */
class ProxyHintsSerializer {

	public String serialize(ProxyHints hints) {
		StringBuilder builder = new StringBuilder();
		builder.append("[\n");
		Iterator<JdkProxyHint> hintIterator = hints.jdkProxies().iterator();
		while (hintIterator.hasNext()) {
			builder.append("{ \"interfaces\": [ ");
			JdkProxyHint hint = hintIterator.next();
			Iterator<TypeReference> interfaceIterator = hint.getProxiedInterfaces().iterator();
			while (interfaceIterator.hasNext()) {
				String name = JsonUtils.escape(interfaceIterator.next().getCanonicalName());
				builder.append("\"").append(name).append("\"");
				if (interfaceIterator.hasNext()) {
					builder.append(", ");
				}
			}
			builder.append(" ] }");
			if (hintIterator.hasNext()) {
				builder.append(",\n");
			}
		}
		builder.append("\n]\n");
		return builder.toString();
	}

}
