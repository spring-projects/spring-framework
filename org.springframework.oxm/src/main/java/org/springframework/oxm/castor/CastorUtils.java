/*
 * Copyright 2005 the original author or authors.
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
package org.springframework.oxm.castor;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.xml.XMLException;
import org.springframework.oxm.XmlMappingException;

/**
 * Generic utility methods for working with Castor. Mainly for internal use within the framework.
 *
 * @author Arjen Poutsma
 * @since 1.0.0
 */
public class CastorUtils {

    /**
     * Converts the given <code>XMLException</code> to an appropriate exception from the
     * <code>org.springframework.oxm</code> hierarchy.
     * <p/>
     * A boolean flag is used to indicate whether this exception occurs during marshalling or unmarshalling, since
     * Castor itself does not make this distinction in its exception hierarchy.
     *
     * @param ex          Castor <code>XMLException</code> that occured
     * @param marshalling indicates whether the exception occurs during marshalling (<code>true</code>), or
     *                    unmarshalling (<code>false</code>)
     * @return the corresponding <code>XmlMappingException</code>
     */
    public static XmlMappingException convertXmlException(XMLException ex, boolean marshalling) {
        if (ex instanceof MarshalException) {
            MarshalException marshalException = (MarshalException) ex;
            if (marshalling) {
                return new CastorMarshallingFailureException(marshalException);
            }
            else {
                return new CastorUnmarshallingFailureException(marshalException);
            }
        }
        else if (ex instanceof ValidationException) {
            return new CastorValidationFailureException((ValidationException) ex);
        }
        // fallback
        return new CastorSystemException("Unknown Castor exception: " + ex.getMessage(), ex);
    }

}
