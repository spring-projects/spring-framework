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

package org.springframework.beans.factory.generator;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.aot.generator.ResolvableTypeGenerator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.support.MultiCodeBlock;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Support for writing parameters.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class BeanParameterGenerator {

	private final ResolvableTypeGenerator typeGenerator = new ResolvableTypeGenerator();

	private final BiConsumer<BeanDefinition, Builder> innerBeanDefinitionWriter;


	/**
	 * Create an instance with the callback to use to write an inner bean
	 * definition.
	 * @param innerBeanDefinitionWriter the inner bean definition writer
	 */
	public BeanParameterGenerator(BiConsumer<BeanDefinition, Builder> innerBeanDefinitionWriter) {
		this.innerBeanDefinitionWriter = innerBeanDefinitionWriter;
	}

	/**
	 * Create an instance with no support for inner bean definitions.
	 */
	public BeanParameterGenerator() {
		this((beanDefinition, builder) -> {
			throw new IllegalStateException("Inner bean definition is not supported by this instance");
		});
	}


	/**
	 * Write the specified parameter {@code value}.
	 * @param value the value of the parameter
	 * @return the value of the parameter
	 */
	public CodeBlock writeParameterValue(@Nullable Object value) {
		return writeParameterValue(value, () -> ResolvableType.forInstance(value));
	}

	/**
	 * Write the specified parameter {@code value}.
	 * @param value the value of the parameter
	 * @param parameterType the type of the parameter
	 * @return the value of the parameter
	 */
	public CodeBlock writeParameterValue(@Nullable Object value, Supplier<ResolvableType> parameterType) {
		Builder code = CodeBlock.builder();
		writeParameterValue(code, value, parameterType);
		return code.build();
	}

	/**
	 * Write the parameter types of the specified {@link Executable}.
	 * @param executable the executable
	 * @return the parameter types of the executable as a comma separated list
	 */
	public CodeBlock writeExecutableParameterTypes(Executable executable) {
		Class<?>[] parameterTypes = Arrays.stream(executable.getParameters())
				.map(Parameter::getType).toArray(Class<?>[]::new);
		return CodeBlock.of(Arrays.stream(parameterTypes).map(d -> "$T.class")
				.collect(Collectors.joining(", ")), (Object[]) parameterTypes);
	}

	private void writeParameterValue(Builder code, @Nullable Object value, Supplier<ResolvableType> parameterTypeSupplier) {
		if (value == null) {
			code.add("null");
			return;
		}
		ResolvableType parameterType = parameterTypeSupplier.get();
		if (parameterType.isArray()) {
			code.add("new $T { ", parameterType.toClass());
			code.add(writeAll(Arrays.asList(ObjectUtils.toObjectArray(value)),
					item -> parameterType.getComponentType()));
			code.add(" }");
		}
		else if (value instanceof List<?> list) {
			if (list.isEmpty()) {
				code.add("$T.emptyList()", Collections.class);
			}
			else {
				Class<?> listType = (value instanceof ManagedList ? ManagedList.class : List.class);
				code.add("$T.of(", listType);
				ResolvableType collectionType = parameterType.as(List.class).getGenerics()[0];
				code.add(writeAll(list, item -> collectionType));
				code.add(")");
			}
		}
		else if (value instanceof Set<?> set) {
			if (set.isEmpty()) {
				code.add("$T.emptySet()", Collections.class);
			}
			else {
				Class<?> setType = (value instanceof ManagedSet ? ManagedSet.class : Set.class);
				code.add("$T.of(", setType);
				ResolvableType collectionType = parameterType.as(Set.class).getGenerics()[0];
				code.add(writeAll(set, item -> collectionType));
				code.add(")");
			}
		}
		else if (value instanceof Map<?, ?> map) {
			if (map.size() <= 10) {
				code.add("$T.of(", Map.class);
				List<Object> parameters = new ArrayList<>();
				map.forEach((mapKey, mapValue) -> {
					parameters.add(mapKey);
					parameters.add(mapValue);
				});
				code.add(writeAll(parameters, ResolvableType::forInstance));
				code.add(")");
			}
		}
		else if (value instanceof Character character) {
			String result = '\'' + characterLiteralWithoutSingleQuotes(character) + '\'';
			code.add(result);
		}
		else if (isPrimitiveOrWrapper(value)) {
			code.add("$L", value);
		}
		else if (value instanceof String) {
			code.add("$S", value);
		}
		else if (value instanceof Enum<?> enumValue) {
			code.add("$T.$N", enumValue.getClass(), enumValue.name());
		}
		else if (value instanceof Class) {
			code.add("$T.class", value);
		}
		else if (value instanceof ResolvableType) {
			code.add(this.typeGenerator.generateTypeFor((ResolvableType) value));
		}
		else if (value instanceof BeanDefinition) {
			this.innerBeanDefinitionWriter.accept((BeanDefinition) value, code);
		}
		else if (value instanceof BeanReference) {
			code.add("new $T($S)", RuntimeBeanReference.class, ((BeanReference) value).getBeanName());
		}
		else {
			throw new IllegalArgumentException("Parameter of type " + parameterType + " is not supported");
		}
	}

	private <T> CodeBlock writeAll(Iterable<T> items, Function<T, ResolvableType> elementType) {
		MultiCodeBlock multi = new MultiCodeBlock();
		items.forEach(item -> multi.add(code ->
				writeParameterValue(code, item, () -> elementType.apply(item))));
		return multi.join(", ");
	}

	private boolean isPrimitiveOrWrapper(Object value) {
		Class<?> valueType = value.getClass();
		return (valueType.isPrimitive() || valueType == Double.class || valueType == Float.class
				|| valueType == Long.class || valueType == Integer.class || valueType == Short.class
				|| valueType == Character.class || valueType == Byte.class || valueType == Boolean.class);
	}

	// Copied from com.squareup.javapoet.Util
	private static String characterLiteralWithoutSingleQuotes(char c) {
		// see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
		return switch (c) {
			case '\b' -> "\\b"; /* \u0008: backspace (BS) */
			case '\t' -> "\\t"; /* \u0009: horizontal tab (HT) */
			case '\n' -> "\\n"; /* \u000a: linefeed (LF) */
			case '\f' -> "\\f"; /* \u000c: form feed (FF) */
			case '\r' -> "\\r"; /* \u000d: carriage return (CR) */
			case '\"' -> "\""; /* \u0022: double quote (") */
			case '\'' -> "\\'"; /* \u0027: single quote (') */
			case '\\' -> "\\\\"; /* \u005c: backslash (\) */
			default -> Character.isISOControl(c) ? String.format("\\u%04x", (int) c) : Character.toString(c);
		};
	}

}
