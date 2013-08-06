/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link ParameterizedTypeReference}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class ParameterizedTypeReferenceTests {

	@Test
	public void map() throws NoSuchMethodException {
		Type mapType = getClass().getMethod("mapMethod").getGenericReturnType();
		ParameterizedTypeReference<Map<Object,String>> mapTypeReference = new ParameterizedTypeReference<Map<Object,String>>() {};
		assertEquals(mapType, mapTypeReference.getType());
	}

	@Test
	public void list() throws NoSuchMethodException {
		Type mapType = getClass().getMethod("listMethod").getGenericReturnType();
		ParameterizedTypeReference<List<String>> mapTypeReference = new ParameterizedTypeReference<List<String>>() {};
		assertEquals(mapType, mapTypeReference.getType());
	}

	@Test
	public void string() {
		ParameterizedTypeReference<String> typeReference = new ParameterizedTypeReference<String>() {};
		assertEquals(String.class, typeReference.getType());
	}

	public static Map<Object, String> mapMethod() {
		return null;
	}

	public static List<String> listMethod() {
		return null;
	}

}
