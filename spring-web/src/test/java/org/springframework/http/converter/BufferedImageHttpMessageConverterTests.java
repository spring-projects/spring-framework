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

package org.springframework.http.converter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BufferedImageHttpMessageConverter.
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class BufferedImageHttpMessageConverterTests {

	private BufferedImageHttpMessageConverter converter;

	@BeforeEach
	public void setUp() {
		converter = new BufferedImageHttpMessageConverter();
	}

	@Test
	public void canRead() {
		assertThat(converter.canRead(BufferedImage.class, null)).as("Image not supported").isTrue();
		assertThat(converter.canRead(BufferedImage.class, new MediaType("image", "png"))).as("Image not supported").isTrue();
	}

	@Test
	public void canWrite() {
		assertThat(converter.canWrite(BufferedImage.class, null)).as("Image not supported").isTrue();
		assertThat(converter.canWrite(BufferedImage.class, new MediaType("image", "png"))).as("Image not supported").isTrue();
		assertThat(converter.canWrite(BufferedImage.class, new MediaType("*", "*"))).as("Image not supported").isTrue();
	}

	@Test
	public void read() throws IOException {
		Resource logo = new ClassPathResource("logo.jpg", BufferedImageHttpMessageConverterTests.class);
		byte[] body = FileCopyUtils.copyToByteArray(logo.getInputStream());
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		inputMessage.getHeaders().setContentType(new MediaType("image", "jpeg"));
		BufferedImage result = converter.read(BufferedImage.class, inputMessage);
		assertThat(result.getHeight()).as("Invalid height").isEqualTo(500);
		assertThat(result.getWidth()).as("Invalid width").isEqualTo(750);
	}

	@Test
	public void write() throws IOException {
		Resource logo = new ClassPathResource("logo.jpg", BufferedImageHttpMessageConverterTests.class);
		BufferedImage body = ImageIO.read(logo.getFile());
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MediaType contentType = new MediaType("image", "png");
		converter.write(body, contentType, outputMessage);
		assertThat(outputMessage.getWrittenHeaders().getContentType()).as("Invalid content type").isEqualTo(contentType);
		assertThat(outputMessage.getBodyAsBytes().length > 0).as("Invalid size").isTrue();
		BufferedImage result = ImageIO.read(new ByteArrayInputStream(outputMessage.getBodyAsBytes()));
		assertThat(result.getHeight()).as("Invalid height").isEqualTo(500);
		assertThat(result.getWidth()).as("Invalid width").isEqualTo(750);
	}

	@Test
	public void writeDefaultContentType() throws IOException {
		Resource logo = new ClassPathResource("logo.jpg", BufferedImageHttpMessageConverterTests.class);
		MediaType contentType = new MediaType("image", "png");
		converter.setDefaultContentType(contentType);
		BufferedImage body = ImageIO.read(logo.getFile());
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(body, new MediaType("*", "*"), outputMessage);
		assertThat(outputMessage.getWrittenHeaders().getContentType()).as("Invalid content type").isEqualTo(contentType);
		assertThat(outputMessage.getBodyAsBytes().length > 0).as("Invalid size").isTrue();
		BufferedImage result = ImageIO.read(new ByteArrayInputStream(outputMessage.getBodyAsBytes()));
		assertThat(result.getHeight()).as("Invalid height").isEqualTo(500);
		assertThat(result.getWidth()).as("Invalid width").isEqualTo(750);
	}

}
