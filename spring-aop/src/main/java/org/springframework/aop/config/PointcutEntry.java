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
 * {@link ParseState} entry representing a pointcut.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class PointcutEntry implements ParseState.Entry {

	private final String name;

	/**
	 * Creates a new instance of the {@link PointcutEntry} class.
	 * @param name the bean name of the pointcut
	 */
	public PointcutEntry(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Pointcut '" + this.name + "'";
	}

}
