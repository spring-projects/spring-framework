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

package org.springframework.web.servlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.util.NestedServletException;

/**
 * ViewRendererServlet is a bridge servlet, mainly for the Portlet MVC support.
 *
 * <p>For usage with Portlets, this Servlet is necessary to force the portlet container
 * to convert the PortletRequest to a ServletRequest, which it has to do when
 * including a resource via the PortletRequestDispatcher. This allows for reuse
 * of the entire Servlet-based View support even in a Portlet environment.
 *
 * <p>The actual mapping of the bridge servlet is configurable in the DispatcherPortlet,
 * via a "viewRendererUrl" property. The default is "/WEB-INF/servlet/view", which is
 * just available for internal resource dispatching.
 *
 * @author William G. Thompson, Jr.
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ViewRendererServlet extends HttpServlet {

	/**
	 * Request attribute to hold current web application context.
	 * Otherwise only the global web app context is obtainable by tags etc.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getWebApplicationContext
	 */
	public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE;

	/** Name of request attribute that holds the View object */
	public static final String VIEW_ATTRIBUTE = ViewRendererServlet.class.getName() + ".VIEW";

	/** Name of request attribute that holds the model Map */
	public static final String MODEL_ATTRIBUTE = ViewRendererServlet.class.getName() + ".MODEL";


	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	@Override
	protected final void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * Process this request, handling exceptions.
	 * The actually event handling is performed by the abstract
	 * {@code renderView()} template method.
	 * @see #renderView
	 */
	protected final void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		try {
			renderView(request, response);
		}
		catch (ServletException ex) {
			throw ex;
		}
		catch (IOException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new NestedServletException("View rendering failed", ex);
		}
	}

	/**
	 * Retrieve the View instance and model Map to render
	 * and trigger actual rendering.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception in case of any kind of processing failure
	 * @see org.springframework.web.servlet.View#render
	 */
	@SuppressWarnings("unchecked")
	protected void renderView(HttpServletRequest request, HttpServletResponse response) throws Exception {
		View view = (View) request.getAttribute(VIEW_ATTRIBUTE);
		if (view == null) {
			throw new ServletException("Could not complete render request: View is null");
		}
		Map<String, Object> model = (Map<String, Object>) request.getAttribute(MODEL_ATTRIBUTE);
		view.render(model, request, response);
	}

}
