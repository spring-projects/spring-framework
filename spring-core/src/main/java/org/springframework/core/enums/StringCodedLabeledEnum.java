/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.util.Assert;

/**
 * Implementation of LabeledEnum which uses a String as the code type.
 *
 * <p>Should almost always be subclassed, but for some simple situations it may be
 * used directly. Note that you will not be able to use unique type-based
 * functionality like {@code LabeledEnumResolver.getLabeledEnumSet(type)} in this case.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 * @see org.springframework.core.enums.LabeledEnumResolver#getLabeledEnumSet(Class)
 * @deprecated as of Spring 3.0, in favor of Java 5 enums.
 */
@Deprecated
@SuppressWarnings("serial")
public class StringCodedLabeledEnum extends AbstractGenericLabeledEnum {

	/**
	 * The unique code of this enum.
	 */
	private final String code;


	/**
	 * Create a new StringCodedLabeledEnum instance.
	 * @param code the String code
	 * @param label the label (can be {@code null})
	 */
	public StringCodedLabeledEnum(String code, String label) {
		super(label);
		Assert.notNull(code, "'code' must not be null");
		this.code = code;
	}


	@Override
	public Comparable getCode() {
		return this.code;
	}

	/**
	 * Return the String code of this LabeledEnum instance.
	 */
	public String getStringCode() {
		return (String) getCode();
	}

}
