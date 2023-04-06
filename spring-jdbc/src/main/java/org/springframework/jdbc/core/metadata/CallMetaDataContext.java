/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Class to manage context meta-data used for the configuration
 * and execution of a stored procedure call.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Kiril Nugmanov
 * @since 2.5
 */
public class CallMetaDataContext {

	// Logger available to subclasses
	protected final Log logger = LogFactory.getLog(getClass());

	// Name of procedure to call
	@Nullable
	private String procedureName;

	// Name of catalog for call
	@Nullable
	private String catalogName;

	// Name of schema for call
	@Nullable
	private String schemaName;

	// List of SqlParameter objects to be used in call execution
	private List<SqlParameter> callParameters = new ArrayList<>();

	// Actual name to use for the return value in the output map
	@Nullable
	private String actualFunctionReturnName;

	// Set of in parameter names to exclude use for any not listed
	private Set<String> limitedInParameterNames = new HashSet<>();

	// List of SqlParameter names for out parameters
	private List<String> outParameterNames = new ArrayList<>();

	// Indicates whether this is a procedure or a function
	private boolean function = false;

	// Indicates whether this procedure's return value should be included
	private boolean returnValueRequired = false;

	// Should we access call parameter meta-data info or not
	private boolean accessCallParameterMetaData = true;

	// Should we bind parameter by name
	private boolean namedBinding;

	// The provider of call meta-data
	@Nullable
	private CallMetaDataProvider metaDataProvider;


	/**
	 * Specify the name used for the return value of the function.
	 */
	public void setFunctionReturnName(String functionReturnName) {
		this.actualFunctionReturnName = functionReturnName;
	}

	/**
	 * Get the name used for the return value of the function.
	 */
	public String getFunctionReturnName() {
		return (this.actualFunctionReturnName != null ? this.actualFunctionReturnName : "return");
	}

	/**
	 * Specify a limited set of in parameters to be used.
	 */
	public void setLimitedInParameterNames(Set<String> limitedInParameterNames) {
		this.limitedInParameterNames = limitedInParameterNames;
	}

	/**
	 * Get a limited set of in parameters to be used.
	 */
	public Set<String> getLimitedInParameterNames() {
		return this.limitedInParameterNames;
	}

	/**
	 * Specify the names of the out parameters.
	 */
	public void setOutParameterNames(List<String> outParameterNames) {
		this.outParameterNames = outParameterNames;
	}

	/**
	 * Get a list of the out parameter names.
	 */
	public List<String> getOutParameterNames() {
		return this.outParameterNames;
	}

	/**
	 * Specify the name of the procedure.
	 */
	public void setProcedureName(@Nullable String procedureName) {
		this.procedureName = procedureName;
	}

	/**
	 * Get the name of the procedure.
	 */
	@Nullable
	public String getProcedureName() {
		return this.procedureName;
	}

	/**
	 * Specify the name of the catalog.
	 */
	public void setCatalogName(@Nullable String catalogName) {
		this.catalogName = catalogName;
	}

	/**
	 * Get the name of the catalog.
	 */
	@Nullable
	public String getCatalogName() {
		return this.catalogName;
	}

	/**
	 * Specify the name of the schema.
	 */
	public void setSchemaName(@Nullable String schemaName) {
		this.schemaName = schemaName;
	}

	/**
	 * Get the name of the schema.
	 */
	@Nullable
	public String getSchemaName() {
		return this.schemaName;
	}

	/**
	 * Specify whether this call is a function call.
	 */
	public void setFunction(boolean function) {
		this.function = function;
	}

	/**
	 * Check whether this call is a function call.
	 */
	public boolean isFunction() {
		return this.function;
	}

	/**
	 * Specify whether a return value is required.
	 */
	public void setReturnValueRequired(boolean returnValueRequired) {
		this.returnValueRequired = returnValueRequired;
	}

	/**
	 * Check whether a return value is required.
	 */
	public boolean isReturnValueRequired() {
		return this.returnValueRequired;
	}

	/**
	 * Specify whether call parameter meta-data should be accessed.
	 */
	public void setAccessCallParameterMetaData(boolean accessCallParameterMetaData) {
		this.accessCallParameterMetaData = accessCallParameterMetaData;
	}

	/**
	 * Check whether call parameter meta-data should be accessed.
	 */
	public boolean isAccessCallParameterMetaData() {
		return this.accessCallParameterMetaData;
	}

	/**
	 * Specify whether parameters should be bound by name.
	 * @since 4.2
	 */
	public void setNamedBinding(boolean namedBinding) {
		this.namedBinding = namedBinding;
	}

	/**
	 * Check whether parameters should be bound by name.
	 * @since 4.2
	 */
	public boolean isNamedBinding() {
		return this.namedBinding;
	}


	/**
	 * Initialize this class with meta-data from the database.
	 * @param dataSource the DataSource used to retrieve meta-data
	 */
	public void initializeMetaData(DataSource dataSource) {
		this.metaDataProvider = CallMetaDataProviderFactory.createMetaDataProvider(dataSource, this);
	}

	private CallMetaDataProvider obtainMetaDataProvider() {
		Assert.state(this.metaDataProvider != null, "No CallMetaDataProvider - call initializeMetaData first");
		return this.metaDataProvider;
	}

	/**
	 * Create a ReturnResultSetParameter/SqlOutParameter depending on the support provided
	 * by the JDBC driver used for the database in use.
	 * @param parameterName the name of the parameter (also used as the name of the List returned in the output)
	 * @param rowMapper a RowMapper implementation used to map the data returned in the result set
	 * @return the appropriate SqlParameter
	 */
	public SqlParameter createReturnResultSetParameter(String parameterName, RowMapper<?> rowMapper) {
		CallMetaDataProvider provider = obtainMetaDataProvider();
		if (provider.isReturnResultSetSupported()) {
			return new SqlReturnResultSet(parameterName, rowMapper);
		}
		else {
			if (provider.isRefCursorSupported()) {
				return new SqlOutParameter(parameterName, provider.getRefCursorSqlType(), rowMapper);
			}
			else {
				throw new InvalidDataAccessApiUsageException(
						"Return of a ResultSet from a stored procedure is not supported");
			}
		}
	}

	/**
	 * Get the name of the single out parameter for this call.
	 * If there are multiple parameters, the name of the first one will be returned.
	 */
	@Nullable
	public String getScalarOutParameterName() {
		if (isFunction()) {
			return getFunctionReturnName();
		}
		else {
			if (this.outParameterNames.size() > 1) {
				logger.info("Accessing single output value when procedure has more than one output parameter");
			}
			return (!this.outParameterNames.isEmpty() ? this.outParameterNames.get(0) : null);
		}
	}

	/**
	 * Get the List of SqlParameter objects to be used in call execution.
	 */
	public List<SqlParameter> getCallParameters() {
		return this.callParameters;
	}

	/**
	 * Process the list of parameters provided, and if procedure column meta-data is used,
	 * the parameters will be matched against the meta-data information and any missing
	 * ones will be automatically included.
	 * @param parameters the list of parameters to use as a base
	 */
	public void processParameters(List<SqlParameter> parameters) {
		this.callParameters = reconcileParameters(parameters);
	}

	/**
	 * Reconcile the provided parameters with available meta-data and add new ones where appropriate.
	 */
	protected List<SqlParameter> reconcileParameters(List<SqlParameter> parameters) {
		CallMetaDataProvider provider = obtainMetaDataProvider();

		final List<SqlParameter> declaredReturnParams = new ArrayList<>();
		final Map<String, SqlParameter> declaredParams = new LinkedHashMap<>();
		boolean returnDeclared = false;
		List<String> outParamNames = new ArrayList<>();
		List<String> metaDataParamNames = new ArrayList<>();

		// Get the names of the meta-data parameters
		for (CallParameterMetaData meta : provider.getCallParameterMetaData()) {
			if (!meta.isReturnParameter()) {
				metaDataParamNames.add(lowerCase(meta.getParameterName()));
			}
		}

		// Separate implicit return parameters from explicit parameters...
		for (SqlParameter param : parameters) {
			if (param.isResultsParameter()) {
				declaredReturnParams.add(param);
			}
			else {
				String paramName = param.getName();
				if (paramName == null) {
					throw new IllegalArgumentException("Anonymous parameters not supported for calls - " +
							"please specify a name for the parameter of SQL type " + param.getSqlType());
				}
				String paramNameToMatch = lowerCase(provider.parameterNameToUse(paramName));
				declaredParams.put(paramNameToMatch, param);
				if (param instanceof SqlOutParameter) {
					outParamNames.add(paramName);
					if (isFunction() && !metaDataParamNames.contains(paramNameToMatch) && !returnDeclared) {
						if (logger.isDebugEnabled()) {
							logger.debug("Using declared out parameter '" + paramName +
									"' for function return value");
						}
						this.actualFunctionReturnName = paramName;
						returnDeclared = true;
					}
				}
			}
		}
		setOutParameterNames(outParamNames);

		List<SqlParameter> workParams = new ArrayList<>(declaredReturnParams);
		if (!provider.isProcedureColumnMetaDataUsed()) {
			workParams.addAll(declaredParams.values());
			return workParams;
		}

		Map<String, String> limitedInParamNamesMap = CollectionUtils.newHashMap(this.limitedInParameterNames.size());
		for (String limitedParamName : this.limitedInParameterNames) {
			limitedInParamNamesMap.put(lowerCase(provider.parameterNameToUse(limitedParamName)), limitedParamName);
		}

		for (CallParameterMetaData meta : provider.getCallParameterMetaData()) {
			String paramName = meta.getParameterName();
			String paramNameToCheck = null;
			if (paramName != null) {
				paramNameToCheck = lowerCase(provider.parameterNameToUse(paramName));
			}
			String paramNameToUse = provider.parameterNameToUse(paramName);
			if (declaredParams.containsKey(paramNameToCheck) || (meta.isReturnParameter() && returnDeclared)) {
				SqlParameter param;
				if (meta.isReturnParameter()) {
					param = declaredParams.get(getFunctionReturnName());
					if (param == null && !getOutParameterNames().isEmpty()) {
						param = declaredParams.get(getOutParameterNames().get(0).toLowerCase());
					}
					if (param == null) {
						throw new InvalidDataAccessApiUsageException(
								"Unable to locate declared parameter for function return value - " +
								" add an SqlOutParameter with name '" + getFunctionReturnName() + "'");
					}
					else {
						this.actualFunctionReturnName = param.getName();
					}
				}
				else {
					param = declaredParams.get(paramNameToCheck);
				}
				if (param != null) {
					workParams.add(param);
					if (logger.isDebugEnabled()) {
						logger.debug("Using declared parameter for '" +
								(paramNameToUse != null ? paramNameToUse : getFunctionReturnName()) + "'");
					}
				}
			}
			else {
				if (meta.isReturnParameter()) {
					// DatabaseMetaData.procedureColumnReturn or possibly procedureColumnResult
					if (!isFunction() && !isReturnValueRequired() && paramName != null &&
							provider.byPassReturnParameter(paramName)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Bypassing meta-data return parameter for '" + paramName + "'");
						}
					}
					else {
						String returnNameToUse =
								(StringUtils.hasLength(paramNameToUse) ? paramNameToUse : getFunctionReturnName());
						workParams.add(provider.createDefaultOutParameter(returnNameToUse, meta));
						if (isFunction()) {
							this.actualFunctionReturnName = returnNameToUse;
							outParamNames.add(returnNameToUse);
						}
						if (logger.isDebugEnabled()) {
							logger.debug("Added meta-data return parameter for '" + returnNameToUse + "'");
						}
					}
				}
				else {
					if (paramNameToUse == null) {
						paramNameToUse = "";
					}
					if (meta.getParameterType() == DatabaseMetaData.procedureColumnOut) {
						workParams.add(provider.createDefaultOutParameter(paramNameToUse, meta));
						outParamNames.add(paramNameToUse);
						if (logger.isDebugEnabled()) {
							logger.debug("Added meta-data out parameter for '" + paramNameToUse + "'");
						}
					}
					else if (meta.getParameterType() == DatabaseMetaData.procedureColumnInOut) {
						workParams.add(provider.createDefaultInOutParameter(paramNameToUse, meta));
						outParamNames.add(paramNameToUse);
						if (logger.isDebugEnabled()) {
							logger.debug("Added meta-data in-out parameter for '" + paramNameToUse + "'");
						}
					}
					else {
						// DatabaseMetaData.procedureColumnIn or possibly procedureColumnUnknown
						if (this.limitedInParameterNames.isEmpty() ||
								limitedInParamNamesMap.containsKey(lowerCase(paramNameToUse))) {
							workParams.add(provider.createDefaultInParameter(paramNameToUse, meta));
							if (logger.isDebugEnabled()) {
								logger.debug("Added meta-data in parameter for '" + paramNameToUse + "'");
							}
						}
						else {
							if (logger.isDebugEnabled()) {
								logger.debug("Limited set of parameters " + limitedInParamNamesMap.keySet() +
										" skipped parameter for '" + paramNameToUse + "'");
							}
						}
					}
				}
			}
		}

		return workParams;
	}

	/**
	 * Match input parameter values with the parameters declared to be used in the call.
	 * @param parameterSource the input values
	 * @return a Map containing the matched parameter names with the value taken from the input
	 */
	public Map<String, Object> matchInParameterValuesWithCallParameters(SqlParameterSource parameterSource) {
		// For parameter source lookups we need to provide case-insensitive lookup support
		// since the database meta-data is not necessarily providing case-sensitive parameter names.
		Map<String, String> caseInsensitiveParameterNames =
				SqlParameterSourceUtils.extractCaseInsensitiveParameterNames(parameterSource);

		Map<String, String> callParameterNames = CollectionUtils.newHashMap(this.callParameters.size());
		Map<String, Object> matchedParameters = CollectionUtils.newHashMap(this.callParameters.size());
		for (SqlParameter parameter : this.callParameters) {
			if (parameter.isInputValueProvided()) {
				String parameterName = parameter.getName();
				String parameterNameToMatch = obtainMetaDataProvider().parameterNameToUse(parameterName);
				if (parameterNameToMatch != null) {
					callParameterNames.put(parameterNameToMatch.toLowerCase(), parameterName);
				}
				if (parameterName != null) {
					if (parameterSource.hasValue(parameterName)) {
						matchedParameters.put(parameterName,
								SqlParameterSourceUtils.getTypedValue(parameterSource, parameterName));
					}
					else {
						String lowerCaseName = parameterName.toLowerCase();
						if (parameterSource.hasValue(lowerCaseName)) {
							matchedParameters.put(parameterName,
									SqlParameterSourceUtils.getTypedValue(parameterSource, lowerCaseName));
						}
						else {
							String englishLowerCaseName = parameterName.toLowerCase(Locale.ENGLISH);
							if (parameterSource.hasValue(englishLowerCaseName)) {
								matchedParameters.put(parameterName,
										SqlParameterSourceUtils.getTypedValue(parameterSource, englishLowerCaseName));
							}
							else {
								String propertyName = JdbcUtils.convertUnderscoreNameToPropertyName(parameterName);
								if (parameterSource.hasValue(propertyName)) {
									matchedParameters.put(parameterName,
											SqlParameterSourceUtils.getTypedValue(parameterSource, propertyName));
								}
								else {
									if (caseInsensitiveParameterNames.containsKey(lowerCaseName)) {
										String sourceName = caseInsensitiveParameterNames.get(lowerCaseName);
										matchedParameters.put(parameterName,
												SqlParameterSourceUtils.getTypedValue(parameterSource, sourceName));
									}
									else if (logger.isInfoEnabled()) {
										logger.info("Unable to locate the corresponding parameter value for '" +
												parameterName + "' within the parameter values provided: " +
												caseInsensitiveParameterNames.values());
									}
								}
							}
						}
					}
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Matching " + caseInsensitiveParameterNames.values() + " with " + callParameterNames.values());
			logger.debug("Found match for " + matchedParameters.keySet());
		}
		return matchedParameters;
	}

	/**
	 * Match input parameter values with the parameters declared to be used in the call.
	 * @param inParameters the input values
	 * @return a Map containing the matched parameter names with the value taken from the input
	 */
	public Map<String, ?> matchInParameterValuesWithCallParameters(Map<String, ?> inParameters) {
		CallMetaDataProvider provider = obtainMetaDataProvider();
		if (!provider.isProcedureColumnMetaDataUsed()) {
			return inParameters;
		}

		Map<String, String> callParameterNames = CollectionUtils.newHashMap(this.callParameters.size());
		for (SqlParameter parameter : this.callParameters) {
			if (parameter.isInputValueProvided()) {
				String parameterName =  parameter.getName();
				String parameterNameToMatch = provider.parameterNameToUse(parameterName);
				if (parameterNameToMatch != null) {
					callParameterNames.put(parameterNameToMatch.toLowerCase(), parameterName);
				}
			}
		}

		Map<String, Object> matchedParameters = CollectionUtils.newHashMap(inParameters.size());
		inParameters.forEach((parameterName, parameterValue) -> {
			String parameterNameToMatch = provider.parameterNameToUse(parameterName);
			String callParameterName = callParameterNames.get(lowerCase(parameterNameToMatch));
			if (callParameterName == null) {
				if (logger.isDebugEnabled()) {
					Object value = parameterValue;
					if (value instanceof SqlParameterValue sqlParameterValue) {
						value = sqlParameterValue.getValue();
					}
					if (value != null) {
						logger.debug("Unable to locate the corresponding IN or IN-OUT parameter for \"" +
								parameterName + "\" in the parameters used: " + callParameterNames.keySet());
					}
				}
			}
			else {
				matchedParameters.put(callParameterName, parameterValue);
			}
		});

		if (matchedParameters.size() < callParameterNames.size()) {
			for (String parameterName : callParameterNames.keySet()) {
				String parameterNameToMatch = provider.parameterNameToUse(parameterName);
				String callParameterName = callParameterNames.get(lowerCase(parameterNameToMatch));
				if (!matchedParameters.containsKey(callParameterName) && logger.isInfoEnabled()) {
					logger.info("Unable to locate the corresponding parameter value for '" + parameterName +
							"' within the parameter values provided: " + inParameters.keySet());
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Matching " + inParameters.keySet() + " with " + callParameterNames.values());
			logger.debug("Found match for " + matchedParameters.keySet());
		}
		return matchedParameters;
	}

	public Map<String, ?> matchInParameterValuesWithCallParameters(Object[] parameterValues) {
		Map<String, Object> matchedParameters = CollectionUtils.newHashMap(parameterValues.length);
		int i = 0;
		for (SqlParameter parameter : this.callParameters) {
			if (parameter.isInputValueProvided()) {
				String parameterName =  parameter.getName();
				matchedParameters.put(parameterName, parameterValues[i++]);
			}
		}
		return matchedParameters;
	}

	/**
	 * Build the call string based on configuration and meta-data information.
	 * @return the call string to be used
	 */
	public String createCallString() {
		Assert.state(this.metaDataProvider != null, "No CallMetaDataProvider available");

		StringBuilder callString;
		int parameterCount = 0;
		String catalogNameToUse;
		String schemaNameToUse;

		// For Oracle where catalogs are not supported we need to reverse the schema name
		// and the catalog name since the catalog is used for the package name
		if (this.metaDataProvider.isSupportsSchemasInProcedureCalls() &&
				!this.metaDataProvider.isSupportsCatalogsInProcedureCalls()) {
			schemaNameToUse = this.metaDataProvider.catalogNameToUse(getCatalogName());
			catalogNameToUse = this.metaDataProvider.schemaNameToUse(getSchemaName());
		}
		else {
			catalogNameToUse = this.metaDataProvider.catalogNameToUse(getCatalogName());
			schemaNameToUse = this.metaDataProvider.schemaNameToUse(getSchemaName());
		}

		if (isFunction() || isReturnValueRequired()) {
			callString = new StringBuilder("{? = call ");
			parameterCount = -1;
		}
		else {
			callString = new StringBuilder("{call ");
		}

		if (StringUtils.hasLength(catalogNameToUse)) {
			callString.append(catalogNameToUse).append('.');
		}
		if (StringUtils.hasLength(schemaNameToUse)) {
			callString.append(schemaNameToUse).append('.');
		}
		callString.append(this.metaDataProvider.procedureNameToUse(getProcedureName()));
		callString.append('(');

		for (SqlParameter parameter : this.callParameters) {
			if (!parameter.isResultsParameter()) {
				if (parameterCount > 0) {
					callString.append(", ");
				}
				if (parameterCount >= 0) {
					callString.append(createParameterBinding(parameter));
				}
				parameterCount++;
			}
		}
		callString.append(")}");

		return callString.toString();
	}

	/**
	 * Build the parameter binding fragment.
	 * @param parameter call parameter
	 * @return parameter binding fragment
	 * @since 4.2
	 */
	protected String createParameterBinding(SqlParameter parameter) {
		return (isNamedBinding() ? parameter.getName() + " => ?" : "?");
	}

	private static String lowerCase(@Nullable String paramName) {
		return (paramName != null ? paramName.toLowerCase() : "");
	}

}
