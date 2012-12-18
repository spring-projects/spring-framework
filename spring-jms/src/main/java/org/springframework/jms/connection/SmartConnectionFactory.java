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

package org.springframework.jms.connection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

/**
 * Extension of the {@code javax.jms.ConnectionFactory} interface,
 * indicating how to release Connections obtained from it.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 */
public interface SmartConnectionFactory extends ConnectionFactory {

	/**
	 * Should we stop the Connection, obtained from this ConnectionFactory?
	 * @param con the Connection to check
	 * @return whether a stop call is necessary
	 * @see javax.jms.Connection#stop()
	 */
	boolean shouldStop(Connection con);

}
