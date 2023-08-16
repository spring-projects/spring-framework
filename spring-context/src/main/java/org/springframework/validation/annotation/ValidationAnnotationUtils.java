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

package org.springframework.validation.annotation;

import java.lang.annotation.Annotation;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;

/**
 * Utility class for handling validation annotations.
 * Mainly for internal use within the framework.
 *
 * @author Christoph Dreis
 * @author Juergen Hoeller
 * @since 5.3.7
 */
public abstract class ValidationAnnotationUtils {

	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];


	/**
	 * Determine any validation hints by the given annotation.
	 * <p>This implementation checks for Spring's
	 * {@link org.springframework.validation.annotation.Validated},
	 * {@code @javax.validation.Valid}, and custom annotations whose
	 * name starts with "Valid" which may optionally declare validation
	 * hints through the "value" attribute.
	 * @param ann the annotation (potentially a validation annotation)
	 * @return the validation hints to apply (possibly an empty array),
	 * or {@code null} if this annotation does not trigger any validation
	 */
	@Nullable
	public static Object[] determineValidationHints(Annotation ann) {
		// Direct presence of @Validated ?
		if (ann instanceof Validated) {
			return ((Validated) ann).value();
		}
		// Direct presence of @Valid ?
		Class<? extends Annotation> annotationType = ann.annotationType();
		if ("javax.validation.Valid".equals(annotationType.getName())) {
			return EMPTY_OBJECT_ARRAY;
		}
		// Meta presence of @Validated ?
		Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
		if (validatedAnn != null) {
			return validatedAnn.value();
		}
		// Custom validation annotation ?
		if (annotationType.getSimpleName().startsWith("Valid")) {
			return convertValidationHints(AnnotationUtils.getValue(ann));
		}
		// No validation triggered
		return null;
	}

	private static Object[] convertValidationHints(@Nullable Object hints) {
		if (hints == null) {
			return EMPTY_OBJECT_ARRAY;
		}
		return (hints instanceof Object[] ? (Object[]) hints : new Object[]{hints});
	}

}
