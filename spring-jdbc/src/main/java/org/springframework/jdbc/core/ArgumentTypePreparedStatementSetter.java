/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.core;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.lang.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

/**
 * Simple adapter for {@link PreparedStatementSetter} that applies
 * given arrays of arguments and JDBC argument types.
 *
 * <p>参数类型预处理语句Setter(ArgumentTypePreparedStatementSetter)
 * <p>适用于{@link PreparedStatementSetter}的简单适配器给定参数数组和JDBC参数类型。
 *
 * @author Juergen Hoeller
 * @since 3.2.3
 */
public class ArgumentTypePreparedStatementSetter implements PreparedStatementSetter, ParameterDisposer {

	/**
	 * 入参
	 */
	@Nullable
	private final Object[] args;

	/**
	 * 入参类型
	 */
	@Nullable
	private final int[] argTypes;


	/**
	 * Create a new ArgTypePreparedStatementSetter for the given arguments.
	 * <p>为给定的参数创建一个新的ArgTypePreparedStateSetter
	 *
	 * @param args     the arguments to set
	 * @param argTypes the corresponding SQL types of the arguments
	 */
	public ArgumentTypePreparedStatementSetter(@Nullable Object[] args, @Nullable int[] argTypes) {
		if ((args != null && argTypes == null) || (args == null && argTypes != null) ||
				(args != null && args.length != argTypes.length)) {
			throw new InvalidDataAccessApiUsageException("args and argTypes parameters must match");
		}
		this.args = args;
		this.argTypes = argTypes;
	}


	@Override
	public void setValues(PreparedStatement ps) throws SQLException {
		// 参数下标
		int parameterPosition = 1;
		// 参数及参数类型非空
		if (this.args != null && this.argTypes != null) {
			// 遍历参数做类型匹配及转换
			for (int i = 0; i < this.args.length; i++) {
				Object arg = this.args[i];
				// 参数为集合类型,则递归遍历解析参数属性
				if (arg instanceof Collection && this.argTypes[i] != Types.ARRAY) {
					Collection<?> entries = (Collection<?>) arg;
					for (Object entry : entries) {
						if (entry instanceof Object[]) {
							Object[] valueArray = ((Object[]) entry);
							for (Object argValue : valueArray) {
								// 参数及类型匹配处理
								doSetValue(ps, parameterPosition, this.argTypes[i], argValue);
								parameterPosition++;
							}
						} else {
							// 参数及类型匹配处理
							doSetValue(ps, parameterPosition, this.argTypes[i], entry);
							parameterPosition++;
						}
					}
				} else {
					// 参数及类型匹配处理
					doSetValue(ps, parameterPosition, this.argTypes[i], arg);
					parameterPosition++;
				}
			}
		}
	}

	/**
	 * Set the value for the prepared statement's specified parameter position using the passed in
	 * value and type. This method can be overridden by subclasses if needed.
	 * <p>使用传入的为准备好的语句的指定参数位置设置值和类型。如果需要, 此方法可以被子类覆盖。
	 *
	 * @param ps                the PreparedStatement
	 * @param parameterPosition index of the parameter position
	 * @param argType           the argument type
	 * @param argValue          the argument value
	 * @throws SQLException if thrown by PreparedStatement methods
	 */
	protected void doSetValue(PreparedStatement ps, int parameterPosition, int argType, Object argValue)
			throws SQLException {
		// 设置参数的值
		StatementCreatorUtils.setParameterValue(ps, parameterPosition, argType, argValue);
	}

	@Override
	public void cleanupParameters() {
		StatementCreatorUtils.cleanupParameters(this.args);
	}

}
