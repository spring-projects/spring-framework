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

import javax.portlet.ActionRequest;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingErrorProcessor;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.portlet.bind.PortletRequestDataBinder;
import org.springframework.web.portlet.context.PortletWebRequest;
import org.springframework.web.portlet.handler.PortletSessionRequiredException;

/**
 * <p>Controller implementation which creates an object (the command object) on
 * receipt of a request and attempts to populate this object with request parameters.</p>
 *
 * <p>This controller is the base for all controllers wishing to populate
 * JavaBeans based on request parameters, validate the content of such
 * JavaBeans using {@link Validator Validators} and use custom editors (in the form of
 * {@link java.beans.PropertyEditor PropertyEditors}) to transform
 * objects into strings and vice versa, for example. Three notions are mentioned here:</p>
 *
 * <p><b>Command class:</b><br>
 * An instance of the command class will be created for each request and populated
 * with request parameters. A command class can basically be any Java class; the only
 * requirement is a no-arg constructor. The command class should preferably be a
 * JavaBean in order to be able to populate bean properties with request parameters.</p>
 *
 * <p><b>Populating using request parameters and PropertyEditors:</b><br>
 * Upon receiving a request, any BaseCommandController will attempt to fill the
 * command object using the request parameters. This is done using the typical
 * and well-known JavaBeans property notation. When a request parameter named
 * {@code 'firstName'} exists, the framework will attempt to call
 * {@code setFirstName([value])} passing the value of the parameter. Nested properties
 * are of course supported. For instance a parameter named {@code 'address.city'}
 * will result in a {@code getAddress().setCity([value])} call on the
 * command class.</p>
 *
 * <p>It's important to realize that you are not limited to String arguments in
 * your JavaBeans. Using the PropertyEditor-notion as supplied by the
 * java.beans package, you will be able to transform Strings to Objects and
 * the other way around. For instance {@code setLocale(Locale loc)} is
 * perfectly possible for a request parameter named {@code locale} having
 * a value of {@code en}, as long as you register the appropriate
 * PropertyEditor in the Controller (see {@link #initBinder initBinder()}
 * for more information on that matter).</p>
 *
 * <p><b>Validators:</b>
 * After the controller has successfully populated the command object with
 * parameters from the request, it will use any configured validators to
 * validate the object. Validation results will be put in a
 * {@link org.springframework.validation.Errors Errors} object which can be
 * used in a View to render any input problems.</p>
 *
 * <p><b><a name="workflow">Workflow
 * (<a href="AbstractController.html#workflow">and that defined by superclass</a>):</b><br>
 * Since this class is an abstract base class for more specific implementation,
 * it does not override the {@code handleRequestInternal()} methods and also has no
 * actual workflow. Implementing classes like
 * {@link AbstractFormController AbstractFormController},
 * {@link AbstractCommandController AbstractCommandController},
 * {@link SimpleFormController SimpleFormController} and
 * {@link AbstractWizardFormController AbstractWizardFormController}
 * provide actual functionality and workflow.
 * More information on workflow performed by superclasses can be found
 * <a href="AbstractController.html#workflow">here</a>.</p>
 *
 * <p><b><a name="config">Exposed configuration properties</a>
 * (<a href="AbstractController.html#config">and those defined by superclass</a>):</b><br>
 * <table border="1">
 *  <tr>
 *      <td><b>name</b></th>
 *      <td><b>default</b></td>
 *      <td><b>description</b></td>
 *  </tr>
 *  <tr>
 *      <td>commandName</td>
 *      <td>command</td>
 *      <td>the name to use when binding the instantiated command class
 *          to the request</td>
 *  </tr>
 *  <tr>
 *      <td>commandClass</td>
 *      <td><i>null</i></td>
 *      <td>the class to use upon receiving a request and which to fill
 *          using the request parameters. What object is used and whether
 *          or not it should be created is defined by extending classes
 *          and their configuration properties and methods.</td>
 *  </tr>
 *  <tr>
 *      <td>validators</td>
 *      <td><i>null</i></td>
 *      <td>Array of Validator beans. The validator will be called at appropriate
 *          places in the workflow of subclasses (have a look at those for more info)
 *          to validate the command object.</td>
 *  </tr>
 *  <tr>
 *      <td>validator</td>
 *      <td><i>null</i></td>
 *      <td>Short-form property for setting only one Validator bean (usually passed in
 *          using a &lt;ref bean="beanId"/&gt; property.</td>
 *  </tr>
 *  <tr>
 *      <td>validateOnBinding</td>
 *      <td>true</td>
 *      <td>Indicates whether or not to validate the command object after the
 *          object has been populated with request parameters.</td>
 *  </tr>
 * </table>
 * </p>
 *
 * <p>Thanks to Rainer Schmitz and Nick Lothian for their suggestions!
 *
 * @author Juergen Hoeller
 * @author John A. Lewis
 * @since 2.0
 * @deprecated as of Spring 3.0, in favor of annotated controllers
 */
@Deprecated
public abstract class BaseCommandController extends AbstractController {

	/**
	 * Unlike the servlet version of these classes, we have to deal with the
	 * two-phase nature of the portlet request. To do this, we need to pass
	 * forward the command object and the bind/validation errors that occured
	 * on the command object from the action phase to the render phase.
	 * The only direct way to pass things forward and preserve them for each
	 * render request is through render parameters, but these are limited to
	 * String objects and we need to pass more complicated objects. The only
	 * other way to do this is in the session. The bad thing about using the
	 * session is that we have no way of knowing when we are done re-rendering
	 * the request and so we don't know when we can remove the objects from
	 * the session. So we will end up polluting the session with old objects
	 * when we finally leave the render of this controller and move on to
	 * somthing else. To minimize the pollution, we will use a static string
	 * value as the session attribute name. At least this way we are only ever
	 * leaving one orphaned set behind. The methods that return these names
	 * can be overridden if you want to use a different method, but be aware
	 * of the session pollution that may occur.
	 */
	private static final String RENDER_COMMAND_SESSION_ATTRIBUTE =
			"org.springframework.web.portlet.mvc.RenderCommand";

	private static final String RENDER_ERRORS_SESSION_ATTRIBUTE =
			"org.springframework.web.portlet.mvc.RenderErrors";

	public static final String DEFAULT_COMMAND_NAME = "command";


	private String commandName = DEFAULT_COMMAND_NAME;

	private Class commandClass;

	private Validator[] validators;

	private boolean validateOnBinding = true;

	private MessageCodesResolver messageCodesResolver;

	private BindingErrorProcessor bindingErrorProcessor;

	private PropertyEditorRegistrar[] propertyEditorRegistrars;

	private WebBindingInitializer webBindingInitializer;


	/**
	 * Set the name of the command in the model.
	 * The command object will be included in the model under this name.
	 */
	public final void setCommandName(String commandName) {
		this.commandName = commandName;
	}

	/**
	 * Return the name of the command in the model.
	 */
	public final String getCommandName() {
		return this.commandName;
	}

	/**
	 * Set the command class for this controller.
	 * An instance of this class gets populated and validated on each request.
	 */
	public final void setCommandClass(Class commandClass) {
		this.commandClass = commandClass;
	}

	/**
	 * Return the command class for this controller.
	 */
	public final Class getCommandClass() {
		return this.commandClass;
	}

	/**
	 * Set the primary Validator for this controller. The Validator
	 * must support the specified command class. If there are one
	 * or more existing validators set already when this method is
	 * called, only the specified validator will be kept. Use
	 * {@link #setValidators(Validator[])} to set multiple validators.
	 */
	public final void setValidator(Validator validator) {
		this.validators = new Validator[] {validator};
	}

	/**
	 * @return the primary Validator for this controller.
	 */
	public final Validator getValidator() {
		return (this.validators != null && this.validators.length > 0 ? this.validators[0] : null);
	}

	/**
	 * Set the Validators for this controller.
	 * The Validator must support the specified command class.
	 */
	public final void setValidators(Validator[] validators) {
		this.validators = validators;
	}

	/**
	 * Return the Validators for this controller.
	 */
	public final Validator[] getValidators() {
		return this.validators;
	}

	/**
	 * Set if the Validator should get applied when binding.
	 */
	public final void setValidateOnBinding(boolean validateOnBinding) {
		this.validateOnBinding = validateOnBinding;
	}

	/**
	 * Return if the Validator should get applied when binding.
	 */
	public final boolean isValidateOnBinding() {
		return this.validateOnBinding;
	}

	/**
	 * Set the strategy to use for resolving errors into message codes.
	 * Applies the given strategy to all data binders used by this controller.
	 * <p>Default is {@code null}, i.e. using the default strategy of the data binder.
	 * @see #createBinder
	 * @see org.springframework.validation.DataBinder#setMessageCodesResolver
	 */
	public final void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
		this.messageCodesResolver = messageCodesResolver;
	}

	/**
	 * Return the strategy to use for resolving errors into message codes (if any).
	 */
	public final MessageCodesResolver getMessageCodesResolver() {
		return this.messageCodesResolver;
	}

	/**
	 * Set the strategy to use for processing binding errors, that is,
	 * required field errors and {@code PropertyAccessException}s.
	 * <p>Default is {@code null}, i.e. using the default strategy of
	 * the data binder.
	 * @see #createBinder
	 * @see org.springframework.validation.DataBinder#setBindingErrorProcessor
	 */
	public final void setBindingErrorProcessor(BindingErrorProcessor bindingErrorProcessor) {
		this.bindingErrorProcessor = bindingErrorProcessor;
	}

	/**
	 * Return the strategy to use for processing binding errors (if any).
	 */
	public final BindingErrorProcessor getBindingErrorProcessor() {
		return this.bindingErrorProcessor;
	}

	/**
	 * Specify a single PropertyEditorRegistrar to be applied
	 * to every DataBinder that this controller uses.
	 * <p>Allows for factoring out the registration of PropertyEditors
	 * to separate objects, as an alternative to {@code initBinder}.
	 * @see #initBinder
	 */
	public final void setPropertyEditorRegistrar(PropertyEditorRegistrar propertyEditorRegistrar) {
		this.propertyEditorRegistrars = new PropertyEditorRegistrar[] {propertyEditorRegistrar};
	}

	/**
	 * Specify one or more PropertyEditorRegistrars to be applied
	 * to every DataBinder that this controller uses.
	 * <p>Allows for factoring out the registration of PropertyEditors
	 * to separate objects, as alternative to {@code initBinder}.
	 * @see #initBinder
	 */
	public final void setPropertyEditorRegistrars(PropertyEditorRegistrar[] propertyEditorRegistrars) {
		this.propertyEditorRegistrars = propertyEditorRegistrars;
	}

	/**
	 * Return the PropertyEditorRegistrars (if any) to be applied
	 * to every DataBinder that this controller uses.
	 */
	public final PropertyEditorRegistrar[] getPropertyEditorRegistrars() {
		return this.propertyEditorRegistrars;
	}

	/**
	 * Specify a WebBindingInitializer which will apply pre-configured
	 * configuration to every DataBinder that this controller uses.
	 * <p>Allows for factoring out the entire binder configuration
	 * to separate objects, as an alternative to {@link #initBinder}.
	 */
	public final void setWebBindingInitializer(WebBindingInitializer webBindingInitializer) {
		this.webBindingInitializer = webBindingInitializer;
	}

	/**
	 * Return the WebBindingInitializer (if any) which will apply pre-configured
	 * configuration to every DataBinder that this controller uses.
	 */
	public final WebBindingInitializer getWebBindingInitializer() {
		return this.webBindingInitializer;
	}


	@Override
	protected void initApplicationContext() {
		if (this.validators != null) {
			for (int i = 0; i < this.validators.length; i++) {
				if (this.commandClass != null && !this.validators[i].supports(this.commandClass))
					throw new IllegalArgumentException("Validator [" + this.validators[i] +
							"] does not support command class [" +
							this.commandClass.getName() + "]");
			}
		}
	}


	/**
	 * Retrieve a command object for the given request.
	 * <p>The default implementation calls {@link #createCommand()}.
	 * Subclasses can override this.
	 * @param request current portlet request
	 * @return object command to bind onto
	 * @see #createCommand
	 */
	protected Object getCommand(PortletRequest request) throws Exception {
		return createCommand();
	}

	/**
	 * Create a new command instance for the command class of this controller.
	 * <p>This implementation uses {@code BeanUtils.instantiateClass},
	 * so the command needs to have a no-arg constructor (supposed to be
	 * public, but not required to).
	 * @return the new command instance
	 * @throws Exception if the command object could not be instantiated
	 * @see org.springframework.beans.BeanUtils#instantiateClass(Class)
	 */
	protected final Object createCommand() throws Exception {
		if (this.commandClass == null) {
			throw new IllegalStateException("Cannot create command without commandClass being set - " +
					"either set commandClass or (in a form controller) override formBackingObject");
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Creating new command of class [" + this.commandClass.getName() + "]");
		}
		return BeanUtils.instantiateClass(this.commandClass);
	}

	/**
	 * Check if the given command object is a valid for this controller,
	 * i.e. its command class.
	 * @param command the command object to check
	 * @return if the command object is valid for this controller
	 */
	protected final boolean checkCommand(Object command) {
		return (this.commandClass == null || this.commandClass.isInstance(command));
	}


	/**
	 * Bind the parameters of the given request to the given command object.
	 * @param request current portlet request
	 * @param command the command to bind onto
	 * @return the PortletRequestDataBinder instance for additional custom validation
	 * @throws Exception in case of invalid state or arguments
	 */
	protected final PortletRequestDataBinder bindAndValidate(PortletRequest request, Object command)
			throws Exception {

		PortletRequestDataBinder binder = createBinder(request, command);
		if (!suppressBinding(request)) {
			binder.bind(request);
			BindException errors = new BindException(binder.getBindingResult());
			onBind(request, command, errors);
			if (this.validators != null && isValidateOnBinding() && !suppressValidation(request)) {
				for (int i = 0; i < this.validators.length; i++) {
					ValidationUtils.invokeValidator(this.validators[i], command, errors);
				}
			}
			onBindAndValidate(request, command, errors);
		}
		return binder;
	}

	/**
	 * Return whether to suppress binding for the given request.
	 * <p>The default implementation always returns {@code false}.
	 * Can be overridden in subclasses to suppress validation:
	 * for example, if a special request parameter is set.
	 * @param request current portlet request
	 * @return whether to suppress binding for the given request
	 * @see #suppressValidation
	 */
	protected boolean suppressBinding(PortletRequest request) {
		return false;
	}

	/**
	 * Create a new binder instance for the given command and request.
	 * <p>Called by {@code bindAndValidate}. Can be overridden to plug in
	 * custom PortletRequestDataBinder instances.
	 * <p>The default implementation creates a standard PortletRequestDataBinder and
	 * invokes {@code prepareBinder} and {@code initBinder}.
	 * <p>Note that neither {@code prepareBinder} nor {@code initBinder}
	 * will be invoked automatically if you override this method! Call those methods
	 * at appropriate points of your overridden method.
	 * @param request current portlet request
	 * @param command the command to bind onto
	 * @return the new binder instance
	 * @throws Exception in case of invalid state or arguments
	 * @see #bindAndValidate
	 * @see #prepareBinder
	 * @see #initBinder
	 */
	protected PortletRequestDataBinder createBinder(PortletRequest request, Object command)
			throws Exception {

		PortletRequestDataBinder binder = new PortletRequestDataBinder(command, getCommandName());
		prepareBinder(binder);
		initBinder(request, binder);
		return binder;
	}

	/**
	 * Prepare the given binder, applying the specified MessageCodesResolver,
	 * BindingErrorProcessor and PropertyEditorRegistrars (if any).
	 * Called by {@code createBinder}.
	 * @param binder the new binder instance
	 * @see #createBinder
	 * @see #setMessageCodesResolver
	 * @see #setBindingErrorProcessor
	 */
	protected final void prepareBinder(PortletRequestDataBinder binder) {
		if (useDirectFieldAccess()) {
			binder.initDirectFieldAccess();
		}
		if (this.messageCodesResolver != null) {
			binder.setMessageCodesResolver(this.messageCodesResolver);
		}
		if (this.bindingErrorProcessor != null) {
			binder.setBindingErrorProcessor(this.bindingErrorProcessor);
		}
		if (this.propertyEditorRegistrars != null) {
			for (int i = 0; i < this.propertyEditorRegistrars.length; i++) {
				this.propertyEditorRegistrars[i].registerCustomEditors(binder);
			}
		}
	}

	/**
	 * Determine whether to use direct field access instead of bean property access.
	 * Applied by {@code prepareBinder}.
	 * <p>The default is {@code false}. Can be overridden in subclasses.
	 * @see #prepareBinder
	 * @see org.springframework.validation.DataBinder#initDirectFieldAccess()
	 */
	protected boolean useDirectFieldAccess() {
		return false;
	}

	/**
	 * Initialize the given binder instance, for example with custom editors.
	 * Called by {@code createBinder}.
	 * <p>This method allows you to register custom editors for certain fields of your
	 * command class. For instance, you will be able to transform Date objects into a
	 * String pattern and back, in order to allow your JavaBeans to have Date properties
	 * and still be able to set and display them in an HTML interface.
	 * <p>The default implementation is empty.
	 * @param request current portlet request
	 * @param binder new binder instance
	 * @throws Exception in case of invalid state or arguments
	 * @see #createBinder
	 * @see org.springframework.validation.DataBinder#registerCustomEditor
	 * @see org.springframework.beans.propertyeditors.CustomDateEditor
	 */
	protected void initBinder(PortletRequest request, PortletRequestDataBinder binder) throws Exception {
		if (this.webBindingInitializer != null) {
			this.webBindingInitializer.initBinder(binder, new PortletWebRequest(request));
		}
	}

	/**
	 * Callback for custom post-processing in terms of binding.
	 * Called on each submit, after standard binding but before validation.
	 * <p>The default implementation delegates to {@code onBind(request, command)}.
	 * @param request current portlet request
	 * @param command the command object to perform further binding on
	 * @param errors validation errors holder, allowing for additional
	 * custom registration of binding errors
	 * @throws Exception in case of invalid state or arguments
	 * @see #bindAndValidate
	 * @see #onBind(PortletRequest, Object)
	 */
	protected void onBind(PortletRequest request, Object command, BindException errors) throws Exception {
		onBind(request, command);
	}

	/**
	 * Callback for custom post-processing in terms of binding.
	 * Called by the default implementation of the {@code onBind} version with
	 * all parameters, after standard binding but before validation.
	 * <p>The default implementation is empty.
	 * @param request current portlet request
	 * @param command the command object to perform further binding on
	 * @throws Exception in case of invalid state or arguments
	 * @see #onBind(PortletRequest, Object, BindException)
	 */
	protected void onBind(PortletRequest request, Object command) throws Exception {
	}

	/**
	 * Return whether to suppress validation for the given request.
	 * <p>The default implementation always returns {@code false}.
	 * Can be overridden in subclasses to suppress validation:
	 * for example, if a special request parameter is set.
	 * @param request current portlet request
	 * @return whether to suppress validation for the given request
	 */
	protected boolean suppressValidation(PortletRequest request) {
		return false;
	}

	/**
	 * Callback for custom post-processing in terms of binding and validation.
	 * Called on each submit, after standard binding and validation,
	 * but before error evaluation.
	 * <p>The default implementation is empty.
	 * @param request current portlet request
	 * @param command the command object, still allowing for further binding
	 * @param errors validation errors holder, allowing for additional
	 * custom validation
	 * @throws Exception in case of invalid state or arguments
	 * @see #bindAndValidate
	 * @see org.springframework.validation.Errors
	 */
	protected void onBindAndValidate(PortletRequest request, Object command, BindException errors)
			throws Exception {
	}


	/**
	 * Return the name of the session attribute that holds
	 * the render phase command object for this form controller.
	 * @return the name of the render phase command object session attribute
	 * @see javax.portlet.PortletSession#getAttribute
	 */
	protected String getRenderCommandSessionAttributeName() {
		return RENDER_COMMAND_SESSION_ATTRIBUTE;
	}

	/**
	 * Return the name of the session attribute that holds
	 * the render phase command object for this form controller.
	 * @return the name of the render phase command object session attribute
	 * @see javax.portlet.PortletSession#getAttribute
	 */
	protected String getRenderErrorsSessionAttributeName() {
		return RENDER_ERRORS_SESSION_ATTRIBUTE;
	}

	/**
	 * Get the command object cached for the render phase.
	 * @see #getRenderErrors
	 * @see #getRenderCommandSessionAttributeName
	 * @see #setRenderCommandAndErrors
	 */
	protected final Object getRenderCommand(RenderRequest request) throws PortletException {
		PortletSession session = request.getPortletSession(false);
		if (session == null) {
			throw new PortletSessionRequiredException("Could not obtain portlet session");
		}
		Object command = session.getAttribute(getRenderCommandSessionAttributeName());
		if (command == null) {
			throw new PortletSessionRequiredException("Could not obtain command object from portlet session");
		}
		return command;
	}

	/**
	 * Get the bind and validation errors cached for the render phase.
	 * @see #getRenderCommand
	 * @see #getRenderErrorsSessionAttributeName
	 * @see #setRenderCommandAndErrors
	 */
	protected final BindException getRenderErrors(RenderRequest request) throws PortletException {
		PortletSession session = request.getPortletSession(false);
		if (session == null) {
			throw new PortletSessionRequiredException("Could not obtain portlet session");
		}
		BindException errors = (BindException) session.getAttribute(getRenderErrorsSessionAttributeName());
		if (errors == null) {
			throw new PortletSessionRequiredException("Could not obtain errors object from portlet session");
		}
		return errors;
	}

	/**
	 * Set the command object and errors object for the render phase.
	 * @param request the current action request
	 * @param command the command object to preserve for the render phase
	 * @param errors the errors from binding and validation to preserve for the render phase
	 * @see #getRenderCommand
	 * @see #getRenderErrors
	 * @see #getRenderCommandSessionAttributeName
	 * @see #getRenderErrorsSessionAttributeName
	 */
	protected final void setRenderCommandAndErrors(
			ActionRequest request, Object command, BindException errors) throws Exception {

		logger.debug("Storing command and error objects in session for render phase");
		PortletSession session = request.getPortletSession();
		session.setAttribute(getRenderCommandSessionAttributeName(), command);
		session.setAttribute(getRenderErrorsSessionAttributeName(), errors);
	}

}
