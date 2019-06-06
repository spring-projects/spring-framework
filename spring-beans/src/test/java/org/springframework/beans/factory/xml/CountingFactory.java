/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.tests.sample.beans.TestBean;

/**
 * @author Juergen Hoeller
 */
public class CountingFactory implements FactoryBean<String> {

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


	@Override
	public String getObject() {
		return "myString";
	}

	@Override
	public Class<String> getObjectType() {
		return String.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
