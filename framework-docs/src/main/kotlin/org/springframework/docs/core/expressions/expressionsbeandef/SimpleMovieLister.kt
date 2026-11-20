/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.expressions.expressionsbeandef

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

// tag::snippet[]
class SimpleMovieLister {

	private lateinit var movieFinder: MovieFinder
	private lateinit var defaultLocale: String

	@Autowired
	fun configure(movieFinder: MovieFinder,
				  @Value("#{ systemProperties['user.region'] }") defaultLocale: String) {
		this.movieFinder = movieFinder
		this.defaultLocale = defaultLocale
	}

	// ...
}
// end::snippet[]
