/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class BeanWrapperAutoGrowingTests {

	Bean bean = new Bean();

	BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);

	@Before
	public void setUp() {
		wrapper.setAutoGrowNestedPaths(true);
	}

	@Test
	public void getPropertyValueNullValueInNestedPath() {
		assertNull(wrapper.getPropertyValue("nested.prop"));
	}

	@Test
	public void setPropertyValueNullValueInNestedPath() {
		wrapper.setPropertyValue("nested.prop", "test");
		assertEquals("test", bean.getNested().getProp());
	}

	@Test(expected=NullValueInNestedPathException.class)
	public void getPropertyValueNullValueInNestedPathNoDefaultConstructor() {
		wrapper.getPropertyValue("nestedNoConstructor.prop");
	}

	@Test
	public void getPropertyValueAutoGrowArray() {
		assertNotNull(wrapper.getPropertyValue("array[0]"));
		assertEquals(1, bean.getArray().length);
		assertTrue(bean.getArray()[0] instanceof Bean);
	}

	@Test
	public void setPropertyValueAutoGrowArray() {
		wrapper.setPropertyValue("array[0].prop", "test");
		assertEquals("test", bean.getArray()[0].getProp());
	}

	@Test
	public void getPropertyValueAutoGrowArrayBySeveralElements() {
		assertNotNull(wrapper.getPropertyValue("array[4]"));
		assertEquals(5, bean.getArray().length);
		assertTrue(bean.getArray()[0] instanceof Bean);
		assertTrue(bean.getArray()[1] instanceof Bean);
		assertTrue(bean.getArray()[2] instanceof Bean);
		assertTrue(bean.getArray()[3] instanceof Bean);
		assertTrue(bean.getArray()[4] instanceof Bean);
		assertNotNull(wrapper.getPropertyValue("array[0]"));
		assertNotNull(wrapper.getPropertyValue("array[1]"));
		assertNotNull(wrapper.getPropertyValue("array[2]"));
		assertNotNull(wrapper.getPropertyValue("array[3]"));
	}

	@Test
	public void getPropertyValueAutoGrowMultiDimensionalArray() {
		assertNotNull(wrapper.getPropertyValue("multiArray[0][0]"));
		assertEquals(1, bean.getMultiArray()[0].length);
		assertTrue(bean.getMultiArray()[0][0] instanceof Bean);
	}

	@Test
	public void getPropertyValueAutoGrowList() {
		assertNotNull(wrapper.getPropertyValue("list[0]"));
		assertEquals(1, bean.getList().size());
		assertTrue(bean.getList().get(0) instanceof Bean);
	}

	@Test
	public void setPropertyValueAutoGrowList() {
		wrapper.setPropertyValue("list[0].prop", "test");
		assertEquals("test", bean.getList().get(0).getProp());
	}

	@Test
	public void getPropertyValueAutoGrowListBySeveralElements() {
		assertNotNull(wrapper.getPropertyValue("list[4]"));
		assertEquals(5, bean.getList().size());
		assertTrue(bean.getList().get(0) instanceof Bean);
		assertTrue(bean.getList().get(1) instanceof Bean);
		assertTrue(bean.getList().get(2) instanceof Bean);
		assertTrue(bean.getList().get(3) instanceof Bean);
		assertTrue(bean.getList().get(4) instanceof Bean);
		assertNotNull(wrapper.getPropertyValue("list[0]"));
		assertNotNull(wrapper.getPropertyValue("list[1]"));
		assertNotNull(wrapper.getPropertyValue("list[2]"));
		assertNotNull(wrapper.getPropertyValue("list[3]"));
	}

	@Test
	public void getPropertyValueAutoGrowListFailsAgainstLimit() {
		wrapper.setAutoGrowCollectionLimit(2);
		try {
			assertNotNull(wrapper.getPropertyValue("list[4]"));
			fail("Should have thrown InvalidPropertyException");
		}
		catch (InvalidPropertyException ex) {
			// expected
			assertTrue(ex.getRootCause() instanceof IndexOutOfBoundsException);
		}
	}

	@Test
	public void getPropertyValueAutoGrowMultiDimensionalList() {
		assertNotNull(wrapper.getPropertyValue("multiList[0][0]"));
		assertEquals(1, bean.getMultiList().get(0).size());
		assertTrue(bean.getMultiList().get(0).get(0) instanceof Bean);
	}

	@Test(expected=InvalidPropertyException.class)
	public void getPropertyValueAutoGrowListNotParameterized() {
		wrapper.getPropertyValue("listNotParameterized[0]");
	}

	@Test
	public void setPropertyValueAutoGrowMap() {
		wrapper.setPropertyValue("map[A]", new Bean());
		assertTrue(bean.getMap().get("A") instanceof Bean);
	}

	@Test
	public void setNestedPropertyValueAutoGrowMap() {
		wrapper.setPropertyValue("map[A].nested", new Bean());
		assertTrue(bean.getMap().get("A").getNested() instanceof Bean);
	}


	public static class Bean {

		private String prop;

		private Bean nested;

		private NestedNoDefaultConstructor nestedNoConstructor;

		private Bean[] array;

		private Bean[][] multiArray;

		private List<Bean> list;

		private List<List<Bean>> multiList;

		private List listNotParameterized;

		private Map<String, Bean> map;

		public String getProp() {
			return prop;
		}

		public void setProp(String prop) {
			this.prop = prop;
		}

		public Bean getNested() {
			return nested;
		}

		public void setNested(Bean nested) {
			this.nested = nested;
		}

		public Bean[] getArray() {
			return array;
		}

		public void setArray(Bean[] array) {
			this.array = array;
		}

		public Bean[][] getMultiArray() {
			return multiArray;
		}

		public void setMultiArray(Bean[][] multiArray) {
			this.multiArray = multiArray;
		}

		public List<Bean> getList() {
			return list;
		}

		public void setList(List<Bean> list) {
			this.list = list;
		}

		public List<List<Bean>> getMultiList() {
			return multiList;
		}

		public void setMultiList(List<List<Bean>> multiList) {
			this.multiList = multiList;
		}

		public NestedNoDefaultConstructor getNestedNoConstructor() {
			return nestedNoConstructor;
		}

		public void setNestedNoConstructor(NestedNoDefaultConstructor nestedNoConstructor) {
			this.nestedNoConstructor = nestedNoConstructor;
		}

		public List getListNotParameterized() {
			return listNotParameterized;
		}

		public void setListNotParameterized(List listNotParameterized) {
			this.listNotParameterized = listNotParameterized;
		}

		public Map<String, Bean> getMap() {
			return map;
		}

		public void setMap(Map<String, Bean> map) {
			this.map = map;
		}
	}


	public static class NestedNoDefaultConstructor {

		private NestedNoDefaultConstructor() {
		}
	}

}
