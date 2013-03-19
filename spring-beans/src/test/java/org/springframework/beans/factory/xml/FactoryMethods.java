/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans.factory.xml;

import java.util.Collections;
import java.util.List;

import org.springframework.tests.sample.beans.TestBean;


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

	protected static FactoryMethods newInstance(TestBean tb, int num, String name) {
		if (name == null) {
			throw new IllegalStateException("Should never be called with null value");
		}
		return new FactoryMethods(tb, name, num);
	}

	static FactoryMethods newInstance(TestBean tb, int num, Integer something) {
		if (something != null) {
			throw new IllegalStateException("Should never be called with non-null value");
		}
		return new FactoryMethods(tb, null, num);
	}

	@SuppressWarnings("unused")
	private static List listInstance() {
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

}
