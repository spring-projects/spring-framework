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
import org.springframework.oxm.UnmarshallingFailureException;
import org.xml.sax.SAXException;

/**
 * Castor-specific subclass of <code>UnmarshallingFailureException</code>.
 *
 * @author Arjen Poutsma
 * @see CastorUtils#convertXmlException
 * @since 1.0.0
 */
public class CastorUnmarshallingFailureException extends UnmarshallingFailureException {

    public CastorUnmarshallingFailureException(MarshalException ex) {
        super("Castor unmarshalling exception: " + ex.getMessage(), ex);
    }

    public CastorUnmarshallingFailureException(SAXException ex) {
        super("Castor unmarshalling exception: " + ex.getMessage(), ex);
    }
}
