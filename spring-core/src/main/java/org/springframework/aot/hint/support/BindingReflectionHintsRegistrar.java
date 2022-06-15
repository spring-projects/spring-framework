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

package org.springframework.aot.hint.support;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeHint.Builder;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

/**
 * Register the necessary reflection hints so that the specified type can be
 * bound at runtime. Fields, constructors and property methods are registered,
 * except for a set of types like those in the {@code java.} package where just
 * the type is registered. Types are discovered transitively and generic types
 * are registered as well.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
public class BindingReflectionHintsRegistrar {

	private static final Log logger = LogFactory.getLog(BindingReflectionHintsRegistrar.class);

	private static final Consumer<ExecutableHint.Builder> INVOKE = builder -> builder
			.withMode(ExecutableMode.INVOKE);

	private static final String KOTLIN_COMPANION_SUFFIX = "$Companion";

	/**
	 * Register the necessary reflection hints to bind the specified types.
	 * @param hints the hints instance to use
	 * @param types the types to bind
	 */
	public void registerReflectionHints(ReflectionHints hints, Type... types) {
		for (Type type : types) {
			Set<Class<?>> referencedTypes = new LinkedHashSet<>();
			collectReferencedTypes(new HashSet<>(), referencedTypes, type);
			for (Class<?> referencedType : referencedTypes) {
				hints.registerType(referencedType, builder -> {
					if (shouldRegisterMembers(referencedType)) {
						registerMembers(hints, referencedType, builder);
					}
				});
			}
		}
	}

	/**
	 * Return whether the members of the type should be registered transitively.
	 * @param type the type to evaluate
	 * @return {@code true} if the members of the type should be registered transitively
	 */
	protected boolean shouldRegisterMembers(Class<?> type) {
		return !type.getCanonicalName().startsWith("java.");
	}

	private void registerMembers(ReflectionHints hints, Class<?> type, Builder builder) {
		builder.withMembers(MemberCategory.DECLARED_FIELDS,
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(type);
			PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
			for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
				Method writeMethod = propertyDescriptor.getWriteMethod();
				if (writeMethod != null && writeMethod.getDeclaringClass() != Object.class) {
					hints.registerMethod(writeMethod, INVOKE);
					MethodParameter methodParameter = MethodParameter.forExecutable(writeMethod, 0);
					registerReflectionHints(hints, methodParameter.getGenericParameterType());
				}
				Method readMethod = propertyDescriptor.getReadMethod();
				if (readMethod != null && readMethod.getDeclaringClass() != Object.class) {
					hints.registerMethod(readMethod, INVOKE);
					MethodParameter methodParameter = MethodParameter.forExecutable(readMethod, -1);
					registerReflectionHints(hints, methodParameter.getGenericParameterType());
				}
			}
			String companionClassName = type.getCanonicalName() + KOTLIN_COMPANION_SUFFIX;
			if (KotlinDetector.isKotlinType(type) && ClassUtils.isPresent(companionClassName, null)) {
				Class<?> companionClass = ClassUtils.resolveClassName(companionClassName, null);
				Method serializerMethod = ClassUtils.getMethodIfAvailable(companionClass, "serializer");
				if (serializerMethod != null) {
					hints.registerMethod(serializerMethod);
				}
			}
		}
		catch (IntrospectionException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Ignoring referenced type [" + type.getName() + "]: " + ex.getMessage());
			}
		}
	}

	private void collectReferencedTypes(Set<Type> seen, Set<Class<?>> types, Type type) {
		if (seen.contains(type)) {
			return;
		}
		seen.add(type);
		ResolvableType resolvableType = ResolvableType.forType(type);
		Class<?> clazz = resolvableType.resolve();
		if (clazz != null) {
			types.add(clazz);
			for (ResolvableType genericResolvableType : resolvableType.getGenerics()) {
				collectReferencedTypes(seen, types, genericResolvableType.getType());
			}
		}
	}
}
