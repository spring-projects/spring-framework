/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	protected void count() {
		assertCount(13);
	}

	protected final void assertCount(int count) {
		String[] defnames = getListableBeanFactory().getBeanDefinitionNames();
		assertThat(defnames).as("We should have " + count + " beans, not " + defnames.length).hasSize(count);
	}

	protected void assertTestBeanCount(int count) {
		String[] defNames = getListableBeanFactory().getBeanNamesForType(TestBean.class, true, false);
		assertThat(defNames).as("We should have " + count + " beans for class org.springframework.beans.testfixture.beans.TestBean, not " +
				defNames.length).hasSize(count);

		int countIncludingFactoryBeans = count + 2;
		String[] names = getListableBeanFactory().getBeanNamesForType(TestBean.class, true, true);
		assertThat(names.length).as("We should have " + countIncludingFactoryBeans +
				" beans for class org.springframework.beans.testfixture.beans.TestBean, not " + names.length)
				.isEqualTo(countIncludingFactoryBeans);
	}

	@Test
	protected void getDefinitionsForNoSuchClass() {
		String[] defnames = getListableBeanFactory().getBeanNamesForType(String.class);
		assertThat(defnames).as("No string definitions").isEmpty();
	}

	/**
	 * Check that count refers to factory class, not bean class. (We don't know
	 * what type factories may return, and it may even change over time.)
	 */
	@Test
	protected void getCountForFactoryClass() {
		assertThat(getListableBeanFactory().getBeanNamesForType(FactoryBean.class)).as("Should have 2 factories, not " +
				getListableBeanFactory().getBeanNamesForType(FactoryBean.class).length).hasSize(2);

		assertThat(getListableBeanFactory().getBeanNamesForType(FactoryBean.class)).as("Should have 2 factories, not " +
				getListableBeanFactory().getBeanNamesForType(FactoryBean.class).length).hasSize(2);
	}

	@Test
	protected void containsBeanDefinition() {
		assertThat(getListableBeanFactory().containsBeanDefinition("rod")).isTrue();
		assertThat(getListableBeanFactory().containsBeanDefinition("roderick")).isTrue();
	}

}
