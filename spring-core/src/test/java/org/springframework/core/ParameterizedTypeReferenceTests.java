/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link ParameterizedTypeReference}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
class ParameterizedTypeReferenceTests {

	@Test
	void stringTypeReference() {
		ParameterizedTypeReference<String> typeReference = new ParameterizedTypeReference<>() {};
		assertThat(typeReference.getType()).isEqualTo(String.class);
	}

	@Test
	void mapTypeReference() throws Exception {
		Type mapType = getClass().getMethod("mapMethod").getGenericReturnType();
		ParameterizedTypeReference<Map<Object,String>> typeReference = new ParameterizedTypeReference<>() {};
		assertThat(typeReference.getType()).isEqualTo(mapType);
	}

	@Test
	void listTypeReference() throws Exception {
		Type listType = getClass().getMethod("listMethod").getGenericReturnType();
		ParameterizedTypeReference<List<String>> typeReference = new ParameterizedTypeReference<>() {};
		assertThat(typeReference.getType()).isEqualTo(listType);
	}

	@Test
	void reflectiveTypeReferenceWithSpecificDeclaration() throws Exception{
		Type listType = getClass().getMethod("listMethod").getGenericReturnType();
		ParameterizedTypeReference<List<String>> typeReference = ParameterizedTypeReference.forType(listType);
		assertThat(typeReference.getType()).isEqualTo(listType);
	}

	@Test
	void reflectiveTypeReferenceWithGenericDeclaration() throws Exception{
		Type listType = getClass().getMethod("listMethod").getGenericReturnType();
		ParameterizedTypeReference<?> typeReference = ParameterizedTypeReference.forType(listType);
		assertThat(typeReference.getType()).isEqualTo(listType);
	}


	public static Map<Object, String> mapMethod() {
		return null;
	}

	public static List<String> listMethod() {
		return null;
	}

}
