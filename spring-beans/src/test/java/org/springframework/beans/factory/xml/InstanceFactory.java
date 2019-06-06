/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.springframework.tests.sample.beans.TestBean;

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
