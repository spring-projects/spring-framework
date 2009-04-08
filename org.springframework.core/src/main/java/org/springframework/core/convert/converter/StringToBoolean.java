/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.core.convert.converter;

import org.springframework.util.Assert;

/**
 * Converts String to a Boolean.  The trueString and falseStrings are configurable.
 * 
 * @see #StringToBoolean(String, String)
 * @author Keith Donald
 */
public class StringToBoolean implements Converter<String, Boolean> {

	private String trueString;

	private String falseString;

	/**
	 * Create a StringToBoolean converter with the default 'true' and 'false' strings.
	 */
	public StringToBoolean() {
		this("true", "false");
	}

	/**
	 * Create a StringToBoolean converter configured with specific values for true and false strings.
	 * @param trueString special true string to use (required)
	 * @param falseString special false string to use (required)
	 */
	public StringToBoolean(String trueString, String falseString) {
		Assert.hasText(trueString, "The true string is required");
		Assert.hasText(falseString, "The false string is required");		
		this.trueString = trueString;
		this.falseString = falseString;
	}

	public Boolean convert(String source) {
		if (source.equals(trueString)) {
			return Boolean.TRUE;
		} else if (source.equals(falseString)) {
			return Boolean.FALSE;
		} else {
			throw new IllegalArgumentException("Invalid boolean string '" + source + "'; expected '" + trueString + "' or '" + falseString + "'");
		}
	}

	public String convertBack(Boolean target) {
		if (Boolean.TRUE.equals(target)) {
			return trueString;
		} else if (Boolean.FALSE.equals(target)) {
			return falseString;
		} else {
			throw new IllegalArgumentException("Invalid boolean value " + target);
		}
	}

}