/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans;

import java.util.Collections;
import java.util.List;

/**
 * Test class for Spring's ability to create objects using static
 * factory methods, rather than constructors.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class FactoryMethods {

	public static FactoryMethods nullInstance() {
		return null;
	}

	public static FactoryMethods defaultInstance() {
		TestBean tb = new TestBean();
		tb.setName("defaultInstance");
		return new FactoryMethods(tb, "default", 0);
	}

	/**
	 * Note that overloaded methods are supported.
	 */
	public static FactoryMethods newInstance(TestBean tb) {
		return new FactoryMethods(tb, "default", 0);
	}

	public static FactoryMethods newInstance(TestBean tb, int num, String name) {
		if (name == null) {
			throw new IllegalStateException("Should never be called with null value");
		}
		return new FactoryMethods(tb, name, num);
	}

	static ExtendedFactoryMethods newInstance(TestBean tb, int num, Integer something) {
		if (something != null) {
			throw new IllegalStateException("Should never be called with non-null value");
		}
		return new ExtendedFactoryMethods(tb, null, num);
	}

	@SuppressWarnings("unused")
	private static List<?> listInstance() {
		return Collections.EMPTY_LIST;
	}


	private int num = 0;
	private String name = "default";
	private TestBean tb;
	private String stringValue;


	/**
	 * Constructor is private: not for use outside this class,
	 * even by IoC container.
	 */
	private FactoryMethods(TestBean tb, String name, int num) {
		this.tb = tb;
		this.name = name;
		this.num = num;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	public String getStringValue() {
		return this.stringValue;
	}

	public TestBean getTestBean() {
		return this.tb;
	}

	protected TestBean protectedGetTestBean() {
		return this.tb;
	}

	@SuppressWarnings("unused")
	private TestBean privateGetTestBean() {
		return this.tb;
	}

	public int getNum() {
		return num;
	}

	public String getName() {
		return name;
	}

	/**
	 * Set via Setter Injection once instance is created.
	 */
	public void setName(String name) {
		this.name = name;
	}


	public static class ExtendedFactoryMethods extends FactoryMethods {

		ExtendedFactoryMethods(TestBean tb, String name, int num) {
			super(tb, name, num);
		}
	}

}
