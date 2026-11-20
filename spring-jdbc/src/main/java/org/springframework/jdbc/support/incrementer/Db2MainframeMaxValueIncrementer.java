/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
