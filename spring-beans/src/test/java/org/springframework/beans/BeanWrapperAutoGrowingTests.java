/*
 * Copyright 2002-2021 the original author or authors.
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
 * @author Sam Brannen
 */
public class BeanWrapperAutoGrowingTests {

	private final Bean bean = new Bean();

	private final BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);


	@BeforeEach
	public void setup() {
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
	public void getPropertyValueAutoGrow2dArray() {
		assertNotNull(wrapper.getPropertyValue("multiArray[0][0]"));
		assertThat(bean.getMultiArray()[0].length).isEqualTo(1);
		assertThat(bean.getMultiArray()[0][0]).isInstanceOf(Bean.class);
	}

	@Test
	public void getPropertyValueAutoGrow3dArray() {
		assertNotNull(wrapper.getPropertyValue("threeDimensionalArray[1][2][3]"));
		assertThat(bean.getThreeDimensionalArray()[1].length).isEqualTo(3);
		assertThat(bean.getThreeDimensionalArray()[1][2][3]).isInstanceOf(Bean.class);
	}

	@Test
	public void setPropertyValueAutoGrow2dArray() {
		Bean newBean = new Bean();
		newBean.setProp("enigma");
		wrapper.setPropertyValue("multiArray[2][3]", newBean);
		assertThat(bean.getMultiArray()[2][3])
			.isInstanceOf(Bean.class)
			.extracting(Bean::getProp).isEqualTo("enigma");
	}

	@Test
	public void setPropertyValueAutoGrow3dArray() {
		Bean newBean = new Bean();
		newBean.setProp("enigma");
		wrapper.setPropertyValue("threeDimensionalArray[2][3][4]", newBean);
		assertThat(bean.getThreeDimensionalArray()[2][3][4])
			.isInstanceOf(Bean.class)
			.extracting(Bean::getProp).isEqualTo("enigma");
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
				wrapper.getPropertyValue("list[4]"))
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


	private static void assertNotNull(Object propertyValue) {
		assertThat(propertyValue).isNotNull();
	}


	@SuppressWarnings("rawtypes")
	public static class Bean {

		private String prop;

		private Bean nested;

		private NestedNoDefaultConstructor nestedNoConstructor;

		private Bean[] array;

		private Bean[][] multiArray;

		private Bean[][][] threeDimensionalArray;

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

		public Bean[][][] getThreeDimensionalArray() {
			return threeDimensionalArray;
		}

		public void setThreeDimensionalArray(Bean[][][] threeDimensionalArray) {
			this.threeDimensionalArray = threeDimensionalArray;
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
