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

package org.springframework.aot.generate;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Internal class used to support {@link MethodGenerator#withName(Object...)}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class MethodGeneratorWithName implements MethodGenerator {

	private static final String[] PREFIXES = { "get", "set", "is" };

	private final MethodGenerator methodGenerator;

	private final Object[] nameParts;


	MethodGeneratorWithName(MethodGenerator methodGenerator, Object[] nameParts) {
		this.methodGenerator = methodGenerator;
		this.nameParts = nameParts;
	}


	@Override
	public GeneratedMethod generateMethod(Object... methodNameParts) {
		return this.methodGenerator.generateMethod(generateName(methodNameParts));
	}

	private Object[] generateName(Object... methodNameParts) {
		String joined = MethodNameGenerator.join(methodNameParts);
		String prefix = getPrefix(joined);
		String suffix = joined.substring(prefix.length());
		Object[] result = this.nameParts;
		if (StringUtils.hasLength(prefix)) {
			result = ObjectUtils.addObjectToArray(result, prefix, 0);
		}
		if (StringUtils.hasLength(suffix)) {
			result = ObjectUtils.addObjectToArray(result, suffix);
		}
		return result;
	}

	private String getPrefix(String name) {
		for (String candidate : PREFIXES) {
			if (name.startsWith(candidate)) {
				return candidate;
			}
		}
		return "";
	}

}
