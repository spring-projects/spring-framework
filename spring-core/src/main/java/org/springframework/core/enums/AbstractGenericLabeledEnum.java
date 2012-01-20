/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.core.enums;

/**
 * Base class for labeled enum instances that aren't static.
 *
 * @author Keith Donald
 * @since 1.2.6
 * @deprecated as of Spring 3.0, in favor of Java 5 enums.
 */
public abstract class AbstractGenericLabeledEnum extends AbstractLabeledEnum {

	/**
	 * A descriptive label for the enum.
	 */
	private final String label;


	/**
	 * Create a new StaticLabeledEnum instance.
	 * @param label the label; if <code>null</code>), the enum's code
	 * will be used as label
	 */
	protected AbstractGenericLabeledEnum(String label) {
		this.label = label;
	}


	public String getLabel() {
		if (this.label != null) {
			return label;
		}
		else {
			return getCode().toString();
		}
	}	

}
