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
 * Converts a String to a Character and back.
 * 
 * @author Keith Donald
 */
public class StringToCharacter implements Converter<String, Character> {

	public Character convert(String source) {
		if (source.length() != 1) {
			throw new IllegalArgumentException("To be a Character the String '" + source + "' must have a length of 1");
		}
		return Character.valueOf(source.charAt(0));
	}

	public String convertBack(Character target) {
		return target.toString();
	}

}