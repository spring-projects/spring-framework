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
package org.springframework.oxm.jaxb;

import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.ValidationException;

import org.springframework.oxm.XmlMappingException;
import org.springframework.util.ClassUtils;

/**
 * Generic utility methods for working with JAXB. Mainly for internal use within the framework.
 *
 * @author Arjen Poutsma
 * @since 1.0.0
 */
public abstract class JaxbUtils {

    public static final int JAXB_1 = 0;

    public static final int JAXB_2 = 1;

    private static final String JAXB_2_CLASS_NAME = "javax.xml.bind.Binder";

    private static int jaxbVersion = JAXB_1;

    static {
        try {
            ClassUtils.forName(JAXB_2_CLASS_NAME);
            jaxbVersion = JAXB_2;
        }
        catch (ClassNotFoundException ex1) {
            // leave JAXB 1 as default
        }
    }

    /**
     * Gets the major JAXB version. This means we can do things like if <code>(getJaxbVersion() &lt;= JAXB_2)</code>.
     *
     * @return a code comparable to the JAXP_XX codes in this class
     * @see #JAXB_1
     * @see #JAXB_2
     */
    public static int getJaxbVersion() {
        return jaxbVersion;
    }

    /**
     * Converts the given <code>JAXBException</code> to an appropriate exception from the
     * <code>org.springframework.oxm</code> hierarchy.
     *
     * @param ex <code>JAXBException</code> that occured
     * @return the corresponding <code>XmlMappingException</code>
     */
    public static XmlMappingException convertJaxbException(JAXBException ex) {
        if (ex instanceof MarshalException) {
            return new JaxbMarshallingFailureException((MarshalException) ex);
        }
        else if (ex instanceof UnmarshalException) {
            return new JaxbUnmarshallingFailureException((UnmarshalException) ex);
        }
        else if (ex instanceof ValidationException) {
            return new JaxbValidationFailureException((ValidationException) ex);
        }
        // fallback
        return new JaxbSystemException(ex);
    }

}
