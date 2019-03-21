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

package org.springframework.expression.spel.standard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateAwareExpressionParser;
import org.springframework.expression.spel.InternalParseException;
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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Hand-written SpEL parser. Instances are reusable but are not thread-safe.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 */
class InternalSpelExpressionParser extends TemplateAwareExpressionParser {

	private static final Pattern VALID_QUALIFIED_ID_PATTERN = Pattern.compile("[\\p{L}\\p{N}_$]+");


	private final SpelParserConfiguration configuration;

	// For rules that build nodes, they are stacked here for return
	private final Deque<SpelNodeImpl> constructedNodes = new ArrayDeque<>();

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

		try {
			this.expressionString = expressionString;
			Tokenizer tokenizer = new Tokenizer(expressionString);
			this.tokenStream = tokenizer.process();
			this.tokenStreamLength = this.tokenStream.size();
			this.tokenStreamPointer = 0;
			this.constructedNodes.clear();
			SpelNodeImpl ast = eatExpression();
			Assert.state(ast != null, "No node");
			Token t = peekToken();
			if (t != null) {
				throw new SpelParseException(t.startPos, SpelMessage.MORE_INPUT, toString(nextToken()));
			}
			Assert.isTrue(this.constructedNodes.isEmpty(), "At least one node expected");
			return new SpelExpression(expressionString, ast, this.configuration);
		}
		catch (InternalParseException ex) {
			throw ex.getCause();
		}
	}

	//	expression
	//    : logicalOrExpression
	//      ( (ASSIGN^ logicalOrExpression)
	//	    | (DEFAULT^ logicalOrExpression)
	//	    | (QMARK^ expression COLON! expression)
	//      | (ELVIS^ expression))?;
	@Nullable
	private SpelNodeImpl eatExpression() {
		SpelNodeImpl expr = eatLogicalOrExpression();
		Token t = peekToken();
		if (t != null) {
			if (t.kind == TokenKind.ASSIGN) {  // a=b
				if (expr == null) {
					expr = new NullLiteral(toPos(t.startPos - 1, t.endPos - 1));
				}
				nextToken();
				SpelNodeImpl assignedValue = eatLogicalOrExpression();
				return new Assign(toPos(t), expr, assignedValue);
			}
			if (t.kind == TokenKind.ELVIS) {  // a?:b (a if it isn't null, otherwise b)
				if (expr == null) {
					expr = new NullLiteral(toPos(t.startPos - 1, t.endPos - 2));
				}
				nextToken();  // elvis has left the building
				SpelNodeImpl valueIfNull = eatExpression();
				if (valueIfNull == null) {
					valueIfNull = new NullLiteral(toPos(t.startPos + 1, t.endPos + 1));
				}
				return new Elvis(toPos(t), expr, valueIfNull);
			}
			if (t.kind == TokenKind.QMARK) {  // a?b:c
				if (expr == null) {
					expr = new NullLiteral(toPos(t.startPos - 1, t.endPos - 1));
				}
				nextToken();
				SpelNodeImpl ifTrueExprValue = eatExpression();
				eatToken(TokenKind.COLON);
				SpelNodeImpl ifFalseExprValue = eatExpression();
				return new Ternary(toPos(t), expr, ifTrueExprValue, ifFalseExprValue);
			}
		}
		return expr;
	}

	//logicalOrExpression : logicalAndExpression (OR^ logicalAndExpression)*;
	@Nullable
	private SpelNodeImpl eatLogicalOrExpression() {
		SpelNodeImpl expr = eatLogicalAndExpression();
		while (peekIdentifierToken("or") || peekToken(TokenKind.SYMBOLIC_OR)) {
			Token t = takeToken();  //consume OR
			SpelNodeImpl rhExpr = eatLogicalAndExpression();
			checkOperands(t, expr, rhExpr);
			expr = new OpOr(toPos(t), expr, rhExpr);
		}
		return expr;
	}

	// logicalAndExpression : relationalExpression (AND^ relationalExpression)*;
	@Nullable
	private SpelNodeImpl eatLogicalAndExpression() {
		SpelNodeImpl expr = eatRelationalExpression();
		while (peekIdentifierToken("and") || peekToken(TokenKind.SYMBOLIC_AND)) {
			Token t = takeToken();  // consume 'AND'
			SpelNodeImpl rhExpr = eatRelationalExpression();
			checkOperands(t, expr, rhExpr);
			expr = new OpAnd(toPos(t), expr, rhExpr);
		}
		return expr;
	}

	// relationalExpression : sumExpression (relationalOperator^ sumExpression)?;
	@Nullable
	private SpelNodeImpl eatRelationalExpression() {
		SpelNodeImpl expr = eatSumExpression();
		Token relationalOperatorToken = maybeEatRelationalOperator();
		if (relationalOperatorToken != null) {
			Token t = takeToken();  // consume relational operator token
			SpelNodeImpl rhExpr = eatSumExpression();
			checkOperands(t, expr, rhExpr);
			TokenKind tk = relationalOperatorToken.kind;

			if (relationalOperatorToken.isNumericRelationalOperator()) {
				int pos = toPos(t);
				if (tk == TokenKind.GT) {
					return new OpGT(pos, expr, rhExpr);
				}
				if (tk == TokenKind.LT) {
					return new OpLT(pos, expr, rhExpr);
				}
				if (tk == TokenKind.LE) {
					return new OpLE(pos, expr, rhExpr);
				}
				if (tk == TokenKind.GE) {
					return new OpGE(pos, expr, rhExpr);
				}
				if (tk == TokenKind.EQ) {
					return new OpEQ(pos, expr, rhExpr);
				}
				Assert.isTrue(tk == TokenKind.NE, "Not-equals token expected");
				return new OpNE(pos, expr, rhExpr);
			}

			if (tk == TokenKind.INSTANCEOF) {
				return new OperatorInstanceof(toPos(t), expr, rhExpr);
			}

			if (tk == TokenKind.MATCHES) {
				return new OperatorMatches(toPos(t), expr, rhExpr);
			}

			Assert.isTrue(tk == TokenKind.BETWEEN, "Between token expected");
			return new OperatorBetween(toPos(t), expr, rhExpr);
		}
		return expr;
	}

	//sumExpression: productExpression ( (PLUS^ | MINUS^) productExpression)*;
	@Nullable
	private SpelNodeImpl eatSumExpression() {
		SpelNodeImpl expr = eatProductExpression();
		while (peekToken(TokenKind.PLUS, TokenKind.MINUS, TokenKind.INC)) {
			Token t = takeToken();  //consume PLUS or MINUS or INC
			SpelNodeImpl rhExpr = eatProductExpression();
			checkRightOperand(t, rhExpr);
			if (t.kind == TokenKind.PLUS) {
				expr = new OpPlus(toPos(t), expr, rhExpr);
			}
			else if (t.kind == TokenKind.MINUS) {
				expr = new OpMinus(toPos(t), expr, rhExpr);
			}
		}
		return expr;
	}

	// productExpression: powerExpr ((STAR^ | DIV^| MOD^) powerExpr)* ;
	@Nullable
	private SpelNodeImpl eatProductExpression() {
		SpelNodeImpl expr = eatPowerIncDecExpression();
		while (peekToken(TokenKind.STAR, TokenKind.DIV, TokenKind.MOD)) {
			Token t = takeToken();  // consume STAR/DIV/MOD
			SpelNodeImpl rhExpr = eatPowerIncDecExpression();
			checkOperands(t, expr, rhExpr);
			if (t.kind == TokenKind.STAR) {
				expr = new OpMultiply(toPos(t), expr, rhExpr);
			}
			else if (t.kind == TokenKind.DIV) {
				expr = new OpDivide(toPos(t), expr, rhExpr);
			}
			else {
				Assert.isTrue(t.kind == TokenKind.MOD, "Mod token expected");
				expr = new OpModulus(toPos(t), expr, rhExpr);
			}
		}
		return expr;
	}

	// powerExpr  : unaryExpression (POWER^ unaryExpression)? (INC || DEC) ;
	@Nullable
	private SpelNodeImpl eatPowerIncDecExpression() {
		SpelNodeImpl expr = eatUnaryExpression();
		if (peekToken(TokenKind.POWER)) {
			Token t = takeToken();  //consume POWER
			SpelNodeImpl rhExpr = eatUnaryExpression();
			checkRightOperand(t, rhExpr);
			return new OperatorPower(toPos(t), expr, rhExpr);
		}
		if (expr != null && peekToken(TokenKind.INC, TokenKind.DEC)) {
			Token t = takeToken();  //consume INC/DEC
			if (t.getKind() == TokenKind.INC) {
				return new OpInc(toPos(t), true, expr);
			}
			return new OpDec(toPos(t), true, expr);
		}
		return expr;
	}

	// unaryExpression: (PLUS^ | MINUS^ | BANG^ | INC^ | DEC^) unaryExpression | primaryExpression ;
	@Nullable
	private SpelNodeImpl eatUnaryExpression() {
		if (peekToken(TokenKind.PLUS, TokenKind.MINUS, TokenKind.NOT)) {
			Token t = takeToken();
			SpelNodeImpl expr = eatUnaryExpression();
			Assert.state(expr != null, "No node");
			if (t.kind == TokenKind.NOT) {
				return new OperatorNot(toPos(t), expr);
			}
			if (t.kind == TokenKind.PLUS) {
				return new OpPlus(toPos(t), expr);
			}
			Assert.isTrue(t.kind == TokenKind.MINUS, "Minus token expected");
			return new OpMinus(toPos(t), expr);
		}
		if (peekToken(TokenKind.INC, TokenKind.DEC)) {
			Token t = takeToken();
			SpelNodeImpl expr = eatUnaryExpression();
			if (t.getKind() == TokenKind.INC) {
				return new OpInc(toPos(t), false, expr);
			}
			return new OpDec(toPos(t), false, expr);
		}
		return eatPrimaryExpression();
	}

	// primaryExpression : startNode (node)? -> ^(EXPRESSION startNode (node)?);
	@Nullable
	private SpelNodeImpl eatPrimaryExpression() {
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
		return new CompoundExpression(toPos(start.getStartPosition(),
				nodes.get(nodes.size() - 1).getEndPosition()),
				nodes.toArray(new SpelNodeImpl[0]));
	}

	// node : ((DOT dottedNode) | (SAFE_NAVI dottedNode) | nonDottedNode)+;
	@Nullable
	private SpelNodeImpl eatNode() {
		return (peekToken(TokenKind.DOT, TokenKind.SAFE_NAVI) ? eatDottedNode() : eatNonDottedNode());
	}

	// nonDottedNode: indexer;
	@Nullable
	private SpelNodeImpl eatNonDottedNode() {
		if (peekToken(TokenKind.LSQUARE)) {
			if (maybeEatIndexer()) {
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
				maybeEatProjection(nullSafeNavigation) || maybeEatSelection(nullSafeNavigation)) {
			return pop();
		}
		if (peekToken() == null) {
			// unexpectedly ran out of data
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
					toPos(t.startPos, functionOrVariableName.endPos)));
			return true;
		}

		push(new FunctionReference(functionOrVariableName.stringValue(),
				toPos(t.startPos, functionOrVariableName.endPos), args));
		return true;
	}

	// methodArgs : LPAREN! (argument (COMMA! argument)* (COMMA!)?)? RPAREN!;
	@Nullable
	private SpelNodeImpl[] maybeEatMethodArgs() {
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
			throw new InternalParseException(new SpelParseException(this.expressionString,
					positionOf(peekToken()), SpelMessage.MISSING_CONSTRUCTOR_ARGS));
		}
		consumeArguments(accumulatedArguments);
		eatToken(TokenKind.RPAREN);
	}

	/**
	 * Used for consuming arguments for either a method or a constructor call.
	 */
	private void consumeArguments(List<SpelNodeImpl> accumulatedArguments) {
		Token t = peekToken();
		Assert.state(t != null, "Expected token");
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
	@Nullable
	private SpelNodeImpl eatStartNode() {
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
		else if (maybeEatProjection(false) || maybeEatSelection(false) || maybeEatIndexer()) {
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
				beanReference = new BeanReference(
						toPos(beanRefToken.startPos, beanNameToken.endPos), beanNameString);
			}
			else {
				beanReference = new BeanReference(toPos(beanNameToken), beanName);
			}
			this.constructedNodes.push(beanReference);
			return true;
		}
		return false;
	}

	private boolean maybeEatTypeReference() {
		if (peekToken(TokenKind.IDENTIFIER)) {
			Token typeName = peekToken();
			Assert.state(typeName != null, "Expected token");
			if (!"T".equals(typeName.stringValue())) {
				return false;
			}
			// It looks like a type reference but is T being used as a map key?
			Token t = takeToken();
			if (peekToken(TokenKind.RSQUARE)) {
				// looks like 'T]' (T is map key)
				push(new PropertyOrFieldReference(false, t.stringValue(), toPos(t)));
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
			this.constructedNodes.push(new TypeReference(toPos(typeName), node, dims));
			return true;
		}
		return false;
	}

	private boolean maybeEatNullReference() {
		if (peekToken(TokenKind.IDENTIFIER)) {
			Token nullToken = peekToken();
			Assert.state(nullToken != null, "Expected token");
			if (!"null".equalsIgnoreCase(nullToken.stringValue())) {
				return false;
			}
			nextToken();
			this.constructedNodes.push(new NullLiteral(toPos(nullToken)));
			return true;
		}
		return false;
	}

	//projection: PROJECT^ expression RCURLY!;
	private boolean maybeEatProjection(boolean nullSafeNavigation) {
		Token t = peekToken();
		if (!peekToken(TokenKind.PROJECT, true)) {
			return false;
		}
		Assert.state(t != null, "No token");
		SpelNodeImpl expr = eatExpression();
		Assert.state(expr != null, "No node");
		eatToken(TokenKind.RSQUARE);
		this.constructedNodes.push(new Projection(nullSafeNavigation, toPos(t), expr));
		return true;
	}

	// list = LCURLY (element (COMMA element)*) RCURLY
	// map  = LCURLY (key ':' value (COMMA key ':' value)*) RCURLY
	private boolean maybeEatInlineListOrMap() {
		Token t = peekToken();
		if (!peekToken(TokenKind.LCURLY, true)) {
			return false;
		}
		Assert.state(t != null, "No token");
		SpelNodeImpl expr = null;
		Token closingCurly = peekToken();
		if (peekToken(TokenKind.RCURLY, true)) {
			// empty list '{}'
			Assert.state(closingCurly != null, "No token");
			expr = new InlineList(toPos(t.startPos, closingCurly.endPos));
		}
		else if (peekToken(TokenKind.COLON, true)) {
			closingCurly = eatToken(TokenKind.RCURLY);
			// empty map '{:}'
			expr = new InlineMap(toPos(t.startPos, closingCurly.endPos));
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
				expr = new InlineList(toPos(t.startPos, closingCurly.endPos), elements.toArray(new SpelNodeImpl[0]));
			}
			else if (peekToken(TokenKind.COMMA, true)) {  // multi-item list
				List<SpelNodeImpl> elements = new ArrayList<>();
				elements.add(firstExpression);
				do {
					elements.add(eatExpression());
				}
				while (peekToken(TokenKind.COMMA, true));
				closingCurly = eatToken(TokenKind.RCURLY);
				expr = new InlineList(toPos(t.startPos, closingCurly.endPos), elements.toArray(new SpelNodeImpl[0]));

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
				expr = new InlineMap(toPos(t.startPos, closingCurly.endPos), elements.toArray(new SpelNodeImpl[0]));
			}
			else {
				throw internalException(t.startPos, SpelMessage.OOD);
			}
		}
		this.constructedNodes.push(expr);
		return true;
	}

	private boolean maybeEatIndexer() {
		Token t = peekToken();
		if (!peekToken(TokenKind.LSQUARE, true)) {
			return false;
		}
		Assert.state(t != null, "No token");
		SpelNodeImpl expr = eatExpression();
		Assert.state(expr != null, "No node");
		eatToken(TokenKind.RSQUARE);
		this.constructedNodes.push(new Indexer(toPos(t), expr));
		return true;
	}

	private boolean maybeEatSelection(boolean nullSafeNavigation) {
		Token t = peekToken();
		if (!peekSelectToken()) {
			return false;
		}
		Assert.state(t != null, "No token");
		nextToken();
		SpelNodeImpl expr = eatExpression();
		if (expr == null) {
			throw internalException(toPos(t), SpelMessage.MISSING_SELECTION_EXPRESSION);
		}
		eatToken(TokenKind.RSQUARE);
		if (t.kind == TokenKind.SELECT_FIRST) {
			this.constructedNodes.push(new Selection(nullSafeNavigation, Selection.FIRST, toPos(t), expr));
		}
		else if (t.kind == TokenKind.SELECT_LAST) {
			this.constructedNodes.push(new Selection(nullSafeNavigation, Selection.LAST, toPos(t), expr));
		}
		else {
			this.constructedNodes.push(new Selection(nullSafeNavigation, Selection.ALL, toPos(t), expr));
		}
		return true;
	}

	/**
	 * Eat an identifier, possibly qualified (meaning that it is dotted).
	 * TODO AndyC Could create complete identifiers (a.b.c) here rather than a sequence of them? (a, b, c)
	 */
	private SpelNodeImpl eatPossiblyQualifiedId() {
		Deque<SpelNodeImpl> qualifiedIdPieces = new ArrayDeque<>();
		Token node = peekToken();
		while (isValidQualifiedId(node)) {
			nextToken();
			if (node.kind != TokenKind.DOT) {
				qualifiedIdPieces.add(new Identifier(node.stringValue(), toPos(node)));
			}
			node = peekToken();
		}
		if (qualifiedIdPieces.isEmpty()) {
			if (node == null) {
				throw internalException( this.expressionString.length(), SpelMessage.OOD);
			}
			throw internalException(node.startPos, SpelMessage.NOT_EXPECTED_TOKEN,
					"qualified ID", node.getKind().toString().toLowerCase());
		}
		int pos = toPos(qualifiedIdPieces.getFirst().getStartPosition(), qualifiedIdPieces.getLast().getEndPosition());
		return new QualifiedIdentifier(pos, qualifiedIdPieces.toArray(new SpelNodeImpl[0]));
	}

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
						toPos(methodOrPropertyName)));
				return true;
			}
			// method reference
			push(new MethodReference(nullSafeNavigation, methodOrPropertyName.stringValue(),
					toPos(methodOrPropertyName), args));
			// TODO what is the end position for a method reference? the name or the last arg?
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
				push(new PropertyOrFieldReference(false, newToken.stringValue(), toPos(newToken)));
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
						dimensions.add(null);
					}
					eatToken(TokenKind.RSQUARE);
				}
				if (maybeEatInlineListOrMap()) {
					nodes.add(pop());
				}
				push(new ConstructorReference(toPos(newToken),
						dimensions.toArray(new SpelNodeImpl[0]), nodes.toArray(new SpelNodeImpl[0])));
			}
			else {
				// regular constructor invocation
				eatConstructorArgs(nodes);
				// TODO correct end position?
				push(new ConstructorReference(toPos(newToken), nodes.toArray(new SpelNodeImpl[0])));
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
			push(Literal.getIntLiteral(t.stringValue(), toPos(t), 10));
		}
		else if (t.kind == TokenKind.LITERAL_LONG) {
			push(Literal.getLongLiteral(t.stringValue(), toPos(t), 10));
		}
		else if (t.kind == TokenKind.LITERAL_HEXINT) {
			push(Literal.getIntLiteral(t.stringValue(), toPos(t), 16));
		}
		else if (t.kind == TokenKind.LITERAL_HEXLONG) {
			push(Literal.getLongLiteral(t.stringValue(), toPos(t), 16));
		}
		else if (t.kind == TokenKind.LITERAL_REAL) {
			push(Literal.getRealLiteral(t.stringValue(), toPos(t), false));
		}
		else if (t.kind == TokenKind.LITERAL_REAL_FLOAT) {
			push(Literal.getRealLiteral(t.stringValue(), toPos(t), true));
		}
		else if (peekIdentifierToken("true")) {
			push(new BooleanLiteral(t.stringValue(), toPos(t), true));
		}
		else if (peekIdentifierToken("false")) {
			push(new BooleanLiteral(t.stringValue(), toPos(t), false));
		}
		else if (t.kind == TokenKind.LITERAL_STRING) {
			push(new StringLiteral(t.stringValue(), toPos(t), t.stringValue()));
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
			nextToken();
			SpelNodeImpl expr = eatExpression();
			Assert.state(expr != null, "No node");
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
	@Nullable
	private Token maybeEatRelationalOperator() {
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
					expectedKind.toString().toLowerCase(), t.getKind().toString().toLowerCase());
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
			// Might be one of the textual forms of the operators (e.g. NE for != ) -
			// in which case we can treat it as an identifier. The list is represented here:
			// Tokenizer.alternativeOperatorNames and those ones are in order in the TokenKind enum.
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

	@Nullable
	private Token nextToken() {
		if (this.tokenStreamPointer >= this.tokenStreamLength) {
			return null;
		}
		return this.tokenStream.get(this.tokenStreamPointer++);
	}

	@Nullable
	private Token peekToken() {
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
		return t.kind.toString().toLowerCase();
	}

	private void checkOperands(Token token, @Nullable SpelNodeImpl left, @Nullable SpelNodeImpl right) {
		checkLeftOperand(token, left);
		checkRightOperand(token, right);
	}

	private void checkLeftOperand(Token token, @Nullable SpelNodeImpl operandExpression) {
		if (operandExpression == null) {
			throw internalException(token.startPos, SpelMessage.LEFT_OPERAND_PROBLEM);
		}
	}

	private void checkRightOperand(Token token, @Nullable SpelNodeImpl operandExpression) {
		if (operandExpression == null) {
			throw internalException(token.startPos, SpelMessage.RIGHT_OPERAND_PROBLEM);
		}
	}

	private InternalParseException internalException(int pos, SpelMessage message, Object... inserts) {
		return new InternalParseException(new SpelParseException(this.expressionString, pos, message, inserts));
	}

	private int toPos(Token t) {
		// Compress the start and end of a token into a single int
		return (t.startPos << 16) + t.endPos;
	}

	private int toPos(int start, int end) {
		return (start << 16) + end;
	}

}
