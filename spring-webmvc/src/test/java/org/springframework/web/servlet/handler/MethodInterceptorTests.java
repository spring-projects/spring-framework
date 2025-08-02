package org.springframework.web.servlet.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MethodInterceptorTests {

	private final Object handler = new Object();

	@Test
	void preHandle_skippedForDisallowedMethod() throws Exception {
		AtomicBoolean called = new AtomicBoolean(false);
		HandlerInterceptor delegate = new StubInterceptor(called);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();

		MethodInterceptor interceptor = new MethodInterceptor(delegate, Set.of("POST"));
		boolean result = interceptor.preHandle(request, response, handler);

		assertThat(result).isTrue(); 
		assertThat(called.get()).isFalse(); 
	}

	@Test
	void preHandle_invokedForAllowedMethod() throws Exception {
		AtomicBoolean called = new AtomicBoolean(false);
		HandlerInterceptor delegate = new StubInterceptor(called);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();

		MethodInterceptor interceptor = new MethodInterceptor(delegate, Set.of("POST"));
		boolean result = interceptor.preHandle(request, response, handler);

		assertThat(result).isTrue(); 
		assertThat(called.get()).isTrue(); 
	}

	@Test
	void postHandle_skippedForDisallowedMethod() throws Exception {
		AtomicBoolean called = new AtomicBoolean(false);
		HandlerInterceptor delegate = new StubInterceptor(called);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();

		MethodInterceptor interceptor = new MethodInterceptor(delegate, Set.of("POST"));
		interceptor.postHandle(request, response, handler, new ModelAndView());

		assertThat(called.get()).isFalse();
	}

	@Test
	void postHandle_invokedForAllowedMethod() throws Exception {
		AtomicBoolean called = new AtomicBoolean(false);
		HandlerInterceptor delegate = new StubInterceptor(called);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();

		MethodInterceptor interceptor = new MethodInterceptor(delegate, Set.of("POST"));
		interceptor.postHandle(request, response, handler, new ModelAndView());

		assertThat(called.get()).isTrue();
	}

	@Test
	void afterCompletion_invokedForAllowedMethod() throws Exception {
		AtomicBoolean called = new AtomicBoolean(false);
		HandlerInterceptor delegate = new StubInterceptor(called);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();

		MethodInterceptor interceptor = new MethodInterceptor(delegate, Set.of("POST"));
		interceptor.afterCompletion(request, response, handler, null);

		assertThat(called.get()).isTrue();
	}

	@Test
	void afterCompletion_skippedForDisallowedMethod() throws Exception {
		AtomicBoolean called = new AtomicBoolean(false);
		HandlerInterceptor delegate = new StubInterceptor(called);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();

		MethodInterceptor interceptor = new MethodInterceptor(delegate, Set.of("POST"));
		interceptor.afterCompletion(request, response, handler, null);

		assertThat(called.get()).isFalse();
	}

	@Test
	void preHandle_invokedForAllowedMethod1() throws Exception {
		AtomicBoolean called = new AtomicBoolean(false);
		HandlerInterceptor delegate = new StubInterceptor(called);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();

		MethodInterceptor interceptor = new MethodInterceptor(delegate, Set.of("POST"));
		boolean result = interceptor.preHandle(request, response, handler);

		assertThat(result).isTrue();
		assertThat(called.get()).isTrue();
	}

	private static class StubInterceptor implements HandlerInterceptor {
		private final AtomicBoolean flag;

		StubInterceptor(AtomicBoolean flag) {
			this.flag = flag;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
			flag.set(true);
			return true;
		}

		@Override
		public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
				ModelAndView modelAndView) {
			flag.set(true);
		}

		@Override
		public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
				Exception ex) {
			flag.set(true);
		}
	}
}
