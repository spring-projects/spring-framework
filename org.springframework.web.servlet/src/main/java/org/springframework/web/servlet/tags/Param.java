/*
 * Copyright 2008 the original author or authors.
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

package org.springframework.web.servlet.tags;

/**
 * Bean used to pass name-value pair parameters from a {@link ParamTag} to a
 * {@link ParamAware} tag.
 * 
 * @author Scott Andrews
 * @since 3.0
 * @see ParamTag
 */
public class Param {

	private String name;

	private String value;

	/**
	 * @return the non-encoded parameter name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the non-encoded name of the parameter
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the non-encoded parameter value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set the non-encoded value of the parameter
	 */
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "JSP Tag Param: name '" + name + "', value '" + value + "'";
	}

}
