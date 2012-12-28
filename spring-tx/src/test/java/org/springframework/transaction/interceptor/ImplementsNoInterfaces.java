/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.transaction.interceptor;

import org.springframework.beans.TestBean;

/**
 * Test for CGLIB proxying that implements no interfaces
 * and has one dependency.
 *
 * @author Rod Johnson
 */
public class ImplementsNoInterfaces {

	private TestBean testBean;

	public void setDependency(TestBean testBean) {
		this.testBean = testBean;
	}

	public String getName() {
		return testBean.getName();
	}

	public void setName(String name) {
		testBean.setName(name);
	}

}
