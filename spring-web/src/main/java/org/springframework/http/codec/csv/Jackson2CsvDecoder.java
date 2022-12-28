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

package org.springframework.http.codec.csv;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDataBufferDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN;

/**
 * Decoder for CSV files.
 * <p>
 * Uses a lookahead to circumvent the fact that the Jackson CSV parser has no streaming API.
 * For a CSV with a header the lookahead has at least to be 3:
 * <ul>
 *     <li>Header</li>
 *     <li>First data row</li>
 *     <li>Read ahead</li>
 * </ul>
 * The maximum number of consecutive comment and empty lines in the CSV have to be considered additionally.
 */
public final class Jackson2CsvDecoder<T> extends AbstractDataBufferDecoder<T> {

	/**
	 * The default charset "UTF-8".
	 */
	public static final Charset DEFAULT_CHARSET = UTF_8;

	/**
	 * Element type for parsing a whole row as a {@link String}.
	 */
	private static final ResolvableType STRING_TYPE = ResolvableType.forType(String.class);


	/**
	 * CSV mapper.
	 */
	private final CsvMapper mapper;

	/**
	 * CSV schema.
	 */
	private final CsvSchema schema;

	/**
	 * Decoder for parsing a whole row as a {@link String}.
	 */
	private final StringDecoder stringDecoder;

	/**
	 * The default charset. Used if the MIME type contains none. Defaults to {@link #DEFAULT_CHARSET}.
	 */
	private Charset defaultCharset = DEFAULT_CHARSET;

	/**
	 * Number of rows to lookahead for skipping of rows. Defaults to 16.
	 */
	private int lookahead = 16;


	public Jackson2CsvDecoder(CsvMapper mapper, CsvSchema schema) {
		super(MimeType.valueOf("text/csv"));
		Assert.notNull(mapper, "mapper must not be null");
		Assert.notNull(schema, "schema must not be null");
		this.mapper = mapper;
		this.schema = schema;
		this.stringDecoder = StringDecoder.textPlainOnly(List.of(new String(schema.getLineSeparator())), false);
	}

	@Override
	public void setMaxInMemorySize(int byteCount) {
		super.setMaxInMemorySize(byteCount);
		stringDecoder.setMaxInMemorySize(byteCount);
	}

	/**
	 * The default charset. Used if the MIME type contains none. Defaults to {@link #DEFAULT_CHARSET}.
	 */
	public Charset getDefaultCharset() {
		return defaultCharset;
	}

	/**
	 * The default charset. Used if the MIME type contains none. Defaults to {@link #DEFAULT_CHARSET}.
	 */
	public void setDefaultCharset(Charset defaultCharset) {
		Assert.notNull(defaultCharset, "defaultCharset must not be null");
		this.defaultCharset = defaultCharset;
	}

	/**
	 * Number of rows to lookahead for skipping of rows. Defaults to 16.
	 */
	public int getLookAhead() {
		return lookahead;
	}

	/**
	 * Number of rows to lookahead for skipping of rows. Defaults to 16.
	 */
	public void setLookAhead(int lookahead) {
		Assert.isTrue(lookahead > 0, "lookahead must be positive");
		this.lookahead = lookahead;
	}

	@Override
	public Flux<T> decode(
			Publisher<DataBuffer> input,
			ResolvableType elementType,
			@Nullable MimeType mimeType,
			@Nullable Map<String, Object> hints) {
		Assert.notNull(input, "input must not be null");
		Assert.notNull(elementType, "elementType must not be null");

		var parser = new Jackson2CsvReactiveParser<T>(
				mapper.readerFor(mapper.constructType(elementType.getType())).with(schema),
				lookahead);

		return splitRows(input, mimeType, hints)
				.flatMap(parser::parse)
				.concatWith(Flux.defer(parser::parseRemaining));
	}

	/**
	 * Split input into rows.
	 */
	private Flux<String> splitRows(Publisher<DataBuffer> input, MimeType mimeType, Map<String, Object> hints) {
		var textMimeType = new MimeType(TEXT_PLAIN,
				mimeType != null && mimeType.getCharset() != null ? mimeType.getCharset() : defaultCharset);
		return stringDecoder.decode(input, STRING_TYPE, textMimeType, hints);
	}

}
