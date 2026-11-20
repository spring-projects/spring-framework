/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
