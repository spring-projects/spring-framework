/*
 * Copyright 2011-2012 the original author or authors.
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
package org.springframework.beans.type;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for {@link ParameterizedTypeInformation}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class ParameterizedTypeUnitTests {

	@Mock
	ParameterizedType one;

	@Before
	public void setUp() {
		when(one.getActualTypeArguments()).thenReturn(new Type[0]);
	}

	@Test
	public void considersTypeInformationsWithDifferingParentsNotEqual() {

		TypeDiscoverer<String> stringParent = new TypeDiscoverer<String>(String.class, null);
		TypeDiscoverer<Object> objectParent = new TypeDiscoverer<Object>(Object.class, null);

		ParameterizedTypeInformation<Object> first = new ParameterizedTypeInformation<Object>(one, stringParent);
		ParameterizedTypeInformation<Object> second = new ParameterizedTypeInformation<Object>(one, objectParent);

		assertFalse(first.equals(second));
	}

	@Test
	public void considersTypeInformationsWithSameParentsNotEqual() {

		TypeDiscoverer<String> stringParent = new TypeDiscoverer<String>(String.class, null);

		ParameterizedTypeInformation<Object> first = new ParameterizedTypeInformation<Object>(one, stringParent);
		ParameterizedTypeInformation<Object> second = new ParameterizedTypeInformation<Object>(one, stringParent);

		assertTrue(first.equals(second));
	}

	/**
	 * @see DATACMNS-88
	 */
	@Test
	public void resolvesMapValueTypeCorrectly() {

		TypeInformation<Foo> type = ClassTypeInformation.from(Foo.class);
		TypeInformation<?> propertyType = type.getProperty("param");
		assertThat(propertyType.getProperty("value").getType(), is(typeCompatibleWith(String.class)));
		assertThat(propertyType.getMapValueType().getType(), is(typeCompatibleWith(String.class)));

		propertyType = type.getProperty("param2");
		assertThat(propertyType.getProperty("value").getType(), is(typeCompatibleWith(String.class)));
		assertThat(propertyType.getMapValueType().getType(), is(typeCompatibleWith(Locale.class)));
	}

	@SuppressWarnings("serial")
	class Localized<S> extends HashMap<Locale, S> {
		S value;
	}

	@SuppressWarnings("serial")
	class Localized2<S> extends HashMap<S, Locale> {
		S value;
	}

	class Foo {
		Localized<String> param;
		Localized2<String> param2;
	}
}
