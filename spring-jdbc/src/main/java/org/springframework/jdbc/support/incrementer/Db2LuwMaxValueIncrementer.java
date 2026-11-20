/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * @since 4.3.15
 * @see Db2MainframeMaxValueIncrementer
 */
public class Db2LuwMaxValueIncrementer extends AbstractSequenceMaxValueIncrementer {

	/**
	 * Default constructor for bean property style usage.
	 * @see #setDataSource
	 * @see #setIncrementerName
	 */
	public Db2LuwMaxValueIncrementer() {
	}

	/**
	 * Convenience constructor.
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence/table to use
	 */
	public Db2LuwMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}


	@Override
	protected String getSequenceQuery() {
		return "values nextval for " + getIncrementerName();
	}

}
