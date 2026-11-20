/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
