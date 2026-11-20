/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.MultipartBodyBuilder.PublisherEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class MultipartBodyBuilderTests {

	@Test
	void builder() {

		MultipartBodyBuilder builder = new MultipartBodyBuilder();

		MultiValueMap<String, String> multipartData = new LinkedMultiValueMap<>();
		multipartData.add("form field", "form value");
		builder.part("key", multipartData).header("foo", "bar");

		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		builder.part("logo", logo).header("baz", "qux");

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.add("foo", "bar");
		HttpEntity<String> entity = new HttpEntity<>("body", entityHeaders);
		builder.part("entity", entity).header("baz", "qux");

		Publisher<String> publisher = Flux.just("foo", "bar", "baz");
		builder.asyncPart("publisherClass", publisher, String.class).header("baz", "qux");
		builder.asyncPart("publisherPtr", publisher, new ParameterizedTypeReference<>() {}).header("baz", "qux");

		MultiValueMap<String, HttpEntity<?>> result = builder.build();

		assertThat(result).hasSize(5);
		HttpEntity<?> resultEntity = result.getFirst("key");
		assertThat(resultEntity).isNotNull();
		assertThat(resultEntity.getBody()).isEqualTo(multipartData);
		assertThat(resultEntity.getHeaders().getFirst("foo")).isEqualTo("bar");

		resultEntity = result.getFirst("logo");
		assertThat(resultEntity).isNotNull();
		assertThat(resultEntity.getBody()).isEqualTo(logo);
		assertThat(resultEntity.getHeaders().getFirst("baz")).isEqualTo("qux");

		resultEntity = result.getFirst("entity");
		assertThat(resultEntity).isNotNull();
		assertThat(resultEntity.getBody()).isEqualTo("body");
		assertThat(resultEntity.getHeaders().getFirst("foo")).isEqualTo("bar");
		assertThat(resultEntity.getHeaders().getFirst("baz")).isEqualTo("qux");

		resultEntity = result.getFirst("publisherClass");
		assertThat(resultEntity).isNotNull();
		assertThat(resultEntity.getBody()).isEqualTo(publisher);
		assertThat(((PublisherEntity<?, ?>) resultEntity).getResolvableType()).isEqualTo(ResolvableType.forClass(String.class));
		assertThat(resultEntity.getHeaders().getFirst("baz")).isEqualTo("qux");

		resultEntity = result.getFirst("publisherPtr");
		assertThat(resultEntity).isNotNull();
		assertThat(resultEntity.getBody()).isEqualTo(publisher);
		assertThat(((PublisherEntity<?, ?>) resultEntity).getResolvableType()).isEqualTo(ResolvableType.forClass(String.class));
		assertThat(resultEntity.getHeaders().getFirst("baz")).isEqualTo("qux");
	}

	@Test // SPR-16601
	void publisherEntityAcceptedAsInput() {

		Publisher<String> publisher = Flux.just("foo", "bar", "baz");
		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		builder.asyncPart("publisherClass", publisher, String.class).header("baz", "qux");
		HttpEntity<?> entity = builder.build().getFirst("publisherClass");

		assertThat(entity).isNotNull();
		assertThat(entity.getClass()).isEqualTo(PublisherEntity.class);

		// Now build a new MultipartBodyBuilder, as BodyInserters.fromMultipartData would do...

		builder = new MultipartBodyBuilder();
		builder.part("publisherClass", entity);
		entity = builder.build().getFirst("publisherClass");

		assertThat(entity).isNotNull();
		assertThat(entity.getClass()).isEqualTo(PublisherEntity.class);
	}

}
