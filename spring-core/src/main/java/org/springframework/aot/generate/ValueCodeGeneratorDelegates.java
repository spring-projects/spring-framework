/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.aot.generate.ValueCodeGenerator.Delegate;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Code generator {@link Delegate} for well known value types.
 *
 * @author Stephane Nicoll
 * @since 6.1.2
 */
public abstract class ValueCodeGeneratorDelegates {

	/**
	 * Return the {@link Delegate} implementations for common value types.
	 * These are:
	 * <ul>
	 * <li>Primitive types,</li>
	 * <li>String,</li>
	 * <li>Charset,</li>
	 * <li>Enum,</li>
	 * <li>Class,</li>
	 * <li>{@link ResolvableType},</li>
	 * <li>Array,</li>
	 * <li>List via {@code List.of},</li>
	 * <li>Set via {@code Set.of} and support of {@link LinkedHashSet},</li>
	 * <li>Map via {@code Map.of} or {@code Map.ofEntries},</li>
	 * <li>Records containing common value types for all its {@link RecordComponent components},</li>
	 * <li>Objects with public setters for all non-public fields, with the value of each field being a common value type.</li>
	 * </ul>
	 * Those implementations do not require the {@link ValueCodeGenerator} to be
	 * {@linkplain ValueCodeGenerator#scoped(GeneratedMethods) scoped}.
	 */
	public static final List<Delegate> INSTANCES = List.of(
			new PrimitiveDelegate(),
			new StringDelegate(),
			new CharsetDelegate(),
			new EnumDelegate(),
			new ClassDelegate(),
			new ResolvableTypeDelegate(),
			new ArrayDelegate(),
			new ListDelegate(),
			new SetDelegate(),
			new MapDelegate(),
			new RecordDelegate(),
			new ObjectDelegate()
	);

	private static boolean isAccessible(Class<?> type) {
		Class<?> currentType = type;
		while (currentType != null) {
			if (!Modifier.isPublic(currentType.getModifiers())) {
				return false;
			}
			currentType = currentType.getDeclaringClass();
		}
		return true;
	}

	/**
	 * Abstract {@link Delegate} for {@code Collection} types.
	 * @param <T> type the collection type
	 */
	public abstract static class CollectionDelegate<T extends Collection<?>> implements Delegate {

		private final Class<?> collectionType;

		private final CodeBlock emptyResult;

		protected CollectionDelegate(Class<?> collectionType, CodeBlock emptyResult) {
			this.collectionType = collectionType;
			this.emptyResult = emptyResult;
		}

		@Override
		@SuppressWarnings("unchecked")
		public CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
			if (this.collectionType.isInstance(value)) {
				T collection = (T) value;
				if (collection.isEmpty()) {
					return this.emptyResult;
				}
				return generateCollectionCode(valueCodeGenerator, collection);
			}
			return null;
		}

		protected CodeBlock generateCollectionCode(ValueCodeGenerator valueCodeGenerator, T collection) {
			return generateCollectionOf(valueCodeGenerator, collection, this.collectionType);
		}

		protected final CodeBlock generateCollectionOf(ValueCodeGenerator valueCodeGenerator,
				Collection<?> collection, Class<?> collectionType) {
			Builder code = CodeBlock.builder();
			code.add("$T.of(", collectionType);
			Iterator<?> iterator = collection.iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();
				code.add("$L", valueCodeGenerator.generateCode(element));
				if (iterator.hasNext()) {
					code.add(", ");
				}
			}
			code.add(")");
			return code.build();
		}
	}

	/**
	 * {@link Delegate} for {@link Map} types.
	 */
	public static class MapDelegate implements Delegate {

		private static final CodeBlock EMPTY_RESULT = CodeBlock.of("$T.emptyMap()", Collections.class);

		@Override
		public CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
			if (value instanceof Map<?, ?> map) {
				if (map.isEmpty()) {
					return EMPTY_RESULT;
				}
				return generateMapCode(valueCodeGenerator, map);
			}
			return null;
		}

		/**
		 * Generate the code for a non-empty {@link Map}.
		 * @param valueCodeGenerator the code generator to use for embedded values
		 * @param map the value to generate
		 * @return the code that represents the specified map or {@code null} if
		 * the specified map is not supported.
		 */
		@Nullable
		protected CodeBlock generateMapCode(ValueCodeGenerator valueCodeGenerator, Map<?, ?> map) {
			map = orderForCodeConsistency(map);
			boolean useOfEntries = map.size() > 10;
			CodeBlock.Builder code = CodeBlock.builder();
			code.add("$T" + ((!useOfEntries) ? ".of(" : ".ofEntries("), Map.class);
			Iterator<? extends Entry<?, ?>> iterator = map.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<?, ?> entry = iterator.next();
				CodeBlock keyCode = valueCodeGenerator.generateCode(entry.getKey());
				CodeBlock valueCode = valueCodeGenerator.generateCode(entry.getValue());
				if (!useOfEntries) {
					code.add("$L, $L", keyCode, valueCode);
				}
				else {
					code.add("$T.entry($L,$L)", Map.class, keyCode, valueCode);
				}
				if (iterator.hasNext()) {
					code.add(", ");
				}
			}
			code.add(")");
			return code.build();
		}

		private <K, V> Map<K, V> orderForCodeConsistency(Map<K, V> map) {
			try {
				return new TreeMap<>(map);
			}
			catch (ClassCastException ex) {
				// If elements are not comparable, just keep the original map
				return map;
			}
		}
	}

	/**
	 * {@link Delegate} for {@code primitive} types.
	 */
	private static class PrimitiveDelegate implements Delegate {

		private static final Map<Character, String> CHAR_ESCAPES = Map.of(
				'\b', "\\b",
				'\t', "\\t",
				'\n', "\\n",
				'\f', "\\f",
				'\r', "\\r",
				'\"', "\"",
				'\'', "\\'",
				'\\', "\\\\"
		);


		@Override
		@Nullable
		public CodeBlock generateCode(ValueCodeGenerator codeGenerator, Object value) {
			if (value instanceof Boolean || value instanceof Integer) {
				return CodeBlock.of("$L", value);
			}
			if (value instanceof Byte) {
				return CodeBlock.of("(byte) $L", value);
			}
			if (value instanceof Short) {
				return CodeBlock.of("(short) $L", value);
			}
			if (value instanceof Long) {
				return CodeBlock.of("$LL", value);
			}
			if (value instanceof Float) {
				return CodeBlock.of("$LF", value);
			}
			if (value instanceof Double) {
				return CodeBlock.of("(double) $L", value);
			}
			if (value instanceof Character character) {
				return CodeBlock.of("'$L'", escape(character));
			}
			return null;
		}

		private String escape(char ch) {
			String escaped = CHAR_ESCAPES.get(ch);
			if (escaped != null) {
				return escaped;
			}
			return (!Character.isISOControl(ch)) ? Character.toString(ch)
					: String.format("\\u%04x", (int) ch);
		}
	}

	/**
	 * {@link Delegate} for {@link String} types.
	 */
	private static class StringDelegate implements Delegate {

		@Override
		@Nullable
		public CodeBlock generateCode(ValueCodeGenerator codeGenerator, Object value) {
			if (value instanceof String) {
				return CodeBlock.of("$S", value);
			}
			return null;
		}
	}

	/**
	 * {@link Delegate} for {@link Charset} types.
	 */
	private static class CharsetDelegate implements Delegate {

		@Override
		@Nullable
		public CodeBlock generateCode(ValueCodeGenerator codeGenerator, Object value) {
			if (value instanceof Charset charset) {
				return CodeBlock.of("$T.forName($S)", Charset.class, charset.name());
			}
			return null;
		}
	}

	/**
	 * {@link Delegate} for {@link Enum} types.
	 */
	private static class EnumDelegate implements Delegate {

		@Override
		@Nullable
		public CodeBlock generateCode(ValueCodeGenerator codeGenerator, Object value) {
			if (value instanceof Enum<?> enumValue) {
				return CodeBlock.of("$T.$L", enumValue.getDeclaringClass(),
						enumValue.name());
			}
			return null;
		}
	}

	/**
	 * {@link Delegate} for {@link Class} types.
	 */
	private static class ClassDelegate implements Delegate {

		@Override
		@Nullable
		public CodeBlock generateCode(ValueCodeGenerator codeGenerator, Object value) {
			if (value instanceof Class<?> clazz) {
				return CodeBlock.of("$T.class", ClassUtils.getUserClass(clazz));
			}
			return null;
		}
	}

	/**
	 * {@link Delegate} for {@link ResolvableType} types.
	 */
	private static class ResolvableTypeDelegate implements Delegate {

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

		@Override
		@Nullable
		public CodeBlock generateCode(ValueCodeGenerator codeGenerator, Object value) {
			if (value instanceof ResolvableType resolvableType) {
				return generateCode(resolvableType, false);
			}
			return null;
		}
	}

	/**
	 * {@link Delegate} for {@code array} types.
	 */
	private static class ArrayDelegate implements Delegate {

		@Override
		@Nullable
		public CodeBlock generateCode(ValueCodeGenerator codeGenerator, Object value) {
			if (value.getClass().isArray()) {
				Stream<CodeBlock> elements = Arrays.stream(ObjectUtils.toObjectArray(value))
						.map(codeGenerator::generateCode);
				CodeBlock.Builder code = CodeBlock.builder();
				code.add("new $T {", value.getClass());
				code.add(elements.collect(CodeBlock.joining(", ")));
				code.add("}");
				return code.build();
			}
			return null;
		}
	}

	/**
	 * {@link Delegate} for {@link List} types.
	 */
	private static class ListDelegate extends CollectionDelegate<List<?>> {

		ListDelegate() {
			super(List.class, CodeBlock.of("$T.emptyList()", Collections.class));
		}
	}

	/**
	 * {@link Delegate} for {@link Set} types.
	 */
	private static class SetDelegate extends CollectionDelegate<Set<?>> {

		SetDelegate() {
			super(Set.class, CodeBlock.of("$T.emptySet()", Collections.class));
		}

		@Override
		protected CodeBlock generateCollectionCode(ValueCodeGenerator valueCodeGenerator, Set<?> collection) {
			if (collection instanceof LinkedHashSet) {
				return CodeBlock.of("new $T($L)", LinkedHashSet.class,
						generateCollectionOf(valueCodeGenerator, collection, List.class));
			}
			return super.generateCollectionCode(valueCodeGenerator,
					orderForCodeConsistency(collection));
		}

		private Set<?> orderForCodeConsistency(Set<?> set) {
			try {
				return new TreeSet<Object>(set);
			}
			catch (ClassCastException ex) {
				// If elements are not comparable, just keep the original set
				return set;
			}
		}
	}

	/**
	 * {@link Delegate} for accessible public {@link Record}.
	 */
	private static class RecordDelegate implements Delegate {
		@Override
		public CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
			if (!(value instanceof Record record) || !isAccessible(record.getClass())) {
				return null;
			}
			// Guaranteed to be in the same order as the canonical constructor
			RecordComponent[] recordComponents = record.getClass().getRecordComponents();
			// A public record is guaranteed to have a public constructor taking all its components
			CodeBlock.Builder builder = CodeBlock.builder();
			builder.add("new $T(", record.getClass());
			for (int i = 0; i < recordComponents.length; i++) {
				if (i != 0) {
					builder.add(", ");
				}
				Object componentValue;
				try {
					componentValue = recordComponents[i].getAccessor().invoke(record);
				}
				catch (IllegalAccessException | InvocationTargetException ex) {
					throw new ValueCodeGenerationException("Unable to generate code for value (" + record + ") since its component (" + recordComponents[i].getName() + ") could not be read.", record, ex);
				}
				builder.add(valueCodeGenerator.generateCode(componentValue));
			}
			builder.add(")");
			return builder.build();
		}
	}

	/**
	 * {@link Delegate} for accessible public {@link Record} and {@link Object} types with a public no-args constructor and
	 * public setters for all non-public fields.
	 */
	private static class ObjectDelegate implements Delegate {
		private static boolean isNotObjectCommonValueType(Object value) {
			return getGeneratedCodeType(value.getClass()) != value.getClass()
					|| value instanceof Record
					|| ClassUtils.isSimpleValueType(value.getClass())
					|| value.getClass().isArray();
		}

		@Override
		public CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
			if (isNotObjectCommonValueType(value) || !isAccessible(value.getClass())) {
				// Use a different delegate to generate the code
				return null;
			}
			try {
				CodeBlock.Builder builder = CodeBlock.builder();

				// A trick to get a code block where we can generate statements
				// anywhere in an expression (i.e. we can put the generated code as an argument
				// to a method): create a supplier lambda and immediately call its
				// "get" method.
				// We need to cast the lambda to Supplier, so Java will
				// know the type of the lambda
				// String tryMe = ((Supplier<String>) () -> { return "hi"; }).get();
				builder.add("(($T<$T>) () -> {$>\n", Supplier.class, value.getClass());

				String identifier = valueCodeGenerator.getIdentifierForCurrentDepth();

				generateCodeToSetFields(valueCodeGenerator, builder, identifier,
						value);

				builder.add("return $N;", identifier);
				builder.add("$<\n}).get()");
				return builder.build();
			}
			catch (ValueCodeGenerationException ex) {
				// If we fail to generate code, return null, since a different Delegate
				// might be able to handle this Object
				return null;
			}
		}

		private static Class<?> getGeneratedCodeType(Class<?> type) {
			if (Set.class.isAssignableFrom(type)) {
				return Set.class;
			}
			if (List.class.isAssignableFrom(type)) {
				return List.class;
			}
			if (Map.class.isAssignableFrom(type)) {
				return Map.class;
			}
			return type;
		}

		private static void generateCodeToSetFields(ValueCodeGenerator valueCodeGenerator,
				CodeBlock.Builder builder,
				String identifier,
				Object value) {
			try {
				// A Constructor is returned only if it is public; a private no-args
				// Constructor will also raise a NoSuchMethodException
				value.getClass().getConstructor();
			}
			catch (NoSuchMethodException ex) {
				throw new ValueCodeGenerationException("Unable to generate code for value (" + value + ") since its class does not have an accessible no-args constructor.", value, ex);
			}
			builder.add("$T $N = new $T();\n", value.getClass(), identifier, value.getClass());
			// Sort the object's fields, so we have a consistent ordering
			List<Field> objectFields = new ArrayList<>();
			ReflectionUtils.doWithFields(value.getClass(), objectFields::add);
			objectFields.sort(Comparator.comparing(Field::getName));

			for (Field field : objectFields) {
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				Object fieldValue;
				try {
					// Set the field to accessible so we can read it
					field.setAccessible(true);
					// Use Field::get instead of ReflectionUtils::getField,
					// since we want to throw a ValueCodeGenerationException
					// if the field cannot be read for any reason
					fieldValue = field.get(value);
				}
				catch (InaccessibleObjectException | IllegalAccessException | SecurityException ex) {
					throw new ValueCodeGenerationException("Unable to generate code for value (" + value + ") since its field (" + field + ") cannot be read.",
							value, ex);
				}
				if (Modifier.isPublic(field.getModifiers())) {
					builder.add("$N.$N = $L;\n", identifier, field.getName(),
							valueCodeGenerator.generateCode(fieldValue));
					continue;
				}
				String methodSuffix = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
				String setterMethodName = "set" + methodSuffix;
				Class<?> expectedParameterType = getGeneratedCodeType(field.getType());
				Method setterMethod = ReflectionUtils.findMethod(value.getClass(), setterMethodName, expectedParameterType);
				if (setterMethod == null
						|| !Modifier.isPublic(setterMethod.getModifiers())
						|| Modifier.isStatic(setterMethod.getModifiers())) {
					throw new ValueCodeGenerationException("Unable to generate code for value (" + value + ") since its field (" + field + ") is not public and does not have a public setter.",
							value, null);
				}
				builder.add("$N.$N($L);\n", identifier, setterMethodName,
						valueCodeGenerator.generateCode(fieldValue));
			}
		}
	}
}
