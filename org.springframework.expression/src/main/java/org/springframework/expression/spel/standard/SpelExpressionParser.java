/*
 * Copyright 2008-2009 the original author or authors.
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
import java.util.List;
import java.util.Stack;

import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateAwareExpressionParser;
import org.springframework.expression.spel.SpelExpression;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.ast.Assign;
import org.springframework.expression.spel.ast.BooleanLiteral;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.ConstructorReference;
import org.springframework.expression.spel.ast.Elvis;
import org.springframework.expression.spel.ast.FunctionReference;
import org.springframework.expression.spel.ast.Identifier;
import org.springframework.expression.spel.ast.Indexer;
import org.springframework.expression.spel.ast.Literal;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.NullLiteral;
import org.springframework.expression.spel.ast.OpEQ;
import org.springframework.expression.spel.ast.OpGE;
import org.springframework.expression.spel.ast.OpGT;
import org.springframework.expression.spel.ast.OpLE;
import org.springframework.expression.spel.ast.OpLT;
import org.springframework.expression.spel.ast.OpNE;
import org.springframework.expression.spel.ast.OperatorAnd;
import org.springframework.expression.spel.ast.OperatorDivide;
import org.springframework.expression.spel.ast.OperatorInstanceof;
import org.springframework.expression.spel.ast.OperatorMatches;
import org.springframework.expression.spel.ast.OperatorMinus;
import org.springframework.expression.spel.ast.OperatorModulus;
import org.springframework.expression.spel.ast.OperatorMultiply;
import org.springframework.expression.spel.ast.OperatorNot;
import org.springframework.expression.spel.ast.OperatorOr;
import org.springframework.expression.spel.ast.OperatorPlus;
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


/**
 * Hand written SpEL parser.  Instances are reusable.
 * 
 * @author Andy Clement
 * @since 3.0
 */
public class SpelExpressionParser extends TemplateAwareExpressionParser {

	// The expression being parsed
	private String expressionString;	
	
	// The token stream constructed from that expression string
	private List<Token> tokenStream;
	
	// length of a populated token stream
	private int tokenStreamLength;
	
	// Current location in the token stream when processing tokens
	private int tokenStreamPointer;
	
	// For rules that build nodes, they are stacked here for return
	private Stack<SpelNodeImpl> constructedNodes = new Stack<SpelNodeImpl>();

	public SpelExpressionParser() {
	}

	public SpelExpression parse(String expressionString) throws ParseException {
		return doParseExpression(expressionString, null);
	}
	
	protected SpelExpression doParseExpression(String expressionString, ParserContext context) throws ParseException {
		try {
			Tokenizer tokenizer = new Tokenizer(expressionString);
			tokenizer.process();
			this.expressionString = expressionString;
			tokenStream = tokenizer.getTokens();
			tokenStreamLength = tokenStream.size();
			tokenStreamPointer = 0;
			constructedNodes.clear();
			SpelNode ast = eatExpression();
			if (moreTokens()) {
				throw new SpelParseException(peekToken().startpos,SpelMessages.MORE_INPUT,toString(nextToken()));
			}
			assert constructedNodes.isEmpty();
			return new SpelExpression(expressionString,ast);	
		} catch (InternalParseException ipe) {
			throw ipe.getCause();
		}
	}
	
	public String toString(Token t) {
		if (t.getKind().hasPayload()) {
			return t.stringValue();
		} else {
			return t.kind.toString().toLowerCase();
		}
	}
	
	//	expression
	//    : logicalOrExpression
	//      ( (ASSIGN^ logicalOrExpression) 
	//	    | (DEFAULT^ logicalOrExpression) 
	//	    | (QMARK^ expression COLON! expression))?;
	private SpelNodeImpl eatExpression() {
		SpelNodeImpl expr = eatLogicalOrExpression();
		if (moreTokens()) {
			Token t = peekToken();
			if (t.kind==TokenKind.ASSIGN) { // a=b
				nextToken();
				SpelNodeImpl assignedValue = eatLogicalOrExpression();
				return new Assign(toPos(t),expr,assignedValue);
			} else if (t.kind==TokenKind.ELVIS) { // a?:b (a if it isn't null, otherwise b)
				nextToken(); // elvis has left the building
				SpelNodeImpl valueIfNull = eatLogicalOrExpression();
				return new Elvis(toPos(t),expr,valueIfNull);
			} else if (t.kind==TokenKind.QMARK) { // a?b:c
				nextToken();
				SpelNodeImpl ifTrueExprValue = eatLogicalOrExpression();	
				eatToken(TokenKind.COLON);
				SpelNodeImpl ifFalseExprValue = eatLogicalOrExpression();	
				return new Ternary(toPos(t),expr,ifTrueExprValue,ifFalseExprValue);
			}
		}
		return expr;
	}
	                        
	//logicalOrExpression : logicalAndExpression (OR^ logicalAndExpression)*;
	private SpelNodeImpl eatLogicalOrExpression() {
		SpelNodeImpl expr = eatLogicalAndExpression();
		while (peekIdentifierToken("or")) {
			Token t = nextToken();//consume OR
			SpelNodeImpl expr2 = eatLogicalAndExpression();
			checkRightOperand(t,expr2);
			expr = new OperatorOr(toPos(t),expr,expr2);
		}
		return expr;
	}
	
	private void checkRightOperand(Token token, SpelNodeImpl operandExpression) {
		if (operandExpression==null) {
			throw new InternalParseException(new SpelParseException(token.startpos,SpelMessages.RIGHT_OPERAND_PROBLEM));
		}
	}

	//logicalAndExpression : relationalExpression (AND^ relationalExpression)*;
	private SpelNodeImpl eatLogicalAndExpression() {
		SpelNodeImpl expr = eatRelationalExpression();
		while (peekIdentifierToken("and")) {
			Token t = nextToken();// consume 'AND'
			SpelNodeImpl rightExpr = eatRelationalExpression();
			checkRightOperand(t,rightExpr);
			expr = new OperatorAnd(toPos(t),expr,rightExpr);
		}
		return expr;
	}
	
	//relationalExpression : sumExpression (relationalOperator^ sumExpression)?;
	private SpelNodeImpl eatRelationalExpression() {
		SpelNodeImpl expr = eatSumExpression();
		Token relationalOperatorToken = maybeEatRelationalOperator();
		if (relationalOperatorToken!=null) {
			Token t = nextToken();//consume relational operator token
			SpelNodeImpl expr2 = eatSumExpression();
			checkRightOperand(t,expr2);
			if (relationalOperatorToken.isNumericRelationalOperator()) {
				if (relationalOperatorToken.isGreaterThan()) {
					return new OpGT(toPos(t),expr,expr2);
				} else if (relationalOperatorToken.isLessThan()) {
					return new OpLT(toPos(t),expr,expr2);
				} else if (relationalOperatorToken.isLessThanOrEqual()) {
					return new OpLE(toPos(t),expr,expr2);
				} else if (relationalOperatorToken.isGreaterThanOrEqual()) {
					return new OpGE(toPos(t),expr,expr2);
				} else if (relationalOperatorToken.isEquality()) {
					return new OpEQ(toPos(t),expr,expr2);
				} else {
					assert relationalOperatorToken.kind==TokenKind.NE;
					return new OpNE(toPos(t),expr,expr2);
				}
			}
			if (relationalOperatorToken.kind==TokenKind.INSTANCEOF) {
				return new OperatorInstanceof(toPos(t),expr,expr2);
			} else if (relationalOperatorToken.kind==TokenKind.MATCHES) {
				return new OperatorMatches(toPos(t),expr,expr2);
			} else {
				assert relationalOperatorToken.kind==TokenKind.BETWEEN;
				return new org.springframework.expression.spel.ast.OperatorBetween(toPos(t),expr,expr2);
			}
		}
		return expr;
	}
	
	//sumExpression: productExpression ( (PLUS^ | MINUS^) productExpression)*;
	private SpelNodeImpl eatSumExpression() {
		SpelNodeImpl expr = eatProductExpression();		
		while (peekToken(TokenKind.PLUS,TokenKind.MINUS)) {
			Token t = nextToken();//consume PLUS or MINUS
			SpelNodeImpl rhOperand = eatProductExpression();
			checkRightOperand(t,rhOperand);
			if (t.getKind()==TokenKind.PLUS) {
				expr = new OperatorPlus(toPos(t),expr,rhOperand);
			} else {
				expr = new OperatorMinus(toPos(t),expr,rhOperand);
			}
		}
		return expr;
	}
	
	//productExpression: powerExpr ((STAR^ | DIV^| MOD^) powerExpr)* ;
	private SpelNodeImpl eatProductExpression() {
		SpelNodeImpl expr = eatPowerExpression();
		while (peekToken(TokenKind.STAR,TokenKind.DIV,TokenKind.MOD)) {
			Token t = nextToken(); // consume STAR/DIV/MOD
			SpelNodeImpl expr2 = eatPowerExpression();
			checkRightOperand(t,expr2);
			if (t.getKind()==TokenKind.STAR) {
				expr = new OperatorMultiply(toPos(t),expr,expr2);
			} else if (t.getKind()==TokenKind.DIV) {
				expr = new OperatorDivide(toPos(t),expr,expr2);
			} else {
				expr = new OperatorModulus(toPos(t),expr,expr2);
			}
		}
		return expr;
	}
	
	//powerExpr  : unaryExpression (POWER^ unaryExpression)? ;
	private SpelNodeImpl eatPowerExpression() {
		SpelNodeImpl expr = eatUnaryExpression();
		if (peekToken(TokenKind.POWER)) {
			Token t = nextToken();//consume POWER
			SpelNodeImpl expr2 = eatUnaryExpression();
			checkRightOperand(t,expr2);
			return new OperatorPower(toPos(t),expr, expr2);
		}
		return expr;
	}

	//unaryExpression:	(PLUS^ | MINUS^ | BANG^) unaryExpression |	primaryExpression ;
	private SpelNodeImpl eatUnaryExpression() {
		if (peekToken(TokenKind.PLUS) || peekToken(TokenKind.MINUS) || peekToken(TokenKind.BANG)) {
			Token t = nextToken();
			SpelNodeImpl expr = eatUnaryExpression();
			if (t.kind==TokenKind.BANG) {
				return new OperatorNot(toPos(t),expr);
			} else if (t.kind==TokenKind.PLUS) {
				return new OperatorPlus(toPos(t),expr);
			} else {
				assert t.kind==TokenKind.MINUS;
				return new OperatorMinus(toPos(t),expr);
			}
		} else {
			return eatPrimaryExpression();
		}
	}
	
	//primaryExpression : startNode (node)? -> ^(EXPRESSION startNode (node)?);
	private SpelNodeImpl eatPrimaryExpression() {
		List<SpelNodeImpl> nodes = new ArrayList<SpelNodeImpl>();
		SpelNodeImpl start = eatStartNode();
		nodes.add(start);
		while (maybeEatNode()) {
			nodes.add(pop());
		}
		if (nodes.size()==1) {
			return nodes.get(0);
		} else {
			return new CompoundExpression(toPos(start.getStartPosition(),nodes.get(nodes.size()-1).getEndPosition()),nodes.toArray(new SpelNodeImpl[nodes.size()]));
		}
	}
	
	private int toPos(int start,int end) {
		return (start<<16)+end;
	}
	
	//node : ((DOT dottedNode) | nonDottedNode)+;
	private boolean maybeEatNode() {
		Token t = peekToken();
		SpelNodeImpl expr = null;
		if (t!=null && peekToken(TokenKind.DOT,TokenKind.SAFE_NAVI)) {
			expr = eatDottedNode();
		} else {
			expr = maybeEatNonDottedNode();
		}
		if (expr==null) {
			return false;
		} else {
			push(expr);
			return true;
		}
	}
	
	//nonDottedNode: indexer;
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
		//eatToken(TokenKind.DOT);
		boolean nullSafeNavigation = t.kind==TokenKind.SAFE_NAVI;
		if (maybeEatMethodOrProperty(nullSafeNavigation) || maybeEatFunctionOrVar() || maybeEatProjection(nullSafeNavigation) || maybeEatSelection(nullSafeNavigation)) {
			return pop();
		}
		throw new InternalParseException(new SpelParseException(expressionString,t.startpos,SpelMessages.UNEXPECTED_DATA_AFTER_DOT,toString(peekToken())));
	}
	
//	functionOrVar 
//    : (POUND ID LPAREN) => function
//    | var
//    ;
//    
//function : POUND id=ID methodArgs -> ^(FUNCTIONREF[$id] methodArgs);
//    
//var : POUND id=ID -> ^(VARIABLEREF[$id]); 

	private boolean maybeEatFunctionOrVar() {
		if (!peekToken(TokenKind.HASH)) {
			return false;
		}
		Token t = nextToken();
		Token functionOrVariableName = eatToken(TokenKind.IDENTIFIER);
		SpelNodeImpl[] args = maybeEatMethodArgs();
		if (args==null) {
			push(new VariableReference(functionOrVariableName.data,toPos(t.startpos,functionOrVariableName.endpos)));
			return true;
		} else {
			push(new FunctionReference(functionOrVariableName.data,toPos(t.startpos,functionOrVariableName.endpos),args));
			return true;
		}
	}
	
	
	//methodArgs : LPAREN! (argument (COMMA! argument)* (COMMA!)?)? RPAREN!;
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
			throw new InternalParseException(new SpelParseException(expressionString,positionOf(peekToken()),SpelMessages.MISSING_CONSTRUCTOR_ARGS));
		}
		consumeArguments(accumulatedArguments);
		eatToken(TokenKind.RPAREN);
	}
	
	/**
	 * Used for consuming arguments for either a method or a constructor call
	 */
	private void consumeArguments(List<SpelNodeImpl> accumulatedArguments) {
		int pos = peekToken().startpos;
		Token next = null;
		do {
			nextToken();// consume ( (first time through) or comma (subsequent times)
			Token t = peekToken();
			if (t==null) {
				raiseInternalException(pos,SpelMessages.RUN_OUT_OF_ARGUMENTS);
			}
			if (t.kind!=TokenKind.RPAREN) {
				accumulatedArguments.add(eatExpression());
			}
			next = peekToken();
		} while (next!=null && next.kind==TokenKind.COMMA);
		if (next==null) {
			raiseInternalException(pos,SpelMessages.RUN_OUT_OF_ARGUMENTS);
		}
	}
	
	private int positionOf(Token t) {
		if (t==null) {
			// if null assume the problem is because the right token was 
			// not found at the end of the expression
			return expressionString.length();
		} else {
			return t.startpos;
		}
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
		} else if (maybeEatParenExpression()) {
			return pop();
		} else if (maybeEatTypeReference() || maybeEatNullReference() || maybeEatConstructorReference() || maybeEatMethodOrProperty(false) || maybeEatFunctionOrVar()) {
			return pop();
		} else if (maybeEatProjection(false) || maybeEatSelection(false) || maybeEatIndexer()) {
			return pop();
		} else {
			return null;
		}
	}
	


	private boolean maybeEatTypeReference() {
		if (peekToken(TokenKind.IDENTIFIER)) {
			Token typeName = peekToken();
			if (!typeName.stringValue().equals("T")) {
				return false;
			}
			nextToken();
			eatToken(TokenKind.LPAREN);
			SpelNodeImpl node = eatPossiblyQualifiedId();
			// dotted qualified id
			eatToken(TokenKind.RPAREN);
			constructedNodes.push(new TypeReference(toPos(typeName),node));
			return true;
		}
		return false;
	}

	private boolean maybeEatNullReference() {
		if (peekToken(TokenKind.IDENTIFIER)) {
			Token nullToken = peekToken();
			if (!nullToken.stringValue().equals("null")) {
				return false;
			}
			nextToken();
			constructedNodes.push(new NullLiteral(toPos(nullToken)));
			return true;
		}
		return false;
	}

	//projection: PROJECT^ expression RCURLY!;
	private boolean maybeEatProjection(boolean nullSafeNavigation) {
		Token t = peekToken();
		if (!peekToken(TokenKind.PROJECT)) {
			return false;
		}
		nextToken();
		SpelNodeImpl expr = eatExpression();
		eatToken(TokenKind.RSQUARE);
		constructedNodes.push(new Projection(nullSafeNavigation, toPos(t),expr));
		return true;
	}
	
	private boolean maybeEatIndexer() {
		Token t = peekToken();
		if (!peekToken(TokenKind.LSQUARE)) {
			return false;
		}
		nextToken();
		SpelNodeImpl expr = eatExpression();
		eatToken(TokenKind.RSQUARE);
		constructedNodes.push(new Indexer(toPos(t),expr));
		return true;
	}
	
	private boolean maybeEatSelection(boolean nullSafeNavigation) {
		Token t = peekToken();
		if (!peekSelectToken()) {
			return false;
		}
		nextToken();
		SpelNodeImpl expr = eatExpression();
		eatToken(TokenKind.RSQUARE);
		if (t.kind==TokenKind.SELECT_FIRST) {			
			constructedNodes.push(new Selection(nullSafeNavigation,Selection.FIRST,toPos(t),expr));
		} else if (t.kind==TokenKind.SELECT_LAST) {
			constructedNodes.push(new Selection(nullSafeNavigation,Selection.LAST,toPos(t),expr));
		} else {
			constructedNodes.push(new Selection(nullSafeNavigation,Selection.ALL,toPos(t),expr));
		}
		return true;
	}

	private SpelNodeImpl eatPossiblyQualifiedId() {
		List<SpelNodeImpl> qualifiedIdPieces = new ArrayList<SpelNodeImpl>();
		Token startnode = eatToken(TokenKind.IDENTIFIER);
		qualifiedIdPieces.add(new Identifier(startnode.stringValue(),toPos(startnode)));
		while (peekToken(TokenKind.DOT)) {
			nextToken();
			Token node = eatToken(TokenKind.IDENTIFIER);
			qualifiedIdPieces.add(new Identifier(node.stringValue(),toPos(node)));			
		}
		return new QualifiedIdentifier(toPos(startnode.startpos,qualifiedIdPieces.get(qualifiedIdPieces.size()-1).getEndPosition()),qualifiedIdPieces.toArray(new SpelNodeImpl[qualifiedIdPieces.size()]));
	}
	
	private boolean maybeEatMethodOrProperty(boolean nullSafeNavigation) {
		if (peekToken(TokenKind.IDENTIFIER)) {
			Token methodOrPropertyName = nextToken();
			SpelNodeImpl[] args = maybeEatMethodArgs();
			if (args==null) {
				// property
				push(new PropertyOrFieldReference(nullSafeNavigation, methodOrPropertyName.data,toPos(methodOrPropertyName)));
				return true;
			} else {
				// methodreference
				push(new MethodReference(nullSafeNavigation, methodOrPropertyName.data,toPos(methodOrPropertyName),args));
				// TODO what is the end position for a method reference? the name or the last arg?
				return true;
			}
		}
		return false;
	}
	
	//constructor  
//	:	('new' qualifiedId LPAREN) => 'new' qualifiedId ctorArgs -> ^(CONSTRUCTOR qualifiedId ctorArgs)
//	;
	private boolean maybeEatConstructorReference() {
		if (peekIdentifierToken("new")) {
			Token newToken = nextToken();
			SpelNodeImpl possiblyQualifiedConstructorName = eatPossiblyQualifiedId();
			List<SpelNodeImpl> nodes = new ArrayList<SpelNodeImpl>();
			nodes.add(possiblyQualifiedConstructorName);
			eatConstructorArgs(nodes);
			push(new ConstructorReference(toPos(newToken),nodes.toArray(new SpelNodeImpl[nodes.size()]))); // TODO  correct end position?
			return true;
		}
		return false;
	}


	private void push(SpelNodeImpl newNode) {
		constructedNodes.push(newNode);
	}

	private SpelNodeImpl pop() {
		return constructedNodes.pop();
	}
	
//	literal: 
//  INTEGER_LITERAL 
//	| boolLiteral
//	| STRING_LITERAL
	
//  | HEXADECIMAL_INTEGER_LITERAL 
//  | REAL_LITERAL
//	| DQ_STRING_LITERAL
//	| NULL_LITERAL
//	;
	private boolean maybeEatLiteral() {
		Token t = peekToken();
		if (t==null) {
			return false;
		}
		if (t.kind==TokenKind.LITERAL_INT) {
			nextToken();
			push(Literal.getIntLiteral(t.data, toPos(t), 10));
			return true;
		} else if (t.kind==TokenKind.LITERAL_LONG) {
			nextToken();
			push(Literal.getLongLiteral(t.data, toPos(t), 10));
			return true;
		} else if (t.kind==TokenKind.LITERAL_HEXINT) {
			nextToken();
			push(Literal.getIntLiteral(t.data, toPos(t), 16));
			return true;
		} else if (t.kind==TokenKind.LITERAL_HEXLONG) {
			nextToken();
			push(Literal.getLongLiteral(t.data, toPos(t), 16));
			return true;
		} else if (t.kind==TokenKind.LITERAL_REAL) {
			nextToken();
			push(Literal.getRealLiteral(t.data, toPos(t),false));
			return true;
		} else if (t.kind==TokenKind.LITERAL_REAL_FLOAT) {
			nextToken();
			push(Literal.getRealLiteral(t.data, toPos(t), true));
			return true;
		} else if (peekIdentifierToken("true")) { 
			nextToken(); 
			push(new BooleanLiteral(t.data,toPos(t),true));
			return true;
		} else if (peekIdentifierToken("false")) {
			nextToken();
			push(new BooleanLiteral(t.data,toPos(t),false));
			return true;
		} else if (t.kind==TokenKind.LITERAL_STRING) {
			nextToken();
			push(new StringLiteral(t.data,toPos(t),t.data));
			return true;
		}
		return false;
	}

	private int toPos(Token t) {
		return (t.startpos<<16)+t.endpos;
	}

	//parenExpr : LPAREN! expression RPAREN!;
	private boolean maybeEatParenExpression() {
		if (peekToken(TokenKind.LPAREN)) {
			nextToken();
			SpelNodeImpl expr = eatExpression();
			eatToken(TokenKind.RPAREN);
			push(expr);
			return true;
		} else {
			return false;
		}
	}
	

	// relationalOperator
	// : EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL | INSTANCEOF 
	// | BETWEEN | MATCHES 
	private Token maybeEatRelationalOperator() {
		Token t = peekToken();
		if (t==null) {
			return null; 
		}
		if (t.isNumericRelationalOperator()) {
			return t;
		}
		if (t.isIdentifier()) {
			String idString = t.stringValue();
			if (idString.equalsIgnoreCase("instanceof")) {
				return t.asInstanceOfToken();
			} else if (idString.equalsIgnoreCase("matches")) {
				return t.asMatchesToken();
			} else if (idString.equalsIgnoreCase("between")) {
				return t.asBetweenToken();
			}			
		}
		return null;
	}

	private Token eatToken(TokenKind expectedKind) {
		assert moreTokens();
		Token t = nextToken();
		if (t==null) {
			raiseInternalException( expressionString.length(), SpelMessages.OOD);
		}
		if (t.kind!=expectedKind) { 
			raiseInternalException(t.startpos,SpelMessages.NOT_EXPECTED_TOKEN, expectedKind.toString().toLowerCase(),t.getKind().toString().toLowerCase());
		}
		return t;
	}

	private boolean peekToken(TokenKind desiredTokenKind) {
		if (!moreTokens()) return false;
		Token t = peekToken();
		return t.kind==desiredTokenKind;
	}

	private boolean peekToken(TokenKind possible1,TokenKind possible2) {
		if (!moreTokens()) return false;
		Token t = peekToken();
		return t.kind==possible1 || t.kind==possible2;
	}

	private boolean peekToken(TokenKind possible1,TokenKind possible2, TokenKind possible3) {
		if (!moreTokens()) return false;
		Token t = peekToken();
		return t.kind==possible1 || t.kind==possible2 || t.kind==possible3;
	}

	private boolean peekIdentifierToken(String identifierString) {
		if (!moreTokens()) return false;
		Token t = peekToken();
		return t.kind==TokenKind.IDENTIFIER && t.stringValue().equalsIgnoreCase(identifierString);
	}
	
	private boolean peekSelectToken() {
		if (!moreTokens()) return false;
		Token t = peekToken();
		return t.kind==TokenKind.SELECT || t.kind==TokenKind.SELECT_FIRST || t.kind==TokenKind.SELECT_LAST;
	}
	
	
	private boolean moreTokens() {
		return tokenStreamPointer<tokenStream.size();
	}

	
	private Token nextToken() {
		if (tokenStreamPointer>=tokenStreamLength) {
			return null;
		}
		return tokenStream.get(tokenStreamPointer++);
	}

	private Token peekToken() {
		if (tokenStreamPointer>=tokenStreamLength) {
			return null;
		}
		return tokenStream.get(tokenStreamPointer);
	}

	private void raiseInternalException(int pos, SpelMessages message,Object... inserts) {
		throw new InternalParseException(new SpelParseException(expressionString,pos,message,inserts));		
	}

}
