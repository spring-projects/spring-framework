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

package org.springframework.aot.generator;

import java.util.Arrays;

import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.support.MultiCodeBlock;
import org.springframework.util.ClassUtils;

/**
 * Code generator for {@link ResolvableType}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class ResolvableTypeGenerator {

	/**
	 * Generate a type signature for the specified {@link ResolvableType}.
	 * @param target the type to generate
	 * @return the representation of that type
	 */
	public CodeBlock generateTypeFor(ResolvableType target) {
		CodeBlock.Builder code = CodeBlock.builder();
		generate(code, target, false);
		return code.build();
	}

	private void generate(CodeBlock.Builder code, ResolvableType target, boolean forceResolvableType) {
		Class<?> type = ClassUtils.getUserClass(target.toClass());
		if (!target.hasGenerics()) {
			if (forceResolvableType) {
				code.add("$T.forClass($T.class)", ResolvableType.class, type);
			}
			else {
				code.add("$T.class", type);
			}
		}
		else {
			code.add("$T.forClassWithGenerics($T.class, ", ResolvableType.class, type);
			ResolvableType[] generics = target.getGenerics();
			boolean hasGenericParameter = Arrays.stream(generics).anyMatch(ResolvableType::hasGenerics);
			MultiCodeBlock multi = new MultiCodeBlock();
			for (int i = 0; i < generics.length; i++) {
				ResolvableType parameter = target.getGeneric(i);
				multi.add(parameterCode -> generate(parameterCode, parameter, hasGenericParameter));
			}
			code.add(multi.join(", ")).add(")");
		}
	}

}
