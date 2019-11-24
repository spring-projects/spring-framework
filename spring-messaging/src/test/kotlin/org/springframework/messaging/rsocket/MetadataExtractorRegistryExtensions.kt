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

package org.springframework.messaging.rsocket

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.util.MimeType
import java.util.function.BiConsumer

class MetadataExtractorRegistryExtensions {

	@Test
	fun `metadataToExtract with String parameter`() {
		val extractor = mockk<MetadataExtractorRegistry>(relaxed = true)
		val name = "name"
		val mimeType = MimeType.valueOf("application/json")
		extractor.metadataToExtract<String>(mimeType, name)
		verify {
			extractor.metadataToExtract(mimeType, object: ParameterizedTypeReference<String>() {}, name)
		}
	}

	@Test
	fun `metadataToExtract with BiConsumer parameter`() {
		val extractor = mockk<MetadataExtractorRegistry>(relaxed = true)
		val mimeType = MimeType.valueOf("application/json")
		extractor.metadataToExtract<Map<String, String>>(mimeType) { jsonMap, outputMap ->
			outputMap.putAll(jsonMap)
		}
		verify {
			extractor.metadataToExtract(mimeType, object: ParameterizedTypeReference<Map<String, String>>() {}, any<BiConsumer<Map<String, String>, MutableMap<String, Any>>>())
		}
	}
}