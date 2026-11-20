/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.Principal;

import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PrincipalMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class PrincipalMethodArgumentResolverTests {

	private PrincipalMethodArgumentResolver resolver = new PrincipalMethodArgumentResolver();

	private MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "");

	private ServletWebRequest webRequest = new ServletWebRequest(servletRequest, new MockHttpServletResponse());

	private Method method;


	@BeforeEach
	void setup() throws Exception {
		method = getClass().getMethod("supportedParams", ServletRequest.class, Principal.class);
	}


	@Test
	void principal() throws Exception {
		Principal principal = () -> "Foo";
		servletRequest.setUserPrincipal(principal);

		MethodParameter principalParameter = new MethodParameter(method, 1);
		assertThat(resolver.supportsParameter(principalParameter)).as("Principal not supported").isTrue();

		Object result = resolver.resolveArgument(principalParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(principal);
	}

	@Test
	void principalAsNull() throws Exception {
		MethodParameter principalParameter = new MethodParameter(method, 1);
		assertThat(resolver.supportsParameter(principalParameter)).as("Principal not supported").isTrue();

		Object result = resolver.resolveArgument(principalParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isNull();
	}

	@Test // gh-25780
	void annotatedPrincipal() throws Exception {
		Principal principal = () -> "Foo";
		servletRequest.setUserPrincipal(principal);
		Method principalMethod = getClass().getMethod("supportedParamsWithAnnotatedPrincipal", Principal.class);

		MethodParameter principalParameter = new MethodParameter(principalMethod, 0);
		assertThat(resolver.supportsParameter(principalParameter)).isTrue();
	}


	@SuppressWarnings("unused")
	public void supportedParams(ServletRequest p0, Principal p1) {}

	@Target({ ElementType.PARAMETER })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface AuthenticationPrincipal {}

	@SuppressWarnings("unused")
	public void supportedParamsWithAnnotatedPrincipal(@AuthenticationPrincipal Principal p) {}

}
