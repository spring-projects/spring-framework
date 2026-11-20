/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.test;

import java.math.BigDecimal;

/**
 * @author Thomas Risberg
 */
public class ConcretePerson extends AbstractPerson {

	private BigDecimal balance;


	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

}
