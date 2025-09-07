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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.KType;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.http.converter.AbstractKotlinSerializationHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.SmartHttpMessageConverter;

/**
 * A {@link RequestBodyAdvice} implementation that adds support for resolving
 * Kotlin {@link KType} from the parameter and providing it as a hint with a
 * {@code "kotlin.reflect.KType"} key.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 * @see AbstractKotlinSerializationHttpMessageConverter
 */
@SuppressWarnings("removal")
public class KotlinRequestBodyAdvice extends RequestBodyAdviceAdapter {

	@Override
	public boolean supports(MethodParameter methodParameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {

		return AbstractKotlinSerializationHttpMessageConverter.class.isAssignableFrom(converterType);
	}

	@Override
	public @Nullable Map<String, Object> determineReadHints(MethodParameter parameter, Type targetType,
			Class<? extends SmartHttpMessageConverter<?>> converterType) {

		KFunction<?> function = ReflectJvmMapping.getKotlinFunction(Objects.requireNonNull(parameter.getMethod()));
		int i = 0;
		int index = parameter.getParameterIndex();
		for (KParameter p : Objects.requireNonNull(function).getParameters()) {
			if (KParameter.Kind.VALUE.equals(p.getKind())) {
				if (index == i++) {
					if (HttpEntity.class.isAssignableFrom(parameter.getParameterType())) {
						return Collections.singletonMap(KType.class.getName(),
								Objects.requireNonNull(p.getType().getArguments().get(0).getType()));
					}
					return Collections.singletonMap(KType.class.getName(), p.getType());
				}
			}
		}
		return null;
	}
}
