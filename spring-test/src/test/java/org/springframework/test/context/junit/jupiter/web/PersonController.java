/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.web;

import org.springframework.test.context.junit.jupiter.comics.Person;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Sam Brannen
 * @since 5.0
 */
@RestController
class PersonController {

	@GetMapping("/person/{id}")
	Person getPerson(@PathVariable long id) {
		if (id == 42) {
			return new Person("Dilbert");
		}
		return new Person("Wally");
	}

}
