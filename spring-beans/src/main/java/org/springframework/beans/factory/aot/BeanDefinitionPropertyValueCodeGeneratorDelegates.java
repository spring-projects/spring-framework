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

package org.springframework.beans.factory.aot;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.ValueCodeGenerator;
import org.springframework.aot.generate.ValueCodeGenerator.Delegate;
import org.springframework.aot.generate.ValueCodeGeneratorDelegates;
import org.springframework.aot.generate.ValueCodeGeneratorDelegates.CollectionDelegate;
import org.springframework.aot.generate.ValueCodeGeneratorDelegates.MapDelegate;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.javapoet.AnnotationSpec;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;

/**
 * Code generator {@link Delegate} for common bean definition property values.
 *
 * @author Stephane Nicoll
 * @since 6.1.2
 */
abstract class BeanDefinitionPropertyValueCodeGeneratorDelegates {

	/**
	 * A list of {@link Delegate} implementations for the following common bean
	 * definition property value types.
	 * <ul>
	 * <li>{@link ManagedList}</li>
	 * <li>{@link ManagedSet}</li>
	 * <li>{@link ManagedMap}</li>
	 * <li>{@link LinkedHashMap}</li>
	 * <li>{@link BeanReference}</li>
	 * <li>{@link TypedStringValue}</li>
	 * </ul>
	 * When combined with {@linkplain ValueCodeGeneratorDelegates#INSTANCES the
	 * delegates for common value types}, this should be added first as they have
	 * special handling for list, set, and map.
	 */
	public static final List<Delegate> INSTANCES = List.of(
			new ManagedListDelegate(),
			new ManagedSetDelegate(),
			new ManagedMapDelegate(),
			new LinkedHashMapDelegate(),
			new BeanReferenceDelegate(),
			new TypedStringValueDelegate()
	);


	/**
	 * {@link Delegate} for {@link ManagedList} types.
	 */
	private static class ManagedListDelegate extends CollectionDelegate<ManagedList<?>> {

		public ManagedListDelegate() {
			super(ManagedList.class, CodeBlock.of("new $T()", ManagedList.class));
		}
	}


	/**
	 * {@link Delegate} for {@link ManagedSet} types.
	 */
	private static class ManagedSetDelegate extends CollectionDelegate<ManagedSet<?>> {

		public ManagedSetDelegate() {
			super(ManagedSet.class, CodeBlock.of("new $T()", ManagedSet.class));
		}
	}


	/**
	 * {@link Delegate} for {@link ManagedMap} types.
	 */
	private static class ManagedMapDelegate implements Delegate {

		private static final CodeBlock EMPTY_RESULT = CodeBlock.of("$T.ofEntries()", ManagedMap.class);

		@Override
		@Nullable
		public CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
			if (value instanceof ManagedMap<?, ?> managedMap) {
				return generateManagedMapCode(valueCodeGenerator, managedMap);
			}
			return null;
		}

		private <K, V> CodeBlock generateManagedMapCode(ValueCodeGenerator valueCodeGenerator,
				ManagedMap<K, V> managedMap) {
			if (managedMap.isEmpty()) {
				return EMPTY_RESULT;
			}
			CodeBlock.Builder code = CodeBlock.builder();
			code.add("$T.ofEntries(", ManagedMap.class);
			Iterator<Entry<K, V>> iterator = managedMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<?, ?> entry = iterator.next();
				code.add("$T.entry($L,$L)", Map.class,
						valueCodeGenerator.generateCode(entry.getKey()),
						valueCodeGenerator.generateCode(entry.getValue()));
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
	private static class LinkedHashMapDelegate extends MapDelegate {

		@Override
		@Nullable
		protected CodeBlock generateMapCode(ValueCodeGenerator valueCodeGenerator, Map<?, ?> map) {
			GeneratedMethods generatedMethods = valueCodeGenerator.getGeneratedMethods();
			if (map instanceof LinkedHashMap<?, ?> && generatedMethods != null) {
				return generateLinkedHashMapCode(valueCodeGenerator, generatedMethods, map);
			}
			return super.generateMapCode(valueCodeGenerator, map);
		}

		private CodeBlock generateLinkedHashMapCode(ValueCodeGenerator valueCodeGenerator,
				GeneratedMethods generatedMethods, Map<?, ?> map) {

			GeneratedMethod generatedMethod = generatedMethods.add("getMap", method -> {
				method.addAnnotation(AnnotationSpec
						.builder(SuppressWarnings.class)
						.addMember("value", "{\"rawtypes\", \"unchecked\"}")
						.build());
				method.returns(Map.class);
				method.addStatement("$T map = new $T($L)", Map.class,
						LinkedHashMap.class, map.size());
				map.forEach((key, value) -> method.addStatement("map.put($L, $L)",
						valueCodeGenerator.generateCode(key),
						valueCodeGenerator.generateCode(value)));
				method.addStatement("return map");
			});
			return CodeBlock.of("$L()", generatedMethod.getName());
		}
	}


	/**
	 * {@link Delegate} for {@link BeanReference} types.
	 */
	private static class BeanReferenceDelegate implements Delegate {

		@Override
		@Nullable
		public CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
			if (value instanceof RuntimeBeanReference runtimeBeanReference &&
					runtimeBeanReference.getBeanType() != null) {
				return CodeBlock.of("new $T($T.class)", RuntimeBeanReference.class,
						runtimeBeanReference.getBeanType());
			}
			else if (value instanceof BeanReference beanReference) {
				return CodeBlock.of("new $T($S)", RuntimeBeanReference.class,
						beanReference.getBeanName());
			}
			return null;
		}
	}


	/**
	 * {@link Delegate} for {@link TypedStringValue} types.
	 */
	private static class TypedStringValueDelegate implements Delegate {

		@Override
		@Nullable
		public CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
			if (value instanceof TypedStringValue typedStringValue) {
				return generateTypeStringValueCode(valueCodeGenerator, typedStringValue);
			}
			return null;
		}

		private CodeBlock generateTypeStringValueCode(ValueCodeGenerator valueCodeGenerator, TypedStringValue typedStringValue) {
			String value = typedStringValue.getValue();
			if (typedStringValue.hasTargetType()) {
				return CodeBlock.of("new $T($S, $L)", TypedStringValue.class, value,
						valueCodeGenerator.generateCode(typedStringValue.getTargetType()));
			}
			return valueCodeGenerator.generateCode(value);
		}
	}
}
