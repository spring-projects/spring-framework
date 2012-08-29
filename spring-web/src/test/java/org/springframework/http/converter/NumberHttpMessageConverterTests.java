package org.springframework.http.converter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test cases for {@link NumberHttpMessageConverter} class.
 * 
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 */
public class NumberHttpMessageConverterTests {

	private final NumberHttpMessageConverter converter = new NumberHttpMessageConverter();

	@Test
	public void testSupports() {
		assertFalse(converter.supports(Boolean.class));
		assertFalse(converter.supports(NumberHttpMessageConverter.class));
		assertFalse(converter.supports(String.class));
		assertFalse(converter.supports(BigInteger.class));

		assertTrue(converter.supports(Byte.class));
		assertTrue(converter.supports(Short.class));
		assertTrue(converter.supports(Integer.class));
		assertTrue(converter.supports(Long.class));
		assertTrue(converter.supports(Float.class));
		assertTrue(converter.supports(Double.class));
	}

	@Test(expected = HttpMessageNotReadableException.class)
	public void testReadForNotParseableContent() throws IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();

		request.setContent("dummy".getBytes(NumberHttpMessageConverter.DEFAULT_MEDIA_TYPE
				.getCharSet()));

		converter.read(Short.class, new ServletServerHttpRequest(request));
	}

	@Test(expected = RuntimeException.class)
	public void testReadForUnsupportedNumberClass() throws IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();

		request.setContent("14".getBytes(NumberHttpMessageConverter.DEFAULT_MEDIA_TYPE.getCharSet()));

		converter.read(BigInteger.class, new ServletServerHttpRequest(request));
	}

	@Test
	public void testRead() throws IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();

		request.setContentType(MediaType.TEXT_PLAIN_VALUE);

		Short shortValue = Short.valueOf((short) 781);

		request.setContent(shortValue.toString().getBytes(
				NumberHttpMessageConverter.DEFAULT_MEDIA_TYPE.getCharSet()));

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
		MockHttpServletResponse response = new MockHttpServletResponse();

		converter.write(Byte.valueOf((byte) -8), NumberHttpMessageConverter.DEFAULT_MEDIA_TYPE,
				new ServletServerHttpResponse(response));

		assertEquals(2, response.getContentLength());
		assertArrayEquals(new byte[] { '-', '8' }, response.getContentAsByteArray());

		response = new MockHttpServletResponse();

		converter.write(Integer.valueOf(958),
				new MediaType("text", "plain", Charset.forName("UTF-16")),
				new ServletServerHttpResponse(response));

		assertEquals(8, response.getContentLength());
		// First two bytes are UTF-16 BOM:
		assertArrayEquals(new byte[] { -2, -1, 0, '9', 0, '5', 0, '8' },
				response.getContentAsByteArray());
	}
}
