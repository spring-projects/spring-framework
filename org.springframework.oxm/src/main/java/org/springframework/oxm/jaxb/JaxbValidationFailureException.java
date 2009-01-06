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

import javax.xml.bind.ValidationException;

import org.springframework.oxm.ValidationFailureException;

/**
 * JAXB-specific subclass of <code>ValidationFailureException</code>.
 *
 * @author Arjen Poutsma
 * @see JaxbUtils#convertJaxbException
 * @since 1.0.0
 */
public class JaxbValidationFailureException extends ValidationFailureException {

    public JaxbValidationFailureException(ValidationException ex) {
        super("JAXB validation exception: " + ex.getMessage(), ex);
    }

}
