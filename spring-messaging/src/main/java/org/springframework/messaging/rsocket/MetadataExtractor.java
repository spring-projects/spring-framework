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

import org.springframework.core.codec.DecodingException;
import org.springframework.util.MimeType;

/**
 * Strategy to extract a map of value(s) from {@link Payload} metadata, which
 * could be composite metadata with multiple entries. Each metadata entry
 * is decoded based on its {@code MimeType} and a name is assigned to the decoded
 * value. The resulting name-value pairs can be added to the headers of a
 * {@link org.springframework.messaging.Message Message}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 * @see MetadataExtractorRegistry
 */
public interface MetadataExtractor {

	/**
	 * The key to assign to the extracted "route" of the payload.
	 */
	String ROUTE_KEY = "route";


	/**
	 * Extract a map of values from the given {@link Payload} metadata.
	 * The Payload "route", if present, should be saved under {@link #ROUTE_KEY}.
	 * @param payload the payload whose metadata should be read
	 * @param metadataMimeType the metadata MimeType for the connection.
	 * @return name values pairs extracted from the metadata
	 * @throws DecodingException if the metadata cannot be decoded
	 * @throws IllegalArgumentException if routing metadata cannot be decoded
	 */
	Map<String, Object> extract(Payload payload, MimeType metadataMimeType);

}
