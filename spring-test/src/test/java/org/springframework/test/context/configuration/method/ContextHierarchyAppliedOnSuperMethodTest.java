/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.configuration.method;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link ContextHierarchy} presented on inherited methods
 * @author Sergei Ustimenko
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ContextHierarchyOnMethodTest.MainContext.class)
public class ContextHierarchyAppliedOnSuperMethodTest
		extends ContextHierarchyOnMethodTest.ContextHierarchyOnMethodParent
		implements ContextHierarchyOnMethodTest.ContextHierarchyInterface,
		ContextHierarchyOnMethodTest.ContextHierarchyMetaInterface {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private String testBean;

	@Test
	@Override
	public void shouldLoadContextsInReverseOrderFromParentParent() {
		assertNotNull("child ApplicationContext", applicationContext);
		assertNotNull("parent ApplicationContext", applicationContext.getParent());
		assertNull("grandparent ApplicationContext", applicationContext.getParent().getParent());
		assertEquals("overridenParent", testBean);
	}


	@Test
	@Override
	public void shouldLoadContextsForMetaOnParentParent() {
		assertNotNull("child ApplicationContext", applicationContext);
		assertNotNull("parent ApplicationContext", applicationContext.getParent());
		assertNull("grandparent ApplicationContext", applicationContext.getParent().getParent());
		assertEquals("overridenMetaOnParent", testBean);
	}

	@Test
	@Override
	public void shouldPickUpHierarchyFromParentInterface() {
		assertEquals("parentInterface", testBean);
	}

	@Test
	@Override
	public void shouldPickUpMetaHierarchyFromParentInterface() {
		assertEquals("metaOnParentInterface", testBean);
	}

}
