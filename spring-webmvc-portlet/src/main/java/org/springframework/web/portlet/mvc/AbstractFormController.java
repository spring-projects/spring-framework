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

import java.util.Arrays;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.bind.PortletRequestDataBinder;
import org.springframework.web.portlet.handler.PortletSessionRequiredException;

/**
 * <p>Form controller that auto-populates a form bean from the request.
 * This, either using a new bean instance per request, or using the same bean
 * when the <code>sessionForm</code> property has been set to
 * <code>true</code>.</p>
 *
 * <p>This class is the base class for both framework subclasses such as
 * {@link SimpleFormController} and {@link AbstractWizardFormController}
 * and custom form controllers that you may provide yourself.</p>
 *
 * <p>A form-input view and an after-submission view have to be provided
 * programmatically. To provide those views using configuration properties,
 * use the {@link SimpleFormController}.</p>
 *
 * <p>Subclasses need to override <code>showForm</code> to prepare the form view,
 * <code>processFormSubmission</code> to handle submit requests, and
 * <code>renderFormSubmission</code> to display the results of the submit.
 * For the latter two methods, binding errors like type mismatches will be
 * reported via the given "errors" holder. For additional custom form validation,
 * a validator (property inherited from BaseCommandController) can be used,
 * reporting via the same "errors" instance.</p>
 *
 * <p>Comparing this Controller to the Struts notion of the <code>Action</code>
 * shows us that with Spring, you can use any ordinary JavaBeans or database-
 * backed JavaBeans without having to implement a framework-specific class
 * (like Struts' <code>ActionForm</code>). More complex properties of JavaBeans
 * (Dates, Locales, but also your own application-specific or compound types)
 * can be represented and submitted to the controller, by using the notion of
 * a <code>java.beans.PropertyEditors</code>. For more information on that
 * subject, see the workflow of this controller and the explanation of the
 * {@link BaseCommandController BaseCommandController}.</p>
 *
 * <p>This controller is different from it's servlet counterpart in that it must take
 * into account the two phases of a portlet request: the action phase and the render
 * phase. See the JSR-168 spec for more details on these two phases.
 * Be especially aware that the action phase is called only once, but that the
 * render phase will be called repeatedly by the portal; it does this every time
 * the page containing the portlet is updated, even if the activity is in some other
 * portlet. (This is not quite true, the portal can also be told to cache the results of
 * the render for a period of time, but assume it is true for programming purposes.)</p>
 *
 * <p><b><a name="workflow">Workflow
 * (<a href="BaseCommandController.html#workflow">and that defined by superclass</a>):</b><br>
 * <ol>
 * <li><b>The controller receives a request for a new form (typically a
 * Render Request only).</b>  The render phase will proceed to display
 * the form as follows.</li>
 * <li>Call to {@link #formBackingObject formBackingObject()} which by
 * default, returns an instance of the commandClass that has been
 * configured (see the properties the superclass exposes), but can also be
 * overridden to e.g. retrieve an object from the database (that needs to
 * be modified using the form).</li>
 * <li>Call to {@link #initBinder initBinder()} which allows you to
 * register custom editors for certain fields (often properties of non-
 * primitive or non-String types) of the command class. This will render
 * appropriate Strings for those property values, e.g. locale-specific
 * date strings. </li>
 * <li>The {@link PortletRequestDataBinder PortletRequestDataBinder}
 * gets applied to populate the new form object with initial request parameters and the
 * {@link #onBindOnNewForm(RenderRequest, Object, BindException)} callback method is invoked.
 * (<i>only if <code>bindOnNewForm</code> is set to <code>true</code></i>)
 * Make sure that the initial parameters do not include the parameter that indicates a
 * form submission has occurred.</li>
 * <li>Call to {@link #showForm(RenderRequest, RenderResponse,
		* BindException) showForm} to return a View that should be rendered
 * (typically the view that renders the form). This method has to be
 * implemented in subclasses. </li>
 * <li>The showForm() implementation will call {@link #referenceData referenceData},
 * which you can implement to provide any relevant reference data you might need
 * when editing a form (e.g. a List of Locale objects you're going to let the
 * user select one from).</li>
 * <li>Model gets exposed and view gets rendered, to let the user fill in
 * the form.</li>
 * <li><b>The controller receives a form submission (typically an Action
 * Request).</b> To use a different way of detecting a form submission,
 * override the {@link #isFormSubmission isFormSubmission} method.
 * The action phase will proceed to process the form submission as follows.</li>
 * <li>If <code>sessionForm</code> is not set, {@link #formBackingObject
 * formBackingObject} is called to retrieve a form object. Otherwise,
 * the controller tries to find the command object which is already bound
 * in the session. If it cannot find the object, the action phase does a
 * call to {@link #handleInvalidSubmit handleInvalidSubmit} which - by default -
 * tries to create a new form object and resubmit the form.  It then sets
 * a render parameter that will indicate to the render phase that this was
 * an invalid submit.</li>
 * <li>Still in the action phase of a valid submit, the {@link
 * PortletRequestDataBinder PortletRequestDataBinder} gets applied to populate
 * the form object with current request parameters.</li>
 * <li>Call to {@link #onBind onBind(PortletRequest, Object, Errors)}
 * which allows you to do custom processing after binding but before
 * validation (e.g. to manually bind request parameters to bean
 * properties, to be seen by the Validator).</li>
 * <li>If <code>validateOnBinding</code> is set, a registered Validator
 * will be invoked. The Validator will check the form object properties,
 * and register corresponding errors via the given {@link Errors Errors}
 * object.</li>
 * <li>Call to {@link #onBindAndValidate onBindAndValidate} which allows
 * you to do custom processing after binding and validation (e.g. to
 * manually bind request parameters, and to validate them outside a
 * Validator).</li>
 * <li>Call to {@link #processFormSubmission processFormSubmission}
 * to process the submission, with or without binding errors.
 * This method has to be implemented in subclasses and will be called
 * only once per form submission.</li>
 * <li>The portal will then call the render phase of processing the form
 * submission.  This phase will be called repeatedly by the portal every
 * time the page is refreshed.  All processing here should take this into
 * account.  Any one-time-only actions (such as modifying a database) must
 * be done in the action phase.</li>
 * <li>If the action phase indicated this is an invalid submit, the render
 * phase calls {@link #renderInvalidSubmit renderInvalidSubmit} which &ndash;
 * also by default &ndash; will render the results of the resubmitted
 * form.  Be sure to override both <code>handleInvalidSubmit</code> and
 * <code>renderInvalidSubmit</code> if you want to change this overall
 * behavior.</li>
 * <li>Finally, call {@link #renderFormSubmission renderFormSubmission} to
 * render the results of the submission, with or without binding errors.
 * This method has to be implemented in subclasses and will be called
 * repeatedly by the portal.</li>
 * </ol>
 * </p>
 *
 * <p>In session form mode, a submission without an existing form object in the
 * session is considered invalid, like in the case of a resubmit/reload by the browser.
 * The {@link #handleInvalidSubmit handleInvalidSubmit} /
 * {@link #renderInvalidSubmit renderInvalidSubmit} methods are invoked then,
 * by default trying to resubmit. This can be overridden in subclasses to show
 * corresponding messages or to redirect to a new form, in order to avoid duplicate
 * submissions. The form object in the session can be considered a transaction token
 * in that case.</p>
 *
 * <p>Make sure that any URLs that take you to your form controller are Render URLs,
 * so that it will not try to treat the initial call as a form submission.
 * If you use action URLs to link to your controller, you will need to override the
 * {@link #isFormSubmission isFormSubmission} method to use a different mechanism for
 * determining whether a form has been submitted. Make sure this method will work for
 * both the ActionRequest and the RenderRequest objects.</p>
 *
 * <p>Note that views should never retrieve form beans from the session but always
 * from the request, as prepared by the form controller. Remember that some view
 * technologies like Velocity cannot even access a HTTP session.</p>
 *
 * <p><b><a name="config">Exposed configuration properties</a>
 * (<a href="BaseCommandController.html#config">and those defined by superclass</a>):</b><br>
 * <table border="1">
 * <tr>
 * <td><b>name</b></td>
 * <td><b>default</b></td>
 * <td><b>description</b></td>
 * </tr>
 * <tr>
 * <td>bindOnNewForm</td>
 * <td>false</td>
 * <td>Indicates whether to bind portlet request parameters when
 * creating a new form. Otherwise, the parameters will only be
 * bound on form submission attempts.</td>
 * </tr>
 * <tr>
 * <td>sessionForm</td>
 * <td>false</td>
 * <td>Indicates whether the form object should be kept in the session
 * when a user asks for a new form. This allows you e.g. to retrieve
 * an object from the database, let the user edit it, and then persist
 * it again. Otherwise, a new command object will be created for each
 * request (even when showing the form again after validation errors).</td>
 * </tr>
 * <tr>
 * <td>redirectAction</td>
 * <td>false</td>
 * <td>Specifies whether <code>processFormSubmission</code> is expected to call
 * {@link ActionResponse#sendRedirect ActionResponse.sendRedirect}.
 * This is important because some methods may not be called before
 * {@link ActionResponse#sendRedirect ActionResponse.sendRedirect} (e.g.
 * {@link ActionResponse#setRenderParameter ActionResponse.setRenderParameter}).
 * Setting this flag will prevent AbstractFormController from setting render
 * parameters that it normally needs for the render phase.
 * If this is set true and <code>sendRedirect</code> is not called, then
 * <code>processFormSubmission</code> must call
 * {@link #setFormSubmit setFormSubmit}.
 * Otherwise, the render phase will not realize the form was submitted
 * and will simply display a new blank form.</td>
 * </tr>
 * <tr>
 * <td>renderParameters</td>
 * <td>null</td>
 * <td>An array of parameters that will be passed forward from the action
 * phase to the render phase if the form needs to be displayed
 * again.  These can also be passed forward explicitly by calling
 * the <code>passRenderParameters</code> method from any action
 * phase method.  Abstract descendants of this controller should follow
 * similar behavior.  If there are parameters you need in
 * <code>renderFormSubmission</code>, then you need to pass those
 * forward from <code>processFormSubmission</code>.  If you override the
 * default behavior of invalid submits and you set sessionForm to true,
 * then you probably will not need to set this because your parameters
 * are only going to be needed on the first request.</td>
 * </tr>
 * </table>
 * </p>
 *
 * <p>Thanks to Rainer Schmitz and Nick Lothian for their suggestions!
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @author Rob Harrop
 * @since 2.0
 * @see #showForm(RenderRequest, RenderResponse, BindException)
 * @see SimpleFormController
 * @see AbstractWizardFormController
 * @deprecated as of Spring 3.0, in favor of annotated controllers
 */
@Deprecated
public abstract class AbstractFormController extends BaseCommandController {

	/**
	 * These render parameters are used to indicate forward to the render phase
	 * if the form was submitted and if the submission was invalid.
	 */
	private static final String FORM_SUBMISSION_PARAMETER = "form-submit";

	private static final String INVALID_SUBMISSION_PARAMETER = "invalid-submit";

	private static final String TRUE = Boolean.TRUE.toString();


	private boolean bindOnNewForm = false;

	private boolean sessionForm = false;

	private boolean redirectAction = false;

	private String[] renderParameters = null;


	/**
	 * Create a new AbstractFormController.
	 * <p>Subclasses should set the following properties, either in the constructor
	 * or via a BeanFactory: commandName, commandClass, bindOnNewForm, sessionForm.
	 * Note that commandClass doesn't need to be set when overriding
	 * <code>formBackingObject</code>, as the latter determines the class anyway.
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
	 * Set if request parameters should be bound to the form object
	 * in case of a non-submitting request, i.e. a new form.
	 */
	public final void setBindOnNewForm(boolean bindOnNewForm) {
		this.bindOnNewForm = bindOnNewForm;
	}

	/**
	 * Return if request parameters should be bound in case of a new form.
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
	 */
	public final void setSessionForm(boolean sessionForm) {
		this.sessionForm = sessionForm;
	}

	/**
	 * Return if session form mode is activated.
	 */
	public final boolean isSessionForm() {
		return this.sessionForm;
	}

	/**
	 * Specify whether the action phase is expected to call
	 * {@link ActionResponse#sendRedirect}.
	 * This information is important because some methods may not be called
	 * before {@link ActionResponse#sendRedirect}, e.g.
	 * {@link ActionResponse#setRenderParameter} and
	 * {@link ActionResponse#setRenderParameters}.
	 * <p><b>NOTE:</b> Call this at initialization time of your controller:
	 * either in the constructor or in the bean definition for your controller.
	 * @see ActionResponse#sendRedirect
	 */
	public void setRedirectAction(boolean redirectAction) {
		this.redirectAction = redirectAction;
	}

	/**
	 * Return if {@link ActionResponse#sendRedirect} is
	 * expected to be called in the action phase.
	 */
	public boolean isRedirectAction() {
		return this.redirectAction;
	}

	/**
	 * Specify the list of parameters that should be passed forward
	 * from the action phase to the render phase whenever the form is
	 * re-rendered or when {@link #passRenderParameters} is called.
	 * @see #passRenderParameters
	 */
	public void setRenderParameters(String[] parameters) {
		this.renderParameters = parameters;
	}

	/**
	 * Returns the list of parameters that will be passed forward
	 * from the action phase to the render phase whenever the form is
	 * rerendered or when {@link #passRenderParameters} is called.
	 * @return the list of parameters
	 * @see #passRenderParameters
	 */
	public String[] getRenderParameters() {
		return this.renderParameters;
	}


	/**
	 * Handles action phase of two cases: form submissions and showing a new form.
	 * Delegates the decision between the two to <code>isFormSubmission</code>,
	 * always treating requests without existing form session attribute
	 * as new form when using session form mode.
	 * @see #isFormSubmission
	 * @see #processFormSubmission
	 * @see #handleRenderRequestInternal
	 */
	@Override
	protected void handleActionRequestInternal(ActionRequest request, ActionResponse response)
			throws Exception {

		// Form submission or new form to show?
		if (isFormSubmission(request)) {
			// Fetch form object, bind, validate, process submission.
			try {
				Object command = getCommand(request);
				if (logger.isDebugEnabled()) {
					logger.debug("Processing valid submit (redirectAction = " + isRedirectAction() + ")");
				}
				if (!isRedirectAction()) {
					setFormSubmit(response);
				}
				PortletRequestDataBinder binder = bindAndValidate(request, command);
				BindException errors = new BindException(binder.getBindingResult());
				processFormSubmission(request, response, command, errors);
				setRenderCommandAndErrors(request, command, errors);
				return;
			}
			catch (PortletSessionRequiredException ex) {
				// Cannot submit a session form if no form object is in the session.
				if (logger.isDebugEnabled()) {
					logger.debug("Invalid submit detected: " + ex.getMessage());
				}
				setFormSubmit(response);
				setInvalidSubmit(response);
				handleInvalidSubmit(request, response);
				return;
			}
		}

		else {
			logger.debug("Not a form submit - passing parameters to render phase");
			passRenderParameters(request, response);
			return;
		}
	}

	/**
	 * Handles render phase of two cases: form submissions and showing a new form.
	 * Delegates the decision between the two to <code>isFormSubmission</code>,
	 * always treating requests without existing form session attribute
	 * as new form when using session form mode.
	 * @see #isFormSubmission
	 * @see #showNewForm
	 * @see #processFormSubmission
	 * @see #handleActionRequestInternal
	 */
	@Override
	protected ModelAndView handleRenderRequestInternal(RenderRequest request, RenderResponse response)
			throws Exception {

		// Form submission or new form to show?
		if (isFormSubmission(request)) {

			// If it is an invalid submit then handle it.
			if (isInvalidSubmission(request)) {
				logger.debug("Invalid submit - calling renderInvalidSubmit");
				return renderInvalidSubmit(request, response);
			}

			// Valid submit -> render.
			logger.debug("Valid submit - calling renderFormSubmission");
			return renderFormSubmission(request, response, getRenderCommand(request), getRenderErrors(request));
		}

		else {
			// New form to show: render form view.
			return showNewForm(request, response);
		}
	}

	/**
	 * Determine if the given request represents a form submission.
	 * <p>The default implementation checks to see if this is an ActionRequest
	 * and treats all action requests as form submission. During the action
	 * phase it will pass forward a render parameter to indicate to the render
	 * phase that this is a form submission. This method can check both
	 * kinds of requests and indicate if this is a form submission.
	 * <p>Subclasses can override this to use a custom strategy, e.g. a specific
	 * request parameter (assumably a hidden field or submit button name). Make
	 * sure that the override can handle both ActionRequest and RenderRequest
	 * objects properly.
	 * @param request current request
	 * @return if the request represents a form submission
	 */
	protected boolean isFormSubmission(PortletRequest request) {
		return (request instanceof ActionRequest || TRUE.equals(request.getParameter(getFormSubmitParameterName())));
	}

	/**
	 * Determine if the given request represents an invalid form submission.
	 */
	protected boolean isInvalidSubmission(PortletRequest request) {
		return TRUE.equals(request.getParameter(getInvalidSubmitParameterName()));
	}

	/**
	 * Return the name of the render parameter that indicates this
	 * was a form submission.
	 * @return the name of the render parameter
	 * @see javax.portlet.RenderRequest#getParameter
	 */
	protected String getFormSubmitParameterName() {
		return FORM_SUBMISSION_PARAMETER;
	}

	/**
	 * Return the name of the render parameter that indicates this
	 * was an invalid form submission.
	 * @return the name of the render parameter
	 * @see javax.portlet.RenderRequest#getParameter
	 */
	protected String getInvalidSubmitParameterName() {
		return INVALID_SUBMISSION_PARAMETER;
	}

	/**
	 * Set the action response parameter that indicates this in a form submission.
	 * @param response the current action response
	 * @see #getFormSubmitParameterName()
	 */
	protected final void setFormSubmit(ActionResponse response) {
		if (logger.isDebugEnabled()) {
			logger.debug("Setting render parameter [" + getFormSubmitParameterName() +
					"] to indicate this is a form submission");
		}
		try {
			response.setRenderParameter(getFormSubmitParameterName(), TRUE);
		}
		catch (IllegalStateException ex) {
			// Ignore in case sendRedirect was already set.
		}
	}

	/**
	 * Set the action response parameter that indicates this in an invalid submission.
	 * @param response the current action response
	 * @see #getInvalidSubmitParameterName()
	 */
	protected final void setInvalidSubmit(ActionResponse response) {
		if (logger.isDebugEnabled()) {
			logger.debug("Setting render parameter [" + getInvalidSubmitParameterName() +
					"] to indicate this is an invalid submission");
		}
		try {
			response.setRenderParameter(getInvalidSubmitParameterName(), TRUE);
		}
		catch (IllegalStateException ex) {
			// Ignore in case sendRedirect was already set.
		}
	}

	/**
	 * Return the name of the PortletSession attribute that holds the form object
	 * for this form controller.
	 * <p>The default implementation delegates to the
	 * <code>getFormSessionAttributeName</code> version without arguments.
	 * @param request current HTTP request
	 * @return the name of the form session attribute,
	 * or <code>null</code> if not in session form mode
	 * @see #getFormSessionAttributeName()
	 * @see javax.portlet.PortletSession#getAttribute
	 */
	protected String getFormSessionAttributeName(PortletRequest request) {
		return getFormSessionAttributeName();
	}

	/**
	 * Return the name of the PortletSession attribute that holds the form object
	 * for this form controller.
	 * <p>Default is an internal name, of no relevance to applications, as the form
	 * session attribute is not usually accessed directly. Can be overridden to use
	 * an application-specific attribute name, which allows other code to access
	 * the session attribute directly.
	 * @return the name of the form session attribute
	 * @see javax.portlet.PortletSession#getAttribute
	 */
	protected String getFormSessionAttributeName() {
		return getClass().getName() + ".FORM." + getCommandName();
	}

	/**
	 * Pass the specified list of action request parameters to the render phase
	 * by putting them into the action response object. This may not be called
	 * when the action will call will call
	 * {@link ActionResponse#sendRedirect sendRedirect}.
	 * @param request the current action request
	 * @param response the current action response
	 * @see ActionResponse#setRenderParameter
	 */
	protected void passRenderParameters(ActionRequest request, ActionResponse response) {
		if (this.renderParameters == null) {
			return;
		}
		try {
			for (int i = 0; i < this.renderParameters.length; i++) {
				String paramName = this.renderParameters[i];
				String paramValues[] = request.getParameterValues(paramName);
				if (paramValues != null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Passing parameter to render phase '" + paramName + "' = " +
								(paramValues == null ? "NULL" : Arrays.asList(paramValues).toString()));
					}
					response.setRenderParameter(paramName, paramValues);
				}
			}
		}
		catch (IllegalStateException ex) {
			// Ignore in case sendRedirect was already set.
		}
	}


	/**
	 * Show a new form. Prepares a backing object for the current form
	 * and the given request, including checking its validity.
	 * @param request current render request
	 * @param response current render response
	 * @return the prepared form view
	 * @throws Exception in case of an invalid new form object
	 * @see #getErrorsForNewForm
	 */
	protected final ModelAndView showNewForm(RenderRequest request, RenderResponse response)
			throws Exception {

		logger.debug("Displaying new form");
		return showForm(request, response, getErrorsForNewForm(request));
	}

	/**
	 * Create a BindException instance for a new form.
	 * Called by <code>showNewForm</code>.
	 * <p>Can be used directly when intending to show a new form but with
	 * special errors registered on it (for example, on invalid submit).
	 * Usually, the resulting BindException will be passed to
	 * <code>showForm</code>, after registering the errors on it.
	 * @param request current render request
	 * @return the BindException instance
	 * @throws Exception in case of an invalid new form object
	 */
	protected final BindException getErrorsForNewForm(RenderRequest request) throws Exception {
		// Create form-backing object for new form
		Object command = formBackingObject(request);
		if (command == null) {
			throw new PortletException("Form object returned by formBackingObject() must not be null");
		}
		if (!checkCommand(command)) {
			throw new PortletException("Form object returned by formBackingObject() must match commandClass");
		}

		// Bind without validation, to allow for prepopulating a form, and for
		// convenient error evaluation in views (on both first attempt and resubmit).
		PortletRequestDataBinder binder = createBinder(request, command);
		BindException errors = new BindException(binder.getBindingResult());

		if (isBindOnNewForm()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Binding to new form");
			}
			binder.bind(request);
			onBindOnNewForm(request, command, errors);
		}

		// Return BindException object that resulted from binding.
		return errors;
	}

	/**
	 * Callback for custom post-processing in terms of binding for a new form.
	 * Called when preparing a new form if <code>bindOnNewForm</code> is <code>true</code>.
	 * <p>Default implementation delegates to <code>onBindOnNewForm(request, command)</code>.
	 * @param request current render request
	 * @param command the command object to perform further binding on
	 * @param errors validation errors holder, allowing for additional
	 * custom registration of binding errors
	 * @throws Exception in case of invalid state or arguments
	 * @see #onBindOnNewForm(RenderRequest, Object)
	 * @see #setBindOnNewForm
	 */
	protected void onBindOnNewForm(RenderRequest request, Object command, BindException errors)
			throws Exception {

		onBindOnNewForm(request, command);
	}

	/**
	 * Callback for custom post-processing in terms of binding for a new form.
	 * Called by the default implementation of the <code>onBindOnNewForm</code> version
	 * with all parameters, after standard binding when displaying the form view.
	 * Only called if <code>bindOnNewForm</code> is set to <code>true</code>.
	 * <p>The default implementation is empty.
	 * @param request current render request
	 * @param command the command object to perform further binding on
	 * @throws Exception in case of invalid state or arguments
	 * @see #onBindOnNewForm(RenderRequest, Object, BindException)
	 * @see #setBindOnNewForm(boolean)
	 */
	protected void onBindOnNewForm(RenderRequest request, Object command) throws Exception {
	}


	/**
	 * Return the form object for the given request.
	 * <p>Calls <code>formBackingObject</code> if the object is not in the session
	 * @param request current request
	 * @return object form to bind onto
	 * @see #formBackingObject
	 */
	@Override
	protected final Object getCommand(PortletRequest request) throws Exception {
		// If not in session-form mode, create a new form-backing object.
		if (!isSessionForm()) {
			return formBackingObject(request);
		}

		// Session-form mode: retrieve form object from portlet session attribute.
		PortletSession session = request.getPortletSession(false);
		if (session == null) {
			throw new PortletSessionRequiredException("Must have session when trying to bind (in session-form mode)");
		}
		String formAttrName = getFormSessionAttributeName(request);
		Object sessionFormObject = session.getAttribute(formAttrName);
		if (sessionFormObject == null) {
			throw new PortletSessionRequiredException("Form object not found in session (in session-form mode)");
		}

		// Remove form object from porlet session: we might finish the form workflow
		// in this request. If it turns out that we need to show the form view again,
		// we'll re-bind the form object to the portlet session.
		if (logger.isDebugEnabled()) {
			logger.debug("Removing form session attribute [" + formAttrName + "]");
		}
		session.removeAttribute(formAttrName);

		// Check the command object to make sure its valid
		if (!checkCommand(sessionFormObject)) {
			throw new PortletSessionRequiredException("Object found in session does not match commandClass");
		}

		return sessionFormObject;
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
	 * <p>The default implementation calls <code>BaseCommandController.createCommand</code>,
	 * creating a new empty instance of the command class.
	 * Subclasses can override this to provide a preinitialized backing object.
	 * @param request current portlet request
	 * @return the backing object
	 * @throws Exception in case of invalid state or arguments
	 * @see #setCommandName
	 * @see #setCommandClass
	 * @see #createCommand
	 */
	protected Object formBackingObject(PortletRequest request) throws Exception {
		return createCommand();
	}


	/**
	 * Prepare the form model and view, including reference and error data.
	 * Can show a configured form page, or generate a form view programmatically.
	 * <p>A typical implementation will call
	 * <code>showForm(request, errors, "myView")</code>
	 * to prepare the form view for a specific view name, returning the
	 * ModelAndView provided there.
	 * <p>For building a custom ModelAndView, call <code>errors.getModel()</code>
	 * to populate the ModelAndView model with the command and the Errors instance,
	 * under the specified command name, as expected by the "spring:bind" tag.
	 * You also need to include the model returned by <code>referenceData</code>.
	 * <p>Note: If you decide to have a "formView" property specifying the
	 * view name, consider using SimpleFormController.
	 * @param request current render request
	 * @param response current render response
	 * @param errors validation errors holder
	 * @return the prepared form view, or null if handled directly
	 * @throws Exception in case of invalid state or arguments
	 * @see #showForm(RenderRequest, BindException, String)
	 * @see org.springframework.validation.Errors
	 * @see org.springframework.validation.BindException#getModel
	 * @see #referenceData(PortletRequest, Object, Errors)
	 * @see SimpleFormController#setFormView
	 */
	protected abstract ModelAndView showForm(RenderRequest request, RenderResponse response, BindException errors) throws Exception;

	/**
	 * Prepare model and view for the given form, including reference and errors.
	 * <p>In session form mode: Re-puts the form object in the session when
	 * returning to the form, as it has been removed by getCommand.
	 * <p>Can be used in subclasses to redirect back to a specific form page.
	 * @param request current render request
	 * @param errors validation errors holder
	 * @param viewName name of the form view
	 * @return the prepared form view
	 * @throws Exception in case of invalid state or arguments
	 * @see #showForm(RenderRequest, BindException, String, Map)
	 * @see #showForm(RenderRequest, RenderResponse, BindException)
	 */
	protected final ModelAndView showForm(RenderRequest request, BindException errors, String viewName)
			throws Exception {

		return showForm(request, errors, viewName, null);
	}

	/**
	 * Prepare model and view for the given form, including reference and errors,
	 * adding a controller-specific control model.
	 * <p>In session form mode: Re-puts the form object in the session when returning
	 * to the form, as it has been removed by getCommand.
	 * <p>Can be used in subclasses to redirect back to a specific form page.
	 * @param request current render request
	 * @param errors validation errors holder
	 * @param viewName name of the form view
	 * @param controlModel model map containing controller-specific control data
	 * (e.g. current page in wizard-style controllers or special error message)
	 * @return the prepared form view
	 * @throws Exception in case of invalid state or arguments
	 * @see #showForm(RenderRequest, BindException, String)
	 * @see #showForm(RenderRequest, RenderResponse, BindException)
	 */
	protected final ModelAndView showForm(RenderRequest request, BindException errors, String viewName, Map<String, ?> controlModel)
			throws Exception {

		// In session form mode, re-expose form object as portlet session attribute.
		// Re-binding is necessary for proper state handling in a cluster,
		// to notify other nodes of changes in the form object.
		if (isSessionForm()) {
			String formAttrName = getFormSessionAttributeName(request);
			if (logger.isDebugEnabled()) {
				logger.debug("Setting form session attribute [" + formAttrName + "] to: " + errors.getTarget());
			}
			request.getPortletSession().setAttribute(formAttrName, errors.getTarget());
		}

		// Fetch errors model as starting point, containing form object under
		// "commandName", and corresponding Errors instance under internal key.
		Map<String, Object> model = errors.getModel();

		// Merge reference data into model, if any.
		Map<String, ?> referenceData = referenceData(request, errors.getTarget(), errors);
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
	 * <p>The default implementation returns <code>null</code>.
	 * Subclasses can override this to set reference data used in the view.
	 * @param request current render request
	 * @param command form object with request parameters bound onto it
	 * @param errors validation errors holder
	 * @return a Map with reference data entries, or null if none
	 * @throws Exception in case of invalid state or arguments
	 * @see ModelAndView
	 */
	protected Map<String, ?> referenceData(PortletRequest request, Object command, Errors errors) throws Exception {
		return null;
	}


	/**
	 * Process render phase of form submission request. Called by <code>handleRequestInternal</code>
	 * in case of a form submission, with or without binding errors. Implementations
	 * need to proceed properly, typically showing a form view in case of binding
	 * errors or rendering the result of a submit action else.
	 * <p>For a success view, call <code>errors.getModel()</code> to populate the
	 * ModelAndView model with the command and the Errors instance, under the
	 * specified command name, as expected by the "spring:bind" tag. For a form view,
	 * simply return the ModelAndView object privded by <code>showForm</code>.
	 * @param request current render request
	 * @param response current render response
	 * @param command form object with request parameters bound onto it
	 * @param errors errors holder
	 * @return the prepared model and view, or null
	 * @throws Exception in case of errors
	 * @see #handleRenderRequestInternal
	 * @see #processFormSubmission
	 * @see #isFormSubmission
	 * @see #showForm(RenderRequest, RenderResponse, BindException)
	 * @see org.springframework.validation.Errors
	 * @see org.springframework.validation.BindException#getModel
	 */
	protected abstract ModelAndView renderFormSubmission(RenderRequest request, RenderResponse response, Object command, BindException errors)
			throws Exception;

	/**
	 * Process action phase of form submission request. Called by <code>handleRequestInternal</code>
	 * in case of a form submission, with or without binding errors. Implementations
	 * need to proceed properly, typically performing a submit action if there are no binding errors.
	 * <p>Subclasses can implement this to provide custom submission handling
	 * like triggering a custom action. They can also provide custom validation
	 * or proceed with the submission accordingly.
	 * @param request current action request
	 * @param response current action response
	 * @param command form object with request parameters bound onto it
	 * @param errors errors holder (subclass can add errors if it wants to)
	 * @throws Exception in case of errors
	 * @see #handleActionRequestInternal
	 * @see #renderFormSubmission
	 * @see #isFormSubmission
	 * @see org.springframework.validation.Errors
	 */
	protected abstract void processFormSubmission(ActionRequest request, ActionResponse response, Object command, BindException errors)
			throws Exception;

	/**
	 * Handle an invalid submit request, e.g. when in session form mode but no form object
	 * was found in the session (like in case of an invalid resubmit by the browser).
	 * <p>The default implementation simply tries to resubmit the form with a new form object.
	 * This should also work if the user hit the back button, changed some form data,
	 * and resubmitted the form.
	 * <p>Note: To avoid duplicate submissions, you need to override this method.
	 * Either show some "invalid submit" message, or call <code>showNewForm</code> for
	 * resetting the form (prepopulating it with the current values if "bindOnNewForm"
	 * is true). In this case, the form object in the session serves as transaction token.
	 * <pre class="code">
	 * protected ModelAndView renderInvalidSubmit(RenderRequest request, RenderResponse response) throws Exception {
	 *   return showNewForm(request, response);
	 * }</pre>
	 * You can also show a new form but with special errors registered on it:
	 * <pre class="code">
	 * protected ModelAndView renderInvalidSubmit(RenderRequest request, RenderResponse response) throws Exception {
	 *   BindException errors = getErrorsForNewForm(request);
	 *   errors.reject("duplicateFormSubmission", "Duplicate form submission");
	 *   return showForm(request, response, errors);
	 * }</pre>
	 * <p><b>WARNING:</b> If you override this method, be sure to also override the action
	 * phase version of this method so that it will not attempt to perform the resubmit
	 * action by default.
	 * @param request current render request
	 * @param response current render response
	 * @return a prepared view, or null if handled directly
	 * @throws Exception in case of errors
	 * @see #handleInvalidSubmit
	 */
	protected ModelAndView renderInvalidSubmit(RenderRequest request, RenderResponse response)
			throws Exception {

		return renderFormSubmission(request, response, getRenderCommand(request), getRenderErrors(request));
	}

	/**
	 * Handle an invalid submit request, e.g. when in session form mode but no form object
	 * was found in the session (like in case of an invalid resubmit by the browser).
	 * <p>The default implementation simply tries to resubmit the form with a new form object.
	 * This should also work if the user hit the back button, changed some form data,
	 * and resubmitted the form.
	 * <p>Note: To avoid duplicate submissions, you need to override this method.
	 * Most likely you will simply want it to do nothing here in the action phase
	 * and diplay an appropriate error and a new form in the render phase.
	 * <pre class="code">
	 * protected void handleInvalidSubmit(ActionRequest request, ActionResponse response) throws Exception {
	 * }</pre>
	 * <p>If you override this method but you do need a command object and bind errors
	 * in the render phase, be sure to call {@link #setRenderCommandAndErrors setRenderCommandAndErrors}
	 * from here.
	 * @param request current action request
	 * @param response current action response
	 * @throws Exception in case of errors
	 * @see #renderInvalidSubmit
	 * @see #setRenderCommandAndErrors
	 */
	protected void handleInvalidSubmit(ActionRequest request, ActionResponse response) throws Exception {
		passRenderParameters(request, response);
		Object command = formBackingObject(request);
		if (command == null) {
			throw new PortletException("Form object returned by formBackingObject() must not be null");
		}
		if (!checkCommand(command)) {
			throw new PortletException("Form object returned by formBackingObject() must match commandClass");
		}
		PortletRequestDataBinder binder = bindAndValidate(request, command);
		BindException errors = new BindException(binder.getBindingResult());
		processFormSubmission(request, response, command, errors);
		setRenderCommandAndErrors(request, command, errors);
	}

}
