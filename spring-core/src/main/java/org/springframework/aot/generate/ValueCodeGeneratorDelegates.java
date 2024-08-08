/*
 * Copyright 2002-2024 the original author or authors.
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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.springframework.aot.generate.ValueCodeGenerator.Delegate;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Code generator {@link Delegate} for well known value types.
 *
 * @author Stephane Nicoll
 * @since 6.1.2
 */
public abstract class ValueCodeGeneratorDelegates {

	/**
	 * A list of {@link Delegate} implementations for the following common value
	 * types.
	 * <ul>
	 * <li>Primitive types</li>
	 * <li>String</li>
	 * <li>Charset</li>
	 * <li>Enum</li>
	 * <li>Class</li>
	 * <li>{@link ResolvableType}</li>
	 * <li>Array</li>
	 * <li>List via {@code List.of}</li>
	 * <li>Set via {@code Set.of} and support for {@link LinkedHashSet}</li>
	 * <li>Map via {@code Map.of} or {@code Map.ofEntries}</li>
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
			new MapDelegate());


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
		@Nullable
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
		@Nullable
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

		@Override
		@Nullable
		public CodeBlock generateCode(ValueCodeGenerator codeGenerator, Object value) {
			if (value instanceof ResolvableType resolvableType) {
				return generateCode(resolvableType, false);
			}
			return null;
		}


		private static CodeBlock generateCode(ResolvableType resolvableType, boolean allowClassResult) {
			if (ResolvableType.NONE.equals(resolvableType)) {
				return CodeBlock.of("$T.NONE", ResolvableType.class);
			}
			Class<?> type = ClassUtils.getUserClass(resolvableType.toClass());
			if (resolvableType.hasGenerics() && resolvableType.hasResolvableGenerics()) {
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

}
