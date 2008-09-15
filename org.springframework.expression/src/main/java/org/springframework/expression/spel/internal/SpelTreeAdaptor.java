/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.expression.spel.internal;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.springframework.expression.spel.ast.ArgList;
import org.springframework.expression.spel.ast.Assign;
import org.springframework.expression.spel.ast.BooleanLiteral;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.ConstructorReference;
import org.springframework.expression.spel.ast.DateLiteral;
import org.springframework.expression.spel.ast.Dot;
import org.springframework.expression.spel.ast.ExpressionListNode;
import org.springframework.expression.spel.ast.FunctionReference;
import org.springframework.expression.spel.ast.Identifier;
import org.springframework.expression.spel.ast.Indexer;
import org.springframework.expression.spel.ast.Lambda;
import org.springframework.expression.spel.ast.ListInitializer;
import org.springframework.expression.spel.ast.Literal;
import org.springframework.expression.spel.ast.LocalFunctionReference;
import org.springframework.expression.spel.ast.LocalVariableReference;
import org.springframework.expression.spel.ast.MapEntry;
import org.springframework.expression.spel.ast.MapInitializer;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.NullLiteral;
import org.springframework.expression.spel.ast.OperatorAnd;
import org.springframework.expression.spel.ast.OperatorBetween;
import org.springframework.expression.spel.ast.OperatorDivide;
import org.springframework.expression.spel.ast.OperatorEquality;
import org.springframework.expression.spel.ast.OperatorGreaterThan;
import org.springframework.expression.spel.ast.OperatorGreaterThanOrEqual;
import org.springframework.expression.spel.ast.OperatorIn;
import org.springframework.expression.spel.ast.OperatorInequality;
import org.springframework.expression.spel.ast.OperatorIs;
import org.springframework.expression.spel.ast.OperatorLessThan;
import org.springframework.expression.spel.ast.OperatorLessThanOrEqual;
import org.springframework.expression.spel.ast.OperatorMatches;
import org.springframework.expression.spel.ast.OperatorMinus;
import org.springframework.expression.spel.ast.OperatorModulus;
import org.springframework.expression.spel.ast.OperatorMultiply;
import org.springframework.expression.spel.ast.OperatorNot;
import org.springframework.expression.spel.ast.OperatorOr;
import org.springframework.expression.spel.ast.OperatorPlus;
import org.springframework.expression.spel.ast.Placeholder;
import org.springframework.expression.spel.ast.Projection;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.ast.QualifiedIdentifier;
import org.springframework.expression.spel.ast.RealLiteral;
import org.springframework.expression.spel.ast.Reference;
import org.springframework.expression.spel.ast.Selection;
import org.springframework.expression.spel.ast.StringLiteral;
import org.springframework.expression.spel.ast.Ternary;
import org.springframework.expression.spel.ast.TypeReference;
import org.springframework.expression.spel.ast.VariableReference;
import org.springframework.expression.spel.generated.SpringExpressionsLexer;

public class SpelTreeAdaptor extends CommonTreeAdaptor {
	@Override
	public Object create(Token payload) {
		if (payload != null) {
			switch (payload.getType()) {
			case SpringExpressionsLexer.EXPRESSIONLIST:
				return new ExpressionListNode(payload);

			case SpringExpressionsLexer.TRUE:
				return new BooleanLiteral(payload, true);
			case SpringExpressionsLexer.FALSE:
				return new BooleanLiteral(payload, false);

			case SpringExpressionsLexer.OR:
				return new OperatorOr(payload);
			case SpringExpressionsLexer.AND:
				return new OperatorAnd(payload);
			case SpringExpressionsLexer.BANG:
				return new OperatorNot(payload);

			case SpringExpressionsLexer.REAL_LITERAL:
				return new RealLiteral(payload);
			case SpringExpressionsLexer.INTEGER_LITERAL:
				return Literal.getIntLiteral(payload, 10);
			case SpringExpressionsLexer.HEXADECIMAL_INTEGER_LITERAL:
				return Literal.getIntLiteral(payload, 16);

			case SpringExpressionsLexer.NOT_EQUAL:
				return new OperatorInequality(payload);
			case SpringExpressionsLexer.EQUAL:
				return new OperatorEquality(payload);
			case SpringExpressionsLexer.GREATER_THAN:
				return new OperatorGreaterThan(payload);
			case SpringExpressionsLexer.LESS_THAN:
				return new OperatorLessThan(payload);
			case SpringExpressionsLexer.LESS_THAN_OR_EQUAL:
				return new OperatorLessThanOrEqual(payload);
			case SpringExpressionsLexer.GREATER_THAN_OR_EQUAL:
				return new OperatorGreaterThanOrEqual(payload);
			case SpringExpressionsLexer.PLUS:
				return new OperatorPlus(payload);
			case SpringExpressionsLexer.MINUS:
				return new OperatorMinus(payload);
			case SpringExpressionsLexer.STAR/* MULTIPLY */:
				return new OperatorMultiply(payload);
			case SpringExpressionsLexer.DIV/* DIVIDE */:
				return new OperatorDivide(payload);
			case SpringExpressionsLexer.MOD:
				return new OperatorModulus(payload);

			case SpringExpressionsLexer.STRING_LITERAL:
			case SpringExpressionsLexer.DQ_STRING_LITERAL:
				return new StringLiteral(payload);
			case SpringExpressionsLexer.NULL_LITERAL:
				return new NullLiteral(payload);
			case SpringExpressionsLexer.DATE_LITERAL:
				return new DateLiteral(payload);

			case SpringExpressionsLexer.ID:
				return new Identifier(payload);
			case SpringExpressionsLexer.PROPERTY_OR_FIELD:
				return new PropertyOrFieldReference(payload);
			case SpringExpressionsLexer.METHOD:
				return new MethodReference(payload);
			case SpringExpressionsLexer.QUALIFIED_IDENTIFIER:
				return new QualifiedIdentifier(payload);
			case SpringExpressionsLexer.REFERENCE:
				return new Reference(payload);
			case SpringExpressionsLexer.TYPEREF:
				return new TypeReference(payload);

			case SpringExpressionsLexer.EXPRESSION:
				return new CompoundExpression(payload);

			case SpringExpressionsLexer.LIST_INITIALIZER:
				return new ListInitializer(payload);
			case SpringExpressionsLexer.MAP_ENTRY:
				return new MapEntry(payload);
			case SpringExpressionsLexer.MAP_INITIALIZER:
				return new MapInitializer(payload);

			case SpringExpressionsLexer.CONSTRUCTOR:
				return new ConstructorReference(payload, false);
			case SpringExpressionsLexer.CONSTRUCTOR_ARRAY:
				return new ConstructorReference(payload, true);
			case SpringExpressionsLexer.LOCALFUNC:
				return new LocalFunctionReference(payload);
			case SpringExpressionsLexer.LOCALVAR:
				return new LocalVariableReference(payload);
			case SpringExpressionsLexer.VARIABLEREF:
				return new VariableReference(payload);
			case SpringExpressionsLexer.FUNCTIONREF:
				return new FunctionReference(payload);
			case SpringExpressionsLexer.PROJECT:
				return new Projection(payload);
			case SpringExpressionsLexer.SELECT:
				return new Selection(payload, Selection.ALL);
			case SpringExpressionsLexer.SELECT_FIRST:
				return new Selection(payload, Selection.FIRST);
			case SpringExpressionsLexer.SELECT_LAST:
				return new Selection(payload, Selection.LAST);

			case SpringExpressionsLexer.ASSIGN:
				return new Assign(payload);
			case SpringExpressionsLexer.QMARK:
				return new Ternary(payload);
			case SpringExpressionsLexer.INDEXER:
				return new Indexer(payload);
				// case SpringExpressionsLexer.UPTO: return new Range(payload);

			case SpringExpressionsLexer.IN:
				return new OperatorIn(payload);
			case SpringExpressionsLexer.BETWEEN:
				return new OperatorBetween(payload);
			case SpringExpressionsLexer.MATCHES:
				return new OperatorMatches(payload);
			case SpringExpressionsLexer.IS:
				return new OperatorIs(payload);

			case SpringExpressionsLexer.ARGLIST:
				return new ArgList(payload);
			case SpringExpressionsLexer.LAMBDA:
				return new Lambda(payload);

			case SpringExpressionsLexer.RPAREN:
				return new Placeholder(payload);
			case SpringExpressionsLexer.COLON:
				return new Placeholder(payload);

				// case SpringExpressionsLexer.EOF: return new ErrorNode(payload);
			case SpringExpressionsLexer.DOT:
				return new Dot(payload);

			default:
				throw new RuntimeException("Not implemented for '" + payload + "' " + getToken(payload) + "'   "
						+ payload.getType());
			}
		}
		return new EmptySpelNode(payload);
	}
}