/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.config.java.internal.parsing.asm;


/**
 * Note: the visibility of this interface would be reduced to package-private
 * save for an obscure restriction of JDK dynamic proxies.
 * {@link MutableAnnotationUtils#createMutableAnnotation(Class)} creates a proxy based on two
 * interfaces: this one, and whatever annotation is currently being parsed.
 * The restriction is that both interfaces may not be package-private if they
 * are in separate packages.  In order to avoid unnecessarily restricting the
 * visibility options for user-defined annotations, this interface becomes public.
 * Because it is in the internal.* package, it won't pollute the public API, but
 * developers should take caution not to use this annotation outside the
 * internal.parsing.asm package.
 *
 * @author Chris Beams
 */
public interface MutableAnnotation {
    void setAttributeValue(String attribName, Object attribValue);
    Class<?> getAttributeType(String attributeName);
}
