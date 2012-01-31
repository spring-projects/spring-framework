/*
 * Copyright 2002-2010 the original author or authors.
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
import javax.portlet.PortletException;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;

import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.handler.PortletContentGenerator;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * Convenient superclass for controller implementations, using the Template
 * Method design pattern.
 *
 * <p>As stated in the {@link Controller Controller}
 * interface, a lot of functionality is already provided by certain abstract
 * base controllers. The AbstractController is one of the most important
 * abstract base controller providing basic features such controlling if a
 * session is required and render caching.
 *
 * <p><b><a name="workflow">Workflow
 * (<a href="Controller.html#workflow">and that defined by interface</a>):</b><br>
 * <ol>
 *  <li>If this is an action request, {@link #handleActionRequest handleActionRequest}
 *      will be called by the DispatcherPortlet once to perform the action defined by this
 *      controller.</li>
 *  <li>If a session is required, try to get it (PortletException if not found).</li>
 *  <li>Call method {@link #handleActionRequestInternal handleActionRequestInternal},
 *      (optionally synchronizing around the call on the PortletSession),
 *      which should be overridden by extending classes to provide actual functionality to
 *      perform the desired action of the controller.  This will be executed only once.</li>
 *  <li>For a straight render request, or the render phase of an action request (assuming the
 *      same controller is called for the render phase -- see tip below),
 *      {@link #handleRenderRequest handleRenderRequest} will be called by the DispatcherPortlet
 *      repeatedly to render the display defined by this controller.</li>
 *  <li>If a session is required, try to get it (PortletException if none found).</li>
 *  <li>It will control caching as defined by the cacheSeconds property.</li>
 *  <li>Call method {@link #handleRenderRequestInternal handleRenderRequestInternal},
 *      (optionally synchronizing around the call on the PortletSession),
 *      which should be overridden by extending classes to provide actual functionality to
 *      return {@link org.springframework.web.portlet.ModelAndView ModelAndView} objects.
 *      This will be executed repeatedly as the portal updates the current displayed page.</li>
 * </ol>
 *
 * <p><b><a name="config">Exposed configuration properties</a>
 * (<a href="Controller.html#config">and those defined by interface</a>):</b><br>
 * <table border="1">
 *  <tr>
 *      <td><b>name</b></th>
 *      <td><b>default</b></td>
 *      <td><b>description</b></td>
 *  </tr>
 *  <tr>
 *      <td>requireSession</td>
 *      <td>false</td>
 *      <td>whether a session should be required for requests to be able to
 *          be handled by this controller. This ensures, derived controller
 *          can - without fear of Nullpointers - call request.getSession() to
 *          retrieve a session. If no session can be found while processing
 *          the request, a PortletException will be thrown</td>
 *  </tr>
 *  <tr>
 *      <td>synchronizeOnSession</td>
 *      <td>false</td>
 *      <td>whether the calls to <code>handleRenderRequestInternal</code> and
 *          <code>handleRenderRequestInternal</code> should be
 *          synchronized around the PortletSession, to serialize invocations
 *          from the same client. No effect if there is no PortletSession.
 *      </td>
 *  </tr>
 *  <tr>
 *      <td>cacheSeconds</td>
 *      <td>-1</td>
 *      <td>indicates the amount of seconds to specify caching is allowed in
 *          the render response generatedby  this request. 0 (zero) will indicate
 *          no caching is allowed at all, -1 (the default) will not override the
 *          portlet configuration and any positive number will cause the render
 *          response to declare the amount indicated as seconds to cache the content</td>
 *  </tr>
 *  <tr>
 *      <td>renderWhenMinimized</td>
 *      <td>false</td>
 *      <td>whether should be rendered when the portlet is in a minimized state --
 *          will return null for the ModelandView when the portlet is minimized
 *          and this is false</td>
 *  </tr>
 * </table>
 *
 * <p><b>TIP:</b> The controller mapping will be run twice by the PortletDispatcher for
 * action requests -- once for the action phase and again for the render phase.  You can
 * reach the render phase of a different controller by simply changing the values for the
 * criteria your mapping is using, such as portlet mode or a request parameter, during the
 * action phase of your controller.  This is very handy since redirects within the portlet
 * are apparently impossible.  Before doing this, it is usually wise to call
 * <code>clearAllRenderParameters</code> and then explicitly set all the parameters that
 * you want the new controller to see.  This avoids unexpected parameters from being passed
 * to the render phase of the second controller, such as the parameter indicating a form
 * submit ocurred in an <code>AbstractFormController</code>.
 *
 * <p>Thanks to Rainer Schmitz and Nick Lothian for their suggestions!
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 * @see ResourceAwareController
 * @see EventAwareController
 */
public abstract class AbstractController extends PortletContentGenerator implements Controller {

	private boolean synchronizeOnSession = false;

	private boolean renderWhenMinimized = false;


	/**
	 * Set if controller execution should be synchronized on the session,
	 * to serialize parallel invocations from the same client.
	 * <p>More specifically, the execution of the <code>handleActionRequestInternal</code>
	 * method will get synchronized if this flag is "true". The best available
	 * session mutex will be used for the synchronization; ideally, this will
	 * be a mutex exposed by HttpSessionMutexListener.
	 * <p>The session mutex is guaranteed to be the same object during
	 * the entire lifetime of the session, available under the key defined
	 * by the <code>SESSION_MUTEX_ATTRIBUTE</code> constant. It serves as a
	 * safe reference to synchronize on for locking on the current session.
	 * <p>In many cases, the PortletSession reference itself is a safe mutex
	 * as well, since it will always be the same object reference for the
	 * same active logical session. However, this is not guaranteed across
	 * different servlet containers; the only 100% safe way is a session mutex.
	 * @see #handleActionRequestInternal
	 * @see org.springframework.web.util.HttpSessionMutexListener
	 * @see org.springframework.web.portlet.util.PortletUtils#getSessionMutex(javax.portlet.PortletSession)
	 */
	public final void setSynchronizeOnSession(boolean synchronizeOnSession) {
		this.synchronizeOnSession = synchronizeOnSession;
	}

	/**
	 * Return whether controller execution should be synchronized on the session.
	 */
	public final boolean isSynchronizeOnSession() {
		return this.synchronizeOnSession;
	}

	/**
	 * Set if the controller should render an view when the portlet is in
	 * a minimized window.  The default is false.
	 * @see javax.portlet.RenderRequest#getWindowState
	 * @see javax.portlet.WindowState#MINIMIZED
	 */
	public final void setRenderWhenMinimized(boolean renderWhenMinimized) {
		this.renderWhenMinimized = renderWhenMinimized;
	}

	/**
	 * Return whether controller will render when portlet is minimized.
	 */
	public final boolean isRenderWhenMinimized() {
		return this.renderWhenMinimized;
	}


	public void handleActionRequest(ActionRequest request, ActionResponse response) throws Exception {
		// Delegate to PortletContentGenerator for checking and preparing.
		check(request, response);

		// Execute in synchronized block if required.
		if (this.synchronizeOnSession) {
			PortletSession session = request.getPortletSession(false);
			if (session != null) {
				Object mutex = PortletUtils.getSessionMutex(session);
				synchronized (mutex) {
					handleActionRequestInternal(request, response);
					return;
				}
			}
		}

		handleActionRequestInternal(request, response);
	}

	public ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) throws Exception {
		// If the portlet is minimized and we don't want to render then return null.
		if (WindowState.MINIMIZED.equals(request.getWindowState()) && !this.renderWhenMinimized) {
			return null;
		}
	    
		// Delegate to PortletContentGenerator for checking and preparing.
		checkAndPrepare(request, response);

		// Execute in synchronized block if required.
		if (this.synchronizeOnSession) {
			PortletSession session = request.getPortletSession(false);
			if (session != null) {
				Object mutex = PortletUtils.getSessionMutex(session);
				synchronized (mutex) {
					return handleRenderRequestInternal(request, response);
				}
			}
		}

		return handleRenderRequestInternal(request, response);
	}


	/**
	 * Subclasses are meant to override this method if the controller
	 * is expected to handle action requests. The contract is the same as
	 * for <code>handleActionRequest</code>.
	 * <p>The default implementation throws a PortletException.
	 * @see #handleActionRequest
	 * @see #handleRenderRequestInternal
	 */
	protected void handleActionRequestInternal(ActionRequest request, ActionResponse response)
			throws Exception {

		throw new PortletException("[" + getClass().getName() + "] does not handle action requests");
	}

	/**
	 * Subclasses are meant to override this method if the controller
	 * is expected to handle render requests. The contract is the same as
	 * for <code>handleRenderRequest</code>.
	 * <p>The default implementation throws a PortletException.
	 * @see #handleRenderRequest
	 * @see #handleActionRequestInternal
	 */
	protected ModelAndView handleRenderRequestInternal(RenderRequest request, RenderResponse response)
			throws Exception {

		throw new PortletException("[" + getClass().getName() + "] does not handle render requests");
	}

}
