/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.transaction.annotation;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;

import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * Strategy implementation for parsing Spring's {@link Transactional} annotation.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class SpringTransactionAnnotationParser implements TransactionAnnotationParser, Serializable {

	public TransactionAttribute parseTransactionAnnotation(AnnotatedElement ae) {
		Transactional ann = ae.getAnnotation(Transactional.class);
		if (ann != null) {
			return parseTransactionAnnotation(ann);
		}
		else {
			return null;
		}
	}

	public TransactionAttribute parseTransactionAnnotation(Transactional ann) {
		RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
		rbta.setPropagationBehavior(ann.propagation().value());
		rbta.setIsolationLevel(ann.isolation().value());
		rbta.setTimeout(ann.timeout());
		rbta.setReadOnly(ann.readOnly());
		ArrayList<RollbackRuleAttribute> rollBackRules = new ArrayList<RollbackRuleAttribute>();
		Class[] rbf = ann.rollbackFor();
		for (int i = 0; i < rbf.length; ++i) {
			RollbackRuleAttribute rule = new RollbackRuleAttribute(rbf[i]);
			rollBackRules.add(rule);
		}
		String[] rbfc = ann.rollbackForClassName();
		for (int i = 0; i < rbfc.length; ++i) {
			RollbackRuleAttribute rule = new RollbackRuleAttribute(rbfc[i]);
			rollBackRules.add(rule);
		}
		Class[] nrbf = ann.noRollbackFor();
		for (int i = 0; i < nrbf.length; ++i) {
			NoRollbackRuleAttribute rule = new NoRollbackRuleAttribute(nrbf[i]);
			rollBackRules.add(rule);
		}
		String[] nrbfc = ann.noRollbackForClassName();
		for (int i = 0; i < nrbfc.length; ++i) {
			NoRollbackRuleAttribute rule = new NoRollbackRuleAttribute(nrbfc[i]);
			rollBackRules.add(rule);
		}
		rbta.getRollbackRules().addAll(rollBackRules);
		return rbta;
	}

}
