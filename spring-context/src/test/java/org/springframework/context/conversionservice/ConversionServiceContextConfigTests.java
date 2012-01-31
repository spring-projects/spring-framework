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

package org.springframework.context.conversionservice;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Keith Donald
 */
public class ConversionServiceContextConfigTests {
	
	@Test
	public void testConfigOk() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("org/springframework/context/conversionservice/conversionService.xml");
		TestClient client = context.getBean("testClient", TestClient.class);
		assertEquals(2, client.getBars().size());
		assertEquals("value1", client.getBars().get(0).getValue());
		assertEquals("value2", client.getBars().get(1).getValue());
		assertTrue(client.isBool());
	}

}
