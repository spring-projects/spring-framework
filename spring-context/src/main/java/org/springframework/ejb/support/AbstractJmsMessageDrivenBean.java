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

import javax.jms.MessageListener;

/**
 * Convenient base class for JMS-based EJB 2.x MDBs. Requires subclasses
 * to implement the JMS <code>javax.jms.MessageListener</code> interface.
 *
 * @author Rod Johnson
 * @deprecated as of Spring 3.2, in favor of implementing EJBs in EJB 3 style
 */
@Deprecated
public abstract class AbstractJmsMessageDrivenBean extends AbstractMessageDrivenBean implements MessageListener {

	// Empty: The purpose of this class is to ensure
	// that subclasses implement <code>javax.jms.MessageListener</code>.

}
