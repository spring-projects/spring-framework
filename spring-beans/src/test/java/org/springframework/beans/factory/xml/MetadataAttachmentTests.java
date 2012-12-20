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

package org.springframework.beans.factory.xml;

import junit.framework.TestCase;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Rob Harrop
 */
public class MetadataAttachmentTests extends TestCase {

	private DefaultListableBeanFactory beanFactory;

	protected void setUp() throws Exception {
		this.beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
			new ClassPathResource("withMeta.xml", getClass()));
	}

	public void testMetadataAttachment() throws Exception {
		BeanDefinition beanDefinition1 = this.beanFactory.getMergedBeanDefinition("testBean1");
		assertEquals("bar", beanDefinition1.getAttribute("foo"));
	}

	public void testMetadataIsInherited() throws Exception {
		BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("testBean2");
		assertEquals("Metadata not inherited", "bar", beanDefinition.getAttribute("foo"));
		assertEquals("Child metdata not attached", "123", beanDefinition.getAttribute("abc"));
	}

	public void testPropertyMetadata() throws Exception {
		BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("testBean3");
		PropertyValue pv = beanDefinition.getPropertyValues().getPropertyValue("name");
		assertEquals("Harrop", pv.getAttribute("surname"));
	}

}
