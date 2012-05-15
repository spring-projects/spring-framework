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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson 2.x converter tests.
 *
 * @author Rossen Stoyanchev
 */
public class MappingJackson2HttpMessageConverterTests extends AbstractMappingJacksonHttpMessageConverterTests<MappingJackson2HttpMessageConverter> {

	@Override
	protected MappingJackson2HttpMessageConverter createConverter() {
		return new MappingJackson2HttpMessageConverter();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readGenerics() throws IOException {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter() {

			@Override
			protected JavaType getJavaType(Class<?> clazz) {
				if (List.class.isAssignableFrom(clazz)) {
					return new ObjectMapper().getTypeFactory().constructCollectionType(ArrayList.class, MyBean.class);
				}
				else {
					return super.getJavaType(clazz);
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
	public void prettyPrint() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		PrettyPrintBean bean = new PrettyPrintBean();
		bean.setName("Jason");

		getConverter().setPrettyPrint(true);
		getConverter().writeInternal(bean, outputMessage);
		String result = outputMessage.getBodyAsString(Charset.forName("UTF-8"));

		assertEquals("{\n  \"name\" : \"Jason\"\n}", result);
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
