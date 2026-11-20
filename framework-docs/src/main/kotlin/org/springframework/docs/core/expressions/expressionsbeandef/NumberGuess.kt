/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.expressions.expressionsbeandef

import org.springframework.beans.factory.annotation.Value

// tag::snippet[]
class NumberGuess {

	@set:Value("#{ T(java.lang.Math).random() * 100.0 }")
	var randomNumber: Double = 0.0
}
// end::snippet[]
