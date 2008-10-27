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

import org.springframework.web.servlet.ModelAndView;

/**
 * ThrowawayController is an alternative to Spring's default Controller interface,
 * for executable per-request command instances that are not aware of the Servlet API.
 * In contrast to Controller, implementing beans are not supposed to be defined as
 * Servlet/Struts-style singletons that process a HttpServletRequest but rather as
 * WebWork/Maverick-style prototypes that get populated with request parameters,
 * executed to determine a view, and thrown away afterwards.
 *
 * <p>The main advantage of this controller programming model is that controllers
 * are testable without HttpServletRequest/HttpServletResponse mocks, just like
 * WebWork actions. They are still web UI workflow controllers: Spring does not
 * aim for the arguably hard-to-achieve reusability of such controllers in non-web
 * environments, as XWork (the generic command framework from WebWork2) does
 * but just for ease of testing.
 *
 * <p>A ThrowawayController differs from the command notion of Base- or
 * AbstractCommandController in that a ThrowawayController is an <i>executable</i>
 * command that contains workflow logic to determine the next view to render,
 * while BaseCommandController treats commands as plain parameter holders.
 *
 * <p>If binding request parameters to this controller fails, a fatal BindException
 * will be thrown.
 *
 * <p>If you need access to the HttpServletRequest and/or HttpServletResponse,
 * consider implementing Controller or deriving from AbstractCommandController.
 * ThrowawayController is specifically intended for controllers that are not aware
 * of the Servlet API at all. Accordingly, if you need to handle session form objects
 * or even wizard forms, consider the corresponding Controller subclasses.
 *
 * @author Juergen Hoeller
 * @since 08.12.2003
 * @see org.springframework.web.servlet.mvc.Controller
 * @see org.springframework.web.servlet.mvc.AbstractCommandController
 * @deprecated as of Spring 2.5, in favor of annotation-based controllers.
 * To be removed in Spring 3.0.
 */
public interface ThrowawayController {

	/**
	 * Execute this controller according to its bean properties.
	 * Gets invoked after a new instance of the controller has been populated with request
	 * parameters. Is supposed to return a ModelAndView in any case, as it is not able to
	 * generate a response itself.
	 * @return a ModelAndView to render
	 * @throws Exception in case of errors
	 */
	ModelAndView execute() throws Exception;

}
