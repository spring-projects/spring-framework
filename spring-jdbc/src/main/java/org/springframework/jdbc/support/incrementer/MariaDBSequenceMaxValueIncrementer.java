/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * {@link DataFieldMaxValueIncrementer} that retrieves the next value of a given
 * MariaDB sequence.
 *
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
public class MariaDBSequenceMaxValueIncrementer extends AbstractSequenceMaxValueIncrementer {

	/**
	 * Default constructor for bean property style usage.
	 * @see #setDataSource
	 * @see #setIncrementerName
	 */
	public MariaDBSequenceMaxValueIncrementer() {
	}

	/**
	 * Convenience constructor.
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence to use
	 */
	public MariaDBSequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}

	@Override
	protected String getSequenceQuery() {
		return "select next value for " + getIncrementerName();
	}

}
