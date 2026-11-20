/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.expressions.expressionsbeandef;

import org.springframework.beans.factory.annotation.Value;

// tag::snippet[]
public class MovieRecommender {

	private String defaultLocale;

	private CustomerPreferenceDao customerPreferenceDao;

	public MovieRecommender(CustomerPreferenceDao customerPreferenceDao,
			@Value("#{systemProperties['user.country']}") String defaultLocale) {
		this.customerPreferenceDao = customerPreferenceDao;
		this.defaultLocale = defaultLocale;
	}

	// ...
}
// end::snippet[]
