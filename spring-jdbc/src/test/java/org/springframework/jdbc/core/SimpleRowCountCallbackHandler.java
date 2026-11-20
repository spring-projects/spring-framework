/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core;

import java.sql.ResultSet;

/**
 * Simple row count callback handler for testing purposes.
 * Does not call any JDBC methods on the given ResultSet.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public class SimpleRowCountCallbackHandler implements RowCallbackHandler {

	private int count;


	@Override
	public void processRow(ResultSet rs) {
		count++;
	}

	public int getCount() {
		return count;
	}

}
