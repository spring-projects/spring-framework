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

package org.springframework.ejb.support;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * Interface to be implemented by Session Beans that want
 * to expose important state to cooperating classes.
 *
 * <p>Implemented by Spring's AbstractSessionBean class and hence by
 * all of Spring's specific Session Bean support classes, such as
 * {@link AbstractStatelessSessionBean} and {@link AbstractStatefulSessionBean}.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see AbstractStatelessSessionBean
 * @see AbstractStatefulSessionBean
 */
public interface SmartSessionBean extends SessionBean {

	/**
	 * Return the SessionContext that was passed to the Session Bean
	 * by the EJB container. Can be used by cooperating infrastructure
	 * code to get access to the user credentials, for example.
	 */
	SessionContext getSessionContext();

}
