/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http;

import org.junit.Test;

import org.springframework.core.io.Resource;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class MediaTypeFactoryTests {

	@Test
	public void getMediaType() {
		assertEquals(MediaType.APPLICATION_XML, MediaTypeFactory.getMediaType("file.xml").get());
		assertEquals(MediaType.parseMediaType("application/javascript"), MediaTypeFactory.getMediaType("file.js").get());
		assertEquals(MediaType.parseMediaType("text/css"), MediaTypeFactory.getMediaType("file.css").get());
		assertFalse(MediaTypeFactory.getMediaType("file.foobar").isPresent());
	}

	@Test
	public void nullParameter() {
		assertFalse(MediaTypeFactory.getMediaType((String) null).isPresent());
		assertFalse(MediaTypeFactory.getMediaType((Resource) null).isPresent());
		assertTrue(MediaTypeFactory.getMediaTypes(null).isEmpty());
	}

}