/*
 * Copyright 2007 the original author or authors.
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

package org.springframework.oxm.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.Annotations;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.springframework.util.Assert;

/**
 * Subclass of the {@link XStreamMarshaller} that supports JDK 1.5+ annotation metadata for aliases.
 *
 * @author Arjen Poutsma
 * @see XStreamAlias
 * @since 1.0.2
 */
public class AnnotationXStreamMarshaller extends XStreamMarshaller {

    /**
     * Sets the classes, for which mappings will be read from class-level JDK 1.5+ annotation metadata.
     *
     * @see Annotations#configureAliases(XStream, Class[])
     */
    public void setAnnotatedClass(Class<?> annotatedClass) {
        Assert.notNull(annotatedClass, "'annotatedClass' must not be null");
        Annotations.configureAliases(getXStream(), annotatedClass);
    }

    /**
     * Sets annotated classes, for which aliases will be read from class-level JDK 1.5+ annotation metadata.
     *
     * @see Annotations#configureAliases(XStream, Class[])
     */
    public void setAnnotatedClasses(Class<?>[] annotatedClasses) {
        Assert.notEmpty(annotatedClasses, "'annotatedClasses' must not be empty");
        Annotations.configureAliases(getXStream(), annotatedClasses);
    }

}
