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
import java.util.LinkedHashSet;
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
 * @author Juergen Hoeller
 * @since 5.0
 */
public class CandidateComponentsIndex {

	private static final AntPathMatcher pathMatcher = new AntPathMatcher(".");

	private final Set<String> registeredScans = new LinkedHashSet<>();

	private final MultiValueMap<String, Entry> index = new LinkedMultiValueMap<>();

	private final boolean complete;


	/**
	 * Create a new index instance from parsed components index files.
	 */
	CandidateComponentsIndex(List<Properties> content) {
		for (Properties entry : content) {
			entry.forEach((type, values) -> {
				String[] stereotypes = ((String) values).split(",");
				for (String stereotype : stereotypes) {
					this.index.add(stereotype, new Entry((String) type));
				}
			});
		}
		this.complete = true;
	}

	/**
	 * Create a new index instance for programmatic population.
	 * @since 7.0
	 */
	public CandidateComponentsIndex() {
		this.complete = false;
	}


	/**
	 * Register the given base packages (or base package patterns) as scanned.
	 * @since 7.0
	 */
	public void registerScan(String... basePackages) {
		Collections.addAll(this.registeredScans, basePackages);
	}

	/**
	 * Return the registered base packages (or base package patterns).
	 * @since 7.0
	 */
	public Set<String> getRegisteredScans() {
		return this.registeredScans;
	}

	/**
	 * Determine whether this index contains entries for the given base package
	 * (or base package pattern).
	 * @since 7.0
	 */
	public boolean hasScannedPackage(String packageName) {
		return (this.complete ||
				this.registeredScans.stream().anyMatch(basePackage -> matchPackage(basePackage, packageName)));
	}

	/**
	 * Programmatically register one or more stereotypes for the given candidate type.
	 * @since 7.0
	 */
	public void registerCandidateType(String type, String... stereotypes) {
		for (String stereotype : stereotypes) {
			this.index.add(stereotype, new Entry(type));
		}
	}

	/**
	 * Return the registered stereotypes packages (or base package patterns).
	 * @since 7.0
	 */
	public Set<String> getRegisteredStereotypes() {
		return this.index.keySet();
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
			return candidates.stream()
					.filter(t -> t.match(basePackage))
					.map(t -> t.type)
					.collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}

	private static boolean matchPackage(String basePackage, String packageName) {
		if (pathMatcher.isPattern(basePackage)) {
			return pathMatcher.match(basePackage, packageName);
		}
		else {
			return packageName.equals(basePackage) || packageName.startsWith(basePackage + ".");
		}
	}


	private static class Entry {

		final String type;

		private final String packageName;

		Entry(String type) {
			this.type = type;
			this.packageName = ClassUtils.getPackageName(type);
		}

		public boolean match(String basePackage) {
			return matchPackage(basePackage, this.packageName);
		}
	}

}
