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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link ContextConfiguration} annotation presented on inherited methods.
 * @author Sergei Ustimenko
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ContextConfigurationOnMethodTest.TypeContext.class)
public class ContextConfigurationAppliedOnSuperMethodTest
		extends ContextConfigurationOnMethodTest.ContextConfigurationOnMethodParent
		implements ContextConfigurationOnMethodTest.ContextConfigurationInterface,
		ContextConfigurationOnMethodTest.ContextConfigurationMetaInterface {

	@Autowired
	protected String bean;

	@Test
	@Override
	public void shouldPickUpParentBean() {
		assertEquals("parent", bean);
	}

	@Test
	@Override
	public void shouldPickUpParentParentBean() {
		assertEquals("parentParent", bean);
	}

	@Test
	@Override
	public void shouldPickUpMetaParent() {
		assertEquals("metaOnParent", bean);
	}

	@Test
	@Override
	public void shouldPickUpMetaParentParent() {
		assertEquals("metaOnParentParent", bean);
	}

	@Test
	@Override
	public void shouldPickUpConfigurationFromParentInterface() {
		assertEquals("parentInterface", bean);
	}

	@Test
	@Override
	public void shouldPickUpMetaFromParentInterface() {
		assertEquals("metaOnParentInterface", bean);
	}
}
