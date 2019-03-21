/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.core.env;

import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link CompositePropertySource}.
 *
 * @author Phillip Webb
 */
public class CompositePropertySourceTests {

	@Test
	public void addFirst() {
		PropertySource<?> p1 = new MapPropertySource("p1", Collections.emptyMap());
		PropertySource<?> p2 = new MapPropertySource("p2", Collections.emptyMap());
		PropertySource<?> p3 = new MapPropertySource("p3", Collections.emptyMap());
		CompositePropertySource composite = new CompositePropertySource("c");
		composite.addPropertySource(p2);
		composite.addPropertySource(p3);
		composite.addPropertySource(p1);
		composite.addFirstPropertySource(p1);
		String s = composite.toString();
		int i1 = s.indexOf("name='p1'");
		int i2 = s.indexOf("name='p2'");
		int i3 = s.indexOf("name='p3'");
		assertTrue("Bad order: " + s, ((i1 < i2) && (i2 < i3)));
	}

}
