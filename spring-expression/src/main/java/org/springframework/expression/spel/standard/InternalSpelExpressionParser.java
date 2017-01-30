/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.expression.spel.standard;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Hand written SpEL parser. Instances are reusable but are not thread-safe.
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
	private final Stack<SpelNodeImpl> constructedNodes = new Stack<SpelNodeImpl>();

	// The expression being parsed
	private String expressionString;

	// The token stream constructed from that expression string
	private List<Token> tokenStream;

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
	protected SpelExpression doParseExpression(String expressionString, ParserContext context) throws ParseException {
		try {
			this.expressionString = expressionString;
			Tokenizer tokenizer = new Tokenizer(expressionString);
			tokenizer.process();
			this.tokenStream = tokenizer.getTokens();
			this.tokenStreamLength = this.tokenStream.size();
			this.tokenStreamPointer = 0;
			this.constructedNodes.clear();
			SpelNodeImpl ast = eatExpression();
			if (moreTokens()) {
				throw new SpelParseException(peekToken().startPos, SpelMessage.MORE_INPUT, toString(nextToken()));
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
	private SpelNodeImpl eatExpression() {
		SpelNodeImpl expr = eatLogicalOrExpression();
		if (moreTokens()) {
			Token t = peekToken();
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
				if (valueIfNull==null) {
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
	private SpelNodeImpl eatLogicalOrExpression() {
		SpelNodeImpl expr = eatLogicalAndExpression();
		while (peekIdentifierToken("or") || peekToken(TokenKind.SYMBOLIC_OR)) {
			Token t = nextToken();  //consume OR
			SpelNodeImpl rhExpr = eatLogicalAndExpression();
			checkOperands(t, expr, rhExpr);
			expr = new OpOr(toPos(t), expr, rhExpr);
		}
		return expr;
	}

	// logicalAndExpression : relationalExpression (AND^ relationalExpression)*;
	private SpelNodeImpl eatLogicalAndExpression() {
		SpelNodeImpl expr = eatRelationalExpression();
		while (peekIdentifierToken("and") || peekToken(TokenKind.SYMBOLIC_AND)) {
			Token t = nextToken();  // consume 'AND'
			SpelNodeImpl rhExpr = eatRelationalExpression();
			checkOperands(t, expr, rhExpr);
			expr = new OpAnd(toPos(t), expr, rhExpr);
		}
		return expr;
	}

	// relationalExpression : sumExpression (relationalOperator^ sumExpression)?;
	private SpelNodeImpl eatRelationalExpression() {
		SpelNodeImpl expr = eatSumExpression();
		Token relationalOperatorToken = maybeEatRelationalOperator();
		if (relationalOperatorToken != null) {
			Token t = nextToken();  // consume relational operator token
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
	private SpelNodeImpl eatSumExpression() {
		SpelNodeImpl expr = eatProductExpression();
		while (peekToken(TokenKind.PLUS, TokenKind.MINUS, TokenKind.INC)) {
			Token t = nextToken();//consume PLUS or MINUS or INC
			SpelNodeImpl rhExpr = eatProductExpression();
			checkRightOperand(t,rhExpr);
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
	private SpelNodeImpl eatProductExpression() {
		SpelNodeImpl expr = eatPowerIncDecExpression();
		while (peekToken(TokenKind.STAR, TokenKind.DIV, TokenKind.MOD)) {
			Token t = nextToken();  // consume STAR/DIV/MOD
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
	private SpelNodeImpl eatPowerIncDecExpression() {
		SpelNodeImpl expr = eatUnaryExpression();
		if (peekToken(TokenKind.POWER)) {
			Token t = nextToken();  //consume POWER
			SpelNodeImpl rhExpr = eatUnaryExpression();
			checkRightOperand(t,rhExpr);
			return new OperatorPower(toPos(t), expr, rhExpr);
		}

		if (expr != null && peekToken(TokenKind.INC, TokenKind.DEC)) {
			Token t = nextToken();  //consume INC/DEC
			if (t.getKind() == TokenKind.INC) {
				return new OpInc(toPos(t), true, expr);
			}
			return new OpDec(toPos(t), true, expr);
		}

		return expr;
	}

	// unaryExpression: (PLUS^ | MINUS^ | BANG^ | INC^ | DEC^) unaryExpression | primaryExpression ;
	private SpelNodeImpl eatUnaryExpression() {
		if (peekToken(TokenKind.PLUS, TokenKind.MINUS, TokenKind.NOT)) {
			Token t = nextToken();
			SpelNodeImpl expr = eatUnaryExpression();
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
			Token t = nextToken();
			SpelNodeImpl expr = eatUnaryExpression();
			if (t.getKind() == TokenKind.INC) {
				return new OpInc(toPos(t), false, expr);
			}
			return new OpDec(toPos(t), false, expr);
		}

		return eatPrimaryExpression();
	}

	// primaryExpression : startNode (node)? -> ^(EXPRESSION startNode (node)?);
	private SpelNodeImpl eatPrimaryExpression() {
		List<SpelNodeImpl> nodes = new ArrayList<SpelNodeImpl>();
		SpelNodeImpl start = eatStartNode();  // always a start node
		nodes.add(start);
		while (maybeEatNode()) {
			nodes.add(pop());
		}
		if (nodes.size() == 1) {
			return nodes.get(0);
		}
		return new CompoundExpression(toPos(start.getStartPosition(),
				nodes.get(nodes.size() - 1).getEndPosition()),
				nodes.toArray(new SpelNodeImpl[nodes.size()]));
	}

	// node : ((DOT dottedNode) | (SAFE_NAVI dottedNode) | nonDottedNode)+;
	private boolean maybeEatNode() {
		SpelNodeImpl expr = null;
		if (peekToken(TokenKind.DOT, TokenKind.SAFE_NAVI)) {
			expr = eatDottedNode();
		}
		else {
			expr = maybeEatNonDottedNode();
		}

		if (expr==null) {
			return false;
		}
		else {
			push(expr);
			return true;
		}
	}

	// nonDottedNode: indexer;
	private SpelNodeImpl maybeEatNonDottedNode() {
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
		Token t = nextToken();// it was a '.' or a '?.'
		boolean nullSafeNavigation = t.kind == TokenKind.SAFE_NAVI;
		if (maybeEatMethodOrProperty(nullSafeNavigation) || maybeEatFunctionOrVar()
				|| maybeEatProjection(nullSafeNavigation)
				|| maybeEatSelection(nullSafeNavigation)) {
			return pop();
		}
		if (peekToken() == null) {
			// unexpectedly ran out of data
			raiseInternalException(t.startPos, SpelMessage.OOD);
		}
		else {
			raiseInternalException(t.startPos, SpelMessage.UNEXPECTED_DATA_AFTER_DOT,
					toString(peekToken()));
		}
		return null;
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
		Token t = nextToken();
		Token functionOrVariableName = eatToken(TokenKind.IDENTIFIER);
		SpelNodeImpl[] args = maybeEatMethodArgs();
		if (args == null) {
			push(new VariableReference(functionOrVariableName.data, toPos(t.startPos,
					functionOrVariableName.endPos)));
			return true;
		}

		push(new FunctionReference(functionOrVariableName.data, toPos(t.startPos,
				functionOrVariableName.endPos), args));
		return true;
	}

	// methodArgs : LPAREN! (argument (COMMA! argument)* (COMMA!)?)? RPAREN!;
	private SpelNodeImpl[] maybeEatMethodArgs() {
		if (!peekToken(TokenKind.LPAREN)) {
			return null;
		}
		List<SpelNodeImpl> args = new ArrayList<SpelNodeImpl>();
		consumeArguments(args);
		eatToken(TokenKind.RPAREN);
		return args.toArray(new SpelNodeImpl[args.size()]);
	}

	private void eatConstructorArgs(List<SpelNodeImpl> accumulatedArguments) {
		if (!peekToken(TokenKind.LPAREN)) {
			throw new InternalParseException(new SpelParseException(this.expressionString,positionOf(peekToken()),SpelMessage.MISSING_CONSTRUCTOR_ARGS));
		}
		consumeArguments(accumulatedArguments);
		eatToken(TokenKind.RPAREN);
	}

	/**
	 * Used for consuming arguments for either a method or a constructor call
	 */
	private void consumeArguments(List<SpelNodeImpl> accumulatedArguments) {
		int pos = peekToken().startPos;
		Token next;
		do {
			nextToken();  // consume ( (first time through) or comma (subsequent times)
			Token t = peekToken();
			if (t == null) {
				raiseInternalException(pos, SpelMessage.RUN_OUT_OF_ARGUMENTS);
			}
			if (t.kind != TokenKind.RPAREN) {
				accumulatedArguments.add(eatExpression());
			}
			next = peekToken();
		}
		while (next != null && next.kind == TokenKind.COMMA);

		if (next == null) {
			raiseInternalException(pos, SpelMessage.RUN_OUT_OF_ARGUMENTS);
		}
	}

	private int positionOf(Token t) {
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
			Token beanRefToken = nextToken();
			Token beanNameToken = null;
			String beanName = null;
			if (peekToken(TokenKind.IDENTIFIER)) {
				beanNameToken = eatToken(TokenKind.IDENTIFIER);
				beanName = beanNameToken.data;
			}
			else if (peekToken(TokenKind.LITERAL_STRING)) {
				beanNameToken = eatToken(TokenKind.LITERAL_STRING);
				beanName = beanNameToken.stringValue();
				beanName = beanName.substring(1, beanName.length() - 1);
			}
			else {
				raiseInternalException(beanRefToken.startPos,
						SpelMessage.INVALID_BEAN_REFERENCE);
			}

			BeanReference beanReference = null;
			if (beanRefToken.getKind() == TokenKind.FACTORY_BEAN_REF) {
				String beanNameString = new StringBuilder().append(TokenKind.FACTORY_BEAN_REF.tokenChars).append(beanName).toString();
				beanReference = new BeanReference(toPos(beanRefToken.startPos,beanNameToken.endPos),beanNameString);
			}
			else {
				beanReference = new BeanReference(toPos(beanNameToken) ,beanName);
			}
			this.constructedNodes.push(beanReference);
			return true;
		}
		return false;
	}

	private boolean maybeEatTypeReference() {
		if (peekToken(TokenKind.IDENTIFIER)) {
			Token typeName = peekToken();
			if (!typeName.stringValue().equals("T")) {
				return false;
			}
			// It looks like a type reference but is T being used as a map key?
			Token t = nextToken();
			if (peekToken(TokenKind.RSQUARE)) {
				// looks like 'T]' (T is map key)
				push(new PropertyOrFieldReference(false,t.data,toPos(t)));
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
			this.constructedNodes.push(new TypeReference(toPos(typeName),node,dims));
			return true;
		}
		return false;
	}

	private boolean maybeEatNullReference() {
		if (peekToken(TokenKind.IDENTIFIER)) {
			Token nullToken = peekToken();
			if (!nullToken.stringValue().equalsIgnoreCase("null")) {
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
		SpelNodeImpl expr = eatExpression();
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
		SpelNodeImpl expr = null;
		Token closingCurly = peekToken();
		if (peekToken(TokenKind.RCURLY, true)) {
			// empty list '{}'
			expr = new InlineList(toPos(t.startPos,closingCurly.endPos));
		}
		else if (peekToken(TokenKind.COLON,true)) {
			closingCurly = eatToken(TokenKind.RCURLY);
			// empty map '{:}'
			expr = new InlineMap(toPos(t.startPos,closingCurly.endPos));
		}
		else {
			SpelNodeImpl firstExpression = eatExpression();
			// Next is either:
			// '}' - end of list
			// ',' - more expressions in this list
			// ':' - this is a map!

			if (peekToken(TokenKind.RCURLY)) { // list with one item in it
				List<SpelNodeImpl> listElements = new ArrayList<SpelNodeImpl>();
				listElements.add(firstExpression);
				closingCurly = eatToken(TokenKind.RCURLY);
				expr = new InlineList(toPos(t.startPos,closingCurly.endPos),listElements.toArray(new SpelNodeImpl[listElements.size()]));
			}
			else if (peekToken(TokenKind.COMMA, true)) { // multi item list
				List<SpelNodeImpl> listElements = new ArrayList<SpelNodeImpl>();
				listElements.add(firstExpression);
				do {
					listElements.add(eatExpression());
				}
				while (peekToken(TokenKind.COMMA,true));
				closingCurly = eatToken(TokenKind.RCURLY);
				expr = new InlineList(toPos(t.startPos,closingCurly.endPos),listElements.toArray(new SpelNodeImpl[listElements.size()]));

			}
			else if (peekToken(TokenKind.COLON, true)) {  // map!
				List<SpelNodeImpl> mapElements = new ArrayList<SpelNodeImpl>();
				mapElements.add(firstExpression);
				mapElements.add(eatExpression());
				while (peekToken(TokenKind.COMMA,true)) {
					mapElements.add(eatExpression());
					eatToken(TokenKind.COLON);
					mapElements.add(eatExpression());
				}
				closingCurly = eatToken(TokenKind.RCURLY);
				expr = new InlineMap(toPos(t.startPos,closingCurly.endPos),mapElements.toArray(new SpelNodeImpl[mapElements.size()]));
			}
			else {
				raiseInternalException(t.startPos, SpelMessage.OOD);
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
		SpelNodeImpl expr = eatExpression();
		eatToken(TokenKind.RSQUARE);
		this.constructedNodes.push(new Indexer(toPos(t), expr));
		return true;
	}

	private boolean maybeEatSelection(boolean nullSafeNavigation) {
		Token t = peekToken();
		if (!peekSelectToken()) {
			return false;
		}
		nextToken();
		SpelNodeImpl expr = eatExpression();
		if (expr == null) {
			raiseInternalException(toPos(t), SpelMessage.MISSING_SELECTION_EXPRESSION);
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
		LinkedList<SpelNodeImpl> qualifiedIdPieces = new LinkedList<SpelNodeImpl>();
		Token node = peekToken();
		while (isValidQualifiedId(node)) {
			nextToken();
			if (node.kind != TokenKind.DOT) {
				qualifiedIdPieces.add(new Identifier(node.stringValue(),toPos(node)));
			}
			node = peekToken();
		}
		if (qualifiedIdPieces.isEmpty()) {
			if (node == null) {
				raiseInternalException( this.expressionString.length(), SpelMessage.OOD);
			}
			raiseInternalException(node.startPos, SpelMessage.NOT_EXPECTED_TOKEN,
					"qualified ID", node.getKind().toString().toLowerCase());
		}
		int pos = toPos(qualifiedIdPieces.getFirst().getStartPosition(), qualifiedIdPieces.getLast().getEndPosition());
		return new QualifiedIdentifier(pos, qualifiedIdPieces.toArray(new SpelNodeImpl[qualifiedIdPieces.size()]));
	}

	private boolean isValidQualifiedId(Token node) {
		if (node == null || node.kind == TokenKind.LITERAL_STRING) {
			return false;
		}
		if (node.kind == TokenKind.DOT || node.kind == TokenKind.IDENTIFIER) {
			return true;
		}
		String value = node.stringValue();
		return (StringUtils.hasLength(value) && VALID_QUALIFIED_ID_PATTERN.matcher(value).matches());
	}

	// This is complicated due to the support for dollars in identifiers.  Dollars are normally separate tokens but
	// there we want to combine a series of identifiers and dollars into a single identifier
	private boolean maybeEatMethodOrProperty(boolean nullSafeNavigation) {
		if (peekToken(TokenKind.IDENTIFIER)) {
			Token methodOrPropertyName = nextToken();
			SpelNodeImpl[] args = maybeEatMethodArgs();
			if (args==null) {
				// property
				push(new PropertyOrFieldReference(nullSafeNavigation, methodOrPropertyName.data,toPos(methodOrPropertyName)));
				return true;
			}
			// methodreference
			push(new MethodReference(nullSafeNavigation, methodOrPropertyName.data, toPos(methodOrPropertyName), args));
			// TODO what is the end position for a method reference? the name or the last arg?
			return true;
		}
		return false;
	}

	//constructor
    //:	('new' qualifiedId LPAREN) => 'new' qualifiedId ctorArgs -> ^(CONSTRUCTOR qualifiedId ctorArgs)
	private boolean maybeEatConstructorReference() {
		if (peekIdentifierToken("new")) {
			Token newToken = nextToken();
			// It looks like a constructor reference but is NEW being used as a map key?
			if (peekToken(TokenKind.RSQUARE)) {
				// looks like 'NEW]' (so NEW used as map key)
				push(new PropertyOrFieldReference(false, newToken.data ,toPos(newToken)));
				return true;
			}
			SpelNodeImpl possiblyQualifiedConstructorName = eatPossiblyQualifiedId();
			List<SpelNodeImpl> nodes = new ArrayList<SpelNodeImpl>();
			nodes.add(possiblyQualifiedConstructorName);
			if (peekToken(TokenKind.LSQUARE)) {
				// array initializer
				List<SpelNodeImpl> dimensions = new ArrayList<SpelNodeImpl>();
				while (peekToken(TokenKind.LSQUARE,true)) {
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
				push(new ConstructorReference(toPos(newToken), dimensions.toArray(new SpelNodeImpl[dimensions.size()]),
						nodes.toArray(new SpelNodeImpl[nodes.size()])));
			}
			else {
				// regular constructor invocation
				eatConstructorArgs(nodes);
				// TODO correct end position?
				push(new ConstructorReference(toPos(newToken),
						nodes.toArray(new SpelNodeImpl[nodes.size()])));
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
			push(Literal.getIntLiteral(t.data, toPos(t), 10));
		}
		else if (t.kind == TokenKind.LITERAL_LONG) {
			push(Literal.getLongLiteral(t.data, toPos(t), 10));
		}
		else if (t.kind == TokenKind.LITERAL_HEXINT) {
			push(Literal.getIntLiteral(t.data, toPos(t), 16));
		}
		else if (t.kind == TokenKind.LITERAL_HEXLONG) {
			push(Literal.getLongLiteral(t.data, toPos(t), 16));
		}
		else if (t.kind == TokenKind.LITERAL_REAL) {
			push(Literal.getRealLiteral(t.data, toPos(t), false));
		}
		else if (t.kind == TokenKind.LITERAL_REAL_FLOAT) {
			push(Literal.getRealLiteral(t.data, toPos(t), true));
		}
		else if (peekIdentifierToken("true")) {
			push(new BooleanLiteral(t.data, toPos(t), true));
		}
		else if (peekIdentifierToken("false")) {
			push(new BooleanLiteral(t.data, toPos(t), false));
		}
		else if (t.kind == TokenKind.LITERAL_STRING) {
			push(new StringLiteral(t.data, toPos(t), t.data));
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
			raiseInternalException( this.expressionString.length(), SpelMessage.OOD);
		}
		if (t.kind != expectedKind) {
			raiseInternalException(t.startPos, SpelMessage.NOT_EXPECTED_TOKEN,
					expectedKind.toString().toLowerCase(), t.getKind().toString().toLowerCase());
		}
		return t;
	}

	private boolean peekToken(TokenKind desiredTokenKind) {
		return peekToken(desiredTokenKind,false);
	}

	private boolean peekToken(TokenKind desiredTokenKind, boolean consumeIfMatched) {
		if (!moreTokens()) {
			return false;
		}
		Token t = peekToken();
		if (t.kind == desiredTokenKind) {
			if (consumeIfMatched) {
				this.tokenStreamPointer++;
			}
			return true;
		}

		if (desiredTokenKind == TokenKind.IDENTIFIER) {
			// might be one of the textual forms of the operators (e.g. NE for != ) - in which case we can treat it as an identifier
			// The list is represented here: Tokenizer.alternativeOperatorNames and those ones are in order in the TokenKind enum
			if (t.kind.ordinal() >= TokenKind.DIV.ordinal() && t.kind.ordinal() <= TokenKind.NOT.ordinal() && t.data != null) {
				// if t.data were null, we'd know it wasn't the textual form, it was the symbol form
				return true;
			}
		}
		return false;
	}

	private boolean peekToken(TokenKind possible1, TokenKind possible2) {
		if (!moreTokens()) {
			return false;
		}
		Token t = peekToken();
		return (t.kind == possible1 || t.kind == possible2);
	}

	private boolean peekToken(TokenKind possible1, TokenKind possible2, TokenKind possible3) {
		if (!moreTokens()) {
			return false;
		}
		Token t = peekToken();
		return (t.kind == possible1 || t.kind == possible2 || t.kind == possible3);
	}

	private boolean peekIdentifierToken(String identifierString) {
		if (!moreTokens()) {
			return false;
		}
		Token t = peekToken();
		return (t.kind == TokenKind.IDENTIFIER && t.stringValue().equalsIgnoreCase(identifierString));
	}

	private boolean peekSelectToken() {
		if (!moreTokens()) {
			return false;
		}
		Token t = peekToken();
		return (t.kind == TokenKind.SELECT || t.kind == TokenKind.SELECT_FIRST || t.kind == TokenKind.SELECT_LAST);
	}

	private boolean moreTokens() {
		return this.tokenStreamPointer<this.tokenStream.size();
	}

	private Token nextToken() {
		if (this.tokenStreamPointer >= this.tokenStreamLength) {
			return null;
		}
		return this.tokenStream.get(this.tokenStreamPointer++);
	}

	private Token peekToken() {
		if (this.tokenStreamPointer >= this.tokenStreamLength) {
			return null;
		}
		return this.tokenStream.get(this.tokenStreamPointer);
	}

	private void raiseInternalException(int pos, SpelMessage message, Object... inserts) {
		throw new InternalParseException(new SpelParseException(this.expressionString, pos, message, inserts));
	}

	public String toString(Token t) {
		if (t.getKind().hasPayload()) {
			return t.stringValue();
		}
		return t.kind.toString().toLowerCase();
	}

	private void checkOperands(Token token, SpelNodeImpl left, SpelNodeImpl right) {
		checkLeftOperand(token, left);
		checkRightOperand(token, right);
	}

	private void checkLeftOperand(Token token, SpelNodeImpl operandExpression) {
		if (operandExpression==null) {
			raiseInternalException(token.startPos, SpelMessage.LEFT_OPERAND_PROBLEM);
		}
	}

	private void checkRightOperand(Token token, SpelNodeImpl operandExpression) {
		if (operandExpression == null) {
			raiseInternalException(token.startPos, SpelMessage.RIGHT_OPERAND_PROBLEM);
		}
	}

	/**
	 * Compress the start and end of a token into a single int.
	 */
	private int toPos(Token t) {
		return (t.startPos<<16) + t.endPos;
	}

	private int toPos(int start,int end) {
		return (start<<16) + end;
	}

}
