/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import org.springframework.beans.testfixture.beans.TestBean;

/**
 * Test class for Spring's ability to create
 * objects using static factory methods, rather
 * than constructors.
 * @author Rod Johnson
 */
public class TestBeanCreator {

	public static TestBean createTestBean(String name, int age) {
		TestBean tb = new TestBean();
		tb.setName(name);
		tb.setAge(age);
		return tb;
	}

	public static TestBean createTestBean() {
		TestBean tb = new TestBean();
		tb.setName("Tristan");
		tb.setAge(2);
		return tb;
	}

}
