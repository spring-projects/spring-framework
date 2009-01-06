/*
 * Copyright 2006 the original author or authors.
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

package org.springframework.oxm.jibx;

import org.jibx.runtime.JiBXException;
import org.jibx.runtime.ValidationException;
import org.springframework.oxm.XmlMappingException;

/**
 * Generic utility methods for working with JiBX. Mainly for internal use within the framework.
 *
 * @author Arjen Poutsma
 * @since 1.0.0
 */
public abstract class JibxUtils {

    /**
     * Converts the given <code>JiBXException</code> to an appropriate exception from the
     * <code>org.springframework.oxm</code> hierarchy.
     * <p/>
     * A boolean flag is used to indicate whether this exception occurs during marshalling or unmarshalling, since JiBX
     * itself does not make this distinction in its exception hierarchy.
     *
     * @param ex          <code>JiBXException</code> that occured
     * @param marshalling indicates whether the exception occurs during marshalling (<code>true</code>), or
     *                    unmarshalling (<code>false</code>)
     * @return the corresponding <code>XmlMappingException</code>
     */
    public static XmlMappingException convertJibxException(JiBXException ex, boolean marshalling) {
        if (ex instanceof ValidationException) {
            return new JibxValidationFailureException((ValidationException) ex);
        }
        else {
            if (marshalling) {
                return new JibxMarshallingFailureException(ex);
            }
            else {
                return new JibxUnmarshallingFailureException(ex);
            }
        }
    }
}
