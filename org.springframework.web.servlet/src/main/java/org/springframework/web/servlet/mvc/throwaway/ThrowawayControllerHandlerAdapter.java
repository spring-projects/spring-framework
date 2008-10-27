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

package org.springframework.web.servlet.mvc.throwaway;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

/**
 * Adapter to use the ThrowawayController workflow interface with the
 * generic DispatcherServlet. Does not support last-modified checks.
 *
 * <p>This is an SPI class, not used directly by application code.
 * It can be explicitly configured in a DispatcherServlet context, to use a
 * customized version instead of the default ThrowawayControllerHandlerAdapter.
 *
 * @author Juergen Hoeller
 * @since 08.12.2003
 * @deprecated as of Spring 2.5, in favor of annotation-based controllers.
 * To be removed in Spring 3.0.
 */
public class ThrowawayControllerHandlerAdapter implements HandlerAdapter {

	public static final String DEFAULT_COMMAND_NAME = "throwawayController";

	private String commandName = DEFAULT_COMMAND_NAME;


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


	public boolean supports(Object handler) {
		return (handler instanceof ThrowawayController);
	}


	/**
	 * This implementation binds request parameters to the ThrowawayController
	 * instance and then calls <code>execute</code> on it.
	 * @see #createBinder
	 * @see ThrowawayController#execute
	 */
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		ThrowawayController throwaway = (ThrowawayController) handler;

		ServletRequestDataBinder binder = createBinder(request, throwaway);
		binder.bind(request);
		binder.closeNoCatch();

		return throwaway.execute();
	}

	/**
	 * Create a new binder instance for the given command and request.
	 * <p>Called by <code>bindAndValidate</code>. Can be overridden to plug in
	 * custom ServletRequestDataBinder subclasses.
	 * <p>Default implementation creates a standard ServletRequestDataBinder,
	 * sets the specified MessageCodesResolver (if any), and invokes initBinder.
	 * Note that <code>initBinder</code> will not be invoked if you override this method!
	 * @param request current HTTP request
	 * @param command the command to bind onto
	 * @return the new binder instance
	 * @throws Exception in case of invalid state or arguments
	 * @see #initBinder
	 * @see #getCommandName
	 */
	protected ServletRequestDataBinder createBinder(HttpServletRequest request, ThrowawayController command)
	    throws Exception {

		ServletRequestDataBinder binder = new ServletRequestDataBinder(command, getCommandName());
		initBinder(request, binder);
		return binder;
	}

	/**
	 * Initialize the given binder instance, for example with custom editors.
	 * Called by <code>createBinder</code>.
	 * <p>This method allows you to register custom editors for certain fields of your
	 * command class. For instance, you will be able to transform Date objects into a
	 * String pattern and back, in order to allow your JavaBeans to have Date properties
	 * and still be able to set and display them in an HTML interface.
	 * <p>Default implementation is empty.
	 * @param request current HTTP request
	 * @param binder new binder instance
	 * @throws Exception in case of invalid state or arguments
	 * @see #createBinder
	 * @see org.springframework.validation.DataBinder#registerCustomEditor
	 * @see org.springframework.beans.propertyeditors.CustomDateEditor
	 */
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder)
	    throws Exception {
	}


	/**
	 * This implementation always returns -1, as last-modified checks are not supported.
	 */
	public long getLastModified(HttpServletRequest request, Object handler) {
		return -1;
	}

}
