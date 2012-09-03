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

package org.springframework.http.converter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test cases for {@link ObjectToStringHttpMessageConverter} class.
 * 
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 */
public class ObjectToStringHttpMessageConverterTest {

	private ObjectToStringHttpMessageConverter converter;

	@Before
	public void setUp() {
		converter = new ObjectToStringHttpMessageConverter();

		converter.setConversionService(new DefaultConversionService());
	}

	@Test
	public void testSupports() {
		converter.afterPropertiesSet();

		assertFalse(converter.supports(Math.class));
		assertFalse(converter.supports(Resource.class));

		assertTrue(converter.supports(Locale.class));
		assertTrue(converter.supports(Boolean.class));
		assertTrue(converter.supports(String.class));
		assertTrue(converter.supports(Enum.class));

		assertTrue(converter.supports(Byte.class));
		assertTrue(converter.supports(Short.class));
		assertTrue(converter.supports(Integer.class));
		assertTrue(converter.supports(Long.class));
		assertTrue(converter.supports(Float.class));
		assertTrue(converter.supports(Double.class));
		assertTrue(converter.supports(BigInteger.class));
	}

	@Test
	public void testSetDefaultCharset() throws IOException {
		converter.afterPropertiesSet();

		MockHttpServletResponse response = new MockHttpServletResponse();

		converter.write(Integer.valueOf(5), null, new ServletServerHttpResponse(response));

		assertEquals("ISO-8859-1", response.getCharacterEncoding());

		response = new MockHttpServletResponse();

		converter.setDefaultCharset(Charset.forName("UTF-16"));
		converter.afterPropertiesSet();

		converter.write(Byte.valueOf((byte) 31), null, new ServletServerHttpResponse(response));

		assertEquals("UTF-16", response.getCharacterEncoding());
	}

	@Test
	public void testSetWriteAcceptCharset() throws IOException {
		converter.setWriteAcceptCharset(false);
		converter.afterPropertiesSet();

		MockHttpServletResponse response = new MockHttpServletResponse();

		converter.write(new Date(), null, new ServletServerHttpResponse(response));

		assertNull(response.getHeader("Accept-Charset"));

		converter.setWriteAcceptCharset(true);
		converter.afterPropertiesSet();

		response = new MockHttpServletResponse();

		converter.write(new Date(), null, new ServletServerHttpResponse(response));

		assertNotNull(response.getHeader("Accept-Charset"));
	}

	@Test
	public void testRead() throws IOException {
		converter.afterPropertiesSet();

		MockHttpServletRequest request = new MockHttpServletRequest();

		request.setContentType(MediaType.TEXT_PLAIN_VALUE);

		Short shortValue = Short.valueOf((short) 781);

		request.setContent(shortValue.toString().getBytes(
				ObjectToStringHttpMessageConverter.DEFAULT_CHARSET));

		assertEquals(shortValue, converter.read(Short.class, new ServletServerHttpRequest(request)));

		Float floatValue = Float.valueOf(123);

		request.setCharacterEncoding("UTF-16");
		request.setContent(floatValue.toString().getBytes("UTF-16"));

		assertEquals(floatValue, converter.read(Float.class, new ServletServerHttpRequest(request)));

		Long longValue = Long.valueOf(55819182821331L);

		request.setCharacterEncoding("UTF-8");
		request.setContent(longValue.toString().getBytes("UTF-8"));

		assertEquals(longValue, converter.read(Long.class, new ServletServerHttpRequest(request)));
	}

	@Test
	public void testWrite() throws IOException {
		converter.afterPropertiesSet();

		MockHttpServletResponse response = new MockHttpServletResponse();

		converter.write(Byte.valueOf((byte) -8), null, new ServletServerHttpResponse(response));

		assertEquals("ISO-8859-1", response.getCharacterEncoding());
		assertTrue(response.getContentType().startsWith(MediaType.TEXT_PLAIN_VALUE));
		assertEquals(2, response.getContentLength());
		assertArrayEquals(new byte[] { '-', '8' }, response.getContentAsByteArray());

		response = new MockHttpServletResponse();

		converter.write(Integer.valueOf(958),
				new MediaType("text", "plain", Charset.forName("UTF-16")),
				new ServletServerHttpResponse(response));

		assertEquals("UTF-16", response.getCharacterEncoding());
		assertTrue(response.getContentType().startsWith(MediaType.TEXT_PLAIN_VALUE));
		assertEquals(8, response.getContentLength());
		// First two bytes are UTF-16 BOM:
		assertArrayEquals(new byte[] { -2, -1, 0, '9', 0, '5', 0, '8' },
				response.getContentAsByteArray());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAssertionInAfterPropertiesSet() {
		new ObjectToStringHttpMessageConverter().afterPropertiesSet();
	}
}
