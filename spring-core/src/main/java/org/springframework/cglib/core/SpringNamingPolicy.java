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

package org.springframework.cglib.core;

/**
 * Custom variant of CGLIB's {@link DefaultNamingPolicy}, modifying the tag
 * in generated class names from "EnhancerByCGLIB" etc to a "SpringCGLIB" tag
 * and using a plain counter suffix instead of a hash code suffix (as of 6.0).
 *
 * <p>This allows for reliably discovering pre-generated Spring proxy classes
 * in the classpath.
 *
 * @author Juergen Hoeller
 * @since 3.2.8 / 6.0
 */
public final class SpringNamingPolicy implements NamingPolicy {

	public static final SpringNamingPolicy INSTANCE = new SpringNamingPolicy();

	private static final String LABEL = "$$SpringCGLIB$$";

	private SpringNamingPolicy() {
	}

	@Override
	public String getClassName(String prefix, String source, Object key, Predicate names) {
		if (prefix == null) {
			prefix = "org.springframework.cglib.empty.Object";
		}
		else if (prefix.startsWith("java.") || prefix.startsWith("javax.")) {
			prefix = "_" + prefix;
		}

		String base;
		int existingLabel = prefix.indexOf(LABEL);
		if (existingLabel >= 0) {
			base = prefix.substring(0, existingLabel + LABEL.length());
		}
		else {
			base = prefix + LABEL;
		}

		int index = 0;
		String attempt = base + index;
		while (names.evaluate(attempt)) {
			attempt = base + index++;
		}
		return attempt;
	}

}
