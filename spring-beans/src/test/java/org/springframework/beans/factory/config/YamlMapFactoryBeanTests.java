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

package org.springframework.beans.factory.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link YamlMapFactoryBean}.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 */
class YamlMapFactoryBeanTests {

	private final YamlMapFactoryBean factory = new YamlMapFactoryBean();


	@Test
	void testSetIgnoreResourceNotFound() {
		this.factory.setResolutionMethod(YamlMapFactoryBean.ResolutionMethod.OVERRIDE_AND_IGNORE);
		this.factory.setResources(new FileSystemResource("non-exsitent-file.yml"));
		assertThat(this.factory.getObject()).isEmpty();
	}

	@Test
	void testSetBarfOnResourceNotFound() {
		assertThatIllegalStateException().isThrownBy(() -> {
				this.factory.setResources(new FileSystemResource("non-exsitent-file.yml"));
				this.factory.getObject().size();
		});
	}

	@Test
	void testGetObject() {
		this.factory.setResources(new ByteArrayResource("foo: bar".getBytes()));
		assertThat(this.factory.getObject()).hasSize(1);
	}

	@SuppressWarnings("unchecked")
	@Test
	void testOverrideAndRemoveDefaults() {
		this.factory.setResources(new ByteArrayResource("foo:\n  bar: spam".getBytes()),
				new ByteArrayResource("foo:\n  spam: bar".getBytes()));

		assertThat(this.factory.getObject()).hasSize(1);
		assertThat(((Map<String, Object>) this.factory.getObject().get("foo"))).hasSize(2);
	}

	@Test
	void testFirstFound() {
		this.factory.setResolutionMethod(YamlProcessor.ResolutionMethod.FIRST_FOUND);
		this.factory.setResources(new AbstractResource() {
			@Override
			public String getDescription() {
				return "non-existent";
			}
			@Override
			public InputStream getInputStream() throws IOException {
				throw new IOException("planned");
			}
		}, new ByteArrayResource("foo:\n  spam: bar".getBytes()));

		assertThat(this.factory.getObject()).hasSize(1);
	}

	@Test
	void testMapWithPeriodsInKey() {
		this.factory.setResources(new ByteArrayResource("foo:\n  ? key1.key2\n  : value".getBytes()));
		Map<String, Object> map = this.factory.getObject();

		assertThat(map).hasSize(1);
		assertThat(map.containsKey("foo")).isTrue();
		Object object = map.get("foo");
		assertThat(object).isInstanceOf(LinkedHashMap.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> sub = (Map<String, Object>) object;
		assertThat(sub.containsKey("key1.key2")).isTrue();
		assertThat(sub.get("key1.key2")).isEqualTo("value");
	}

	@Test
	void testMapWithIntegerValue() {
		this.factory.setResources(new ByteArrayResource("foo:\n  ? key1.key2\n  : 3".getBytes()));
		Map<String, Object> map = this.factory.getObject();

		assertThat(map).hasSize(1);
		assertThat(map.containsKey("foo")).isTrue();
		Object object = map.get("foo");
		assertThat(object).isInstanceOf(LinkedHashMap.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> sub = (Map<String, Object>) object;
		assertThat(sub).hasSize(1);
		assertThat(sub.get("key1.key2")).isEqualTo(3);
	}

	@Test
	void testDuplicateKey() {
		this.factory.setResources(new ByteArrayResource("mymap:\n  foo: bar\nmymap:\n  bar: foo".getBytes()));
		assertThatExceptionOfType(DuplicateKeyException.class).isThrownBy(() ->
				this.factory.getObject().get("mymap"));
	}

}
