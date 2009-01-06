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

import junit.framework.TestCase;
import org.apache.xmlbeans.XMLStreamValidationException;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.xml.sax.SAXException;

public class XmlBeansUtilsTest extends TestCase {

    public void testConvertXMLStreamValidationException() {
        assertTrue("Invalid exception conversion", XmlBeansUtils.convertXmlBeansException(
                new XMLStreamValidationException(XmlError.forMessage("")),
                true) instanceof XmlBeansValidationFailureException);

    }

    public void testConvertXmlException() {
        assertTrue("Invalid exception conversion", XmlBeansUtils
                .convertXmlBeansException(new XmlException(""), true) instanceof XmlBeansMarshallingFailureException);
        assertTrue("Invalid exception conversion", XmlBeansUtils.convertXmlBeansException(new XmlException(""),
                false) instanceof XmlBeansUnmarshallingFailureException);
    }

    public void testConvertSAXException() {
        assertTrue("Invalid exception conversion", XmlBeansUtils
                .convertXmlBeansException(new SAXException(""), true) instanceof XmlBeansMarshallingFailureException);
        assertTrue("Invalid exception conversion", XmlBeansUtils.convertXmlBeansException(new SAXException(""),
                false) instanceof XmlBeansUnmarshallingFailureException);
    }

    public void testFallbackException() {
        assertTrue("Invalid exception conversion",
                XmlBeansUtils.convertXmlBeansException(new Exception(""), false) instanceof XmlBeansSystemException);

    }

}
