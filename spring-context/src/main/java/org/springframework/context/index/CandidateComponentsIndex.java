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

package org.springframework.context.index;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;
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
 * @deprecated as of 6.1, in favor of the AOT engine.
 */
@Deprecated(since = "6.1", forRemoval = true)
public class CandidateComponentsIndex {

	private static final AntPathMatcher pathMatcher = new AntPathMatcher(".");

	private final MultiValueMap<String, Entry> index;


	CandidateComponentsIndex(List<Properties> content) {
		this.index = parseIndex(content);
	}

	private static MultiValueMap<String, Entry> parseIndex(List<Properties> content) {
		MultiValueMap<String, Entry> index = new LinkedMultiValueMap<>();
		for (Properties entry : content) {
			entry.forEach((type, values) -> {
				String[] stereotypes = ((String) values).split(",");
				for (String stereotype : stereotypes) {
					index.add(stereotype, new Entry((String) type));
				}
			});
		}
		return index;
	}


	/**
	 * Return the candidate types that are associated with the specified stereotype.
	 * @param basePackage the package to check for candidates
	 * @param stereotype the stereotype to use
	 * @return the candidate types associated with the specified {@code stereotype}
	 * or an empty set if none has been found for the specified {@code basePackage}
	 */
	public Set<String> getCandidateTypes(String basePackage, String stereotype) {
		List<Entry> candidates = this.index.get(stereotype);
		if (candidates != null) {
			return candidates.parallelStream()
					.filter(t -> t.match(basePackage))
					.map(t -> t.type)
					.collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}


	private static class Entry {

		private final String type;

		private final String packageName;

		Entry(String type) {
			this.type = type;
			this.packageName = ClassUtils.getPackageName(type);
		}

		public boolean match(String basePackage) {
			if (pathMatcher.isPattern(basePackage)) {
				return pathMatcher.match(basePackage, this.packageName);
			}
			else {
				return this.type.startsWith(basePackage);
			}
		}
	}

}
