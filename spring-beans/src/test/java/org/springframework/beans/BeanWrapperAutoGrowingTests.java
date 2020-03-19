/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class BeanWrapperAutoGrowingTests {

	private final Bean bean = new Bean();

	private final BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);


	@BeforeEach
	public void setUp() {
		wrapper.setAutoGrowNestedPaths(true);
	}


	@Test
	public void getPropertyValueNullValueInNestedPath() {
		assertThat(wrapper.getPropertyValue("nested.prop")).isNull();
	}

	@Test
	public void setPropertyValueNullValueInNestedPath() {
		wrapper.setPropertyValue("nested.prop", "test");
		assertThat(bean.getNested().getProp()).isEqualTo("test");
	}

	@Test
	public void getPropertyValueNullValueInNestedPathNoDefaultConstructor() {
		assertThatExceptionOfType(NullValueInNestedPathException.class).isThrownBy(() ->
				wrapper.getPropertyValue("nestedNoConstructor.prop"));
	}

	@Test
	public void getPropertyValueAutoGrowArray() {
		assertNotNull(wrapper.getPropertyValue("array[0]"));
		assertThat(bean.getArray().length).isEqualTo(1);
		assertThat(bean.getArray()[0]).isInstanceOf(Bean.class);
	}

	private void assertNotNull(Object propertyValue) {
		assertThat(propertyValue).isNotNull();
	}


	@Test
	public void setPropertyValueAutoGrowArray() {
		wrapper.setPropertyValue("array[0].prop", "test");
		assertThat(bean.getArray()[0].getProp()).isEqualTo("test");
	}

	@Test
	public void getPropertyValueAutoGrowArrayBySeveralElements() {
		assertNotNull(wrapper.getPropertyValue("array[4]"));
		assertThat(bean.getArray().length).isEqualTo(5);
		assertThat(bean.getArray()[0]).isInstanceOf(Bean.class);
		assertThat(bean.getArray()[1]).isInstanceOf(Bean.class);
		assertThat(bean.getArray()[2]).isInstanceOf(Bean.class);
		assertThat(bean.getArray()[3]).isInstanceOf(Bean.class);
		assertThat(bean.getArray()[4]).isInstanceOf(Bean.class);
		assertNotNull(wrapper.getPropertyValue("array[0]"));
		assertNotNull(wrapper.getPropertyValue("array[1]"));
		assertNotNull(wrapper.getPropertyValue("array[2]"));
		assertNotNull(wrapper.getPropertyValue("array[3]"));
	}

	@Test
	public void getPropertyValueAutoGrowMultiDimensionalArray() {
		assertNotNull(wrapper.getPropertyValue("multiArray[0][0]"));
		assertThat(bean.getMultiArray()[0].length).isEqualTo(1);
		assertThat(bean.getMultiArray()[0][0]).isInstanceOf(Bean.class);
	}

	@Test
	public void getPropertyValueAutoGrowList() {
		assertNotNull(wrapper.getPropertyValue("list[0]"));
		assertThat(bean.getList().size()).isEqualTo(1);
		assertThat(bean.getList().get(0)).isInstanceOf(Bean.class);
	}

	@Test
	public void setPropertyValueAutoGrowList() {
		wrapper.setPropertyValue("list[0].prop", "test");
		assertThat(bean.getList().get(0).getProp()).isEqualTo("test");
	}

	@Test
	public void getPropertyValueAutoGrowListBySeveralElements() {
		assertNotNull(wrapper.getPropertyValue("list[4]"));
		assertThat(bean.getList().size()).isEqualTo(5);
		assertThat(bean.getList().get(0)).isInstanceOf(Bean.class);
		assertThat(bean.getList().get(1)).isInstanceOf(Bean.class);
		assertThat(bean.getList().get(2)).isInstanceOf(Bean.class);
		assertThat(bean.getList().get(3)).isInstanceOf(Bean.class);
		assertThat(bean.getList().get(4)).isInstanceOf(Bean.class);
		assertNotNull(wrapper.getPropertyValue("list[0]"));
		assertNotNull(wrapper.getPropertyValue("list[1]"));
		assertNotNull(wrapper.getPropertyValue("list[2]"));
		assertNotNull(wrapper.getPropertyValue("list[3]"));
	}

	@Test
	public void getPropertyValueAutoGrowListFailsAgainstLimit() {
		wrapper.setAutoGrowCollectionLimit(2);
		assertThatExceptionOfType(InvalidPropertyException.class).isThrownBy(() ->
				assertNotNull(wrapper.getPropertyValue("list[4]")))
			.withRootCauseInstanceOf(IndexOutOfBoundsException.class);
	}

	@Test
	public void getPropertyValueAutoGrowMultiDimensionalList() {
		assertNotNull(wrapper.getPropertyValue("multiList[0][0]"));
		assertThat(bean.getMultiList().get(0).size()).isEqualTo(1);
		assertThat(bean.getMultiList().get(0).get(0)).isInstanceOf(Bean.class);
	}

	@Test
	public void getPropertyValueAutoGrowListNotParameterized() {
		assertThatExceptionOfType(InvalidPropertyException.class).isThrownBy(() ->
				wrapper.getPropertyValue("listNotParameterized[0]"));
	}

	@Test
	public void setPropertyValueAutoGrowMap() {
		wrapper.setPropertyValue("map[A]", new Bean());
		assertThat(bean.getMap().get("A")).isInstanceOf(Bean.class);
	}

	@Test
	public void setNestedPropertyValueAutoGrowMap() {
		wrapper.setPropertyValue("map[A].nested", new Bean());
		assertThat(bean.getMap().get("A").getNested()).isInstanceOf(Bean.class);
	}


	@SuppressWarnings("rawtypes")
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
