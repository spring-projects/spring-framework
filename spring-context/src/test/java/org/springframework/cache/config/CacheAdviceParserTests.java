/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cache.config;

import org.junit.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.GenericXmlApplicationContext;

import static org.junit.Assert.*;

/**
 * AOP advice specific parsing tests.
 *
 * @author Stephane Nicoll
 */
public class CacheAdviceParserTests {

	@Test
	public void keyAndKeyGeneratorCannotBeSetTogether() {
		try {
			new GenericXmlApplicationContext("/org/springframework/cache/config/cache-advice-invalid.xml");
			fail("Should have failed to load context, one advise define both a key and a key generator");
		} catch (BeanDefinitionStoreException e) { // TODO better exception handling
		}
	}
}
