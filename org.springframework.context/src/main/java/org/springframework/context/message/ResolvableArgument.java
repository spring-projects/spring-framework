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
package org.springframework.context.message;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.style.ToStringCreator;

/**
 * A message argument value that is resolved from a MessageSource.
 * Allows the value to be localized.
 * @see MessageSource
 * @author Keith Donald
 */
public class ResolvableArgument implements MessageSourceResolvable {

	private String code;

	/**
	 * Creates a resolvable argument.
	 * @param code the code that will be used to lookup the argument value from the message source
	 */
	public ResolvableArgument(String code) {
		this.code = code;
	}

	public String[] getCodes() {
		return new String[] { code.toString() };
	}

	public Object[] getArguments() {
		return null;
	}
	
	public String getDefaultMessage() {
		return String.valueOf(code);
	}

	public String toString() {
		return new ToStringCreator(this).append("code", code).toString();
	}

}