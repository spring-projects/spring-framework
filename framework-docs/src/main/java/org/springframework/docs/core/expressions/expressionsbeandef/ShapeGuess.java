/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.expressions.expressionsbeandef;

import org.springframework.beans.factory.annotation.Value;

// tag::snippet[]
public class ShapeGuess {

	private double initialShapeSeed;

	@Value("#{ numberGuess.randomNumber }")
	public void setInitialShapeSeed(double initialShapeSeed) {
		this.initialShapeSeed = initialShapeSeed;
	}

	public double getInitialShapeSeed() {
		return initialShapeSeed;
	}
}
// end::snippet[]
