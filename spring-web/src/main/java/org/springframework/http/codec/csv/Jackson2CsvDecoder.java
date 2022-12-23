package org.springframework.http.codec.csv;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDataBufferDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN;

import static com.google.common.base.Preconditions.checkNotNull;
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
     * Constructor.
     */
    public Jackson2CsvDecoder(CsvMapper mapper, CsvSchema schema) {
        super(MimeType.valueOf("text/csv"));
        this.mapper = checkNotNull(mapper, "Precondition violated: mapper != null.");
        this.schema = checkNotNull(schema, "Precondition violated: schema != null.");
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
        this.defaultCharset = checkNotNull(defaultCharset, "Precondition violated: defaultCharset != null.");
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
        this.lookahead = lookahead;
    }

    @Override
    public Flux<T> decode(
            Publisher<DataBuffer> input,
            ResolvableType elementType,
            @Nullable MimeType mimeType,
            @Nullable Map<String, Object> hints) {
        checkNotNull(input, "Precondition violated: input != null.");
        checkNotNull(elementType, "Precondition violated: elementType != null.");

        var parser = parser(elementType);
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

    /**
     * Create a parser for the element type.
     */
    private Jackson2CsvReactiveParser<T> parser(ResolvableType elementType) {
        return new Jackson2CsvReactiveParser<>(
                mapper.readerFor(mapper.constructType(elementType.getType())).with(schema),
                lookahead);
    }
}
