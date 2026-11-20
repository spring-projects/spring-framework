/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.expressions.expressionsbeandef

import org.springframework.beans.factory.annotation.Value

// tag::snippet[]
class MovieRecommender(private val customerPreferenceDao: CustomerPreferenceDao,
					   @Value("#{systemProperties['user.country']}")
					   private val defaultLocale: String) {
	// ...
}
// end::snippet[]
