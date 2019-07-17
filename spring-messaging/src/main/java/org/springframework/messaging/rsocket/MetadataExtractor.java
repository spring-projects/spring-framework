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
package org.springframework.messaging.rsocket;

import java.util.Map;

import io.rsocket.Payload;

import org.springframework.util.MimeType;

/**
 * Strategy to extract a map of values from the metadata of a {@link Payload}.
 * This includes decoding metadata entries based on their mime type and
 * assigning a name to the decoded value. The resulting name-value pairs can
 * be added to the headers of a
 * {@link org.springframework.messaging.Message Message}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public interface MetadataExtractor {

	/**
	 * The key to assign to the extracted "route" of the payload.
	 */
	String ROUTE_KEY = "route";

	/**
	 * Constant for mime type {@code message/x.rsocket.composite-metadata.v0}.
	 */
	MimeType COMPOSITE_METADATA = new MimeType("message", "x.rsocket.composite-metadata.v0");

	/**
	 * Constant for mime type {@code message/x.rsocket.routing.v0}.
	 */
	MimeType ROUTING = new MimeType("message", "x.rsocket.routing.v0");


	/**
	 * Extract a map of values from the given {@link Payload} metadata.
	 * <p>Metadata may be composite and consist of multiple entries
	 * Implementations are free to extract any number of name-value pairs per
	 * metadata entry. The Payload "route" should be saved under the
	 * {@link #ROUTE_KEY}.
	 * @param payload the payload whose metadata should be read
	 * @param metadataMimeType the mime type of the metadata; this is what was
	 * specified by the client at the start of the RSocket connection.
	 * @param strategies for access to codecs and a DataBufferFactory
	 * @return a map of 0 or more decoded metadata values with assigned names
	 */
	Map<String, Object> extract(Payload payload, MimeType metadataMimeType, RSocketStrategies strategies);

}
