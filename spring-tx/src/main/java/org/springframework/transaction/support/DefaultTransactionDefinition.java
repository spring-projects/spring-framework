/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.support;

import java.io.Serializable;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link TransactionDefinition} interface,
 * offering bean-style configuration and sensible default values
 * (PROPAGATION_REQUIRED, ISOLATION_DEFAULT, TIMEOUT_DEFAULT, readOnly=false).
 *
 * <p>Base class for both {@link TransactionTemplate} and
 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 08.05.2003
 */
@SuppressWarnings("serial")
public class DefaultTransactionDefinition implements TransactionDefinition, Serializable {

	/** Prefix for the propagation constants defined in TransactionDefinition. */
	public static final String PREFIX_PROPAGATION = "PROPAGATION_";

	/** Prefix for the isolation constants defined in TransactionDefinition. */
	public static final String PREFIX_ISOLATION = "ISOLATION_";

	/** Prefix for transaction timeout values in description strings. */
	public static final String PREFIX_TIMEOUT = "timeout_";

	/** Marker for read-only transactions in description strings. */
	public static final String READ_ONLY_MARKER = "readOnly";


	/**
	 * Map of constant names to constant values for the propagation constants
	 * defined in {@link TransactionDefinition}.
	 */
	static final Map<String, Integer> propagationConstants = Map.of(
			"PROPAGATION_REQUIRED", TransactionDefinition.PROPAGATION_REQUIRED,
			"PROPAGATION_SUPPORTS", TransactionDefinition.PROPAGATION_SUPPORTS,
			"PROPAGATION_MANDATORY", TransactionDefinition.PROPAGATION_MANDATORY,
			"PROPAGATION_REQUIRES_NEW", TransactionDefinition.PROPAGATION_REQUIRES_NEW,
			"PROPAGATION_NOT_SUPPORTED", TransactionDefinition.PROPAGATION_NOT_SUPPORTED,
			"PROPAGATION_NEVER", TransactionDefinition.PROPAGATION_NEVER,
			"PROPAGATION_NESTED", TransactionDefinition.PROPAGATION_NESTED
		);

	/**
	 * Map of constant names to constant values for the isolation constants
	 * defined in {@link TransactionDefinition}.
	 */
	static final Map<String, Integer> isolationConstants = Map.of(
			"ISOLATION_DEFAULT", TransactionDefinition.ISOLATION_DEFAULT,
			"ISOLATION_READ_UNCOMMITTED", TransactionDefinition.ISOLATION_READ_UNCOMMITTED,
			"ISOLATION_READ_COMMITTED", TransactionDefinition.ISOLATION_READ_COMMITTED,
			"ISOLATION_REPEATABLE_READ", TransactionDefinition.ISOLATION_REPEATABLE_READ,
			"ISOLATION_SERIALIZABLE", TransactionDefinition.ISOLATION_SERIALIZABLE
		);

	private int propagationBehavior = PROPAGATION_REQUIRED;

	private int isolationLevel = ISOLATION_DEFAULT;

	private int timeout = TIMEOUT_DEFAULT;

	private boolean readOnly = false;

	@Nullable
	private String name;


	/**
	 * Create a new DefaultTransactionDefinition, with default settings.
	 * Can be modified through bean property setters.
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 */
	public DefaultTransactionDefinition() {
	}

	/**
	 * Copy constructor. Definition can be modified through bean property setters.
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 */
	public DefaultTransactionDefinition(TransactionDefinition other) {
		this.propagationBehavior = other.getPropagationBehavior();
		this.isolationLevel = other.getIsolationLevel();
		this.timeout = other.getTimeout();
		this.readOnly = other.isReadOnly();
		this.name = other.getName();
	}

	/**
	 * Create a new DefaultTransactionDefinition with the given
	 * propagation behavior. Can be modified through bean property setters.
	 * @param propagationBehavior one of the propagation constants in the
	 * TransactionDefinition interface
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 */
	public DefaultTransactionDefinition(int propagationBehavior) {
		this.propagationBehavior = propagationBehavior;
	}


	/**
	 * Set the propagation behavior by the name of the corresponding constant in
	 * {@link TransactionDefinition} &mdash; for example, {@code "PROPAGATION_REQUIRED"}.
	 * @param constantName name of the constant
	 * @throws IllegalArgumentException if the supplied value is not resolvable
	 * to one of the {@code PROPAGATION_} constants or is {@code null}
	 * @see #setPropagationBehavior
	 * @see #PROPAGATION_REQUIRED
	 */
	public final void setPropagationBehaviorName(String constantName) throws IllegalArgumentException {
		Assert.hasText(constantName, "'constantName' must not be null or blank");
		Integer propagationBehavior = propagationConstants.get(constantName);
		Assert.notNull(propagationBehavior, "Only propagation behavior constants allowed");
		this.propagationBehavior = propagationBehavior;
	}

	/**
	 * Set the propagation behavior. Must be one of the propagation constants
	 * in the TransactionDefinition interface. Default is PROPAGATION_REQUIRED.
	 * <p>Exclusively designed for use with {@link #PROPAGATION_REQUIRED} or
	 * {@link #PROPAGATION_REQUIRES_NEW} since it only applies to newly started
	 * transactions. Consider switching the "validateExistingTransactions" flag to
	 * "true" on your transaction manager if you'd like isolation level declarations
	 * to get rejected when participating in an existing transaction with a different
	 * isolation level.
	 * <p>Note that a transaction manager that does not support custom isolation levels
	 * will throw an exception when given any other level than {@link #ISOLATION_DEFAULT}.
	 * @throws IllegalArgumentException if the supplied value is not one of the
	 * {@code PROPAGATION_} constants
	 * @see #PROPAGATION_REQUIRED
	 */
	public final void setPropagationBehavior(int propagationBehavior) {
		Assert.isTrue(propagationConstants.containsValue(propagationBehavior),
				"Only values of propagation constants allowed");
		this.propagationBehavior = propagationBehavior;
	}

	@Override
	public final int getPropagationBehavior() {
		return this.propagationBehavior;
	}

	/**
	 * Set the isolation level by the name of the corresponding constant in
	 * {@link TransactionDefinition} &mdash; for example, {@code "ISOLATION_DEFAULT"}.
	 * @param constantName name of the constant
	 * @throws IllegalArgumentException if the supplied value is not resolvable
	 * to one of the {@code ISOLATION_} constants or is {@code null}
	 * @see #setIsolationLevel
	 * @see #ISOLATION_DEFAULT
	 */
	public final void setIsolationLevelName(String constantName) throws IllegalArgumentException {
		Assert.hasText(constantName, "'constantName' must not be null or blank");
		Integer isolationLevel = isolationConstants.get(constantName);
		Assert.notNull(isolationLevel, "Only isolation constants allowed");
		this.isolationLevel = isolationLevel;
	}

	/**
	 * Set the isolation level. Must be one of the isolation constants
	 * in the TransactionDefinition interface. Default is ISOLATION_DEFAULT.
	 * <p>Exclusively designed for use with {@link #PROPAGATION_REQUIRED} or
	 * {@link #PROPAGATION_REQUIRES_NEW} since it only applies to newly started
	 * transactions. Consider switching the "validateExistingTransactions" flag to
	 * "true" on your transaction manager if you'd like isolation level declarations
	 * to get rejected when participating in an existing transaction with a different
	 * isolation level.
	 * <p>Note that a transaction manager that does not support custom isolation levels
	 * will throw an exception when given any other level than {@link #ISOLATION_DEFAULT}.
	 * @throws IllegalArgumentException if the supplied value is not one of the
	 * {@code ISOLATION_} constants
	 * @see #ISOLATION_DEFAULT
	 */
	public final void setIsolationLevel(int isolationLevel) {
		Assert.isTrue(isolationConstants.containsValue(isolationLevel),
				"Only values of isolation constants allowed");
		this.isolationLevel = isolationLevel;
	}

	@Override
	public final int getIsolationLevel() {
		return this.isolationLevel;
	}

	/**
	 * Set the timeout to apply, as number of seconds.
	 * Default is TIMEOUT_DEFAULT (-1).
	 * <p>Exclusively designed for use with {@link #PROPAGATION_REQUIRED} or
	 * {@link #PROPAGATION_REQUIRES_NEW} since it only applies to newly started
	 * transactions.
	 * <p>Note that a transaction manager that does not support timeouts will throw
	 * an exception when given any other timeout than {@link #TIMEOUT_DEFAULT}.
	 * @see #TIMEOUT_DEFAULT
	 */
	public final void setTimeout(int timeout) {
		if (timeout < TIMEOUT_DEFAULT) {
			throw new IllegalArgumentException("Timeout must be a positive integer or TIMEOUT_DEFAULT");
		}
		this.timeout = timeout;
	}

	@Override
	public final int getTimeout() {
		return this.timeout;
	}

	/**
	 * Set whether to optimize as read-only transaction.
	 * Default is "false".
	 * <p>The read-only flag applies to any transaction context, whether backed
	 * by an actual resource transaction ({@link #PROPAGATION_REQUIRED}/
	 * {@link #PROPAGATION_REQUIRES_NEW}) or operating non-transactionally at
	 * the resource level ({@link #PROPAGATION_SUPPORTS}). In the latter case,
	 * the flag will only apply to managed resources within the application,
	 * such as a Hibernate {@code Session}.
	 * <p>This just serves as a hint for the actual transaction subsystem;
	 * it will <i>not necessarily</i> cause failure of write access attempts.
	 * A transaction manager which cannot interpret the read-only hint will
	 * <i>not</i> throw an exception when asked for a read-only transaction.
	 */
	public final void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	@Override
	public final boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * Set the name of this transaction. Default is none.
	 * <p>This will be used as transaction name to be shown in a
	 * transaction monitor, if applicable (for example, WebLogic's).
	 */
	public final void setName(String name) {
		this.name = name;
	}

	@Override
	@Nullable
	public final String getName() {
		return this.name;
	}


	/**
	 * This implementation compares the {@code toString()} results.
	 * @see #toString()
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof TransactionDefinition && toString().equals(other.toString())));
	}

	/**
	 * This implementation returns {@code toString()}'s hash code.
	 * @see #toString()
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Return an identifying description for this transaction definition.
	 * <p>The format matches the one used by
	 * {@link org.springframework.transaction.interceptor.TransactionAttributeEditor},
	 * to be able to feed {@code toString} results into bean properties of type
	 * {@link org.springframework.transaction.interceptor.TransactionAttribute}.
	 * <p>Has to be overridden in subclasses for correct {@code equals}
	 * and {@code hashCode} behavior. Alternatively, {@link #equals}
	 * and {@link #hashCode} can be overridden themselves.
	 * @see #getDefinitionDescription()
	 * @see org.springframework.transaction.interceptor.TransactionAttributeEditor
	 */
	@Override
	public String toString() {
		return getDefinitionDescription().toString();
	}

	/**
	 * Return an identifying description for this transaction definition.
	 * <p>Available to subclasses, for inclusion in their {@code toString()} result.
	 */
	protected final StringBuilder getDefinitionDescription() {
		StringBuilder result = new StringBuilder();
		result.append(getPropagationBehaviorName(this.propagationBehavior));
		result.append(',');
		result.append(getIsolationLevelName(this.isolationLevel));
		if (this.timeout != TIMEOUT_DEFAULT) {
			result.append(',');
			result.append(PREFIX_TIMEOUT).append(this.timeout);
		}
		if (this.readOnly) {
			result.append(',');
			result.append(READ_ONLY_MARKER);
		}
		return result;
	}

	private static String getPropagationBehaviorName(int propagationBehavior) {
		return switch(propagationBehavior) {
			case TransactionDefinition.PROPAGATION_REQUIRED -> "PROPAGATION_REQUIRED";
			case TransactionDefinition.PROPAGATION_SUPPORTS -> "PROPAGATION_SUPPORTS";
			case TransactionDefinition.PROPAGATION_MANDATORY -> "PROPAGATION_MANDATORY";
			case TransactionDefinition.PROPAGATION_REQUIRES_NEW -> "PROPAGATION_REQUIRES_NEW";
			case TransactionDefinition.PROPAGATION_NOT_SUPPORTED -> "PROPAGATION_NOT_SUPPORTED";
			case TransactionDefinition.PROPAGATION_NEVER -> "PROPAGATION_NEVER";
			case TransactionDefinition.PROPAGATION_NESTED -> "PROPAGATION_NESTED";
			default -> throw new IllegalArgumentException("Unsupported propagation behavior: " + propagationBehavior);
		};
	}

	static String getIsolationLevelName(int isolationLevel) {
		return switch(isolationLevel) {
			case TransactionDefinition.ISOLATION_DEFAULT -> "ISOLATION_DEFAULT";
			case TransactionDefinition.ISOLATION_READ_UNCOMMITTED -> "ISOLATION_READ_UNCOMMITTED";
			case TransactionDefinition.ISOLATION_READ_COMMITTED -> "ISOLATION_READ_COMMITTED";
			case TransactionDefinition.ISOLATION_REPEATABLE_READ -> "ISOLATION_REPEATABLE_READ";
			case TransactionDefinition.ISOLATION_SERIALIZABLE -> "ISOLATION_SERIALIZABLE";
			default -> throw new IllegalArgumentException("Unsupported isolation level: " + isolationLevel);
		};
	}

}
