/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
class DigestUtilsTests {

	private byte[] bytes;


	@BeforeEach
	void createBytes() throws UnsupportedEncodingException {
		bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
	}


	@Test
	void md5() throws IOException {
		byte[] expected = new byte[]
				{-0x4f, 0xa, -0x73, -0x4f, 0x64, -0x20, 0x75, 0x41, 0x5, -0x49, -0x57, -0x65, -0x19, 0x2e, 0x3f, -0x1b};

		byte[] result = DigestUtils.md5Digest(bytes);
		assertThat(result).as("Invalid hash").isEqualTo(expected);

		result = DigestUtils.md5Digest(new ByteArrayInputStream(bytes));
		assertThat(result).as("Invalid hash").isEqualTo(expected);
	}

	@Test
	void md5Hex() throws IOException {
		String expected = "b10a8db164e0754105b7a99be72e3fe5";

		String hash = DigestUtils.md5DigestAsHex(bytes);
		assertThat(hash).as("Invalid hash").isEqualTo(expected);

		hash = DigestUtils.md5DigestAsHex(new ByteArrayInputStream(bytes));
		assertThat(hash).as("Invalid hash").isEqualTo(expected);
	}

	@Test
	void md5StringBuilder() throws IOException {
		String expected = "b10a8db164e0754105b7a99be72e3fe5";

		StringBuilder builder = new StringBuilder();
		DigestUtils.appendMd5DigestAsHex(bytes, builder);
		assertThat(builder.toString()).as("Invalid hash").isEqualTo(expected);

		builder = new StringBuilder();
		DigestUtils.appendMd5DigestAsHex(new ByteArrayInputStream(bytes), builder);
		assertThat(builder.toString()).as("Invalid hash").isEqualTo(expected);
	}

}
