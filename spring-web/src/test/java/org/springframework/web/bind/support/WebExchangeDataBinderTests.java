/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.bind.support;

import java.beans.PropertyEditorSupport;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.FormHttpMessageWriter;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.core.ResolvableType.forClassWithGenerics;

/**
 * Tests for {@link WebExchangeDataBinder}.
 *
 * @author Rossen Stoyanchev
 */
class WebExchangeDataBinderTests {

	private TestBean testBean;

	private WebExchangeDataBinder binder;


	@BeforeEach
	void setup() {
		this.testBean = new TestBean();
		this.binder = new WebExchangeDataBinder(this.testBean, "person");
		this.binder.registerCustomEditor(ITestBean.class, new TestBeanPropertyEditor());
	}


	@Test
	void testBindingWithNestedObjectCreation() {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("spouse", "someValue");
		formData.add("spouse.name", "test");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));

		assertThat(this.testBean.getSpouse()).isNotNull();
		assertThat(testBean.getSpouse().getName()).isEqualTo("test");
	}

	@Test
	void testFieldPrefixCausesFieldReset() {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("_postProcessed", "visible");
		formData.add("postProcessed", "on");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertThat(this.testBean.isPostProcessed()).isTrue();

		formData.remove("postProcessed");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertThat(this.testBean.isPostProcessed()).isFalse();
	}

	@Test
	void testFieldPrefixCausesFieldResetWithIgnoreUnknownFields() {
		this.binder.setIgnoreUnknownFields(false);

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("_postProcessed", "visible");
		formData.add("postProcessed", "on");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertThat(this.testBean.isPostProcessed()).isTrue();

		formData.remove("postProcessed");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertThat(this.testBean.isPostProcessed()).isFalse();
	}

	@Test // gh-25836
	public void testFieldWithEmptyArrayIndex() {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("stringArray[]", "ONE");
		formData.add("stringArray[]", "TWO");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertThat(this.testBean.getStringArray()).containsExactly("ONE", "TWO");
	}

	@Test
	void testFieldDefault() {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("!postProcessed", "off");
		formData.add("postProcessed", "on");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertThat(this.testBean.isPostProcessed()).isTrue();

		formData.remove("postProcessed");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertThat(this.testBean.isPostProcessed()).isFalse();
	}

	@Test
	void testFieldDefaultPreemptsFieldMarker() {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("!postProcessed", "on");
		formData.add("_postProcessed", "visible");
		formData.add("postProcessed", "on");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertThat(this.testBean.isPostProcessed()).isTrue();

		formData.remove("postProcessed");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertThat(this.testBean.isPostProcessed()).isTrue();

		formData.remove("!postProcessed");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertThat(this.testBean.isPostProcessed()).isFalse();
	}

	@Test
	void testFieldDefaultNonBoolean() {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("!name", "anonymous");
		formData.add("name", "Scott");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertThat(this.testBean.getName()).isEqualTo("Scott");

		formData.remove("name");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertThat(this.testBean.getName()).isEqualTo("anonymous");
	}

	@Test
	void testWithCommaSeparatedStringArray() {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("stringArray", "bar");
		formData.add("stringArray", "abc");
		formData.add("stringArray", "123,def");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertThat(this.testBean.getStringArray().length).as("Expected all three items to be bound").isEqualTo(3);

		formData.remove("stringArray");
		formData.add("stringArray", "123,def");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertThat(this.testBean.getStringArray().length).as("Expected only 1 item to be bound").isEqualTo(1);
	}

	@Test
	void testBindingWithNestedObjectCreationAndWrongOrder() {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("spouse.name", "test");
		formData.add("spouse", "someValue");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));

		assertThat(this.testBean.getSpouse()).isNotNull();
		assertThat(this.testBean.getSpouse().getName()).isEqualTo("test");
	}

	@Test
	void testBindingWithQueryParams() {
		String url = "/path?spouse=someValue&spouse.name=test";
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(url));
		this.binder.bind(exchange).block(Duration.ofSeconds(5));

		assertThat(this.testBean.getSpouse()).isNotNull();
		assertThat(this.testBean.getSpouse().getName()).isEqualTo("test");
	}

	@Test
	void testMultipart() {

		MultipartBean bean = new MultipartBean();
		WebExchangeDataBinder binder = new WebExchangeDataBinder(bean);

		MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
		data.add("name", "bar");
		data.add("someList", "123");
		data.add("someList", "abc");
		data.add("someArray", "dec");
		data.add("someArray", "456");
		data.add("part", new ClassPathResource("org/springframework/http/codec/multipart/foo.txt"));
		data.add("somePartList", new ClassPathResource("org/springframework/http/codec/multipart/foo.txt"));
		data.add("somePartList", new ClassPathResource("/org/springframework/web/spring.png"));
		binder.bind(exchangeMultipart(data)).block(Duration.ofMillis(5000));

		assertThat(bean.getName()).isEqualTo("bar");
		assertThat(bean.getSomeList()).isEqualTo(Arrays.asList("123", "abc"));
		assertThat(bean.getSomeArray()).isEqualTo(new String[] {"dec", "456"});
		assertThat(bean.getPart().filename()).isEqualTo("foo.txt");
		assertThat(bean.getSomePartList()).hasSize(2);
		assertThat(bean.getSomePartList().get(0).filename()).isEqualTo("foo.txt");
		assertThat(bean.getSomePartList().get(1).filename()).isEqualTo("spring.png");
	}

	@Test
	void testMultipartDataClass() {
		WebExchangeDataBinder binder = new WebExchangeDataBinder(null);
		binder.setTargetType(ResolvableType.forClass(MultipartDataClass.class));

		MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
		data.add("part", new ClassPathResource("org/springframework/http/codec/multipart/foo.txt"));
		binder.construct(exchangeMultipart(data)).block(Duration.ofMillis(5000));

		MultipartDataClass bean = (MultipartDataClass) binder.getTarget();
		assertThat(bean.getPart().filename()).isEqualTo("foo.txt");
		assertThat(bean.getNullablePart()).isNull(); // gh-31778
	}


	private ServerWebExchange exchange(MultiValueMap<String, String> formData) {

		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, "/");

		new FormHttpMessageWriter().write(Mono.just(formData),
				forClassWithGenerics(MultiValueMap.class, String.class, String.class),
				MediaType.APPLICATION_FORM_URLENCODED, request, Collections.emptyMap()).block();

		return MockServerWebExchange.from(
				MockServerHttpRequest
						.post("/")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.body(request.getBody()));
	}

	private ServerWebExchange exchangeMultipart(MultiValueMap<String, ?> multipartData) {

		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, "/");

		new MultipartHttpMessageWriter().write(Mono.just(multipartData), forClass(MultiValueMap.class),
				MediaType.MULTIPART_FORM_DATA, request, Collections.emptyMap()).block();

		MockServerHttpRequest serverRequest = MockServerHttpRequest.post("/")
				.contentType(request.getHeaders().getContentType())
				.body(request.getBody());

		return MockServerWebExchange.from(serverRequest);
	}


	private static class TestBeanPropertyEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) {
			setValue(new TestBean());
		}
	}

	@SuppressWarnings("unused")
	private static class MultipartBean {

		private String name;

		private List<?> someList;

		private String[] someArray;

		private FilePart part;

		private List<FilePart> somePartList;


		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<?> getSomeList() {
			return this.someList;
		}

		public void setSomeList(List<?> someList) {
			this.someList = someList;
		}

		public String[] getSomeArray() {
			return this.someArray;
		}

		public void setSomeArray(String[] someArray) {
			this.someArray = someArray;
		}

		public FilePart getPart() {
			return this.part;
		}

		public void setPart(FilePart part) {
			this.part = part;
		}

		public List<FilePart> getSomePartList() {
			return this.somePartList;
		}

		public void setSomePartList(List<FilePart> somePartList) {
			this.somePartList = somePartList;
		}
	}


	static class MultipartDataClass {

		private final FilePart part;

		private final @Nullable FilePart nullablePart;

		MultipartDataClass(FilePart part, @Nullable FilePart nullablePart) {
			this.part = part;
			this.nullablePart = nullablePart;
		}

		public FilePart getPart() {
			return part;
		}

		public @Nullable FilePart getNullablePart() {
			return nullablePart;
		}
	}

}
