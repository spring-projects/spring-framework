/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.web.portlet.mvc;

import java.util.HashMap;
import java.util.Map;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * Form controller for typical wizard-style workflows.
 *
 * <p>In contrast to classic forms, wizards have more than one form view page.
 * Therefore, there are various actions instead of one single submit action:
 * <ul>
 * <li>finish: trying to leave the wizard successfully, i.e. performing its
 * final action, and thus needing a valid state;
 * <li>cancel: leaving the wizard without performing its final action, and
 * thus without regard to the validity of its current state;
 * <li>page change: showing another wizard page, e.g. the next or previous
 * one, with regard to "dirty back" and "dirty forward".
 * </ul>
 *
 * <p>Finish and cancel actions can be triggered by request parameters, named
 * PARAM_FINISH ("_finish") and PARAM_CANCEL ("_cancel"), ignoring parameter
 * values to allow for HTML buttons. The target page for page changes can be
 * specified by PARAM_TARGET, appending the page number to the parameter name
 * (e.g. "_target1"). The action parameters are recognized when triggered by
 * image buttons too (via "_finish.x", "_abort.x", or "_target1.x").
 *
 * <p>The current page number will be stored in the session. It can also be
 * specified as request parameter PARAM_PAGE ("_page") in order to properly handle
 * usage of the back button in a browser: In this case, a submission will always
 * contain the correct page number, even if the user submitted from an old view.
 *
 * <p>The page can only be changed if it validates correctly, except if a
 * "dirty back" or "dirty forward" is allowed. At finish, all pages get
 * validated again to guarantee a consistent state.
 *
 * <p>Note that a validator's default validate method is not executed when using
 * this class! Rather, the <code>validatePage</code> implementation should call
 * special <code>validateXXX</code> methods that the validator needs to provide,
 * validating certain pieces of the object. These can be combined to validate
 * the elements of individual pages.
 *
 * <p>Note: Page numbering starts with 0, to be able to pass an array
 * consisting of the corresponding view names to the "pages" bean property.
 *
 * <p>Parameters indicated with <code>setPassRenderParameters</code> will be present
 * for each page.  If there are render parameters you need in <code>renderFinish</code>
 * or <code>renderCancel</code>, then you need to pass those forward from the
 * <code>processFinish</code> or <code>processCancel</code> methods, respectively.

 * @author Juergen Hoeller
 * @author John A. Lewis
 * @since 2.0
 * @see #setPages
 * @see #validatePage
 * @see #processFinish
 * @see #processCancel
 * @deprecated as of Spring 3.0, in favor of annotated controllers
 */
@Deprecated
public abstract class AbstractWizardFormController extends AbstractFormController {

	/**
	 * Parameter triggering the finish action.
	 * Can be called from any wizard page!
	 */
	public static final String PARAM_FINISH = "_finish";

	/**
	 * Parameter triggering the cancel action.
	 * Can be called from any wizard page!
	 */
	public static final String PARAM_CANCEL = "_cancel";

	/**
	 * Parameter specifying the target page,
	 * appending the page number to the name.
	 */
	public static final String PARAM_TARGET = "_target";

	/**
	 * Parameter specifying the current page as value. Not necessary on
	 * form pages, but allows to properly handle usage of the back button.
	 * @see #setPageAttribute
	 */
	public static final String PARAM_PAGE = "_page";


	private String[] pages;

	private String pageAttribute;

	private boolean allowDirtyBack = true;

	private boolean allowDirtyForward = false;


	/**
	 * Create a new AbstractWizardFormController.
	 * <p>"sessionForm" is automatically turned on, "validateOnBinding"
	 * turned off, and "cacheSeconds" set to 0 by the base class
	 * (-> no caching for all form controllers).
	 */
	public AbstractWizardFormController() {
		// AbstractFormController sets default cache seconds to 0.
		super();

		// Always needs session to keep data from all pages.
		setSessionForm(true);

		// Never validate everything on binding ->
		// wizards validate individual pages.
		setValidateOnBinding(false);
	}

	/**
	 * Set the wizard pages, i.e. the view names for the pages.
	 * The array index is interpreted as page number.
	 * @param pages view names for the pages
	 */
	public final void setPages(String[] pages) {
		if (pages == null || pages.length == 0) {
			throw new IllegalArgumentException("No wizard pages defined");
		}
		this.pages = pages;
	}

	/**
	 * Return the wizard pages, i.e. the view names for the pages.
	 * The array index corresponds to the page number.
	 * <p>Note that a concrete wizard form controller might override
	 * <code>getViewName(PortletRequest, Object, int)</code> to
	 * determine the view name for each page dynamically.
	 * @see #getViewName(PortletRequest, Object, int)
	 */
	public final String[] getPages() {
		return this.pages;
	}

	/**
	 * Return the number of wizard pages.
	 * Useful to check whether the last page has been reached.
	 * <p>Note that a concrete wizard form controller might override
	 * <code>getPageCount(PortletRequest, Object)</code> to determine
	 * the page count dynamically.
	 * @see #getPageCount(PortletRequest, Object)
	 */
	protected final int getPageCount() {
		return this.pages.length;
	}

	/**
	 * Set the name of the page attribute in the model, containing
	 * an Integer with the current page number.
	 * <p>This will be necessary for single views rendering multiple view pages.
	 * It also allows for specifying the optional "_page" parameter.
	 * @param pageAttribute name of the page attribute
	 * @see #PARAM_PAGE
	 */
	public final void setPageAttribute(String pageAttribute) {
		this.pageAttribute = pageAttribute;
	}

	/**
	 * Return the name of the page attribute in the model.
	 */
	public final String getPageAttribute() {
		return this.pageAttribute;
	}

	/**
	 * Set if "dirty back" is allowed, i.e. if moving to a former wizard
	 * page is allowed in case of validation errors for the current page.
	 * @param allowDirtyBack if "dirty back" is allowed
	 */
	public final void setAllowDirtyBack(boolean allowDirtyBack) {
		this.allowDirtyBack = allowDirtyBack;
	}

	/**
	 * Return whether "dirty back" is allowed.
	 */
	public final boolean isAllowDirtyBack() {
		return this.allowDirtyBack;
	}

	/**
	 * Set if "dirty forward" is allowed, i.e. if moving to a later wizard
	 * page is allowed in case of validation errors for the current page.
	 * @param allowDirtyForward if "dirty forward" is allowed
	 */
	public final void setAllowDirtyForward(boolean allowDirtyForward) {
		this.allowDirtyForward = allowDirtyForward;
	}

	/**
	 * Return whether "dirty forward" is allowed.
	 */
	public final boolean isAllowDirtyForward() {
		return this.allowDirtyForward;
	}


	/**
	 * Calls page-specific onBindAndValidate method.
	 */
	@Override
	protected final void onBindAndValidate(PortletRequest request, Object command, BindException errors)
			throws Exception {

		onBindAndValidate(request, command, errors, getCurrentPage(request));
	}

	/**
	 * Callback for custom post-processing in terms of binding and validation.
	 * Called on each submit, after standard binding but before page-specific
	 * validation of this wizard form controller.
	 * <p>Note: AbstractWizardFormController does not perform standard
	 * validation on binding but rather applies page-specific validation
	 * on processing the form submission.
	 * @param request current portlet request
	 * @param command bound command
	 * @param errors Errors instance for additional custom validation
	 * @param page current wizard page
	 * @throws Exception in case of invalid state or arguments
	 * @see #bindAndValidate
	 * @see #processFormSubmission
	 * @see org.springframework.validation.Errors
	 */
	protected void onBindAndValidate(PortletRequest request, Object command, BindException errors, int page)
			throws Exception {
	}

	/**
	 * Consider an explicit finish or cancel request as a form submission too.
	 * @see #isFinishRequest(PortletRequest)
	 * @see #isCancelRequest(PortletRequest)
	 */
	@Override
	protected boolean isFormSubmission(PortletRequest request) {
		return super.isFormSubmission(request) || isFinishRequest(request) || isCancelRequest(request);
	}

	/**
	 * Calls page-specific referenceData method.
	 */
	@Override
	protected final Map referenceData(PortletRequest request, Object command, Errors errors)
			throws Exception {

		return referenceData(request, command, errors, getCurrentPage(request));
	}

	/**
	 * Create a reference data map for the given request, consisting of
	 * bean name/bean instance pairs as expected by ModelAndView.
	 * <p>The default implementation delegates to {@link #referenceData(PortletRequest, int)}.
	 * Subclasses can override this to set reference data used in the view.
	 * @param request current portlet request
	 * @param command form object with request parameters bound onto it
	 * @param errors validation errors holder
	 * @param page current wizard page
	 * @return a Map with reference data entries, or null if none
	 * @throws Exception in case of invalid state or arguments
	 * @see #referenceData(PortletRequest, int)
	 * @see org.springframework.web.portlet.ModelAndView
	 */
	protected Map referenceData(PortletRequest request, Object command, Errors errors, int page)
			throws Exception {

		return referenceData(request, page);
	}

	/**
	 * Create a reference data map for the given request, consisting of
	 * bean name/bean instance pairs as expected by ModelAndView.
	 * <p>The default implementation returns <code>null</code>.
	 * Subclasses can override this to set reference data used in the view.
	 * @param request current portlet request
	 * @param page current wizard page
	 * @return a Map with reference data entries, or null if none
	 * @throws Exception in case of invalid state or arguments
	 * @see org.springframework.web.portlet.ModelAndView
	 */
	protected Map referenceData(PortletRequest request, int page) throws Exception {
		return null;
	}


	/**
	 * Show the first page as form view.
	 * <p>This can be overridden in subclasses, e.g. to prepare wizard-specific
	 * error views in case of an Exception.
	 */
	@Override
	protected ModelAndView showForm(
			RenderRequest request, RenderResponse response, BindException errors) throws Exception {

		return showPage(request, errors, getInitialPage(request, errors.getTarget()));
	}

	/**
	 * Prepare the form model and view, including reference and error data,
	 * for the given page. Can be used in <code>processFinish</code> implementations,
	 * to show the corresponding page in case of validation errors.
	 * @param request current portlet render request
	 * @param errors validation errors holder
	 * @param page number of page to show
	 * @return the prepared form view
	 * @throws Exception in case of invalid state or arguments
	 */
	protected final ModelAndView showPage(RenderRequest request, BindException errors, int page)
			throws Exception {

		if (page >= 0 && page < getPageCount(request, errors.getTarget())) {
			if (logger.isDebugEnabled()) {
				logger.debug("Showing wizard page " + page + " for form bean '" + getCommandName() + "'");
			}
			
			// Set page session attribute, expose overriding request attribute.
			Integer pageInteger = new Integer(page);
			String pageAttrName = getPageSessionAttributeName(request);
			if (isSessionForm()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Setting page session attribute [" + pageAttrName + "] to: " + pageInteger);
				}
				request.getPortletSession().setAttribute(pageAttrName, pageInteger);
			}
			request.setAttribute(pageAttrName, pageInteger);
			
			// Set page request attribute for evaluation by views.
			Map controlModel = new HashMap();
			if (this.pageAttribute != null) {
				controlModel.put(this.pageAttribute, new Integer(page));
			}
			String viewName = getViewName(request, errors.getTarget(), page);
			return showForm(request, errors, viewName, controlModel);
		}

		else {
			throw new PortletException("Invalid wizard page number: " + page);
		}
	}

	/**
	 * Return the page count for this wizard form controller.
	 * <p>The default implementation delegates to {@link #getPageCount()}.
	 * Can be overridden to dynamically adapt the page count.
	 * @param request current portlet request
	 * @param command the command object as returned by formBackingObject
	 * @return the current page count
	 * @see #getPageCount
	 */
	protected int getPageCount(PortletRequest request, Object command) {
		return getPageCount();
	}

	/**
	 * Return the name of the view for the specified page of this wizard form controller.
	 * <p>The default implementation takes the view name from the {@link #getPages()} array.
	 * Can be overridden to dynamically switch the page view or to return view names
	 * for dynamically defined pages.
	 * @param request current portlet request
	 * @param command the command object as returned by <code>formBackingObject</code>
	 * @return the current page count
	 * @see #getPageCount
	 */
	protected String getViewName(PortletRequest request, Object command, int page) {
		return getPages()[page];
	}

	/**
	 * Return the initial page of the wizard, i.e. the page shown at wizard startup.
	 * <p>The default implementation delegates to {@link #getInitialPage(PortletRequest)}.
	 * @param request current portlet request
	 * @param command the command object as returned by <code>formBackingObject</code>
	 * @return the initial page number
	 * @see #getInitialPage(PortletRequest)
	 * @see #formBackingObject
	 */
	protected int getInitialPage(PortletRequest request, Object command) {
		return getInitialPage(request);
	}

	/**
	 * Return the initial page of the wizard, i.e. the page shown at wizard startup.
	 * <p>The default implementation returns 0 for first page.
	 * @param request current portlet request
	 * @return the initial page number
	 */
	protected int getInitialPage(PortletRequest request) {
		return 0;
	}

	/**
	 * Return the name of the PortletSession attribute that holds the page object
	 * for this wizard form controller.
	 * <p>The default implementation delegates to the <code>getPageSessionAttributeName</code>
	 * version without arguments.
	 * @param request current portlet request
	 * @return the name of the form session attribute, or null if not in session form mode
	 * @see #getPageSessionAttributeName
	 * @see #getFormSessionAttributeName
	 * @see javax.portlet.PortletSession#getAttribute
	 */
	protected String getPageSessionAttributeName(PortletRequest request) {
		return getPageSessionAttributeName();
	}

	/**
	 * Return the name of the PortletSession attribute that holds the page object
	 * for this wizard form controller.
	 * <p>Default is an internal name, of no relevance to applications, as the form
	 * session attribute is not usually accessed directly. Can be overridden to use
	 * an application-specific attribute name, which allows other code to access
	 * the session attribute directly.
	 * @return the name of the page session attribute
	 * @see #getFormSessionAttributeName
	 * @see javax.portlet.PortletSession#getAttribute
	 */
	protected String getPageSessionAttributeName() {
		return getClass().getName() + ".PAGE." + getCommandName();
	}

	/**
	 * Pass the page number to the render phase by setting a render parameter.
	 * This method may not be called when the action calls
	 * {@link javax.portlet.ActionResponse#sendRedirect(String)}.
	 * @param response the current action response
	 * @param page the page number
	 * @see ActionResponse#setRenderParameter
	 */
	protected void setPageRenderParameter(ActionResponse response, int page) {
		if (logger.isDebugEnabled())
			logger.debug("Setting page number render parameter [" + PARAM_PAGE + "] to [" + page + "]");
		try {
			response.setRenderParameter(PARAM_PAGE, new Integer(page).toString());
		}
		catch (IllegalStateException ex) {
			// ignore in case sendRedirect was already set
		}
	}

	/**
	 * Pass the the parameter that indicates the target page of the request
	 * forward to the render phase. If the <code>getTargetPage<code> method
	 * was overridden, this may need to be overriden as well.
	 * @param request the current action request
	 * @param response the current action response
	 * @see #PARAM_TARGET
	 * @see #getTargetPage(PortletRequest, int)
	 * @see #getTargetPage(PortletRequest, Object, Errors, int)
	 * @see ActionResponse#setRenderParameter
	 */
	protected void setTargetRenderParameter(ActionRequest request, ActionResponse response) {
		try {
			Map<String, Object> params = PortletUtils.getParametersStartingWith(request, PARAM_TARGET);
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				String param = PARAM_TARGET + entry.getKey();
				Object value = entry.getValue();
				if (logger.isDebugEnabled()) {
					logger.debug("Setting target render parameter [" + param + "]");
				}
				if (value instanceof String) {
					response.setRenderParameter(param, (String) value);
				}
				else if (value instanceof String[]) {
					response.setRenderParameter(param, (String[]) value);
				}
			}
		}
		catch (IllegalStateException ex) {
			// ignore in case sendRedirect was already set
		}
	}

	/**
	 * Pass the the parameter that indicates a finish request forward to the
	 * render phase. If the <code>isFinishRequest</code> method
	 * was overridden, this may need to be overriden as well.
	 * @param request the current action request
	 * @param response the current action response
	 * @see #PARAM_FINISH
	 * @see #isFinishRequest
	 * @see ActionResponse#setRenderParameter
	 */
	protected void setFinishRenderParameter(ActionRequest request, ActionResponse response) {
		if (logger.isDebugEnabled())
			logger.debug("Setting cancel render parameter [" + PARAM_FINISH + "]");
		try {
			String name = PortletUtils.getSubmitParameter(request, PARAM_FINISH);
			if (name != null)
				response.setRenderParameter(name, request.getParameter(name));
		}
		catch (IllegalStateException ex) {
			// ignore in case sendRedirect was already set
		}
	}

	/**
	 * Pass the the parameter that indicates a cancel request forward to the
	 * render phase. If the <code>isCancelRequest</code> method
	 * was overridden, this may need to be overriden as well.
	 * @param request the current action request
	 * @param response the current action response
	 * @see #PARAM_CANCEL
	 * @see #isCancelRequest
	 * @see ActionResponse#setRenderParameter
	 */
	protected void setCancelRenderParameter(ActionRequest request, ActionResponse response) {
		if (logger.isDebugEnabled())
			logger.debug("Setting cancel render parameter [" + PARAM_CANCEL + "]");
		try {
			String name = PortletUtils.getSubmitParameter(request, PARAM_CANCEL);
			if (name != null)
				response.setRenderParameter(name, request.getParameter(name));
		}
		catch (IllegalStateException ex) {
			// ignore in case sendRedirect was already set
		}
	}

	/**
	 * Handle an invalid submit request, e.g. when in session form mode but no form object
	 * was found in the session (like in case of an invalid resubmit by the browser).
	 * <p>The default implementation for wizard form controllers simply shows the initial page
	 * of a new wizard form. If you want to show some "invalid submit" message, you need
	 * to override this method.
	 * @param request current portlet render request
	 * @param response current portlet render response
	 * @return a prepared view, or null if handled directly
	 * @throws Exception in case of errors
	 * @see #showNewForm
	 * @see #setBindOnNewForm
	 * @see #handleInvalidSubmit
	 */
	@Override
	protected ModelAndView renderInvalidSubmit(RenderRequest request, RenderResponse response)
			throws Exception {

		return showNewForm(request, response);
	}

	/**
	 * Handle an invalid submit request, e.g. when in session form mode but no form object
	 * was found in the session (like in case of an invalid resubmit by the browser).
	 * <p>The default implementation for wizard form controllers simply shows the initial page
	 * of a new wizard form, so here in the action phase this method does nothing. If you
	 * want to take some action on an invalid submit, you need to override this method.
	 * @param request current portlet action request
	 * @param response current portlet action response
	 * @throws Exception in case of errors
	 * @see #renderInvalidSubmit
	 */
	@Override
	protected void handleInvalidSubmit(ActionRequest request, ActionResponse response) throws Exception {
	}

	/**
	 * Apply wizard workflow: finish, cancel, page change.
	 * @see #processFormSubmission
	 */
	@Override
	protected final ModelAndView renderFormSubmission(RenderRequest request, RenderResponse response, Object command, BindException errors)
			throws Exception {

		int currentPage = getCurrentPage(request);
		String pageAttrName = getPageSessionAttributeName(request);
		request.setAttribute(pageAttrName, new Integer(currentPage));

		// cancel?
		if (isCancelRequest(request)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Cancelling wizard for form bean '" + getCommandName() + "'");
			}
			return renderCancel(request, response, command, errors);
		}

		// finish?
		if (isFinishRequest(request)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Finishing wizard for form bean '" + getCommandName() + "'");
			}
			return renderValidatePagesAndFinish(request, response, command, errors, currentPage);
		}

		// Normal submit: show specified target page.
		int targetPage = getTargetPage(request, command, errors, currentPage);
		if (logger.isDebugEnabled()) {
			logger.debug("Target page " + targetPage + " requested");
		}
		if (targetPage != currentPage) {
			if (!errors.hasErrors() || (this.allowDirtyBack && targetPage < currentPage) ||
					(this.allowDirtyForward && targetPage > currentPage)) {
				// Allowed to go to target page.
				return showPage(request, errors, targetPage);
			}
		}

		// Show current page again
		return showPage(request, errors, currentPage);
	}


	/**
	 * Apply wizard workflow: finish, cancel, page change.
	 * @see #renderFormSubmission
	 */
	@Override
	protected final void processFormSubmission(
			ActionRequest request, ActionResponse response, Object command, BindException errors)
			throws Exception {

		int currentPage = getCurrentPage(request);
		// Remove page session attribute, provide copy as request attribute.
		String pageAttrName = getPageSessionAttributeName(request);
		if (isSessionForm()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Removing page session attribute [" + pageAttrName + "]");
			}
			request.getPortletSession().removeAttribute(pageAttrName);
		}
		request.setAttribute(pageAttrName, new Integer(currentPage));
		
		// cancel?
		if (isCancelRequest(request)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Cancelling wizard for form bean '" + getCommandName() + "'");
			}
			setPageRenderParameter(response, currentPage);
			setCancelRenderParameter(request, response);
			processCancel(request, response, command, errors);
			return;
		}

		// finish?
		if (isFinishRequest(request)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Finishing wizard for form bean '" + getCommandName() + "'");
			}
			if (!isRedirectAction()) {
				setPageRenderParameter(response, currentPage);
				setFinishRenderParameter(request, response);
			}
			validatePagesAndFinish(request, response, command, errors, currentPage);
			return;
		}

		// Normal submit: validate current page
		if (!suppressValidation(request)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Validating wizard page " + currentPage + " for form bean '" + getCommandName() + "'");
			}
			validatePage(command, errors, currentPage, false);
		}

		setPageRenderParameter(response, currentPage);
		setTargetRenderParameter(request, response);
		passRenderParameters(request, response);

		// Give subclasses a change to perform custom post-procession
		// of the current page and its command object.
		postProcessPage(request, command, errors, currentPage);

	}

	/**
	 * Return the current page number. Used by {@link #processFormSubmission}.
	 * <p>The default implementation checks the page session attribute.
	 * Subclasses can override this for customized page determination.
	 * @param request current portlet request
	 * @return the current page number
	 * @see #getPageSessionAttributeName()
	 */
	protected int getCurrentPage(PortletRequest request) {
		// Check for overriding attribute in request.
		String pageAttrName = getPageSessionAttributeName(request);
		Integer pageAttr = (Integer) request.getAttribute(pageAttrName);
		if (pageAttr != null) {
			return pageAttr.intValue();
		}
		// Check for explicit request parameter.
		String pageParam = request.getParameter(PARAM_PAGE);
		if (pageParam != null) {
			return Integer.parseInt(pageParam);
		}
		// Check for original attribute in session.
		if (isSessionForm()) {
			pageAttr = (Integer) request.getPortletSession().getAttribute(pageAttrName);
			if (pageAttr != null) {
				return pageAttr.intValue();
			}
		}
		throw new IllegalStateException("Page attribute [" + pageAttrName + "] neither found in session nor in request");
	}

	/**
	 * Determine whether the incoming request is a request to finish the
	 * processing of the current form.
	 * <p>By default, this method returns <code>true</code> if a parameter
	 * matching the "_finish" key is present in the request, otherwise it
	 * returns <code>false</code>. Subclasses may override this method
	 * to provide custom logic to detect a finish request.
	 * <p>The parameter is recognized both when sent as a plain parameter
	 * ("_finish") or when triggered by an image button ("_finish.x").
	 * @param request current portlet request
	 * @return whether the request indicates to finish form processing
	 * @see #PARAM_FINISH
	 */
	protected boolean isFinishRequest(PortletRequest request) {
		return PortletUtils.hasSubmitParameter(request, PARAM_FINISH);
	}

	/**
	 * Determine whether the incoming request is a request to cancel the
	 * processing of the current form.
	 * <p>By default, this method returns <code>true</code> if a parameter
	 * matching the "_cancel" key is present in the request, otherwise it
	 * returns <code>false</code>. Subclasses may override this method
	 * to provide custom logic to detect a cancel request.
	 * <p>The parameter is recognized both when sent as a plain parameter
	 * ("_cancel") or when triggered by an image button ("_cancel.x").
	 * @param request current portlet request
	 * @return whether the request indicates to cancel form processing
	 * @see #PARAM_CANCEL
	 */
	protected boolean isCancelRequest(PortletRequest request) {
		return PortletUtils.hasSubmitParameter(request, PARAM_CANCEL);
	}

	/**
	 * Return the target page specified in the request.
	 * <p>The default implementation delegates to {@link #getTargetPage(PortletRequest, int)}.
	 * Subclasses can override this for customized target page determination.
	 * @param request current portlet request
	 * @param command form object with request parameters bound onto it
	 * @param errors validation errors holder
	 * @param currentPage the current page, to be returned as fallback
	 * if no target page specified
	 * @return the page specified in the request, or current page if not found
	 * @see #getTargetPage(PortletRequest, int)
	 */
	protected int getTargetPage(PortletRequest request, Object command, Errors errors, int currentPage) {
		return getTargetPage(request, currentPage);
	}

	/**
	 * Return the target page specified in the request.
	 * <p>The default implementation examines "_target" parameter (e.g. "_target1").
	 * Subclasses can override this for customized target page determination.
	 * @param request current portlet request
	 * @param currentPage the current page, to be returned as fallback
	 * if no target page specified
	 * @return the page specified in the request, or current page if not found
	 * @see #PARAM_TARGET
	 */
	protected int getTargetPage(PortletRequest request, int currentPage) {
		return PortletUtils.getTargetPage(request, PARAM_TARGET, currentPage);
	}

	/**
	 * Validate all pages and process finish.
	 * If there are page validation errors, show the corresponding view page.
	 * @see #validatePagesAndFinish
	 */
	private ModelAndView renderValidatePagesAndFinish(
			RenderRequest request, RenderResponse response, Object command, BindException errors, int currentPage)
			throws Exception {

		// In case of any errors -> show current page.
		if (errors.hasErrors())
			return showPage(request, errors, currentPage);

		// No remaining errors -> proceed with finish.
		return renderFinish(request, response, command, errors);
	}

	/**
	 * Validate all pages and process finish.
	 * If there are page validation errors, show the corresponding view page.
	 * @see #renderValidatePagesAndFinish
	 */
	private void validatePagesAndFinish(
			ActionRequest request, ActionResponse response, Object command, BindException errors, int currentPage)
			throws Exception {

		// In case of binding errors -> show current page.
		if (errors.hasErrors()) {
			setPageRenderParameter(response, currentPage);
			passRenderParameters(request, response);
			return;
		}

		if (!suppressValidation(request)) {
			// In case of remaining errors on a page -> show the page.
			for (int page = 0; page < getPageCount(request, command); page++) {
				validatePage(command, errors, page, true);
				if (errors.hasErrors()) {
					setPageRenderParameter(response, currentPage);
					passRenderParameters(request, response);
					return;
				}
			}
		}

		// No remaining errors -> proceed with finish.
		if (!isRedirectAction())
			setPageRenderParameter(response, currentPage);
		processFinish(request, response, command, errors);

	}

	/**
	 * Template method for custom validation logic for individual pages.
	 * The default implementation calls <code>validatePage(command, errors, page)</code>.
	 * <p>Implementations will typically call fine-granular <code>validateXXX</code>
	 * methods of this instance's Validator, combining them to validation of the
	 * corresponding pages. The Validator's default <code>validate</code> method
	 * will not be called by a wizard form controller!
	 * @param command form object with the current wizard state
	 * @param errors validation errors holder
	 * @param page number of page to validate
	 * @param finish whether this method is called during final revalidation on finish
	 * (else, it is called for validating the current page)
	 * @see #validatePage(Object, Errors, int)
	 * @see org.springframework.validation.Validator#validate
	 */
	protected void validatePage(Object command, Errors errors, int page, boolean finish) {
		validatePage(command, errors, page);
	}

	/**
	 * Template method for custom validation logic for individual pages.
	 * The default implementation is empty.
	 * <p>Implementations will typically call fine-granular validateXXX methods of this
	 * instance's validator, combining them to validation of the corresponding pages.
	 * The validator's default <code>validate</code> method will not be called by a
	 * wizard form controller!
	 * @param command form object with the current wizard state
	 * @param errors validation errors holder
	 * @param page number of page to validate
	 * @see org.springframework.validation.Validator#validate
	 */
	protected void validatePage(Object command, Errors errors, int page) {
	}

	/**
	 * Post-process the given page after binding and validation, potentially
	 * updating its command object. The passed-in request might contain special
	 * parameters sent by the page.
	 * <p>Only invoked when displaying another page or the same page again,
	 * not when finishing or cancelling.
	 * @param request current action request
	 * @param command form object with request parameters bound onto it
	 * @param errors validation errors holder
	 * @param page number of page to post-process
	 * @throws Exception in case of invalid state or arguments
	 */
	protected void postProcessPage(ActionRequest request, Object command, Errors errors, int page)
			throws Exception {
	}

	/**
	 * Template method for the render phase of the finish action of this wizard.
	 * <p>The default implementation throws a PortletException, saying that a finish
	 * render request is not supported by this controller. Thus, you do not need to
	 * implement this template method if you do not need to render after a finish.
	 * <p>Call <code>errors.getModel()</code> to populate the ModelAndView model
	 * with the command and the Errors instance, under the specified command name,
	 * as expected by the "spring:bind" tag.
	 * @param request current portlet render request
	 * @param response current portlet render response
	 * @param command form object with the current wizard state
	 * @param errors validation errors holder
	 * @return the finish view
	 * @throws Exception in case of invalid state or arguments
	 * @see #processFinish
	 * @see org.springframework.validation.Errors
	 * @see org.springframework.validation.BindException#getModel
	 */
	protected ModelAndView renderFinish(
			RenderRequest request, RenderResponse response, Object command, BindException errors)
			throws Exception {

		throw new PortletException("Wizard form controller class [" + getClass().getName() + "] does not support a finish render request");
	}

	/**
	 * Template method for the action phase of the finish action of this wizard.
	 * <p>The default implementation throws a PortletException, saying that a finish
	 * action request is not supported by this controller. You will almost certainly
	 * need to override this method.
	 * @param request current portlet action request
	 * @param response current portlet action response
	 * @param command form object with the current wizard state
	 * @param errors validation errors holder
	 * @throws Exception in case of invalid state or arguments
	 * @see #renderFinish
	 * @see org.springframework.validation.Errors
	 */
	protected void processFinish(
			ActionRequest request, ActionResponse response, Object command, BindException errors)
			throws Exception {

		throw new PortletException(
				"Wizard form controller class [" + getClass().getName() + "] does not support a finish action request");
	}

	/**
	 * Template method for the render phase of the cancel action of this wizard.
	 * <p>The default implementation throws a PortletException, saying that a cancel
	 * render request is not supported by this controller. Thus, you do not need to
	 * implement this template method if you do not support a cancel operation.
	 * <p>Call <code>errors.getModel()</code> to populate the ModelAndView model
	 * with the command and the Errors instance, under the specified command name,
	 * as expected by the "spring:bind" tag.
	 * @param request current portlet render request
	 * @param response current portlet render response
	 * @param command form object with the current wizard state
	 * @param errors Errors instance containing errors
	 * @return the cancellation view
	 * @throws Exception in case of invalid state or arguments
	 * @see #processCancel
	 * @see org.springframework.validation.Errors
	 * @see org.springframework.validation.BindException#getModel
	 */
	protected ModelAndView renderCancel(
			RenderRequest request, RenderResponse response, Object command, BindException errors)
			throws Exception {

		throw new PortletException(
				"Wizard form controller class [" + getClass().getName() + "] does not support a cancel render request");
	}

	/**
	 * Template method for the action phase of the cancel action of this wizard.
	 * <p>The default implementation throws a PortletException, saying that a cancel
	 * action request is not supported by this controller. Thus, you do not need to
	 * implement this template method if you do not support a cancel operation.
	 * @param request current portlet action request
	 * @param response current portlet action response
	 * @param command form object with the current wizard state
	 * @param errors Errors instance containing errors
	 * @throws Exception in case of invalid state or arguments
	 * @see #renderCancel
	 * @see org.springframework.validation.Errors
	 */
	protected void processCancel(
			ActionRequest request, ActionResponse response, Object command, BindException errors)
			throws Exception {

		throw new PortletException(
				"Wizard form controller class [" + getClass().getName() + "] does not support a cancel action request");
	}

}
