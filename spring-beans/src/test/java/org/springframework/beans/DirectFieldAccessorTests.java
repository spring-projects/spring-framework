/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.beans;

import org.junit.Test;

import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * Specific {@link DirectFieldAccessor} tests.
 *
 * @author Jose Luis Martin
 * @author Chris Beams
 * @author Stephane Nicoll
 */
public class DirectFieldAccessorTests extends AbstractPropertyAccessorTests {

	@Override
	protected DirectFieldAccessor createAccessor(Object target) {
		return new DirectFieldAccessor(target);
	}


	@Test
	public void withShadowedField() throws Exception {
		final StringBuilder sb = new StringBuilder();

		@SuppressWarnings("serial")
		TestBean target = new TestBean() {
			@SuppressWarnings("unused")
			StringBuilder name = sb;
		};

		DirectFieldAccessor dfa = createAccessor(target);
		assertEquals(StringBuilder.class, dfa.getPropertyType("name"));
		assertEquals(sb, dfa.getPropertyValue("name"));
	}

}
