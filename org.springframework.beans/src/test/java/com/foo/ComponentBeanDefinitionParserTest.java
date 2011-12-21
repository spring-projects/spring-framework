/*
 * Copyright 2006-2010 the original author or authors.
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
package com.foo;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * @author Costin Leau
 */
public class ComponentBeanDefinitionParserTest {

	private static XmlBeanFactory bf;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		bf = new XmlBeanFactory(new ClassPathResource(
				"com/foo/component-config.xml"));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		bf.destroySingletons();
	}

	private Component getBionicFamily() {
		return bf.getBean("bionic-family", Component.class);
	}

	@Test
	public void testBionicBasic() throws Exception {
		Component cp = getBionicFamily();
		assertThat("Bionic-1", equalTo(cp.getName()));
	}

	@Test
	public void testBionicFirstLevelChildren() throws Exception {
		Component cp = getBionicFamily();
		List<Component> components = cp.getComponents();
		assertThat(2, equalTo(components.size()));
		assertThat("Mother-1", equalTo(components.get(0).getName()));
		assertThat("Rock-1", equalTo(components.get(1).getName()));
	}

	@Test
	public void testBionicSecondLevenChildren() throws Exception {
		Component cp = getBionicFamily();
		List<Component> components = cp.getComponents().get(0).getComponents();
		assertThat(2, equalTo(components.size()));
		assertThat("Karate-1", equalTo(components.get(0).getName()));
		assertThat("Sport-1", equalTo(components.get(1).getName()));
	}
}