/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.springframework.util.StringUtils;

/**
 * Class to manage context metadata used for the configuration and execution of the call.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 */
public class CallMetaDataContext {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** name of procedure to call **/
	private String procedureName;

	/** name of catalog for call **/
	private String catalogName;

	/** name of schema for call **/
	private String schemaName;

	/** List of SqlParameter objects to be used in call execution */
	private List<SqlParameter> callParameters = new ArrayList<SqlParameter>();

	/** Default name to use for the return value in the output map */
	private String defaultFunctionReturnName = "return";

	/** Actual name to use for the return value in the output map */
	private String actualFunctionReturnName = null;

	/** Set of in parameter names to exclude use for any not listed */
	private Set<String> limitedInParameterNames = new HashSet<String>();

	/** List of SqlParameter names for out parameters */
	private List<String> outParameterNames = new ArrayList<String>();

	/** should we access call parameter meta data info or not */
	private boolean accessCallParameterMetaData = true;

	/** indicates whether this is a procedure or a function **/
	private boolean function;

	/** indicates whether this procedure's return value should be included  **/
	private boolean returnValueRequired;

	/** the provider of call meta data */
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
		return this.actualFunctionReturnName != null ? this.actualFunctionReturnName : this.defaultFunctionReturnName;
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
	public void setProcedureName(String procedureName) {
		this.procedureName = procedureName;
	}

	/**
	 * Get the name of the procedure.
	 */
	public String getProcedureName() {
		return this.procedureName;
	}

	/**
	 * Specify the name of the catalog.
	 */
	public void setCatalogName(String catalogName) {
		this.catalogName = catalogName;
	}

	/**
	 * Get the name of the catalog.
	 */
	public String getCatalogName() {
		return this.catalogName;
	}

	/**
	 * Secify the name of the schema.
	 */
	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	/**
	 * Get the name of the schema.
	 */
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
	 * Specify whether call parameter metadata should be accessed.
	 */
	public void setAccessCallParameterMetaData(boolean accessCallParameterMetaData) {
		this.accessCallParameterMetaData = accessCallParameterMetaData;
	}

	/**
	 * Check whether call parameter metadata should be accessed.
	 */
	public boolean isAccessCallParameterMetaData() {
		return this.accessCallParameterMetaData;
	}


	/**
	 * Create a ReturnResultSetParameter/SqlOutParameter depending on the support provided
	 * by the JDBC driver used for the database in use.
	 * @param parameterName the name of the parameter (also used as the name of the List returned in the output)
	 * @param rowMapper a RowMapper implementation used to map the data returned in the result set
	 * @return the appropriate SqlParameter
	 */
	public SqlParameter createReturnResultSetParameter(String parameterName, RowMapper<?> rowMapper) {
		if (this.metaDataProvider.isReturnResultSetSupported()) {
			return new SqlReturnResultSet(parameterName, rowMapper);
		}
		else {
			if (this.metaDataProvider.isRefCursorSupported()) {
				return new SqlOutParameter(parameterName, this.metaDataProvider.getRefCursorSqlType(), rowMapper);
			}
			else {
				throw new InvalidDataAccessApiUsageException("Return of a ResultSet from a stored procedure is not supported.");
			}
		}
	}

	/**
	 * Get the name of the single out parameter for this call.
	 * If there are multiple parameters, the name of the first one will be returned.
	 */
	public String getScalarOutParameterName() {
		if (isFunction()) {
			return getFunctionReturnName();
		}
		else {
			if (this.outParameterNames.size() > 1) {
				logger.warn("Accessing single output value when procedure has more than one output parameter");
			}
			return (this.outParameterNames.size() > 0 ? this.outParameterNames.get(0) : null);
		}
	}

	/**
	 * Get the List of SqlParameter objects to be used in call execution.
	 */
	public List<SqlParameter> getCallParameters() {
		return this.callParameters;
	}

	/**
	 * Initialize this class with metadata from the database.
	 * @param dataSource the DataSource used to retrieve metadata
	 */
	public void initializeMetaData(DataSource dataSource) {
		this.metaDataProvider = CallMetaDataProviderFactory.createMetaDataProvider(dataSource, this);
	}

	/**
	 * Process the list of parameters provided, and if procedure column metadata is used,
	 * the parameters will be matched against the metadata information and any missing
	 * ones will be automatically included.
	 * @param parameters the list of parameters to use as a base
	 */
	public void processParameters(List<SqlParameter> parameters) {
		this.callParameters = reconcileParameters(parameters);
	}

	/**
	 * Reconcile the provided parameters with available metadata and add new ones where appropriate.
	 */
	protected List<SqlParameter> reconcileParameters(List<SqlParameter> parameters) {
		final List<SqlParameter> declaredReturnParameters = new ArrayList<SqlParameter>();
		final Map<String, SqlParameter> declaredParameters = new LinkedHashMap<String, SqlParameter>();
		boolean returnDeclared = false;
		List<String> outParameterNames = new ArrayList<String>();
		List<String> metaDataParameterNames = new ArrayList<String>();

		// Get the names of the meta data parameters
		for (CallParameterMetaData meta : this.metaDataProvider.getCallParameterMetaData()) {
			if (meta.getParameterType() != DatabaseMetaData.procedureColumnReturn) {
				metaDataParameterNames.add(meta.getParameterName().toLowerCase());
			}
		}

		// Separate implicit return parameters from explicit parameters...
		for (SqlParameter parameter : parameters) {
			if (parameter.isResultsParameter()) {
				declaredReturnParameters.add(parameter);
			}
			else {
				String parameterNameToMatch = this.metaDataProvider.parameterNameToUse(parameter.getName()).toLowerCase();
				declaredParameters.put(parameterNameToMatch, parameter);
				if (parameter instanceof SqlOutParameter) {
					outParameterNames.add(parameter.getName());
					if (isFunction() && !metaDataParameterNames.contains(parameterNameToMatch)) {
						if (!returnDeclared) {
							if (logger.isDebugEnabled()) {
								logger.debug("Using declared out parameter '" + parameter.getName() + "' for function return value");
							}
							setFunctionReturnName(parameter.getName());
							returnDeclared = true;
						}
					}
				}
			}
		}
		setOutParameterNames(outParameterNames);

		List<SqlParameter> workParameters = new ArrayList<SqlParameter>();
		workParameters.addAll(declaredReturnParameters);

		if (!this.metaDataProvider.isProcedureColumnMetaDataUsed()) {
			workParameters.addAll(declaredParameters.values());
			return workParameters;
		}

		Map<String, String> limitedInParamNamesMap = new HashMap<String, String>(this.limitedInParameterNames.size());
		for (String limitedParameterName : this.limitedInParameterNames) {
			limitedInParamNamesMap.put(
					this.metaDataProvider.parameterNameToUse(limitedParameterName).toLowerCase(), limitedParameterName);
		}

		for (CallParameterMetaData meta : this.metaDataProvider.getCallParameterMetaData()) {
			String parNameToCheck = null;
			if (meta.getParameterName() != null) {
				parNameToCheck = this.metaDataProvider.parameterNameToUse(meta.getParameterName()).toLowerCase();
			}
			String parNameToUse = this.metaDataProvider.parameterNameToUse(meta.getParameterName());
			if (declaredParameters.containsKey(parNameToCheck) ||
					(meta.getParameterType() == DatabaseMetaData.procedureColumnReturn && returnDeclared)) {
				SqlParameter parameter;
				if (meta.getParameterType() == DatabaseMetaData.procedureColumnReturn) {
					parameter = declaredParameters.get(getFunctionReturnName());
					if (parameter == null && getOutParameterNames().size() > 0) {
						parameter = declaredParameters.get(getOutParameterNames().get(0).toLowerCase());
					}
					if (parameter == null) {
						throw new InvalidDataAccessApiUsageException(
								"Unable to locate declared parameter for function return value - " +
								" add a SqlOutParameter with name \"" + getFunctionReturnName() +"\"");
					}
					else {
						setFunctionReturnName(parameter.getName());
					}
				}
				else {
					parameter = declaredParameters.get(parNameToCheck);
				}
				if (parameter != null) {
					workParameters.add(parameter);
					if (logger.isDebugEnabled()) {
						logger.debug("Using declared parameter for: " +
								(parNameToUse == null ? getFunctionReturnName() : parNameToUse));
					}
				}
			}
			else {
				if (meta.getParameterType() == DatabaseMetaData.procedureColumnReturn) {
					if (!isFunction() && !isReturnValueRequired() &&
							this.metaDataProvider.byPassReturnParameter(meta.getParameterName())) {
						if (logger.isDebugEnabled()) {
							logger.debug("Bypassing metadata return parameter for: " + meta.getParameterName());
						}
					}
					else {
						String returnNameToUse =(StringUtils.hasLength(meta.getParameterName()) ?
								parNameToUse : getFunctionReturnName());
						workParameters.add(this.metaDataProvider.createDefaultOutParameter(returnNameToUse, meta));
						if (isFunction()) {
							setFunctionReturnName(returnNameToUse);
							outParameterNames.add(returnNameToUse);
						}
						if (logger.isDebugEnabled()) {
							logger.debug("Added metadata return parameter for: " + returnNameToUse);
						}
					}
				}
				else {
					if (meta.getParameterType() == DatabaseMetaData.procedureColumnOut) {
						workParameters.add(this.metaDataProvider.createDefaultOutParameter(parNameToUse, meta));
						outParameterNames.add(parNameToUse);
						if (logger.isDebugEnabled()) {
							logger.debug("Added metadata out parameter for: " + parNameToUse);
						}
					}
					else if (meta.getParameterType() == DatabaseMetaData.procedureColumnInOut) {
						workParameters.add(this.metaDataProvider.createDefaultInOutParameter(parNameToUse, meta));
						outParameterNames.add(parNameToUse);
						if (logger.isDebugEnabled()) {
							logger.debug("Added metadata in out parameter for: " + parNameToUse);
						}
					}
					else {
						if (this.limitedInParameterNames.isEmpty() ||
								limitedInParamNamesMap.containsKey(parNameToUse.toLowerCase())) {
							workParameters.add(this.metaDataProvider.createDefaultInParameter(parNameToUse, meta));
							if (logger.isDebugEnabled()) {
								logger.debug("Added metadata in parameter for: " + parNameToUse);
							}
						}
						else {
							if (logger.isDebugEnabled()) {
								logger.debug("Limited set of parameters " + limitedInParamNamesMap.keySet() +
										" skipped parameter for: " + parNameToUse);
							}
						}
					}
				}
			}
		}

		return workParameters;
	}

	/**
	 * Match input parameter values with the parameters declared to be used in the call.
	 * @param parameterSource the input values
	 * @return a Map containing the matched parameter names with the value taken from the input
	 */
	public Map<String, Object> matchInParameterValuesWithCallParameters(SqlParameterSource parameterSource) {
		// For parameter source lookups we need to provide case-insensitive lookup support
		// since the database metadata is not necessarily providing case sensitive parameter names.
		Map<String, String> caseInsensitiveParameterNames =
				SqlParameterSourceUtils.extractCaseInsensitiveParameterNames(parameterSource);

		Map<String, String> callParameterNames = new HashMap<String, String>(this.callParameters.size());
		Map<String, Object> matchedParameters = new HashMap<String, Object>(this.callParameters.size());
		for (SqlParameter parameter : this.callParameters) {
			if (parameter.isInputValueProvided()) {
				String parameterName = parameter.getName();
				String parameterNameToMatch = this.metaDataProvider.parameterNameToUse(parameterName);
				if (parameterNameToMatch != null) {
					callParameterNames.put(parameterNameToMatch.toLowerCase(), parameterName);
				}
				if (parameterName != null) {
					if (parameterSource.hasValue(parameterName)) {
						matchedParameters.put(parameterName, SqlParameterSourceUtils.getTypedValue(parameterSource, parameterName));
					}
					else {
						String lowerCaseName = parameterName.toLowerCase();
						if (parameterSource.hasValue(lowerCaseName)) {
							matchedParameters.put(parameterName, SqlParameterSourceUtils.getTypedValue(parameterSource, lowerCaseName));
						}
						else {
							String englishLowerCaseName = parameterName.toLowerCase(Locale.ENGLISH);
							if (parameterSource.hasValue(englishLowerCaseName)) {
								matchedParameters.put(parameterName, SqlParameterSourceUtils.getTypedValue(parameterSource, englishLowerCaseName));
							}
							else {
								String propertyName = JdbcUtils.convertUnderscoreNameToPropertyName(parameterName);
								if (parameterSource.hasValue(propertyName)) {
									matchedParameters.put(parameterName, SqlParameterSourceUtils.getTypedValue(parameterSource, propertyName));
								}
								else {
									if (caseInsensitiveParameterNames.containsKey(lowerCaseName)) {
										String sourceName = caseInsensitiveParameterNames.get(lowerCaseName);
										matchedParameters.put(parameterName, SqlParameterSourceUtils.getTypedValue(parameterSource, sourceName));
									}
									else {
										logger.warn("Unable to locate the corresponding parameter value for '" + parameterName +
												"' within the parameter values provided: " + caseInsensitiveParameterNames.values());
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
		if (!this.metaDataProvider.isProcedureColumnMetaDataUsed()) {
			return inParameters;
		}
		Map<String, String> callParameterNames = new HashMap<String, String>(this.callParameters.size());
		for (SqlParameter parameter : this.callParameters) {
			if (parameter.isInputValueProvided()) {
				String parameterName =  parameter.getName();
				String parameterNameToMatch = this.metaDataProvider.parameterNameToUse(parameterName);
				if (parameterNameToMatch != null) {
					callParameterNames.put(parameterNameToMatch.toLowerCase(), parameterName);
				}
			}
		}
		Map<String, Object> matchedParameters = new HashMap<String, Object>(inParameters.size());
		for (String parameterName : inParameters.keySet()) {
			String parameterNameToMatch = this.metaDataProvider.parameterNameToUse(parameterName);
			String callParameterName = callParameterNames.get(parameterNameToMatch.toLowerCase());
			if (callParameterName == null) {
				if (logger.isDebugEnabled()) {
					Object value = inParameters.get(parameterName);
					if (value instanceof SqlParameterValue) {
						value = ((SqlParameterValue)value).getValue();
					}
					if (value != null) {
						logger.debug("Unable to locate the corresponding IN or IN-OUT parameter for \"" + parameterName +
								"\" in the parameters used: " + callParameterNames.keySet());
					}
				}
			}
			else {
				matchedParameters.put(callParameterName, inParameters.get(parameterName));
			}
		}
		if (matchedParameters.size() < callParameterNames.size()) {
			for (String parameterName : callParameterNames.keySet()) {
				String parameterNameToMatch = this.metaDataProvider.parameterNameToUse(parameterName);
				String callParameterName = callParameterNames.get(parameterNameToMatch.toLowerCase());
				if (!matchedParameters.containsKey(callParameterName)) {
					logger.warn("Unable to locate the corresponding parameter value for '" + parameterName +
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
		Map<String, Object> matchedParameters = new HashMap<String, Object>(parameterValues.length);
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
	 * Build the call string based on configuration and metadata information.
	 * @return the call string to be used
	 */
	public String createCallString() {
		String callString;
		int parameterCount = 0;
		String catalogNameToUse;
		String schemaNameToUse;

		// For Oracle where catalogs are not supported we need to reverse the schema name
		// and the catalog name since the cataog is used for the package name
		if (this.metaDataProvider.isSupportsSchemasInProcedureCalls() &&
				!this.metaDataProvider.isSupportsCatalogsInProcedureCalls()) {
			schemaNameToUse = this.metaDataProvider.catalogNameToUse(this.getCatalogName());
			catalogNameToUse = this.metaDataProvider.schemaNameToUse(this.getSchemaName());
		}
		else {
			catalogNameToUse = this.metaDataProvider.catalogNameToUse(this.getCatalogName());
			schemaNameToUse = this.metaDataProvider.schemaNameToUse(this.getSchemaName());
		}
		String procedureNameToUse = this.metaDataProvider.procedureNameToUse(this.getProcedureName());
		if (this.isFunction() || this.isReturnValueRequired()) {
			callString = "{? = call " +
					(StringUtils.hasLength(catalogNameToUse) ? catalogNameToUse + "." : "") +
					(StringUtils.hasLength(schemaNameToUse) ? schemaNameToUse + "." : "") +
					procedureNameToUse + "(";
			parameterCount = -1;
		}
		else {
			callString = "{call " +
					(StringUtils.hasLength(catalogNameToUse) ? catalogNameToUse + "." : "") +
					(StringUtils.hasLength(schemaNameToUse) ? schemaNameToUse + "." : "") +
					procedureNameToUse + "(";
		}
		for (SqlParameter parameter : this.callParameters) {
			if (!(parameter.isResultsParameter())) {
				if (parameterCount > 0) {
					callString += ", ";
				}
				if (parameterCount >= 0) {
					callString += "?";
				}
				parameterCount++;
			}
		}
		callString += ")}";

		return callString;
	}

}
