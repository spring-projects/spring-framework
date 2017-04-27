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

package org.springframework.http.codec.multipart;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Sebastien Deleuze
 */
public class MultipartHttpMessageReaderTests {

	private MultipartHttpMessageReader reader;

	@Before
	public void setUp() throws Exception {
		this.reader = (elementType, message, hints) -> {
			throw new UnsupportedOperationException();
		};
	}

	@Test
	public void canRead() {
		assertTrue(this.reader.canRead(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class),
				MediaType.MULTIPART_FORM_DATA));

		assertFalse(this.reader.canRead(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
				MediaType.MULTIPART_FORM_DATA));

		assertFalse(this.reader.canRead(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
				MediaType.MULTIPART_FORM_DATA));

		assertFalse(this.reader.canRead(
				ResolvableType.forClassWithGenerics(Map.class, String.class, String.class),
				MediaType.MULTIPART_FORM_DATA));

		assertFalse(this.reader.canRead(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class),
				MediaType.APPLICATION_FORM_URLENCODED));
	}

}
