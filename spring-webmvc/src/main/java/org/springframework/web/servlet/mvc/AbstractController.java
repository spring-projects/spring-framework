/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.WebUtils;

/**
 * Convenient superclass for controller implementations, using the Template Method
 * design pattern.
 *
 * <p><b>Workflow
 * (<a href="Controller.html#workflow">and that defined by interface</a>):</b><br>
 * <ol>
 * <li>{@link #handleRequest(HttpServletRequest, HttpServletResponse) handleRequest()}
 * will be called by the DispatcherServlet</li>
 * <li>Inspection of supported methods (ServletException if request method
 * is not support)</li>
 * <li>If session is required, try to get it (ServletException if not found)</li>
 * <li>Set caching headers if needed according to the cacheSeconds property</li>
 * <li>Call abstract method
 * {@link #handleRequestInternal(HttpServletRequest, HttpServletResponse) handleRequestInternal()}
 * (optionally synchronizing around the call on the HttpSession),
 * which should be implemented by extending classes to provide actual
 * functionality to return {@link org.springframework.web.servlet.ModelAndView ModelAndView} objects.</li>
 * </ol>
 *
 * <p><b><a name="config">Exposed configuration properties</a>
 * (<a href="Controller.html#config">and those defined by interface</a>):</b><br>
 * <table border="1">
 * <tr>
 * <td><b>name</b></td>
 * <td><b>default</b></td>
 * <td><b>description</b></td>
 * </tr>
 * <tr>
 * <td>supportedMethods</td>
 * <td>GET,POST</td>
 * <td>comma-separated (CSV) list of methods supported by this controller,
 * such as GET, POST and PUT</td>
 * </tr>
 * <tr>
 * <td>requireSession</td>
 * <td>false</td>
 * <td>whether a session should be required for requests to be able to
 * be handled by this controller. This ensures that derived controller
 * can - without fear of null pointers - call request.getSession() to
 * retrieve a session. If no session can be found while processing
 * the request, a ServletException will be thrown</td>
 * </tr>
 * <tr>
 * <td>cacheSeconds</td>
 * <td>-1</td>
 * <td>indicates the amount of seconds to include in the cache header
 * for the response following on this request. 0 (zero) will include
 * headers for no caching at all, -1 (the default) will not generate
 * <i>any headers</i> and any positive number will generate headers
 * that state the amount indicated as seconds to cache the content</td>
 * </tr>
 * <tr>
 * <td>synchronizeOnSession</td>
 * <td>false</td>
 * <td>whether the call to {@code handleRequestInternal} should be
 * synchronized around the HttpSession, to serialize invocations
 * from the same client. No effect if there is no HttpSession.
 * </td>
 * </tr>
 * </table>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see WebContentInterceptor
 */
public abstract class AbstractController extends WebContentGenerator implements Controller {

	private boolean synchronizeOnSession = false;


	/**
	 * Create a new AbstractController which supports
	 * HTTP methods GET, HEAD and POST by default.
	 */
	public AbstractController() {
		this(true);
	}

	/**
	 * Create a new AbstractController.
	 * @param restrictDefaultSupportedMethods {@code true} if this
	 * controller should support HTTP methods GET, HEAD and POST by default,
	 * or {@code false} if it should be unrestricted
	 * @since 4.3
	 */
	public AbstractController(boolean restrictDefaultSupportedMethods) {
		super(restrictDefaultSupportedMethods);
	}


	/**
	 * Set if controller execution should be synchronized on the session,
	 * to serialize parallel invocations from the same client.
	 * <p>More specifically, the execution of the {@code handleRequestInternal}
	 * method will get synchronized if this flag is "true". The best available
	 * session mutex will be used for the synchronization; ideally, this will
	 * be a mutex exposed by HttpSessionMutexListener.
	 * <p>The session mutex is guaranteed to be the same object during
	 * the entire lifetime of the session, available under the key defined
	 * by the {@code SESSION_MUTEX_ATTRIBUTE} constant. It serves as a
	 * safe reference to synchronize on for locking on the current session.
	 * <p>In many cases, the HttpSession reference itself is a safe mutex
	 * as well, since it will always be the same object reference for the
	 * same active logical session. However, this is not guaranteed across
	 * different servlet containers; the only 100% safe way is a session mutex.
	 * @see AbstractController#handleRequestInternal
	 * @see org.springframework.web.util.HttpSessionMutexListener
	 * @see org.springframework.web.util.WebUtils#getSessionMutex(jakarta.servlet.http.HttpSession)
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


	@Override
	public @Nullable ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			response.setHeader(HttpHeaders.ALLOW, getAllowHeader());
			return null;
		}

		// Delegate to WebContentGenerator for checking and preparing.
		checkRequest(request);
		prepareResponse(response);

		// Execute handleRequestInternal in synchronized block if required.
		if (this.synchronizeOnSession) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				Object mutex = WebUtils.getSessionMutex(session);
				synchronized (mutex) {
					return handleRequestInternal(request, response);
				}
			}
		}

		return handleRequestInternal(request, response);
	}

	/**
	 * Template method. Subclasses must implement this.
	 * The contract is the same as for {@code handleRequest}.
	 * @see #handleRequest
	 */
	protected abstract @Nullable ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
