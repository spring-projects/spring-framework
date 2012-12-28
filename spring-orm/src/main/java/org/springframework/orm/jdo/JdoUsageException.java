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

package org.springframework.orm.jdo;

import javax.jdo.JDOFatalUserException;
import javax.jdo.JDOUserException;

import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * JDO-specific subclass of InvalidDataAccessApiUsageException.
 * Converts JDO's JDOUserException and JDOFatalUserException.
 *
 * @author Juergen Hoeller
 * @since 03.06.2003
 * @see PersistenceManagerFactoryUtils#convertJdoAccessException
 */
@SuppressWarnings("serial")
public class JdoUsageException extends InvalidDataAccessApiUsageException {

	public JdoUsageException(JDOUserException ex) {
		super(ex.getMessage(), ex);
	}

	public JdoUsageException(JDOFatalUserException ex) {
		super(ex.getMessage(), ex);
	}

}
