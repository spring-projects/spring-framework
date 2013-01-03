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

package org.springframework.web.portlet.mvc;

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

/**
 * <p>Concrete FormController implementation that provides configurable
 * form and success views, and an onSubmit chain for convenient overriding.
 * Automatically resubmits to the form view in case of validation errors,
 * and renders the success view in case of a valid submission.</p>
 *
 * <p>The workflow of this Controller does not differ much from the one described
 * in the {@link AbstractFormController AbstractFormController}. The difference
 * is that you do not need to implement {@link #showForm showForm},
 * {@link #processFormSubmission processFormSubmission}, and
 * {@link #renderFormSubmission renderFormSubmission}: A form view and a
 * success view can be configured declaratively.</p>
 *
 * <p>This controller is different from it's servlet counterpart in that it must take
 * into account the two phases of a portlet request: the action phase and the render
 * phase.  See the JSR-168 spec for more details on these two phases.
 * Be especially aware that the action phase is called only once, but that the
 * render phase will be called repeatedly by the portal -- it does this every time
 * the page containing the portlet is updated, even if the activity is in some other
 * portlet.  The main difference in the methods in this class is that the
 * {@code onSubmit} methods have all been split into {@code onSubmitAction}
 * and {@code onSubmitRender} to account for the two phases.</p>
 *
 * <p><b><a name="workflow">Workflow
 * (<a href="AbstractFormController.html#workflow">in addition to the superclass</a>):</b><br>
 * <ol>
 * <li>Call to {@link #processFormSubmission processFormSubmission} which inspects
 * the {@link org.springframework.validation.Errors Errors} object to see if
 * any errors have occurred during binding and validation.</li>
 * <li>If errors occured, the controller will return the configured formView,
 * showing the form again (possibly rendering according error messages).</li>
 * <li>If {@link #isFormChangeRequest isFormChangeRequest} is overridden and returns
 * true for the given request, the controller will return the formView too.
 * In that case, the controller will also suppress validation. Before returning the formView,
 * the controller will invoke {@link #onFormChange}, giving sub-classes a chance
 * to make modification to the command object.
 * This is intended for requests that change the structure of the form,
 * which should not cause validation and show the form in any case.</li>
 * <li>If no errors occurred, the controller will call
 * {@link #onSubmitAction(ActionRequest, ActionResponse, Object, BindException) onSubmitAction}
 * during the action phase and then {@link #onSubmitRender(RenderRequest, RenderResponse,
 * Object, BindException) onSubmitRender} during the render phase, which in case of the
 * default implementation delegate to {@link #onSubmitAction(Object, BindException)
 * onSubmitAction} and {@link #onSubmitRender(Object, BindException) onSubmitRender}
 * with just the command object.
 * The default implementation of the latter method will return the configured
 * {@code successView}. Consider just implementing {@link #doSubmitAction doSubmitAction}
 * for simply performing a submit action during the action phase and then rendering
 * the success view during the render phase.</li>
 * </ol>
 * </p>
 *
 * <p>The submit behavior can be customized by overriding one of the
 * {@link #onSubmitAction onSubmitAction} or {@link #onSubmitRender onSubmitRender}
 * methods. Submit actions can also perform custom validation if necessary
 * (typically database-driven checks), calling {@link #showForm(RenderRequest,
 * RenderResponse, BindException) showForm} in case of validation errors to show
 * the form view again.  You do not have to override both the {@code onSubmitAction} and
 * {@code onSubmitRender} methods at a given level unless you truly have custom logic to
 * perform in both.<p>
 *
 * <p><b>WARNING:</b> Make sure that any one-time system updates (such as database
 * updates or file writes) are performed in either an {@link #onSubmitAction onSubmitAction}
 * method or the {@link #doSubmitAction doSubmitAction} method.  Logic in the
 * {@link #onSubmitRender onSubmitRender} methods may be executed repeatedly by
 * the portal whenever the page containing the portlet is updated.</p>
 *
 * <p><b><a name="config">Exposed configuration properties</a>
 * (<a href="AbstractFormController.html#config">and those defined by superclass</a>):</b><br>
 * <table border="1">
 * <tr>
 * <td><b>name</b></td>
 * <td><b>default</b></td>
 * <td><b>description</b></td>
 * </tr>
 * <tr>
 * <td>formView</td>
 * <td><i>null</i></td>
 * <td>Indicates what view to use when the user asks for a new form
 * or when validation errors have occurred on form submission.</td>
 * </tr>
 * <tr>
 * <td>successView</td>
 * <td><i>null</i></td>
 * <td>Indicates what view to use when successful form submissions have
 * occurred. Such a success view could e.g. display a submission summary.
 * More sophisticated actions can be implemented by overriding one of
 * the {@link #onSubmitRender(Object) onSubmitRender()} methods.</td>
 * </tr>
 * <table>
 * </p>
 *
 * <p>Parameters indicated with {@code setPassRenderParameters} will be
 * preserved if the form has errors or if a form change request occurs.
 * If there are render parameters you need in {@code onSubmitRender},
 * then you need to pass those forward from {@code onSubmitAction}.
 *
 * <p>Thanks to Rainer Schmitz and Nick Lothian for their suggestions!
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 * @deprecated as of Spring 3.0, in favor of annotated controllers
 */
@Deprecated
public class SimpleFormController extends AbstractFormController {

	private String formView;

	private String successView;


	/**
	 * Create a new SimpleFormController.
	 * <p>Subclasses should set the following properties, either in the constructor
	 * or via a BeanFactory: commandName, commandClass, sessionForm, formView,
	 * successView. Note that commandClass doesn't need to be set when overriding
	 * {@code formBackingObject}, as this determines the class anyway.
	 * @see #setCommandClass(Class)
	 * @see #setCommandName(String)
	 * @see #setSessionForm(boolean)
	 * @see #setFormView
	 * @see #setSuccessView
	 * @see #formBackingObject(PortletRequest)
	 */
	public SimpleFormController() {
		// AbstractFormController sets default cache seconds to 0.
		super();
	}

	/**
	 * Set the name of the view that should be used for form display.
	 */
	public final void setFormView(String formView) {
		this.formView = formView;
	}

	/**
	 * Return the name of the view that should be used for form display.
	 */
	public final String getFormView() {
		return this.formView;
	}

	/**
	 * Set the name of the view that should be shown on successful submit.
	 */
	public final void setSuccessView(String successView) {
		this.successView = successView;
	}

	/**
	 * Return the name of the view that should be shown on successful submit.
	 */
	public final String getSuccessView() {
		return this.successView;
	}


	/**
	 * This implementation shows the configured form view, delegating to the
	 * analogous showForm version with a controlModel argument.
	 * <p>Can be called within onSubmit implementations, to redirect back to the form
	 * in case of custom validation errors (i.e. not determined by the validator).
	 * <p>Can be overridden in subclasses to show a custom view, writing directly
	 * to the response or preparing the response before rendering a view.
	 * <p>If calling showForm with a custom control model in subclasses, it's preferable
	 * to override the analogous showForm version with a controlModel argument
	 * (which will handle both standard form showing and custom form showing then).
	 * @see #setFormView
	 * @see #showForm(RenderRequest, RenderResponse, BindException, Map)
	 */
	@Override
	protected ModelAndView showForm(RenderRequest request, RenderResponse response, BindException errors)
			throws Exception {

		return showForm(request, response, errors, null);
	}

	/**
	 * This implementation shows the configured form view.
	 * <p>Can be called within onSubmit implementations, to redirect back to the form
	 * in case of custom validation errors (i.e. not determined by the validator).
	 * <p>Can be overridden in subclasses to show a custom view, writing directly
	 * to the response or preparing the response before rendering a view.
	 * @param request current render request
	 * @param errors validation errors holder
	 * @param controlModel model map containing controller-specific control data
	 * (e.g. current page in wizard-style controllers or special error message)
	 * @return the prepared form view
	 * @throws Exception in case of invalid state or arguments
	 * @see #setFormView
	 */
	protected ModelAndView showForm(RenderRequest request, RenderResponse response, BindException errors, Map controlModel)
			throws Exception {

		return showForm(request, errors, getFormView(), controlModel);
	}

	/**
	 * Create a reference data map for the given request and command,
	 * consisting of bean name/bean instance pairs as expected by ModelAndView.
	 * <p>The default implementation delegates to {@link #referenceData(PortletRequest)}.
	 * Subclasses can override this to set reference data used in the view.
	 * @param request current portlet request
	 * @param command form object with request parameters bound onto it
	 * @param errors validation errors holder
	 * @return a Map with reference data entries, or null if none
	 * @throws Exception in case of invalid state or arguments
	 * @see ModelAndView
	 */
	@Override
	protected Map referenceData(PortletRequest request, Object command, Errors errors) throws Exception {
		return referenceData(request);
	}

	/**
	 * Create a reference data map for the given request.
	 * Called by referenceData version with all parameters.
	 * <p>The default implementation returns {@code null}.
	 * Subclasses can override this to set reference data used in the view.
	 * @param request current portlet request
	 * @return a Map with reference data entries, or null if none
	 * @throws Exception in case of invalid state or arguments
	 * @see #referenceData(PortletRequest, Object, Errors)
	 * @see ModelAndView
	 */
	protected Map referenceData(PortletRequest request) throws Exception {
		return null;
	}


	/**
	 * This implementation calls {@code showForm} in case of errors,
	 * and delegates to {@code onSubmitRender}'s full version else.
	 * <p>This can only be overridden to check for an action that should be executed
	 * without respect to binding errors, like a cancel action. To just handle successful
	 * submissions without binding errors, override one of the {@code onSubmitRender}
	 * methods.
	 * @see #showForm(RenderRequest, RenderResponse, BindException)
	 * @see #onSubmitRender(RenderRequest, RenderResponse, Object, BindException)
	 * @see #onSubmitRender(Object, BindException)
	 * @see #onSubmitRender(Object)
	 * @see #processFormSubmission(ActionRequest, ActionResponse, Object, BindException)
	 */
	@Override
	protected ModelAndView renderFormSubmission(RenderRequest request, RenderResponse response, Object command, BindException errors)
			throws Exception {

		if (errors.hasErrors() || isFormChangeRequest(request)) {
			return showForm(request, response, errors);
		}
		else {
			return onSubmitRender(request, response, command, errors);
		}
	}

	/**
	 * This implementation does nothing in case of errors,
	 * and delegates to {@code onSubmitAction}'s full version else.
	 * <p>This can only be overridden to check for an action that should be executed
	 * without respect to binding errors, like a cancel action. To just handle successful
	 * submissions without binding errors, override one of the {@code onSubmitAction}
	 * methods or {@code doSubmitAction}.
	 * @see #showForm
	 * @see #onSubmitAction(ActionRequest, ActionResponse, Object, BindException)
	 * @see #onSubmitAction(Object, BindException)
	 * @see #onSubmitAction(Object)
	 * @see #doSubmitAction(Object)
	 * @see #renderFormSubmission(RenderRequest, RenderResponse, Object, BindException)
	 */
	@Override
	protected void processFormSubmission(
			ActionRequest request, ActionResponse response, Object command, BindException errors)
			throws Exception {

		if (errors.hasErrors()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Data binding errors: " + errors.getErrorCount());
			}
			if (isRedirectAction()) {
				setFormSubmit(response);
			}
			passRenderParameters(request, response);
		}
		else if (isFormChangeRequest(request)) {
			logger.debug("Detected form change request -> routing request to onFormChange");
			if (isRedirectAction()) {
				setFormSubmit(response);
			}
			passRenderParameters(request, response);
			onFormChange(request, response, command, errors);
		}
		else {
			logger.debug("No errors - processing submit");
			onSubmitAction(request, response, command, errors);
		}
	}

	/**
	 * This implementation delegates to {@link #isFormChangeRequest}:
	 * A form change request changes the appearance of the form
	 * and should not get validated but just show the new form.
	 * @see #isFormChangeRequest
	 */
	@Override
	protected boolean suppressValidation(PortletRequest request) {
		return isFormChangeRequest(request);
	}

	/**
	 * Determine whether the given request is a form change request.
	 * A form change request changes the appearance of the form
	 * and should always show the new form, without validation.
	 * <p>Gets called by {@link #suppressValidation} and {@link #processFormSubmission}.
	 * Consequently, this single method determines to suppress validation
	 * <i>and</i> to show the form view in any case.
	 * <p>The default implementation returns {@code false}.
	 * @param request current portlet request
	 * @return whether the given request is a form change request
	 * @see #suppressValidation
	 * @see #processFormSubmission
	 */
	protected boolean isFormChangeRequest(PortletRequest request) {
		return false;
	}

	/**
	 * Called during form submission if {@link #isFormChangeRequest(PortletRequest)}
	 * returns {@code true}. Allows subclasses to implement custom logic
	 * to modify the command object to directly modify data in the form.
	 * <p>The default implementation delegates to
	 * {@code onFormChange(request, response, command)}.
	 * @param request current action request
	 * @param response current action response
	 * @param command form object with request parameters bound onto it
	 * @param errors validation errors holder, allowing for additional
	 * custom validation
	 * @throws Exception in case of errors
	 * @see #isFormChangeRequest(PortletRequest)
	 * @see #onFormChange(ActionRequest, ActionResponse, Object)
	 */
	protected void onFormChange(ActionRequest request, ActionResponse response, Object command, BindException errors)
			throws Exception {

		onFormChange(request, response, command);
	}

	/**
	 * Simpler {@code onFormChange} variant, called by the full version
	 * {@code onFormChange(request, response, command, errors)}.
	 * <p>The default implementation is empty.
	 * @param request current action request
	 * @param response current action response
	 * @param command form object with request parameters bound onto it
	 * @throws Exception in case of errors
	 * @see #onFormChange(ActionRequest, ActionResponse, Object, BindException)
	 */
	protected void onFormChange(ActionRequest request, ActionResponse response, Object command)
			throws Exception {
	}


	/**
	 * Submit render phase callback with all parameters. Called in case of submit without errors
	 * reported by the registered validator, or on every submit if no validator.
	 * <p>The default implementation delegates to {@link #onSubmitRender(Object, BindException)}.
	 * For simply performing a submit action and rendering the specified success view,
	 * do not implement an {@code onSubmitRender} at all.
	 * <p>Subclasses can override this to provide custom rendering to display results of
	 * the action phase. Implementations can also call {@code showForm} to return to the form
	 * if the {@code onSubmitAction} failed custom validation. Do <i>not</i> implement multiple
	 * {@code onSubmitRender} methods: In that case,
	 * just this method will be called by the controller.
	 * <p>Call {@code errors.getModel()} to populate the ModelAndView model
	 * with the command and the Errors instance, under the specified command name,
	 * as expected by the "spring:bind" tag.
	 * @param request current render request
	 * @param response current render response
	 * @param command form object with request parameters bound onto it
	 * @param errors Errors instance without errors (subclass can add errors if it wants to)
	 * @return the prepared model and view
	 * @throws Exception in case of errors
	 * @see #onSubmitAction(ActionRequest, ActionResponse, Object, BindException)
	 * @see #onSubmitRender(Object, BindException)
	 * @see #doSubmitAction
	 * @see #showForm
	 * @see org.springframework.validation.Errors
	 * @see org.springframework.validation.BindException#getModel
	 */
	protected ModelAndView onSubmitRender(RenderRequest request, RenderResponse response, Object command, BindException errors)
			throws Exception {

		return onSubmitRender(command, errors);
	}

	/**
	 * Submit action phase callback with all parameters. Called in case of submit without errors
	 * reported by the registered validator respectively on every submit if no validator.
	 * <p>The default implementation delegates to {@link #onSubmitAction(Object, BindException)}.
	 * For simply performing a submit action consider implementing {@code doSubmitAction}
	 * rather than an {@code onSubmitAction} version.
	 * <p>Subclasses can override this to provide custom submission handling like storing
	 * the object to the database. Implementations can also perform custom validation and
	 * signal the render phase to call {@code showForm} to return to the form. Do <i>not</i>
	 * implement multiple {@code onSubmitAction} methods: In that case,
	 * just this method will be called by the controller.
	 * @param request current action request
	 * @param response current action response
	 * @param command form object with request parameters bound onto it
	 * @param errors Errors instance without errors (subclass can add errors if it wants to)
	 * @throws Exception in case of errors
	 * @see #onSubmitRender(RenderRequest, RenderResponse, Object, BindException)
	 * @see #onSubmitAction(Object, BindException)
	 * @see #doSubmitAction
	 * @see org.springframework.validation.Errors
	 */
	protected void onSubmitAction(ActionRequest request, ActionResponse response, Object command, BindException errors)
			throws Exception {

		onSubmitAction(command, errors);
	}

	/**
	 * Simpler {@code onSubmitRender} version. Called by the default implementation
	 * of the {@code onSubmitRender} version with all parameters.
	 * <p>The default implementation calls {@link #onSubmitRender(Object)}, using the
	 * returned ModelAndView if actually implemented in a subclass. Else, the
	 * default behavior will apply: rendering the success view with the command
	 * and Errors instance as model.
	 * <p>Subclasses can override this to provide custom submission handling that
	 * does not need request and response.
	 * <p>Call {@code errors.getModel()} to populate the ModelAndView model
	 * with the command and the Errors instance, under the specified command name,
	 * as expected by the "spring:bind" tag.
	 * @param command form object with request parameters bound onto it
	 * @param errors Errors instance without errors
	 * @return the prepared model and view, or null
	 * @throws Exception in case of errors
	 * @see #onSubmitRender(RenderRequest, RenderResponse, Object, BindException)
	 * @see #onSubmitRender(Object)
	 * @see #onSubmitAction(Object, BindException)
	 * @see #setSuccessView
	 * @see org.springframework.validation.Errors
	 * @see org.springframework.validation.BindException#getModel
	 */
	protected ModelAndView onSubmitRender(Object command, BindException errors) throws Exception {
		ModelAndView mv = onSubmitRender(command);
		if (mv != null) {
			// simplest onSubmit version implemented in custom subclass
			return mv;
		}
		else {
			// default behavior: render success view
			if (getSuccessView() == null) {
				throw new PortletException("successView isn't set");
			}
			return new ModelAndView(getSuccessView(), errors.getModel());
		}
	}

	/**
	 * Simpler {@code onSubmitAction} version. Called by the default implementation
	 * of the {@code onSubmitAction} version with all parameters.
	 * <p>The default implementation calls {@link #onSubmitAction(Object)}.
	 * <p>Subclasses can override this to provide custom submission handling that
	 * does not need request and response.
	 * @param command form object with request parameters bound onto it
	 * @param errors Errors instance without errors
	 * @throws Exception in case of errors
	 * @see #onSubmitAction(ActionRequest, ActionResponse, Object, BindException)
	 * @see #onSubmitAction(Object)
	 * @see #onSubmitRender(Object, BindException)
	 * @see org.springframework.validation.Errors
	 */
	protected void onSubmitAction(Object command, BindException errors) throws Exception {
		onSubmitAction(command);
	}

	/**
	 * Simplest {@code onSubmitRender} version. Called by the default implementation
	 * of the {@code onSubmitRender} version with command and BindException parameters.
	 * <p>This implementation returns null as ModelAndView, making the calling
	 * {@code onSubmitRender} method perform its default rendering of the success view.
	 * <p>Subclasses can override this to provide custom submission handling
	 * that just depends on the command object.
	 * @param command form object with request parameters bound onto it
	 * @return the prepared model and view, or null for default (i.e. successView)
	 * @throws Exception in case of errors
	 * @see #onSubmitRender(Object, BindException)
	 * @see #onSubmitAction(Object)
	 * @see #doSubmitAction
	 * @see #setSuccessView
	 */
	protected ModelAndView onSubmitRender(Object command) throws Exception {
		return null;
	}

	/**
	 * Simplest {@code onSubmitAction} version. Called by the default implementation
	 * of the {@code onSubmitAction} version with command and BindException parameters.
	 * <p>This implementation calls {@code doSubmitAction}.
	 * <p>Subclasses can override this to provide custom submission handling
	 * that just depends on the command object.
	 * @param command form object with request parameters bound onto it
	 * @throws Exception in case of errors
	 * @see #onSubmitAction(Object, BindException)
	 * @see #onSubmitRender(Object)
	 * @see #doSubmitAction
	 */
	protected void onSubmitAction(Object command) throws Exception {
		doSubmitAction(command);
	}

	/**
	 * Template method for submit actions. Called by the default implementation
	 * of the simplest {@code onSubmitAction} version.
	 * <p><b>This is the preferred submit callback to implement if you want to
	 * perform an action (like storing changes to the database) and then render
	 * the success view with the command and Errors instance as model.</b>
	 * @param command form object with request parameters bound onto it
	 * @throws Exception in case of errors
	 * @see #onSubmitAction(Object)
	 * @see #onSubmitRender(Object)
	 * @see #setSuccessView
	 */
	protected void doSubmitAction(Object command) throws Exception {
	}

}
