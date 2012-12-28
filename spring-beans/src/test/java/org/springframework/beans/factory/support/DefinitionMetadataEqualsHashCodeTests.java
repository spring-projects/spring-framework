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

package org.springframework.beans.factory.support;

import junit.framework.TestCase;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;

import test.beans.TestBean;

/**
 * @author Rob Harrop
 */
public class DefinitionMetadataEqualsHashCodeTests extends TestCase {

	@SuppressWarnings("serial")
	public void testRootBeanDefinitionEqualsAndHashCode() throws Exception {
		RootBeanDefinition master = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition equal = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition notEqual = new RootBeanDefinition(String.class);
		RootBeanDefinition subclass = new RootBeanDefinition(TestBean.class) {};
		setBaseProperties(master);
		setBaseProperties(equal);
		setBaseProperties(notEqual);
		setBaseProperties(subclass);

		assertEqualsContract(master, equal, notEqual, subclass);
		assertEquals("Hash code for equal instances should match", master.hashCode(), equal.hashCode());
	}

	@SuppressWarnings("serial")
	public void testChildBeanDefinitionEqualsAndHashCode() throws Exception {
		ChildBeanDefinition master = new ChildBeanDefinition("foo");
		ChildBeanDefinition equal = new ChildBeanDefinition("foo");
		ChildBeanDefinition notEqual = new ChildBeanDefinition("bar");
		ChildBeanDefinition subclass = new ChildBeanDefinition("foo"){};
		setBaseProperties(master);
		setBaseProperties(equal);
		setBaseProperties(notEqual);
		setBaseProperties(subclass);

		assertEqualsContract(master, equal, notEqual, subclass);
		assertEquals("Hash code for equal instances should match", master.hashCode(), equal.hashCode());
	}

	public void testRuntimeBeanReference() throws Exception {
		RuntimeBeanReference master = new RuntimeBeanReference("name");
		RuntimeBeanReference equal = new RuntimeBeanReference("name");
		RuntimeBeanReference notEqual = new RuntimeBeanReference("someOtherName");
		RuntimeBeanReference subclass = new RuntimeBeanReference("name"){};
		assertEqualsContract(master, equal, notEqual, subclass);
	}
	private void setBaseProperties(AbstractBeanDefinition definition) {
		definition.setAbstract(true);
		definition.setAttribute("foo", "bar");
		definition.setAutowireCandidate(false);
		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
		//definition.getConstructorArgumentValues().addGenericArgumentValue("foo");
		definition.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
		definition.setDependsOn(new String[]{"foo", "bar"});
		definition.setDestroyMethodName("destroy");
		definition.setEnforceDestroyMethod(false);
		definition.setEnforceInitMethod(true);
		definition.setFactoryBeanName("factoryBean");
		definition.setFactoryMethodName("factoryMethod");
		definition.setInitMethodName("init");
		definition.setLazyInit(true);
		definition.getMethodOverrides().addOverride(new LookupOverride("foo", "bar"));
		definition.getMethodOverrides().addOverride(new ReplaceOverride("foo", "bar"));
		definition.getPropertyValues().add("foo", "bar");
		definition.setResourceDescription("desc");
		definition.setRole(BeanDefinition.ROLE_APPLICATION);
		definition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		definition.setSource("foo");
	}

	private void assertEqualsContract(Object master, Object equal, Object notEqual, Object subclass) {
		assertEquals("Should be equal", master, equal);
		assertFalse("Should not be equal", master.equals(notEqual));
		assertEquals("Subclass should be equal", master, subclass);
	}

}
