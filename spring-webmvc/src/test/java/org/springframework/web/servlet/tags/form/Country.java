/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags.form;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rob Harrop
 * @author Sam Brannen
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class Country {

	public static final Country COUNTRY_AT = new Country("AT", "Austria");

	public static final Country COUNTRY_NL = new Country("NL", "Netherlands");

	public static final Country COUNTRY_UK = new Country("UK", "United Kingdom");

	public static final Country COUNTRY_US = new Country("US", "United States");


	private final String isoCode;

	private final String name;


	public Country(String isoCode, String name) {
		this.isoCode = isoCode;
		this.name = name;
	}


	public String getIsoCode() {
		return this.isoCode;
	}

	public String getName() {
		return this.name;
	}


	@Override
	public String toString() {
		return this.name + "(" + this.isoCode + ")";
	}

	public static Country getCountryWithIsoCode(final String isoCode) {
		if (COUNTRY_AT.isoCode.equals(isoCode)) {
			return COUNTRY_AT;
		}
		if (COUNTRY_NL.isoCode.equals(isoCode)) {
			return COUNTRY_NL;
		}
		if (COUNTRY_UK.isoCode.equals(isoCode)) {
			return COUNTRY_UK;
		}
		if (COUNTRY_US.isoCode.equals(isoCode)) {
			return COUNTRY_US;
		}
		return null;
	}

	public static List getCountries() {
		List countries = new ArrayList();
		countries.add(COUNTRY_AT);
		countries.add(COUNTRY_NL);
		countries.add(COUNTRY_UK);
		countries.add(COUNTRY_US);
		return countries;
	}

}
