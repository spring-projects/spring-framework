/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigvalidation

import org.springframework.validation.Errors
import org.springframework.validation.Validator

class FooValidator : Validator {
	override fun supports(clazz: Class<*>) = false

	override fun validate(target: Any, errors: Errors) {
	}
}