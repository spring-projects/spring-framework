/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.web.servlet.view.tiles;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.tiles.ComponentContext;

/**
 * @author Juergen Hoeller
 * @since 22.08.2003
 */
public class TestComponentController extends ComponentControllerSupport {

	@Override
	protected void doPerform(ComponentContext componentContext, HttpServletRequest request, HttpServletResponse response) {
		request.setAttribute("testAttr", "testVal");
		TilesView.setPath(request, "/WEB-INF/jsp/layout.jsp");
	}

}
