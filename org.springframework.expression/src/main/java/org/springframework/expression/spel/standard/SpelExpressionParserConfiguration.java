/*
 * Copyright 2008-2009 the original author or authors.
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
package org.springframework.expression.spel.standard;

/**
 * Bit flags that configure optional behaviour in the parser.  Pass the necessary
 * bits when calling the expression parser constructor.
 * 
 * @author Andy Clement
 * @since 3.0
 */
public interface SpelExpressionParserConfiguration {

	/**
	 * This option applies to maps/collections and regular objects.  If the initial part of an expression evaluates to null and then an
	 * attempt is made to resolve an index '[]' or property against it, and this option is set, then the relevant object will be constructed so that
	 * the index/property resolution can proceed.
	 */
	static final int CreateObjectIfAttemptToReferenceNull = 0x0001;
	
	static final int GrowListsOnIndexBeyondSize   = 0x0002;
	
}
