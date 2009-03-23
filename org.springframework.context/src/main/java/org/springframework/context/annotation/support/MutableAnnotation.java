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

package org.springframework.context.annotation.support;

import org.springframework.context.annotation.Configuration;

/**
 * Interface used when dynamically creating mutable instances of annotations associated
 * with {@link Configuration} class processing. This functionality is necessary given
 * that parsing of Configuration classes is done with ASM. Annotation metadata (including
 * attributes) is parsed from the classfiles, and instances of those annotations are
 * then created using this interface and its associated utilities. The annotation
 * instances are attached to the {@link ConfigurationModel} objects at runtime, namely
 * {@link BeanMethod}. This approach is better than the alternative of creating fine-grained
 * model representations of all annotations and attributes. It is better to simply attach
 * annotation instances and read them as needed.
 * 
 * <p>Note: the visibility of this interface would be reduced to package-private save for an
 * obscure restriction of JDK dynamic proxies.
 * {@link MutableAnnotationUtils#createMutableAnnotation(Class)} creates a proxy based on
 * two interfaces: this one, and whatever annotation is currently being parsed. The
 * restriction is that both interfaces may not be package-private if they are in separate
 * packages. In order to avoid unnecessarily restricting the visibility options for
 * user-defined annotations, this interface becomes public. Developers should take caution
 * not to use this annotation outside this package.
 * 
 * @author Chris Beams
 * @see MutableAnnotationUtils
 * @see MutableAnnotationVisitor
 * @see MutableAnnotationInvocationHandler
 */
public interface MutableAnnotation {

	void setAttributeValue(String attribName, Object attribValue);

	Class<?> getAttributeType(String attributeName);

}
