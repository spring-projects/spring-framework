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

import org.springframework.oxm.UncategorizedXmlMappingException;

/**
 * JiBX-specific subclass of <code>UncategorizedXmlMappingException</code>, for <code>JiBXBException</code>s that cannot
 * be distinguished further.
 *
 * @author Arjen Poutsma
 * @see JibxUtils#convertJibxException(org.jibx.runtime.JiBXException,boolean)
 * @since 1.0.0
 */
public class JibxSystemException extends UncategorizedXmlMappingException {

    public JibxSystemException(Exception ex) {
        super(ex.getMessage(), ex);
    }
}
