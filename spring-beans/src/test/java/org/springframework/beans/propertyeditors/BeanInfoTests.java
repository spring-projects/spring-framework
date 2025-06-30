/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.beans.propertyeditors;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.FatalBeanException;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 06.03.2006
 */
class BeanInfoTests {

	@Test
	void testComplexObject() {
		ValueBean bean = new ValueBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		Integer value = 1;

		bw.setPropertyValue("value", value);
		assertThat(value).as("value not set correctly").isEqualTo(bean.getValue());

		value = 2;
		bw.setPropertyValue("value", value.toString());
		assertThat(value).as("value not converted").isEqualTo(bean.getValue());

		bw.setPropertyValue("value", null);
		assertThat(bean.getValue()).as("value not null").isNull();

		bw.setPropertyValue("value", "");
		assertThat(bean.getValue()).as("value not converted to null").isNull();
	}


	public static class ValueBean {

		private Integer value;

		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}
	}


	public static class ValueBeanBeanInfo extends SimpleBeanInfo {

		@Override
		public PropertyDescriptor[] getPropertyDescriptors() {
			try {
				PropertyDescriptor pd = new PropertyDescriptor("value", ValueBean.class);
				pd.setPropertyEditorClass(MyNumberEditor.class);
				return new PropertyDescriptor[] {pd};
			}
			catch (IntrospectionException ex) {
				throw new FatalBeanException("Couldn't create PropertyDescriptor", ex);
			}
		}
	}


	public static class MyNumberEditor extends CustomNumberEditor {

		private Object target;

		public MyNumberEditor() throws IllegalArgumentException {
			super(Integer.class, true);
		}

		public MyNumberEditor(Object target) throws IllegalArgumentException {
			super(Integer.class, true);
			this.target = target;
		}

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			Assert.isTrue(this.target instanceof ValueBean, "Target must be available");
			super.setAsText(text);
		}

	}

}
