/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Trivial controller that always returns a pre-configured view and optionally
 * sets the response status code. The view and status can be configured using
 * the provided configuration properties.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rossen Stoyanchev
 */
public class ParameterizableViewController extends AbstractController {

	@Nullable
	private Object view;

	@Nullable
	private HttpStatus statusCode;

	private boolean statusOnly;


	public ParameterizableViewController() {
		super(false);
		setSupportedMethods(HttpMethod.GET.name(), HttpMethod.HEAD.name());
	}

	/**
	 * Set a view name for the ModelAndView to return, to be resolved by the
	 * DispatcherServlet via a ViewResolver. Will override any pre-existing
	 * view name or View.
	 */
	public void setViewName(@Nullable String viewName) {
		this.view = viewName;
	}

	/**
	 * Return the name of the view to delegate to, or {@code null} if using a
	 * View instance.
	 */
	@Nullable
	public String getViewName() {
		return (this.view instanceof String ? (String) this.view : null);
	}

	/**
	 * Set a View object for the ModelAndView to return.
	 * Will override any pre-existing view name or View.
	 * @since 4.1
	 */
	public void setView(View view) {
		this.view = view;
	}

	/**
	 * Return the View object, or {@code null} if we are using a view name
	 * to be resolved by the DispatcherServlet via a ViewResolver.
	 * @since 4.1
	 */
	@Nullable
	public View getView() {
		return (this.view instanceof View ? (View) this.view : null);
	}

	/**
	 * Configure the HTTP status code that this controller should set on the
	 * response.
	 * <p>When a "redirect:" prefixed view name is configured, there is no need
	 * to set this property since RedirectView will do that. However this property
	 * may still be used to override the 3xx status code of {@code RedirectView}.
	 * For full control over redirecting provide a {@code RedirectView} instance.
	 * <p>If the status code is 204 and no view is configured, the request is
	 * fully handled within the controller.
	 * @since 4.1
	 */
	public void setStatusCode(@Nullable HttpStatus statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * Return the configured HTTP status code or {@code null}.
	 * @since 4.1
	 */
	@Nullable
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}


	/**
	 * The property can be used to indicate the request is considered fully
	 * handled within the controller and that no view should be used for rendering.
	 * Useful in combination with {@link #setStatusCode}.
	 * <p>By default this is set to {@code false}.
	 * @since 4.1
	 */
	public void setStatusOnly(boolean statusOnly) {
		this.statusOnly = statusOnly;
	}

	/**
	 * Whether the request is fully handled within the controller.
	 */
	public boolean isStatusOnly() {
		return this.statusOnly;
	}


	/**
	 * Return a ModelAndView object with the specified view name.
	 * <p>The content of the {@link RequestContextUtils#getInputFlashMap
	 * "input" FlashMap} is also added to the model.
	 * @see #getViewName()
	 */
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		String viewName = getViewName();

		if (getStatusCode() != null) {
			if (getStatusCode().is3xxRedirection()) {
				request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, getStatusCode());
				viewName = (viewName != null && !viewName.startsWith("redirect:") ? "redirect:" + viewName : viewName);
			}
			else {
				response.setStatus(getStatusCode().value());
				if (isStatusOnly() || (getStatusCode().equals(HttpStatus.NO_CONTENT) && getViewName() == null)) {
					return null;
				}
			}
		}

		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addAllObjects(RequestContextUtils.getInputFlashMap(request));

		if (getViewName() != null) {
			modelAndView.setViewName(viewName);
		}
		else {
			modelAndView.setView(getView());
		}

		return (isStatusOnly() ? null : modelAndView);
	}

}
