/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.type.classreading;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.SpringProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetadataReaderFactoryDelegate}.
 */
class MetadataReaderFactoryDelegateTests {

	@AfterEach
	void resetProperty() {
		SpringProperties.setProperty(MetadataReaderFactoryDelegate.ASM_METADATA_READER_PROPERTY_NAME, null);
	}

	@Test
	void usesClassFileMetadataReaderFactoryByDefault() {
		assertThat(MetadataReaderFactory.create((ClassLoader) null))
				.isInstanceOf(ClassFileMetadataReaderFactory.class);
	}

	@Test
	void asmPropertySelectsSimpleMetadataReaderFactory() {
		SpringProperties.setFlag(MetadataReaderFactoryDelegate.ASM_METADATA_READER_PROPERTY_NAME);
		assertThat(MetadataReaderFactory.create((ClassLoader) null))
				.isInstanceOf(SimpleMetadataReaderFactory.class);
	}

}
