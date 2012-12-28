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
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.springframework.web.portlet.ModelAndView;

/**
 * <p>Trivial controller that transforms the PortletMode to a view name.
 * The advantage here is that the client is not exposed to
 * the concrete view technology but rather just to the controller URL;
 * the concrete view will be determined by the ViewResolver.</p>
 *
 * <p>Example: PortletMode.VIEW -> "view"</p>
 *
 * <p>This controller does not handle action requests.</p>
 *
 * @author William G. Thompson, Jr.
 * @author John A. Lewis
 * @since 2.0
 */
public class PortletModeNameViewController implements Controller {

	@Override
	public void handleActionRequest(ActionRequest request, ActionResponse response) throws Exception {
		throw new PortletException("PortletModeNameViewController does not handle action requests");
	}

	@Override
	public ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) {
		return new ModelAndView(request.getPortletMode().toString());
	}

}
