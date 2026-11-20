/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.setup;

import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.Nullable;

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

	private @Nullable HttpSession session;


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
