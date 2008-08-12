/*
 * Copyright 2004-2008 the original author or authors.
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

import org.springframework.expression.spel.standard.StandardTypeLocator;

/**
 * Tests referring to system types, custom types, unqualified types.
 * 
 * @author Andy Clement
 */
public class TypeReferencing extends ExpressionTestCase {

	public void testFromAJar() {
		evaluate("new SimpleType().sayHi('Andy')", "Hi! Andy", String.class);
	}

	public void testFromAJar2() {
		evaluate("new a.b.c.PackagedType().sayHi('Andy')", "Hi! Andy", String.class);
	}

	public void testFromAJar3() {
		evaluateAndCheckError("new PackagedType().sayHi('Andy')", SpelMessages.TYPE_NOT_FOUND);
	}

	public void testFromAJar4() {
		((StandardTypeLocator) eContext.getTypeUtils().getTypeLocator()).registerImport("a.b.c");
		evaluate("new PackagedType().sayHi('Andy')", "Hi! Andy", String.class);
	}

}
