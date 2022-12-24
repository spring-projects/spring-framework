package org.springframework.http.codec.csv;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link Jackson2CsvReactiveParser}.
 */
class Jackson2CsvReactiveParserTests {
	/**
	 * Type for rows: {@code Map<String, String>}.
	 */
	private static final TypeReference<Map<String, String>> CSV_RECORD_TYPE = new TypeReference<>() {};

	/**
     * Test for {@link Jackson2CsvReactiveParser#parse(String)}.
     */
    @Test
    void parse_withHeader() {
        var parser = parser(CsvSchema.builder()
                .setColumnSeparator(';')
                .setLineSeparator('\n')
                // Read header from CSV.
                .setUseHeader(true),
                3);

        var rows = rows("header1;header2", "value1;value2", "value3;value4")
                .flatMap(parser::parse)
                .concatWith(Flux.defer(parser::parseRemaining))
                .toIterable();

        assertThat(rows).containsOnly(
                Map.of("header1", "value1", "header2", "value2"),
                Map.of("header1", "value3", "header2", "value4"));
    }

    /**
     * Test for {@link Jackson2CsvReactiveParser#parse(String)}.
     */
    @Test
    void parse_withoutHeader() {
        var parser = parser(CsvSchema.builder()
                .setColumnSeparator(';')
                .setLineSeparator('\n')
                .addColumn("header1")
                .addColumn("header2"),
                2);

        var rows = rows("value1;value2", "value3;value4")
                .flatMap(parser::parse)
                .concatWith(Flux.defer(parser::parseRemaining))
                .toIterable();

        assertThat(rows).containsOnly(
                Map.of("header1", "value1", "header2", "value2"),
                Map.of("header1", "value3", "header2", "value4"));
    }

	/**
	 * Create a parser for the schema.
	 */
    private Jackson2CsvReactiveParser<Map<String, String>> parser(CsvSchema.Builder schema, int lookahead) {
        var csvMapper = CsvMapper.builder().build();
        var objectReader = csvMapper
                .readerFor(CSV_RECORD_TYPE)
                .with(schema.build());
        return new Jackson2CsvReactiveParser<>(objectReader, lookahead);
    }

	/**
	 * Create rows.
	 */
    private Flux<String> rows(String... rows) {
        return Flux.just(rows).map(row -> row + "\n");
    }
}