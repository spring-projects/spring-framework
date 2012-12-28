/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

/**
 * <p>Extension of {@code SimpleFormController} that supports "cancellation"
 * of form processing. By default, this controller looks for a given parameter in the
 * request, identified by the {@code cancelParamKey}. If this parameter is present,
 * then the controller will return the configured {@code cancelView}, otherwise
 * processing is passed back to the superclass.</p>
 *
 * <p><b><a name="workflow">Workflow
 * (<a href="SimpleFormController.html#workflow">in addition to the superclass</a>):</b><br>
 *  <ol>
 *   <li>Call to {@link #processFormSubmission processFormSubmission} which calls
 *       {@link #isCancelRequest} to see if the incoming request is to cancel the
 *       current form entry. By default, {@link #isCancelRequest} returns {@code true}
 *       if the configured {@code cancelParamKey} exists in the request.
 *       This behavior can be overridden in subclasses.</li>
 *   <li>If {@link #isCancelRequest} returns {@code false}, then the controller
 *       will delegate all processing back to {@link SimpleFormController SimpleFormController},
 *       otherwise it will call the {@link #onCancel} version with all parameters.
 *       By default, that method will delegate to the {@link #onCancel} version with just
 *       the command object, which will in turn simply return the configured
 *       {@code cancelView}. This behavior can be overridden in subclasses.</li>
 *  </ol>
 * </p>
 *
 * <p>Thanks to Erwin Bolwidt for submitting the original prototype
 * of such a cancellable form controller!</p>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2.3
 * @see #setCancelParamKey
 * @see #setCancelView
 * @see #isCancelRequest(javax.servlet.http.HttpServletRequest)
 * @see #onCancel(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, Object)
 * @deprecated as of Spring 3.0, in favor of annotated controllers
 */
@Deprecated
public class CancellableFormController extends SimpleFormController {

	/**
	 * Default parameter triggering the cancel action.
	 * Can be called even with validation errors on the form.
	 */
	private static final String PARAM_CANCEL = "_cancel";


	private String cancelParamKey = PARAM_CANCEL;

	private String cancelView;


	/**
	 * Set the key of the request parameter used to identify a cancel request.
	 * Default is "_cancel".
	 * <p>The parameter is recognized both when sent as a plain parameter
	 * ("_cancel") or when triggered by an image button ("_cancel.x").
   */
	public final void setCancelParamKey(String cancelParamKey) {
		this.cancelParamKey = cancelParamKey;
	}

	/**
	 * Return the key of the request parameter used to identify a cancel request.
	 */
	public final String getCancelParamKey() {
		return this.cancelParamKey;
	}

	/**
	 * Sets the name of the cancel view.
	 */
	public final void setCancelView(String cancelView) {
		this.cancelView = cancelView;
	}

	/**
	 * Gets the name of the cancel view.
	 */
	public final String getCancelView() {
		return this.cancelView;
	}


	/**
	 * Consider an explicit cancel request as a form submission too.
	 * @see #isCancelRequest(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected boolean isFormSubmission(HttpServletRequest request) {
		return super.isFormSubmission(request) || isCancelRequest(request);
	}

	/**
	 * Suppress validation for an explicit cancel request too.
	 * @see #isCancelRequest(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected boolean suppressValidation(HttpServletRequest request, Object command) {
		return super.suppressValidation(request, command) || isCancelRequest(request);
	}

	/**
	 * This implementation first checks to see if the incoming is a cancel request,
	 * through a call to {@link #isCancelRequest}. If so, control is passed to
	 * {@link #onCancel}; otherwise, control is passed up to
	 * {@link SimpleFormController#processFormSubmission}.
	 * @see #isCancelRequest
	 * @see #onCancel(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, Object)
	 * @see SimpleFormController#processFormSubmission
	 */
	@Override
	protected ModelAndView processFormSubmission(
			HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		if (isCancelRequest(request)) {
			return onCancel(request, response, command);
		}
		else {
			return super.processFormSubmission(request, response, command, errors);
		}
	}

	/**
	 * Determine whether the incoming request is a request to cancel the
	 * processing of the current form.
	 * <p>By default, this method returns {@code true} if a parameter
	 * matching the configured {@code cancelParamKey} is present in
	 * the request, otherwise it returns {@code false}. Subclasses may
	 * override this method to provide custom logic to detect a cancel request.
	 * <p>The parameter is recognized both when sent as a plain parameter
	 * ("_cancel") or when triggered by an image button ("_cancel.x").
	 * @param request current HTTP request
	 * @see #setCancelParamKey
	 * @see #PARAM_CANCEL
	 */
	protected boolean isCancelRequest(HttpServletRequest request) {
		return WebUtils.hasSubmitParameter(request, getCancelParamKey());
	}

	/**
	 * Callback method for handling a cancel request. Called if {@link #isCancelRequest}
	 * returns {@code true}.
	 * <p>Default implementation delegates to {@code onCancel(Object)} to return
	 * the configured {@code cancelView}. Subclasses may override either of the two
	 * methods to build a custom {@link ModelAndView ModelAndView} that may contain model
	 * parameters used in the cancel view.
	 * <p>If you simply want to move the user to a new view and you don't want to add
	 * additional model parameters, use {@link #setCancelView(String)} rather than
	 * overriding an {@code onCancel} method.
	 * @param request current servlet request
	 * @param response current servlet response
	 * @param command form object with request parameters bound onto it
	 * @return the prepared model and view, or {@code null}
	 * @throws Exception in case of errors
	 * @see #isCancelRequest(javax.servlet.http.HttpServletRequest)
	 * @see #onCancel(Object)
	 * @see #setCancelView
	 */
	protected ModelAndView onCancel(HttpServletRequest request, HttpServletResponse response, Object command)
			throws Exception {

		return onCancel(command);
	}

	/**
	 * Simple {@code onCancel} version. Called by the default implementation
	 * of the {@code onCancel} version with all parameters.
	 * <p>Default implementation returns eturns the configured {@code cancelView}.
	 * Subclasses may override this method to build a custom {@link ModelAndView ModelAndView}
	 * that may contain model parameters used in the cancel view.
	 * <p>If you simply want to move the user to a new view and you don't want to add
	 * additional model parameters, use {@link #setCancelView(String)} rather than
	 * overriding an {@code onCancel} method.
	 * @param command form object with request parameters bound onto it
	 * @return the prepared model and view, or {@code null}
	 * @throws Exception in case of errors
	 * @see #onCancel(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, Object)
	 * @see #setCancelView
	 */
	protected ModelAndView onCancel(Object command) throws Exception {
		return new ModelAndView(getCancelView());
	}

}
