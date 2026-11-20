/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind.support;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * SPI for resolving custom arguments for a specific handler method parameter.
 * Typically implemented to detect special parameter types, resolving
 * well-known argument values for them.
 *
 * <p>A typical implementation could look like as follows:
 *
 * <pre class="code">
 * public class MySpecialArgumentResolver implements WebArgumentResolver {
 *
 *   public Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) {
 *     if (methodParameter.getParameterType().equals(MySpecialArg.class)) {
 *       return new MySpecialArg("myValue");
 *     }
 *     return UNRESOLVED;
 *   }
 * }</pre>
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#setCustomArgumentResolvers
 */
@FunctionalInterface
public interface WebArgumentResolver {

	/**
	 * Marker to be returned when the resolver does not know how to
	 * handle the given method parameter.
	 */
	Object UNRESOLVED = new Object();


	/**
	 * Resolve an argument for the given handler method parameter within the given web request.
	 * @param methodParameter the handler method parameter to resolve
	 * @param webRequest the current web request, allowing access to the native request as well
	 * @return the argument value, or {@code UNRESOLVED} if not resolvable
	 * @throws Exception in case of resolution failure
	 */
	@Nullable Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) throws Exception;

}
