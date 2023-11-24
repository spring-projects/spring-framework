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

package org.springframework.beans.factory.aot;

import java.util.Arrays;

import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.ClassUtils;

/**
 * Internal code generator used to support {@link ResolvableType}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 */
final class ResolvableTypeCodeGenerator {


	private ResolvableTypeCodeGenerator() {
	}


	public static CodeBlock generateCode(ResolvableType resolvableType) {
		return generateCode(resolvableType, false);
	}

	private static CodeBlock generateCode(ResolvableType resolvableType, boolean allowClassResult) {
		if (ResolvableType.NONE.equals(resolvableType)) {
			return CodeBlock.of("$T.NONE", ResolvableType.class);
		}
		Class<?> type = ClassUtils.getUserClass(resolvableType.toClass());
		if (resolvableType.hasGenerics() && !resolvableType.hasUnresolvableGenerics()) {
			return generateCodeWithGenerics(resolvableType, type);
		}
		if (allowClassResult) {
			return CodeBlock.of("$T.class", type);
		}
		return CodeBlock.of("$T.forClass($T.class)", ResolvableType.class, type);
	}

	private static CodeBlock generateCodeWithGenerics(ResolvableType target, Class<?> type) {
		ResolvableType[] generics = target.getGenerics();
		boolean hasNoNestedGenerics = Arrays.stream(generics).noneMatch(ResolvableType::hasGenerics);
		CodeBlock.Builder code = CodeBlock.builder();
		code.add("$T.forClassWithGenerics($T.class", ResolvableType.class, type);
		for (ResolvableType generic : generics) {
			code.add(", $L", generateCode(generic, hasNoNestedGenerics));
		}
		code.add(")");
		return code.build();
	}

}
