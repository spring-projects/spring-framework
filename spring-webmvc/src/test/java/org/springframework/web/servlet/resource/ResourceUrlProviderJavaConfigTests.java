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

package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.util.Arrays;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.test.MockFilterChain;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import static org.junit.Assert.*;


/**
 * Integration tests using {@link ResourceUrlEncodingFilter} and
 * {@link ResourceUrlProvider} with the latter configured in Spring MVC Java config.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceUrlProviderJavaConfigTests {

	private final TestServlet servlet = new TestServlet();

	private MockFilterChain filterChain;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@Before
	@SuppressWarnings("resource")
	public void setup() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(WebConfig.class);
		context.refresh();

		this.request = new MockHttpServletRequest("GET", "/");
		this.request.setContextPath("/myapp");
		this.response = new MockHttpServletResponse();

		this.filterChain = new MockFilterChain(this.servlet,
				new ResourceUrlEncodingFilter(),
				new ResourceUrlProviderExposingFilter(context));
	}

	@Test
	public void resolvePathWithServletMappedAsRoot() throws Exception {
		this.request.setRequestURI("/myapp/index");
		this.request.setServletPath("/index");
		this.filterChain.doFilter(this.request, this.response);

		assertEquals("/myapp/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css",
				resolvePublicResourceUrlPath("/myapp/resources/foo.css"));
	}

	@Test
	public void resolvePathWithServletMappedByPrefix() throws Exception {
		this.request.setRequestURI("/myapp/myservlet/index");
		this.request.setServletPath("/myservlet");
		this.filterChain.doFilter(this.request, this.response);

		assertEquals("/myapp/myservlet/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css",
				resolvePublicResourceUrlPath("/myapp/myservlet/resources/foo.css"));
	}

	@Test
	public void resolvePathNoMatch() throws Exception {
		this.request.setRequestURI("/myapp/myservlet/index");
		this.request.setServletPath("/myservlet");
		this.filterChain.doFilter(this.request, this.response);

		assertEquals("/myapp/myservlet/index", resolvePublicResourceUrlPath("/myapp/myservlet/index"));
	}


	private String resolvePublicResourceUrlPath(String path) {
		return this.servlet.wrappedResponse.encodeURL(path);
	}


	@Configuration
	static class WebConfig extends WebMvcConfigurationSupport {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/resources/**")
				.addResourceLocations("classpath:org/springframework/web/servlet/resource/test/")
				.resourceChain(true).addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));
		}
	}

	@SuppressWarnings("serial")
	private static class ResourceUrlProviderExposingFilter implements Filter {

		private final ApplicationContext context;

		public ResourceUrlProviderExposingFilter(ApplicationContext context) {
			this.context = context;
		}

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {

		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			Object urlProvider = context.getBean(ResourceUrlProvider.class);
			request.setAttribute(ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR, urlProvider);
			chain.doFilter(request, response);
		}

		@Override
		public void destroy() {

		}
	}

	@SuppressWarnings("serial")
	private static class TestServlet extends HttpServlet {

		private HttpServletResponse wrappedResponse;

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response) {
			this.wrappedResponse = response;
		}
	}

}
