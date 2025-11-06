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

package org.springframework.web.service.registry;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.echo.EchoA;
import org.springframework.web.service.registry.echo.EchoB;
import org.springframework.web.service.registry.greeting.GreetingA;
import org.springframework.web.service.registry.greeting.GreetingB;
import org.springframework.web.testfixture.http.client.MockClientHttpRequest;
import org.springframework.web.testfixture.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.web.service.registry.HttpServiceGroup.ClientType.REST_CLIENT;

/**
 * Unit tests for {@link HttpServiceProxyRegistryFactoryBean}.
 * @author Rossen Stoyanchev
 */
public class HttpServiceProxyRegistryFactoryBeanTests {

	@Test
	void twoGroups() {
		String echoName = "echo";
		String greetingName = "greeting";
		GroupsMetadata groupsMetadata = new GroupsMetadata();

		List<String> echoServices = List.of(EchoA.class.getName(), EchoB.class.getName());
		groupsMetadata.getOrCreateGroup(echoName, REST_CLIENT).httpServiceTypeNames().addAll(echoServices);

		List<String> greetingServices = List.of(GreetingA.class.getName(), GreetingB.class.getName());
		groupsMetadata.getOrCreateGroup(greetingName, REST_CLIENT).httpServiceTypeNames().addAll(greetingServices);

		Predicate<HttpServiceGroup> echoFilter = group -> group.name().equals(echoName);
		Predicate<HttpServiceGroup> greetingFilter = group -> group.name().equals(greetingName);
		TestConfigurer groupConfigurer = new TestConfigurer(List.of(echoFilter, greetingFilter));

		HttpServiceProxyRegistry registry = initProxyRegistry(groupConfigurer, groupsMetadata);
		assertThat(registry.getGroupNames()).containsExactlyInAnyOrder(echoName, greetingName);
		assertThat(registry.getClientTypesInGroup(echoName)).containsExactlyInAnyOrder(EchoA.class, EchoB.class);
		assertThat(registry.getClientTypesInGroup(greetingName)).containsExactlyInAnyOrder(GreetingA.class, GreetingB.class);

		assertThat(groupConfigurer.invocations)
				.containsKeys(echoFilter, greetingFilter)
				.containsEntry(echoFilter, List.of(echoName))
				.containsEntry(greetingFilter, List.of(greetingName));
	}

	@Test
	void initializeClientBuilder() throws Exception {
		GroupsMetadata groupsMetadata = new GroupsMetadata();
		groupsMetadata.getOrCreateGroup("echo", REST_CLIENT).httpServiceTypeNames().add(EchoA.class.getName());

		ClientHttpRequestFactory requestFactory = Mockito.mock(ClientHttpRequestFactory.class);
		MockClientHttpRequest request = new MockClientHttpRequest();
		request.setResponse(new MockClientHttpResponse());
		given(requestFactory.createRequest(any(), any())).willReturn(request);

		RestClient.Builder clientBuilder = RestClient.builder().baseUrl("/").requestFactory(requestFactory);
		RestClientHttpServiceGroupConfigurer groupConfigurer = groups -> groups.forEachClient(group -> clientBuilder);

		HttpServiceProxyRegistry registry = initProxyRegistry(groupConfigurer, groupsMetadata);
		registry.getClient(EchoA.class).handle("foo");

		verify(requestFactory, atLeastOnce()).createRequest(any(), any());
	}

	private HttpServiceProxyRegistry initProxyRegistry(
			RestClientHttpServiceGroupConfigurer groupConfigurer, GroupsMetadata groupsMetadata) {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean(RestClientHttpServiceGroupConfigurer.class, () -> groupConfigurer);
		context.refresh();

		HttpServiceProxyRegistryFactoryBean factoryBean = new HttpServiceProxyRegistryFactoryBean(groupsMetadata);
		factoryBean.setApplicationContext(context);
		factoryBean.setBeanClassLoader(getClass().getClassLoader());
		factoryBean.afterPropertiesSet();

		return factoryBean.getObject();
	}


	private static class TestConfigurer implements RestClientHttpServiceGroupConfigurer {

		private final List<Predicate<HttpServiceGroup>> filters;

		private final MultiValueMap<Predicate<HttpServiceGroup>, String> invocations = new LinkedMultiValueMap<>();

		TestConfigurer(List<Predicate<HttpServiceGroup>> filters) {
			this.filters = filters;
		}

		@Override
		public void configureGroups(Groups<RestClient.Builder> groups) {
			filters.forEach(filter -> groups.filter(filter)
					.forEachClient((group, builder) -> invocations.add(filter, group.name())));
		}
	}

	static class MyC implements HttpServiceGroupConfigurer.GroupCallback<RestClient> {

		@Override
		public void withGroup(HttpServiceGroup group, RestClient clientBuilder, HttpServiceProxyFactory.Builder factoryBuilder) {

		}
	}

}
