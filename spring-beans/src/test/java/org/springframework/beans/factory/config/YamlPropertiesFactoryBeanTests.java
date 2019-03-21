/*
 * Copyright 2002-2018 the original author or authors.
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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.beans.factory.config.YamlProcessor.*;

/**
 * Tests for {@link YamlPropertiesFactoryBean}.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 */
public class YamlPropertiesFactoryBeanTests {

	@Rule
	public ExpectedException exception = ExpectedException.none();


	@Test
	public void testLoadResource() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource("foo: bar\nspam:\n  foo: baz".getBytes()));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo"), equalTo("bar"));
		assertThat(properties.getProperty("spam.foo"), equalTo("baz"));
	}

	@Test
	public void testBadResource() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource(
				"foo: bar\ncd\nspam:\n  foo: baz".getBytes()));
		this.exception.expect(ScannerException.class);
		this.exception.expectMessage("line 3, column 1");
		factory.getObject();
	}

	@Test
	public void testLoadResourcesWithOverride() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(
				new ByteArrayResource("foo: bar\nspam:\n  foo: baz".getBytes()),
				new ByteArrayResource("foo:\n  bar: spam".getBytes()));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo"), equalTo("bar"));
		assertThat(properties.getProperty("spam.foo"), equalTo("baz"));
		assertThat(properties.getProperty("foo.bar"), equalTo("spam"));
	}

	@Test
	public void testLoadResourcesWithInternalOverride() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource(
				"foo: bar\nspam:\n  foo: baz\nfoo: bucket".getBytes()));
		exception.expect(ParserException.class);
		factory.getObject();
	}

	@Test
	@Ignore("We can't fail on duplicate keys because the Map is created by the YAML library")
	public void testLoadResourcesWithNestedInternalOverride() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource(
				"foo:\n  bar: spam\n  foo: baz\nbreak: it\nfoo: bucket".getBytes()));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo.bar"), equalTo("spam"));
	}

	@Test
	public void testLoadResourceWithMultipleDocuments() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource(
				"foo: bar\nspam: baz\n---\nfoo: bag".getBytes()));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo"), equalTo("bag"));
		assertThat(properties.getProperty("spam"), equalTo("baz"));
	}

	@Test
	public void testLoadResourceWithSelectedDocuments() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource(
				"foo: bar\nspam: baz\n---\nfoo: bag\nspam: bad".getBytes()));
		factory.setDocumentMatchers(properties -> ("bag".equals(properties.getProperty("foo")) ?
				MatchStatus.FOUND : MatchStatus.NOT_FOUND));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo"), equalTo("bag"));
		assertThat(properties.getProperty("spam"), equalTo("bad"));
	}

	@Test
	public void testLoadResourceWithDefaultMatch() {
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
		assertThat(properties.getProperty("foo"), equalTo("bag"));
		assertThat(properties.getProperty("spam"), equalTo("bad"));
		assertThat(properties.getProperty("one"), equalTo("two"));
	}

	@Test
	public void testLoadResourceWithoutDefaultMatch() {
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
		assertThat(properties.getProperty("foo"), equalTo("bag"));
		assertThat(properties.getProperty("spam"), equalTo("bad"));
		assertThat(properties.getProperty("one"), nullValue());
	}

	@Test
	public void testLoadResourceWithDefaultMatchSkippingMissedMatch() {
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
		assertThat(properties.getProperty("foo"), equalTo("bag"));
		assertThat(properties.getProperty("spam"), equalTo("bad"));
		assertThat(properties.getProperty("one"), equalTo("two"));
	}

	@Test
	public void testLoadNonExistentResource() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResolutionMethod(ResolutionMethod.OVERRIDE_AND_IGNORE);
		factory.setResources(new ClassPathResource("no-such-file.yml"));
		Properties properties = factory.getObject();
		assertThat(properties.size(), equalTo(0));
	}

	@Test
	public void testLoadNull() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource("foo: bar\nspam:".getBytes()));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo"), equalTo("bar"));
		assertThat(properties.getProperty("spam"), equalTo(""));
	}

	@Test
	public void testLoadArrayOfString() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource("foo:\n- bar\n- baz".getBytes()));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo[0]"), equalTo("bar"));
		assertThat(properties.getProperty("foo[1]"), equalTo("baz"));
		assertThat(properties.get("foo"), is(nullValue()));
	}

	@Test
	public void testLoadArrayOfInteger() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource("foo:\n- 1\n- 2".getBytes()));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo[0]"), equalTo("1"));
		assertThat(properties.getProperty("foo[1]"), equalTo("2"));
		assertThat(properties.get("foo"), is(nullValue()));
	}

	@Test
	public void testLoadArrayOfObject() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ByteArrayResource(
				"foo:\n- bar:\n    spam: crap\n- baz\n- one: two\n  three: four".getBytes()
		));
		Properties properties = factory.getObject();
		assertThat(properties.getProperty("foo[0].bar.spam"), equalTo("crap"));
		assertThat(properties.getProperty("foo[1]"), equalTo("baz"));
		assertThat(properties.getProperty("foo[2].one"), equalTo("two"));
		assertThat(properties.getProperty("foo[2].three"), equalTo("four"));
		assertThat(properties.get("foo"), is(nullValue()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testYaml() {
		Yaml yaml = new Yaml();
		Map<String, ?> map = yaml.loadAs("foo: bar\nspam:\n  foo: baz", Map.class);
		assertThat(map.get("foo"), equalTo("bar"));
		assertThat(((Map<String, Object>) map.get("spam")).get("foo"), equalTo("baz"));
	}

}
