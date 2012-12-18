/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.aop.config;

import org.springframework.beans.factory.parsing.ParseState;

/**
 * {@link ParseState} entry representing an advice element.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class AdviceEntry implements ParseState.Entry {

	private final String kind;


	/**
	 * Creates a new instance of the {@link AdviceEntry} class.
	 * @param kind the kind of advice represented by this entry (before, after, around, etc.)
	 */
	public AdviceEntry(String kind) {
		this.kind = kind;
	}

	@Override
	public String toString() {
		return "Advice (" + this.kind + ")";
	}

}
