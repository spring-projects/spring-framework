/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Concrete implementation of {@link AbstractSpr3350SingleSpringContextTests}
 * which configures a {@link PropertiesBeanDefinitionReader} instead of the
 * default {@link XmlBeanDefinitionReader}.
 *
 * @author Sam Brannen
 * @since 2.5
 */
public class PropertiesBasedSpr3350SingleSpringContextTests extends AbstractSpr3350SingleSpringContextTests {

	public PropertiesBasedSpr3350SingleSpringContextTests() {
		super();
	}

	public PropertiesBasedSpr3350SingleSpringContextTests(String name) {
		super(name);
	}

	/**
	 * Creates a new {@link PropertiesBeanDefinitionReader}.
	 *
	 * @see org.springframework.test.AbstractSingleSpringContextTests#createBeanDefinitionReader(org.springframework.context.support.GenericApplicationContext)
	 */
	@Override
	protected final BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
		return new PropertiesBeanDefinitionReader(context);
	}

	/**
	 * Returns
	 * &quot;PropertiesBasedSpr3350SingleSpringContextTests-context.properties&quot;.
	 */
	@Override
	protected final String getConfigPath() {
		return "PropertiesBasedSpr3350SingleSpringContextTests-context.properties";
	}
}
