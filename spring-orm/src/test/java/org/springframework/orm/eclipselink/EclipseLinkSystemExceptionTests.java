/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.junit.Assert.*;

import org.eclipse.persistence.exceptions.DatabaseException;
import org.hibernate.HibernateException;
import org.junit.Test;

/**
 * @author Jan Stamer
 */
@SuppressWarnings("serial")
public class EclipseLinkSystemExceptionTests {

	@Test
	public void withNull() {
		EclipseLinkSystemException exception = new EclipseLinkSystemException(null);
		assertNull(exception.getCause());
		assertNull(exception.getMessage());
	}

	@Test
	public void createWithCause() {
		DatabaseException dbExceptionWithCause = new DatabaseException("my custom exception cause") {
		};
		EclipseLinkSystemException elSystemException = new EclipseLinkSystemException(dbExceptionWithCause);
		assertEquals(dbExceptionWithCause, elSystemException.getCause());
		assertTrue(elSystemException.getMessage().contains("my custom exception cause"));
	}

	@Test
	public void createWithNullCause() throws HibernateException {
		DatabaseException dbExceptionWithCause = new DatabaseException((String) null) {
		};
		EclipseLinkSystemException elSystemException = new EclipseLinkSystemException(dbExceptionWithCause);
		assertEquals(dbExceptionWithCause, elSystemException.getCause());
		assertTrue(elSystemException.getMessage().contains("null"));
	}

}
