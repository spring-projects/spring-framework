/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.oxm.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;

/**
 * Parser for the {@code <oxm:castor-marshaller/>} element.
 *
 * @author Jakub Narloch
 * @since 3.1
 * @deprecated as of Spring Framework 4.3.13, due to the lack of activity on the Castor project
 */
@Deprecated
class CastorMarshallerBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.oxm.castor.CastorMarshaller";
	}

}
