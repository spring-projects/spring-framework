/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.expression.spel;

/**
 * Captures the possible configuration settings for a compiler that can be
 * used when evaluating expressions.
 *
 * @author Andy Clement
 * @since 4.1
 */
public enum SpelCompilerMode {

	/**
	 * The compiler is switched off; this is the default.
	 */
	OFF,

	/**
	 * In immediate mode, expressions are compiled as soon as possible (usually after 
	 * 1 interpreted run).
	 * If a compiled expression fails it will throw an exception to the caller.
	 */
	IMMEDIATE,

	/**
	 * In mixed mode, expression evaluation silently switches between interpreted 
	 * and compiled over time.
	 * After a number of runs the expression gets compiled. If it later fails 
	 * (possibly due to inferred type information changing) then that will be 
	 * caught internally and the system switches back to interpreted mode. 
	 * It may subsequently compile it again later.
	 */
	MIXED

}
