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

package org.springframework.web.servlet.view.tiles;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.support.JstlUtils;
import org.springframework.web.servlet.support.RequestContext;

/**
 * Specialization of {@link TilesView} for JSTL pages,
 * i.e. Tiles pages that  use the JSP Standard Tag Library.
 *
 * <p><b>NOTE:</b> This TilesJstlView class supports Tiles 1.x,
 * a.k.a. "Struts Tiles", which comes as part of Struts 1.x.
 * For Tiles 2.x support, check out
 * {@link org.springframework.web.servlet.view.tiles2.TilesView}.
 *
 * <p>Exposes JSTL-specific request attributes specifying locale
 * and resource bundle for JSTL's formatting and message tags,
 * using Spring's locale and message source.
 *
 * <p>This is a separate class mainly to avoid JSTL dependencies
 * in TilesView itself.
 *
 * @author Juergen Hoeller
 * @since 20.08.2003
 * @see org.springframework.web.servlet.support.JstlUtils#exposeLocalizationContext
 * @deprecated as of Spring 3.0
 */
@Deprecated
public class TilesJstlView extends TilesView {

	@Override
	protected void exposeHelpers(HttpServletRequest request) throws Exception {
		JstlUtils.exposeLocalizationContext(new RequestContext(request, getServletContext()));
	}

}
