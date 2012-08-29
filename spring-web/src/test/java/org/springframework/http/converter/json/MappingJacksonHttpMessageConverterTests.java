/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.http.converter.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;

/**
 * Jackson 1.x converter tests.
 *
 * @author Rossen Stoyanchev
 */
public class MappingJacksonHttpMessageConverterTests extends AbstractMappingJacksonHttpMessageConverterTests<MappingJacksonHttpMessageConverter> {

	@Override
	protected MappingJacksonHttpMessageConverter createConverter() {
		return new MappingJacksonHttpMessageConverter();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readGenerics() throws IOException {
		MappingJacksonHttpMessageConverter converter = new MappingJacksonHttpMessageConverter() {
			@Override
			protected JavaType getJavaType(Type type) {
				if (type instanceof Class && List.class.isAssignableFrom((Class<?>)type)) {
					return TypeFactory.collectionType(ArrayList.class, MyBean.class);
				}
				else {
					return super.getJavaType(type);
				}
			}
		};
		String body =
				"[{\"bytes\":\"AQI=\",\"array\":[\"Foo\",\"Bar\"],\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}]";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));

		List<MyBean> results = (List<MyBean>) converter.read(List.class, inputMessage);
		assertEquals(1, results.size());
		MyBean result = results.get(0);
		assertEquals("Foo", result.getString());
		assertEquals(42, result.getNumber());
		assertEquals(42F, result.getFraction(), 0F);
		assertArrayEquals(new String[]{"Foo", "Bar"}, result.getArray());
		assertTrue(result.isBool());
		assertArrayEquals(new byte[]{0x1, 0x2}, result.getBytes());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readParameterizedType() throws IOException {
		ParameterizedTypeReference<List<MyBean>> beansList = new ParameterizedTypeReference<List<MyBean>>() {};

		String body =
				"[{\"bytes\":\"AQI=\",\"array\":[\"Foo\",\"Bar\"],\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}]";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));

		MappingJacksonHttpMessageConverter converter = new MappingJacksonHttpMessageConverter();
		List<MyBean> results = (List<MyBean>) converter.read(beansList.getType(), inputMessage);
		assertEquals(1, results.size());
		MyBean result = results.get(0);
		assertEquals("Foo", result.getString());
		assertEquals(42, result.getNumber());
		assertEquals(42F, result.getFraction(), 0F);
		assertArrayEquals(new String[]{"Foo", "Bar"}, result.getArray());
		assertTrue(result.isBool());
		assertArrayEquals(new byte[]{0x1, 0x2}, result.getBytes());
	}

	@Test
	public void prettyPrint() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		PrettyPrintBean bean = new PrettyPrintBean();
		bean.setName("Jason");

		getConverter().setPrettyPrint(true);
		getConverter().writeInternal(bean, outputMessage);
		String result = outputMessage.getBodyAsString(Charset.forName("UTF-8"));

		assertEquals("{" + NEWLINE_SYSTEM_PROPERTY + "  \"name\" : \"Jason\"" + NEWLINE_SYSTEM_PROPERTY + "}", result);
	}


	public static class PrettyPrintBean {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
