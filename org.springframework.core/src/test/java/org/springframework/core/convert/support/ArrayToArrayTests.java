/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.core.convert.support;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.core.convert.TypeDescriptor;

/**
 * @author Keith Donald
 */
public class ArrayToArrayTests {
	
	@Test
	public void testArrayToArrayConversion() {
		DefaultConversionService service = new DefaultConversionService();
		ArrayToArray c = new ArrayToArray(TypeDescriptor.valueOf(String[].class), TypeDescriptor.valueOf(Integer[].class), service);
		Integer[] result = (Integer[]) c.execute(new String[] { "1", "2", "3" });
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

}
