/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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