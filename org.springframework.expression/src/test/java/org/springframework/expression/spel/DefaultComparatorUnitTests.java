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

import junit.framework.TestCase;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.spel.standard.StandardComparator;

/**
 * Unit tests for type comparison
 * 
 * @author Andy Clement
 */
public class DefaultComparatorUnitTests extends TestCase {

	public void testPrimitives() throws EvaluationException {
		TypeComparator comparator = new StandardComparator();
		// primitive int
		assertTrue(comparator.compare(1, 2) < 0);
		assertTrue(comparator.compare(1, 1) == 0);
		assertTrue(comparator.compare(2, 1) > 0);

		assertTrue(comparator.compare(1.0d, 2) < 0);
		assertTrue(comparator.compare(1.0d, 1) == 0);
		assertTrue(comparator.compare(2.0d, 1) > 0);

		assertTrue(comparator.compare(1.0f, 2) < 0);
		assertTrue(comparator.compare(1.0f, 1) == 0);
		assertTrue(comparator.compare(2.0f, 1) > 0);

	}
}
