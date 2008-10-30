/*
 * Copyright ${YEAR} the original author or authors.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class Md5HashUtilsTests {

	private byte[] bytes;

	@Before
	public void createBytes() throws UnsupportedEncodingException {
		bytes = "Hello World".getBytes("UTF-8");
	}

	@Test
	public void hash() {
		byte[] result = Md5HashUtils.getHash(bytes);
		byte[] expected = new byte[]{-0x4f, 0xa, -0x73, -0x4f, 0x64, -0x20, 0x75, 0x41, 0x5, -0x49, -0x57, -0x65, -0x19,
				0x2e, 0x3f, -0x1b};
		assertArrayEquals("Invalid hash", expected, result);
	}

	@Test
	public void hashString() throws UnsupportedEncodingException {
		String hash = Md5HashUtils.getHashString(bytes);
		assertEquals("Invalid hash", "b10a8db164e0754105b7a99be72e3fe5", hash);
	}

	@Test
	public void hashStringBuilder() throws UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder();
		Md5HashUtils.appendHashString(bytes, builder);
		assertEquals("Invalid hash", "b10a8db164e0754105b7a99be72e3fe5", builder.toString());
	}


}