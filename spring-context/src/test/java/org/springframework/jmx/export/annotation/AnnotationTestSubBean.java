/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
