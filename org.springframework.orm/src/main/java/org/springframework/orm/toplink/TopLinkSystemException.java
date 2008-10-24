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

package org.springframework.orm.toplink;

import oracle.toplink.exceptions.TopLinkException;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * TopLink-specific subclass of UncategorizedDataAccessException,
 * for TopLink system errors that do not match any concrete
 * <code>org.springframework.dao</code> exceptions.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see SessionFactoryUtils#convertTopLinkAccessException
 */
public class TopLinkSystemException extends UncategorizedDataAccessException {

	public TopLinkSystemException(TopLinkException ex) {
		super(ex.getMessage(), ex);
	}

}
