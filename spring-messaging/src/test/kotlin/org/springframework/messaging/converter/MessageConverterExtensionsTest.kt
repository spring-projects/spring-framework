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

package org.springframework.messaging.converter

import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test
import org.springframework.messaging.MessageHeaders.CONTENT_TYPE
import org.springframework.messaging.support.MessageBuilder
import org.springframework.util.MimeTypeUtils.TEXT_PLAIN

class MessageConverterExtensionsTest {

	@Test
	fun `MessageConverter fromMessage inline extension`() {
		val converter = GenericMessageConverter()
		val content = MessageBuilder.withPayload("ABC").setHeader(CONTENT_TYPE, TEXT_PLAIN).build()
		assertThat(converter.fromMessage<String>(content)).isEqualTo("ABC")
	}
}