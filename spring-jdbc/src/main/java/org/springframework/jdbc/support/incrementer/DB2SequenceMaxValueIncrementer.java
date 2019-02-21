/*
 * Copyright 2002-2008 the original author or authors.
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

/**
 * {@link DataFieldMaxValueIncrementer} that retrieves the next value
 * of a given sequence on DB2 LUW (for Linux, Unix and Windows).
 *
 * <p>Thanks to Mark MacMahon for the suggestion!
 *
 * @author Juergen Hoeller
 * @since 1.1.3
 * @deprecated in favor of the specifically named {@link Db2LuwMaxValueIncrementer}
 */
@Deprecated
public class DB2SequenceMaxValueIncrementer extends Db2LuwMaxValueIncrementer {

	/**
	 * Default constructor for bean property style usage.
	 * @see #setDataSource
	 * @see #setIncrementerName
	 */
	public DB2SequenceMaxValueIncrementer() {
	}

	/**
	 * Convenience constructor.
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence/table to use
	 */
	public DB2SequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}

}
