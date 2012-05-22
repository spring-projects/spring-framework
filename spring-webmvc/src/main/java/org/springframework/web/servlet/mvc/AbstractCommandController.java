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
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;

/**
 * Abstract base class for custom command controllers.
 *
 * <p>Autopopulates a command bean from the request. For command validation,
 * a validator (property inherited from {@link BaseCommandController}) can be
 * used.
 *
 * <p>In most cases this command controller should not be used to handle form
 * submission, because functionality for forms is offered in more detail by the
 * {@link org.springframework.web.servlet.mvc.AbstractFormController} and its
 * corresponding implementations.
 *
 * <p><b><a name="config">Exposed configuration properties</a>
 * (<a href="BaseCommandController.html#config">and those defined by superclass</a>):</b><br>
 * <i>none</i> (so only those available in superclass).</p>
 *
 * <p><b><a name="workflow">Workflow
 * (<a name="BaseCommandController.html#workflow">and that defined by superclass</a>):</b><br>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setCommandClass
 * @see #setCommandName
 * @see #setValidator
 * @deprecated as of Spring 3.0, in favor of annotated controllers
 */
@Deprecated
public abstract class AbstractCommandController extends BaseCommandController {

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
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		Object command = getCommand(request);
		ServletRequestDataBinder binder = bindAndValidate(request, command);
		BindException errors = new BindException(binder.getBindingResult());
		return handle(request, response, command, errors);
	}

	/**
	 * Template method for request handling, providing a populated and validated instance
	 * of the command class, and an Errors object containing binding and validation errors.
	 * <p>Call <code>errors.getModel()</code> to populate the ModelAndView model
	 * with the command and the Errors instance, under the specified command name,
	 * as expected by the "spring:bind" tag.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param command the populated command object
	 * @param errors validation errors holder
	 * @return a ModelAndView to render, or <code>null</code> if handled directly
	 * @see org.springframework.validation.Errors
	 * @see org.springframework.validation.BindException#getModel
	 */
	protected abstract ModelAndView handle(
			HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception;

}
