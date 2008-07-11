/*
 * Copyright 2004-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.binding.expression;

/**
 * An evaluation exception indicating a expression that references a property failed to evaluate because the property
 * could not be found.
 * @author Keith Donald
 */
public class PropertyNotFoundException extends EvaluationException {

	/**
	 * Creates a new property not found exception
	 * @param contextClass the class of object upon which property evaluation was attempted
	 * @param property the property that could not be found
	 * @param cause root cause of the failure
	 */
	public PropertyNotFoundException(Class contextClass, String property, Throwable cause) {
		super(contextClass, property, "Property '" + property + "' not found on context of class ["
				+ contextClass.getName() + "]", cause);
	}

}
