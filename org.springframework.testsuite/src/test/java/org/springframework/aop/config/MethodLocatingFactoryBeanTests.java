/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.aop.config;

import java.lang.reflect.Method;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.mock.easymock.AbstractScalarMockTemplate;
import org.springframework.test.AssertThrows;

/**
 * @author Rick Evans
 */
public final class MethodLocatingFactoryBeanTests extends TestCase {

	private static final String BEAN_NAME = "string";


	public void testIsSingleton() throws Exception {
		MethodLocatingFactoryBean factory = new MethodLocatingFactoryBean();
		assertTrue(factory.isSingleton());
	}

	public void testGetObjectType() throws Exception {
		MethodLocatingFactoryBean factory = new MethodLocatingFactoryBean();
		assertEquals(Method.class, factory.getObjectType());
	}

	public void testWithNullTargetBeanName() throws Exception {
		new BeanFactoryScalarMockTemplate() {
			public void doTestInternal(final BeanFactory beanFactory) throws Exception {
				new AssertThrows(IllegalArgumentException.class) {
					public void test() throws Exception {
						MethodLocatingFactoryBean factory = new MethodLocatingFactoryBean();
						factory.setMethodName("toString()");
						factory.setBeanFactory(beanFactory);
					}
				}.runTest();
			}
		}.test();
	}

	public void testWithEmptyTargetBeanName() throws Exception {
		new BeanFactoryScalarMockTemplate() {
			public void doTestInternal(final BeanFactory beanFactory) throws Exception {
				new AssertThrows(IllegalArgumentException.class) {
					public void test() throws Exception {
						MethodLocatingFactoryBean factory = new MethodLocatingFactoryBean();
						factory.setTargetBeanName("");
						factory.setMethodName("toString()");
						factory.setBeanFactory(beanFactory);
					}
				}.runTest();
			}
		}.test();
	}

	public void testWithNullTargetMethodName() throws Exception {
		new BeanFactoryScalarMockTemplate() {
			public void doTestInternal(final BeanFactory beanFactory) throws Exception {
				new AssertThrows(IllegalArgumentException.class) {
					public void test() throws Exception {
						MethodLocatingFactoryBean factory = new MethodLocatingFactoryBean();
						factory.setTargetBeanName(BEAN_NAME);
						factory.setBeanFactory(beanFactory);
					}
				}.runTest();
			}
		}.test();
	}

	public void testWithEmptyTargetMethodName() throws Exception {
		new BeanFactoryScalarMockTemplate() {
			public void doTestInternal(final BeanFactory beanFactory) throws Exception {
				new AssertThrows(IllegalArgumentException.class) {
					public void test() throws Exception {
						MethodLocatingFactoryBean factory = new MethodLocatingFactoryBean();
						factory.setTargetBeanName(BEAN_NAME);
						factory.setMethodName("");
						factory.setBeanFactory(beanFactory);
					}
				}.runTest();
			}
		}.test();
	}

	public void testWhenTargetBeanClassCannotBeResolved() throws Exception {
		new BeanFactoryScalarMockTemplate() {
			protected void setupBeanFactoryExpectations(MockControl mockControl, BeanFactory beanFactory) throws Exception {
				beanFactory.getType(BEAN_NAME);
				mockControl.setReturnValue(null);
			}
			protected void doTestInternal(final BeanFactory beanFactory) throws Exception {
				new AssertThrows(IllegalArgumentException.class) {
					public void test() throws Exception {
						MethodLocatingFactoryBean factory = new MethodLocatingFactoryBean();
						factory.setTargetBeanName(BEAN_NAME);
						factory.setMethodName("toString()");
						factory.setBeanFactory(beanFactory);
					}
				}.runTest();
			}
		}.test();
	}

	public void testSunnyDayPath() throws Exception {
		new BeanFactoryScalarMockTemplate() {
			protected void setupBeanFactoryExpectations(MockControl mockControl, BeanFactory beanFactory) throws Exception {
				beanFactory.getType(BEAN_NAME);
				mockControl.setReturnValue(String.class);
			}
			protected void doTestInternal(final BeanFactory beanFactory) throws Exception {
				MethodLocatingFactoryBean factory = new MethodLocatingFactoryBean();
				factory.setTargetBeanName(BEAN_NAME);
				factory.setMethodName("toString()");
				factory.setBeanFactory(beanFactory);
				Object result = factory.getObject();
				assertNotNull(result);
				assertTrue(result instanceof Method);
				Method method = (Method) result;
				assertEquals("Bingo", method.invoke("Bingo", new Object[]{}));
			}
		}.test();
	}

	public void testWhereMethodCannotBeResolved() throws Exception {
		new BeanFactoryScalarMockTemplate() {
			protected void setupBeanFactoryExpectations(MockControl mockControl, BeanFactory beanFactory) throws Exception {
				beanFactory.getType(BEAN_NAME);
				mockControl.setReturnValue(String.class);
			}
			protected void doTestInternal(final BeanFactory beanFactory) throws Exception {
				new AssertThrows(IllegalArgumentException.class) {
					public void test() throws Exception {
						MethodLocatingFactoryBean factory = new MethodLocatingFactoryBean();
						factory.setTargetBeanName(BEAN_NAME);
						factory.setMethodName("loadOfOld()");
						factory.setBeanFactory(beanFactory);
					}
				}.runTest();
			}
		}.test();
	}


	private static abstract class BeanFactoryScalarMockTemplate extends AbstractScalarMockTemplate {

		public BeanFactoryScalarMockTemplate() {
			super(BeanFactory.class);
		}

		public void setupExpectations(MockControl mockControl, Object mockObject) throws Exception {
			setupBeanFactoryExpectations(mockControl, (BeanFactory) mockObject);
		}

		public void doTest(Object mockObject) throws Exception {
			doTestInternal((BeanFactory) mockObject);
		}

		protected void setupBeanFactoryExpectations(MockControl mockControl, BeanFactory beanFactory) throws Exception {
		}

		protected abstract void doTestInternal(final BeanFactory beanFactory) throws Exception;
	}

}
