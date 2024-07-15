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

package org.springframework.web.service.invoker;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.*;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RequestParamArgumentResolver}.
 *
 * <p>Additional tests for this resolver:
 * <ul>
 * <li>Base class functionality in {@link NamedValueArgumentResolverTests}
 * <li>Form data vs query params in {@link HttpRequestValuesTests}
 * </ul>
 *
 * @author Rossen Stoyanchev
 */
class RequestParamArgumentResolverTests {

	private final TestExchangeAdapter client = new TestExchangeAdapter();

	private final Service service =
			HttpServiceProxyFactory.builderFor(this.client).build().createClient(Service.class);


	@Test
	@SuppressWarnings("unchecked")
	void requestParam() {
		this.service.postForm("value 1", "value 2");

		Object body = this.client.getRequestValues().getBodyValue();
		assertThat(body).isInstanceOf(MultiValueMap.class);
		assertThat((MultiValueMap<String, String>) body).hasSize(2)
				.containsEntry("param1", List.of("value 1"))
				.containsEntry("param2", List.of("value 2"));
	}

	@Test
	void requestParam2() {
		ConversionService conversionService = new DefaultConversionService();
		boolean formatAsSingleValue = false;
		Service service =
				HttpServiceProxyFactory.builderFor(this.client)
						.customArgumentResolver(new RequestParamArgumentResolver(conversionService, formatAsSingleValue))
						.build().createClient(Service.class);
		List<String> collectionParams = List.of("1", "2", "3");
		service.getForm("value 1", collectionParams);

		Object uriVariables = this.client.getRequestValues().getUriVariables();
		assertThat(uriVariables).isNotInstanceOf(MultiValueMap.class)
				.isInstanceOf(HashMap.class);
		assertThat((HashMap<String, String>) uriVariables).hasSize(4)
				.containsEntry("queryParam0", "param1")
				.containsEntry("queryParam0[0]", "value 1")
				.containsEntry("queryParam1", "param2")
				.containsEntry("queryParam1[0]", String.join(",", collectionParams));
	}

	@Test
	@SuppressWarnings("unchecked")
	void requestParam3() {
		ConversionService conversionService = new DefaultConversionService();
		boolean formatAsSingleValue = false;
		Service service =
				HttpServiceProxyFactory.builderFor(this.client)
						.customArgumentResolver(new RequestParamArgumentResolver(conversionService, formatAsSingleValue))
						.build().createClient(Service.class);
		List<String> collectionParams = List.of("1", "2", "3");
		service.putForm("value 1", collectionParams);

		Object body = this.client.getRequestValues().getBodyValue();
		assertThat(body).isInstanceOf(MultiValueMap.class);
		assertThat((MultiValueMap<String, String>) body).hasSize(2)
				.containsEntry("param1", List.of("value 1"))
				.containsEntry("param2", collectionParams);
	}

	@Test
	@SuppressWarnings("unchecked")
	void requestParam4() {
		ConversionService conversionService = new DefaultConversionService();
		boolean formatAsSingleValue = false;
		Service service =
				HttpServiceProxyFactory.builderFor(this.client)
						.customArgumentResolver(new RequestParamArgumentResolver(conversionService, formatAsSingleValue))
						.build().createClient(Service.class);
		List<String> collectionParams = List.of("1", "2", "3");
		service.deleteForm("value 1", collectionParams);

		Object body = this.client.getRequestValues().getBodyValue();
		assertThat(body).isInstanceOf(MultiValueMap.class);
		assertThat((MultiValueMap<String, String>) body).hasSize(2)
				.containsEntry("param1", List.of("value 1"))
				.containsEntry("param2", collectionParams);
	}

	@Test
	@SuppressWarnings("unchecked")
	void requestParam5() {
		ConversionService conversionService = new DefaultConversionService();
		boolean formatAsSingleValue = false;
		Service service =
				HttpServiceProxyFactory.builderFor(this.client)
						.customArgumentResolver(new RequestParamArgumentResolver(conversionService, formatAsSingleValue))
						.build().createClient(Service.class);
		List<String> collectionParams = List.of("1", "2", "3");
		service.patchForm("value 1", collectionParams);

		Object body = this.client.getRequestValues().getBodyValue();
		assertThat(body).isInstanceOf(MultiValueMap.class);
		assertThat((MultiValueMap<String, String>) body).hasSize(2)
				.containsEntry("param1", List.of("value 1"))
				.containsEntry("param2", collectionParams);
	}

	@Test
	@SuppressWarnings("unchecked")
	void requestParam6() {
		ConversionService conversionService = new DefaultConversionService();
		boolean formatAsSingleValue = false;
		Service2 service =
				HttpServiceProxyFactory.builderFor(this.client)
						.customArgumentResolver(new RequestParamArgumentResolver(conversionService, formatAsSingleValue))
						.build().createClient(Service2.class);
		List<String> collectionParams = List.of("1", "2", "3");
		service.getForm("value 1", collectionParams);

		Object body = this.client.getRequestValues().getBodyValue();
		assertThat(body).isInstanceOf(MultiValueMap.class);
		assertThat((MultiValueMap<String, String>) body).hasSize(2)
				.containsEntry("param1", List.of("value 1"))
				.containsEntry("param2", collectionParams);
	}

	@Test
	@SuppressWarnings("unchecked")
	void requestParam7() {
		ConversionService conversionService = new DefaultConversionService();
		boolean formatAsSingleValue = false;
		Service3 service =
				HttpServiceProxyFactory.builderFor(this.client)
						.customArgumentResolver(new RequestParamArgumentResolver(conversionService, formatAsSingleValue))
						.build().createClient(Service3.class);
		List<String> collectionParams = List.of("1", "2", "3");
		service.getForm("value 1", collectionParams);

		Object uriVariables = this.client.getRequestValues().getUriVariables();
		assertThat(uriVariables).isNotInstanceOf(MultiValueMap.class)
				.isInstanceOf(HashMap.class);
		assertThat((HashMap<String, String>) uriVariables).hasSize(4)
				.containsEntry("queryParam0", "param1")
				.containsEntry("queryParam0[0]", "value 1")
				.containsEntry("queryParam1", "param2")
				.containsEntry("queryParam1[0]", String.join(",", collectionParams));
	}


	private interface Service {

		@PostExchange(contentType = "application/x-www-form-urlencoded")
		void postForm(@RequestParam String param1, @RequestParam String param2);

		@GetExchange
		void getForm(@RequestParam String param1, @RequestParam List<String> param2);

		@PutExchange(contentType = "application/x-www-form-urlencoded")
		void putForm(@RequestParam String param1, @RequestParam List<String> param2);

		@PutExchange(contentType = "application/x-www-form-urlencoded")
		void deleteForm(@RequestParam String param1, @RequestParam List<String> param2);

		@PatchExchange(contentType = "application/x-www-form-urlencoded")
		void patchForm(@RequestParam String param1, @RequestParam List<String> param2);
	}

	@HttpExchange(contentType = "application/x-www-form-urlencoded")
	private interface Service2 {

		@GetExchange
		void getForm(@RequestParam String param1, @RequestParam List<String> param2);
	}

	@HttpExchange
	private interface Service3 {

		@GetExchange
		void getForm(@RequestParam String param1, @RequestParam List<String> param2);
	}
}
