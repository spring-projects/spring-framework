/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jmx.export.metadata;

/**
 * Metadata about JMX operation parameters.
 * Used in conjunction with a {@link ManagedOperation} attribute.
 *
 * @author Rob Harrop
 * @since 1.2
 */
public class ManagedOperationParameter {

	private int index = 0;

	private String name = "";

	private String description = "";


	/**
	 * Set the index of this parameter in the operation signature.
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * Return the index of this parameter in the operation signature.
	 */
	public int getIndex() {
		return this.index;
	}

	/**
	 * Set the name of this parameter in the operation signature.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the name of this parameter in the operation signature.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Set a description for this parameter.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Return a description for this parameter.
	 */
	public String getDescription() {
		return this.description;
	}

}
