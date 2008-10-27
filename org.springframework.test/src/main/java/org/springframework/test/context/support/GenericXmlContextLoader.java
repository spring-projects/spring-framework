/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.test.context.support;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>
 * Concrete implementation of {@link AbstractGenericContextLoader} which reads
 * bean definitions from XML resources.
 * </p>
 *
 * @author Sam Brannen
 * @since 2.5
 */
public class GenericXmlContextLoader extends AbstractGenericContextLoader {

	/**
	 * <p>
	 * Creates a new {@link XmlBeanDefinitionReader}.
	 * </p>
	 *
	 * @return a new XmlBeanDefinitionReader.
	 * @see AbstractGenericContextLoader#createBeanDefinitionReader(GenericApplicationContext)
	 * @see XmlBeanDefinitionReader
	 */
	@Override
	protected BeanDefinitionReader createBeanDefinitionReader(final GenericApplicationContext context) {
		return new XmlBeanDefinitionReader(context);
	}

	/**
	 * Returns &quot;<code>-context.xml</code>&quot;.
	 *
	 * @see org.springframework.test.context.support.AbstractContextLoader#getResourceSuffix()
	 */
	@Override
	public String getResourceSuffix() {
		return "-context.xml";
	}

}
