/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import org.springframework.beans.testfixture.beans.FactoryMethods;
import org.springframework.beans.testfixture.beans.TestBean;

/**
 * Test class for Spring's ability to create objects using
 * static factory methods, rather than constructors.
 *
 * @author Rod Johnson
 */
public class InstanceFactory {

	protected static int count = 0;

	private String factoryBeanProperty;

	public InstanceFactory() {
		count++;
	}

	public void setFactoryBeanProperty(String s) {
		this.factoryBeanProperty = s;
	}

	public String getFactoryBeanProperty() {
		return this.factoryBeanProperty;
	}

	public FactoryMethods defaultInstance() {
		TestBean tb = new TestBean();
		tb.setName(this.factoryBeanProperty);
		return FactoryMethods.newInstance(tb);
	}

	/**
	 * Note that overloaded methods are supported.
	 */
	public FactoryMethods newInstance(TestBean tb) {
		return FactoryMethods.newInstance(tb);
	}

	public FactoryMethods newInstance(TestBean tb, int num, String name) {
		return FactoryMethods.newInstance(tb, num, name);
	}

}
