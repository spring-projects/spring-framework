/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.web.servlet.setup;

import jakarta.servlet.http.HttpSession;

import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link MockMvcConfigurer} that stores and re-uses the HTTP session across
 * multiple requests performed through the same {@code MockMvc} instance.
 *
 * <p>Example use:
 * <pre class="code">
 * import static org.springframework.test.web.servlet.setup.SharedHttpSessionConfigurer.sharedHttpSession;
 *
 * // ...
 *
 * MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
 *         .apply(sharedHttpSession())
 *         .build();
 *
 * // Use mockMvc to perform requests ...
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class SharedHttpSessionConfigurer implements MockMvcConfigurer {

	@Nullable
	private HttpSession session;


	@Override
	public void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
		builder.alwaysDo(result -> this.session = result.getRequest().getSession(false));
	}

	@Override
	public RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder,
			WebApplicationContext context) {

		return request -> {
			if (this.session != null) {
				request.setSession(this.session);
			}
			return request;
		};
	}

	public static SharedHttpSessionConfigurer sharedHttpSession() {
		return new SharedHttpSessionConfigurer();
	}

}
