package org.springframework.transaction.chained;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;
import org.springframework.transaction.PlatformTransactionManager;

final class TransactionManagerMatcher extends TypeSafeMatcher<PlatformTransactionManager> {

	private final boolean commitCheck;

	TransactionManagerMatcher(boolean commitCheck) {
		this.commitCheck = commitCheck;
	}

	@Override
	public boolean matchesSafely(PlatformTransactionManager platformTransactionManager) {
		TestPlatformTransactionManager ptm = (TestPlatformTransactionManager) platformTransactionManager;
		if (commitCheck) {
			return ptm.isCommitted();
		} else {
			return ptm.wasRolledBack();
		}
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("that a " + (commitCheck ? "committed" : "rolled-back") + " TransactionManager");
	}

	@Factory
	static TransactionManagerMatcher isCommitted() {
		return new TransactionManagerMatcher(true);
	}

	@Factory
	static TransactionManagerMatcher wasRolledback() {
		return new TransactionManagerMatcher(false);
	}
}
