/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with {@link ServletCookieValueMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
class ServletCookieValueMethodArgumentResolverTests {

	private ServletCookieValueMethodArgumentResolver resolver;

	private MockHttpServletRequest request;

	private ServletWebRequest webRequest;

	private MethodParameter cookieParameter;
	private MethodParameter cookieStringParameter;


	@BeforeEach
	void setup() throws Exception {
		resolver = new ServletCookieValueMethodArgumentResolver(null);
		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request, new MockHttpServletResponse());

		Method method = getClass().getMethod("params", Cookie.class, String.class);
		cookieParameter = new SynthesizingMethodParameter(method, 0);
		cookieStringParameter = new SynthesizingMethodParameter(method, 1);
	}


	@Test
	void resolveCookieArgument() throws Exception {
		Cookie expected = new Cookie("name", "foo");
		request.setCookies(expected);

		Cookie result = (Cookie) resolver.resolveArgument(cookieParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isEqualTo(expected);
	}

	@Test
	void resolveCookieStringArgument() throws Exception {
		Cookie cookie = new Cookie("name", "foo");
		request.setCookies(cookie);

		String result = (String) resolver.resolveArgument(cookieStringParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isEqualTo(cookie.getValue());
	}

	@Test // gh-26989
	void resolveCookieWithEncodingTurnedOff() throws Exception {
		Cookie cookie = new Cookie("name", "Tl=Q/0AUSOx[n)2z4(t]20FZv#?[Ge%H");
		request.setCookies(cookie);

		this.resolver.setUrlDecode(false);
		String result = (String) resolver.resolveArgument(cookieStringParameter, null, webRequest, null);

		assertThat(result).as("Invalid result").isEqualTo(cookie.getValue());
	}


	public void params(
			@CookieValue("name") Cookie cookie,
			@CookieValue(name = "name", defaultValue = "bar") String cookieString) {
	}

}
