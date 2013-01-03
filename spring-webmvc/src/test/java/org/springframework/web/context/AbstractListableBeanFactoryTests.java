package org.springframework.web.context;

import org.springframework.beans.TestBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;

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
		assertTrue("We should have " + count + " beans, not " + defnames.length, defnames.length == count);
	}

	public void assertTestBeanCount(int count) {
		String[] defNames = getListableBeanFactory().getBeanNamesForType(TestBean.class, true, false);
		assertTrue("We should have " + count + " beans for class org.springframework.beans.TestBean, not " +
				defNames.length, defNames.length == count);

		int countIncludingFactoryBeans = count + 2;
		String[] names = getListableBeanFactory().getBeanNamesForType(TestBean.class, true, true);
		assertTrue("We should have " + countIncludingFactoryBeans +
				" beans for class org.springframework.beans.TestBean, not " + names.length,
				names.length == countIncludingFactoryBeans);
	}

	public void testGetDefinitionsForNoSuchClass() {
		String[] defnames = getListableBeanFactory().getBeanNamesForType(String.class);
		assertTrue("No string definitions", defnames.length == 0);
	}

	/**
	 * Check that count refers to factory class, not bean class. (We don't know
	 * what type factories may return, and it may even change over time.)
	 */
	public void testGetCountForFactoryClass() {
		assertTrue("Should have 2 factories, not " +
				getListableBeanFactory().getBeanNamesForType(FactoryBean.class).length,
				getListableBeanFactory().getBeanNamesForType(FactoryBean.class).length == 2);

		assertTrue("Should have 2 factories, not " +
				getListableBeanFactory().getBeanNamesForType(FactoryBean.class).length,
				getListableBeanFactory().getBeanNamesForType(FactoryBean.class).length == 2);
	}

	public void testContainsBeanDefinition() {
		assertTrue(getListableBeanFactory().containsBeanDefinition("rod"));
		assertTrue(getListableBeanFactory().containsBeanDefinition("roderick"));
	}

}