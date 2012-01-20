/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.mvc.annotation;

import java.lang.reflect.Method;

import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * SPI for resolving custom return values from a specific handler method. Typically implemented to detect special return
 * types, resolving well-known result values for them.
 *
 * <p>A typical implementation could look like as follows:
 *
 * <pre class="code">
 * public class MyModelAndViewResolver implements ModelAndViewResolver {
 *
 *   public ModelAndView resolveModelAndView(Method handlerMethod,
 *		   			Class handlerType,
 *		   			Object returnValue,
 *		 			ExtendedModelMap implicitModel,
 *		 			NativeWebRequest webRequest) {
 *     if (returnValue instanceof MySpecialRetVal.class)) {
 *       return new MySpecialRetVal(returnValue);
 *     }
 *     return UNRESOLVED;
 *   }
 * }</pre>
 *
 * @author Arjen Poutsma
 * @see org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter#setCustomModelAndViewResolvers
 * @see org.springframework.web.portlet.mvc.annotation.AnnotationMethodHandlerAdapter#setCustomModelAndViewResolvers
 * @since 3.0
 */
public interface ModelAndViewResolver {

	/** Marker to be returned when the resolver does not know how to handle the given method parameter. */
	ModelAndView UNRESOLVED = new ModelAndView();

	ModelAndView resolveModelAndView(Method handlerMethod,
			Class handlerType,
			Object returnValue,
			ExtendedModelMap implicitModel,
			NativeWebRequest webRequest);
}
