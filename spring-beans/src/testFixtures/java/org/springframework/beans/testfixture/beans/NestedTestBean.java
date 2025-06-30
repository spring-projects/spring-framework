/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.beans.testfixture.beans;

import org.jspecify.annotations.Nullable;

/**
 * Simple nested test bean used for testing bean factories, AOP framework etc.
 *
 * @author Trevor D. Cook
 * @since 30.09.2003
 */
public class NestedTestBean implements INestedTestBean {

	private String company = "";

	public NestedTestBean() {
	}

	public NestedTestBean(String company) {
		setCompany(company);
	}

	public void setCompany(String company) {
		this.company = (company != null ? company : "");
	}

	@Override
	public String getCompany() {
		return company;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (!(obj instanceof NestedTestBean ntb)) {
			return false;
		}
		return this.company.equals(ntb.company);
	}

	@Override
	public int hashCode() {
		return this.company.hashCode();
	}

	@Override
	public String toString() {
		return "NestedTestBean: " + this.company;
	}

}
