/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.annotation;

/**
 * @author Rob Harrop
 */
public interface IAnnotationTestBean {

	@ManagedAttribute
	String getColour();

	@ManagedAttribute
	void setColour(String colour);

	@ManagedOperation
	void fromInterface();

	@ManagedOperation
	int getExpensiveToCalculate();
}
