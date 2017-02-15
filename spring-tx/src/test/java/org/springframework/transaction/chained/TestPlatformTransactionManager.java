package org.springframework.transaction.chained;

import static org.mockito.Mockito.mock;

import org.hamcrest.Factory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

class TestPlatformTransactionManager implements PlatformTransactionManager {

	private final String name;
	private Long commitTime;
	private Long rollbackTime;

	TestPlatformTransactionManager(String name) {
		this.name = name;
	}

	@Factory
	static PlatformTransactionManager createFailingTransactionManager(String name) {
		return new TestPlatformTransactionManager(name + "-failing") {
			@Override
			public void commit(TransactionStatus status) {
				throw new IllegalStateException();
			}

			@Override
			public void rollback(TransactionStatus status) {
				throw new IllegalStateException();
			}
		};
	}

	@Factory
	static PlatformTransactionManager createNonFailingTransactionManager(String name) {
		return new TestPlatformTransactionManager(name + "-non-failing");
	}

	@Override
	public String toString() {
		return name + (isCommitted() ? " (committed) " : " (not committed)");
	}

	@Override
	public TransactionStatus getTransaction(TransactionDefinition definition) {
		return mock(TransactionStatus.class);
	}

	@Override
	public void commit(TransactionStatus status) {
		commitTime = System.currentTimeMillis();
	}

	@Override
	public void rollback(TransactionStatus status) {
		rollbackTime = System.currentTimeMillis();
	}

	public boolean isCommitted() {
		return commitTime != null;
	}

	public boolean wasRolledBack() {
		return rollbackTime != null;
	}

	public Long getCommitTime() {
		return commitTime;
	}
}
