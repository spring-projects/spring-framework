/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webfluxfnhandlervalidation

import org.springframework.docs.web.webfluxfnhandlerclasses.Person
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class PersonValidator : Validator {

	override fun supports(clazz: Class<*>): Boolean {
		return Person::class.java.isAssignableFrom(clazz)
	}

	override fun validate(target: Any, errors: Errors) {
		// Validation logic
	}
}
