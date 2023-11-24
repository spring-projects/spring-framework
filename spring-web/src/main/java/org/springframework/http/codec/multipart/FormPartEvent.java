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

package org.springframework.http.codec.multipart;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Represents an event triggered for a form field. Contains the
 * {@linkplain #value() value}, besides the {@linkplain #headers() headers}
 * exposed through {@link PartEvent}.
 *
 * <p>Multipart form fields trigger one {@code FormPartEvent}, as
 * {@linkplain PartEvent described here}.
 *
 * @author Arjen Poutsma
 * @since 6.0
 */
public interface FormPartEvent extends PartEvent {

	/**
	 * Return the form field value.
	 */
	String value();

	/**
	 * Creates a stream with a single {@code FormPartEven} based on the given
	 * {@linkplain PartEvent#name() name} and {@linkplain #value() value}.
	 * @param name the name of the part
	 * @param value the form field value
	 * @return a single event stream
	 */
	static Mono<FormPartEvent> create(String name, String value) {
		return create(name, value, null);
	}

	/**
	 * Creates a stream with a single {@code FormPartEven} based on the given
	 * {@linkplain PartEvent#name() name} and {@linkplain #value() value}.
	 * @param name the name of the part
	 * @param value the form field value
	 * @param headersConsumer used to change default headers. Can be {@code null}.
	 * @return a single event stream
	 */
	static Mono<FormPartEvent> create(String name, String value, @Nullable Consumer<HttpHeaders> headersConsumer) {
		Assert.hasLength(name, "Name must not be empty");
		Assert.notNull(value, "Value must not be null");

		return Mono.fromCallable(() -> {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));
			headers.setContentDisposition(ContentDisposition.formData().
					name(name)
					.build());
			if (headersConsumer != null) {
				headersConsumer.accept(headers);
			}
			return DefaultPartEvents.form(headers, value);
		});
	}

}
