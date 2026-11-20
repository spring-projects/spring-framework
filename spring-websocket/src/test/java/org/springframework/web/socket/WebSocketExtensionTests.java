/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link WebSocketExtension}.
 *
 * @author Brian Clozel
 */
class WebSocketExtensionTests {

	@Test
	void parseHeaderSingle() {
		List<WebSocketExtension> extensions =
				WebSocketExtension.parseExtensions("x-test-extension ; foo=bar ; bar=baz");

		assertThat(extensions).hasSize(1);
		WebSocketExtension extension = extensions.get(0);

		assertThat(extension.getName()).isEqualTo("x-test-extension");
		assertThat(extension.getParameters()).hasSize(2);
		assertThat(extension.getParameters().get("foo")).isEqualTo("bar");
		assertThat(extension.getParameters().get("bar")).isEqualTo("baz");
	}

	@Test
	void parseHeaderMultiple() {
		List<WebSocketExtension> extensions =
				WebSocketExtension.parseExtensions("x-foo-extension, x-bar-extension");

		assertThat(extensions.stream().map(WebSocketExtension::getName))
				.containsExactly("x-foo-extension", "x-bar-extension");
	}

}
