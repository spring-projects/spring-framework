/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.orm.jpa;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import javax.sql.DataSource;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Costin Leau
 * @author Juergen Hoeller
 */
public class JpaTransactionManagerTests extends TestCase {

	private MockControl factoryControl, managerControl, txControl;

	private EntityManager manager;

	private EntityTransaction tx;

	private EntityManagerFactory factory;

	private JpaTransactionManager transactionManager;

	private JpaTemplate template;

	private TransactionTemplate tt;


	protected void setUp() throws Exception {
		factoryControl = MockControl.createControl(EntityManagerFactory.class);
		factory = (EntityManagerFactory) factoryControl.getMock();
		managerControl = MockControl.createControl(EntityManager.class);
		manager = (EntityManager) managerControl.getMock();
		txControl = MockControl.createControl(EntityTransaction.class);
		tx = (EntityTransaction) txControl.getMock();

		transactionManager = new JpaTransactionManager(factory);
		template = new JpaTemplate(factory);
		template.afterPropertiesSet();
		tt = new TransactionTemplate(transactionManager);

		factoryControl.expectAndReturn(factory.createEntityManager(), manager);
		managerControl.expectAndReturn(manager.getTransaction(), tx);
		tx.begin();
		managerControl.expectAndReturn(manager.isOpen(), true);
		manager.close();
	}

	protected void tearDown() throws Exception {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}


	public void testTransactionCommit() {
		managerControl.expectAndReturn(manager.getTransaction(), tx);
		txControl.expectAndReturn(tx.getRollbackOnly(), false);
		managerControl.expectAndReturn(manager.getTransaction(), tx);
		tx.commit();
		manager.flush();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.hasResource(factory));
				return template.execute(new JpaCallback() {
					public Object doInJpa(EntityManager em) {
						em.flush();
						return l;
					}
				});
			}
		});
		assertSame(l, result);

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testTransactionCommitWithRollbackException() {
		managerControl.expectAndReturn(manager.getTransaction(), tx);
		txControl.expectAndReturn(tx.getRollbackOnly(), true);
		managerControl.expectAndReturn(manager.getTransaction(), tx);
		tx.commit();
		txControl.setThrowable(new RollbackException());
		manager.flush();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		try {
			Object result = tt.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(factory));
					return template.execute(new JpaCallback() {
						public Object doInJpa(EntityManager em) {
							em.flush();
							return l;
						}
					});
				}
			});
			assertSame(l, result);
		}
		catch (TransactionSystemException tse) {
			// expected
			assertTrue(tse.getCause() instanceof RollbackException);
		}

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testTransactionRollback() {
		managerControl.expectAndReturn(manager.getTransaction(), tx);
		txControl.expectAndReturn(tx.isActive(), true);
		tx.rollback();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(factory));
					return template.execute(new JpaCallback() {
						public Object doInJpa(EntityManager em) {
							throw new RuntimeException("some exception");
						}
					});
				}
			});
			fail("Should have propagated RuntimeException");
		}
		catch (RuntimeException ex) {
			// expected
			assertEquals("some exception", ex.getMessage());
		}

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testTransactionRollbackWithAlreadyRolledBack() {
		managerControl.expectAndReturn(manager.getTransaction(), tx);
		txControl.expectAndReturn(tx.isActive(), false);
		// tx.rollback();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(factory));
					return template.execute(new JpaCallback() {
						public Object doInJpa(EntityManager em) {
							throw new RuntimeException("some exception");
						}
					});
				}
			});
			fail("Should have propagated RuntimeException");
		}
		catch (RuntimeException ex) {
			// expected
		}

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testTransactionRollbackOnly() {
		managerControl.expectAndReturn(manager.getTransaction(), tx);
		txControl.expectAndReturn(tx.isActive(), true);
		manager.flush();
		tx.rollback();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.hasResource(factory));

				Object res = template.execute(new JpaCallback() {
					public Object doInJpa(EntityManager em) {
						em.flush();
						return l;
					}
				});
				status.setRollbackOnly();

				return res;
			}
		});

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testParticipatingTransactionWithCommit() {
		managerControl.expectAndReturn(manager.getTransaction(), tx, 2);
		manager.flush();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				txControl.reset();
				txControl.expectAndReturn(tx.getRollbackOnly(), false);
				tx.commit();
				txControl.replay();

				assertTrue(TransactionSynchronizationManager.hasResource(factory));

				return tt.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {

						return template.execute(new JpaCallback() {
							public Object doInJpa(EntityManager em) {
								em.flush();
								return l;
							}
						});
					}
				});
			}
		});

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testParticipatingTransactionWithRollback() {
		managerControl.expectAndReturn(manager.getTransaction(), tx, 2);
		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					txControl.reset();
					txControl.expectAndReturn(tx.isActive(), true, 2);
					tx.setRollbackOnly();
					tx.rollback();
					txControl.replay();

					assertTrue(TransactionSynchronizationManager.hasResource(factory));
					return tt.execute(new TransactionCallback() {
						public Object doInTransaction(TransactionStatus status) {
							return template.execute(new JpaCallback() {
								public Object doInJpa(EntityManager em) {
									throw new RuntimeException("exception");
								}
							});
						}
					});
				}
			});
			fail("Should have propagated RuntimeException");
		}
		catch (RuntimeException ex) {
			// expected
		}

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testParticipatingTransactionWithRollbackOnly() {
		managerControl.expectAndReturn(manager.getTransaction(), tx, 3);
		manager.flush();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					txControl.reset();
					txControl.expectAndReturn(tx.isActive(), true);
					tx.setRollbackOnly();
					txControl.expectAndReturn(tx.getRollbackOnly(), true);
					tx.commit();
					txControl.setThrowable(new RollbackException());
					txControl.replay();

					assertTrue(TransactionSynchronizationManager.hasResource(factory));

					return tt.execute(new TransactionCallback() {
						public Object doInTransaction(TransactionStatus status) {

							template.execute(new JpaCallback() {
								public Object doInJpa(EntityManager em2) {
									em2.flush();
									return l;
								}
							});

							status.setRollbackOnly();
							return null;
						}
					});
				}
			});
			fail("Should have thrown TransactionSystemException");
		}
		catch (TransactionSystemException tse) {
			// expected
			assertTrue(tse.getCause() instanceof RollbackException);
		}

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testParticipatingTransactionWithRequiresNew() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		factoryControl.expectAndReturn(factory.createEntityManager(), manager);
		managerControl.expectAndReturn(manager.getTransaction(), tx, 5);
		manager.flush();
		managerControl.expectAndReturn(manager.isOpen(), true);
		manager.close();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				txControl.verify();
				txControl.reset();

				tx.begin();
				txControl.expectAndReturn(tx.getRollbackOnly(), false);
				tx.commit();
				txControl.expectAndReturn(tx.getRollbackOnly(), false);
				tx.commit();

				txControl.replay();

				assertTrue(TransactionSynchronizationManager.hasResource(factory));
				return tt.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						return template.execute(new JpaCallback() {
							public Object doInJpa(EntityManager em2) {
								em2.flush();
								return l;
							}
						});
					}
				});
			}
		});
		assertSame(l, result);

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testParticipatingTransactionWithRequiresNewAndPrebound() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		managerControl.expectAndReturn(manager.getTransaction(), tx, 5);
		manager.flush();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			Object result = tt.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					txControl.verify();
					txControl.reset();

					tx.begin();
					txControl.expectAndReturn(tx.getRollbackOnly(), false);
					tx.commit();
					txControl.expectAndReturn(tx.getRollbackOnly(), false);
					tx.commit();

					txControl.replay();

					JpaTemplate template2 = new JpaTemplate(factory);
					template2.execute(new JpaCallback() {
						public Object doInJpa(EntityManager em) throws PersistenceException {
							return null;
						}
					});

					assertTrue(TransactionSynchronizationManager.hasResource(factory));
					return tt.execute(new TransactionCallback() {
						public Object doInTransaction(TransactionStatus status) {
							return template.execute(new JpaCallback() {
								public Object doInJpa(EntityManager em2) {
									em2.flush();
									return l;
								}
							});
						}
					});
				}
			});
			assertSame(l, result);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testPropagationSupportsAndRequiresNew() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		manager.flush();
		managerControl.expectAndReturn(manager.getTransaction(), tx, 2);
		txControl.expectAndReturn(tx.getRollbackOnly(), false);
		tx.commit();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				assertFalse(TransactionSynchronizationManager.hasResource(factory));
				TransactionTemplate tt2 = new TransactionTemplate(transactionManager);
				tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				return tt2.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						return template.execute(new JpaCallback() {
							public Object doInJpa(EntityManager em2) {
								em2.flush();
								return l;
							}
						});
					}
				});
			}
		});
		assertSame(l, result);

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testPropagationSupportsAndRequiresNewAndEarlyAccess() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		factoryControl.expectAndReturn(factory.createEntityManager(), manager);
		managerControl.expectAndReturn(manager.getTransaction(), tx, 2);
		manager.flush();
		managerControl.expectAndReturn(manager.isOpen(), true);
		manager.close();
		txControl.expectAndReturn(tx.getRollbackOnly(), false);
		tx.commit();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				JpaTemplate template2 = new JpaTemplate(factory);
				template2.execute(new JpaCallback() {
					public Object doInJpa(EntityManager em) throws PersistenceException {
						return null;
					}
				});

				assertTrue(TransactionSynchronizationManager.hasResource(factory));
				TransactionTemplate tt2 = new TransactionTemplate(transactionManager);
				tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				return tt2.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						return template.execute(new JpaCallback() {
							public Object doInJpa(EntityManager em2) {
								em2.flush();
								return l;
							}
						});
					}
				});
			}
		});
		assertSame(l, result);

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testTransactionWithRequiresNewInAfterCompletion() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		MockControl managerControl2 = MockControl.createControl(EntityManager.class);
		EntityManager manager2 = (EntityManager) managerControl2.getMock();
		MockControl txControl2 = MockControl.createControl(EntityTransaction.class);
		EntityTransaction tx2 = (EntityTransaction) txControl2.getMock();

		managerControl.expectAndReturn(manager.getTransaction(), tx, 2);
		factoryControl.expectAndReturn(factory.createEntityManager(), manager2);
		managerControl2.expectAndReturn(manager2.getTransaction(), tx2, 3);
		txControl.expectAndReturn(tx.getRollbackOnly(), false);
		txControl2.expectAndReturn(tx2.getRollbackOnly(), false);
		manager.flush();
		tx.commit();
		tx2.begin();
		tx2.commit();
		manager2.flush();
		managerControl2.expectAndReturn(manager2.isOpen(), true);
		manager2.close();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();
		managerControl2.replay();
		txControl2.replay();

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				template.execute(new JpaCallback() {
					public Object doInJpa(EntityManager em2) {
						em2.flush();
						return null;
					}
				});
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
					public void afterCompletion(int status) {
						tt.execute(new TransactionCallback() {
							public Object doInTransaction(TransactionStatus status) {
								return template.execute(new JpaCallback() {
									public Object doInJpa(EntityManager em2) {
										em2.flush();
										return null;
									}
								});
							}
						});
					}
				});
				return null;
			}
		});

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
		managerControl2.verify();
		txControl2.verify();
	}

	public void testTransactionCommitWithPropagationSupports() {
		managerControl.reset();
		txControl.reset();

		manager.flush();
		managerControl.expectAndReturn(manager.isOpen(), true);
		manager.close();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(!TransactionSynchronizationManager.hasResource(factory));
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue(!status.isNewTransaction());
				return template.execute(new JpaCallback() {
					public Object doInJpa(EntityManager em) {
						em.flush();
						return l;
					}
				});
			}
		});
		assertSame(l, result);

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testTransactionRollbackWithPropagationSupports() {
		managerControl.reset();
		txControl.reset();

		manager.flush();
		managerControl.expectAndReturn(manager.isOpen(), true);
		manager.close();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(!TransactionSynchronizationManager.hasResource(factory));
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue(!status.isNewTransaction());
				template.execute(new JpaCallback() {
					public Object doInJpa(EntityManager em) {
						em.flush();
						return null;
					}
				});
				status.setRollbackOnly();
				return null;
			}
		});

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testTransactionCommitWithPrebound() {
		factoryControl.reset();
		managerControl.reset();
		txControl.reset();

		managerControl.expectAndReturn(manager.getTransaction(), tx, 3);
		tx.begin();
		txControl.expectAndReturn(tx.getRollbackOnly(), false);
		tx.commit();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			Object result = tt.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(factory));
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					return template.execute(new JpaCallback() {
						public Object doInJpa(EntityManager em) {
							return l;
						}
					});
				}
			});
			assertSame(l, result);

			assertTrue(TransactionSynchronizationManager.hasResource(factory));
			assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testTransactionRollbackWithPrebound() {
		factoryControl.reset();
		managerControl.reset();
		txControl.reset();

		managerControl.expectAndReturn(manager.getTransaction(), tx, 2);
		tx.begin();
		txControl.expectAndReturn(tx.isActive(), true);
		tx.rollback();
		manager.clear();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			tt.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(factory));
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					template.execute(new JpaCallback() {
						public Object doInJpa(EntityManager em) {
							return null;
						}
					});
					status.setRollbackOnly();
					return null;
				}
			});

			assertTrue(TransactionSynchronizationManager.hasResource(factory));
			assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testTransactionCommitWithPreboundAndPropagationSupports() {
		factoryControl.reset();
		managerControl.reset();
		txControl.reset();

		manager.joinTransaction();
		manager.flush();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			Object result = tt.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(factory));
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue(!status.isNewTransaction());
					return template.execute(new JpaCallback() {
						public Object doInJpa(EntityManager em) {
							em.flush();
							return l;
						}
					});
				}
			});
			assertSame(l, result);

			assertTrue(TransactionSynchronizationManager.hasResource(factory));
			assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testTransactionRollbackWithPreboundAndPropagationSupports() {
		factoryControl.reset();
		managerControl.reset();
		txControl.reset();

		manager.joinTransaction();
		manager.flush();
		manager.clear();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			tt.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.hasResource(factory));
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue(!status.isNewTransaction());
					template.execute(new JpaCallback() {
						public Object doInJpa(EntityManager em) {
							em.flush();
							return null;
						}
					});
					status.setRollbackOnly();
					return null;
				}
			});

			assertTrue(TransactionSynchronizationManager.hasResource(factory));
			assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testTransactionCommitWithDataSource() throws SQLException {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		transactionManager.setDataSource(ds);

		managerControl.expectAndReturn(manager.getTransaction(), tx);
		managerControl.expectAndReturn(manager.getTransaction(), tx);
		txControl.expectAndReturn(tx.getRollbackOnly(), false);
		tx.commit();
		manager.flush();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();
		dsControl.replay();

		final List<String> l = new ArrayList<String>();
		l.add("test");

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.hasResource(factory));
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				return template.execute(new JpaCallback() {
					public Object doInJpa(EntityManager em) {
						em.flush();
						return l;
					}
				});
			}
		});

		assertTrue(result == l);

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
		dsControl.verify();
	}

	public void testInvalidIsolation() {
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		txControl.reset();
		managerControl.reset();

		managerControl.expectAndReturn(manager.isOpen(), true);
		manager.close();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				protected void doInTransactionWithoutResult(TransactionStatus status) {
				}
			});
			fail("Should have thrown InvalidIsolationLevelException");
		}
		catch (InvalidIsolationLevelException ex) {
			// expected
		}

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

	public void testTransactionFlush() {
		managerControl.expectAndReturn(manager.getTransaction(), tx);
		txControl.expectAndReturn(tx.getRollbackOnly(), false);
		managerControl.expectAndReturn(manager.getTransaction(), tx);
		tx.commit();
		manager.flush();

		factoryControl.replay();
		managerControl.replay();
		txControl.replay();

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.hasResource(factory));
				status.flush();
			}
		});

		assertTrue(!TransactionSynchronizationManager.hasResource(factory));
		assertTrue(!TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl.verify();
		managerControl.verify();
		txControl.verify();
	}

}
