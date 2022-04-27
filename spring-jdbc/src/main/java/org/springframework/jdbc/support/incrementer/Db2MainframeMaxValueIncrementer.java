/*
 * Copyright 2002-2018 the original author or authors.
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

import javax.sql.DataSource;

/**
 * {@link DataFieldMaxValueIncrementer} that retrieves the next value
 * of a given sequence on DB2 for the mainframe (z/OS, DB2/390, DB2/400).
 *
 * <p>Thanks to Jens Eickmeyer for the suggestion!
 *
 * @author Juergen Hoeller
 * @since 4.3.15
 * @see Db2LuwMaxValueIncrementer
 */
public class Db2MainframeMaxValueIncrementer extends AbstractSequenceMaxValueIncrementer {

	/**
	 * Default constructor for bean property style usage.
	 * @see #setDataSource
	 * @see #setIncrementerName
	 */
	public Db2MainframeMaxValueIncrementer() {
	}

	/**
	 * Convenience constructor.
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence/table to use
	 */
	public Db2MainframeMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}


	@Override
	protected String getSequenceQuery() {
		return "select next value for " + getIncrementerName() + " from sysibm.sysdummy1";
	}

}
