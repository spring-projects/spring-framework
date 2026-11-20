/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimpleKeyGenerator} and {@link SimpleKey}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 */
class SimpleKeyGeneratorTests {

	private final SimpleKeyGenerator generator = new SimpleKeyGenerator();


	@Test
	void noValues() {
		Object k1 = generateKey(new Object[] {});
		Object k2 = generateKey(new Object[] {});
		Object k3 = generateKey(new Object[] { "different" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}

	@Test
	void singleValue() {
		Object k1 = generateKey(new Object[] { "a" });
		Object k2 = generateKey(new Object[] { "a" });
		Object k3 = generateKey(new Object[] { "different" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
		assertThat(k1).isEqualTo("a");
	}

	@Test
	void multipleValues() {
		Object k1 = generateKey(new Object[] { "a", 1, "b" });
		Object k2 = generateKey(new Object[] { "a", 1, "b" });
		Object k3 = generateKey(new Object[] { "b", 1, "a" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}

	@Test
	void singleNullValue() {
		Object k1 = generateKey(new Object[] { null });
		Object k2 = generateKey(new Object[] { null });
		Object k3 = generateKey(new Object[] { "different" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
		assertThat(k1).isInstanceOf(SimpleKey.class);
	}

	@Test
	void multipleNullValues() {
		Object k1 = generateKey(new Object[] { "a", null, "b", null });
		Object k2 = generateKey(new Object[] { "a", null, "b", null });
		Object k3 = generateKey(new Object[] { "a", null, "b" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}

	@Test
	void plainArray() {
		Object k1 = generateKey(new Object[] { new String[]{"a", "b"} });
		Object k2 = generateKey(new Object[] { new String[]{"a", "b"} });
		Object k3 = generateKey(new Object[] { new String[]{"b", "a"} });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}

	@Test
	void arrayWithExtraParameter() {
		Object k1 = generateKey(new Object[] { new String[]{"a", "b"}, "c" });
		Object k2 = generateKey(new Object[] { new String[]{"a", "b"}, "c" });
		Object k3 = generateKey(new Object[] { new String[]{"b", "a"}, "c" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}

	@Test
	void serializedKeys() throws Exception {
		Object k1 = SerializationTestUtils.serializeAndDeserialize(generateKey(new Object[] { "a", 1, "b" }));
		Object k2 = SerializationTestUtils.serializeAndDeserialize(generateKey(new Object[] { "a", 1, "b" }));
		Object k3 = SerializationTestUtils.serializeAndDeserialize(generateKey(new Object[] { "b", 1, "a" }));
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}


	private Object generateKey(Object[] arguments) {
		Method method = ReflectionUtils.findMethod(getClass(), "generateKey", Object[].class);
		return this.generator.generate(this, method, arguments);
	}

}
