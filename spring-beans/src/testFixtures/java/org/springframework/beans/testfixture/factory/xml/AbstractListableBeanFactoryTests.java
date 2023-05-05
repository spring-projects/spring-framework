/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans.testfixture.factory.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

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
	@Test
	public void count() {
		assertCount(13);
	}

	protected final void assertCount(int count) {
		String[] defnames = getListableBeanFactory().getBeanDefinitionNames();
		assertThat(defnames.length).as("We should have " + count + " beans, not " + defnames.length).isEqualTo(count);
	}

	protected void assertTestBeanCount(int count) {
		String[] defNames = getListableBeanFactory().getBeanNamesForType(TestBean.class, true, false);
		assertThat(defNames.length).as("We should have " + count + " beans for class org.springframework.beans.testfixture.beans.TestBean, not " +
				defNames.length).isEqualTo(count);

		int countIncludingFactoryBeans = count + 2;
		String[] names = getListableBeanFactory().getBeanNamesForType(TestBean.class, true, true);
		assertThat(names.length).as("We should have " + countIncludingFactoryBeans +
				" beans for class org.springframework.beans.testfixture.beans.TestBean, not " + names.length)
				.isEqualTo(countIncludingFactoryBeans);
	}

	@Test
	public void getDefinitionsForNoSuchClass() {
		String[] defnames = getListableBeanFactory().getBeanNamesForType(String.class);
		assertThat(defnames.length).as("No string definitions").isEqualTo(0);
	}

	/**
	 * Check that count refers to factory class, not bean class. (We don't know
	 * what type factories may return, and it may even change over time.)
	 */
	@Test
	public void getCountForFactoryClass() {
		assertThat(getListableBeanFactory().getBeanNamesForType(FactoryBean.class).length).as("Should have 2 factories, not " +
				getListableBeanFactory().getBeanNamesForType(FactoryBean.class).length).isEqualTo(2);

		assertThat(getListableBeanFactory().getBeanNamesForType(FactoryBean.class).length).as("Should have 2 factories, not " +
				getListableBeanFactory().getBeanNamesForType(FactoryBean.class).length).isEqualTo(2);
	}

	@Test
	public void containsBeanDefinition() {
		assertThat(getListableBeanFactory().containsBeanDefinition("rod")).isTrue();
		assertThat(getListableBeanFactory().containsBeanDefinition("roderick")).isTrue();
	}

}
