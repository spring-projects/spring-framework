/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.support.incrementer;

import org.springframework.dao.DataAccessException;

/**
 * Interface that defines contract of incrementing any data store field's
 * maximum value. Works much like a sequence number generator.
 *
 * <p>Typical implementations may use standard SQL, native RDBMS sequences
 * or Stored Procedures to do the job.
 *
 * @author Dmitriy Kopylenko
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 */
public interface DataFieldMaxValueIncrementer {

	/**
	 * Increment the data store field's max value as int.
	 * @return int next data store value such as <b>max + 1</b>
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	int nextIntValue() throws DataAccessException;

	/**
	 * Increment the data store field's max value as long.
	 * @return int next data store value such as <b>max + 1</b>
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	long nextLongValue() throws DataAccessException;

	/**
	 * Increment the data store field's max value as String.
	 * @return next data store value such as <b>max + 1</b>
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	String nextStringValue() throws DataAccessException;

}
