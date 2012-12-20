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
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.springframework.validation.BindException;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.bind.PortletRequestDataBinder;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * <p>Abstract base class for custom command controllers. Autopopulates a
 * command bean from the request. For command validation, a validator
 * (property inherited from BaseCommandController) can be used.</p>
 *
 * <p>This command controller should preferrable not be used to handle form
 * submission, because functionality for forms is more offered in more
 * detail by the {@link org.springframework.web.portlet.mvc.AbstractFormController
 * AbstractFormController} and its corresponding implementations.</p>
 *
 * <p><b><a name="config">Exposed configuration properties</a>
 * (<a href="BaseCommandController.html#config">and those defined by superclass</a>):</b><br>
 * <i>none</i> (so only those available in superclass).</p>
 *
 * <p><b><a name="workflow">Workflow
 * (<a name="BaseCommandController.html#workflow">and that defined by superclass</a>):</b><br>
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setCommandClass
 * @see #setCommandName
 * @see #setValidator
 * @deprecated as of Spring 3.0, in favor of annotated controllers
 */
@Deprecated
public abstract class AbstractCommandController extends BaseCommandController {

	/**
	 * This render parameter is used to indicate forward to the render phase
	 * that a valid command (and errors) object is in the session.
	 */
	private static final String COMMAND_IN_SESSION_PARAMETER = "command-in-session";

	private static final String TRUE = Boolean.TRUE.toString();


	/**
	 * Create a new AbstractCommandController.
	 */
	public AbstractCommandController() {
	}

	/**
	 * Create a new AbstractCommandController.
	 * @param commandClass class of the command bean
	 */
	public AbstractCommandController(Class<?> commandClass) {
		setCommandClass(commandClass);
	}

	/**
	 * Create a new AbstractCommandController.
	 * @param commandClass class of the command bean
	 * @param commandName name of the command bean
	 */
	public AbstractCommandController(Class<?> commandClass, String commandName) {
		setCommandClass(commandClass);
		setCommandName(commandName);
	}


	@Override
	protected final void handleActionRequestInternal(ActionRequest request, ActionResponse response)
			throws Exception {

		// Create the command object.
		Object command = getCommand(request);

		// Compute the errors object.
		PortletRequestDataBinder binder = bindAndValidate(request, command);
		BindException errors = new BindException(binder.getBindingResult());

		// Actually handle the action.
		handleAction(request, response, command, errors);

		// Pass the command and errors forward to the render phase.
		setRenderCommandAndErrors(request, command, errors);
		setCommandInSession(response);
	}

	@Override
	protected final ModelAndView handleRenderRequestInternal(
			RenderRequest request, RenderResponse response) throws Exception {

		Object command = null;
		BindException errors = null;

		// Get the command and errors objects from the session, if they exist.
		if (isCommandInSession(request)) {
			logger.debug("Render phase obtaining command and errors objects from session");
			command = getRenderCommand(request);
			errors = getRenderErrors(request);
		}
		else {
			logger.debug("Render phase creating new command and errors objects");
			command = getCommand(request);
			PortletRequestDataBinder binder = bindAndValidate(request, command);
			errors = new BindException(binder.getBindingResult());
		}

		return handleRender(request, response, command, errors);
	}


	/**
	 * Template method for request handling, providing a populated and validated instance
	 * of the command class, and an Errors object containing binding and validation errors.
	 * <p>Call <code>errors.getModel()</code> to populate the ModelAndView model
	 * with the command and the Errors instance, under the specified command name,
	 * as expected by the "spring:bind" tag.
	 * @param request current action request
	 * @param response current action response
	 * @param command the populated command object
	 * @param errors validation errors holder
	 * @see org.springframework.validation.Errors
	 * @see org.springframework.validation.BindException#getModel
	 */
	protected abstract void handleAction(
			ActionRequest request, ActionResponse response, Object command, BindException errors)
			throws Exception;

	/**
	 * Template method for render request handling, providing a populated and validated instance
	 * of the command class, and an Errors object containing binding and validation errors.
	 * <p>Call <code>errors.getModel()</code> to populate the ModelAndView model
	 * with the command and the Errors instance, under the specified command name,
	 * as expected by the "spring:bind" tag.
	 * @param request current render request
	 * @param response current render response
	 * @param command the populated command object
	 * @param errors validation errors holder
	 * @return a ModelAndView to render, or null if handled directly
	 * @see org.springframework.validation.Errors
	 * @see org.springframework.validation.BindException#getModel
	 */
	protected abstract ModelAndView handleRender(
			RenderRequest request, RenderResponse response, Object command, BindException errors)
			throws Exception;


	/**
	 * Return the name of the render parameter that indicates there
	 * is a valid command (and errors) object in the session.
	 * @return the name of the render parameter
	 * @see javax.portlet.RenderRequest#getParameter
	 */
	protected String getCommandInSessionParameterName() {
		return COMMAND_IN_SESSION_PARAMETER;
	}

	/**
	 * Set the action response parameter that indicates there is a
	 * command (and errors) object in the session for the render phase.
	 * @param response the current action response
	 * @see #getCommandInSessionParameterName
	 * @see #isCommandInSession
	 */
	protected final void setCommandInSession(ActionResponse response) {
		if (logger.isDebugEnabled()) {
			logger.debug("Setting render parameter [" + getCommandInSessionParameterName() +
					"] to indicate a valid command (and errors) object are in the session");
		}
		try {
			response.setRenderParameter(getCommandInSessionParameterName(), TRUE);
		}
		catch (IllegalStateException ex) {
			// Ignore in case sendRedirect was already set.
		}
	}

	/**
	 * Determine if there is a valid command (and errors) object in the
	 * session for this render request.
	 * @param request current render request
	 * @return if there is a valid command object in the session
	 * @see #getCommandInSessionParameterName
	 * @see #setCommandInSession
	 */
	protected boolean isCommandInSession(RenderRequest request) {
		return (TRUE.equals(request.getParameter(getCommandInSessionParameterName())) &&
				PortletUtils.getSessionAttribute(request, getRenderCommandSessionAttributeName()) != null);
	}

}
