/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.util;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SerializationUtils}.
 *
 * @author Dave Syer
 * @author Sam Brannen
 * @since 3.0.5
 */
class SerializationUtilsTests {

	private static final BigInteger FOO = new BigInteger(
			"-9702942423549012526722364838327831379660941553432801565505143675386108883970811292563757558516603356009681061" +
			"5697574744209306031461371833798723505120163874786203211176873686513374052845353833564048");


	@Test
	@SuppressWarnings("deprecation")
	void serializeCycleSunnyDay() {
		assertThat(SerializationUtils.deserialize(SerializationUtils.serialize("foo"))).isEqualTo("foo");
	}

	@Test
	void serializeNonSerializableRecord() {
		record Person(String firstName, String lastName) {}
		Person jane = new Person("Jane", "Doe");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> SerializationUtils.serialize(jane))
			.withCauseExactlyInstanceOf(NotSerializableException.class);
	}

	@Test
	@SuppressWarnings("deprecation")
	void serializeAndDeserializeSerializableRecord() {
		record Person(String firstName, String lastName) implements Serializable {}
		Person jane = new Person("Jane", "Doe");
		assertThat(SerializationUtils.deserialize(SerializationUtils.serialize(jane))).isEqualTo(jane);
	}

	@Test
	@SuppressWarnings("deprecation")
	void deserializeUndefined() {
		assertThatIllegalStateException().isThrownBy(() -> SerializationUtils.deserialize(FOO.toByteArray()));
	}

	@Test
	void serializeNonSerializable() {
		assertThatIllegalArgumentException().isThrownBy(() -> SerializationUtils.serialize(new Object()));
	}

	@Test
	@SuppressWarnings("deprecation")
	void deserializeNonSerializable() {
		assertThatIllegalArgumentException().isThrownBy(() -> SerializationUtils.deserialize("foo".getBytes()));
	}

	@Test
	void serializeNull() {
		assertThat(SerializationUtils.serialize(null)).isNull();
	}

	@Test
	@SuppressWarnings("deprecation")
	void deserializeNull() {
		assertThat(SerializationUtils.deserialize(null)).isNull();
	}

	@Test
	void cloneException() {
		IllegalArgumentException ex = new IllegalArgumentException("foo");
		assertThat(SerializationUtils.clone(ex)).hasMessage("foo").isNotSameAs(ex);
	}

}
