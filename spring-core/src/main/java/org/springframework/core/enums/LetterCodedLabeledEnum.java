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

import org.springframework.util.Assert;

/**
 * Implementation of LabeledEnum which uses a letter as the code type.
 *
 * <p>Should almost always be subclassed, but for some simple situations it may be
 * used directly. Note that you will not be able to use unique type-based functionality
 * like <code>LabeledEnumResolver.getLabeledEnumSet(type)</code> in this case.
 *
 * @author Keith Donald
 * @since 1.2.2
 * @deprecated as of Spring 3.0, in favor of Java 5 enums.
 */
@Deprecated
public class LetterCodedLabeledEnum extends AbstractGenericLabeledEnum {

	/**
	 * The unique code of this enum.
	 */
	private final Character code;


	/**
	 * Create a new LetterCodedLabeledEnum instance.
	 * @param code the letter code
	 * @param label the label (can be <code>null</code>)
	 */
	public LetterCodedLabeledEnum(char code, String label) {
		super(label);
		Assert.isTrue(Character.isLetter(code),
				"The code '" + code + "' is invalid: it must be a letter");
		this.code = new Character(code);
	}

	
	public Comparable getCode() {
		return code;
	}

	/**
	 * Return the letter code of this LabeledEnum instance.
	 */
	public char getLetterCode() {
		return ((Character) getCode()).charValue();
	}

}
