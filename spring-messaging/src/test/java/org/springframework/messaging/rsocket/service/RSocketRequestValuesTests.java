/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.messaging.rsocket.service;


import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link RSocketRequestValues}.
 * @author Rossen Stoyanchev
 */
public class RSocketRequestValuesTests {

	@Test
	void route() {
		String myRoute = "myRoute";
		RSocketRequestValues values = RSocketRequestValues.builder(myRoute).build();
		assertThat(values.getRoute()).isEqualTo(myRoute);
	}

	@Test
	void routeOverride() {
		RSocketRequestValues values = RSocketRequestValues.builder("route1").setRoute("route2").build();

		assertThat(values.getRoute()).isEqualTo("route2");
	}

	@Test
	void payloadValue() {
		String payload = "myValue";
		RSocketRequestValues values = RSocketRequestValues.builder(null).setPayloadValue(payload).build();

		assertThat(values.getPayloadValue()).isEqualTo(payload);
		assertThat(values.getPayload()).isNull();
	}

	@Test
	void payloadPublisher() {
		Mono<String> payloadMono = Mono.just( "myValue");
		RSocketRequestValues values = RSocketRequestValues.builder(null)
				.setPayload(payloadMono, new ParameterizedTypeReference<>() { })
				.build();

		assertThat(values.getPayloadValue()).isNull();
		assertThat(values.getPayload()).isSameAs(payloadMono);
	}

	@Test
	void metadata() {
		RSocketRequestValues values = RSocketRequestValues.builder(null)
				.addMetadata("myMetadata1").addMimeType(MimeTypeUtils.TEXT_PLAIN)
				.addMetadata("myMetadata2").addMimeType(MimeTypeUtils.TEXT_HTML)
				.build();

		assertThat(values.getMetadata())
				.hasSize(2)
				.containsEntry("myMetadata1", MimeTypeUtils.TEXT_PLAIN)
				.containsEntry("myMetadata2", MimeTypeUtils.TEXT_HTML);
	}

	@Test
	void metadataInvalidEntry() {
		// MimeType without metadata
		assertThatIllegalArgumentException()
				.isThrownBy(() -> RSocketRequestValues.builder(null).addMimeType(MimeTypeUtils.TEXT_PLAIN));

		// Metadata without MimeType
		assertThatIllegalArgumentException()
				.isThrownBy(() -> RSocketRequestValues.builder(null)
						.addMetadata("metadata1")
						.addMetadata("metadata2"));
	}

}
