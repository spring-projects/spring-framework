/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.codec.json

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.testfixture.codec.AbstractEncoderTests
import org.springframework.http.MediaType
import org.springframework.http.customJson
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.math.BigDecimal

/**
 * Tests for the JSON encoding using kotlinx.serialization with a custom serializer module.
 *
 * @author Sebastien Deleuze
 */
class CustomKotlinSerializationJsonEncoderTests :
		AbstractEncoderTests<KotlinSerializationJsonEncoder>(KotlinSerializationJsonEncoder(customJson)) {

	@Test
	override fun canEncode() {
		val bigDecimalType = ResolvableType.forClass(BigDecimal::class.java)
		Assertions.assertThat(encoder.canEncode(bigDecimalType, MediaType.APPLICATION_JSON)).isTrue()
	}

	@Test
	override fun encode() {
		val input = Mono.just(BigDecimal(1))
		testEncode(input, BigDecimal::class.java) { step: StepVerifier.FirstStep<DataBuffer?> ->
			step.consumeNextWith(expectString("1.0")
					.andThen { dataBuffer: DataBuffer? -> DataBufferUtils.release(dataBuffer) })
					.verifyComplete()
		}
	}

}
