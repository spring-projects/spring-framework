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

import java.util.List;

import junit.framework.TestCase;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.support.StandardTypeLocator;

/**
 * Unit tests for type comparison
 * 
 * @author Andy Clement
 */
public class StandardTypeLocatorTests extends TestCase {

	public void testImports() throws EvaluationException {
		StandardTypeLocator locator = new StandardTypeLocator();
		assertEquals(Integer.class,locator.findType("java.lang.Integer"));
		assertEquals(String.class,locator.findType("java.lang.String"));
		
		List<String> prefixes = locator.getImportPrefixes();
		assertEquals(1,prefixes.size());
		assertTrue(prefixes.contains("java.lang"));
		assertFalse(prefixes.contains("java.util"));

		assertEquals(Boolean.class,locator.findType("Boolean"));
		// currently does not know about java.util by default
//		assertEquals(java.util.List.class,locator.findType("List"));
		
		try {
			locator.findType("URL");
			fail("Should have failed");
		} catch (EvaluationException ee) {
			SpelException sEx = (SpelException)ee;
			assertEquals(SpelMessages.TYPE_NOT_FOUND,sEx.getMessageUnformatted());
		}
		locator.registerImport("java.net");
		assertEquals(java.net.URL.class,locator.findType("URL"));
		
	}

}
