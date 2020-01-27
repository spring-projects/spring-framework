/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;
import org.yaml.snakeyaml.scanner.ScannerException;

import org.springframework.beans.factory.config.YamlProcessor.DocumentMatcher;
import org.springframework.beans.factory.config.YamlProcessor.MatchStatus;
import org.springframework.beans.factory.config.YamlProcessor.ResolutionMethod;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link YamlPropertiesFactoryBean}.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 */
public class YamlPropertiesFactoryBeanTests {

	@Test
	public void loadResource() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource("foo: bar\nspam:\n  foo: baz".getBytes()));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo")).isEqualTo("bar");
		assertThat(properties.getProperty("spam.foo")).isEqualTo("baz");
	}

	@Test
	public void badResource() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource("foo: bar\ncd\nspam:\n  foo: baz".getBytes()));
		assertThatExceptionOfType(ScannerException.class)
			.isThrownBy(factory::getObject)
			.withMessageContaining("line 3, column 1");
	}

	@Test
	public void loadResourcesWithOverride() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(
				new ByteArrayResource("foo: bar\nspam:\n  foo: baz".getBytes()),
				new ByteArrayResource("foo:\n  bar: spam".getBytes()));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo")).isEqualTo("bar");
		assertThat(properties.getProperty("spam.foo")).isEqualTo("baz");
		assertThat(properties.getProperty("foo.bar")).isEqualTo("spam");
	}

	@Test
	public void loadResourcesWithInternalOverride() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource(
				"foo: bar\nspam:\n  foo: baz\nfoo: bucket".getBytes()));
		assertThatExceptionOfType(DuplicateKeyException.class).isThrownBy(factory::getObject);
	}

	@Test
	public void loadResourcesWithNestedInternalOverride() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource(
				"foo:\n  bar: spam\n  foo: baz\nbreak: it\nfoo: bucket".getBytes()));
		assertThatExceptionOfType(DuplicateKeyException.class).isThrownBy(factory::getObject);
	}

	@Test
	public void loadResourceWithMultipleDocuments() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource(
				"foo: bar\nspam: baz\n---\nfoo: bag".getBytes()));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo")).isEqualTo("bag");
		assertThat(properties.getProperty("spam")).isEqualTo("baz");
	}

	@Test
	public void loadResourceWithSelectedDocuments() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource(
				"foo: bar\nspam: baz\n---\nfoo: bag\nspam: bad".getBytes()));
		factory.setDocumentMatchers(properties -> ("bag".equals(properties.getProperty("foo")) ?
				MatchStatus.FOUND : MatchStatus.NOT_FOUND));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo")).isEqualTo("bag");
		assertThat(properties.getProperty("spam")).isEqualTo("bad");
	}

	@Test
	public void loadResourceWithDefaultMatch() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setMatchDefault(true);
		factory.setResources(new ByteArrayResource(
				"one: two\n---\nfoo: bar\nspam: baz\n---\nfoo: bag\nspam: bad".getBytes()));
		factory.setDocumentMatchers(properties -> {
			if (!properties.containsKey("foo")) {
				return MatchStatus.ABSTAIN;
			}
			return ("bag".equals(properties.getProperty("foo")) ?
					MatchStatus.FOUND : MatchStatus.NOT_FOUND);
		});
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo")).isEqualTo("bag");
		assertThat(properties.getProperty("spam")).isEqualTo("bad");
		assertThat(properties.getProperty("one")).isEqualTo("two");
	}

	@Test
	public void loadResourceWithoutDefaultMatch() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setMatchDefault(false);
		factory.setResources(new ByteArrayResource(
				"one: two\n---\nfoo: bar\nspam: baz\n---\nfoo: bag\nspam: bad".getBytes()));
		factory.setDocumentMatchers(new DocumentMatcher() {
			@Override
			public MatchStatus matches(Properties properties) {
				if (!properties.containsKey("foo")) {
					return MatchStatus.ABSTAIN;
				}
				return ("bag".equals(properties.getProperty("foo")) ?
						MatchStatus.FOUND : MatchStatus.NOT_FOUND);
			}
		});
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo")).isEqualTo("bag");
		assertThat(properties.getProperty("spam")).isEqualTo("bad");
		assertThat(properties.getProperty("one")).isNull();
	}

	@Test
	public void loadResourceWithDefaultMatchSkippingMissedMatch() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setMatchDefault(true);
		factory.setResources(new ByteArrayResource(
				"one: two\n---\nfoo: bag\nspam: bad\n---\nfoo: bar\nspam: baz".getBytes()));
		factory.setDocumentMatchers(properties -> {
			if (!properties.containsKey("foo")) {
				return MatchStatus.ABSTAIN;
			}
			return ("bag".equals(properties.getProperty("foo")) ?
					MatchStatus.FOUND : MatchStatus.NOT_FOUND);
		});
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo")).isEqualTo("bag");
		assertThat(properties.getProperty("spam")).isEqualTo("bad");
		assertThat(properties.getProperty("one")).isEqualTo("two");
	}

	@Test
	public void loadNonExistentResource() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResolutionMethod(ResolutionMethod.OVERRIDE_AND_IGNORE);
		factory.setResources(new ClassPathResource("no-such-file.yml"));
		Properties properties = factory.getObject();
		assertThat(properties).isEmpty();
	}

	@Test
	public void loadNull() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource("foo: bar\nspam:".getBytes()));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo")).isEqualTo("bar");
		assertThat(properties.getProperty("spam")).isEqualTo("");
	}

	@Test
	public void loadEmptyArrayValue() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource("a: alpha\ntest: []".getBytes()));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("a")).isEqualTo("alpha");
		assertThat(properties.getProperty("test")).isEqualTo("");
	}

	@Test
	public void loadArrayOfString() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource("foo:\n- bar\n- baz".getBytes()));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo[0]")).isEqualTo("bar");
		assertThat(properties.getProperty("foo[1]")).isEqualTo("baz");
		assertThat(properties.get("foo")).isNull();
	}

	@Test
	public void loadArrayOfInteger() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource("foo:\n- 1\n- 2".getBytes()));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo[0]")).isEqualTo("1");
		assertThat(properties.getProperty("foo[1]")).isEqualTo("2");
		assertThat(properties.get("foo")).isNull();
	}

	@Test
	public void loadArrayOfObject() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource(
				"foo:\n- bar:\n    spam: crap\n- baz\n- one: two\n  three: four".getBytes()
		));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo[0].bar.spam")).isEqualTo("crap");
		assertThat(properties.getProperty("foo[1]")).isEqualTo("baz");
		assertThat(properties.getProperty("foo[2].one")).isEqualTo("two");
		assertThat(properties.getProperty("foo[2].three")).isEqualTo("four");
		assertThat(properties.get("foo")).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void yaml() {
		Yaml yaml = new Yaml();
		Map<String, ?> map = yaml.loadAs("foo: bar\nspam:\n  foo: baz", Map.class);
		assertThat(map.get("foo")).isEqualTo("bar");
		assertThat(((Map<String, Object>) map.get("spam")).get("foo")).isEqualTo("baz");
	}

}
