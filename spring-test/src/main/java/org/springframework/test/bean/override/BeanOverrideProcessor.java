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

package org.springframework.test.bean.override;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;

/**
 * An interface for Bean Overriding concrete processing.
 * Processors are generally linked to one or more specific concrete annotations
 * (meta-annotated with {@link BeanOverride}) and specify different steps in the
 * process of parsing these annotations, ultimately creating
 * {@link OverrideMetadata} which will be used to instantiate the overrides.
 *
 * <p>Implementations are required to have a no-argument constructor and be
 * stateless.
 *
 * @author Simon Baslé
 * @since 6.2
 */
@FunctionalInterface
public interface BeanOverrideProcessor {

	/**
	 * Determine a {@link ResolvableType} for which an {@link OverrideMetadata}
	 * instance will be created, e.g. by using the annotation to determine the
	 * type.
	 * <p>Defaults to the field corresponding {@link ResolvableType},
	 * additionally tracking the source class if the field is a {@link TypeVariable}.
	 */
	default ResolvableType getOrDeduceType(Field field, Annotation annotation, Class<?> source) {
		return (field.getGenericType() instanceof TypeVariable) ? ResolvableType.forField(field, source)
				: ResolvableType.forField(field);
	}

	/**
	 * Create an {@link OverrideMetadata} for a given annotated field and target
	 * {@link #getOrDeduceType(Field, Annotation, Class) type}.
	 * Specific implementations of metadata can have state to be used during
	 * override {@link OverrideMetadata#createOverride(String, BeanDefinition,
	 * Object) instance creation} (e.g. from further parsing the annotation or
	 * the annotated field).
	 * @param field the annotated field
	 * @param overrideAnnotation the field annotation
	 * @param typeToOverride the target type
	 * @return a new {@link OverrideMetadata}
	 * @see #getOrDeduceType(Field, Annotation, Class)
	 * @see MergedAnnotation#synthesize()
	 */
	OverrideMetadata createMetadata(Field field, Annotation overrideAnnotation, ResolvableType typeToOverride);
}
