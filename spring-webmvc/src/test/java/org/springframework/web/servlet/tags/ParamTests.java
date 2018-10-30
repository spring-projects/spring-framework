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

package org.springframework.web.servlet.tags;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Param}.
 *
 * @author Scott Andrews
 */
public class ParamTests {

	private final Param param = new Param();

	@Test
	public void name() {
		param.setName("name");
		assertEquals("name", param.getName());
	}

	@Test
	public void value() {
		param.setValue("value");
		assertEquals("value", param.getValue());
	}

	@Test
	public void nullDefaults() {
		assertNull(param.getName());
		assertNull(param.getValue());
	}

}
