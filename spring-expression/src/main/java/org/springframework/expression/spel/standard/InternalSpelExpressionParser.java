/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.expression.spel.standard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateAwareExpressionParser;
import org.springframework.expression.spel.InternalParseException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.ast.Assign;
import org.springframework.expression.spel.ast.BeanReference;
import org.springframework.expression.spel.ast.BooleanLiteral;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.ConstructorReference;
import org.springframework.expression.spel.ast.Elvis;
import org.springframework.expression.spel.ast.FunctionReference;
import org.springframework.expression.spel.ast.Identifier;
import org.springframework.expression.spel.ast.Indexer;
import org.springframework.expression.spel.ast.InlineList;
import org.springframework.expression.spel.ast.InlineMap;
import org.springframework.expression.spel.ast.Literal;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.NullLiteral;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpDec;
import org.springframework.expression.spel.ast.OpDivide;
import org.springframework.expression.spel.ast.OpEQ;
import org.springframework.expression.spel.ast.OpGE;
import org.springframework.expression.spel.ast.OpGT;
import org.springframework.expression.spel.ast.OpInc;
import org.springframework.expression.spel.ast.OpLE;
import org.springframework.expression.spel.ast.OpLT;
import org.springframework.expression.spel.ast.OpMinus;
import org.springframework.expression.spel.ast.OpModulus;
import org.springframework.expression.spel.ast.OpMultiply;
import org.springframework.expression.spel.ast.OpNE;
import org.springframework.expression.spel.ast.OpOr;
import org.springframework.expression.spel.ast.OpPlus;
import org.springframework.expression.spel.ast.OperatorBetween;
import org.springframework.expression.spel.ast.OperatorInstanceof;
import org.springframework.expression.spel.ast.OperatorMatches;
import org.springframework.expression.spel.ast.OperatorNot;
import org.springframework.expression.spel.ast.OperatorPower;
import org.springframework.expression.spel.ast.Projection;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.ast.QualifiedIdentifier;
import org.springframework.expression.spel.ast.Selection;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.expression.spel.ast.StringLiteral;
import org.springframework.expression.spel.ast.Ternary;
import org.springframework.expression.spel.ast.TypeReference;
import org.springframework.expression.spel.ast.VariableReference;
import org.springframework.lang.Contract;
import org.springframework.util.StringUtils;

/**
 * Handwritten SpEL parser. Instances are reusable but are not thread-safe.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.0
 */
class InternalSpelExpressionParser extends TemplateAwareExpressionParser {

	private static final Pattern VALID_QUALIFIED_ID_PATTERN = Pattern.compile("[\\p{L}\\p{N}_$]+");

	private final SpelParserConfiguration configuration;

	// For rules that build nodes, they are stacked here for return
	private final Deque<SpelNodeImpl> constructedNodes = new ArrayDeque<>();

	// Shared cache for compiled regex patterns
	private final ConcurrentMap<String, Pattern> patternCache = new ConcurrentHashMap<>();

	// The expression being parsed
	private String expressionString = "";

	// The token stream constructed from that expression string
	private List<Token> tokenStream = Collections.emptyList();

	// length of a populated token stream
	private int tokenStreamLength;

	// Current location in the token stream when processing tokens
	private int tokenStreamPointer;


	/**
	 * Create a parser with some configured behavior.
	 * @param configuration custom configuration options
	 */
	public InternalSpelExpressionParser(SpelParserConfiguration configuration) {
		this.configuration = configuration;
	}


	@Override
	protected SpelExpression doParseExpression(String expressionString, @Nullable ParserContext context)
			throws ParseException {

		checkExpressionLength(expressionString);

		try {
			this.expressionString = expressionString;
			Tokenizer tokenizer = new Tokenizer(expressionString);
			this.tokenStream = tokenizer.process();
			this.tokenStreamLength = this.tokenStream.size();
			this.tokenStreamPointer = 0;
			this.constructedNodes.clear();
			SpelNodeImpl ast = eatExpression();
			if (ast == null) {
				throw new SpelParseException(this.expressionString, 0, SpelMessage.OOD);
			}
			Token t = peekToken();
			if (t != null) {
				throw new SpelParseException(this.expressionString, t.startPos, SpelMessage.MORE_INPUT, toString(nextToken()));
			}
			return new SpelExpression(expressionString, ast, this.configuration);
		}
		catch (InternalParseException ex) {
			throw ex.getCause();
		}
	}

	private void checkExpressionLength(String string) {
		int maxLength = this.configuration.getMaximumExpressionLength();
		if (string.length() > maxLength) {
			throw new SpelEvaluationException(SpelMessage.MAX_EXPRESSION_LENGTH_EXCEEDED, maxLength);
		}
	}

	//	expression
	//    : logicalOrExpression
	//      ( (ASSIGN^ logicalOrExpression)
	//	    | (DEFAULT^ logicalOrExpression)
	//	    | (QMARK^ expression COLON! expression)
	//      | (ELVIS^ expression))?;
	@SuppressWarnings("NullAway") // Not null assertion performed in SpelNodeImpl constructor
	private @Nullable SpelNodeImpl eatExpression() {
		SpelNodeImpl expr = eatLogicalOrExpression();
		Token t = peekToken();
		if (t != null) {
			if (t.kind == TokenKind.ASSIGN) {  // a=b
				if (expr == null) {
					expr = new NullLiteral(t.startPos - 1, t.endPos - 1);
				}
				nextToken();
				SpelNodeImpl assignedValue = eatLogicalOrExpression();
				return new Assign(t.startPos, t.endPos, expr, assignedValue);
			}
			if (t.kind == TokenKind.ELVIS) {  // a?:b (a if it isn't null, otherwise b)
				if (expr == null) {
					expr = new NullLiteral(t.startPos - 1, t.endPos - 2);
				}
				nextToken();  // elvis has left the building
				SpelNodeImpl valueIfNull = eatExpression();
				if (valueIfNull == null) {
					valueIfNull = new NullLiteral(t.startPos + 1, t.endPos + 1);
				}
				return new Elvis(t.startPos, t.endPos, expr, valueIfNull);
			}
			if (t.kind == TokenKind.QMARK) {  // a?b:c
				if (expr == null) {
					expr = new NullLiteral(t.startPos - 1, t.endPos - 1);
				}
				nextToken();
				SpelNodeImpl ifTrueExprValue = eatExpression();
				eatToken(TokenKind.COLON);
				SpelNodeImpl ifFalseExprValue = eatExpression();
				return new Ternary(t.startPos, t.endPos, expr, ifTrueExprValue, ifFalseExprValue);
			}
		}
		return expr;
	}

	//logicalOrExpression : logicalAndExpression (OR^ logicalAndExpression)*;
	private @Nullable SpelNodeImpl eatLogicalOrExpression() {
		SpelNodeImpl expr = eatLogicalAndExpression();
		while (peekIdentifierToken("or") || peekToken(TokenKind.SYMBOLIC_OR)) {
			Token t = takeToken();  //consume OR
			SpelNodeImpl rhExpr = eatLogicalAndExpression();
			checkOperands(t, expr, rhExpr);
			expr = new OpOr(t.startPos, t.endPos, expr, rhExpr);
		}
		return expr;
	}

	// logicalAndExpression : relationalExpression (AND^ relationalExpression)*;
	private @Nullable SpelNodeImpl eatLogicalAndExpression() {
		SpelNodeImpl expr = eatRelationalExpression();
		while (peekIdentifierToken("and") || peekToken(TokenKind.SYMBOLIC_AND)) {
			Token t = takeToken();  // consume 'AND'
			SpelNodeImpl rhExpr = eatRelationalExpression();
			checkOperands(t, expr, rhExpr);
			expr = new OpAnd(t.startPos, t.endPos, expr, rhExpr);
		}
		return expr;
	}

	// relationalExpression : sumExpression (relationalOperator^ sumExpression)?;
	private @Nullable SpelNodeImpl eatRelationalExpression() {
		SpelNodeImpl expr = eatSumExpression();
		Token relationalOperatorToken = maybeEatRelationalOperator();
		if (relationalOperatorToken != null) {
			Token t = takeToken();  // consume relational operator token
			SpelNodeImpl rhExpr = eatSumExpression();
			checkOperands(t, expr, rhExpr);
			TokenKind tk = relationalOperatorToken.kind;

			if (relationalOperatorToken.isNumericRelationalOperator()) {
				if (tk == TokenKind.GT) {
					return new OpGT(t.startPos, t.endPos, expr, rhExpr);
				}
				if (tk == TokenKind.LT) {
					return new OpLT(t.startPos, t.endPos, expr, rhExpr);
				}
				if (tk == TokenKind.LE) {
					return new OpLE(t.startPos, t.endPos, expr, rhExpr);
				}
				if (tk == TokenKind.GE) {
					return new OpGE(t.startPos, t.endPos, expr, rhExpr);
				}
				if (tk == TokenKind.EQ) {
					return new OpEQ(t.startPos, t.endPos, expr, rhExpr);
				}
				if (tk == TokenKind.NE) {
					return new OpNE(t.startPos, t.endPos, expr, rhExpr);
				}
			}

			if (tk == TokenKind.INSTANCEOF) {
				return new OperatorInstanceof(t.startPos, t.endPos, expr, rhExpr);
			}
			if (tk == TokenKind.MATCHES) {
				return new OperatorMatches(this.patternCache, t.startPos, t.endPos, expr, rhExpr);
			}
			if (tk == TokenKind.BETWEEN) {
				return new OperatorBetween(t.startPos, t.endPos, expr, rhExpr);
			}
		}
		return expr;
	}

	//sumExpression: productExpression ( (PLUS^ | MINUS^) productExpression)*;
	@SuppressWarnings("NullAway") // Not null assertion performed in SpelNodeImpl constructor
	private @Nullable SpelNodeImpl eatSumExpression() {
		SpelNodeImpl expr = eatProductExpression();
		while (peekToken(TokenKind.PLUS, TokenKind.MINUS, TokenKind.INC)) {
			Token t = takeToken();  //consume PLUS or MINUS or INC
			SpelNodeImpl rhExpr = eatProductExpression();
			checkRightOperand(t, rhExpr);
			if (t.kind == TokenKind.PLUS) {
				expr = new OpPlus(t.startPos, t.endPos, expr, rhExpr);
			}
			else if (t.kind == TokenKind.MINUS) {
				expr = new OpMinus(t.startPos, t.endPos, expr, rhExpr);
			}
		}
		return expr;
	}

	// productExpression: powerExpr ((STAR^ | DIV^| MOD^) powerExpr)* ;
	private @Nullable SpelNodeImpl eatProductExpression() {
		SpelNodeImpl expr = eatPowerIncDecExpression();
		while (peekToken(TokenKind.STAR, TokenKind.DIV, TokenKind.MOD)) {
			Token t = takeToken();  // consume STAR/DIV/MOD
			SpelNodeImpl rhExpr = eatPowerIncDecExpression();
			checkOperands(t, expr, rhExpr);
			if (t.kind == TokenKind.STAR) {
				expr = new OpMultiply(t.startPos, t.endPos, expr, rhExpr);
			}
			else if (t.kind == TokenKind.DIV) {
				expr = new OpDivide(t.startPos, t.endPos, expr, rhExpr);
			}
			else if (t.kind == TokenKind.MOD) {
				expr = new OpModulus(t.startPos, t.endPos, expr, rhExpr);
			}
		}
		return expr;
	}

	// powerExpr  : unaryExpression (POWER^ unaryExpression)? (INC || DEC) ;
	@SuppressWarnings("NullAway") // Not null assertion performed in SpelNodeImpl constructor
	private @Nullable SpelNodeImpl eatPowerIncDecExpression() {
		SpelNodeImpl expr = eatUnaryExpression();
		if (peekToken(TokenKind.POWER)) {
			Token t = takeToken();  //consume POWER
			SpelNodeImpl rhExpr = eatUnaryExpression();
			checkRightOperand(t, rhExpr);
			return new OperatorPower(t.startPos, t.endPos, expr, rhExpr);
		}
		if (expr != null && peekToken(TokenKind.INC, TokenKind.DEC)) {
			Token t = takeToken();  //consume INC/DEC
			if (t.getKind() == TokenKind.INC) {
				return new OpInc(t.startPos, t.endPos, true, expr);
			}
			return new OpDec(t.startPos, t.endPos, true, expr);
		}
		return expr;
	}

	// unaryExpression: (PLUS^ | MINUS^ | BANG^ | INC^ | DEC^) unaryExpression | primaryExpression ;
	@SuppressWarnings("NullAway") // Not null assertion performed in SpelNodeImpl constructor
	private @Nullable SpelNodeImpl eatUnaryExpression() {
		if (peekToken(TokenKind.NOT, TokenKind.PLUS, TokenKind.MINUS)) {
			Token t = takeToken();
			SpelNodeImpl expr = eatUnaryExpression();
			if (expr == null) {
				throw internalException(t.startPos, SpelMessage.OOD);
			}
			if (t.kind == TokenKind.NOT) {
				return new OperatorNot(t.startPos, t.endPos, expr);
			}
			if (t.kind == TokenKind.PLUS) {
				return new OpPlus(t.startPos, t.endPos, expr);
			}
			if (t.kind == TokenKind.MINUS) {
				return new OpMinus(t.startPos, t.endPos, expr);
			}
		}
		if (peekToken(TokenKind.INC, TokenKind.DEC)) {
			Token t = takeToken();
			SpelNodeImpl expr = eatUnaryExpression();
			if (t.getKind() == TokenKind.INC) {
				return new OpInc(t.startPos, t.endPos, false, expr);
			}
			if (t.kind == TokenKind.DEC) {
				return new OpDec(t.startPos, t.endPos, false, expr);
			}
		}
		return eatPrimaryExpression();
	}

	// primaryExpression : startNode (node)? -> ^(EXPRESSION startNode (node)?);
	private @Nullable SpelNodeImpl eatPrimaryExpression() {
		SpelNodeImpl start = eatStartNode();  // always a start node
		List<SpelNodeImpl> nodes = null;
		SpelNodeImpl node = eatNode();
		while (node != null) {
			if (nodes == null) {
				nodes = new ArrayList<>(4);
				nodes.add(start);
			}
			nodes.add(node);
			node = eatNode();
		}
		if (start == null || nodes == null) {
			return start;
		}
		return new CompoundExpression(start.getStartPosition(), nodes.get(nodes.size() - 1).getEndPosition(),
				nodes.toArray(new SpelNodeImpl[0]));
	}

	// node : ((DOT dottedNode) | (SAFE_NAVI dottedNode) | nonDottedNode)+;
	private @Nullable SpelNodeImpl eatNode() {
		return (peekToken(TokenKind.DOT, TokenKind.SAFE_NAVI) ? eatDottedNode() : eatNonDottedNode());
	}

	// nonDottedNode: indexer;
	private @Nullable SpelNodeImpl eatNonDottedNode() {
		if (peekToken(TokenKind.LSQUARE)) {
			if (maybeEatIndexer(false)) {
				return pop();
			}
		}
		return null;
	}

	//dottedNode
	// : ((methodOrProperty
	//	  | functionOrVar
	//    | projection
	//    | selection
	//    | firstSelection
	//    | lastSelection
	//    ))
	//	;
	private SpelNodeImpl eatDottedNode() {
		Token t = takeToken();  // it was a '.' or a '?.'
		boolean nullSafeNavigation = (t.kind == TokenKind.SAFE_NAVI);
		if (maybeEatMethodOrProperty(nullSafeNavigation) || maybeEatFunctionOrVar() ||
				maybeEatProjection(nullSafeNavigation) || maybeEatSelection(nullSafeNavigation) ||
				maybeEatIndexer(nullSafeNavigation)) {
			return pop();
		}
		if (peekToken() == null) {
			throw internalException(t.startPos, SpelMessage.OOD);
		}
		else {
			throw internalException(t.startPos, SpelMessage.UNEXPECTED_DATA_AFTER_DOT, toString(peekToken()));
		}
	}

	// functionOrVar
	// : (POUND ID LPAREN) => function
	// | var
	//
	// function : POUND id=ID methodArgs -> ^(FUNCTIONREF[$id] methodArgs);
	// var : POUND id=ID -> ^(VARIABLEREF[$id]);
	private boolean maybeEatFunctionOrVar() {
		if (!peekToken(TokenKind.HASH)) {
			return false;
		}
		Token t = takeToken();
		Token functionOrVariableName = eatToken(TokenKind.IDENTIFIER);
		SpelNodeImpl[] args = maybeEatMethodArgs();
		if (args == null) {
			push(new VariableReference(functionOrVariableName.stringValue(),
					t.startPos, functionOrVariableName.endPos));
			return true;
		}

		push(new FunctionReference(functionOrVariableName.stringValue(),
				t.startPos, functionOrVariableName.endPos, args));
		return true;
	}

	// methodArgs : LPAREN! (argument (COMMA! argument)* (COMMA!)?)? RPAREN!;
	private SpelNodeImpl @Nullable [] maybeEatMethodArgs() {
		if (!peekToken(TokenKind.LPAREN)) {
			return null;
		}
		List<SpelNodeImpl> args = new ArrayList<>();
		consumeArguments(args);
		eatToken(TokenKind.RPAREN);
		return args.toArray(new SpelNodeImpl[0]);
	}

	private void eatConstructorArgs(List<SpelNodeImpl> accumulatedArguments) {
		if (!peekToken(TokenKind.LPAREN)) {
			throw internalException(positionOf(peekToken()), SpelMessage.MISSING_CONSTRUCTOR_ARGS);
		}
		consumeArguments(accumulatedArguments);
		eatToken(TokenKind.RPAREN);
	}

	/**
	 * Used for consuming arguments for either a method or a constructor call.
	 */
	private void consumeArguments(List<SpelNodeImpl> accumulatedArguments) {
		Token t = peekToken();
		if (t == null) {
			return;
		}
		int pos = t.startPos;
		Token next;
		do {
			nextToken();  // consume (first time through) or comma (subsequent times)
			t = peekToken();
			if (t == null) {
				throw internalException(pos, SpelMessage.RUN_OUT_OF_ARGUMENTS);
			}
			if (t.kind != TokenKind.RPAREN) {
				accumulatedArguments.add(eatExpression());
			}
			next = peekToken();
		}
		while (next != null && next.kind == TokenKind.COMMA);

		if (next == null) {
			throw internalException(pos, SpelMessage.RUN_OUT_OF_ARGUMENTS);
		}
	}

	private int positionOf(@Nullable Token t) {
		if (t == null) {
			// if null assume the problem is because the right token was
			// not found at the end of the expression
			return this.expressionString.length();
		}
		return t.startPos;
	}

	//startNode
	// : parenExpr | literal
	//	    | type
	//	    | methodOrProperty
	//	    | functionOrVar
	//	    | projection
	//	    | selection
	//	    | firstSelection
	//	    | lastSelection
	//	    | indexer
	//	    | constructor
	private @Nullable SpelNodeImpl eatStartNode() {
		if (maybeEatLiteral()) {
			return pop();
		}
		else if (maybeEatParenExpression()) {
			return pop();
		}
		else if (maybeEatTypeReference() || maybeEatNullReference() || maybeEatConstructorReference() ||
				maybeEatMethodOrProperty(false) || maybeEatFunctionOrVar()) {
			return pop();
		}
		else if (maybeEatBeanReference()) {
			return pop();
		}
		else if (maybeEatProjection(false) || maybeEatSelection(false) || maybeEatIndexer(false)) {
			return pop();
		}
		else if (maybeEatInlineListOrMap()) {
			return pop();
		}
		else {
			return null;
		}
	}

	// parse: @beanname @'bean.name'
	// quoted if dotted
	private boolean maybeEatBeanReference() {
		if (peekToken(TokenKind.BEAN_REF) || peekToken(TokenKind.FACTORY_BEAN_REF)) {
			Token beanRefToken = takeToken();
			Token beanNameToken = null;
			String beanName = null;
			if (peekToken(TokenKind.IDENTIFIER)) {
				beanNameToken = eatToken(TokenKind.IDENTIFIER);
				beanName = beanNameToken.stringValue();
			}
			else if (peekToken(TokenKind.LITERAL_STRING)) {
				beanNameToken = eatToken(TokenKind.LITERAL_STRING);
				beanName = beanNameToken.stringValue();
				beanName = beanName.substring(1, beanName.length() - 1);
			}
			else {
				throw internalException(beanRefToken.startPos, SpelMessage.INVALID_BEAN_REFERENCE);
			}
			BeanReference beanReference;
			if (beanRefToken.getKind() == TokenKind.FACTORY_BEAN_REF) {
				String beanNameString = String.valueOf(TokenKind.FACTORY_BEAN_REF.tokenChars) + beanName;
				beanReference = new BeanReference(beanRefToken.startPos, beanNameToken.endPos, beanNameString);
			}
			else {
				beanReference = new BeanReference(beanNameToken.startPos, beanNameToken.endPos, beanName);
			}
			this.constructedNodes.push(beanReference);
			return true;
		}
		return false;
	}

	private boolean maybeEatTypeReference() {
		if (peekToken(TokenKind.IDENTIFIER)) {
			Token typeName = peekToken();
			if (typeName == null || !"T".equals(typeName.stringValue())) {
				return false;
			}
			// It looks like a type reference but is T being used as a map key?
			Token t = takeToken();
			if (peekToken(TokenKind.RSQUARE)) {
				// looks like 'T]' (T is map key)
				push(new PropertyOrFieldReference(false, t.stringValue(), t.startPos, t.endPos));
				return true;
			}
			eatToken(TokenKind.LPAREN);
			SpelNodeImpl node = eatPossiblyQualifiedId();
			// dotted qualified id
			// Are there array dimensions?
			int dims = 0;
			while (peekToken(TokenKind.LSQUARE, true)) {
				eatToken(TokenKind.RSQUARE);
				dims++;
			}
			eatToken(TokenKind.RPAREN);
			this.constructedNodes.push(new TypeReference(typeName.startPos, typeName.endPos, node, dims));
			return true;
		}
		return false;
	}

	private boolean maybeEatNullReference() {
		if (peekToken(TokenKind.IDENTIFIER)) {
			Token nullToken = peekToken();
			if (nullToken == null || !"null".equalsIgnoreCase(nullToken.stringValue())) {
				return false;
			}
			nextToken();
			this.constructedNodes.push(new NullLiteral(nullToken.startPos, nullToken.endPos));
			return true;
		}
		return false;
	}

	//projection: PROJECT^ expression RCURLY!;
	private boolean maybeEatProjection(boolean nullSafeNavigation) {
		Token t = peekToken();
		if (t == null || !peekToken(TokenKind.PROJECT, true)) {
			return false;
		}
		SpelNodeImpl expr = eatExpression();
		if (expr == null) {
			throw internalException(t.startPos, SpelMessage.OOD);
		}
		eatToken(TokenKind.RSQUARE);
		this.constructedNodes.push(new Projection(nullSafeNavigation, t.startPos, t.endPos, expr));
		return true;
	}

	// list = LCURLY (element (COMMA element)*) RCURLY
	// map  = LCURLY (key ':' value (COMMA key ':' value)*) RCURLY
	private boolean maybeEatInlineListOrMap() {
		Token t = peekToken();
		if (t == null || !peekToken(TokenKind.LCURLY, true)) {
			return false;
		}
		SpelNodeImpl expr = null;
		Token closingCurly = peekToken();
		if (closingCurly != null && peekToken(TokenKind.RCURLY, true)) {
			// empty list '{}'
			expr = new InlineList(t.startPos, closingCurly.endPos);
		}
		else if (peekToken(TokenKind.COLON, true)) {
			closingCurly = eatToken(TokenKind.RCURLY);
			// empty map '{:}'
			expr = new InlineMap(t.startPos, closingCurly.endPos);
		}
		else {
			SpelNodeImpl firstExpression = eatExpression();
			// Next is either:
			// '}' - end of list
			// ',' - more expressions in this list
			// ':' - this is a map!
			if (peekToken(TokenKind.RCURLY)) {  // list with one item in it
				List<SpelNodeImpl> elements = new ArrayList<>();
				elements.add(firstExpression);
				closingCurly = eatToken(TokenKind.RCURLY);
				expr = new InlineList(t.startPos, closingCurly.endPos, elements.toArray(new SpelNodeImpl[0]));
			}
			else if (peekToken(TokenKind.COMMA, true)) {  // multi-item list
				List<SpelNodeImpl> elements = new ArrayList<>();
				elements.add(firstExpression);
				do {
					elements.add(eatExpression());
				}
				while (peekToken(TokenKind.COMMA, true));
				closingCurly = eatToken(TokenKind.RCURLY);
				expr = new InlineList(t.startPos, closingCurly.endPos, elements.toArray(new SpelNodeImpl[0]));

			}
			else if (peekToken(TokenKind.COLON, true)) {  // map!
				List<SpelNodeImpl> elements = new ArrayList<>();
				elements.add(firstExpression);
				elements.add(eatExpression());
				while (peekToken(TokenKind.COMMA, true)) {
					elements.add(eatExpression());
					eatToken(TokenKind.COLON);
					elements.add(eatExpression());
				}
				closingCurly = eatToken(TokenKind.RCURLY);
				expr = new InlineMap(t.startPos, closingCurly.endPos, elements.toArray(new SpelNodeImpl[0]));
			}
			else {
				throw internalException(t.startPos, SpelMessage.OOD);
			}
		}
		this.constructedNodes.push(expr);
		return true;
	}

	private boolean maybeEatIndexer(boolean nullSafeNavigation) {
		Token t = peekToken();
		if (t == null || !peekToken(TokenKind.LSQUARE, true)) {
			return false;
		}
		SpelNodeImpl expr = eatExpression();
		if (expr == null) {
			throw internalException(t.startPos, SpelMessage.MISSING_SELECTION_EXPRESSION);
		}
		eatToken(TokenKind.RSQUARE);
		this.constructedNodes.push(new Indexer(nullSafeNavigation, t.startPos, t.endPos, expr));
		return true;
	}

	private boolean maybeEatSelection(boolean nullSafeNavigation) {
		Token t = peekToken();
		if (t == null || !peekSelectToken()) {
			return false;
		}
		nextToken();
		SpelNodeImpl expr = eatExpression();
		if (expr == null) {
			throw internalException(t.startPos, SpelMessage.MISSING_SELECTION_EXPRESSION);
		}
		eatToken(TokenKind.RSQUARE);
		if (t.kind == TokenKind.SELECT_FIRST) {
			this.constructedNodes.push(new Selection(nullSafeNavigation, Selection.FIRST, t.startPos, t.endPos, expr));
		}
		else if (t.kind == TokenKind.SELECT_LAST) {
			this.constructedNodes.push(new Selection(nullSafeNavigation, Selection.LAST, t.startPos, t.endPos, expr));
		}
		else {
			this.constructedNodes.push(new Selection(nullSafeNavigation, Selection.ALL, t.startPos, t.endPos, expr));
		}
		return true;
	}

	/**
	 * Eat an identifier, possibly qualified (meaning that it is dotted).
	 */
	private SpelNodeImpl eatPossiblyQualifiedId() {
		Deque<SpelNodeImpl> qualifiedIdPieces = new ArrayDeque<>();
		Token node = peekToken();
		while (isValidQualifiedId(node)) {
			nextToken();
			if (node.kind != TokenKind.DOT) {
				qualifiedIdPieces.add(new Identifier(node.stringValue(), node.startPos, node.endPos));
			}
			node = peekToken();
		}
		if (qualifiedIdPieces.isEmpty()) {
			if (node == null) {
				throw internalException( this.expressionString.length(), SpelMessage.OOD);
			}
			throw internalException(node.startPos, SpelMessage.NOT_EXPECTED_TOKEN,
					"qualified ID", node.getKind().toString().toLowerCase(Locale.ROOT));
		}
		return new QualifiedIdentifier(qualifiedIdPieces.getFirst().getStartPosition(),
				qualifiedIdPieces.getLast().getEndPosition(), qualifiedIdPieces.toArray(new SpelNodeImpl[0]));
	}

	@Contract("null -> false")
	private boolean isValidQualifiedId(@Nullable Token node) {
		if (node == null || node.kind == TokenKind.LITERAL_STRING) {
			return false;
		}
		if (node.kind == TokenKind.DOT || node.kind == TokenKind.IDENTIFIER) {
			return true;
		}
		String value = node.stringValue();
		return (StringUtils.hasLength(value) && VALID_QUALIFIED_ID_PATTERN.matcher(value).matches());
	}

	// This is complicated due to the support for dollars in identifiers.
	// Dollars are normally separate tokens but there we want to combine
	// a series of identifiers and dollars into a single identifier.
	private boolean maybeEatMethodOrProperty(boolean nullSafeNavigation) {
		if (peekToken(TokenKind.IDENTIFIER)) {
			Token methodOrPropertyName = takeToken();
			SpelNodeImpl[] args = maybeEatMethodArgs();
			if (args == null) {
				// property
				push(new PropertyOrFieldReference(nullSafeNavigation, methodOrPropertyName.stringValue(),
						methodOrPropertyName.startPos, methodOrPropertyName.endPos));
				return true;
			}
			// method reference
			push(new MethodReference(nullSafeNavigation, methodOrPropertyName.stringValue(),
					methodOrPropertyName.startPos, methodOrPropertyName.endPos, args));
			return true;
		}
		return false;
	}

	//constructor
    //:	('new' qualifiedId LPAREN) => 'new' qualifiedId ctorArgs -> ^(CONSTRUCTOR qualifiedId ctorArgs)
	private boolean maybeEatConstructorReference() {
		if (peekIdentifierToken("new")) {
			Token newToken = takeToken();
			// It looks like a constructor reference but is NEW being used as a map key?
			if (peekToken(TokenKind.RSQUARE)) {
				// looks like 'NEW]' (so NEW used as map key)
				push(new PropertyOrFieldReference(false, newToken.stringValue(), newToken.startPos, newToken.endPos));
				return true;
			}
			SpelNodeImpl possiblyQualifiedConstructorName = eatPossiblyQualifiedId();
			List<SpelNodeImpl> nodes = new ArrayList<>();
			nodes.add(possiblyQualifiedConstructorName);
			if (peekToken(TokenKind.LSQUARE)) {
				// array initializer
				List<SpelNodeImpl> dimensions = new ArrayList<>();
				while (peekToken(TokenKind.LSQUARE, true)) {
					if (!peekToken(TokenKind.RSQUARE)) {
						dimensions.add(eatExpression());
					}
					else {
						// A missing array dimension is tracked as null and will be
						// rejected later during evaluation.
						dimensions.add(null);
					}
					eatToken(TokenKind.RSQUARE);
				}
				if (maybeEatInlineListOrMap()) {
					nodes.add(pop());
				}
				push(new ConstructorReference(newToken.startPos, newToken.endPos,
						dimensions.toArray(new SpelNodeImpl[0]), nodes.toArray(new SpelNodeImpl[0])));
			}
			else {
				// regular constructor invocation
				eatConstructorArgs(nodes);
				push(new ConstructorReference(newToken.startPos, newToken.endPos, nodes.toArray(new SpelNodeImpl[0])));
			}
			return true;
		}
		return false;
	}

	private void push(SpelNodeImpl newNode) {
		this.constructedNodes.push(newNode);
	}

	private SpelNodeImpl pop() {
		return this.constructedNodes.pop();
	}

	//	literal
	//  : INTEGER_LITERAL
	//	| boolLiteral
	//	| STRING_LITERAL
	//  | HEXADECIMAL_INTEGER_LITERAL
	//  | REAL_LITERAL
	//	| DQ_STRING_LITERAL
	//	| NULL_LITERAL
	private boolean maybeEatLiteral() {
		Token t = peekToken();
		if (t == null) {
			return false;
		}
		if (t.kind == TokenKind.LITERAL_INT) {
			push(Literal.getIntLiteral(t.stringValue(), t.startPos, t.endPos, 10));
		}
		else if (t.kind == TokenKind.LITERAL_LONG) {
			push(Literal.getLongLiteral(t.stringValue(), t.startPos, t.endPos, 10));
		}
		else if (t.kind == TokenKind.LITERAL_HEXINT) {
			push(Literal.getIntLiteral(t.stringValue(), t.startPos, t.endPos, 16));
		}
		else if (t.kind == TokenKind.LITERAL_HEXLONG) {
			push(Literal.getLongLiteral(t.stringValue(), t.startPos, t.endPos, 16));
		}
		else if (t.kind == TokenKind.LITERAL_REAL) {
			push(Literal.getRealLiteral(t.stringValue(), t.startPos, t.endPos, false));
		}
		else if (t.kind == TokenKind.LITERAL_REAL_FLOAT) {
			push(Literal.getRealLiteral(t.stringValue(), t.startPos, t.endPos, true));
		}
		else if (peekIdentifierToken("true")) {
			push(new BooleanLiteral(t.stringValue(), t.startPos, t.endPos, true));
		}
		else if (peekIdentifierToken("false")) {
			push(new BooleanLiteral(t.stringValue(), t.startPos, t.endPos, false));
		}
		else if (t.kind == TokenKind.LITERAL_STRING) {
			push(new StringLiteral(t.stringValue(), t.startPos, t.endPos, t.stringValue()));
		}
		else {
			return false;
		}
		nextToken();
		return true;
	}

	//parenExpr : LPAREN! expression RPAREN!;
	private boolean maybeEatParenExpression() {
		if (peekToken(TokenKind.LPAREN)) {
			Token t = nextToken();
			if (t == null) {
				return false;
			}
			SpelNodeImpl expr = eatExpression();
			if (expr == null) {
				throw internalException(t.startPos, SpelMessage.OOD);
			}
			eatToken(TokenKind.RPAREN);
			push(expr);
			return true;
		}
		else {
			return false;
		}
	}

	// relationalOperator
	// : EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN
	// | GREATER_THAN_OR_EQUAL | INSTANCEOF | BETWEEN | MATCHES
	private @Nullable Token maybeEatRelationalOperator() {
		Token t = peekToken();
		if (t == null) {
			return null;
		}
		if (t.isNumericRelationalOperator()) {
			return t;
		}
		if (t.isIdentifier()) {
			String idString = t.stringValue();
			if (idString.equalsIgnoreCase("instanceof")) {
				return t.asInstanceOfToken();
			}
			if (idString.equalsIgnoreCase("matches")) {
				return t.asMatchesToken();
			}
			if (idString.equalsIgnoreCase("between")) {
				return t.asBetweenToken();
			}
		}
		return null;
	}

	private Token eatToken(TokenKind expectedKind) {
		Token t = nextToken();
		if (t == null) {
			int pos = this.expressionString.length();
			throw internalException(pos, SpelMessage.OOD);
		}
		if (t.kind != expectedKind) {
			throw internalException(t.startPos, SpelMessage.NOT_EXPECTED_TOKEN,
					expectedKind.toString().toLowerCase(Locale.ROOT), t.getKind().toString().toLowerCase(Locale.ROOT));
		}
		return t;
	}

	private boolean peekToken(TokenKind desiredTokenKind) {
		return peekToken(desiredTokenKind, false);
	}

	private boolean peekToken(TokenKind desiredTokenKind, boolean consumeIfMatched) {
		Token t = peekToken();
		if (t == null) {
			return false;
		}
		if (t.kind == desiredTokenKind) {
			if (consumeIfMatched) {
				this.tokenStreamPointer++;
			}
			return true;
		}

		if (desiredTokenKind == TokenKind.IDENTIFIER) {
			// Might be one of the textual forms of the operators (for example, NE for != ) -
			// in which case we can treat it as an identifier. The list is represented here:
			// Tokenizer.ALTERNATIVE_OPERATOR_NAMES and those ones are in order in the TokenKind enum.
			if (t.kind.ordinal() >= TokenKind.DIV.ordinal() && t.kind.ordinal() <= TokenKind.NOT.ordinal() &&
					t.data != null) {
				// if t.data were null, we'd know it wasn't the textual form, it was the symbol form
				return true;
			}
		}
		return false;
	}

	private boolean peekToken(TokenKind possible1, TokenKind possible2) {
		Token t = peekToken();
		if (t == null) {
			return false;
		}
		return (t.kind == possible1 || t.kind == possible2);
	}

	private boolean peekToken(TokenKind possible1, TokenKind possible2, TokenKind possible3) {
		Token t = peekToken();
		if (t == null) {
			return false;
		}
		return (t.kind == possible1 || t.kind == possible2 || t.kind == possible3);
	}

	private boolean peekIdentifierToken(String identifierString) {
		Token t = peekToken();
		if (t == null) {
			return false;
		}
		return (t.kind == TokenKind.IDENTIFIER && identifierString.equalsIgnoreCase(t.stringValue()));
	}

	private boolean peekSelectToken() {
		Token t = peekToken();
		if (t == null) {
			return false;
		}
		return (t.kind == TokenKind.SELECT || t.kind == TokenKind.SELECT_FIRST || t.kind == TokenKind.SELECT_LAST);
	}

	private Token takeToken() {
		if (this.tokenStreamPointer >= this.tokenStreamLength) {
			throw new IllegalStateException("No token");
		}
		return this.tokenStream.get(this.tokenStreamPointer++);
	}

	private @Nullable Token nextToken() {
		if (this.tokenStreamPointer >= this.tokenStreamLength) {
			return null;
		}
		return this.tokenStream.get(this.tokenStreamPointer++);
	}

	private @Nullable Token peekToken() {
		if (this.tokenStreamPointer >= this.tokenStreamLength) {
			return null;
		}
		return this.tokenStream.get(this.tokenStreamPointer);
	}

	public String toString(@Nullable Token t) {
		if (t == null) {
			return "";
		}
		if (t.getKind().hasPayload()) {
			return t.stringValue();
		}
		return t.kind.toString().toLowerCase(Locale.ROOT);
	}

	@Contract("_, null, _ -> fail; _, _, null -> fail")
	private void checkOperands(Token token, @Nullable SpelNodeImpl left, @Nullable SpelNodeImpl right) {
		checkLeftOperand(token, left);
		checkRightOperand(token, right);
	}

	@Contract("_, null -> fail")
	private void checkLeftOperand(Token token, @Nullable SpelNodeImpl operandExpression) {
		if (operandExpression == null) {
			throw internalException(token.startPos, SpelMessage.LEFT_OPERAND_PROBLEM);
		}
	}

	@Contract("_, null -> fail")
	private void checkRightOperand(Token token, @Nullable SpelNodeImpl operandExpression) {
		if (operandExpression == null) {
			throw internalException(token.startPos, SpelMessage.RIGHT_OPERAND_PROBLEM);
		}
	}

	private InternalParseException internalException(int startPos, SpelMessage message, Object... inserts) {
		return new InternalParseException(new SpelParseException(this.expressionString, startPos, message, inserts));
	}

}
