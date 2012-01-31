/*
 * Copyright 2002-2006 the original author or authors.
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

import org.springframework.beans.factory.FactoryBean;

import test.beans.TestBean;

/**
 * @author Juergen Hoeller
 */
public class CountingFactory implements FactoryBean {

	private static int factoryBeanInstanceCount = 0;


	/**
	 * Clear static state.
	 */
	public static void reset() {
		factoryBeanInstanceCount = 0;
	}

	public static int getFactoryBeanInstanceCount() {
		return factoryBeanInstanceCount;
	}


	public CountingFactory() {
		factoryBeanInstanceCount++;
	}

	public void setTestBean(TestBean tb) {
		if (tb.getSpouse() == null) {
			throw new IllegalStateException("TestBean needs to have spouse");
		}
	}


	public Object getObject() {
		return "myString";
	}

	public Class getObjectType() {
		return String.class;
	}

	public boolean isSingleton() {
		return true;
	}

}