/*
 * Copyright 2002-2013 the original author or authors.
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
import org.springframework.tests.sample.beans.CustomEnum;
import org.springframework.tests.sample.beans.GenericBean;

import static org.junit.Assert.*;

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

	@Test
	public void testCustomEnumArrayWithSingleValue() {
		GenericBean<?> gb = new GenericBean<Object>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumArray", "VALUE_1");
		assertEquals(1, gb.getCustomEnumArray().length);
		assertEquals(CustomEnum.VALUE_1, gb.getCustomEnumArray()[0]);
	}

	@Test
	public void testCustomEnumArrayWithMultipleValues() {
		GenericBean<?> gb = new GenericBean<Object>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumArray", new String[] {"VALUE_1", "VALUE_2"});
		assertEquals(2, gb.getCustomEnumArray().length);
		assertEquals(CustomEnum.VALUE_1, gb.getCustomEnumArray()[0]);
		assertEquals(CustomEnum.VALUE_2, gb.getCustomEnumArray()[1]);
	}

	@Test
	public void testCustomEnumArrayWithMultipleValuesAsCsv() {
		GenericBean<?> gb = new GenericBean<Object>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumArray", "VALUE_1,VALUE_2");
		assertEquals(2, gb.getCustomEnumArray().length);
		assertEquals(CustomEnum.VALUE_1, gb.getCustomEnumArray()[0]);
		assertEquals(CustomEnum.VALUE_2, gb.getCustomEnumArray()[1]);
	}

	@Test
	public void testCustomEnumSetWithSingleValue() {
		GenericBean<?> gb = new GenericBean<Object>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSet", "VALUE_1");
		assertEquals(1, gb.getCustomEnumSet().size());
		assertTrue(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1));
	}

	@Test
	public void testCustomEnumSetWithMultipleValues() {
		GenericBean<?> gb = new GenericBean<Object>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSet", new String[] {"VALUE_1", "VALUE_2"});
		assertEquals(2, gb.getCustomEnumSet().size());
		assertTrue(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1));
		assertTrue(gb.getCustomEnumSet().contains(CustomEnum.VALUE_2));
	}

	@Test
	public void testCustomEnumSetWithMultipleValuesAsCsv() {
		GenericBean<?> gb = new GenericBean<Object>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSet", "VALUE_1,VALUE_2");
		assertEquals(2, gb.getCustomEnumSet().size());
		assertTrue(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1));
		assertTrue(gb.getCustomEnumSet().contains(CustomEnum.VALUE_2));
	}

	@Test
	public void testCustomEnumSetWithGetterSetterMismatch() {
		GenericBean<?> gb = new GenericBean<Object>();
		BeanWrapper bw = new BeanWrapperImpl(gb);
		bw.setPropertyValue("customEnumSetMismatch", new String[] {"VALUE_1", "VALUE_2"});
		assertEquals(2, gb.getCustomEnumSet().size());
		assertTrue(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1));
		assertTrue(gb.getCustomEnumSet().contains(CustomEnum.VALUE_2));
	}

}
