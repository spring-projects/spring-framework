package org.springframework.http.codec.csv;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDataBufferDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Decoder for CSV files.
 */
public final class Jackson2CsvDecoder<T> extends AbstractDataBufferDecoder<T> {
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
	 * {@link String} decoder for parsing a whole row as a {@link String}.
	 */
	private final StringDecoder stringDecoder;
	/**
	 * Default charset. Defaults to UTF-8.
	 */
	private Charset defaultCharset = UTF_8;
	/**
	 * Number of rows to lookahead for skipping of rows. Defaults to 16.
	 * For a CSV with a header the lookahead has at least to be 3:
	 * <ul>
	 *     <li>Header</li>
	 *     <li>First data row</li>
	 *     <li>Read ahead</li>
	 * </ul>
	 * The maximum number of comment and empty lines have to be considered additionally.
	 */
	private int lookahead = 16;

	/**
	 * Constructor.
	 */
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
	 * Default charset. Defaults to UTF-8.
	 */
	public Charset getDefaultCharset() {
		return defaultCharset;
	}

	/**
	 * Default charset. Defaults to UTF-8.
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
