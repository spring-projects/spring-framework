/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.expression.spel.ast;

import org.springframework.expression.AccessException;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

/**
 * Represents a bean reference to a type, for example "@foo" or "@'foo.bar'"
 *
 * @author Andy Clement
 */
public class BeanReference extends SpelNodeImpl {

	private final String beanname;


	public BeanReference(int pos,String beanname) {
		super(pos);
		this.beanname = beanname;
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		BeanResolver beanResolver = state.getEvaluationContext().getBeanResolver();
		if (beanResolver==null) {
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.NO_BEAN_RESOLVER_REGISTERED, this.beanname);
		}

		try {
			TypedValue bean = new TypedValue(beanResolver.resolve(
					state.getEvaluationContext(), this.beanname));
		   return bean;
		}
		catch (AccessException ae) {
			throw new SpelEvaluationException( getStartPosition(), ae, SpelMessage.EXCEPTION_DURING_BEAN_RESOLUTION,
				this.beanname, ae.getMessage());
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append("@");
		if (this.beanname.indexOf('.') == -1) {
			sb.append(this.beanname);
		}
		else {
			sb.append("'").append(this.beanname).append("'");
		}
		return sb.toString();
	}

}
