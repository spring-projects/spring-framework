/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.context.support;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Servlet variant of {@link org.springframework.context.support.LiveBeansView}'s
 * MBean exposure.
 *
 * <p>Generates a JSON snapshot for current beans and their dependencies in
 * all ApplicationContexts that live within the current web application.
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @see org.springframework.context.support.LiveBeansView#getSnapshotAsJson()
 * @deprecated as of 5.3, in favor of using Spring Boot actuators for such needs
 */
@Deprecated
@SuppressWarnings("serial")
public class LiveBeansViewServlet extends HttpServlet {

	@Nullable
	private org.springframework.context.support.LiveBeansView liveBeansView;


	@Override
	public void init() throws ServletException {
		this.liveBeansView = buildLiveBeansView();
	}

	protected org.springframework.context.support.LiveBeansView buildLiveBeansView() {
		return new ServletContextLiveBeansView(getServletContext());
	}


	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Assert.state(this.liveBeansView != null, "No LiveBeansView available");
		String content = this.liveBeansView.getSnapshotAsJson();
		response.setContentType("application/json");
		response.setContentLength(content.length());
		response.getWriter().write(content);
	}

}
