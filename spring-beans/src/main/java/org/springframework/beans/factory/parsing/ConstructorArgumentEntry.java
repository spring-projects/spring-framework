/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import org.springframework.util.Assert;

/**
 * {@link ParseState} entry representing a (possibly indexed)
 * constructor argument.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ConstructorArgumentEntry implements ParseState.Entry {

	private final int index;


	/**
	 * Creates a new instance of the {@link ConstructorArgumentEntry} class
	 * representing a constructor argument with a (currently) unknown index.
	 */
	public ConstructorArgumentEntry() {
		this.index = -1;
	}

	/**
	 * Creates a new instance of the {@link ConstructorArgumentEntry} class
	 * representing a constructor argument at the supplied {@code index}.
	 * @param index the index of the constructor argument
	 * @throws IllegalArgumentException if the supplied {@code index}
	 * is less than zero
	 */
	public ConstructorArgumentEntry(int index) {
		Assert.isTrue(index >= 0, "Constructor argument index must be greater than or equal to zero");
		this.index = index;
	}


	@Override
	public String toString() {
		return "Constructor-arg" + (this.index >= 0 ? " #" + this.index : "");
	}

}
