/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags.form;

import org.springframework.beans.testfixture.beans.TestBean;

/**
 * @author Juergen Hoeller
 */
public class TestBeanWithRealCountry extends TestBean {

	private Country realCountry = Country.COUNTRY_AT;


	public void setRealCountry(Country realCountry) {
		this.realCountry = realCountry;
	}

	public Country getRealCountry() {
		return realCountry;
	}

}
