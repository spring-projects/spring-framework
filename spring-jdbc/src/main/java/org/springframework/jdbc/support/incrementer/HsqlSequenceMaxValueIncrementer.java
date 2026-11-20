/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * {@link DataFieldMaxValueIncrementer} that retrieves the next value
 * of a given HSQL sequence.
 *
 * <p>Thanks to Guillaume Bilodeau for the suggestion!
 *
 * <p><b>NOTE:</b> This is an alternative to using a regular table to support
 * generating unique keys that was necessary in previous versions of HSQL.
 *
 * @author Thomas Risberg
 * @since 2.5
 * @see HsqlMaxValueIncrementer
 */
public class HsqlSequenceMaxValueIncrementer extends AbstractSequenceMaxValueIncrementer {

	/**
	 * Default constructor for bean property style usage.
	 * @see #setDataSource
	 * @see #setIncrementerName
	 */
	public HsqlSequenceMaxValueIncrementer() {
	}

	/**
	 * Convenience constructor.
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence/table to use
	 */
	public HsqlSequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}


	@Override
	protected String getSequenceQuery() {
		return "call next value for " + getIncrementerName();
	}

}
