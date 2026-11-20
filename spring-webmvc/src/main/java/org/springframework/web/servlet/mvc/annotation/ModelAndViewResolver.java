/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.annotation;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * SPI for resolving custom return values from a specific handler method.
 * Typically implemented to detect special return types, resolving
 * well-known result values for them.
 *
 * <p>A typical implementation could look like as follows:
 *
 * <pre class="code">
 * public class MyModelAndViewResolver implements ModelAndViewResolver {
 *
 *     public ModelAndView resolveModelAndView(Method handlerMethod, Class handlerType,
 *             Object returnValue, ExtendedModelMap implicitModel, NativeWebRequest webRequest) {
 *         if (returnValue instanceof MySpecialRetVal.class)) {
 *             return new MySpecialRetVal(returnValue);
 *         }
 *         return UNRESOLVED;
 *     }
 * }</pre>
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface ModelAndViewResolver {

	/**
	 * Marker to be returned when the resolver does not know how to handle the given method parameter.
	 */
	ModelAndView UNRESOLVED = new ModelAndView();


	ModelAndView resolveModelAndView(Method handlerMethod, Class<?> handlerType,
			@Nullable Object returnValue, ExtendedModelMap implicitModel, NativeWebRequest webRequest);

}
