/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.support;

import org.junit.Test;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class BeanDefinitionTests {

	@Test
	public void beanDefinitionEquality() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setAbstract(true);
		bd.setLazyInit(true);
		bd.setScope("request");
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		assertTrue(!bd.equals(otherBd));
		assertTrue(!otherBd.equals(bd));
		otherBd.setAbstract(true);
		otherBd.setLazyInit(true);
		otherBd.setScope("request");
		assertTrue(bd.equals(otherBd));
		assertTrue(otherBd.equals(bd));
		assertTrue(bd.hashCode() == otherBd.hashCode());
	}

	@Test
	public void beanDefinitionEqualityWithPropertyValues() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.getPropertyValues().add("name", "myName");
		bd.getPropertyValues().add("age", "99");
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		otherBd.getPropertyValues().add("name", "myName");
		assertTrue(!bd.equals(otherBd));
		assertTrue(!otherBd.equals(bd));
		otherBd.getPropertyValues().add("age", "11");
		assertTrue(!bd.equals(otherBd));
		assertTrue(!otherBd.equals(bd));
		otherBd.getPropertyValues().add("age", "99");
		assertTrue(bd.equals(otherBd));
		assertTrue(otherBd.equals(bd));
		assertTrue(bd.hashCode() == otherBd.hashCode());
	}

	@Test
	public void beanDefinitionEqualityWithConstructorArguments() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("test");
		bd.getConstructorArgumentValues().addIndexedArgumentValue(1, new Integer(5));
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		otherBd.getConstructorArgumentValues().addGenericArgumentValue("test");
		assertTrue(!bd.equals(otherBd));
		assertTrue(!otherBd.equals(bd));
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, new Integer(9));
		assertTrue(!bd.equals(otherBd));
		assertTrue(!otherBd.equals(bd));
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, new Integer(5));
		assertTrue(bd.equals(otherBd));
		assertTrue(otherBd.equals(bd));
		assertTrue(bd.hashCode() == otherBd.hashCode());
	}

	@Test
	public void beanDefinitionEqualityWithTypedConstructorArguments() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("test", "int");
		bd.getConstructorArgumentValues().addIndexedArgumentValue(1, new Integer(5), "long");
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		otherBd.getConstructorArgumentValues().addGenericArgumentValue("test", "int");
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, new Integer(5));
		assertTrue(!bd.equals(otherBd));
		assertTrue(!otherBd.equals(bd));
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, new Integer(5), "int");
		assertTrue(!bd.equals(otherBd));
		assertTrue(!otherBd.equals(bd));
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, new Integer(5), "long");
		assertTrue(bd.equals(otherBd));
		assertTrue(otherBd.equals(bd));
		assertTrue(bd.hashCode() == otherBd.hashCode());
	}

	@Test
	public void beanDefinitionHolderEquality() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setAbstract(true);
		bd.setLazyInit(true);
		bd.setScope("request");
		BeanDefinitionHolder holder = new BeanDefinitionHolder(bd, "bd");
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		assertTrue(!bd.equals(otherBd));
		assertTrue(!otherBd.equals(bd));
		otherBd.setAbstract(true);
		otherBd.setLazyInit(true);
		otherBd.setScope("request");
		BeanDefinitionHolder otherHolder = new BeanDefinitionHolder(bd, "bd");
		assertTrue(holder.equals(otherHolder));
		assertTrue(otherHolder.equals(holder));
		assertTrue(holder.hashCode() == otherHolder.hashCode());
	}

	@Test
	public void beanDefinitionMerging() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("test");
		bd.getConstructorArgumentValues().addIndexedArgumentValue(1, new Integer(5));
		bd.getPropertyValues().add("name", "myName");
		bd.getPropertyValues().add("age", "99");
		bd.setQualifiedElement(getClass());

		GenericBeanDefinition childBd = new GenericBeanDefinition();
		childBd.setParentName("bd");

		RootBeanDefinition mergedBd = new RootBeanDefinition(bd);
		mergedBd.overrideFrom(childBd);
		assertEquals(2, mergedBd.getConstructorArgumentValues().getArgumentCount());
		assertEquals(2, mergedBd.getPropertyValues().size());
		assertEquals(bd, mergedBd);

		mergedBd.getConstructorArgumentValues().getArgumentValue(1, null).setValue(new Integer(9));
		assertEquals(new Integer(5), bd.getConstructorArgumentValues().getArgumentValue(1, null).getValue());
		assertEquals(getClass(), bd.getQualifiedElement());
	}

}
