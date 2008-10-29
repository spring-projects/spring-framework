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

package org.springframework.beans.factory.config;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.easymock.AbstractScalarMockTemplate;
import org.springframework.test.AssertThrows;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class CustomScopeConfigurerTests extends TestCase {

	private static final String FOO_SCOPE = "fooScope";


	public void testWithNoScopes() throws Exception {
		new ConfigurableListableBeanFactoryMockTemplate() {
			protected void doTest(ConfigurableListableBeanFactory factory) {
				CustomScopeConfigurer figurer = new CustomScopeConfigurer();
				figurer.postProcessBeanFactory(factory);
			}
		}.test();
	}

	public void testSunnyDayWithBonaFideScopeInstance() throws Exception {
		MockControl mockScope = MockControl.createControl(Scope.class);
		final Scope scope = (Scope) mockScope.getMock();
		mockScope.replay();
		new ConfigurableListableBeanFactoryMockTemplate() {
			public void setupExpectations(MockControl mockControl, ConfigurableListableBeanFactory factory) {
				factory.registerScope(FOO_SCOPE, scope);
			}
			protected void doTest(ConfigurableListableBeanFactory factory) {
				Map scopes = new HashMap();
				scopes.put(FOO_SCOPE, scope);
				CustomScopeConfigurer figurer = new CustomScopeConfigurer();
				figurer.setScopes(scopes);
				figurer.postProcessBeanFactory(factory);
			}
		}.test();
		mockScope.verify();
	}

	public void testSunnyDayWithBonaFideScopeClass() throws Exception {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		Map scopes = new HashMap();
		scopes.put(FOO_SCOPE, NoOpScope.class);
		CustomScopeConfigurer figurer = new CustomScopeConfigurer();
		figurer.setScopes(scopes);
		figurer.postProcessBeanFactory(factory);
		assertTrue(factory.getRegisteredScope(FOO_SCOPE) instanceof NoOpScope);
	}

	public void testSunnyDayWithBonaFideScopeClassname() throws Exception {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		Map scopes = new HashMap();
		scopes.put(FOO_SCOPE, NoOpScope.class.getName());
		CustomScopeConfigurer figurer = new CustomScopeConfigurer();
		figurer.setScopes(scopes);
		figurer.postProcessBeanFactory(factory);
		assertTrue(factory.getRegisteredScope(FOO_SCOPE) instanceof NoOpScope);
	}

	public void testWhereScopeMapHasNullScopeValueInEntrySet() throws Exception {
		new ConfigurableListableBeanFactoryMockTemplate() {
			protected void doTest(final ConfigurableListableBeanFactory factory) {
				new AssertThrows(IllegalArgumentException.class) {
					public void test() throws Exception {
						Map scopes = new HashMap();
						scopes.put(FOO_SCOPE, null);
						CustomScopeConfigurer figurer = new CustomScopeConfigurer();
						figurer.setScopes(scopes);
						figurer.postProcessBeanFactory(factory);
					}
				}.runTest();
			}
		}.test();
	}

	public void testWhereScopeMapHasNonScopeInstanceInEntrySet() throws Exception {
		new ConfigurableListableBeanFactoryMockTemplate() {
			protected void doTest(final ConfigurableListableBeanFactory factory) {
				new AssertThrows(IllegalArgumentException.class) {
					public void test() throws Exception {
						Map scopes = new HashMap();
						scopes.put(FOO_SCOPE, this); // <-- not a valid value...
						CustomScopeConfigurer figurer = new CustomScopeConfigurer();
						figurer.setScopes(scopes);
						figurer.postProcessBeanFactory(factory);
					}
				}.runTest();
			}
		}.test();
	}

	public void testWhereScopeMapHasNonStringTypedScopeNameInKeySet() throws Exception {
		new ConfigurableListableBeanFactoryMockTemplate() {
			protected void doTest(final ConfigurableListableBeanFactory factory) {
				new AssertThrows(IllegalArgumentException.class) {
					public void test() throws Exception {
						Map scopes = new HashMap();
						scopes.put(this, new NoOpScope()); // <-- not a valid value (the key)...
						CustomScopeConfigurer figurer = new CustomScopeConfigurer();
						figurer.setScopes(scopes);
						figurer.postProcessBeanFactory(factory);
					}
				}.runTest();
			}
		}.test();
	}


	private abstract class ConfigurableListableBeanFactoryMockTemplate extends AbstractScalarMockTemplate {

		public ConfigurableListableBeanFactoryMockTemplate() {
			super(ConfigurableListableBeanFactory.class);
		}

		public final void setupExpectations(MockControl mockControl, Object mockObject) throws Exception {
			setupExpectations(mockControl, (ConfigurableListableBeanFactory) mockObject);
		}

		public final void doTest(Object mockObject) throws Exception {
			doTest((ConfigurableListableBeanFactory) mockObject);
		}

		public void setupExpectations(MockControl mockControl, ConfigurableListableBeanFactory factory) throws Exception {
		}

		protected abstract void doTest(ConfigurableListableBeanFactory factory) throws Exception;
	}

}
