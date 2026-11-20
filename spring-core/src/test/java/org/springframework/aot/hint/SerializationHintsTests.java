/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint;

import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SerializationHints}.
 *
 * @author Stephane Nicoll
 */
@Deprecated
@SuppressWarnings("removal")
class SerializationHintsTests {

	private final SerializationHints serializationHints = new SerializationHints();

	@Test
	void registerTypeTwiceExposesOneHint() {
		this.serializationHints.registerType(URL.class);
		this.serializationHints.registerType(TypeReference.of(URL.class.getName()));
		assertThat(this.serializationHints.javaSerializationHints()).singleElement()
				.extracting(JavaSerializationHint::getType).isEqualTo(TypeReference.of(URL.class));
	}

}
