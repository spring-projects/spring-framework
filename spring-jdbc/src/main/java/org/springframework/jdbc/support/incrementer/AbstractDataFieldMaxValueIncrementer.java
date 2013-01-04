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

package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;

/**
 * Base implementation of {@link DataFieldMaxValueIncrementer} that delegates
 * to a single {@link #getNextKey} template method that returns a {@code long}.
 * Uses longs for String values, padding with zeroes if required.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 */
public abstract class AbstractDataFieldMaxValueIncrementer implements DataFieldMaxValueIncrementer, InitializingBean {

	private DataSource dataSource;

	/** The name of the sequence/table containing the sequence */
	private String incrementerName;

	/** The length to which a string result should be pre-pended with zeroes */
	protected int paddingLength = 0;


	/**
	 * Default constructor for bean property style usage.
	 * @see #setDataSource
	 * @see #setIncrementerName
	 */
	public AbstractDataFieldMaxValueIncrementer() {
	}

	/**
	 * Convenience constructor.
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence/table to use
	 */
	public AbstractDataFieldMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		Assert.notNull(dataSource, "DataSource must not be null");
		Assert.notNull(incrementerName, "Incrementer name must not be null");
		this.dataSource = dataSource;
		this.incrementerName = incrementerName;
	}


	/**
	 * Set the data source to retrieve the value from.
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Return the data source to retrieve the value from.
	 */
	public DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * Set the name of the sequence/table.
	 */
	public void setIncrementerName(String incrementerName) {
		this.incrementerName = incrementerName;
	}

	/**
	 * Return the name of the sequence/table.
	 */
	public String getIncrementerName() {
		return this.incrementerName;
	}

	/**
	 * Set the padding length, i.e. the length to which a string result
	 * should be pre-pended with zeroes.
	 */
	public void setPaddingLength(int paddingLength) {
		this.paddingLength = paddingLength;
	}

	/**
	 * Return the padding length for String values.
	 */
	public int getPaddingLength() {
		return this.paddingLength;
	}

	public void afterPropertiesSet() {
		if (this.dataSource == null) {
			throw new IllegalArgumentException("Property 'dataSource' is required");
		}
		if (this.incrementerName == null) {
			throw new IllegalArgumentException("Property 'incrementerName' is required");
		}
	}


	public int nextIntValue() throws DataAccessException {
		return (int) getNextKey();
	}

	public long nextLongValue() throws DataAccessException {
		return getNextKey();
	}

	public String nextStringValue() throws DataAccessException {
		String s = Long.toString(getNextKey());
		int len = s.length();
		if (len < this.paddingLength) {
			StringBuilder sb = new StringBuilder(this.paddingLength);
			for (int i = 0; i < this.paddingLength - len; i++) {
				sb.append('0');
			}
			sb.append(s);
			s = sb.toString();
		}
		return s;
	}


	/**
	 * Determine the next key to use, as a long.
	 * @return the key to use as a long. It will eventually be converted later
	 * in another format by the public concrete methods of this class.
	 */
	protected abstract long getNextKey();

}
