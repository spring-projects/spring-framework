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

/**
 * Converts String to a Boolean and back.
 * 
 * @author Keith Donald
 */
public class StringToBoolean implements Converter<String, Boolean> {

	private static final String VALUE_TRUE = "true";

	private static final String VALUE_FALSE = "false";

	private String trueString;

	private String falseString;

	/**
	 * Create a StringToBoolean converter.
	 */
	public StringToBoolean() {
	}

	/**
	 * Create a StringToBoolean converter configured with specific values for true and false strings.
	 * @param trueString special true string to use
	 * @param falseString special false string to use
	 */
	public StringToBoolean(String trueString, String falseString) {
		this.trueString = trueString;
		this.falseString = falseString;
	}

	public Boolean convert(String source) throws Exception {
		if (trueString != null && source.equals(trueString)) {
			return Boolean.TRUE;
		} else if (falseString != null && source.equals(falseString)) {
			return Boolean.FALSE;
		} else if (trueString == null && source.equals(VALUE_TRUE)) {
			return Boolean.TRUE;
		} else if (falseString == null && source.equals(VALUE_FALSE)) {
			return Boolean.FALSE;
		} else {
			throw new IllegalArgumentException("Invalid boolean value [" + source + "]");
		}
	}

	public String convertBack(Boolean target) throws Exception {
		if (Boolean.TRUE.equals(target)) {
			if (trueString != null) {
				return trueString;
			} else {
				return VALUE_TRUE;
			}
		} else if (Boolean.FALSE.equals(target)) {
			if (falseString != null) {
				return falseString;
			} else {
				return VALUE_FALSE;
			}
		} else {
			throw new IllegalArgumentException("Invalid boolean value [" + target + "]");
		}
	}

}