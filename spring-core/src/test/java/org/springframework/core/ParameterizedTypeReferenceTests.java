/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
