/*
 * Copyright 2002-2008 the original author or authors.
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

import static org.junit.Assert.*;

import org.junit.Test;

import test.beans.CustomEnum;
import test.beans.GenericBean;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public final class BeanWrapperEnumTests {

	@Test
	public void testCustomEnum() {
		GenericBean<?> gb = new GenericBean<Object>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", "VALUE_1");
		assertEquals(CustomEnum.VALUE_1, gb.getCustomEnum());
	}

	@Test
	public void testCustomEnumWithNull() {
		GenericBean<?> gb = new GenericBean<Object>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", null);
		assertEquals(null, gb.getCustomEnum());
	}

	@Test
	public void testCustomEnumWithEmptyString() {
		GenericBean<?> gb = new GenericBean<Object>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnum", "");
		assertEquals(null, gb.getCustomEnum());
	}

}
