/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
class MediaTypeFactoryTests {

	@Test
	void getMediaType() {
		assertThat(MediaTypeFactory.getMediaType("file.xml")).contains(MediaType.APPLICATION_XML);
		assertThat(MediaTypeFactory.getMediaType("file.js")).contains(MediaType.parseMediaType("text/javascript"));
		assertThat(MediaTypeFactory.getMediaType("file.css")).contains(MediaType.parseMediaType("text/css"));
		assertThat(MediaTypeFactory.getMediaType("file.wasm")).contains(MediaType.parseMediaType("application/wasm"));
		assertThat(MediaTypeFactory.getMediaType("file.foobar")).isNotPresent();
	}

	@Test
	void nullParameter() {
		assertThat(MediaTypeFactory.getMediaType((String) null)).isNotPresent();
		assertThat(MediaTypeFactory.getMediaType((Resource) null)).isNotPresent();
		assertThat(MediaTypeFactory.getMediaTypes(null)).isEmpty();
	}

}
