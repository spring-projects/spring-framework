/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;

/**
 * <p>Form controller that auto-populates a form bean from the request.
 * This, either using a new bean instance per request, or using the same bean
 * when the {@code sessionForm} property has been set to {@code true}.</p>
 *
 * <p>This class is the base class for both framework subclasses such as
 * {@link SimpleFormController} and {@link AbstractWizardFormController}
 * and custom form controllers that you may provide yourself.</p>
 *
 * <p>A form-input view and an after-submission view have to be provided
 * programmatically. To provide those views using configuration properties,
 * use the {@link SimpleFormController}.</p>
 *
 * <p>Subclasses need to override {@code showForm} to prepare the form view,
 * and {@code processFormSubmission} to handle submit requests. For the latter,
 * binding errors like type mismatches will be reported via the given "errors" holder.
 * For additional custom form validation, a validator (property inherited from
 * BaseCommandController) can be used, reporting via the same "errors" instance.</p>
 *
 * <p>Comparing this Controller to the Struts notion of the {@code Action}
 * shows us that with Spring, you can use any ordinary JavaBeans or database-
 * backed JavaBeans without having to implement a framework-specific class
 * (like Struts' {@code ActionForm}). More complex properties of JavaBeans
 * (Dates, Locales, but also your own application-specific or compound types)
 * can be represented and submitted to the controller, by using the notion of
 * a {@code java.beans.PropertyEditor}. For more information on that
 * subject, see the workflow of this controller and the explanation of the
 * {@link BaseCommandController}.</p>
 *
 * <p><b><a name="workflow">Workflow
 * (<a href="BaseCommandController.html#workflow">and that defined by superclass</a>):</b><br>
 * <ol>
 *  <li><b>The controller receives a request for a new form (typically a GET).</b></li>
 *  <li>Call to {@link #formBackingObject formBackingObject()} which by default,
 *      returns an instance of the commandClass that has been configured
 *      (see the properties the superclass exposes), but can also be overridden
 *      to e.g. retrieve an object from the database (that needs to be modified
 * using the form).</li>
 *  <li>Call to {@link #initBinder initBinder()} which allows you to register
 *      custom editors for certain fields (often properties of non-primitive
 *      or non-String types) of the command class. This will render appropriate
 *      Strings for those property values, e.g. locale-specific date strings.</li>
 *  <li><em>Only if {@code bindOnNewForm} is set to {@code true}</em>, then
 *      {@link org.springframework.web.bind.ServletRequestDataBinder ServletRequestDataBinder}
 *      gets applied to populate the new form object with initial request parameters and the
 *      {@link #onBindOnNewForm(HttpServletRequest, Object, BindException)} callback method is
 *      called. <em>Note:</em> any defined Validators are not applied at this point, to allow
 *      partial binding. However be aware that any Binder customizations applied via
 *      initBinder() (such as
 *      {@link org.springframework.validation.DataBinder#setRequiredFields(String[])} will
 *      still apply. As such, if using bindOnNewForm=true and initBinder() customizations are
 *      used to validate fields instead of using Validators, in the case that only some fields
 *      will be populated for the new form, there will potentially be some bind errors for
 *      missing fields in the errors object. Any view (JSP, etc.) that displays binder errors
 *      needs to be intelligent and for this case take into account whether it is displaying the
 *      initial form view or subsequent post results, skipping error display for the former.</li>
 *  <li>Call to {@link #showForm(HttpServletRequest, HttpServletResponse, BindException) showForm()}
 *      to return a View that should be rendered (typically the view that renders
 *      the form). This method has to be implemented in subclasses.</li>
 *  <li>The showForm() implementation will call {@link #referenceData referenceData()},
 *      which you can implement to provide any relevant reference data you might need
 *      when editing a form (e.g. a List of Locale objects you're going to let the
 *      user select one from).</li>
 *  <li>Model gets exposed and view gets rendered, to let the user fill in the form.</li>
 *  <li><b>The controller receives a form submission (typically a POST).</b>
 *      To use a different way of detecting a form submission, override the
 *      {@link #isFormSubmission isFormSubmission} method.
 *      </li>
 *  <li>If {@code sessionForm} is not set, {@link #formBackingObject formBackingObject()}
 *      is called to retrieve a form object. Otherwise, the controller tries to
 *      find the command object which is already bound in the session. If it cannot
 *      find the object, it does a call to {@link #handleInvalidSubmit handleInvalidSubmit}
 *      which - by default - tries to create a new form object and resubmit the form.</li>
 *  <li>The {@link org.springframework.web.bind.ServletRequestDataBinder ServletRequestDataBinder}
 *      gets applied to populate the form object with current request parameters.
 *  <li>Call to {@link #onBind onBind(HttpServletRequest, Object, Errors)} which allows
 *      you to do custom processing after binding but before validation (e.g. to manually
 *      bind request parameters to bean properties, to be seen by the Validator).</li>
 *  <li>If {@code validateOnBinding} is set, a registered Validator will be invoked.
 *      The Validator will check the form object properties, and register corresponding
 *      errors via the given {@link org.springframework.validation.Errors Errors}</li> object.
 *  <li>Call to {@link #onBindAndValidate onBindAndValidate()} which allows you
 *      to do custom processing after binding and validation (e.g. to manually
 *      bind request parameters, and to validate them outside a Validator).</li>
 *  <li>Call {@link #processFormSubmission(HttpServletRequest, HttpServletResponse,
 *      Object, BindException) processFormSubmission()} to process the submission, with
 *      or without binding errors. This method has to be implemented in subclasses.</li>
 * </ol>
 * </p>
 *
 * <p>In session form mode, a submission without an existing form object in the
 * session is considered invalid, like in case of a resubmit/reload by the browser.
 * The {@link #handleInvalidSubmit handleInvalidSubmit} method is invoked then,
 * by default trying to resubmit. It can be overridden in subclasses to show
 * corresponding messages or to redirect to a new form, in order to avoid duplicate
 * submissions. The form object in the session can be considered a transaction
 * token in that case.</p>
 *
 * <p>Note that views should never retrieve form beans from the session but always
 * from the request, as prepared by the form controller. Remember that some view
 * technologies like Velocity cannot even access a HTTP session.</p>
 *
 * <p><b><a name="config">Exposed configuration properties</a>
 * (<a href="BaseCommandController.html#config">and those defined by superclass</a>):</b><br>
 * <table border="1">
 *  <tr>
 *      <td><b>name</b></td>
 *      <td><b>default</b></td>
 *      <td><b>description</b></td>
 *  </tr>
 *  <tr>
 *      <td>bindOnNewForm</td>
 *      <td>false</td>
 *      <td>Indicates whether to bind servlet request parameters when
 *          creating a new form. Otherwise, the parameters will only be
 *          bound on form submission attempts.</td>
 *  </tr>
 *  <tr>
 *      <td>sessionForm</td>
 *      <td>false</td>
 *      <td>Indicates whether the form object should be kept in the session
 *          when a user asks for a new form. This allows you e.g. to retrieve
 *          an object from the database, let the user edit it, and then persist
 *          it again. Otherwise, a new command object will be created for each
 *          request (even when showing the form again after validation errors).</td>
 *  </tr>
 * </table>
 * </p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @author Rob Harrop
 * @author Colin Sampaleanu
 * @see #showForm(HttpServletRequest, HttpServletResponse, BindException)
 * @see #processFormSubmission
 * @see SimpleFormController
 * @see AbstractWizardFormController
 * @deprecated as of Spring 3.0, in favor of annotated controllers
 */
@Deprecated
public abstract class AbstractFormController extends BaseCommandController {

	private boolean bindOnNewForm = false;

	private boolean sessionForm = false;


	/**
	 * Create a new AbstractFormController.
	 * <p>Subclasses should set the following properties, either in the constructor
	 * or via a BeanFactory: commandName, commandClass, bindOnNewForm, sessionForm.
	 * Note that "commandClass" doesn't need to be set when overriding
	 * {@link #formBackingObject}, since the latter determines the class anyway.
	 * <p>"cacheSeconds" is by default set to 0 (-> no caching for all form controllers).
	 * @see #setCommandName
	 * @see #setCommandClass
	 * @see #setBindOnNewForm
	 * @see #setSessionForm
	 * @see #formBackingObject
	 */
	public AbstractFormController() {
		setCacheSeconds(0);
	}

	/**
	 * Set whether request parameters should be bound to the form object
	 * in case of a non-submitting request, that is, a new form.
	 */
	public final void setBindOnNewForm(boolean bindOnNewForm) {
		this.bindOnNewForm = bindOnNewForm;
	}

	/**
	 * Return {@code true} if request parameters should be bound in case of a new form.
	 */
	public final boolean isBindOnNewForm() {
		return this.bindOnNewForm;
	}

	/**
	 * Activate/deactivate session form mode. In session form mode,
	 * the form is stored in the session to keep the form object instance
	 * between requests, instead of creating a new one on each request.
	 * <p>This is necessary for either wizard-style controllers that populate a
	 * single form object from multiple pages, or forms that populate a persistent
	 * object that needs to be identical to allow for tracking changes.
	 * <p>Please note that the {@link AbstractFormController} class (and all
	 * subclasses of it unless stated to the contrary) do <i>not</i> support
	 * the notion of a conversation. This is important in the context of this
	 * property, because it means that there is only <i>one</i> form per session:
	 * this means that if session form mode is activated and a user opens up
	 * say two tabs in their browser and attempts to edit two distinct objects
	 * using the same form, then the <i>shared</i> session state can potentially
	 * (and most probably will) be overwritten by the last tab to be opened,
	 * which can lead to errors when either of the forms in each is finally
	 * submitted.
	 * <p>If you need to have per-form, per-session state management (that is,
	 * stateful web conversations), the recommendation is to use
	 * <a href="http://www.springframework.org/webflow">Spring WebFlow</a>,
	 * which has full support for conversations and has a much more flexible
	 * usage model overall.
	 * @param sessionForm {@code true} if session form mode is to be activated
	 */
	public final void setSessionForm(boolean sessionForm) {
		this.sessionForm = sessionForm;
	}

	/**
	 * Return {@code true} if session form mode is activated.
	 */
	public final boolean isSessionForm() {
		return this.sessionForm;
	}


	/**
	 * Handles two cases: form submissions and showing a new form.
	 * Delegates the decision between the two to {@link #isFormSubmission},
	 * always treating requests without existing form session attribute
	 * as new form when using session form mode.
	 * @see #isFormSubmission
	 * @see #showNewForm
	 * @see #processFormSubmission
	 */
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		// Form submission or new form to show?
		if (isFormSubmission(request)) {
			// Fetch form object from HTTP session, bind, validate, process submission.
			try {
				Object command = getCommand(request);
				ServletRequestDataBinder binder = bindAndValidate(request, command);
				BindException errors = new BindException(binder.getBindingResult());
				return processFormSubmission(request, response, command, errors);
			}
			catch (HttpSessionRequiredException ex) {
				// Cannot submit a session form if no form object is in the session.
				if (logger.isDebugEnabled()) {
					logger.debug("Invalid submit detected: " + ex.getMessage());
				}
				return handleInvalidSubmit(request, response);
			}
		}

		else {
			// New form to show: render form view.
			return showNewForm(request, response);
		}
	}

	/**
	 * Determine if the given request represents a form submission.
	 * <p>The default implementation treats a POST request as form submission.
	 * Note: If the form session attribute doesn't exist when using session form
	 * mode, the request is always treated as new form by handleRequestInternal.
	 * <p>Subclasses can override this to use a custom strategy, e.g. a specific
	 * request parameter (assumably a hidden field or submit button name).
	 * @param request current HTTP request
	 * @return if the request represents a form submission
	 */
	protected boolean isFormSubmission(HttpServletRequest request) {
		return "POST".equals(request.getMethod());
	}

	/**
	 * Return the name of the HttpSession attribute that holds the form object
	 * for this form controller.
	 * <p>The default implementation delegates to the {@link #getFormSessionAttributeName()}
	 * variant without arguments.
	 * @param request current HTTP request
	 * @return the name of the form session attribute, or {@code null} if not in session form mode
	 * @see #getFormSessionAttributeName
	 * @see javax.servlet.http.HttpSession#getAttribute
	 */
	protected String getFormSessionAttributeName(HttpServletRequest request) {
		return getFormSessionAttributeName();
	}

	/**
	 * Return the name of the HttpSession attribute that holds the form object
	 * for this form controller.
	 * <p>Default is an internal name, of no relevance to applications, as the form
	 * session attribute is not usually accessed directly. Can be overridden to use
	 * an application-specific attribute name, which allows other code to access
	 * the session attribute directly.
	 * @return the name of the form session attribute
	 * @see javax.servlet.http.HttpSession#getAttribute
	 */
	protected String getFormSessionAttributeName() {
		return getClass().getName() + ".FORM." + getCommandName();
	}


	/**
	 * Show a new form. Prepares a backing object for the current form
	 * and the given request, including checking its validity.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @return the prepared form view
	 * @throws Exception in case of an invalid new form object
	 * @see #getErrorsForNewForm
	 */
	protected final ModelAndView showNewForm(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		logger.debug("Displaying new form");
		return showForm(request, response, getErrorsForNewForm(request));
	}

	/**
	 * Create a BindException instance for a new form.
	 * Called by {@link #showNewForm}.
	 * <p>Can be used directly when intending to show a new form but with
	 * special errors registered on it (for example, on invalid submit).
	 * Usually, the resulting BindException will be passed to
	 * {@link #showForm(HttpServletRequest, HttpServletResponse, BindException)},
	 * after registering the errors on it.
	 * @param request current HTTP request
	 * @return the BindException instance
	 * @throws Exception in case of an invalid new form object
	 * @see #showNewForm
	 * @see #showForm(HttpServletRequest, HttpServletResponse, BindException)
	 * @see #handleInvalidSubmit
	 */
	protected final BindException getErrorsForNewForm(HttpServletRequest request) throws Exception {
		// Create form-backing object for new form.
		Object command = formBackingObject(request);
		if (command == null) {
			throw new ServletException("Form object returned by formBackingObject() must not be null");
		}
		if (!checkCommand(command)) {
			throw new ServletException("Form object returned by formBackingObject() must match commandClass");
		}

		// Bind without validation, to allow for prepopulating a form, and for
		// convenient error evaluation in views (on both first attempt and resubmit).
		ServletRequestDataBinder binder = createBinder(request, command);
		BindException errors = new BindException(binder.getBindingResult());
		if (isBindOnNewForm()) {
			logger.debug("Binding to new form");
			binder.bind(request);
			onBindOnNewForm(request, command, errors);
		}

		// Return BindException object that resulted from binding.
		return errors;
	}

	/**
	 * Callback for custom post-processing in terms of binding for a new form.
	 * Called when preparing a new form if {@code bindOnNewForm} is {@code true}.
	 * <p>The default implementation delegates to {@code onBindOnNewForm(request, command)}.
	 * @param request current HTTP request
	 * @param command the command object to perform further binding on
	 * @param errors validation errors holder, allowing for additional
	 * custom registration of binding errors
	 * @throws Exception in case of invalid state or arguments
	 * @see #onBindOnNewForm(javax.servlet.http.HttpServletRequest, Object)
	 * @see #setBindOnNewForm
	 */
	protected void onBindOnNewForm(HttpServletRequest request, Object command, BindException errors)
			throws Exception {

		onBindOnNewForm(request, command);
	}

	/**
	 * Callback for custom post-processing in terms of binding for a new form.
	 * <p>Called by the default implementation of the
	 * {@link #onBindOnNewForm(HttpServletRequest, Object, BindException)} variant
	 * with all parameters, after standard binding when displaying the form view.
	 * Only called if {@code bindOnNewForm} is set to {@code true}.
	 * <p>The default implementation is empty.
	 * @param request current HTTP request
	 * @param command the command object to perform further binding on
	 * @throws Exception in case of invalid state or arguments
	 * @see #onBindOnNewForm(HttpServletRequest, Object, BindException)
	 * @see #setBindOnNewForm(boolean)
	 */
	protected void onBindOnNewForm(HttpServletRequest request, Object command) throws Exception {
	}


	/**
	 * Return the form object for the given request.
	 * <p>Calls {@link #formBackingObject} if not in session form mode.
	 * Else, retrieves the form object from the session. Note that the form object
	 * gets removed from the session, but it will be re-added when showing the
	 * form for resubmission.
	 * @param request current HTTP request
	 * @return object form to bind onto
	 * @throws org.springframework.web.HttpSessionRequiredException
	 * if a session was expected but no active session (or session form object) found
	 * @throws Exception in case of invalid state or arguments
	 * @see #formBackingObject
	 */
	@Override
	protected final Object getCommand(HttpServletRequest request) throws Exception {
		// If not in session-form mode, create a new form-backing object.
		if (!isSessionForm()) {
			return formBackingObject(request);
		}

		// Session-form mode: retrieve form object from HTTP session attribute.
		HttpSession session = request.getSession(false);
		if (session == null) {
			throw new HttpSessionRequiredException("Must have session when trying to bind (in session-form mode)");
		}
		String formAttrName = getFormSessionAttributeName(request);
		Object sessionFormObject = session.getAttribute(formAttrName);
		if (sessionFormObject == null) {
			throw new HttpSessionRequiredException("Form object not found in session (in session-form mode)");
		}

		// Remove form object from HTTP session: we might finish the form workflow
		// in this request. If it turns out that we need to show the form view again,
		// we'll re-bind the form object to the HTTP session.
		if (logger.isDebugEnabled()) {
			logger.debug("Removing form session attribute [" + formAttrName + "]");
		}
		session.removeAttribute(formAttrName);

		return currentFormObject(request, sessionFormObject);
	}

	/**
	 * Retrieve a backing object for the current form from the given request.
	 * <p>The properties of the form object will correspond to the form field values
	 * in your form view. This object will be exposed in the model under the specified
	 * command name, to be accessed under that name in the view: for example, with
	 * a "spring:bind" tag. The default command name is "command".
	 * <p>Note that you need to activate session form mode to reuse the form-backing
	 * object across the entire form workflow. Else, a new instance of the command
	 * class will be created for each submission attempt, just using this backing
	 * object as template for the initial form.
	 * <p>The default implementation calls {@link #createCommand()},
	 * creating a new empty instance of the specified command class.
	 * Subclasses can override this to provide a preinitialized backing object.
	 * @param request current HTTP request
	 * @return the backing object
	 * @throws Exception in case of invalid state or arguments
	 * @see #setCommandName
	 * @see #setCommandClass
	 * @see #createCommand
	 */
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		return createCommand();
	}

	/**
	 * Return the current form object to use for binding and further processing,
	 * based on the passed-in form object as found in the HttpSession.
	 * <p>The default implementation simply returns the session form object as-is.
	 * Subclasses can override this to post-process the session form object,
	 * for example reattaching it to a persistence manager.
	 * @param sessionFormObject the form object retrieved from the HttpSession
	 * @return the form object to use for binding and further processing
	 * @throws Exception in case of invalid state or arguments
	 */
	protected Object currentFormObject(HttpServletRequest request, Object sessionFormObject) throws Exception {
		return sessionFormObject;
	}


	/**
	 * Prepare the form model and view, including reference and error data.
	 * Can show a configured form page, or generate a form view programmatically.
	 * <p>A typical implementation will call
	 * {@code showForm(request, errors, "myView")}
	 * to prepare the form view for a specific view name, returning the
	 * ModelAndView provided there.
	 * <p>For building a custom ModelAndView, call {@code errors.getModel()}
	 * to populate the ModelAndView model with the command and the Errors instance,
	 * under the specified command name, as expected by the "spring:bind" tag.
	 * You also need to include the model returned by {@link #referenceData}.
	 * <p>Note: If you decide to have a "formView" property specifying the
	 * view name, consider using SimpleFormController.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param errors validation errors holder
	 * @return the prepared form view, or {@code null} if handled directly
	 * @throws Exception in case of invalid state or arguments
	 * @see #showForm(HttpServletRequest, BindException, String)
	 * @see org.springframework.validation.Errors
	 * @see org.springframework.validation.BindException#getModel
	 * @see #referenceData(HttpServletRequest, Object, Errors)
	 * @see SimpleFormController#setFormView
	 */
	protected abstract ModelAndView showForm(
			HttpServletRequest request, HttpServletResponse response, BindException errors)
			throws Exception;

	/**
	 * Prepare model and view for the given form, including reference and errors.
	 * <p>In session form mode: Re-puts the form object in the session when
	 * returning to the form, as it has been removed by getCommand.
	 * <p>Can be used in subclasses to redirect back to a specific form page.
	 * @param request current HTTP request
	 * @param errors validation errors holder
	 * @param viewName name of the form view
	 * @return the prepared form view
	 * @throws Exception in case of invalid state or arguments
	 */
	protected final ModelAndView showForm(HttpServletRequest request, BindException errors, String viewName)
			throws Exception {

		return showForm(request, errors, viewName, null);
	}

	/**
	 * Prepare model and view for the given form, including reference and errors,
	 * adding a controller-specific control model.
	 * <p>In session form mode: Re-puts the form object in the session when returning
	 * to the form, as it has been removed by getCommand.
	 * <p>Can be used in subclasses to redirect back to a specific form page.
	 * @param request current HTTP request
	 * @param errors validation errors holder
	 * @param viewName name of the form view
	 * @param controlModel model map containing controller-specific control data
	 * (e.g. current page in wizard-style controllers or special error message)
	 * @return the prepared form view
	 * @throws Exception in case of invalid state or arguments
	 */
	protected final ModelAndView showForm(
			HttpServletRequest request, BindException errors, String viewName, Map controlModel)
			throws Exception {

		// In session form mode, re-expose form object as HTTP session attribute.
		// Re-binding is necessary for proper state handling in a cluster,
		// to notify other nodes of changes in the form object.
		if (isSessionForm()) {
			String formAttrName = getFormSessionAttributeName(request);
			if (logger.isDebugEnabled()) {
				logger.debug("Setting form session attribute [" + formAttrName + "] to: " + errors.getTarget());
			}
			request.getSession().setAttribute(formAttrName, errors.getTarget());
		}

		// Fetch errors model as starting point, containing form object under
		// "commandName", and corresponding Errors instance under internal key.
		Map model = errors.getModel();

		// Merge reference data into model, if any.
		Map referenceData = referenceData(request, errors.getTarget(), errors);
		if (referenceData != null) {
			model.putAll(referenceData);
		}

		// Merge control attributes into model, if any.
		if (controlModel != null) {
			model.putAll(controlModel);
		}

		// Trigger rendering of the specified view, using the final model.
		return new ModelAndView(viewName, model);
	}

	/**
	 * Create a reference data map for the given request, consisting of
	 * bean name/bean instance pairs as expected by ModelAndView.
	 * <p>The default implementation returns {@code null}.
	 * Subclasses can override this to set reference data used in the view.
	 * @param request current HTTP request
	 * @param command form object with request parameters bound onto it
	 * @param errors validation errors holder
	 * @return a Map with reference data entries, or {@code null} if none
	 * @throws Exception in case of invalid state or arguments
	 * @see ModelAndView
	 */
	protected Map referenceData(HttpServletRequest request, Object command, Errors errors) throws Exception {
		return null;
	}


	/**
	 * Process form submission request. Called by {@link #handleRequestInternal}
	 * in case of a form submission, with or without binding errors. Implementations
	 * need to proceed properly, typically showing a form view in case of binding
	 * errors or performing a submit action else.
	 * <p>Subclasses can implement this to provide custom submission handling like
	 * triggering a custom action. They can also provide custom validation and call
	 * {@link #showForm(HttpServletRequest, HttpServletResponse, BindException)}
	 * or proceed with the submission accordingly.
	 * <p>For a success view, call {@code errors.getModel()} to populate the
	 * ModelAndView model with the command and the Errors instance, under the
	 * specified command name, as expected by the "spring:bind" tag. For a form view,
	 * simply return the ModelAndView object provided by
	 * {@link #showForm(HttpServletRequest, HttpServletResponse, BindException)}.
	 * @param request current servlet request
	 * @param response current servlet response
	 * @param command form object with request parameters bound onto it
	 * @param errors holder without errors (subclass can add errors if it wants to)
	 * @return the prepared model and view, or {@code null}
	 * @throws Exception in case of errors
	 * @see #handleRequestInternal
	 * @see #isFormSubmission
	 * @see #showForm(HttpServletRequest, HttpServletResponse, BindException)
	 * @see org.springframework.validation.Errors
	 * @see org.springframework.validation.BindException#getModel
	 */
	protected abstract ModelAndView processFormSubmission(
			HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception;

	/**
	 * Handle an invalid submit request, e.g. when in session form mode but no form object
	 * was found in the session (like in case of an invalid resubmit by the browser).
	 * <p>The default implementation simply tries to resubmit the form with a new
	 * form object. This should also work if the user hit the back button, changed
	 * some form data, and resubmitted the form.
	 * <p>Note: To avoid duplicate submissions, you need to override this method.
	 * Either show some "invalid submit" message, or call {@link #showNewForm} for
	 * resetting the form (prepopulating it with the current values if "bindOnNewForm"
	 * is true). In this case, the form object in the session serves as transaction token.
	 * <pre>
	 * protected ModelAndView handleInvalidSubmit(HttpServletRequest request, HttpServletResponse response) throws Exception {
	 *   return showNewForm(request, response);
	 * }</pre>
	 * You can also show a new form but with special errors registered on it:
	 * <pre class="code">
	 * protected ModelAndView handleInvalidSubmit(HttpServletRequest request, HttpServletResponse response) throws Exception {
	 *   BindException errors = getErrorsForNewForm(request);
	 *   errors.reject("duplicateFormSubmission", "Duplicate form submission");
	 *   return showForm(request, response, errors);
	 * }</pre>
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @return a prepared view, or {@code null} if handled directly
	 * @throws Exception in case of errors
	 * @see #showNewForm
	 * @see #getErrorsForNewForm
	 * @see #showForm(HttpServletRequest, HttpServletResponse, BindException)
	 * @see #setBindOnNewForm
	 */
	protected ModelAndView handleInvalidSubmit(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		Object command = formBackingObject(request);
		ServletRequestDataBinder binder = bindAndValidate(request, command);
		BindException errors = new BindException(binder.getBindingResult());
		return processFormSubmission(request, response, command, errors);
	}

}
