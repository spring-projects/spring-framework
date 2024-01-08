/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.messaging.rsocket;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.DataBufferDecoder;
import org.springframework.core.codec.DataBufferEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.SimpleRouteMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RSocketStrategies}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
class DefaultRSocketStrategiesTests {

	@Test
	void defaultSettings() {
		RSocketStrategies strategies = RSocketStrategies.create();

		assertThat(strategies.encoders()).hasSize(4).hasOnlyElementsOfTypes(
				CharSequenceEncoder.class,
				ByteArrayEncoder.class,
				ByteBufferEncoder.class,
				DataBufferEncoder.class);

		assertThat(strategies.decoders()).hasSize(4).hasOnlyElementsOfTypes(
				StringDecoder.class,
				ByteArrayDecoder.class,
				ByteBufferDecoder.class,
				DataBufferDecoder.class);

		assertThat(strategies.routeMatcher()).isNotNull();
		assertThat(strategies.metadataExtractor()).isNotNull();
		assertThat(strategies.reactiveAdapterRegistry()).isNotNull();

		assertThat(((DefaultMetadataExtractor) strategies.metadataExtractor()).getDecoders()).hasSize(4);
	}

	@Test
	void explicitValues() {
		SimpleRouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher());
		DefaultMetadataExtractor extractor = new DefaultMetadataExtractor();
		ReactiveAdapterRegistry registry = new ReactiveAdapterRegistry();

		RSocketStrategies strategies = RSocketStrategies.builder()
				.encoders(encoders -> {
					encoders.clear();
					encoders.add(new ByteArrayEncoder());
				})
				.decoders(decoders -> {
					decoders.clear();
					decoders.add(new ByteArrayDecoder());
				})
				.routeMatcher(matcher)
				.metadataExtractor(extractor)
				.reactiveAdapterStrategy(registry)
				.build();

		assertThat(strategies.encoders()).hasSize(1);
		assertThat(strategies.decoders()).hasSize(1);
		assertThat(strategies.routeMatcher()).isSameAs(matcher);
		assertThat(strategies.metadataExtractor()).isSameAs(extractor);
		assertThat(strategies.reactiveAdapterRegistry()).isSameAs(registry);
	}

	@Test
	void copyConstructor() {
		RSocketStrategies strategies1 = RSocketStrategies.create();
		RSocketStrategies strategies2 = strategies1.mutate().build();

		assertThat(strategies1.encoders()).hasSameElementsAs(strategies2.encoders());
		assertThat(strategies1.decoders()).hasSameElementsAs(strategies2.decoders());
		assertThat(strategies1.routeMatcher()).isSameAs(strategies2.routeMatcher());
		assertThat(strategies1.metadataExtractor()).isSameAs(strategies2.metadataExtractor());
		assertThat(strategies1.reactiveAdapterRegistry()).isSameAs(strategies2.reactiveAdapterRegistry());
	}

	@Test
	void applyMetadataExtractors() {
		Consumer<MetadataExtractorRegistry> consumer = mock();
		RSocketStrategies.builder().metadataExtractorRegistry(consumer).build();
		verify(consumer, times(1)).accept(any());
	}

}
