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

package org.springframework.test.context.support;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.test.context.web.WebMergedContextConfiguration;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * TODO [SPR-5243] Document XmlWebContextLoader.
 *
 * @author Sam Brannen
 * @since 3.2
 */
public class XmlWebContextLoader extends AbstractGenericWebContextLoader {

	/**
	 * Returns &quot;<code>-context.xml</code>&quot;.
	 */
	protected String getResourceSuffix() {
		return "-context.xml";
	}

	/**
	 * TODO [SPR-5243] Document overridden loadBeanDefinitions().
	 *
	 * @see org.springframework.test.context.support.AbstractGenericWebContextLoader#loadBeanDefinitions(org.springframework.web.context.support.GenericWebApplicationContext, org.springframework.test.context.web.WebMergedContextConfiguration)
	 */
	@Override
	protected void loadBeanDefinitions(GenericWebApplicationContext context,
			WebMergedContextConfiguration webMergedConfig) {
		new XmlBeanDefinitionReader(context).loadBeanDefinitions(webMergedConfig.getLocations());
	}

}
