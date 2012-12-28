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

import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.springframework.web.portlet.ModelAndView;

/**
 * <p>Trivial controller that always returns a named view. The view
 * can be configured using an exposed configuration property. This
 * controller offers an alternative to sending a request straight to a view
 * such as a JSP. The advantage here is that the client is not exposed to
 * the concrete view technology but rather just to the controller URL;
 * the concrete view will be determined by the ViewResolver.</p>
 *
 * <p><b><a name="workflow">Workflow
 * (<a href="AbstractController.html#workflow">and that defined by superclass</a>):</b><br>
 * <ol>
 *  <li>Render request is received by the controller</li>
 *  <li>call to {@link #handleRenderRequestInternal handleRenderRequestInternal} which
 *      just returns the view, named by the configuration property
 *      {@code viewName}. Nothing more, nothing less</li>
 * </ol>
 * </p>
 *
 * <p>This controller does not handle action requests.</p>
 *
 * <p><b><a name="config">Exposed configuration properties</a>
 * (<a href="AbstractController.html#config">and those defined by superclass</a>):</b><br>
 * <table border="1">
 *  <tr>
 *      <td><b>name</b></td>
 *      <td><b>default</b></td>
 *      <td><b>description</b></td>
 *  </tr>
 *  <tr>
 *      <td>viewName</td>
 *      <td><i>null</i></td>
 *      <td>the name of the view the viewResolver will use to forward to
 *          (if this property is not set, an exception will be thrown during
 *          initialization)</td>
 *  </tr>
 * </table>
 * </p>
 *
 * @author John A. Lewis
 * @since 2.0
 */
public class ParameterizableViewController extends AbstractController {

	private String viewName;


	/**
	 * Set the name of the view to delegate to.
	 */
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	/**
	 * Return the name of the view to delegate to.
	 */
	public String getViewName() {
		return this.viewName;
	}

	@Override
	protected void initApplicationContext() {
		if (this.viewName == null) {
			throw new IllegalArgumentException("Property 'viewName' is required");
		}
	}


	/**
	 * Return a ModelAndView object with the specified view name.
	 */
	@Override
	protected ModelAndView handleRenderRequestInternal(RenderRequest request, RenderResponse response)
			throws Exception {

		return new ModelAndView(getViewName());
	}

}
