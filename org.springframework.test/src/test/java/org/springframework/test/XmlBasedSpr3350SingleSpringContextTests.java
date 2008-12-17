/*
 * Copyright 2007 the original author or authors.
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

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * Concrete implementation of {@link AbstractSpr3350SingleSpringContextTests}
 * which is based on the default {@link XmlBeanDefinitionReader}.
 *
 * @author Sam Brannen
 * @since 2.5
 */
public class XmlBasedSpr3350SingleSpringContextTests extends AbstractSpr3350SingleSpringContextTests {

	public XmlBasedSpr3350SingleSpringContextTests() {
		super();
	}

	public XmlBasedSpr3350SingleSpringContextTests(String name) {
		super(name);
	}

	/**
	 * Returns &quot;XmlBasedSpr3350SingleSpringContextTests-context.xml&quot;.
	 */
	protected final String getConfigPath() {
		return "XmlBasedSpr3350SingleSpringContextTests-context.xml";
	}
}
