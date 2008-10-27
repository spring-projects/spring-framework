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

package org.springframework.web.servlet.view.velocity;

import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.ToolboxManager;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.servlet.ServletToolboxManager;
import org.apache.velocity.tools.view.tools.ViewTool;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link VelocityView} subclass which adds support for Velocity Tools toolboxes
 * and Velocity Tools ViewTool callbacks / Velocity Tools 1.3 init methods.
 *
 * <p>Specify a "toolboxConfigLocation", for example "/WEB-INF/toolbox.xml",
 * to automatically load a Velocity Tools toolbox definition file and expose
 * all defined tools in the specified scopes. If no config location is
 * specified, no toolbox will be loaded and exposed.
 *
 * <p>This view will always create a special Velocity context, namely an
 * instance of the ChainedContext class which is part of the view package
 * of Velocity tools. This allows to use tools from the view package of
 * Velocity Tools, like LinkTool, which need to be initialized with a special
 * context that implements the ViewContext interface (i.e. a ChainedContext).
 *
 * <p>This view also checks tools that are specified as "toolAttributes":
 * If they implement the ViewTool interface, they will get initialized with
 * the Velocity context. This allows tools from the view package of Velocity
 * Tools, such as LinkTool, to be defined as
 * {@link #setToolAttributes "toolAttributes"} on a VelocityToolboxView,
 * instead of in a separate toolbox XML file.
 *
 * <p>This is a separate class mainly to avoid a required dependency on
 * the view package of Velocity Tools in {@link VelocityView} itself.
 * As of Spring 2.0, this class requires Velocity Tools 1.2 or higher.
 *
 * @author Juergen Hoeller
 * @since 1.1.3
 * @see #setToolboxConfigLocation
 * @see #initTool
 * @see org.apache.velocity.tools.view.context.ViewContext
 * @see org.apache.velocity.tools.view.context.ChainedContext
 * @see org.apache.velocity.tools.view.tools.ViewTool
 * @see org.apache.velocity.tools.view.tools.LinkTool
 */
public class VelocityToolboxView extends VelocityView {

	private String toolboxConfigLocation;


	/**
	 * Set a Velocity Toolbox config location, for example "/WEB-INF/toolbox.xml",
	 * to automatically load a Velocity Tools toolbox definition file and expose
	 * all defined tools in the specified scopes. If no config location is
	 * specified, no toolbox will be loaded and exposed.
	 * <p>The specfied location string needs to refer to a ServletContext
	 * resource, as expected by ServletToolboxManager which is part of
	 * the view package of Velocity Tools.
	 * @see org.apache.velocity.tools.view.servlet.ServletToolboxManager#getInstance
	 */
	public void setToolboxConfigLocation(String toolboxConfigLocation) {
		this.toolboxConfigLocation = toolboxConfigLocation;
	}

	/**
	 * Return the Velocity Toolbox config location, if any.
	 */
	protected String getToolboxConfigLocation() {
		return this.toolboxConfigLocation;
	}


	/**
	 * Overridden to create a ChainedContext, which is part of the view package
	 * of Velocity Tools, as special context. ChainedContext is needed for
	 * initialization of ViewTool instances.
	 * @see #initTool
	 */
	protected Context createVelocityContext(
			Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Create a ChainedContext instance.
		ChainedContext velocityContext = new ChainedContext(
				new VelocityContext(model), getVelocityEngine(), request, response, getServletContext());

		// Load a Velocity Tools toolbox, if necessary.
		if (getToolboxConfigLocation() != null) {
			ToolboxManager toolboxManager = ServletToolboxManager.getInstance(
					getServletContext(), getToolboxConfigLocation());
			Map toolboxContext = toolboxManager.getToolbox(velocityContext);
			velocityContext.setToolbox(toolboxContext);
		}

		return velocityContext;
	}

	/**
	 * Overridden to check for the ViewContext interface which is part of the
	 * view package of Velocity Tools. This requires a special Velocity context,
	 * like ChainedContext as set up by {@link #createVelocityContext} in this class.
	 * @see org.apache.velocity.tools.view.tools.ViewTool#init(Object)
	 * @see org.apache.velocity.tools.view.tools.LinkTool#init(Object)
	 */
	protected void initTool(Object tool, Context velocityContext) throws Exception {
		// Initialize ViewTool instances with the Velocity context.
		// Despite having an "init(Object)" method, all known ViewTool
		// implementations expect a ViewContext implementation as argument.
		// ChainedContext implements the ViewContext interface.
		if (tool instanceof ViewTool) {
			// Velocity Tools 1.2: an actual ViewTool implementation.
			((ViewTool) tool).init(velocityContext);
		}
		else {
			// Velocity Tools 1.3: a class-level "init(Object)" method.
			Method initMethod =
					ClassUtils.getMethodIfAvailable(tool.getClass(), "init", new Class[] {Object.class});
			if (initMethod != null) {
				ReflectionUtils.invokeMethod(initMethod, tool, new Object[] {velocityContext});
			}
		}
	}

}
