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
import java.util.List;
import java.util.stream.Stream;

import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.FieldHint;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;

/**
 * Serialize {@link ReflectionHints} to the JSON file expected by GraalVM {@code native-image} compiler,
 * typically named {@code reflect-config.json}.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/Reflection/">Reflection Use in Native Images</a>
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 */
@SuppressWarnings("serial")
class ReflectionHintsSerializer {

	public String serialize(ReflectionHints hints) {
		StringBuilder builder = new StringBuilder();
		builder.append("[\n");
		Iterator<TypeHint> hintIterator = hints.typeHints().iterator();
		while (hintIterator.hasNext()) {
			TypeHint hint = hintIterator.next();
			String name = JsonUtils.escape(hint.getType().getCanonicalName());
			builder.append("{\n\"name\": \"").append(name).append("\"");
			serializeCondition(hint, builder);
			serializeMembers(hint, builder);
			serializeFields(hint, builder);
			serializeExecutables(hint, builder);
			builder.append(" }");
			if (hintIterator.hasNext()) {
				builder.append(",\n");
			}
		}
		builder.append("\n]");
		return builder.toString();
	}

	private void serializeCondition(TypeHint hint, StringBuilder builder) {
		if (hint.getReachableType() != null) {
			String name = JsonUtils.escape(hint.getReachableType().getCanonicalName());
			builder.append(",\n\"condition\": { \"typeReachable\": \"").append(name).append("\" }");
		}
	}

	private void serializeFields(TypeHint hint, StringBuilder builder) {
		Iterator<FieldHint> fieldIterator = hint.fields().iterator();
		if (fieldIterator.hasNext()) {
			builder.append(",\n\"fields\": [\n");
			while (fieldIterator.hasNext()) {
				FieldHint fieldHint = fieldIterator.next();
				String name = JsonUtils.escape(fieldHint.getName());
				builder.append("{ \"name\": \"").append(name).append("\"");
				if (fieldHint.isAllowWrite()) {
					builder.append(", \"allowWrite\": ").append(fieldHint.isAllowWrite());
				}
				if (fieldHint.isAllowUnsafeAccess()) {
					builder.append(", \"allowUnsafeAccess\": ").append(fieldHint.isAllowUnsafeAccess());
				}
				builder.append(" }");
				if (fieldIterator.hasNext()) {
					builder.append(",\n");
				}
			}
			builder.append("\n]");
		}
	}

	private void serializeExecutables(TypeHint hint, StringBuilder builder) {
		List<ExecutableHint> executables = Stream.concat(hint.constructors(), hint.methods()).toList();
		Iterator<ExecutableHint> methodIterator = executables.stream().filter(h -> h.getModes().contains(ExecutableMode.INVOKE) || h.getModes().isEmpty()).iterator();
		Iterator<ExecutableHint> queriedMethodIterator = executables.stream().filter(h -> h.getModes().contains(ExecutableMode.INTROSPECT)).iterator();
		if (methodIterator.hasNext()) {
			builder.append(",\n");
			serializeMethods("methods", methodIterator, builder);
		}
		if (queriedMethodIterator.hasNext()) {
			builder.append(",\n");
			serializeMethods("queriedMethods", queriedMethodIterator, builder);
		}
	}

	private void serializeMethods(String fieldName, Iterator<ExecutableHint> methodIterator, StringBuilder builder) {
		builder.append("\"").append(JsonUtils.escape(fieldName)).append("\": [\n");
		while (methodIterator.hasNext()) {
			ExecutableHint hint = methodIterator.next();
			String name = JsonUtils.escape(hint.getName());
			builder.append("{\n\"name\": \"").append(name).append("\", ").append("\"parameterTypes\": [ ");
			Iterator<TypeReference> parameterIterator =  hint.getParameterTypes().iterator();
			while (parameterIterator.hasNext()) {
				String parameterName = JsonUtils.escape(parameterIterator.next().getCanonicalName());
				builder.append("\"").append(parameterName).append("\"");
				if (parameterIterator.hasNext()) {
					builder.append(", ");
				}
			}
			builder.append(" ] }\n");
			if (methodIterator.hasNext()) {
				builder.append(",\n");
			}
		}
		builder.append("]\n");
	}

	private void serializeMembers(TypeHint hint, StringBuilder builder) {
		Iterator<MemberCategory> categoryIterator = hint.getMemberCategories().iterator();
		if (categoryIterator.hasNext()) {
			builder.append(",\n");
			while (categoryIterator.hasNext()) {
				switch (categoryIterator.next()) {
					case PUBLIC_FIELDS -> builder.append("\"allPublicFields\": true");
					case DECLARED_FIELDS -> builder.append("\"allDeclaredFields\": true");
					case INTROSPECT_PUBLIC_CONSTRUCTORS -> builder.append("\"queryAllPublicConstructors\": true");
					case INTROSPECT_DECLARED_CONSTRUCTORS -> builder.append("\"queryAllDeclaredConstructors\": true");
					case INVOKE_PUBLIC_CONSTRUCTORS -> builder.append("\"allPublicConstructors\": true");
					case INVOKE_DECLARED_CONSTRUCTORS -> builder.append("\"allDeclaredConstructors\": true");
					case INTROSPECT_PUBLIC_METHODS -> builder.append("\"queryAllPublicMethods\": true");
					case INTROSPECT_DECLARED_METHODS -> builder.append("\"queryAllDeclaredMethods\": true");
					case INVOKE_PUBLIC_METHODS -> builder.append("\"allPublicMethods\": true");
					case INVOKE_DECLARED_METHODS -> builder.append("\"allDeclaredMethods\": true");
					case PUBLIC_CLASSES -> builder.append("\"allPublicClasses\": true");
					case DECLARED_CLASSES -> builder.append("\"allDeclaredClasses\": true");
				}
				if (categoryIterator.hasNext()) {
					builder.append(",\n");
				}
			}
		}
	}

}
