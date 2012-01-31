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

import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.junit.Test;

/**
 * Unit tests for {@link CommonsLogFactoryBean}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public final class CommonsLogFactoryBeanTests {

	@Test
	public void testIsSingleton() throws Exception {
		CommonsLogFactoryBean factory = new CommonsLogFactoryBean();
		assertTrue(factory.isSingleton());
	}

	@Test
	public void testGetObjectTypeDefaultsToPlainResourceInterfaceifLookupResourceIsNotSupplied() throws Exception {
		CommonsLogFactoryBean factory = new CommonsLogFactoryBean();
		assertEquals(Log.class, factory.getObjectType());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWhenLogNameIsMissing() throws Exception {
		CommonsLogFactoryBean factory = new CommonsLogFactoryBean();
		factory.afterPropertiesSet();
	}

	@Test
	public void testSunnyDayPath() throws Exception {
		CommonsLogFactoryBean factory = new CommonsLogFactoryBean();
		factory.setLogName("The Tin Drum");
		factory.afterPropertiesSet();
		Object object = factory.getObject();

		assertNotNull("As per FactoryBean contract, the return value of getObject() cannot be null.", object);
		assertTrue("Obviously not getting a Log back", Log.class.isAssignableFrom(object.getClass()));
	}

}
