/*
 * Copyright 2002-2009 the original author or authors.
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

import java.lang.annotation.Annotation;

/**
 * Interface used when dynamically creating mutable instances of annotations associated
 * with {@link Configuration} class processing. This functionality is necessary given
 * that parsing of Configuration classes is done with ASM. Annotation metadata (including
 * attributes) is parsed from the class files, and instances of those annotations are
 * then created using this interface and its associated utilities. The annotation
 * instances are attached to the configuration model objects at runtime, namely
 * {@link ConfigurationClassMethod}. This approach is better than the alternative of
 * creating fine-grained model representations of all annotations and attributes.
 * It is better to simply attach annotation instances and read them as needed.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see ConfigurationClassAnnotationVisitor
 * @see ConfigurationClassReaderUtils#createMutableAnnotation
 */
public interface ConfigurationClassAnnotation extends Annotation {

	void setAttributeValue(String attribName, Object attribValue);

	Class<?> getAttributeType(String attributeName);

}
