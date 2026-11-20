/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.expressions.expressionsbeandef;

import org.springframework.beans.factory.annotation.Value;

// tag::snippet[]
public class NumberGuess {

	private double randomNumber;

	@Value("#{ T(java.lang.Math).random() * 100.0 }")
	public void setRandomNumber(double randomNumber) {
		this.randomNumber = randomNumber;
	}

	public double getRandomNumber() {
		return randomNumber;
	}
}
// end::snippet[]
