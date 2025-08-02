package org.springframework.web.servlet.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Set;

/**
 * {@code MethodInterceptor} is a decorator for {@link HandlerInterceptor} that
 * restricts
 * the execution of the delegate interceptor to specific HTTP methods (e.g.,
 * GET, POST).
 *
 * <p>
 * This allows interceptor logic to be conditionally applied only for the
 * configured methods,
 * enhancing flexibility and reducing unnecessary logic execution for other
 * methods.
 * </p>
 *
 * <p>
 * Example usage: wrap an existing interceptor and allow it to run only for GET
 * and POST requests.
 * </p>
 */
public class MethodInterceptor implements HandlerInterceptor {

	private final HandlerInterceptor interceptor;
	private final Set<String> allowedMethods;

	/**
	 * Constructs a {@code MethodInterceptor}.
	 *
	 * @param interceptor    the original {@link HandlerInterceptor} to be
	 *                       conditionally executed
	 * @param allowedMethods a set of allowed HTTP methods (e.g., "GET", "POST") for
	 *                       which the
	 *                       {@code interceptor} will be invoked
	 */
	public MethodInterceptor(HandlerInterceptor interceptor, Set<String> allowedMethods) {
		this.interceptor = interceptor;
		this.allowedMethods = allowedMethods;
	}

	/**
	 * Invokes {@code preHandle} on the delegate interceptor only if the request
	 * method is allowed.
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (!this.allowedMethods.contains(request.getMethod())) {
			return true;
		}
		return this.interceptor.preHandle(request, response, handler);
	}

	/**
	 * Invokes {@code postHandle} on the delegate interceptor only if the request
	 * method is allowed.
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {
		if (!this.allowedMethods.contains(request.getMethod())) {
			return;
		}
		this.interceptor.postHandle(request, response, handler, modelAndView);
	}

	/**
	 * Invokes {@code afterCompletion} on the delegate interceptor only if the
	 * request method is allowed.
	 */
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) throws Exception {
		if (!this.allowedMethods.contains(request.getMethod())) {
			return;
		}
		this.interceptor.afterCompletion(request, response, handler, ex);
	}
}
