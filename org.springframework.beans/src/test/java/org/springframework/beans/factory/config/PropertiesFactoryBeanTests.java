/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.beans.factory.config;

import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.core.JdkVersion;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Juergen Hoeller
 * @since 01.11.2003
 */
public class PropertiesFactoryBeanTests extends TestCase {

	public void testWithPropertiesFile() throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(new ClassPathResource("/org/springframework/beans/factory/config/test.properties"));
		pfb.afterPropertiesSet();
		Properties props = (Properties) pfb.getObject();
		assertEquals("99", props.getProperty("tb.array[0].age"));
	}

	public void testWithPropertiesXmlFile() throws Exception {
		// ignore for JDK < 1.5
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_15) {
			return;
		}

		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(new ClassPathResource("/org/springframework/beans/factory/config/test-properties.xml"));
		pfb.afterPropertiesSet();
		Properties props = (Properties) pfb.getObject();
		assertEquals("99", props.getProperty("tb.array[0].age"));
	}

	public void testWithLocalProperties() throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		Properties localProps = new Properties();
		localProps.setProperty("key2", "value2");
		pfb.setProperties(localProps);
		pfb.afterPropertiesSet();
		Properties props = (Properties) pfb.getObject();
		assertEquals("value2", props.getProperty("key2"));
	}

	public void testWithPropertiesFileAndLocalProperties() throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(new ClassPathResource("/org/springframework/beans/factory/config/test.properties"));
		Properties localProps = new Properties();
		localProps.setProperty("key2", "value2");
		localProps.setProperty("tb.array[0].age", "0");
		pfb.setProperties(localProps);
		pfb.afterPropertiesSet();
		Properties props = (Properties) pfb.getObject();
		assertEquals("99", props.getProperty("tb.array[0].age"));
		assertEquals("value2", props.getProperty("key2"));
	}

	public void testWithPropertiesFileAndMultipleLocalProperties() throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(new ClassPathResource("/org/springframework/beans/factory/config/test.properties"));

		Properties props1 = new Properties();
		props1.setProperty("key2", "value2");
		props1.setProperty("tb.array[0].age", "0");

		Properties props2 = new Properties();
		props2.setProperty("spring", "framework");
		props2.setProperty("Don", "Mattingly");

		Properties props3 = new Properties();
		props3.setProperty("spider", "man");
		props3.setProperty("bat", "man");

		pfb.setPropertiesArray(new Properties[] {props1, props2, props3});
		pfb.afterPropertiesSet();

		Properties props = (Properties) pfb.getObject();
		assertEquals("99", props.getProperty("tb.array[0].age"));
		assertEquals("value2", props.getProperty("key2"));
		assertEquals("framework", props.getProperty("spring"));
		assertEquals("Mattingly", props.getProperty("Don"));
		assertEquals("man", props.getProperty("spider"));
		assertEquals("man", props.getProperty("bat"));
	}

	public void testWithPropertiesFileAndLocalPropertiesAndLocalOverride() throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(new ClassPathResource("/org/springframework/beans/factory/config/test.properties"));
		Properties localProps = new Properties();
		localProps.setProperty("key2", "value2");
		localProps.setProperty("tb.array[0].age", "0");
		pfb.setProperties(localProps);
		pfb.setLocalOverride(true);
		pfb.afterPropertiesSet();
		Properties props = (Properties) pfb.getObject();
		assertEquals("0", props.getProperty("tb.array[0].age"));
		assertEquals("value2", props.getProperty("key2"));
	}

	public void testWithPrototype() throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setSingleton(false);
		pfb.setLocation(new ClassPathResource("/org/springframework/beans/factory/config/test.properties"));
		Properties localProps = new Properties();
		localProps.setProperty("key2", "value2");
		pfb.setProperties(localProps);
		pfb.afterPropertiesSet();
		Properties props = (Properties) pfb.getObject();
		assertEquals("99", props.getProperty("tb.array[0].age"));
		assertEquals("value2", props.getProperty("key2"));
		Properties newProps = (Properties) pfb.getObject();
		assertTrue(props != newProps);
		assertEquals("99", newProps.getProperty("tb.array[0].age"));
		assertEquals("value2", newProps.getProperty("key2"));
	}

}
