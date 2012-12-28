/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.util;

import java.util.LinkedList;

import junit.framework.*;
import junit.framework.Assert;

import org.springframework.beans.TestBean;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class AutoPopulatingListTests extends TestCase {

	public void testWithClass() throws Exception {
		doTestWithClass(new AutoPopulatingList<Object>(TestBean.class));
	}

	public void testWithClassAndUserSuppliedBackingList() throws Exception {
		doTestWithClass(new AutoPopulatingList<Object>(new LinkedList<Object>(), TestBean.class));
	}

	public void testWithElementFactory() throws Exception {
		doTestWithElementFactory(new AutoPopulatingList<Object>(new MockElementFactory()));
	}

	public void testWithElementFactoryAndUserSuppliedBackingList() throws Exception {
		doTestWithElementFactory(new AutoPopulatingList<Object>(new LinkedList<Object>(), new MockElementFactory()));
	}

	private void doTestWithClass(AutoPopulatingList<Object> list) {
		Object lastElement = null;
		for (int x = 0; x < 10; x++) {
			Object element = list.get(x);
			assertNotNull("Element is null", list.get(x));
			assertTrue("Element is incorrect type", element instanceof TestBean);
			assertNotSame(lastElement, element);
			lastElement = element;
		}

		String helloWorld = "Hello World!";
		list.add(10, null);
		list.add(11, helloWorld);
		assertEquals(helloWorld, list.get(11));

		assertTrue(list.get(10) instanceof TestBean);
		assertTrue(list.get(12) instanceof TestBean);
		assertTrue(list.get(13) instanceof TestBean);
		assertTrue(list.get(20) instanceof TestBean);
	}

	private void doTestWithElementFactory(AutoPopulatingList<Object> list) {
		doTestWithClass(list);

		for(int x = 0; x < list.size(); x++) {
			Object element = list.get(x);
			if(element instanceof TestBean) {
				junit.framework.Assert.assertEquals(x, ((TestBean) element).getAge());
			}
		}
	}

	public void testSerialization() throws Exception {
		AutoPopulatingList<?> list = new AutoPopulatingList<Object>(TestBean.class);
		Assert.assertEquals(list, SerializationTestUtils.serializeAndDeserialize(list));
	}


	private static class MockElementFactory implements AutoPopulatingList.ElementFactory {

		@Override
		public Object createElement(int index) {
			TestBean bean = new TestBean();
			bean.setAge(index);
			return bean;
		}
	}

}
