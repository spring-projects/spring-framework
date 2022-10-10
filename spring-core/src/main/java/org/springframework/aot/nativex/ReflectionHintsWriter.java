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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.FieldHint;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.lang.Nullable;

/**
 * Write {@link ReflectionHints} to the JSON output expected by the GraalVM
 * {@code native-image} compiler, typically named {@code reflect-config.json}
 * or {@code jni-config.json}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Janne Valkealahti
 * @since 6.0
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/Reflection/">Reflection Use in Native Images</a>
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/JNI/">Java Native Interface (JNI) in Native Image</a>
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 */
class ReflectionHintsWriter {

	public static final ReflectionHintsWriter INSTANCE = new ReflectionHintsWriter();

	public void write(BasicJsonWriter writer, ReflectionHints hints) {
		writer.writeArray(hints.typeHints().map(this::toAttributes).toList());
	}

	private Map<String, Object> toAttributes(TypeHint hint) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("name", hint.getType());
		handleCondition(attributes, hint);
		handleCategories(attributes, hint.getMemberCategories());
		handleFields(attributes, hint.fields());
		handleExecutables(attributes, Stream.concat(hint.constructors(), hint.methods()).toList());
		return attributes;
	}

	private void handleCondition(Map<String, Object> attributes, TypeHint hint) {
		if (hint.getReachableType() != null) {
			Map<String, Object> conditionAttributes = new LinkedHashMap<>();
			conditionAttributes.put("typeReachable", hint.getReachableType());
			attributes.put("condition", conditionAttributes);
		}
	}

	private void handleFields(Map<String, Object> attributes, Stream<FieldHint> fields) {
		addIfNotEmpty(attributes, "fields", fields.map(this::toAttributes).toList());
	}

	private Map<String, Object> toAttributes(FieldHint hint) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("name", hint.getName());
		return attributes;
	}

	private void handleExecutables(Map<String, Object> attributes, List<ExecutableHint> hints) {
		addIfNotEmpty(attributes, "methods", hints.stream()
				.filter(h -> h.getMode().equals(ExecutableMode.INVOKE))
				.map(this::toAttributes).toList());
		addIfNotEmpty(attributes, "queriedMethods", hints.stream()
				.filter(h -> h.getMode().equals(ExecutableMode.INTROSPECT))
				.map(this::toAttributes).toList());
	}

	private Map<String, Object> toAttributes(ExecutableHint hint) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("name", hint.getName());
		attributes.put("parameterTypes", hint.getParameterTypes());
		return attributes;
	}

	private void handleCategories(Map<String, Object> attributes, Set<MemberCategory> categories) {
		categories.forEach(category -> {
					switch (category) {
						case PUBLIC_FIELDS -> attributes.put("allPublicFields", true);
						case DECLARED_FIELDS -> attributes.put("allDeclaredFields", true);
						case INTROSPECT_PUBLIC_CONSTRUCTORS ->
								attributes.put("queryAllPublicConstructors", true);
						case INTROSPECT_DECLARED_CONSTRUCTORS ->
								attributes.put("queryAllDeclaredConstructors", true);
						case INVOKE_PUBLIC_CONSTRUCTORS ->
								attributes.put("allPublicConstructors", true);
						case INVOKE_DECLARED_CONSTRUCTORS ->
								attributes.put("allDeclaredConstructors", true);
						case INTROSPECT_PUBLIC_METHODS ->
								attributes.put("queryAllPublicMethods", true);
						case INTROSPECT_DECLARED_METHODS ->
								attributes.put("queryAllDeclaredMethods", true);
						case INVOKE_PUBLIC_METHODS -> attributes.put("allPublicMethods", true);
						case INVOKE_DECLARED_METHODS ->
								attributes.put("allDeclaredMethods", true);
						case PUBLIC_CLASSES -> attributes.put("allPublicClasses", true);
						case DECLARED_CLASSES -> attributes.put("allDeclaredClasses", true);
					}
				}
		);
	}

	private void addIfNotEmpty(Map<String, Object> attributes, String name, @Nullable Object value) {
		if (value != null && (value instanceof Collection<?> collection && !collection.isEmpty())) {
			attributes.put(name, value);
		}
	}

}
