/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.namedparam;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds information about a parsed SQL statement.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ParsedSql {

	final String originalSql;

	final List<String> parameterNames = new ArrayList<>();

	private final List<int[]> parameterIndexes = new ArrayList<>();

	private int namedParameterCount;

	private int unnamedParameterCount;

	private int totalParameterCount;


	/**
	 * Create a new instance of the {@link ParsedSql} class.
	 * @param originalSql the SQL statement that is being (or is to be) parsed
	 */
	ParsedSql(String originalSql) {
		this.originalSql = originalSql;
	}


	/**
	 * Add a named parameter parsed from this SQL statement.
	 * @param parameterName the name of the parameter
	 * @param startIndex the start index in the original SQL String
	 * @param endIndex the end index in the original SQL String
	 */
	void addNamedParameter(String parameterName, int startIndex, int endIndex) {
		this.parameterNames.add(parameterName);
		this.parameterIndexes.add(new int[] {startIndex, endIndex});
	}

	/**
	 * Return all the parameters (bind variables) in the parsed SQL statement.
	 * Repeated occurrences of the same parameter name are included only once.
	 * @since 7.1.1
	 */
	public Set<String> getParameterNames() {
		return new LinkedHashSet<>(this.parameterNames);
	}

	/**
	 * Return the parameter indexes for the specified parameter.
	 * @param parameterPosition the position of the parameter
	 * (as index in the parameter names List)
	 * @return the start index and end index, combined into
	 * an int array of length 2
	 */
	int[] getParameterIndexes(int parameterPosition) {
		return this.parameterIndexes.get(parameterPosition);
	}

	/**
	 * Set the count of named parameters in the SQL statement.
	 * Each parameter name counts once; repeated occurrences do not count here.
	 */
	void setNamedParameterCount(int namedParameterCount) {
		this.namedParameterCount = namedParameterCount;
	}

	/**
	 * Return the count of named parameters in the SQL statement.
	 * Each parameter name counts once; repeated occurrences do not count here.
	 * @since 7.1.1
	 */
	public int getNamedParameterCount() {
		return this.namedParameterCount;
	}

	/**
	 * Set the count of all the unnamed parameters in the SQL statement.
	 */
	void setUnnamedParameterCount(int unnamedParameterCount) {
		this.unnamedParameterCount = unnamedParameterCount;
	}

	/**
	 * Return the count of all the unnamed parameters in the SQL statement.
	 * @since 7.1.1
	 */
	public int getUnnamedParameterCount() {
		return this.unnamedParameterCount;
	}

	/**
	 * Set the total count of all the parameters in the SQL statement.
	 * Repeated occurrences of the same parameter name do count here.
	 */
	void setTotalParameterCount(int totalParameterCount) {
		this.totalParameterCount = totalParameterCount;
	}

	/**
	 * Return the total count of all the parameters in the SQL statement.
	 * Repeated occurrences of the same parameter name do count here.
	 */
	int getTotalParameterCount() {
		return this.totalParameterCount;
	}


	/**
	 * Exposes the original SQL String.
	 */
	@Override
	public String toString() {
		return this.originalSql;
	}

}
