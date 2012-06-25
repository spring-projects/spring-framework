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

package org.springframework.orm.eclipselink;

import org.eclipse.persistence.exceptions.EclipseLinkException;
import org.springframework.dao.UncategorizedDataAccessException;

/**
 * EclipseLink-specific subclass of UncategorizedDataAccessException, for
 * EclipseLink system errors that do not match any concrete
 * <code>org.springframework.dao</code> exceptions.
 * 
 * @author Jan Stamer
 * @since 3.2
 * @see EclipseLinkUtils#convertEclipseLinkAccessException(EclipseLinkException)
 */
public class EclipseLinkSystemException extends UncategorizedDataAccessException {

	/**
	 * Create a new HibernateSystemException, wrapping an arbitrary
	 * HibernateException.
	 * @param cause the HibernateException thrown
	 */
	public EclipseLinkSystemException(EclipseLinkException cause) {
		super(cause != null ? cause.getMessage() : null, cause);
	}

}
