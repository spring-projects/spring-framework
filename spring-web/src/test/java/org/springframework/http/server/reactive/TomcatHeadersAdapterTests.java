/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import org.apache.tomcat.util.http.MimeHeaders;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatHeadersAdapter}.
 *
 * @author Johnny Lim
 * @author Sam Brannen
 * @since 7.0
 */
class TomcatHeadersAdapterTests {

	private final TomcatHeadersAdapter adapter = new TomcatHeadersAdapter(new MimeHeaders());


	@Test
	void clear() {
		adapter.add("key1", "value1");
		adapter.add("key2", "value2");
		assertThat(adapter).isNotEmpty();
		assertThat(adapter).hasSize(2);
		assertThat(adapter).containsKeys("key1", "key2");

		adapter.clear();
		assertThat(adapter).isEmpty();
		assertThat(adapter).hasSize(0);
	}

}
