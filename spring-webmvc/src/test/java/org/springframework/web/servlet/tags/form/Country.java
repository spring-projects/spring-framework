/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.servlet.tags.form;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rob Harrop
 * @author Sam Brannen
 */
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
