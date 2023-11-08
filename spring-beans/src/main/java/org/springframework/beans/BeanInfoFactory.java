/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;

import org.springframework.lang.Nullable;

/**
 * Interface for custom strategies to create BeanInfo for Spring beans.
 * Implementations of this interface are responsible for providing BeanInfo
 * instances for a given bean class.
 *
 * @param <T> The type of the BeanInfo that this factory creates.
 *
 * @since 3.2
 * @author Arjen Poutsma
 */
public interface BeanInfoFactory<T extends BeanInfo> {

    /**
     * Create BeanInfo for the specified bean class.
     *
     * @param beanClass the class for which BeanInfo should be created
     * @return the BeanInfo instance, or null if not supported
     * @throws BeanInfoCreationException if an error occurs during BeanInfo creation
     */
    @Nullable
    T createBeanInfo(Class<?> beanClass) throws BeanInfoCreationException;
}

/**
 * An exception thrown when an error occurs during BeanInfo creation.
 */
class BeanInfoCreationException extends IntrospectionException {

    public BeanInfoCreationException(String message) {
        super(message);
    }

    public BeanInfoCreationException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
