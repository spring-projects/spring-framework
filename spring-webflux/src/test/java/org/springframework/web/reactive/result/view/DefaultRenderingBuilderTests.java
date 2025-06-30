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

package org.springframework.web.reactive.result.view;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultRenderingBuilder}.
 *
 * @author Rossen Stoyanchev
 */
class DefaultRenderingBuilderTests {

	@Test
	void defaultValues() {
		Rendering rendering = Rendering.view("abc").build();

		assertThat(rendering.view()).isEqualTo("abc");
		assertThat(rendering.modelAttributes()).isEqualTo(Collections.emptyMap());
		assertThat(rendering.status()).isNull();
		assertThat(rendering.headers().isEmpty()).isTrue();
	}

	@Test
	void defaultValuesForRedirect() {
		Rendering rendering = Rendering.redirectTo("abc").build();

		Object view = rendering.view();
		assertThat(view).isExactlyInstanceOf(RedirectView.class);
		RedirectView redirectView = (RedirectView) view;
		assertThat(redirectView.getUrl()).isEqualTo("abc");
		assertThat(redirectView.isContextRelative()).isTrue();
		assertThat(redirectView.isPropagateQuery()).isFalse();
	}

	@Test
	void viewName() {
		Rendering rendering = Rendering.view("foo").build();

		assertThat(rendering.view()).isEqualTo("foo");
	}

	@Test
	void modelAttribute() {
		Foo foo = new Foo();
		Rendering rendering = Rendering.view("foo").modelAttribute(foo).build();

		assertThat(rendering.modelAttributes()).isEqualTo(Collections.singletonMap("foo", foo));
	}

	@Test
	void modelAttributes() {
		Foo foo = new Foo();
		Bar bar = new Bar();
		Rendering rendering = Rendering.view("foo").modelAttributes(foo, bar).build();

		Map<String, Object> map = new LinkedHashMap<>(2);
		map.put("foo", foo);
		map.put("bar", bar);
		assertThat(rendering.modelAttributes()).isEqualTo(map);
	}

	@Test
	void model() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("foo", new Foo());
		map.put("bar", new Bar());
		Rendering rendering = Rendering.view("foo").model(map).build();

		assertThat(rendering.modelAttributes()).isEqualTo(map);
	}

	@Test
	void header() {
		Rendering rendering = Rendering.view("foo").header("foo", "bar").build();

		assertThat(rendering.headers().size()).isOne();
		assertThat(rendering.headers().get("foo")).isEqualTo(Collections.singletonList("bar"));
	}

	@Test
	void httpHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");
		Rendering rendering = Rendering.view("foo").headers(headers).build();

		assertThat(rendering.headers()).isEqualTo(headers);
	}

	@Test
	void redirectWithAbsoluteUrl() {
		Rendering rendering = Rendering.redirectTo("foo").contextRelative(false).build();

		Object view = rendering.view();
		assertThat(view).isExactlyInstanceOf(RedirectView.class);
		assertThat(((RedirectView) view).isContextRelative()).isFalse();
	}

	@Test
	void redirectWithPropagateQuery() {
		Rendering rendering = Rendering.redirectTo("foo").propagateQuery(true).build();

		Object view = rendering.view();
		assertThat(view).isExactlyInstanceOf(RedirectView.class);
		assertThat(((RedirectView) view).isPropagateQuery()).isTrue();
	}

	@Test  // gh-33498
	void redirectWithCustomStatus() {
		HttpStatus status = HttpStatus.MOVED_PERMANENTLY;
		Rendering rendering = Rendering.redirectTo("foo").status(status).build();

		Object view = rendering.view();
		assertThat(view).isExactlyInstanceOf(RedirectView.class);
		assertThat(((RedirectView) view).getStatusCode()).isEqualTo(status);
	}


	private static class Foo {}

	private static class Bar {}

}
