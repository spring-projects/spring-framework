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

package org.springframework.beans.factory;

import junit.framework.Assert;

import org.springframework.beans.TestBean;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class AbstractListableBeanFactoryTests extends AbstractBeanFactoryTests {

	/** Subclasses must initialize this */
	protected ListableBeanFactory getListableBeanFactory() {
		BeanFactory bf = getBeanFactory();
		if (!(bf instanceof ListableBeanFactory)) {
			throw new IllegalStateException("ListableBeanFactory required");
		}
		return (ListableBeanFactory) bf;
	}

	/**
	 * Subclasses can override this.
	 */
	public void testCount() {
		assertCount(13);
	}

	protected final void assertCount(int count) {
		String[] defnames = getListableBeanFactory().getBeanDefinitionNames();
		Assert.assertTrue("We should have " + count + " beans, not " + defnames.length, defnames.length == count);
	}

	public void assertTestBeanCount(int count) {
		String[] defNames = getListableBeanFactory().getBeanNamesForType(TestBean.class, true, false);
		Assert.assertTrue("We should have " + count + " beans for class org.springframework.beans.TestBean, not " +
				defNames.length, defNames.length == count);

		int countIncludingFactoryBeans = count + 2;
		String[] names = getListableBeanFactory().getBeanNamesForType(TestBean.class, true, true);
		Assert.assertTrue("We should have " + countIncludingFactoryBeans +
				" beans for class org.springframework.beans.TestBean, not " + names.length,
				names.length == countIncludingFactoryBeans);
	}

	public void testGetDefinitionsForNoSuchClass() {
		String[] defnames = getListableBeanFactory().getBeanNamesForType(String.class);
		Assert.assertTrue("No string definitions", defnames.length == 0);
	}

	/**
	 * Check that count refers to factory class, not bean class. (We don't know
	 * what type factories may return, and it may even change over time.)
	 */
	public void testGetCountForFactoryClass() {
		Assert.assertTrue("Should have 2 factories, not " +
				getListableBeanFactory().getBeanNamesForType(FactoryBean.class).length,
				getListableBeanFactory().getBeanNamesForType(FactoryBean.class).length == 2);

		Assert.assertTrue("Should have 2 factories, not " +
				getListableBeanFactory().getBeanNamesForType(FactoryBean.class).length,
				getListableBeanFactory().getBeanNamesForType(FactoryBean.class).length == 2);
	}

	public void testContainsBeanDefinition() {
		Assert.assertTrue(getListableBeanFactory().containsBeanDefinition("rod"));
		Assert.assertTrue(getListableBeanFactory().containsBeanDefinition("roderick"));
	}

}