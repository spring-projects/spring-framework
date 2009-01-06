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
package org.springframework.oxm.xmlbeans;

import org.apache.xmlbeans.XMLStreamValidationException;
import org.apache.xmlbeans.XmlException;
import org.springframework.oxm.XmlMappingException;
import org.xml.sax.SAXException;

/**
 * Generic utility methods for working with XMLBeans. Mainly for internal use within the framework.
 *
 * @author Arjen Poutsma
 * @since 1.0.0
 */
public class XmlBeansUtils {

    /**
     * Converts the given XMLBeans exception to an appropriate exception from the <code>org.springframework.oxm</code>
     * hierarchy.
     * <p/>
     * A boolean flag is used to indicate whether this exception occurs during marshalling or unmarshalling, since
     * XMLBeans itself does not make this distinction in its exception hierarchy.
     *
     * @param ex          XMLBeans Exception that occured
     * @param marshalling indicates whether the exception occurs during marshalling (<code>true</code>), or
     *                    unmarshalling (<code>false</code>)
     * @return the corresponding <code>XmlMappingException</code>
     */
    public static XmlMappingException convertXmlBeansException(Exception ex, boolean marshalling) {
        if (ex instanceof XMLStreamValidationException) {
            return new XmlBeansValidationFailureException((XMLStreamValidationException) ex);
        }
        else if (ex instanceof XmlException) {
            XmlException xmlException = (XmlException) ex;
            if (marshalling) {
                return new XmlBeansMarshallingFailureException(xmlException);
            }
            else {
                return new XmlBeansUnmarshallingFailureException(xmlException);
            }
        }
        else if (ex instanceof SAXException) {
            SAXException saxException = (SAXException) ex;
            if (marshalling) {
                return new XmlBeansMarshallingFailureException(saxException);
            }
            else {
                return new XmlBeansUnmarshallingFailureException(saxException);
            }
        }
        // fallback
        return new XmlBeansSystemException(ex);
    }

}
