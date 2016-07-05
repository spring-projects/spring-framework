/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.util;

import java.io.UnsupportedEncodingException;
import javax.xml.bind.DatatypeConverter;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @since 4.2
 */
public class Base64UtilsTests {

	@Test
	public void encode() throws UnsupportedEncodingException {
		byte[] bytes = new byte[]
				{-0x4f, 0xa, -0x73, -0x4f, 0x64, -0x20, 0x75, 0x41, 0x5, -0x49, -0x57, -0x65, -0x19, 0x2e, 0x3f, -0x1b};
		assertArrayEquals(bytes, Base64Utils.decode(Base64Utils.encode(bytes)));

		bytes = "Hello World".getBytes("UTF-8");
		assertArrayEquals(bytes, Base64Utils.decode(Base64Utils.encode(bytes)));

		bytes = "Hello World\r\nSecond Line".getBytes("UTF-8");
		assertArrayEquals(bytes, Base64Utils.decode(Base64Utils.encode(bytes)));

		bytes = "Hello World\r\nSecond Line\r\n".getBytes("UTF-8");
		assertArrayEquals(bytes, Base64Utils.decode(Base64Utils.encode(bytes)));

		bytes = new byte[] { (byte) 0xfb, (byte) 0xf0 };
		assertArrayEquals("+/A=".getBytes(), Base64Utils.encode(bytes));
		assertArrayEquals(bytes, Base64Utils.decode(Base64Utils.encode(bytes)));

		assertArrayEquals("-_A=".getBytes(), Base64Utils.encodeUrlSafe(bytes));
		assertArrayEquals(bytes, Base64Utils.decodeUrlSafe(Base64Utils.encodeUrlSafe(bytes)));
	}

	@Test
	public void encodeToStringWithJdk8VsJaxb() throws UnsupportedEncodingException {
		byte[] bytes = new byte[]
				{-0x4f, 0xa, -0x73, -0x4f, 0x64, -0x20, 0x75, 0x41, 0x5, -0x49, -0x57, -0x65, -0x19, 0x2e, 0x3f, -0x1b};
		assertEquals(Base64Utils.encodeToString(bytes), DatatypeConverter.printBase64Binary(bytes));
		assertArrayEquals(bytes, Base64Utils.decodeFromString(Base64Utils.encodeToString(bytes)));
		assertArrayEquals(bytes, DatatypeConverter.parseBase64Binary(DatatypeConverter.printBase64Binary(bytes)));

		bytes = "Hello World".getBytes("UTF-8");
		assertEquals(Base64Utils.encodeToString(bytes), DatatypeConverter.printBase64Binary(bytes));
		assertArrayEquals(bytes, Base64Utils.decodeFromString(Base64Utils.encodeToString(bytes)));
		assertArrayEquals(bytes, DatatypeConverter.parseBase64Binary(DatatypeConverter.printBase64Binary(bytes)));

		bytes = "Hello World\r\nSecond Line".getBytes("UTF-8");
		assertEquals(Base64Utils.encodeToString(bytes), DatatypeConverter.printBase64Binary(bytes));
		assertArrayEquals(bytes, Base64Utils.decodeFromString(Base64Utils.encodeToString(bytes)));
		assertArrayEquals(bytes, DatatypeConverter.parseBase64Binary(DatatypeConverter.printBase64Binary(bytes)));

		bytes = "Hello World\r\nSecond Line\r\n".getBytes("UTF-8");
		assertEquals(Base64Utils.encodeToString(bytes), DatatypeConverter.printBase64Binary(bytes));
		assertArrayEquals(bytes, Base64Utils.decodeFromString(Base64Utils.encodeToString(bytes)));
		assertArrayEquals(bytes, DatatypeConverter.parseBase64Binary(DatatypeConverter.printBase64Binary(bytes)));

	}

	@Test
	public void encodeDecodeUrlSafe() {
		byte[] bytes = new byte[] { (byte) 0xfb, (byte) 0xf0 };
		assertArrayEquals("-_A=".getBytes(), Base64Utils.encodeUrlSafe(bytes));
		assertArrayEquals(bytes, Base64Utils.decodeUrlSafe(Base64Utils.encodeUrlSafe(bytes)));

		assertEquals("-_A=", Base64Utils.encodeToUrlSafeString(bytes));
		assertArrayEquals(bytes, Base64Utils.decodeFromUrlSafeString(Base64Utils.encodeToUrlSafeString(bytes)));
	}

}
