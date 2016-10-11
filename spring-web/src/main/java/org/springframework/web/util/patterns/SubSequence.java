/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.web.util.patterns;

/**
 * Used to represent a subsection of an array, useful when wanting to pass that subset of data
 * to another method (e.g. a java regex matcher) but not wanting to create a new string object to hold
 * all that data.
 * 
 * @author Andy Clement
 */
class SubSequence implements CharSequence {

	private char[] chars;
	private int start, end;

	SubSequence(char[] chars, int start, int end) {
		this.chars = chars;
		this.start = start;
		this.end = end;
	}

	@Override
	public int length() {
		return end - start;
	}

	@Override
	public char charAt(int index) {
		return chars[start + index];
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return new SubSequence(chars, this.start + start, this.start + end);
	}
	
	public String toString() {
		return new String(chars,start,end-start);
	}

}