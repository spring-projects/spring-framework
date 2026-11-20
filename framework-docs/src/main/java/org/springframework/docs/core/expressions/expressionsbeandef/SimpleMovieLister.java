/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.expressions.expressionsbeandef;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

// tag::snippet[]
public class SimpleMovieLister {

	private MovieFinder movieFinder;
	private String defaultLocale;

	@Autowired
	public void configure(MovieFinder movieFinder,
			@Value("#{ systemProperties['user.region'] }") String defaultLocale) {
		this.movieFinder = movieFinder;
		this.defaultLocale = defaultLocale;
	}

	// ...
}
// end::snippet[]
