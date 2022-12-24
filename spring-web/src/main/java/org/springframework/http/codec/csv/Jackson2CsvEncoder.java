package org.springframework.http.codec.csv;

import java.nio.charset.Charset;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Encoder for CSV files.
 */
public final class Jackson2CsvEncoder<T> extends AbstractEncoder<T> {
	/**
	 * Default charset. Defaults to UTF-8.
	 */
	private Charset defaultCharset = UTF_8;

	/**
	 * CSV mapper.
	 */
	private final CsvMapper mapper;

	/**
	 * CSV schema.
	 */
	private final CsvSchema schema;

	/**
	 * Constructor.
	 */
	public Jackson2CsvEncoder(CsvMapper mapper, CsvSchema schema) {
		super(MimeType.valueOf("text/csv"));
		this.mapper = checkNotNull(mapper, "Precondition violated: mapper != null.");
		this.schema = checkNotNull(schema, "Precondition violated: schema != null.");
	}

	@Override
	public Flux<DataBuffer> encode(
			Publisher<? extends T> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
		var objectWriter = mapper.writerFor(mapper.constructType(elementType.getType())).with(schema);
		var charset = mimeType != null && mimeType.getCharset() != null ? mimeType.getCharset() : defaultCharset;
		return Flux.from(inputStream)
				.flatMap(row -> writeCsv(row, objectWriter))
				.map(charset::encode)
				.map(bufferFactory::wrap);
	}

	/**
	 * Writer a row.
	 */
	private Mono<String> writeCsv(T row, ObjectWriter objectWriter) {
		try {
			return Mono.just(objectWriter.writeValueAsString(row));
		} catch (Exception e) {
			return Mono.error(e);
		}
	}
}
