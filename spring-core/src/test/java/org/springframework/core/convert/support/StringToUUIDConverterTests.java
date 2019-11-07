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

package org.springframework.core.convert.support;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author 李晓东 linkedme@qq.com
 */
class StringToUUIDConverterTests {

	private GenericConversionService conversionService;

	@BeforeEach
	void setUp() {
		conversionService = new GenericConversionService();
		this.conversionService.addConverter(new StringToUUIDConverter());
	}

	@Test
	void emptyStringToUUID() throws Exception {
		String source = "";
		TypeDescriptor sourceType = TypeDescriptor.forObject(source);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("uuidTarget"));

		Exception convert = null;
		try {
			this.conversionService.convert(source, sourceType, targetType);
		}
		catch (Exception e) {
			convert = e;
		}

		assertThat(convert.getClass()).isEqualTo(ConversionFailedException.class);
		assertThat(convert.getCause().getClass()).isEqualTo(IllegalArgumentException.class);
	}

	@Test
	void blankStringToUUID() throws Exception {
		String source = "    ";
		TypeDescriptor sourceType = TypeDescriptor.forObject(source);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("uuidTarget"));

		Exception convert = null;
		try {
			this.conversionService.convert(source, sourceType, targetType);
		}
		catch (Exception e) {
			convert = e;
		}

		assertThat(convert.getClass()).isEqualTo(ConversionFailedException.class);
		assertThat(convert.getCause().getClass()).isEqualTo(IllegalArgumentException.class);
	}

	@Test
	void nullToUUID() throws Exception {
		String source = null;
		TypeDescriptor sourceType = TypeDescriptor.forObject(source);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("uuidTarget"));

		Object convert = this.conversionService.convert(source, sourceType, targetType);

		assertThat(convert).isEqualTo(null);
	}

	@Test
	void uuidStringToUUID() throws Exception {
		String source = UUID.randomUUID().toString();
		TypeDescriptor sourceType = TypeDescriptor.forObject(source);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("uuidTarget"));

		Object convert = this.conversionService.convert(source, sourceType, targetType);

		assertThat(convert.toString()).isEqualTo(source);
	}

	@Test
	void notUuidStringToUUID() throws Exception {
		String source = UUID.randomUUID().toString().replace("-", "");
		TypeDescriptor sourceType = TypeDescriptor.forObject(source);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("uuidTarget"));

		Exception convert = null;
		try {
			this.conversionService.convert(source, sourceType, targetType);
		}
		catch (Exception e) {
			convert = e;
		}

		assertThat(convert.getClass()).isEqualTo(ConversionFailedException.class);
		assertThat(convert.getCause().getClass()).isEqualTo(IllegalArgumentException.class);
	}

	public UUID uuidTarget;

}
