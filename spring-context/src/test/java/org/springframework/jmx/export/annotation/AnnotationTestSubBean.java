/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.annotation;

/**
 * @author Rob Harrop
 */
public class AnnotationTestSubBean extends AnnotationTestBean implements IAnnotationTestBean {

	private String colour;

	@Override
	public long myOperation() {
		return 123L;
	}

	@Override
	public void setAge(int age) {
		super.setAge(age);
	}

	@Override
	public int getAge() {
		return super.getAge();
	}

	@Override
	public String getColour() {
		return this.colour;
	}

	@Override
	public void setColour(String colour) {
		this.colour = colour;
	}

	@Override
	public void fromInterface() {
	}

	@Override
	public int getExpensiveToCalculate() {
		return Integer.MAX_VALUE;
	}
}
