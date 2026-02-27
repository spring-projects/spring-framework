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

package org.springframework.aot.nativex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.ConditionalHint;
import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.FieldHint;
import org.springframework.aot.hint.JavaSerializationHint;
import org.springframework.aot.hint.JdkProxyHint;
import org.springframework.aot.hint.LambdaHint;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;

/**
 * Collect {@link ReflectionHints} as map attributes ready for JSON serialization for the GraalVM
 * {@code native-image} compiler.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Janne Valkealahti
 * @see <a href="https://www.graalvm.org/jdk23/reference-manual/native-image/metadata/#reflection">Reflection Use in Native Images</a>
 * @see <a href="https://www.graalvm.org/jdk23/reference-manual/native-image/dynamic-features/JNI/">Java Native Interface (JNI) in Native Image</a>
 * @see <a href="https://www.graalvm.org/jdk23/reference-manual/native-image/overview/BuildConfiguration/">Native Image Build Configuration</a>
 */
class ReflectionHintsAttributes {

	private static final Comparator<JdkProxyHint> JDK_PROXY_HINT_COMPARATOR =
			(left, right) -> {
				String leftSignature = left.getProxiedInterfaces().stream()
						.map(TypeReference::getCanonicalName).collect(Collectors.joining(","));
				String rightSignature = right.getProxiedInterfaces().stream()
						.map(TypeReference::getCanonicalName).collect(Collectors.joining(","));
				return leftSignature.compareTo(rightSignature);
			};

	private static final Comparator<LambdaHint> LAMBDA_HINT_COMPARATOR =
			Comparator.comparing(LambdaHint::getDeclaringClass)
					.thenComparing(LambdaHint::getDeclaringMethod, Comparator.nullsFirst(
							Comparator.comparing(LambdaHint.DeclaringMethod::name)));


	public List<Map<String, Object>> reflection(RuntimeHints hints) {
		List<Map<String, Object>> reflectionHints = new ArrayList<>(reflectionHints(hints));
		reflectionHints.addAll(hints.proxies().jdkProxyHints()
				.sorted(JDK_PROXY_HINT_COMPARATOR)
				.map(this::toAttributes).toList());
		return reflectionHints;
	}

	@SuppressWarnings("removal")
	private List<Map<String, Object>> reflectionHints(RuntimeHints hints) {
		Map<TypeReference, Map<String, Object>> allTypeHints = hints.reflection().typeHints()
				.map(this::toAttributes).collect(Collectors.toMap((attributes -> (TypeReference) Objects.requireNonNull(attributes.get("type"))),
						attributes -> attributes));
		hints.serialization().javaSerializationHints().forEach(hint -> {
			allTypeHints.merge(hint.getType(), toAttributes(hint),
					(currentAttributes, newAttributes) -> {
						handleSerializable(currentAttributes, true);
						return currentAttributes;
					});
		});
		return Stream.concat(
				allTypeHints.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue),
				hints.reflection().lambdaHints().sorted(LAMBDA_HINT_COMPARATOR).map(this::toAttributes)).toList();
	}

	public List<Map<String, Object>> jni(RuntimeHints hints) {
		List<Map<String, Object>> jniHints = new ArrayList<>();
		jniHints.addAll(hints.jni().typeHints()
				.sorted(Comparator.comparing(TypeHint::getType))
				.map(this::toAttributes).toList());
		return jniHints;
	}

	private Map<String, Object> toAttributes(TypeHint hint) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("type", hint.getType());
		handleCondition(attributes, hint);
		handleCategories(attributes, hint.getMemberCategories());
		handleFields(attributes, hint.fields());
		handleExecutables(attributes, Stream.concat(
				hint.constructors(), hint.methods()).sorted().toList());
		handleSerializable(attributes, hint.getSerializable());
		return attributes;
	}

	private Map<String, Object> toAttributes(LambdaHint hint) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		Map<String, Object> lambdaAttributes = new LinkedHashMap<>();
		lambdaAttributes.put("declaringClass", hint.getDeclaringClass());
		LambdaHint.DeclaringMethod declaringMethod = hint.getDeclaringMethod();
		if (declaringMethod != null) {
			Map<String, Object> methodAttributes = new LinkedHashMap<>();
			methodAttributes.put("name", declaringMethod.name());
			methodAttributes.put("parameterTypes", declaringMethod.parameterTypes());
			lambdaAttributes.put("declaringMethod", methodAttributes);
		}
		lambdaAttributes.put("interfaces", hint.getInterfaces());

		attributes.put("lambda", lambdaAttributes);
		return Map.of("type", attributes);
	}

	@SuppressWarnings("removal")
	private Map<String, Object> toAttributes(JavaSerializationHint serializationHint) {
		LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
		handleCondition(attributes, serializationHint);
		attributes.put("type", serializationHint.getType());
		handleSerializable(attributes, true);
		return attributes;
	}

	private void handleCondition(Map<String, Object> attributes, ConditionalHint hint) {
		if (hint.getReachableType() != null) {
			attributes.put("condition", Map.of("typeReached", hint.getReachableType()));
		}
	}

	private void handleFields(Map<String, Object> attributes, Stream<FieldHint> fields) {
		addIfNotEmpty(attributes, "fields", fields
				.sorted(Comparator.comparing(FieldHint::getName, String::compareToIgnoreCase))
				.map(fieldHint -> Map.of("name", fieldHint.getName()))
				.toList());
	}

	private void handleExecutables(Map<String, Object> attributes, List<ExecutableHint> hints) {
		addIfNotEmpty(attributes, "methods", hints.stream()
				.filter(h -> h.getMode().equals(ExecutableMode.INVOKE))
				.map(this::toAttributes).toList());
	}

	private Map<String, Object> toAttributes(ExecutableHint hint) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("name", hint.getName());
		attributes.put("parameterTypes", hint.getParameterTypes());
		return attributes;
	}

	@SuppressWarnings("removal")
	private void handleCategories(Map<String, Object> attributes, Set<MemberCategory> categories) {
		categories.stream().sorted().forEach(category -> {
					switch (category) {
						case ACCESS_PUBLIC_FIELDS, PUBLIC_FIELDS -> attributes.put("allPublicFields", true);
						case ACCESS_DECLARED_FIELDS, DECLARED_FIELDS -> attributes.put("allDeclaredFields", true);
						case INVOKE_PUBLIC_CONSTRUCTORS ->
								attributes.put("allPublicConstructors", true);
						case INVOKE_DECLARED_CONSTRUCTORS ->
								attributes.put("allDeclaredConstructors", true);
						case INVOKE_PUBLIC_METHODS -> attributes.put("allPublicMethods", true);
						case INVOKE_DECLARED_METHODS ->
								attributes.put("allDeclaredMethods", true);
						case PUBLIC_CLASSES -> attributes.put("allPublicClasses", true);
						case DECLARED_CLASSES -> attributes.put("allDeclaredClasses", true);
						case UNSAFE_ALLOCATED -> attributes.put("unsafeAllocated", true);
					}
				}
		);
	}

	private void addIfNotEmpty(Map<String, Object> attributes, String name, @Nullable Object value) {
		if (value != null && (value instanceof Collection<?> collection && !collection.isEmpty())) {
			attributes.put(name, value);
		}
	}

	private Map<String, Object> toAttributes(JdkProxyHint hint) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		handleCondition(attributes, hint);
		attributes.put("type", Map.of("proxy", hint.getProxiedInterfaces()));
		handleSerializable(attributes, hint.getSerializable());
		return attributes;
	}

	private void handleSerializable(Map<String, Object> attributes, @Nullable Boolean serializable) {
		if (serializable != null) {
			attributes.put("serializable", serializable);
		}
	}

}
