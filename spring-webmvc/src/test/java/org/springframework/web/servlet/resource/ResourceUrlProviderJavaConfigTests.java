/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.resource;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.testfixture.servlet.MockFilterChain;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Integration tests using {@link ResourceUrlEncodingFilter} and
 * {@link ResourceUrlProvider} with the latter configured in Spring MVC Java config.
 *
 * @author Rossen Stoyanchev
 */
class ResourceUrlProviderJavaConfigTests {

	private final TestServlet servlet = new TestServlet();

	private MockFilterChain filterChain;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@BeforeEach
	void setup() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(WebConfig.class);
		context.refresh();

		this.request = new MockHttpServletRequest("GET", "/");
		this.request.setContextPath("/myapp");
		this.response = new MockHttpServletResponse();

		this.filterChain = new MockFilterChain(this.servlet,
				new ResourceUrlEncodingFilter(),
				(request, response, chain) -> {
					Object urlProvider = context.getBean(ResourceUrlProvider.class);
					request.setAttribute(ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR, urlProvider);
					chain.doFilter(request, response);
				});
	}

	@Test
	void resolvePathWithServletMappedAsRoot() throws Exception {
		this.request.setRequestURI("/myapp/index");
		this.request.setServletPath("/index");
		this.filterChain.doFilter(this.request, this.response);

		assertThat(resolvePublicResourceUrlPath("/myapp/resources/foo.css")).isEqualTo("/myapp/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css");
	}

	@Test
	void resolvePathWithServletMappedByPrefix() throws Exception {
		this.request.setRequestURI("/myapp/myservlet/index");
		this.request.setServletPath("/myservlet");
		this.filterChain.doFilter(this.request, this.response);

		assertThat(resolvePublicResourceUrlPath("/myapp/myservlet/resources/foo.css")).isEqualTo("/myapp/myservlet/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css");
	}

	@Test
	void resolvePathNoMatch() throws Exception {
		this.request.setRequestURI("/myapp/myservlet/index");
		this.request.setServletPath("/myservlet");
		this.filterChain.doFilter(this.request, this.response);

		assertThat(resolvePublicResourceUrlPath("/myapp/myservlet/index")).isEqualTo("/myapp/myservlet/index");
	}


	private String resolvePublicResourceUrlPath(String path) {
		return this.servlet.wrappedResponse.encodeURL(path);
	}


	@Configuration
	static class WebConfig extends WebMvcConfigurationSupport {

		@Override
		protected void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/resources/**")
				.addResourceLocations("classpath:org/springframework/web/servlet/resource/test/")
				.resourceChain(true).addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));
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
