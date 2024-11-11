/*
 * Copyright 2002-2024 the original author or authors.
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class BeanWrapperAutoGrowingTests {

	private final Bean bean = new Bean();

	private final BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);


	@BeforeEach
	void setup() {
		wrapper.setAutoGrowNestedPaths(true);
	}


	@Test
	void getPropertyValueNullValueInNestedPath() {
		assertThat(wrapper.getPropertyValue("nested.prop")).isNull();
	}

	@Test
	void setPropertyValueNullValueInNestedPath() {
		wrapper.setPropertyValue("nested.prop", "test");
		assertThat(bean.getNested().getProp()).isEqualTo("test");
	}

	@Test
	void getPropertyValueNullValueInNestedPathNoDefaultConstructor() {
		assertThatExceptionOfType(NullValueInNestedPathException.class).isThrownBy(() ->
				wrapper.getPropertyValue("nestedNoConstructor.prop"));
	}

	@Test
	void getPropertyValueAutoGrowArray() {
		assertThat(wrapper.getPropertyValue("array[0]")).isNotNull();
		assertThat(bean.getArray()).hasSize(1);
		assertThat(bean.getArray()[0]).isInstanceOf(Bean.class);
	}

	@Test
	void setPropertyValueAutoGrowArray() {
		wrapper.setPropertyValue("array[0].prop", "test");
		assertThat(bean.getArray()[0].getProp()).isEqualTo("test");
	}

	@Test
	void getPropertyValueAutoGrowArrayBySeveralElements() {
		assertThat(wrapper.getPropertyValue("array[4]")).isNotNull();
		assertThat(bean.getArray()).hasSize(5);
		assertThat(bean.getArray()[0]).isInstanceOf(Bean.class);
		assertThat(bean.getArray()[1]).isInstanceOf(Bean.class);
		assertThat(bean.getArray()[2]).isInstanceOf(Bean.class);
		assertThat(bean.getArray()[3]).isInstanceOf(Bean.class);
		assertThat(bean.getArray()[4]).isInstanceOf(Bean.class);
		assertThat(wrapper.getPropertyValue("array[0]")).isNotNull();
		assertThat(wrapper.getPropertyValue("array[1]")).isNotNull();
		assertThat(wrapper.getPropertyValue("array[2]")).isNotNull();
		assertThat(wrapper.getPropertyValue("array[3]")).isNotNull();
	}

	@Test
	void getPropertyValueAutoGrow2dArray() {
		assertThat(wrapper.getPropertyValue("multiArray[0][0]")).isNotNull();
		assertThat(bean.getMultiArray()[0]).hasSize(1);
		assertThat(bean.getMultiArray()[0][0]).isInstanceOf(Bean.class);
	}

	@Test
	void getPropertyValueAutoGrow3dArray() {
		assertThat(wrapper.getPropertyValue("threeDimensionalArray[1][2][3]")).isNotNull();
		assertThat(bean.getThreeDimensionalArray()[1]).hasNumberOfRows(3);
		assertThat(bean.getThreeDimensionalArray()[1][2][3]).isInstanceOf(Bean.class);
	}

	@Test
	void setPropertyValueAutoGrow2dArray() {
		Bean newBean = new Bean();
		newBean.setProp("enigma");
		wrapper.setPropertyValue("multiArray[2][3]", newBean);
		assertThat(bean.getMultiArray()[2][3])
			.isInstanceOf(Bean.class)
			.extracting(Bean::getProp).isEqualTo("enigma");
	}

	@Test
	void setPropertyValueAutoGrow3dArray() {
		Bean newBean = new Bean();
		newBean.setProp("enigma");
		wrapper.setPropertyValue("threeDimensionalArray[2][3][4]", newBean);
		assertThat(bean.getThreeDimensionalArray()[2][3][4])
			.isInstanceOf(Bean.class)
			.extracting(Bean::getProp).isEqualTo("enigma");
	}

	@Test
	void getPropertyValueAutoGrowList() {
		assertThat(wrapper.getPropertyValue("list[0]")).isNotNull();
		assertThat(bean.getList()).hasSize(1);
		assertThat(bean.getList()).element(0).isInstanceOf(Bean.class);
	}

	@Test
	void setPropertyValueAutoGrowList() {
		wrapper.setPropertyValue("list[0].prop", "test");
		assertThat(bean.getList().get(0).getProp()).isEqualTo("test");
	}

	@Test
	void getPropertyValueAutoGrowListBySeveralElements() {
		assertThat(wrapper.getPropertyValue("list[4]")).isNotNull();
		assertThat(bean.getList()).hasSize(5).allSatisfy(entry ->
				assertThat(entry).isInstanceOf(Bean.class));
		assertThat(wrapper.getPropertyValue("list[0]")).isNotNull();
		assertThat(wrapper.getPropertyValue("list[1]")).isNotNull();
		assertThat(wrapper.getPropertyValue("list[2]")).isNotNull();
		assertThat(wrapper.getPropertyValue("list[3]")).isNotNull();
	}

	@Test
	void getPropertyValueAutoGrowListFailsAgainstLimit() {
		wrapper.setAutoGrowCollectionLimit(2);
		assertThatExceptionOfType(InvalidPropertyException.class)
				.isThrownBy(() -> wrapper.getPropertyValue("list[4]"))
				.withRootCauseInstanceOf(IndexOutOfBoundsException.class);
	}

	@Test
	void getPropertyValueAutoGrowNestedList() {
		assertThat(wrapper.getPropertyValue("nestedList[0][0]")).isNotNull();
		assertThat(bean.getNestedList()).hasSize(1);
		assertThat(bean.getNestedList().get(0)).singleElement().isInstanceOf(Bean.class);
	}

	@Test
	void getPropertyValueAutoGrowNestedNestedList() {
		assertThat(wrapper.getPropertyValue("nestedNestedList[0][0][0]")).isNotNull();
		assertThat(bean.getNestedNestedList()).hasSize(1);
		assertThat(bean.getNestedNestedList().get(0).get(0)).singleElement().isInstanceOf(Bean.class);
	}

	@Test
	void getPropertyValueAutoGrowListNotParameterized() {
		assertThatExceptionOfType(InvalidPropertyException.class).isThrownBy(() ->
				wrapper.getPropertyValue("listNotParameterized[0]"));
	}

	@Test
	void setPropertyValueAutoGrowMap() {
		wrapper.setPropertyValue("map[A]", new Bean());
		assertThat(bean.getMap().get("A")).isInstanceOf(Bean.class);
	}

	@Test
	void setPropertyValueAutoGrowMapNestedValue() {
		wrapper.setPropertyValue("map[A].nested", new Bean());
		assertThat(bean.getMap().get("A").getNested()).isInstanceOf(Bean.class);
	}

	@Test
	void setPropertyValueAutoGrowNestedMapWithinMap() {
		wrapper.setPropertyValue("nestedMap[A][B]", new Bean());
		assertThat(bean.getNestedMap().get("A").get("B")).isInstanceOf(Bean.class);
	}

	@Test @Disabled  // gh-32154
	void setPropertyValueAutoGrowNestedNestedMapWithinMap() {
		wrapper.setPropertyValue("nestedNestedMap[A][B][C]", new Bean());
		assertThat(bean.getNestedNestedMap().get("A").get("B").get("C")).isInstanceOf(Bean.class);
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

		private List<List<Bean>> nestedList;

		private List<List<List<Bean>>> nestedNestedList;

		private List listNotParameterized;

		private Map<String, Bean> map;

		private Map<String, Map<String, Bean>> nestedMap;

		private Map<String, Map<String, Map<String, Bean>>> nestedNestedMap;

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

		public List<List<Bean>> getNestedList() {
			return nestedList;
		}

		public void setNestedList(List<List<Bean>> nestedList) {
			this.nestedList = nestedList;
		}

		public List<List<List<Bean>>> getNestedNestedList() {
			return nestedNestedList;
		}

		public void setNestedNestedList(List<List<List<Bean>>> nestedNestedList) {
			this.nestedNestedList = nestedNestedList;
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

		public Map<String, Map<String, Bean>> getNestedMap() {
			return nestedMap;
		}

		public void setNestedMap(Map<String, Map<String, Bean>> nestedMap) {
			this.nestedMap = nestedMap;
		}

		public Map<String, Map<String, Map<String, Bean>>> getNestedNestedMap() {
			return nestedNestedMap;
		}

		public void setNestedNestedMap(Map<String, Map<String, Map<String, Bean>>> nestedNestedMap) {
			this.nestedNestedMap = nestedNestedMap;
		}
	}


	public static class NestedNoDefaultConstructor {

		private NestedNoDefaultConstructor() {
		}
	}

}
