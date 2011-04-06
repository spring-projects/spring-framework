/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;

/**
 * Convenience methods adapting {@link AnnotationMetadata} and {@link MethodMetadata}
 * annotation attribute maps to the {@link AnnotationAttributes} API. As of Spring 3.1.1,
 * both the reflection- and ASM-based implementations of these SPIs return
 * {@link AnnotationAttributes} instances anyway, but for backward-compatibility, their
 * signatures still return Maps. Therefore, for the usual case, these methods perform
 * little more than a cast from Map to AnnotationAttributes.
 *
 * @author Chris Beams
 * @since 3.1.1
 * @see AnnotationAttributes#fromMap(java.util.Map)
 */
class MetadataUtils {

	public static AnnotationAttributes attributesFor(AnnotationMetadata metadata, Class<?> annoClass) {
		return attributesFor(metadata, annoClass.getName());
	}

	public static AnnotationAttributes attributesFor(AnnotationMetadata metadata, String annoClassName) {
		return AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(annoClassName, false));
	}

	public static AnnotationAttributes attributesFor(MethodMetadata metadata, Class<?> targetAnno) {
		return AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(targetAnno.getName()));
	}

}
