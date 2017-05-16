/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.index;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Provide access to the candidates that are defined in {@code META-INF/spring.components}.
 *
 * <p>An arbitrary number of stereotypes can be registered (and queried) on the index: a
 * typical example is the fully qualified name of an annotation that flags the class for
 * a certain use case. The following call returns all the {@code @Component}
 * <b>candidate</b> types for the {@code com.example} package (and its sub-packages):
 * <pre class="code">
 * Set&lt;String&gt; candidates = index.getCandidateTypes(
 *         "com.example", "org.springframework.stereotype.Component");
 * </pre>
 *
 * <p>The {@code type} is usually the fully qualified name of a class, though this is
 * not a rule. Similarly, the {@code stereotype} is usually the fully qualified name of
 * a target type but it can be any marker really.
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
public class CandidateComponentsIndex {

	private final MultiValueMap<String, String> index;


	CandidateComponentsIndex(List<Properties> content) {
		this.index = parseIndex(content);
	}


	/**
	 * Return the candidate types that are associated with the specified stereotype.
	 * @param basePackage the package to check for candidates
	 * @param stereotype the stereotype to use
	 * @return the candidate types associated with the specified {@code stereotype}
	 * or an empty set if none has been found for the specified {@code basePackage}
	 */
	public Set<String> getCandidateTypes(String basePackage, String stereotype) {
		List<String> candidates = this.index.get(stereotype);
		if (candidates != null) {
			return candidates.parallelStream()
					.filter(t -> t.startsWith(basePackage))
					.collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}

	private static MultiValueMap<String, String> parseIndex(List<Properties> content) {
		MultiValueMap<String, String> index = new LinkedMultiValueMap<>();
		for (Properties entry : content) {
			for (Map.Entry<Object, Object> entries : entry.entrySet()) {
				String type = (String) entries.getKey();
				String[] stereotypes = ((String) entries.getValue()).split(",");
				for (String stereotype : stereotypes) {
					index.add(stereotype, type);
				}
			}
		}
		return index;
	}

}
