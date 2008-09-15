// $ANTLR 3.0.1 /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g 2008-09-15 14:47:51
package org.springframework.expression.spel.generated;

import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.antlr.runtime.tree.*;

@SuppressWarnings( { "unused", "cast", "unchecked" })
public class SpringExpressionsParser extends Parser {
	public static final String[] tokenNames = new String[] { "<invalid>", "<EOR>", "<DOWN>", "<UP>", "EXPRESSIONLIST",
			"INTEGER_LITERAL", "EXPRESSION", "QUALIFIED_IDENTIFIER", "REFERENCE", "PROPERTY_OR_FIELD", "INDEXER",
			"ARGLIST", "CONSTRUCTOR", "DATE_LITERAL", "HOLDER", "CONSTRUCTOR_ARRAY", "NAMED_ARGUMENT", "FUNCTIONREF",
			"TYPEREF", "RANGE", "VARIABLEREF", "LIST_INITIALIZER", "MAP_INITIALIZER", "LOCALVAR", "LOCALFUNC",
			"MAP_ENTRY", "METHOD", "ADD", "SUBTRACT", "NUMBER", "SEMIRPAREN", "ASSIGN", "DEFAULT", "QMARK", "COLON",
			"LPAREN", "RPAREN", "OR", "AND", "PLUS", "MINUS", "STAR", "DIV", "MOD", "POWER", "BANG", "DOT", "POUND",
			"ID", "DOLLAR", "COMMA", "AT", "LBRACKET", "RBRACKET", "PROJECT", "RCURLY", "SELECT", "SELECT_FIRST",
			"SELECT_LAST", "TYPE", "LAMBDA", "PIPE", "LCURLY", "STRING_LITERAL", "DQ_STRING_LITERAL", "NULL_LITERAL",
			"HEXADECIMAL_INTEGER_LITERAL", "REAL_LITERAL", "TRUE", "FALSE", "DECIMAL_DIGIT", "INTEGER_TYPE_SUFFIX",
			"HEX_DIGIT", "EQUAL", "NOT_EQUAL", "LESS_THAN", "LESS_THAN_OR_EQUAL", "GREATER_THAN",
			"GREATER_THAN_OR_EQUAL", "IN", "IS", "BETWEEN", "MATCHES", "SEMI", "APOS", "DOT_ESCAPED", "WS", "UPTO",
			"EXPONENT_PART", "REAL_TYPE_SUFFIX", "SIGN", "'new'", "'date'" };
	public static final int GREATER_THAN_OR_EQUAL = 78;
	public static final int SELECT_FIRST = 57;
	public static final int COMMA = 50;
	public static final int HOLDER = 14;
	public static final int GREATER_THAN = 77;
	public static final int TYPE = 59;
	public static final int EXPRESSIONLIST = 4;
	public static final int MINUS = 40;
	public static final int MAP_ENTRY = 25;
	public static final int SELECT_LAST = 58;
	public static final int NUMBER = 29;
	public static final int LESS_THAN = 75;
	public static final int BANG = 45;
	public static final int ARGLIST = 11;
	public static final int FALSE = 69;
	public static final int METHOD = 26;
	public static final int PROPERTY_OR_FIELD = 9;
	public static final int LBRACKET = 52;
	public static final int INDEXER = 10;
	public static final int MOD = 43;
	public static final int CONSTRUCTOR_ARRAY = 15;
	public static final int FUNCTIONREF = 17;
	public static final int NULL_LITERAL = 65;
	public static final int NAMED_ARGUMENT = 16;
	public static final int OR = 37;
	public static final int PIPE = 61;
	public static final int DOT = 46;
	public static final int RCURLY = 55;
	public static final int EXPRESSION = 6;
	public static final int AND = 38;
	public static final int LCURLY = 62;
	public static final int DATE_LITERAL = 13;
	public static final int REAL_TYPE_SUFFIX = 89;
	public static final int STRING_LITERAL = 63;
	public static final int SELECT = 56;
	public static final int QUALIFIED_IDENTIFIER = 7;
	public static final int RBRACKET = 53;
	public static final int SUBTRACT = 28;
	public static final int ASSIGN = 31;
	public static final int BETWEEN = 81;
	public static final int RPAREN = 36;
	public static final int SIGN = 90;
	public static final int LPAREN = 35;
	public static final int HEX_DIGIT = 72;
	public static final int PLUS = 39;
	public static final int LIST_INITIALIZER = 21;
	public static final int APOS = 84;
	public static final int INTEGER_LITERAL = 5;
	public static final int AT = 51;
	public static final int ID = 48;
	public static final int NOT_EQUAL = 74;
	public static final int RANGE = 19;
	public static final int POWER = 44;
	public static final int TYPEREF = 18;
	public static final int DECIMAL_DIGIT = 70;
	public static final int WS = 86;
	public static final int IS = 80;
	public static final int DOLLAR = 49;
	public static final int LESS_THAN_OR_EQUAL = 76;
	public static final int SEMIRPAREN = 30;
	public static final int DQ_STRING_LITERAL = 64;
	public static final int HEXADECIMAL_INTEGER_LITERAL = 66;
	public static final int MAP_INITIALIZER = 22;
	public static final int LAMBDA = 60;
	public static final int LOCALFUNC = 24;
	public static final int IN = 79;
	public static final int SEMI = 83;
	public static final int CONSTRUCTOR = 12;
	public static final int INTEGER_TYPE_SUFFIX = 71;
	public static final int EQUAL = 73;
	public static final int MATCHES = 82;
	public static final int DOT_ESCAPED = 85;
	public static final int UPTO = 87;
	public static final int EOF = -1;
	public static final int QMARK = 33;
	public static final int REFERENCE = 8;
	public static final int PROJECT = 54;
	public static final int DEFAULT = 32;
	public static final int COLON = 34;
	public static final int DIV = 42;
	public static final int LOCALVAR = 23;
	public static final int STAR = 41;
	public static final int REAL_LITERAL = 67;
	public static final int VARIABLEREF = 20;
	public static final int EXPONENT_PART = 88;
	public static final int TRUE = 68;
	public static final int ADD = 27;
	public static final int POUND = 47;

	public SpringExpressionsParser(TokenStream input) {
		super(input);
		ruleMemo = new HashMap[53 + 1];
	}

	protected TreeAdaptor adaptor = new CommonTreeAdaptor();

	public void setTreeAdaptor(TreeAdaptor adaptor) {
		this.adaptor = adaptor;
	}

	public TreeAdaptor getTreeAdaptor() {
		return adaptor;
	}

	@Override
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override
	public String getGrammarFileName() {
		return "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g";
	}

	// For collecting info whilst processing rules that can be used in messages
	protected Stack<String> paraphrase = new Stack<String>();

	public static class expr_return extends ParserRuleReturnScope {
		Object tree;

		@Override
		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start expr
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:61:1: expr : expression EOF ;
	public final expr_return expr() throws RecognitionException {
		expr_return retval = new expr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token EOF2 = null;
		expression_return expression1 = null;

		Object EOF2_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:61:5: ( expression EOF )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:61:7: expression EOF
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_expression_in_expr181);
				expression1 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, expression1.getTree());
				EOF2 = (Token) input.LT(1);
				match(input, EOF, FOLLOW_EOF_in_expr183);
				if (failed)
					return retval;

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end expr

	public static class expression_return extends ParserRuleReturnScope {
		Object tree;

		@Override
		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start expression
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:65:1: expression : logicalOrExpression ( ( ASSIGN
	// logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )? ;
	public final expression_return expression() throws RecognitionException {
		expression_return retval = new expression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token ASSIGN4 = null;
		Token DEFAULT6 = null;
		Token QMARK8 = null;
		Token COLON10 = null;
		logicalOrExpression_return logicalOrExpression3 = null;

		logicalOrExpression_return logicalOrExpression5 = null;

		logicalOrExpression_return logicalOrExpression7 = null;

		expression_return expression9 = null;

		expression_return expression11 = null;

		Object ASSIGN4_tree = null;
		Object DEFAULT6_tree = null;
		Object QMARK8_tree = null;
		Object COLON10_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:65:12: ( logicalOrExpression ( ( ASSIGN
			// logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )? )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:66:5: logicalOrExpression ( ( ASSIGN logicalOrExpression )
			// | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )?
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_logicalOrExpression_in_expression212);
				logicalOrExpression3 = logicalOrExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, logicalOrExpression3.getTree());
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:67:5: ( ( ASSIGN logicalOrExpression )
				// | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )?
				int alt1 = 4;
				switch (input.LA(1)) {
				case ASSIGN: {
					alt1 = 1;
				}
					break;
				case DEFAULT: {
					alt1 = 2;
				}
					break;
				case QMARK: {
					alt1 = 3;
				}
					break;
				}

				switch (alt1) {
				case 1:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:67:7: ( ASSIGN logicalOrExpression
					// )
				{
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:67:7: ( ASSIGN logicalOrExpression
					// )
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:67:8: ASSIGN logicalOrExpression
					{
						ASSIGN4 = (Token) input.LT(1);
						match(input, ASSIGN, FOLLOW_ASSIGN_in_expression221);
						if (failed)
							return retval;
						if (backtracking == 0) {
							ASSIGN4_tree = (Object) adaptor.create(ASSIGN4);
							root_0 = (Object) adaptor.becomeRoot(ASSIGN4_tree, root_0);
						}
						pushFollow(FOLLOW_logicalOrExpression_in_expression224);
						logicalOrExpression5 = logicalOrExpression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, logicalOrExpression5.getTree());

					}

				}
					break;
				case 2:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:68:6: ( DEFAULT logicalOrExpression
					// )
				{
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:68:6: ( DEFAULT logicalOrExpression
					// )
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:68:7: DEFAULT logicalOrExpression
					{
						DEFAULT6 = (Token) input.LT(1);
						match(input, DEFAULT, FOLLOW_DEFAULT_in_expression234);
						if (failed)
							return retval;
						if (backtracking == 0) {
							DEFAULT6_tree = (Object) adaptor.create(DEFAULT6);
							root_0 = (Object) adaptor.becomeRoot(DEFAULT6_tree, root_0);
						}
						pushFollow(FOLLOW_logicalOrExpression_in_expression237);
						logicalOrExpression7 = logicalOrExpression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, logicalOrExpression7.getTree());

					}

				}
					break;
				case 3:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:69:6: ( QMARK expression COLON
					// expression )
				{
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:69:6: ( QMARK expression COLON
					// expression )
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:69:7: QMARK expression COLON
					// expression
					{
						QMARK8 = (Token) input.LT(1);
						match(input, QMARK, FOLLOW_QMARK_in_expression247);
						if (failed)
							return retval;
						if (backtracking == 0) {
							QMARK8_tree = (Object) adaptor.create(QMARK8);
							root_0 = (Object) adaptor.becomeRoot(QMARK8_tree, root_0);
						}
						pushFollow(FOLLOW_expression_in_expression250);
						expression9 = expression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, expression9.getTree());
						COLON10 = (Token) input.LT(1);
						match(input, COLON, FOLLOW_COLON_in_expression252);
						if (failed)
							return retval;
						pushFollow(FOLLOW_expression_in_expression255);
						expression11 = expression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, expression11.getTree());

					}

				}
					break;

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end expression

	public static class parenExpr_return extends ParserRuleReturnScope {
		Object tree;

		@Override
		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start parenExpr
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:71:1: parenExpr : LPAREN expression RPAREN ;
	public final parenExpr_return parenExpr() throws RecognitionException {
		parenExpr_return retval = new parenExpr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LPAREN12 = null;
		Token RPAREN14 = null;
		expression_return expression13 = null;

		Object LPAREN12_tree = null;
		Object RPAREN14_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:71:11: ( LPAREN expression RPAREN )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:71:13: LPAREN expression RPAREN
			{
				root_0 = (Object) adaptor.nil();

				LPAREN12 = (Token) input.LT(1);
				match(input, LPAREN, FOLLOW_LPAREN_in_parenExpr266);
				if (failed)
					return retval;
				pushFollow(FOLLOW_expression_in_parenExpr269);
				expression13 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, expression13.getTree());
				RPAREN14 = (Token) input.LT(1);
				match(input, RPAREN, FOLLOW_RPAREN_in_parenExpr271);
				if (failed)
					return retval;

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end parenExpr

	public static class logicalOrExpression_return extends ParserRuleReturnScope {
		Object tree;

		@Override
		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start logicalOrExpression
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:74:1: logicalOrExpression : logicalAndExpression ( OR
	// logicalAndExpression )* ;
	public final logicalOrExpression_return logicalOrExpression() throws RecognitionException {
		logicalOrExpression_return retval = new logicalOrExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token OR16 = null;
		logicalAndExpression_return logicalAndExpression15 = null;

		logicalAndExpression_return logicalAndExpression17 = null;

		Object OR16_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:76:1: ( logicalAndExpression ( OR logicalAndExpression )*
			// )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:76:3: logicalAndExpression ( OR logicalAndExpression )*
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_logicalAndExpression_in_logicalOrExpression284);
				logicalAndExpression15 = logicalAndExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, logicalAndExpression15.getTree());
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:76:24: ( OR logicalAndExpression )*
				loop2: do {
					int alt2 = 2;
					int LA2_0 = input.LA(1);

					if ((LA2_0 == OR)) {
						alt2 = 1;
					}

					switch (alt2) {
					case 1:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:76:25: OR logicalAndExpression
					{
						OR16 = (Token) input.LT(1);
						match(input, OR, FOLLOW_OR_in_logicalOrExpression287);
						if (failed)
							return retval;
						if (backtracking == 0) {
							OR16_tree = (Object) adaptor.create(OR16);
							root_0 = (Object) adaptor.becomeRoot(OR16_tree, root_0);
						}
						pushFollow(FOLLOW_logicalAndExpression_in_logicalOrExpression290);
						logicalAndExpression17 = logicalAndExpression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, logicalAndExpression17.getTree());

					}
						break;

					default:
						break loop2;
					}
				} while (true);

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end logicalOrExpression

	public static class logicalAndExpression_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start logicalAndExpression
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:78:1: logicalAndExpression : relationalExpression ( AND
	// relationalExpression )* ;
	public final logicalAndExpression_return logicalAndExpression() throws RecognitionException {
		logicalAndExpression_return retval = new logicalAndExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token AND19 = null;
		relationalExpression_return relationalExpression18 = null;

		relationalExpression_return relationalExpression20 = null;

		Object AND19_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:79:1: ( relationalExpression ( AND relationalExpression )*
			// )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:79:3: relationalExpression ( AND relationalExpression )*
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_relationalExpression_in_logicalAndExpression325);
				relationalExpression18 = relationalExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, relationalExpression18.getTree());
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:79:24: ( AND relationalExpression )*
				loop3: do {
					int alt3 = 2;
					int LA3_0 = input.LA(1);

					if ((LA3_0 == AND)) {
						alt3 = 1;
					}

					switch (alt3) {
					case 1:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:79:25: AND relationalExpression
					{
						AND19 = (Token) input.LT(1);
						match(input, AND, FOLLOW_AND_in_logicalAndExpression328);
						if (failed)
							return retval;
						if (backtracking == 0) {
							AND19_tree = (Object) adaptor.create(AND19);
							root_0 = (Object) adaptor.becomeRoot(AND19_tree, root_0);
						}
						pushFollow(FOLLOW_relationalExpression_in_logicalAndExpression331);
						relationalExpression20 = relationalExpression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, relationalExpression20.getTree());

					}
						break;

					default:
						break loop3;
					}
				} while (true);

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end logicalAndExpression

	public static class relationalExpression_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start relationalExpression
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:81:1: relationalExpression : sumExpression ( relationalOperator
	// sumExpression )? ;
	public final relationalExpression_return relationalExpression() throws RecognitionException {
		relationalExpression_return retval = new relationalExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		sumExpression_return sumExpression21 = null;

		relationalOperator_return relationalOperator22 = null;

		sumExpression_return sumExpression23 = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:81:22: ( sumExpression ( relationalOperator sumExpression
			// )? )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:81:24: sumExpression ( relationalOperator sumExpression )?
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_sumExpression_in_relationalExpression342);
				sumExpression21 = sumExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, sumExpression21.getTree());
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:81:38: ( relationalOperator
				// sumExpression )?
				int alt4 = 2;
				int LA4_0 = input.LA(1);

				if (((LA4_0 >= EQUAL && LA4_0 <= MATCHES))) {
					alt4 = 1;
				}
				switch (alt4) {
				case 1:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:81:39: relationalOperator
					// sumExpression
				{
					pushFollow(FOLLOW_relationalOperator_in_relationalExpression345);
					relationalOperator22 = relationalOperator();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						root_0 = (Object) adaptor.becomeRoot(relationalOperator22.getTree(), root_0);
					pushFollow(FOLLOW_sumExpression_in_relationalExpression348);
					sumExpression23 = sumExpression();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						adaptor.addChild(root_0, sumExpression23.getTree());

				}
					break;

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end relationalExpression

	public static class sumExpression_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start sumExpression
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:83:1: sumExpression : productExpression ( ( PLUS | MINUS )
	// productExpression )* ;
	public final sumExpression_return sumExpression() throws RecognitionException {
		sumExpression_return retval = new sumExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token PLUS25 = null;
		Token MINUS26 = null;
		productExpression_return productExpression24 = null;

		productExpression_return productExpression27 = null;

		Object PLUS25_tree = null;
		Object MINUS26_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:84:2: ( productExpression ( ( PLUS | MINUS )
			// productExpression )* )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:84:4: productExpression ( ( PLUS | MINUS )
			// productExpression )*
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_productExpression_in_sumExpression359);
				productExpression24 = productExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, productExpression24.getTree());
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:84:22: ( ( PLUS | MINUS )
				// productExpression )*
				loop6: do {
					int alt6 = 2;
					int LA6_0 = input.LA(1);

					if (((LA6_0 >= PLUS && LA6_0 <= MINUS))) {
						alt6 = 1;
					}

					switch (alt6) {
					case 1:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:84:24: ( PLUS | MINUS )
						// productExpression
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:84:24: ( PLUS | MINUS )
						int alt5 = 2;
						int LA5_0 = input.LA(1);

						if ((LA5_0 == PLUS)) {
							alt5 = 1;
						} else if ((LA5_0 == MINUS)) {
							alt5 = 2;
						} else {
							if (backtracking > 0) {
								failed = true;
								return retval;
							}
							NoViableAltException nvae = new NoViableAltException("84:24: ( PLUS | MINUS )", 5, 0, input);

							throw nvae;
						}
						switch (alt5) {
						case 1:
							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:84:25: PLUS
						{
							PLUS25 = (Token) input.LT(1);
							match(input, PLUS, FOLLOW_PLUS_in_sumExpression364);
							if (failed)
								return retval;
							if (backtracking == 0) {
								PLUS25_tree = (Object) adaptor.create(PLUS25);
								root_0 = (Object) adaptor.becomeRoot(PLUS25_tree, root_0);
							}

						}
							break;
						case 2:
							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:84:33: MINUS
						{
							MINUS26 = (Token) input.LT(1);
							match(input, MINUS, FOLLOW_MINUS_in_sumExpression369);
							if (failed)
								return retval;
							if (backtracking == 0) {
								MINUS26_tree = (Object) adaptor.create(MINUS26);
								root_0 = (Object) adaptor.becomeRoot(MINUS26_tree, root_0);
							}

						}
							break;

						}

						pushFollow(FOLLOW_productExpression_in_sumExpression373);
						productExpression27 = productExpression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, productExpression27.getTree());

					}
						break;

					default:
						break loop6;
					}
				} while (true);

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end sumExpression

	public static class productExpression_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start productExpression
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:87:1: productExpression : powerExpr ( ( STAR | DIV | MOD )
	// powerExpr )* ;
	public final productExpression_return productExpression() throws RecognitionException {
		productExpression_return retval = new productExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token STAR29 = null;
		Token DIV30 = null;
		Token MOD31 = null;
		powerExpr_return powerExpr28 = null;

		powerExpr_return powerExpr32 = null;

		Object STAR29_tree = null;
		Object DIV30_tree = null;
		Object MOD31_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:88:2: ( powerExpr ( ( STAR | DIV | MOD ) powerExpr )* )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:88:4: powerExpr ( ( STAR | DIV | MOD ) powerExpr )*
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_powerExpr_in_productExpression385);
				powerExpr28 = powerExpr();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, powerExpr28.getTree());
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:88:14: ( ( STAR | DIV | MOD ) powerExpr
				// )*
				loop8: do {
					int alt8 = 2;
					int LA8_0 = input.LA(1);

					if (((LA8_0 >= STAR && LA8_0 <= MOD))) {
						alt8 = 1;
					}

					switch (alt8) {
					case 1:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:88:15: ( STAR | DIV | MOD )
						// powerExpr
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:88:15: ( STAR | DIV | MOD )
						int alt7 = 3;
						switch (input.LA(1)) {
						case STAR: {
							alt7 = 1;
						}
							break;
						case DIV: {
							alt7 = 2;
						}
							break;
						case MOD: {
							alt7 = 3;
						}
							break;
						default:
							if (backtracking > 0) {
								failed = true;
								return retval;
							}
							NoViableAltException nvae = new NoViableAltException("88:15: ( STAR | DIV | MOD )", 7, 0,
									input);

							throw nvae;
						}

						switch (alt7) {
						case 1:
							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:88:16: STAR
						{
							STAR29 = (Token) input.LT(1);
							match(input, STAR, FOLLOW_STAR_in_productExpression389);
							if (failed)
								return retval;
							if (backtracking == 0) {
								STAR29_tree = (Object) adaptor.create(STAR29);
								root_0 = (Object) adaptor.becomeRoot(STAR29_tree, root_0);
							}

						}
							break;
						case 2:
							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:88:24: DIV
						{
							DIV30 = (Token) input.LT(1);
							match(input, DIV, FOLLOW_DIV_in_productExpression394);
							if (failed)
								return retval;
							if (backtracking == 0) {
								DIV30_tree = (Object) adaptor.create(DIV30);
								root_0 = (Object) adaptor.becomeRoot(DIV30_tree, root_0);
							}

						}
							break;
						case 3:
							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:88:30: MOD
						{
							MOD31 = (Token) input.LT(1);
							match(input, MOD, FOLLOW_MOD_in_productExpression398);
							if (failed)
								return retval;
							if (backtracking == 0) {
								MOD31_tree = (Object) adaptor.create(MOD31);
								root_0 = (Object) adaptor.becomeRoot(MOD31_tree, root_0);
							}

						}
							break;

						}

						pushFollow(FOLLOW_powerExpr_in_productExpression402);
						powerExpr32 = powerExpr();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, powerExpr32.getTree());

					}
						break;

					default:
						break loop8;
					}
				} while (true);

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end productExpression

	public static class powerExpr_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start powerExpr
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:90:1: powerExpr : unaryExpression ( POWER unaryExpression )? ;
	public final powerExpr_return powerExpr() throws RecognitionException {
		powerExpr_return retval = new powerExpr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token POWER34 = null;
		unaryExpression_return unaryExpression33 = null;

		unaryExpression_return unaryExpression35 = null;

		Object POWER34_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:90:12: ( unaryExpression ( POWER unaryExpression )? )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:90:14: unaryExpression ( POWER unaryExpression )?
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_unaryExpression_in_powerExpr414);
				unaryExpression33 = unaryExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, unaryExpression33.getTree());
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:90:30: ( POWER unaryExpression )?
				int alt9 = 2;
				int LA9_0 = input.LA(1);

				if ((LA9_0 == POWER)) {
					alt9 = 1;
				}
				switch (alt9) {
				case 1:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:90:31: POWER unaryExpression
				{
					POWER34 = (Token) input.LT(1);
					match(input, POWER, FOLLOW_POWER_in_powerExpr417);
					if (failed)
						return retval;
					if (backtracking == 0) {
						POWER34_tree = (Object) adaptor.create(POWER34);
						root_0 = (Object) adaptor.becomeRoot(POWER34_tree, root_0);
					}
					pushFollow(FOLLOW_unaryExpression_in_powerExpr420);
					unaryExpression35 = unaryExpression();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						adaptor.addChild(root_0, unaryExpression35.getTree());

				}
					break;

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end powerExpr

	public static class unaryExpression_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start unaryExpression
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:92:1: unaryExpression : ( ( PLUS | MINUS | BANG ) unaryExpression |
	// primaryExpression );
	public final unaryExpression_return unaryExpression() throws RecognitionException {
		unaryExpression_return retval = new unaryExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token PLUS36 = null;
		Token MINUS37 = null;
		Token BANG38 = null;
		unaryExpression_return unaryExpression39 = null;

		primaryExpression_return primaryExpression40 = null;

		Object PLUS36_tree = null;
		Object MINUS37_tree = null;
		Object BANG38_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:93:2: ( ( PLUS | MINUS | BANG ) unaryExpression |
			// primaryExpression )
			int alt11 = 2;
			int LA11_0 = input.LA(1);

			if (((LA11_0 >= PLUS && LA11_0 <= MINUS) || LA11_0 == BANG)) {
				alt11 = 1;
			} else if ((LA11_0 == INTEGER_LITERAL || LA11_0 == LPAREN || (LA11_0 >= POUND && LA11_0 <= DOLLAR)
					|| (LA11_0 >= AT && LA11_0 <= LBRACKET) || LA11_0 == PROJECT
					|| (LA11_0 >= SELECT && LA11_0 <= LAMBDA) || (LA11_0 >= LCURLY && LA11_0 <= FALSE) || (LA11_0 >= 91 && LA11_0 <= 92))) {
				alt11 = 2;
			} else {
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"92:1: unaryExpression : ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression );", 11,
						0, input);

				throw nvae;
			}
			switch (alt11) {
			case 1:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:93:4: ( PLUS | MINUS | BANG )
				// unaryExpression
			{
				root_0 = (Object) adaptor.nil();

				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:93:4: ( PLUS | MINUS | BANG )
				int alt10 = 3;
				switch (input.LA(1)) {
				case PLUS: {
					alt10 = 1;
				}
					break;
				case MINUS: {
					alt10 = 2;
				}
					break;
				case BANG: {
					alt10 = 3;
				}
					break;
				default:
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException("93:4: ( PLUS | MINUS | BANG )", 10, 0, input);

					throw nvae;
				}

				switch (alt10) {
				case 1:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:93:5: PLUS
				{
					PLUS36 = (Token) input.LT(1);
					match(input, PLUS, FOLLOW_PLUS_in_unaryExpression434);
					if (failed)
						return retval;
					if (backtracking == 0) {
						PLUS36_tree = (Object) adaptor.create(PLUS36);
						root_0 = (Object) adaptor.becomeRoot(PLUS36_tree, root_0);
					}

				}
					break;
				case 2:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:93:13: MINUS
				{
					MINUS37 = (Token) input.LT(1);
					match(input, MINUS, FOLLOW_MINUS_in_unaryExpression439);
					if (failed)
						return retval;
					if (backtracking == 0) {
						MINUS37_tree = (Object) adaptor.create(MINUS37);
						root_0 = (Object) adaptor.becomeRoot(MINUS37_tree, root_0);
					}

				}
					break;
				case 3:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:93:22: BANG
				{
					BANG38 = (Token) input.LT(1);
					match(input, BANG, FOLLOW_BANG_in_unaryExpression444);
					if (failed)
						return retval;
					if (backtracking == 0) {
						BANG38_tree = (Object) adaptor.create(BANG38);
						root_0 = (Object) adaptor.becomeRoot(BANG38_tree, root_0);
					}

				}
					break;

				}

				pushFollow(FOLLOW_unaryExpression_in_unaryExpression448);
				unaryExpression39 = unaryExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, unaryExpression39.getTree());

			}
				break;
			case 2:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:94:4: primaryExpression
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_primaryExpression_in_unaryExpression454);
				primaryExpression40 = primaryExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, primaryExpression40.getTree());

			}
				break;

			}
			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end unaryExpression

	public static class primaryExpression_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start primaryExpression
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:96:1: primaryExpression : startNode ( node )? -> ^( EXPRESSION
	// startNode ( node )? ) ;
	public final primaryExpression_return primaryExpression() throws RecognitionException {
		primaryExpression_return retval = new primaryExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		startNode_return startNode41 = null;

		node_return node42 = null;

		RewriteRuleSubtreeStream stream_node = new RewriteRuleSubtreeStream(adaptor, "rule node");
		RewriteRuleSubtreeStream stream_startNode = new RewriteRuleSubtreeStream(adaptor, "rule startNode");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:97:5: ( startNode ( node )? -> ^( EXPRESSION startNode (
			// node )? ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:97:7: startNode ( node )?
			{
				pushFollow(FOLLOW_startNode_in_primaryExpression468);
				startNode41 = startNode();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_startNode.add(startNode41.getTree());
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:97:17: ( node )?
				int alt12 = 2;
				int LA12_0 = input.LA(1);

				if ((LA12_0 == DOT || LA12_0 == LBRACKET)) {
					alt12 = 1;
				}
				switch (alt12) {
				case 1:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:97:18: node
				{
					pushFollow(FOLLOW_node_in_primaryExpression471);
					node42 = node();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_node.add(node42.getTree());

				}
					break;

				}

				// AST REWRITE
				// elements: node, startNode
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 97:25: -> ^( EXPRESSION startNode ( node )? )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:97:28: ^( EXPRESSION startNode
						// ( node )? )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(EXPRESSION, "EXPRESSION"), root_1);

							adaptor.addChild(root_1, stream_startNode.next());
							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:97:51: ( node )?
							if (stream_node.hasNext()) {
								adaptor.addChild(root_1, stream_node.next());

							}
							stream_node.reset();

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end primaryExpression

	public static class startNode_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start startNode
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:99:1: startNode : ( parenExpr | methodOrProperty | functionOrVar |
	// localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection
	// | lastSelection | listInitializer | mapInitializer | lambda );
	public final startNode_return startNode() throws RecognitionException {
		startNode_return retval = new startNode_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		parenExpr_return parenExpr43 = null;

		methodOrProperty_return methodOrProperty44 = null;

		functionOrVar_return functionOrVar45 = null;

		localFunctionOrVar_return localFunctionOrVar46 = null;

		reference_return reference47 = null;

		indexer_return indexer48 = null;

		literal_return literal49 = null;

		type_return type50 = null;

		constructor_return constructor51 = null;

		projection_return projection52 = null;

		selection_return selection53 = null;

		firstSelection_return firstSelection54 = null;

		lastSelection_return lastSelection55 = null;

		listInitializer_return listInitializer56 = null;

		mapInitializer_return mapInitializer57 = null;

		lambda_return lambda58 = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:100:5: ( parenExpr | methodOrProperty | functionOrVar |
			// localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection |
			// firstSelection | lastSelection | listInitializer | mapInitializer | lambda )
			int alt13 = 16;
			switch (input.LA(1)) {
			case LPAREN: {
				alt13 = 1;
			}
				break;
			case ID: {
				alt13 = 2;
			}
				break;
			case POUND: {
				int LA13_3 = input.LA(2);

				if ((LA13_3 == LCURLY)) {
					alt13 = 15;
				} else if ((LA13_3 == ID)) {
					alt13 = 3;
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException(
							"99:1: startNode : ( parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
							13, 3, input);

					throw nvae;
				}
			}
				break;
			case DOLLAR: {
				alt13 = 4;
			}
				break;
			case AT: {
				alt13 = 5;
			}
				break;
			case LBRACKET: {
				alt13 = 6;
			}
				break;
			case INTEGER_LITERAL:
			case STRING_LITERAL:
			case DQ_STRING_LITERAL:
			case NULL_LITERAL:
			case HEXADECIMAL_INTEGER_LITERAL:
			case REAL_LITERAL:
			case TRUE:
			case FALSE:
			case 92: {
				alt13 = 7;
			}
				break;
			case TYPE: {
				alt13 = 8;
			}
				break;
			case 91: {
				alt13 = 9;
			}
				break;
			case PROJECT: {
				alt13 = 10;
			}
				break;
			case SELECT: {
				alt13 = 11;
			}
				break;
			case SELECT_FIRST: {
				alt13 = 12;
			}
				break;
			case SELECT_LAST: {
				alt13 = 13;
			}
				break;
			case LCURLY: {
				alt13 = 14;
			}
				break;
			case LAMBDA: {
				alt13 = 16;
			}
				break;
			default:
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"99:1: startNode : ( parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
						13, 0, input);

				throw nvae;
			}

			switch (alt13) {
			case 1:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:101:5: parenExpr
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_parenExpr_in_startNode504);
				parenExpr43 = parenExpr();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, parenExpr43.getTree());

			}
				break;
			case 2:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:102:7: methodOrProperty
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_methodOrProperty_in_startNode512);
				methodOrProperty44 = methodOrProperty();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, methodOrProperty44.getTree());

			}
				break;
			case 3:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:103:7: functionOrVar
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_functionOrVar_in_startNode521);
				functionOrVar45 = functionOrVar();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, functionOrVar45.getTree());

			}
				break;
			case 4:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:104:7: localFunctionOrVar
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_localFunctionOrVar_in_startNode529);
				localFunctionOrVar46 = localFunctionOrVar();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, localFunctionOrVar46.getTree());

			}
				break;
			case 5:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:105:7: reference
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_reference_in_startNode537);
				reference47 = reference();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, reference47.getTree());

			}
				break;
			case 6:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:106:7: indexer
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_indexer_in_startNode545);
				indexer48 = indexer();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, indexer48.getTree());

			}
				break;
			case 7:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:107:7: literal
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_literal_in_startNode553);
				literal49 = literal();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, literal49.getTree());

			}
				break;
			case 8:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:108:7: type
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_type_in_startNode561);
				type50 = type();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, type50.getTree());

			}
				break;
			case 9:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:109:7: constructor
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_constructor_in_startNode569);
				constructor51 = constructor();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, constructor51.getTree());

			}
				break;
			case 10:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:110:7: projection
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_projection_in_startNode577);
				projection52 = projection();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, projection52.getTree());

			}
				break;
			case 11:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:111:7: selection
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_selection_in_startNode586);
				selection53 = selection();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, selection53.getTree());

			}
				break;
			case 12:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:112:7: firstSelection
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_firstSelection_in_startNode595);
				firstSelection54 = firstSelection();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, firstSelection54.getTree());

			}
				break;
			case 13:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:113:7: lastSelection
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_lastSelection_in_startNode603);
				lastSelection55 = lastSelection();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, lastSelection55.getTree());

			}
				break;
			case 14:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:114:7: listInitializer
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_listInitializer_in_startNode611);
				listInitializer56 = listInitializer();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, listInitializer56.getTree());

			}
				break;
			case 15:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:115:7: mapInitializer
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_mapInitializer_in_startNode619);
				mapInitializer57 = mapInitializer();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, mapInitializer57.getTree());

			}
				break;
			case 16:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:116:7: lambda
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_lambda_in_startNode627);
				lambda58 = lambda();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, lambda58.getTree());

			}
				break;

			}
			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end startNode

	public static class node_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start node
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:119:1: node : ( ( DOT dottedNode ) | nonDottedNode )+ ;
	public final node_return node() throws RecognitionException {
		node_return retval = new node_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token DOT59 = null;
		dottedNode_return dottedNode60 = null;

		nonDottedNode_return nonDottedNode61 = null;

		Object DOT59_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:120:2: ( ( ( DOT dottedNode ) | nonDottedNode )+ )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:120:4: ( ( DOT dottedNode ) | nonDottedNode )+
			{
				root_0 = (Object) adaptor.nil();

				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:120:4: ( ( DOT dottedNode ) |
				// nonDottedNode )+
				int cnt14 = 0;
				loop14: do {
					int alt14 = 3;
					int LA14_0 = input.LA(1);

					if ((LA14_0 == DOT)) {
						alt14 = 1;
					} else if ((LA14_0 == LBRACKET)) {
						alt14 = 2;
					}

					switch (alt14) {
					case 1:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:120:5: ( DOT dottedNode )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:120:5: ( DOT dottedNode )
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:120:6: DOT dottedNode
						{
							DOT59 = (Token) input.LT(1);
							match(input, DOT, FOLLOW_DOT_in_node647);
							if (failed)
								return retval;
							if (backtracking == 0) {
								DOT59_tree = (Object) adaptor.create(DOT59);
								adaptor.addChild(root_0, DOT59_tree);
							}
							pushFollow(FOLLOW_dottedNode_in_node649);
							dottedNode60 = dottedNode();
							_fsp--;
							if (failed)
								return retval;
							if (backtracking == 0)
								adaptor.addChild(root_0, dottedNode60.getTree());

						}

					}
						break;
					case 2:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:120:24: nonDottedNode
					{
						pushFollow(FOLLOW_nonDottedNode_in_node654);
						nonDottedNode61 = nonDottedNode();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, nonDottedNode61.getTree());

					}
						break;

					default:
						if (cnt14 >= 1)
							break loop14;
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						EarlyExitException eee = new EarlyExitException(14, input);
						throw eee;
					}
					cnt14++;
				} while (true);

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end node

	public static class nonDottedNode_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start nonDottedNode
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:122:1: nonDottedNode : indexer ;
	public final nonDottedNode_return nonDottedNode() throws RecognitionException {
		nonDottedNode_return retval = new nonDottedNode_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		indexer_return indexer62 = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:123:2: ( indexer )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:123:4: indexer
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_indexer_in_nonDottedNode666);
				indexer62 = indexer();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, indexer62.getTree());

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end nonDottedNode

	public static class dottedNode_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start dottedNode
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:125:1: dottedNode : ( ( methodOrProperty | functionOrVar |
	// projection | selection | firstSelection | lastSelection ) ) ;
	public final dottedNode_return dottedNode() throws RecognitionException {
		dottedNode_return retval = new dottedNode_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		methodOrProperty_return methodOrProperty63 = null;

		functionOrVar_return functionOrVar64 = null;

		projection_return projection65 = null;

		selection_return selection66 = null;

		firstSelection_return firstSelection67 = null;

		lastSelection_return lastSelection68 = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:126:2: ( ( ( methodOrProperty | functionOrVar | projection
			// | selection | firstSelection | lastSelection ) ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:127:2: ( ( methodOrProperty | functionOrVar | projection |
			// selection | firstSelection | lastSelection ) )
			{
				root_0 = (Object) adaptor.nil();

				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:127:2: ( ( methodOrProperty |
				// functionOrVar | projection | selection | firstSelection | lastSelection ) )
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:127:3: ( methodOrProperty |
				// functionOrVar | projection | selection | firstSelection | lastSelection )
				{
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:127:3: ( methodOrProperty |
					// functionOrVar | projection | selection | firstSelection | lastSelection )
					int alt15 = 6;
					switch (input.LA(1)) {
					case ID: {
						alt15 = 1;
					}
						break;
					case POUND: {
						alt15 = 2;
					}
						break;
					case PROJECT: {
						alt15 = 3;
					}
						break;
					case SELECT: {
						alt15 = 4;
					}
						break;
					case SELECT_FIRST: {
						alt15 = 5;
					}
						break;
					case SELECT_LAST: {
						alt15 = 6;
					}
						break;
					default:
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"127:3: ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection )",
								15, 0, input);

						throw nvae;
					}

					switch (alt15) {
					case 1:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:127:4: methodOrProperty
					{
						pushFollow(FOLLOW_methodOrProperty_in_dottedNode679);
						methodOrProperty63 = methodOrProperty();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, methodOrProperty63.getTree());

					}
						break;
					case 2:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:128:4: functionOrVar
					{
						pushFollow(FOLLOW_functionOrVar_in_dottedNode685);
						functionOrVar64 = functionOrVar();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, functionOrVar64.getTree());

					}
						break;
					case 3:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:129:7: projection
					{
						pushFollow(FOLLOW_projection_in_dottedNode693);
						projection65 = projection();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, projection65.getTree());

					}
						break;
					case 4:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:130:7: selection
					{
						pushFollow(FOLLOW_selection_in_dottedNode702);
						selection66 = selection();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, selection66.getTree());

					}
						break;
					case 5:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:131:7: firstSelection
					{
						pushFollow(FOLLOW_firstSelection_in_dottedNode711);
						firstSelection67 = firstSelection();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, firstSelection67.getTree());

					}
						break;
					case 6:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:132:7: lastSelection
					{
						pushFollow(FOLLOW_lastSelection_in_dottedNode720);
						lastSelection68 = lastSelection();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, lastSelection68.getTree());

					}
						break;

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end dottedNode

	public static class functionOrVar_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start functionOrVar
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:136:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );
	public final functionOrVar_return functionOrVar() throws RecognitionException {
		functionOrVar_return retval = new functionOrVar_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		function_return function69 = null;

		var_return var70 = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:137:5: ( ( POUND ID LPAREN )=> function | var )
			int alt16 = 2;
			int LA16_0 = input.LA(1);

			if ((LA16_0 == POUND)) {
				int LA16_1 = input.LA(2);

				if ((LA16_1 == ID)) {
					int LA16_2 = input.LA(3);

					if ((synpred1())) {
						alt16 = 1;
					} else if ((true)) {
						alt16 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"136:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 16, 2, input);

						throw nvae;
					}
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException(
							"136:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 16, 1, input);

					throw nvae;
				}
			} else {
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"136:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 16, 0, input);

				throw nvae;
			}
			switch (alt16) {
			case 1:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:137:7: ( POUND ID LPAREN )=> function
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_function_in_functionOrVar754);
				function69 = function();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, function69.getTree());

			}
				break;
			case 2:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:138:7: var
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_var_in_functionOrVar762);
				var70 = var();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, var70.getTree());

			}
				break;

			}
			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end functionOrVar

	public static class function_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start function
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:141:1: function : POUND id= ID methodArgs -> ^( FUNCTIONREF[$id]
	// methodArgs ) ;
	public final function_return function() throws RecognitionException {
		function_return retval = new function_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token id = null;
		Token POUND71 = null;
		methodArgs_return methodArgs72 = null;

		Object id_tree = null;
		Object POUND71_tree = null;
		RewriteRuleTokenStream stream_POUND = new RewriteRuleTokenStream(adaptor, "token POUND");
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");
		RewriteRuleSubtreeStream stream_methodArgs = new RewriteRuleSubtreeStream(adaptor, "rule methodArgs");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:141:10: ( POUND id= ID methodArgs -> ^( FUNCTIONREF[$id]
			// methodArgs ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:141:12: POUND id= ID methodArgs
			{
				POUND71 = (Token) input.LT(1);
				match(input, POUND, FOLLOW_POUND_in_function779);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_POUND.add(POUND71);

				id = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_function783);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(id);

				pushFollow(FOLLOW_methodArgs_in_function785);
				methodArgs72 = methodArgs();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_methodArgs.add(methodArgs72.getTree());

				// AST REWRITE
				// elements: methodArgs
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 141:35: -> ^( FUNCTIONREF[$id] methodArgs )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:141:38: ^( FUNCTIONREF[$id]
						// methodArgs )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(FUNCTIONREF, id), root_1);

							adaptor.addChild(root_1, stream_methodArgs.next());

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end function

	public static class var_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start var
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:143:1: var : POUND id= ID -> ^( VARIABLEREF[$id] ) ;
	public final var_return var() throws RecognitionException {
		var_return retval = new var_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token id = null;
		Token POUND73 = null;

		Object id_tree = null;
		Object POUND73_tree = null;
		RewriteRuleTokenStream stream_POUND = new RewriteRuleTokenStream(adaptor, "token POUND");
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:143:5: ( POUND id= ID -> ^( VARIABLEREF[$id] ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:143:7: POUND id= ID
			{
				POUND73 = (Token) input.LT(1);
				match(input, POUND, FOLLOW_POUND_in_var806);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_POUND.add(POUND73);

				id = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_var810);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(id);

				// AST REWRITE
				// elements:
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 143:19: -> ^( VARIABLEREF[$id] )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:143:22: ^( VARIABLEREF[$id] )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(VARIABLEREF, id), root_1);

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end var

	public static class localFunctionOrVar_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start localFunctionOrVar
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:145:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction
	// | localVar );
	public final localFunctionOrVar_return localFunctionOrVar() throws RecognitionException {
		localFunctionOrVar_return retval = new localFunctionOrVar_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		localFunction_return localFunction74 = null;

		localVar_return localVar75 = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:146:2: ( ( DOLLAR ID LPAREN )=> localFunction | localVar )
			int alt17 = 2;
			int LA17_0 = input.LA(1);

			if ((LA17_0 == DOLLAR)) {
				int LA17_1 = input.LA(2);

				if ((LA17_1 == ID)) {
					int LA17_2 = input.LA(3);

					if ((synpred2())) {
						alt17 = 1;
					} else if ((true)) {
						alt17 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"145:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );", 17,
								2, input);

						throw nvae;
					}
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException(
							"145:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );", 17, 1,
							input);

					throw nvae;
				}
			} else {
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"145:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );", 17, 0,
						input);

				throw nvae;
			}
			switch (alt17) {
			case 1:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:146:4: ( DOLLAR ID LPAREN )=>
				// localFunction
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_localFunction_in_localFunctionOrVar837);
				localFunction74 = localFunction();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, localFunction74.getTree());

			}
				break;
			case 2:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:147:4: localVar
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_localVar_in_localFunctionOrVar842);
				localVar75 = localVar();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, localVar75.getTree());

			}
				break;

			}
			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end localFunctionOrVar

	public static class localFunction_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start localFunction
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:150:1: localFunction : DOLLAR id= ID methodArgs -> ^(
	// LOCALFUNC[$id] methodArgs ) ;
	public final localFunction_return localFunction() throws RecognitionException {
		localFunction_return retval = new localFunction_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token id = null;
		Token DOLLAR76 = null;
		methodArgs_return methodArgs77 = null;

		Object id_tree = null;
		Object DOLLAR76_tree = null;
		RewriteRuleTokenStream stream_DOLLAR = new RewriteRuleTokenStream(adaptor, "token DOLLAR");
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");
		RewriteRuleSubtreeStream stream_methodArgs = new RewriteRuleSubtreeStream(adaptor, "rule methodArgs");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:150:15: ( DOLLAR id= ID methodArgs -> ^( LOCALFUNC[$id]
			// methodArgs ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:150:17: DOLLAR id= ID methodArgs
			{
				DOLLAR76 = (Token) input.LT(1);
				match(input, DOLLAR, FOLLOW_DOLLAR_in_localFunction852);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_DOLLAR.add(DOLLAR76);

				id = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_localFunction856);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(id);

				pushFollow(FOLLOW_methodArgs_in_localFunction858);
				methodArgs77 = methodArgs();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_methodArgs.add(methodArgs77.getTree());

				// AST REWRITE
				// elements: methodArgs
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 150:41: -> ^( LOCALFUNC[$id] methodArgs )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:150:44: ^( LOCALFUNC[$id]
						// methodArgs )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(LOCALFUNC, id), root_1);

							adaptor.addChild(root_1, stream_methodArgs.next());

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end localFunction

	public static class localVar_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start localVar
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:151:1: localVar : DOLLAR id= ID -> ^( LOCALVAR[$id] ) ;
	public final localVar_return localVar() throws RecognitionException {
		localVar_return retval = new localVar_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token id = null;
		Token DOLLAR78 = null;

		Object id_tree = null;
		Object DOLLAR78_tree = null;
		RewriteRuleTokenStream stream_DOLLAR = new RewriteRuleTokenStream(adaptor, "token DOLLAR");
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:151:9: ( DOLLAR id= ID -> ^( LOCALVAR[$id] ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:151:11: DOLLAR id= ID
			{
				DOLLAR78 = (Token) input.LT(1);
				match(input, DOLLAR, FOLLOW_DOLLAR_in_localVar873);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_DOLLAR.add(DOLLAR78);

				id = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_localVar877);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(id);

				// AST REWRITE
				// elements:
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 151:24: -> ^( LOCALVAR[$id] )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:151:27: ^( LOCALVAR[$id] )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(LOCALVAR, id), root_1);

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end localVar

	public static class methodOrProperty_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start methodOrProperty
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:153:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^(
	// METHOD[$id] methodArgs ) | property );
	public final methodOrProperty_return methodOrProperty() throws RecognitionException {
		methodOrProperty_return retval = new methodOrProperty_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token id = null;
		methodArgs_return methodArgs79 = null;

		property_return property80 = null;

		Object id_tree = null;
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");
		RewriteRuleSubtreeStream stream_methodArgs = new RewriteRuleSubtreeStream(adaptor, "rule methodArgs");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:154:2: ( ( ID LPAREN )=>id= ID methodArgs -> ^(
			// METHOD[$id] methodArgs ) | property )
			int alt18 = 2;
			int LA18_0 = input.LA(1);

			if ((LA18_0 == ID)) {
				int LA18_1 = input.LA(2);

				if ((LA18_1 == EOF || (LA18_1 >= ASSIGN && LA18_1 <= COLON) || (LA18_1 >= RPAREN && LA18_1 <= POWER)
						|| LA18_1 == DOT || LA18_1 == COMMA || (LA18_1 >= LBRACKET && LA18_1 <= RBRACKET)
						|| LA18_1 == RCURLY || (LA18_1 >= EQUAL && LA18_1 <= MATCHES))) {
					alt18 = 2;
				} else if ((LA18_1 == LPAREN) && (synpred3())) {
					alt18 = 1;
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException(
							"153:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );",
							18, 1, input);

					throw nvae;
				}
			} else {
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"153:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );",
						18, 0, input);

				throw nvae;
			}
			switch (alt18) {
			case 1:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:154:4: ( ID LPAREN )=>id= ID methodArgs
			{
				id = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_methodOrProperty903);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(id);

				pushFollow(FOLLOW_methodArgs_in_methodOrProperty905);
				methodArgs79 = methodArgs();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_methodArgs.add(methodArgs79.getTree());

				// AST REWRITE
				// elements: methodArgs
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 154:36: -> ^( METHOD[$id] methodArgs )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:154:39: ^( METHOD[$id]
						// methodArgs )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(METHOD, id), root_1);

							adaptor.addChild(root_1, stream_methodArgs.next());

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}
				break;
			case 2:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:155:4: property
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_property_in_methodOrProperty919);
				property80 = property();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, property80.getTree());

			}
				break;

			}
			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end methodOrProperty

	public static class methodArgs_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start methodArgs
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:161:1: methodArgs : LPAREN ( argument ( COMMA argument )* ( COMMA
	// )? )? RPAREN ;
	public final methodArgs_return methodArgs() throws RecognitionException {
		methodArgs_return retval = new methodArgs_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LPAREN81 = null;
		Token COMMA83 = null;
		Token COMMA85 = null;
		Token RPAREN86 = null;
		argument_return argument82 = null;

		argument_return argument84 = null;

		Object LPAREN81_tree = null;
		Object COMMA83_tree = null;
		Object COMMA85_tree = null;
		Object RPAREN86_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:161:12: ( LPAREN ( argument ( COMMA argument )* ( COMMA )?
			// )? RPAREN )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:161:15: LPAREN ( argument ( COMMA argument )* ( COMMA )?
			// )? RPAREN
			{
				root_0 = (Object) adaptor.nil();

				LPAREN81 = (Token) input.LT(1);
				match(input, LPAREN, FOLLOW_LPAREN_in_methodArgs934);
				if (failed)
					return retval;
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:161:23: ( argument ( COMMA argument )*
				// ( COMMA )? )?
				int alt21 = 2;
				int LA21_0 = input.LA(1);

				if ((LA21_0 == INTEGER_LITERAL || LA21_0 == LPAREN || (LA21_0 >= PLUS && LA21_0 <= MINUS)
						|| LA21_0 == BANG || (LA21_0 >= POUND && LA21_0 <= DOLLAR)
						|| (LA21_0 >= AT && LA21_0 <= LBRACKET) || LA21_0 == PROJECT
						|| (LA21_0 >= SELECT && LA21_0 <= LAMBDA) || (LA21_0 >= LCURLY && LA21_0 <= FALSE) || (LA21_0 >= 91 && LA21_0 <= 92))) {
					alt21 = 1;
				}
				switch (alt21) {
				case 1:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:161:24: argument ( COMMA argument
					// )* ( COMMA )?
				{
					pushFollow(FOLLOW_argument_in_methodArgs938);
					argument82 = argument();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						adaptor.addChild(root_0, argument82.getTree());
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:161:33: ( COMMA argument )*
					loop19: do {
						int alt19 = 2;
						int LA19_0 = input.LA(1);

						if ((LA19_0 == COMMA)) {
							int LA19_1 = input.LA(2);

							if ((LA19_1 == INTEGER_LITERAL || LA19_1 == LPAREN || (LA19_1 >= PLUS && LA19_1 <= MINUS)
									|| LA19_1 == BANG || (LA19_1 >= POUND && LA19_1 <= DOLLAR)
									|| (LA19_1 >= AT && LA19_1 <= LBRACKET) || LA19_1 == PROJECT
									|| (LA19_1 >= SELECT && LA19_1 <= LAMBDA) || (LA19_1 >= LCURLY && LA19_1 <= FALSE) || (LA19_1 >= 91 && LA19_1 <= 92))) {
								alt19 = 1;
							}

						}

						switch (alt19) {
						case 1:
							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:161:34: COMMA argument
						{
							COMMA83 = (Token) input.LT(1);
							match(input, COMMA, FOLLOW_COMMA_in_methodArgs941);
							if (failed)
								return retval;
							pushFollow(FOLLOW_argument_in_methodArgs944);
							argument84 = argument();
							_fsp--;
							if (failed)
								return retval;
							if (backtracking == 0)
								adaptor.addChild(root_0, argument84.getTree());

						}
							break;

						default:
							break loop19;
						}
					} while (true);

					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:161:52: ( COMMA )?
					int alt20 = 2;
					int LA20_0 = input.LA(1);

					if ((LA20_0 == COMMA)) {
						alt20 = 1;
					}
					switch (alt20) {
					case 1:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:161:53: COMMA
					{
						COMMA85 = (Token) input.LT(1);
						match(input, COMMA, FOLLOW_COMMA_in_methodArgs949);
						if (failed)
							return retval;

					}
						break;

					}

				}
					break;

				}

				RPAREN86 = (Token) input.LT(1);
				match(input, RPAREN, FOLLOW_RPAREN_in_methodArgs956);
				if (failed)
					return retval;

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end methodArgs

	public static class property_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start property
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:166:1: property : id= ID -> ^( PROPERTY_OR_FIELD[$id] ) ;
	public final property_return property() throws RecognitionException {
		property_return retval = new property_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token id = null;

		Object id_tree = null;
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:166:9: (id= ID -> ^( PROPERTY_OR_FIELD[$id] ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:166:11: id= ID
			{
				id = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_property969);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(id);

				// AST REWRITE
				// elements:
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 166:17: -> ^( PROPERTY_OR_FIELD[$id] )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:166:20: ^(
						// PROPERTY_OR_FIELD[$id] )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(PROPERTY_OR_FIELD, id), root_1);

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end property

	public static class reference_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start reference
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:173:1: reference : AT pos= LPAREN (cn= contextName COLON )? (q=
	// qualifiedId )? RPAREN -> ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN ) ;
	public final reference_return reference() throws RecognitionException {
		reference_return retval = new reference_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token pos = null;
		Token AT87 = null;
		Token COLON88 = null;
		Token RPAREN89 = null;
		contextName_return cn = null;

		qualifiedId_return q = null;

		Object pos_tree = null;
		Object AT87_tree = null;
		Object COLON88_tree = null;
		Object RPAREN89_tree = null;
		RewriteRuleTokenStream stream_RPAREN = new RewriteRuleTokenStream(adaptor, "token RPAREN");
		RewriteRuleTokenStream stream_COLON = new RewriteRuleTokenStream(adaptor, "token COLON");
		RewriteRuleTokenStream stream_LPAREN = new RewriteRuleTokenStream(adaptor, "token LPAREN");
		RewriteRuleTokenStream stream_AT = new RewriteRuleTokenStream(adaptor, "token AT");
		RewriteRuleSubtreeStream stream_contextName = new RewriteRuleSubtreeStream(adaptor, "rule contextName");
		RewriteRuleSubtreeStream stream_qualifiedId = new RewriteRuleSubtreeStream(adaptor, "rule qualifiedId");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:174:2: ( AT pos= LPAREN (cn= contextName COLON )? (q=
			// qualifiedId )? RPAREN -> ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:174:5: AT pos= LPAREN (cn= contextName COLON )? (q=
			// qualifiedId )? RPAREN
			{
				AT87 = (Token) input.LT(1);
				match(input, AT, FOLLOW_AT_in_reference991);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_AT.add(AT87);

				pos = (Token) input.LT(1);
				match(input, LPAREN, FOLLOW_LPAREN_in_reference995);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LPAREN.add(pos);

				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:174:19: (cn= contextName COLON )?
				int alt22 = 2;
				int LA22_0 = input.LA(1);

				if ((LA22_0 == ID)) {
					int LA22_1 = input.LA(2);

					if ((LA22_1 == COLON || LA22_1 == DIV)) {
						alt22 = 1;
					}
				}
				switch (alt22) {
				case 1:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:174:20: cn= contextName COLON
				{
					pushFollow(FOLLOW_contextName_in_reference1000);
					cn = contextName();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_contextName.add(cn.getTree());
					COLON88 = (Token) input.LT(1);
					match(input, COLON, FOLLOW_COLON_in_reference1002);
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_COLON.add(COLON88);

				}
					break;

				}

				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:174:43: (q= qualifiedId )?
				int alt23 = 2;
				int LA23_0 = input.LA(1);

				if ((LA23_0 == ID)) {
					alt23 = 1;
				}
				switch (alt23) {
				case 1:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:174:44: q= qualifiedId
				{
					pushFollow(FOLLOW_qualifiedId_in_reference1009);
					q = qualifiedId();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_qualifiedId.add(q.getTree());

				}
					break;

				}

				RPAREN89 = (Token) input.LT(1);
				match(input, RPAREN, FOLLOW_RPAREN_in_reference1013);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RPAREN.add(RPAREN89);

				// AST REWRITE
				// elements: cn, RPAREN, q, COLON
				// token labels:
				// rule labels: cn, retval, q
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_cn = new RewriteRuleSubtreeStream(adaptor, "token cn",
							cn != null ? cn.tree : null);
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);
					RewriteRuleSubtreeStream stream_q = new RewriteRuleSubtreeStream(adaptor, "token q",
							q != null ? q.tree : null);

					root_0 = (Object) adaptor.nil();
					// 175:4: -> ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:175:7: ^( REFERENCE[$pos] ( $cn
						// COLON )? ( $q)? RPAREN )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(REFERENCE, pos), root_1);

							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:175:25: ( $cn COLON )?
							if (stream_cn.hasNext() || stream_COLON.hasNext()) {
								adaptor.addChild(root_1, stream_cn.next());
								adaptor.addChild(root_1, stream_COLON.next());

							}
							stream_cn.reset();
							stream_COLON.reset();
							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:175:38: ( $q)?
							if (stream_q.hasNext()) {
								adaptor.addChild(root_1, stream_q.next());

							}
							stream_q.reset();
							adaptor.addChild(root_1, stream_RPAREN.next());

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end reference

	public static class indexer_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start indexer
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:181:1: indexer : LBRACKET r1= argument ( COMMA r2= argument )*
	// RBRACKET -> ^( INDEXER $r1 ( $r2)* ) ;
	public final indexer_return indexer() throws RecognitionException {
		indexer_return retval = new indexer_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LBRACKET90 = null;
		Token COMMA91 = null;
		Token RBRACKET92 = null;
		argument_return r1 = null;

		argument_return r2 = null;

		Object LBRACKET90_tree = null;
		Object COMMA91_tree = null;
		Object RBRACKET92_tree = null;
		RewriteRuleTokenStream stream_COMMA = new RewriteRuleTokenStream(adaptor, "token COMMA");
		RewriteRuleTokenStream stream_LBRACKET = new RewriteRuleTokenStream(adaptor, "token LBRACKET");
		RewriteRuleTokenStream stream_RBRACKET = new RewriteRuleTokenStream(adaptor, "token RBRACKET");
		RewriteRuleSubtreeStream stream_argument = new RewriteRuleSubtreeStream(adaptor, "rule argument");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:181:8: ( LBRACKET r1= argument ( COMMA r2= argument )*
			// RBRACKET -> ^( INDEXER $r1 ( $r2)* ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:181:10: LBRACKET r1= argument ( COMMA r2= argument )*
			// RBRACKET
			{
				LBRACKET90 = (Token) input.LT(1);
				match(input, LBRACKET, FOLLOW_LBRACKET_in_indexer1048);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LBRACKET.add(LBRACKET90);

				pushFollow(FOLLOW_argument_in_indexer1052);
				r1 = argument();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_argument.add(r1.getTree());
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:181:31: ( COMMA r2= argument )*
				loop24: do {
					int alt24 = 2;
					int LA24_0 = input.LA(1);

					if ((LA24_0 == COMMA)) {
						alt24 = 1;
					}

					switch (alt24) {
					case 1:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:181:32: COMMA r2= argument
					{
						COMMA91 = (Token) input.LT(1);
						match(input, COMMA, FOLLOW_COMMA_in_indexer1055);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_COMMA.add(COMMA91);

						pushFollow(FOLLOW_argument_in_indexer1059);
						r2 = argument();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_argument.add(r2.getTree());

					}
						break;

					default:
						break loop24;
					}
				} while (true);

				RBRACKET92 = (Token) input.LT(1);
				match(input, RBRACKET, FOLLOW_RBRACKET_in_indexer1063);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RBRACKET.add(RBRACKET92);

				// AST REWRITE
				// elements: r2, r1
				// token labels:
				// rule labels: r2, retval, r1
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_r2 = new RewriteRuleSubtreeStream(adaptor, "token r2",
							r2 != null ? r2.tree : null);
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);
					RewriteRuleSubtreeStream stream_r1 = new RewriteRuleSubtreeStream(adaptor, "token r1",
							r1 != null ? r1.tree : null);

					root_0 = (Object) adaptor.nil();
					// 181:61: -> ^( INDEXER $r1 ( $r2)* )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:181:64: ^( INDEXER $r1 ( $r2)*
						// )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(INDEXER, "INDEXER"), root_1);

							adaptor.addChild(root_1, stream_r1.next());
							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:181:78: ( $r2)*
							while (stream_r2.hasNext()) {
								adaptor.addChild(root_1, stream_r2.next());

							}
							stream_r2.reset();

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end indexer

	public static class projection_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start projection
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:186:1: projection : PROJECT expression RCURLY ;
	public final projection_return projection() throws RecognitionException {
		projection_return retval = new projection_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token PROJECT93 = null;
		Token RCURLY95 = null;
		expression_return expression94 = null;

		Object PROJECT93_tree = null;
		Object RCURLY95_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:186:11: ( PROJECT expression RCURLY )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:186:13: PROJECT expression RCURLY
			{
				root_0 = (Object) adaptor.nil();

				PROJECT93 = (Token) input.LT(1);
				match(input, PROJECT, FOLLOW_PROJECT_in_projection1090);
				if (failed)
					return retval;
				if (backtracking == 0) {
					PROJECT93_tree = (Object) adaptor.create(PROJECT93);
					root_0 = (Object) adaptor.becomeRoot(PROJECT93_tree, root_0);
				}
				pushFollow(FOLLOW_expression_in_projection1093);
				expression94 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, expression94.getTree());
				RCURLY95 = (Token) input.LT(1);
				match(input, RCURLY, FOLLOW_RCURLY_in_projection1095);
				if (failed)
					return retval;

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end projection

	public static class selection_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start selection
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:188:1: selection : SELECT expression RCURLY ;
	public final selection_return selection() throws RecognitionException {
		selection_return retval = new selection_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token SELECT96 = null;
		Token RCURLY98 = null;
		expression_return expression97 = null;

		Object SELECT96_tree = null;
		Object RCURLY98_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:188:10: ( SELECT expression RCURLY )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:188:12: SELECT expression RCURLY
			{
				root_0 = (Object) adaptor.nil();

				SELECT96 = (Token) input.LT(1);
				match(input, SELECT, FOLLOW_SELECT_in_selection1103);
				if (failed)
					return retval;
				if (backtracking == 0) {
					SELECT96_tree = (Object) adaptor.create(SELECT96);
					root_0 = (Object) adaptor.becomeRoot(SELECT96_tree, root_0);
				}
				pushFollow(FOLLOW_expression_in_selection1106);
				expression97 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, expression97.getTree());
				RCURLY98 = (Token) input.LT(1);
				match(input, RCURLY, FOLLOW_RCURLY_in_selection1108);
				if (failed)
					return retval;

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end selection

	public static class firstSelection_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start firstSelection
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:190:1: firstSelection : SELECT_FIRST expression RCURLY ;
	public final firstSelection_return firstSelection() throws RecognitionException {
		firstSelection_return retval = new firstSelection_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token SELECT_FIRST99 = null;
		Token RCURLY101 = null;
		expression_return expression100 = null;

		Object SELECT_FIRST99_tree = null;
		Object RCURLY101_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:190:15: ( SELECT_FIRST expression RCURLY )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:190:17: SELECT_FIRST expression RCURLY
			{
				root_0 = (Object) adaptor.nil();

				SELECT_FIRST99 = (Token) input.LT(1);
				match(input, SELECT_FIRST, FOLLOW_SELECT_FIRST_in_firstSelection1116);
				if (failed)
					return retval;
				if (backtracking == 0) {
					SELECT_FIRST99_tree = (Object) adaptor.create(SELECT_FIRST99);
					root_0 = (Object) adaptor.becomeRoot(SELECT_FIRST99_tree, root_0);
				}
				pushFollow(FOLLOW_expression_in_firstSelection1119);
				expression100 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, expression100.getTree());
				RCURLY101 = (Token) input.LT(1);
				match(input, RCURLY, FOLLOW_RCURLY_in_firstSelection1121);
				if (failed)
					return retval;

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end firstSelection

	public static class lastSelection_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start lastSelection
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:192:1: lastSelection : SELECT_LAST expression RCURLY ;
	public final lastSelection_return lastSelection() throws RecognitionException {
		lastSelection_return retval = new lastSelection_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token SELECT_LAST102 = null;
		Token RCURLY104 = null;
		expression_return expression103 = null;

		Object SELECT_LAST102_tree = null;
		Object RCURLY104_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:192:14: ( SELECT_LAST expression RCURLY )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:192:16: SELECT_LAST expression RCURLY
			{
				root_0 = (Object) adaptor.nil();

				SELECT_LAST102 = (Token) input.LT(1);
				match(input, SELECT_LAST, FOLLOW_SELECT_LAST_in_lastSelection1129);
				if (failed)
					return retval;
				if (backtracking == 0) {
					SELECT_LAST102_tree = (Object) adaptor.create(SELECT_LAST102);
					root_0 = (Object) adaptor.becomeRoot(SELECT_LAST102_tree, root_0);
				}
				pushFollow(FOLLOW_expression_in_lastSelection1132);
				expression103 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, expression103.getTree());
				RCURLY104 = (Token) input.LT(1);
				match(input, RCURLY, FOLLOW_RCURLY_in_lastSelection1134);
				if (failed)
					return retval;

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end lastSelection

	public static class type_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start type
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:195:1: type : TYPE qualifiedId RPAREN -> ^( TYPEREF qualifiedId ) ;
	public final type_return type() throws RecognitionException {
		type_return retval = new type_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token TYPE105 = null;
		Token RPAREN107 = null;
		qualifiedId_return qualifiedId106 = null;

		Object TYPE105_tree = null;
		Object RPAREN107_tree = null;
		RewriteRuleTokenStream stream_RPAREN = new RewriteRuleTokenStream(adaptor, "token RPAREN");
		RewriteRuleTokenStream stream_TYPE = new RewriteRuleTokenStream(adaptor, "token TYPE");
		RewriteRuleSubtreeStream stream_qualifiedId = new RewriteRuleSubtreeStream(adaptor, "rule qualifiedId");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:195:5: ( TYPE qualifiedId RPAREN -> ^( TYPEREF qualifiedId
			// ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:195:7: TYPE qualifiedId RPAREN
			{
				TYPE105 = (Token) input.LT(1);
				match(input, TYPE, FOLLOW_TYPE_in_type1143);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_TYPE.add(TYPE105);

				pushFollow(FOLLOW_qualifiedId_in_type1145);
				qualifiedId106 = qualifiedId();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_qualifiedId.add(qualifiedId106.getTree());
				RPAREN107 = (Token) input.LT(1);
				match(input, RPAREN, FOLLOW_RPAREN_in_type1147);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RPAREN.add(RPAREN107);

				// AST REWRITE
				// elements: qualifiedId
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 195:31: -> ^( TYPEREF qualifiedId )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:195:34: ^( TYPEREF qualifiedId
						// )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(TYPEREF, "TYPEREF"), root_1);

							adaptor.addChild(root_1, stream_qualifiedId.next());

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end type

	public static class lambda_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start lambda
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:203:1: lambda : LAMBDA ( argList )? PIPE expression RCURLY -> ^(
	// LAMBDA ( argList )? expression ) ;
	public final lambda_return lambda() throws RecognitionException {
		lambda_return retval = new lambda_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LAMBDA108 = null;
		Token PIPE110 = null;
		Token RCURLY112 = null;
		argList_return argList109 = null;

		expression_return expression111 = null;

		Object LAMBDA108_tree = null;
		Object PIPE110_tree = null;
		Object RCURLY112_tree = null;
		RewriteRuleTokenStream stream_RCURLY = new RewriteRuleTokenStream(adaptor, "token RCURLY");
		RewriteRuleTokenStream stream_PIPE = new RewriteRuleTokenStream(adaptor, "token PIPE");
		RewriteRuleTokenStream stream_LAMBDA = new RewriteRuleTokenStream(adaptor, "token LAMBDA");
		RewriteRuleSubtreeStream stream_argList = new RewriteRuleSubtreeStream(adaptor, "rule argList");
		RewriteRuleSubtreeStream stream_expression = new RewriteRuleSubtreeStream(adaptor, "rule expression");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:204:4: ( LAMBDA ( argList )? PIPE expression RCURLY -> ^(
			// LAMBDA ( argList )? expression ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:204:8: LAMBDA ( argList )? PIPE expression RCURLY
			{
				LAMBDA108 = (Token) input.LT(1);
				match(input, LAMBDA, FOLLOW_LAMBDA_in_lambda1174);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LAMBDA.add(LAMBDA108);

				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:204:15: ( argList )?
				int alt25 = 2;
				int LA25_0 = input.LA(1);

				if ((LA25_0 == ID)) {
					alt25 = 1;
				}
				switch (alt25) {
				case 1:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:204:16: argList
				{
					pushFollow(FOLLOW_argList_in_lambda1177);
					argList109 = argList();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_argList.add(argList109.getTree());

				}
					break;

				}

				PIPE110 = (Token) input.LT(1);
				match(input, PIPE, FOLLOW_PIPE_in_lambda1181);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_PIPE.add(PIPE110);

				pushFollow(FOLLOW_expression_in_lambda1183);
				expression111 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_expression.add(expression111.getTree());
				RCURLY112 = (Token) input.LT(1);
				match(input, RCURLY, FOLLOW_RCURLY_in_lambda1185);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RCURLY.add(RCURLY112);

				// AST REWRITE
				// elements: LAMBDA, expression, argList
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 204:49: -> ^( LAMBDA ( argList )? expression )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:204:52: ^( LAMBDA ( argList )?
						// expression )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(stream_LAMBDA.next(), root_1);

							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:204:61: ( argList )?
							if (stream_argList.hasNext()) {
								adaptor.addChild(root_1, stream_argList.next());

							}
							stream_argList.reset();
							adaptor.addChild(root_1, stream_expression.next());

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end lambda

	public static class argList_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start argList
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:206:1: argList : (id+= ID ( COMMA id+= ID )* ) -> ^( ARGLIST (
	// $id)* ) ;
	public final argList_return argList() throws RecognitionException {
		argList_return retval = new argList_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token COMMA113 = null;
		Token id = null;
		List list_id = null;

		Object COMMA113_tree = null;
		Object id_tree = null;
		RewriteRuleTokenStream stream_COMMA = new RewriteRuleTokenStream(adaptor, "token COMMA");
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:206:9: ( (id+= ID ( COMMA id+= ID )* ) -> ^( ARGLIST (
			// $id)* ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:206:11: (id+= ID ( COMMA id+= ID )* )
			{
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:206:11: (id+= ID ( COMMA id+= ID )* )
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:206:12: id+= ID ( COMMA id+= ID )*
				{
					id = (Token) input.LT(1);
					match(input, ID, FOLLOW_ID_in_argList1209);
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_ID.add(id);

					if (list_id == null)
						list_id = new ArrayList();
					list_id.add(id);

					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:206:19: ( COMMA id+= ID )*
					loop26: do {
						int alt26 = 2;
						int LA26_0 = input.LA(1);

						if ((LA26_0 == COMMA)) {
							alt26 = 1;
						}

						switch (alt26) {
						case 1:
							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:206:20: COMMA id+= ID
						{
							COMMA113 = (Token) input.LT(1);
							match(input, COMMA, FOLLOW_COMMA_in_argList1212);
							if (failed)
								return retval;
							if (backtracking == 0)
								stream_COMMA.add(COMMA113);

							id = (Token) input.LT(1);
							match(input, ID, FOLLOW_ID_in_argList1216);
							if (failed)
								return retval;
							if (backtracking == 0)
								stream_ID.add(id);

							if (list_id == null)
								list_id = new ArrayList();
							list_id.add(id);

						}
							break;

						default:
							break loop26;
						}
					} while (true);

				}

				// AST REWRITE
				// elements: id
				// token labels:
				// rule labels: retval
				// token list labels: id
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleTokenStream stream_id = new RewriteRuleTokenStream(adaptor, "token id", list_id);
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 206:36: -> ^( ARGLIST ( $id)* )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:206:39: ^( ARGLIST ( $id)* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(ARGLIST, "ARGLIST"), root_1);

							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:206:49: ( $id)*
							while (stream_id.hasNext()) {
								adaptor.addChild(root_1, stream_id.next());

							}
							stream_id.reset();

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end argList

	public static class constructor_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start constructor
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:208:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new'
	// qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );
	public final constructor_return constructor() throws RecognitionException {
		constructor_return retval = new constructor_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token string_literal114 = null;
		qualifiedId_return qualifiedId115 = null;

		ctorArgs_return ctorArgs116 = null;

		arrayConstructor_return arrayConstructor117 = null;

		Object string_literal114_tree = null;
		RewriteRuleTokenStream stream_91 = new RewriteRuleTokenStream(adaptor, "token 91");
		RewriteRuleSubtreeStream stream_qualifiedId = new RewriteRuleSubtreeStream(adaptor, "rule qualifiedId");
		RewriteRuleSubtreeStream stream_ctorArgs = new RewriteRuleSubtreeStream(adaptor, "rule ctorArgs");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:209:2: ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId
			// ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor )
			int alt27 = 2;
			int LA27_0 = input.LA(1);

			if ((LA27_0 == 91)) {
				int LA27_1 = input.LA(2);

				if ((LA27_1 == ID)) {
					int LA27_2 = input.LA(3);

					if ((synpred4())) {
						alt27 = 1;
					} else if ((true)) {
						alt27 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"208:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );",
								27, 2, input);

						throw nvae;
					}
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException(
							"208:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );",
							27, 1, input);

					throw nvae;
				}
			} else {
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"208:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );",
						27, 0, input);

				throw nvae;
			}
			switch (alt27) {
			case 1:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:209:4: ( 'new' qualifiedId LPAREN )=>
				// 'new' qualifiedId ctorArgs
			{
				string_literal114 = (Token) input.LT(1);
				match(input, 91, FOLLOW_91_in_constructor1252);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_91.add(string_literal114);

				pushFollow(FOLLOW_qualifiedId_in_constructor1254);
				qualifiedId115 = qualifiedId();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_qualifiedId.add(qualifiedId115.getTree());
				pushFollow(FOLLOW_ctorArgs_in_constructor1256);
				ctorArgs116 = ctorArgs();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ctorArgs.add(ctorArgs116.getTree());

				// AST REWRITE
				// elements: qualifiedId, ctorArgs
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 209:61: -> ^( CONSTRUCTOR qualifiedId ctorArgs )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:209:64: ^( CONSTRUCTOR
						// qualifiedId ctorArgs )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(CONSTRUCTOR, "CONSTRUCTOR"), root_1);

							adaptor.addChild(root_1, stream_qualifiedId.next());
							adaptor.addChild(root_1, stream_ctorArgs.next());

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}
				break;
			case 2:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:210:6: arrayConstructor
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_arrayConstructor_in_constructor1273);
				arrayConstructor117 = arrayConstructor();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, arrayConstructor117.getTree());

			}
				break;

			}
			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end constructor

	public static class arrayConstructor_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start arrayConstructor
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:213:1: arrayConstructor : 'new' qualifiedId arrayRank (
	// listInitializer )? -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? ) ;
	public final arrayConstructor_return arrayConstructor() throws RecognitionException {
		arrayConstructor_return retval = new arrayConstructor_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token string_literal118 = null;
		qualifiedId_return qualifiedId119 = null;

		arrayRank_return arrayRank120 = null;

		listInitializer_return listInitializer121 = null;

		Object string_literal118_tree = null;
		RewriteRuleTokenStream stream_91 = new RewriteRuleTokenStream(adaptor, "token 91");
		RewriteRuleSubtreeStream stream_listInitializer = new RewriteRuleSubtreeStream(adaptor, "rule listInitializer");
		RewriteRuleSubtreeStream stream_qualifiedId = new RewriteRuleSubtreeStream(adaptor, "rule qualifiedId");
		RewriteRuleSubtreeStream stream_arrayRank = new RewriteRuleSubtreeStream(adaptor, "rule arrayRank");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:214:2: ( 'new' qualifiedId arrayRank ( listInitializer )?
			// -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:214:4: 'new' qualifiedId arrayRank ( listInitializer )?
			{
				string_literal118 = (Token) input.LT(1);
				match(input, 91, FOLLOW_91_in_arrayConstructor1284);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_91.add(string_literal118);

				pushFollow(FOLLOW_qualifiedId_in_arrayConstructor1286);
				qualifiedId119 = qualifiedId();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_qualifiedId.add(qualifiedId119.getTree());
				pushFollow(FOLLOW_arrayRank_in_arrayConstructor1288);
				arrayRank120 = arrayRank();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_arrayRank.add(arrayRank120.getTree());
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:214:32: ( listInitializer )?
				int alt28 = 2;
				int LA28_0 = input.LA(1);

				if ((LA28_0 == LCURLY)) {
					alt28 = 1;
				}
				switch (alt28) {
				case 1:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:214:33: listInitializer
				{
					pushFollow(FOLLOW_listInitializer_in_arrayConstructor1291);
					listInitializer121 = listInitializer();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_listInitializer.add(listInitializer121.getTree());

				}
					break;

				}

				// AST REWRITE
				// elements: listInitializer, arrayRank, qualifiedId
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 215:4: -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:215:7: ^( CONSTRUCTOR_ARRAY
						// qualifiedId arrayRank ( listInitializer )? )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(
									adaptor.create(CONSTRUCTOR_ARRAY, "CONSTRUCTOR_ARRAY"), root_1);

							adaptor.addChild(root_1, stream_qualifiedId.next());
							adaptor.addChild(root_1, stream_arrayRank.next());
							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:215:49: ( listInitializer
							// )?
							if (stream_listInitializer.hasNext()) {
								adaptor.addChild(root_1, stream_listInitializer.next());

							}
							stream_listInitializer.reset();

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end arrayConstructor

	public static class arrayRank_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start arrayRank
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:218:1: arrayRank : LBRACKET ( expression ( COMMA expression )* )?
	// RBRACKET -> ^( EXPRESSIONLIST ( expression )* ) ;
	public final arrayRank_return arrayRank() throws RecognitionException {
		arrayRank_return retval = new arrayRank_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LBRACKET122 = null;
		Token COMMA124 = null;
		Token RBRACKET126 = null;
		expression_return expression123 = null;

		expression_return expression125 = null;

		Object LBRACKET122_tree = null;
		Object COMMA124_tree = null;
		Object RBRACKET126_tree = null;
		RewriteRuleTokenStream stream_COMMA = new RewriteRuleTokenStream(adaptor, "token COMMA");
		RewriteRuleTokenStream stream_LBRACKET = new RewriteRuleTokenStream(adaptor, "token LBRACKET");
		RewriteRuleTokenStream stream_RBRACKET = new RewriteRuleTokenStream(adaptor, "token RBRACKET");
		RewriteRuleSubtreeStream stream_expression = new RewriteRuleSubtreeStream(adaptor, "rule expression");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:219:5: ( LBRACKET ( expression ( COMMA expression )* )?
			// RBRACKET -> ^( EXPRESSIONLIST ( expression )* ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:219:7: LBRACKET ( expression ( COMMA expression )* )?
			// RBRACKET
			{
				LBRACKET122 = (Token) input.LT(1);
				match(input, LBRACKET, FOLLOW_LBRACKET_in_arrayRank1326);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LBRACKET.add(LBRACKET122);

				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:219:16: ( expression ( COMMA expression
				// )* )?
				int alt30 = 2;
				int LA30_0 = input.LA(1);

				if ((LA30_0 == INTEGER_LITERAL || LA30_0 == LPAREN || (LA30_0 >= PLUS && LA30_0 <= MINUS)
						|| LA30_0 == BANG || (LA30_0 >= POUND && LA30_0 <= DOLLAR)
						|| (LA30_0 >= AT && LA30_0 <= LBRACKET) || LA30_0 == PROJECT
						|| (LA30_0 >= SELECT && LA30_0 <= LAMBDA) || (LA30_0 >= LCURLY && LA30_0 <= FALSE) || (LA30_0 >= 91 && LA30_0 <= 92))) {
					alt30 = 1;
				}
				switch (alt30) {
				case 1:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:219:17: expression ( COMMA
					// expression )*
				{
					pushFollow(FOLLOW_expression_in_arrayRank1329);
					expression123 = expression();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_expression.add(expression123.getTree());
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:219:28: ( COMMA expression )*
					loop29: do {
						int alt29 = 2;
						int LA29_0 = input.LA(1);

						if ((LA29_0 == COMMA)) {
							alt29 = 1;
						}

						switch (alt29) {
						case 1:
							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:219:29: COMMA expression
						{
							COMMA124 = (Token) input.LT(1);
							match(input, COMMA, FOLLOW_COMMA_in_arrayRank1332);
							if (failed)
								return retval;
							if (backtracking == 0)
								stream_COMMA.add(COMMA124);

							pushFollow(FOLLOW_expression_in_arrayRank1334);
							expression125 = expression();
							_fsp--;
							if (failed)
								return retval;
							if (backtracking == 0)
								stream_expression.add(expression125.getTree());

						}
							break;

						default:
							break loop29;
						}
					} while (true);

				}
					break;

				}

				RBRACKET126 = (Token) input.LT(1);
				match(input, RBRACKET, FOLLOW_RBRACKET_in_arrayRank1340);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RBRACKET.add(RBRACKET126);

				// AST REWRITE
				// elements: expression
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 219:59: -> ^( EXPRESSIONLIST ( expression )* )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:219:62: ^( EXPRESSIONLIST (
						// expression )* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(EXPRESSIONLIST, "EXPRESSIONLIST"),
									root_1);

							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:219:79: ( expression )*
							while (stream_expression.hasNext()) {
								adaptor.addChild(root_1, stream_expression.next());

							}
							stream_expression.reset();

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end arrayRank

	public static class listInitializer_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start listInitializer
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:221:1: listInitializer : LCURLY expression ( COMMA expression )*
	// RCURLY -> ^( LIST_INITIALIZER ( expression )* ) ;
	public final listInitializer_return listInitializer() throws RecognitionException {
		listInitializer_return retval = new listInitializer_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LCURLY127 = null;
		Token COMMA129 = null;
		Token RCURLY131 = null;
		expression_return expression128 = null;

		expression_return expression130 = null;

		Object LCURLY127_tree = null;
		Object COMMA129_tree = null;
		Object RCURLY131_tree = null;
		RewriteRuleTokenStream stream_COMMA = new RewriteRuleTokenStream(adaptor, "token COMMA");
		RewriteRuleTokenStream stream_RCURLY = new RewriteRuleTokenStream(adaptor, "token RCURLY");
		RewriteRuleTokenStream stream_LCURLY = new RewriteRuleTokenStream(adaptor, "token LCURLY");
		RewriteRuleSubtreeStream stream_expression = new RewriteRuleSubtreeStream(adaptor, "rule expression");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:222:5: ( LCURLY expression ( COMMA expression )* RCURLY ->
			// ^( LIST_INITIALIZER ( expression )* ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:222:7: LCURLY expression ( COMMA expression )* RCURLY
			{
				LCURLY127 = (Token) input.LT(1);
				match(input, LCURLY, FOLLOW_LCURLY_in_listInitializer1365);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LCURLY.add(LCURLY127);

				pushFollow(FOLLOW_expression_in_listInitializer1367);
				expression128 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_expression.add(expression128.getTree());
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:222:25: ( COMMA expression )*
				loop31: do {
					int alt31 = 2;
					int LA31_0 = input.LA(1);

					if ((LA31_0 == COMMA)) {
						alt31 = 1;
					}

					switch (alt31) {
					case 1:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:222:26: COMMA expression
					{
						COMMA129 = (Token) input.LT(1);
						match(input, COMMA, FOLLOW_COMMA_in_listInitializer1370);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_COMMA.add(COMMA129);

						pushFollow(FOLLOW_expression_in_listInitializer1372);
						expression130 = expression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_expression.add(expression130.getTree());

					}
						break;

					default:
						break loop31;
					}
				} while (true);

				RCURLY131 = (Token) input.LT(1);
				match(input, RCURLY, FOLLOW_RCURLY_in_listInitializer1376);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RCURLY.add(RCURLY131);

				// AST REWRITE
				// elements: expression
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 222:52: -> ^( LIST_INITIALIZER ( expression )* )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:222:55: ^( LIST_INITIALIZER (
						// expression )* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(LIST_INITIALIZER, "LIST_INITIALIZER"),
									root_1);

							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:222:74: ( expression )*
							while (stream_expression.hasNext()) {
								adaptor.addChild(root_1, stream_expression.next());

							}
							stream_expression.reset();

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end listInitializer

	public static class mapInitializer_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start mapInitializer
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:227:1: mapInitializer : POUND LCURLY mapEntry ( COMMA mapEntry )*
	// RCURLY -> ^( MAP_INITIALIZER ( mapEntry )* ) ;
	public final mapInitializer_return mapInitializer() throws RecognitionException {
		mapInitializer_return retval = new mapInitializer_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token POUND132 = null;
		Token LCURLY133 = null;
		Token COMMA135 = null;
		Token RCURLY137 = null;
		mapEntry_return mapEntry134 = null;

		mapEntry_return mapEntry136 = null;

		Object POUND132_tree = null;
		Object LCURLY133_tree = null;
		Object COMMA135_tree = null;
		Object RCURLY137_tree = null;
		RewriteRuleTokenStream stream_COMMA = new RewriteRuleTokenStream(adaptor, "token COMMA");
		RewriteRuleTokenStream stream_RCURLY = new RewriteRuleTokenStream(adaptor, "token RCURLY");
		RewriteRuleTokenStream stream_LCURLY = new RewriteRuleTokenStream(adaptor, "token LCURLY");
		RewriteRuleTokenStream stream_POUND = new RewriteRuleTokenStream(adaptor, "token POUND");
		RewriteRuleSubtreeStream stream_mapEntry = new RewriteRuleSubtreeStream(adaptor, "rule mapEntry");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:228:5: ( POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY
			// -> ^( MAP_INITIALIZER ( mapEntry )* ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:228:7: POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY
			{
				POUND132 = (Token) input.LT(1);
				match(input, POUND, FOLLOW_POUND_in_mapInitializer1404);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_POUND.add(POUND132);

				LCURLY133 = (Token) input.LT(1);
				match(input, LCURLY, FOLLOW_LCURLY_in_mapInitializer1406);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LCURLY.add(LCURLY133);

				pushFollow(FOLLOW_mapEntry_in_mapInitializer1408);
				mapEntry134 = mapEntry();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_mapEntry.add(mapEntry134.getTree());
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:228:29: ( COMMA mapEntry )*
				loop32: do {
					int alt32 = 2;
					int LA32_0 = input.LA(1);

					if ((LA32_0 == COMMA)) {
						alt32 = 1;
					}

					switch (alt32) {
					case 1:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:228:30: COMMA mapEntry
					{
						COMMA135 = (Token) input.LT(1);
						match(input, COMMA, FOLLOW_COMMA_in_mapInitializer1411);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_COMMA.add(COMMA135);

						pushFollow(FOLLOW_mapEntry_in_mapInitializer1413);
						mapEntry136 = mapEntry();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_mapEntry.add(mapEntry136.getTree());

					}
						break;

					default:
						break loop32;
					}
				} while (true);

				RCURLY137 = (Token) input.LT(1);
				match(input, RCURLY, FOLLOW_RCURLY_in_mapInitializer1417);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RCURLY.add(RCURLY137);

				// AST REWRITE
				// elements: mapEntry
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 228:54: -> ^( MAP_INITIALIZER ( mapEntry )* )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:228:57: ^( MAP_INITIALIZER (
						// mapEntry )* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(MAP_INITIALIZER, "MAP_INITIALIZER"),
									root_1);

							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:228:75: ( mapEntry )*
							while (stream_mapEntry.hasNext()) {
								adaptor.addChild(root_1, stream_mapEntry.next());

							}
							stream_mapEntry.reset();

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end mapInitializer

	public static class mapEntry_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start mapEntry
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:230:1: mapEntry : expression COLON expression -> ^( MAP_ENTRY (
	// expression )* ) ;
	public final mapEntry_return mapEntry() throws RecognitionException {
		mapEntry_return retval = new mapEntry_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token COLON139 = null;
		expression_return expression138 = null;

		expression_return expression140 = null;

		Object COLON139_tree = null;
		RewriteRuleTokenStream stream_COLON = new RewriteRuleTokenStream(adaptor, "token COLON");
		RewriteRuleSubtreeStream stream_expression = new RewriteRuleSubtreeStream(adaptor, "rule expression");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:231:5: ( expression COLON expression -> ^( MAP_ENTRY (
			// expression )* ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:231:7: expression COLON expression
			{
				pushFollow(FOLLOW_expression_in_mapEntry1438);
				expression138 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_expression.add(expression138.getTree());
				COLON139 = (Token) input.LT(1);
				match(input, COLON, FOLLOW_COLON_in_mapEntry1440);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_COLON.add(COLON139);

				pushFollow(FOLLOW_expression_in_mapEntry1442);
				expression140 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_expression.add(expression140.getTree());

				// AST REWRITE
				// elements: expression
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 231:35: -> ^( MAP_ENTRY ( expression )* )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:231:38: ^( MAP_ENTRY (
						// expression )* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(MAP_ENTRY, "MAP_ENTRY"), root_1);

							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:231:50: ( expression )*
							while (stream_expression.hasNext()) {
								adaptor.addChild(root_1, stream_expression.next());

							}
							stream_expression.reset();

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end mapEntry

	public static class ctorArgs_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start ctorArgs
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:233:1: ctorArgs : LPAREN ( namedArgument ( COMMA namedArgument )*
	// )? RPAREN ;
	public final ctorArgs_return ctorArgs() throws RecognitionException {
		ctorArgs_return retval = new ctorArgs_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LPAREN141 = null;
		Token COMMA143 = null;
		Token RPAREN145 = null;
		namedArgument_return namedArgument142 = null;

		namedArgument_return namedArgument144 = null;

		Object LPAREN141_tree = null;
		Object COMMA143_tree = null;
		Object RPAREN145_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:234:2: ( LPAREN ( namedArgument ( COMMA namedArgument )*
			// )? RPAREN )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:234:4: LPAREN ( namedArgument ( COMMA namedArgument )* )?
			// RPAREN
			{
				root_0 = (Object) adaptor.nil();

				LPAREN141 = (Token) input.LT(1);
				match(input, LPAREN, FOLLOW_LPAREN_in_ctorArgs1460);
				if (failed)
					return retval;
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:234:12: ( namedArgument ( COMMA
				// namedArgument )* )?
				int alt34 = 2;
				int LA34_0 = input.LA(1);

				if ((LA34_0 == INTEGER_LITERAL || LA34_0 == LPAREN || (LA34_0 >= PLUS && LA34_0 <= MINUS)
						|| LA34_0 == BANG || (LA34_0 >= POUND && LA34_0 <= DOLLAR)
						|| (LA34_0 >= AT && LA34_0 <= LBRACKET) || LA34_0 == PROJECT
						|| (LA34_0 >= SELECT && LA34_0 <= LAMBDA) || (LA34_0 >= LCURLY && LA34_0 <= FALSE) || (LA34_0 >= 91 && LA34_0 <= 92))) {
					alt34 = 1;
				}
				switch (alt34) {
				case 1:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:234:13: namedArgument ( COMMA
					// namedArgument )*
				{
					pushFollow(FOLLOW_namedArgument_in_ctorArgs1464);
					namedArgument142 = namedArgument();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						adaptor.addChild(root_0, namedArgument142.getTree());
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:234:27: ( COMMA namedArgument )*
					loop33: do {
						int alt33 = 2;
						int LA33_0 = input.LA(1);

						if ((LA33_0 == COMMA)) {
							alt33 = 1;
						}

						switch (alt33) {
						case 1:
							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:234:28: COMMA
							// namedArgument
						{
							COMMA143 = (Token) input.LT(1);
							match(input, COMMA, FOLLOW_COMMA_in_ctorArgs1467);
							if (failed)
								return retval;
							pushFollow(FOLLOW_namedArgument_in_ctorArgs1470);
							namedArgument144 = namedArgument();
							_fsp--;
							if (failed)
								return retval;
							if (backtracking == 0)
								adaptor.addChild(root_0, namedArgument144.getTree());

						}
							break;

						default:
							break loop33;
						}
					} while (true);

				}
					break;

				}

				RPAREN145 = (Token) input.LT(1);
				match(input, RPAREN, FOLLOW_RPAREN_in_ctorArgs1476);
				if (failed)
					return retval;

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end ctorArgs

	public static class argument_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start argument
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:236:1: argument : expression ;
	public final argument_return argument() throws RecognitionException {
		argument_return retval = new argument_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		expression_return expression146 = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:236:10: ( expression )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:236:12: expression
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_expression_in_argument1485);
				expression146 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, expression146.getTree());

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end argument

	public static class namedArgument_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start namedArgument
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:238:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression ->
	// ^( NAMED_ARGUMENT[$id] expression ) | argument );
	public final namedArgument_return namedArgument() throws RecognitionException {
		namedArgument_return retval = new namedArgument_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token id = null;
		Token ASSIGN147 = null;
		expression_return expression148 = null;

		argument_return argument149 = null;

		Object id_tree = null;
		Object ASSIGN147_tree = null;
		RewriteRuleTokenStream stream_ASSIGN = new RewriteRuleTokenStream(adaptor, "token ASSIGN");
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");
		RewriteRuleSubtreeStream stream_expression = new RewriteRuleSubtreeStream(adaptor, "rule expression");
		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:239:5: ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^(
			// NAMED_ARGUMENT[$id] expression ) | argument )
			int alt35 = 2;
			int LA35_0 = input.LA(1);

			if ((LA35_0 == ID)) {
				int LA35_1 = input.LA(2);

				if ((LA35_1 == ASSIGN)) {
					int LA35_26 = input.LA(3);

					if ((synpred5())) {
						alt35 = 1;
					} else if ((true)) {
						alt35 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"238:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );",
								35, 26, input);

						throw nvae;
					}
				} else if (((LA35_1 >= DEFAULT && LA35_1 <= QMARK) || (LA35_1 >= LPAREN && LA35_1 <= POWER)
						|| LA35_1 == DOT || LA35_1 == COMMA || LA35_1 == LBRACKET || (LA35_1 >= EQUAL && LA35_1 <= MATCHES))) {
					alt35 = 2;
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException(
							"238:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );",
							35, 1, input);

					throw nvae;
				}
			} else if ((LA35_0 == INTEGER_LITERAL || LA35_0 == LPAREN || (LA35_0 >= PLUS && LA35_0 <= MINUS)
					|| LA35_0 == BANG || LA35_0 == POUND || LA35_0 == DOLLAR || (LA35_0 >= AT && LA35_0 <= LBRACKET)
					|| LA35_0 == PROJECT || (LA35_0 >= SELECT && LA35_0 <= LAMBDA)
					|| (LA35_0 >= LCURLY && LA35_0 <= FALSE) || (LA35_0 >= 91 && LA35_0 <= 92))) {
				alt35 = 2;
			} else {
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"238:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );",
						35, 0, input);

				throw nvae;
			}
			switch (alt35) {
			case 1:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:239:7: ( ID ASSIGN )=>id= ID ASSIGN
				// expression
			{
				id = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_namedArgument1508);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(id);

				ASSIGN147 = (Token) input.LT(1);
				match(input, ASSIGN, FOLLOW_ASSIGN_in_namedArgument1510);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ASSIGN.add(ASSIGN147);

				pushFollow(FOLLOW_expression_in_namedArgument1512);
				expression148 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_expression.add(expression148.getTree());

				// AST REWRITE
				// elements: expression
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 240:19: -> ^( NAMED_ARGUMENT[$id] expression )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:240:22: ^( NAMED_ARGUMENT[$id]
						// expression )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(NAMED_ARGUMENT, id), root_1);

							adaptor.addChild(root_1, stream_expression.next());

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}
				break;
			case 2:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:241:7: argument
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_argument_in_namedArgument1548);
				argument149 = argument();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, argument149.getTree());

			}
				break;

			}
			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end namedArgument

	public static class qualifiedId_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start qualifiedId
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:243:1: qualifiedId : ID ( DOT ID )* -> ^( QUALIFIED_IDENTIFIER ( ID
	// )* ) ;
	public final qualifiedId_return qualifiedId() throws RecognitionException {
		qualifiedId_return retval = new qualifiedId_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token ID150 = null;
		Token DOT151 = null;
		Token ID152 = null;

		Object ID150_tree = null;
		Object DOT151_tree = null;
		Object ID152_tree = null;
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");
		RewriteRuleTokenStream stream_DOT = new RewriteRuleTokenStream(adaptor, "token DOT");

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:243:13: ( ID ( DOT ID )* -> ^( QUALIFIED_IDENTIFIER ( ID
			// )* ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:243:15: ID ( DOT ID )*
			{
				ID150 = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_qualifiedId1560);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(ID150);

				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:243:18: ( DOT ID )*
				loop36: do {
					int alt36 = 2;
					int LA36_0 = input.LA(1);

					if ((LA36_0 == DOT)) {
						alt36 = 1;
					}

					switch (alt36) {
					case 1:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:243:19: DOT ID
					{
						DOT151 = (Token) input.LT(1);
						match(input, DOT, FOLLOW_DOT_in_qualifiedId1563);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_DOT.add(DOT151);

						ID152 = (Token) input.LT(1);
						match(input, ID, FOLLOW_ID_in_qualifiedId1565);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_ID.add(ID152);

					}
						break;

					default:
						break loop36;
					}
				} while (true);

				// AST REWRITE
				// elements: ID
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 243:28: -> ^( QUALIFIED_IDENTIFIER ( ID )* )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:243:31: ^( QUALIFIED_IDENTIFIER
						// ( ID )* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(QUALIFIED_IDENTIFIER,
									"QUALIFIED_IDENTIFIER"), root_1);

							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:243:54: ( ID )*
							while (stream_ID.hasNext()) {
								adaptor.addChild(root_1, stream_ID.next());

							}
							stream_ID.reset();

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end qualifiedId

	public static class contextName_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start contextName
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:245:1: contextName : ID ( DIV ID )* -> ^( QUALIFIED_IDENTIFIER ( ID
	// )* ) ;
	public final contextName_return contextName() throws RecognitionException {
		contextName_return retval = new contextName_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token ID153 = null;
		Token DIV154 = null;
		Token ID155 = null;

		Object ID153_tree = null;
		Object DIV154_tree = null;
		Object ID155_tree = null;
		RewriteRuleTokenStream stream_DIV = new RewriteRuleTokenStream(adaptor, "token DIV");
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:245:13: ( ID ( DIV ID )* -> ^( QUALIFIED_IDENTIFIER ( ID
			// )* ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:245:15: ID ( DIV ID )*
			{
				ID153 = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_contextName1584);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(ID153);

				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:245:18: ( DIV ID )*
				loop37: do {
					int alt37 = 2;
					int LA37_0 = input.LA(1);

					if ((LA37_0 == DIV)) {
						alt37 = 1;
					}

					switch (alt37) {
					case 1:
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:245:19: DIV ID
					{
						DIV154 = (Token) input.LT(1);
						match(input, DIV, FOLLOW_DIV_in_contextName1587);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_DIV.add(DIV154);

						ID155 = (Token) input.LT(1);
						match(input, ID, FOLLOW_ID_in_contextName1589);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_ID.add(ID155);

					}
						break;

					default:
						break loop37;
					}
				} while (true);

				// AST REWRITE
				// elements: ID
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 245:28: -> ^( QUALIFIED_IDENTIFIER ( ID )* )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:245:31: ^( QUALIFIED_IDENTIFIER
						// ( ID )* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(QUALIFIED_IDENTIFIER,
									"QUALIFIED_IDENTIFIER"), root_1);

							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:245:54: ( ID )*
							while (stream_ID.hasNext()) {
								adaptor.addChild(root_1, stream_ID.next());

							}
							stream_ID.reset();

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end contextName

	public static class literal_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start literal
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:247:1: literal : ( INTEGER_LITERAL | STRING_LITERAL |
	// DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL | dateLiteral );
	public final literal_return literal() throws RecognitionException {
		literal_return retval = new literal_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token INTEGER_LITERAL156 = null;
		Token STRING_LITERAL157 = null;
		Token DQ_STRING_LITERAL158 = null;
		Token NULL_LITERAL160 = null;
		Token HEXADECIMAL_INTEGER_LITERAL161 = null;
		Token REAL_LITERAL162 = null;
		boolLiteral_return boolLiteral159 = null;

		dateLiteral_return dateLiteral163 = null;

		Object INTEGER_LITERAL156_tree = null;
		Object STRING_LITERAL157_tree = null;
		Object DQ_STRING_LITERAL158_tree = null;
		Object NULL_LITERAL160_tree = null;
		Object HEXADECIMAL_INTEGER_LITERAL161_tree = null;
		Object REAL_LITERAL162_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:248:2: ( INTEGER_LITERAL | STRING_LITERAL |
			// DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL | dateLiteral
			// )
			int alt38 = 8;
			switch (input.LA(1)) {
			case INTEGER_LITERAL: {
				alt38 = 1;
			}
				break;
			case STRING_LITERAL: {
				alt38 = 2;
			}
				break;
			case DQ_STRING_LITERAL: {
				alt38 = 3;
			}
				break;
			case TRUE:
			case FALSE: {
				alt38 = 4;
			}
				break;
			case NULL_LITERAL: {
				alt38 = 5;
			}
				break;
			case HEXADECIMAL_INTEGER_LITERAL: {
				alt38 = 6;
			}
				break;
			case REAL_LITERAL: {
				alt38 = 7;
			}
				break;
			case 92: {
				alt38 = 8;
			}
				break;
			default:
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"247:1: literal : ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL | dateLiteral );",
						38, 0, input);

				throw nvae;
			}

			switch (alt38) {
			case 1:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:248:4: INTEGER_LITERAL
			{
				root_0 = (Object) adaptor.nil();

				INTEGER_LITERAL156 = (Token) input.LT(1);
				match(input, INTEGER_LITERAL, FOLLOW_INTEGER_LITERAL_in_literal1610);
				if (failed)
					return retval;
				if (backtracking == 0) {
					INTEGER_LITERAL156_tree = (Object) adaptor.create(INTEGER_LITERAL156);
					adaptor.addChild(root_0, INTEGER_LITERAL156_tree);
				}

			}
				break;
			case 2:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:249:4: STRING_LITERAL
			{
				root_0 = (Object) adaptor.nil();

				STRING_LITERAL157 = (Token) input.LT(1);
				match(input, STRING_LITERAL, FOLLOW_STRING_LITERAL_in_literal1616);
				if (failed)
					return retval;
				if (backtracking == 0) {
					STRING_LITERAL157_tree = (Object) adaptor.create(STRING_LITERAL157);
					adaptor.addChild(root_0, STRING_LITERAL157_tree);
				}

			}
				break;
			case 3:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:250:4: DQ_STRING_LITERAL
			{
				root_0 = (Object) adaptor.nil();

				DQ_STRING_LITERAL158 = (Token) input.LT(1);
				match(input, DQ_STRING_LITERAL, FOLLOW_DQ_STRING_LITERAL_in_literal1621);
				if (failed)
					return retval;
				if (backtracking == 0) {
					DQ_STRING_LITERAL158_tree = (Object) adaptor.create(DQ_STRING_LITERAL158);
					adaptor.addChild(root_0, DQ_STRING_LITERAL158_tree);
				}

			}
				break;
			case 4:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:251:4: boolLiteral
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_boolLiteral_in_literal1626);
				boolLiteral159 = boolLiteral();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, boolLiteral159.getTree());

			}
				break;
			case 5:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:252:4: NULL_LITERAL
			{
				root_0 = (Object) adaptor.nil();

				NULL_LITERAL160 = (Token) input.LT(1);
				match(input, NULL_LITERAL, FOLLOW_NULL_LITERAL_in_literal1631);
				if (failed)
					return retval;
				if (backtracking == 0) {
					NULL_LITERAL160_tree = (Object) adaptor.create(NULL_LITERAL160);
					adaptor.addChild(root_0, NULL_LITERAL160_tree);
				}

			}
				break;
			case 6:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:253:4: HEXADECIMAL_INTEGER_LITERAL
			{
				root_0 = (Object) adaptor.nil();

				HEXADECIMAL_INTEGER_LITERAL161 = (Token) input.LT(1);
				match(input, HEXADECIMAL_INTEGER_LITERAL, FOLLOW_HEXADECIMAL_INTEGER_LITERAL_in_literal1636);
				if (failed)
					return retval;
				if (backtracking == 0) {
					HEXADECIMAL_INTEGER_LITERAL161_tree = (Object) adaptor.create(HEXADECIMAL_INTEGER_LITERAL161);
					adaptor.addChild(root_0, HEXADECIMAL_INTEGER_LITERAL161_tree);
				}

			}
				break;
			case 7:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:254:4: REAL_LITERAL
			{
				root_0 = (Object) adaptor.nil();

				REAL_LITERAL162 = (Token) input.LT(1);
				match(input, REAL_LITERAL, FOLLOW_REAL_LITERAL_in_literal1642);
				if (failed)
					return retval;
				if (backtracking == 0) {
					REAL_LITERAL162_tree = (Object) adaptor.create(REAL_LITERAL162);
					adaptor.addChild(root_0, REAL_LITERAL162_tree);
				}

			}
				break;
			case 8:
				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:255:4: dateLiteral
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_dateLiteral_in_literal1647);
				dateLiteral163 = dateLiteral();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, dateLiteral163.getTree());

			}
				break;

			}
			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end literal

	public static class boolLiteral_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start boolLiteral
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:258:1: boolLiteral : ( TRUE | FALSE );
	public final boolLiteral_return boolLiteral() throws RecognitionException {
		boolLiteral_return retval = new boolLiteral_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token set164 = null;

		Object set164_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:258:12: ( TRUE | FALSE )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:
			{
				root_0 = (Object) adaptor.nil();

				set164 = (Token) input.LT(1);
				if ((input.LA(1) >= TRUE && input.LA(1) <= FALSE)) {
					input.consume();
					if (backtracking == 0)
						adaptor.addChild(root_0, adaptor.create(set164));
					errorRecovery = false;
					failed = false;
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					MismatchedSetException mse = new MismatchedSetException(null, input);
					recoverFromMismatchedSet(input, mse, FOLLOW_set_in_boolLiteral0);
					throw mse;
				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end boolLiteral

	public static class dateLiteral_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start dateLiteral
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:260:1: dateLiteral : 'date' LPAREN d= STRING_LITERAL ( COMMA f=
	// STRING_LITERAL )? RPAREN -> ^( DATE_LITERAL $d ( $f)? ) ;
	public final dateLiteral_return dateLiteral() throws RecognitionException {
		dateLiteral_return retval = new dateLiteral_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token d = null;
		Token f = null;
		Token string_literal165 = null;
		Token LPAREN166 = null;
		Token COMMA167 = null;
		Token RPAREN168 = null;

		Object d_tree = null;
		Object f_tree = null;
		Object string_literal165_tree = null;
		Object LPAREN166_tree = null;
		Object COMMA167_tree = null;
		Object RPAREN168_tree = null;
		RewriteRuleTokenStream stream_COMMA = new RewriteRuleTokenStream(adaptor, "token COMMA");
		RewriteRuleTokenStream stream_RPAREN = new RewriteRuleTokenStream(adaptor, "token RPAREN");
		RewriteRuleTokenStream stream_LPAREN = new RewriteRuleTokenStream(adaptor, "token LPAREN");
		RewriteRuleTokenStream stream_92 = new RewriteRuleTokenStream(adaptor, "token 92");
		RewriteRuleTokenStream stream_STRING_LITERAL = new RewriteRuleTokenStream(adaptor, "token STRING_LITERAL");

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:260:12: ( 'date' LPAREN d= STRING_LITERAL ( COMMA f=
			// STRING_LITERAL )? RPAREN -> ^( DATE_LITERAL $d ( $f)? ) )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:260:14: 'date' LPAREN d= STRING_LITERAL ( COMMA f=
			// STRING_LITERAL )? RPAREN
			{
				string_literal165 = (Token) input.LT(1);
				match(input, 92, FOLLOW_92_in_dateLiteral1668);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_92.add(string_literal165);

				LPAREN166 = (Token) input.LT(1);
				match(input, LPAREN, FOLLOW_LPAREN_in_dateLiteral1670);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LPAREN.add(LPAREN166);

				d = (Token) input.LT(1);
				match(input, STRING_LITERAL, FOLLOW_STRING_LITERAL_in_dateLiteral1674);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_STRING_LITERAL.add(d);

				// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
				// springframework/expression/spel/generated/SpringExpressions.g:260:45: ( COMMA f= STRING_LITERAL )?
				int alt39 = 2;
				int LA39_0 = input.LA(1);

				if ((LA39_0 == COMMA)) {
					alt39 = 1;
				}
				switch (alt39) {
				case 1:
					// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
					// springframework/expression/spel/generated/SpringExpressions.g:260:46: COMMA f= STRING_LITERAL
				{
					COMMA167 = (Token) input.LT(1);
					match(input, COMMA, FOLLOW_COMMA_in_dateLiteral1677);
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_COMMA.add(COMMA167);

					f = (Token) input.LT(1);
					match(input, STRING_LITERAL, FOLLOW_STRING_LITERAL_in_dateLiteral1681);
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_STRING_LITERAL.add(f);

				}
					break;

				}

				RPAREN168 = (Token) input.LT(1);
				match(input, RPAREN, FOLLOW_RPAREN_in_dateLiteral1685);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RPAREN.add(RPAREN168);

				// AST REWRITE
				// elements: d, f
				// token labels: d, f
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleTokenStream stream_d = new RewriteRuleTokenStream(adaptor, "token d", d);
					RewriteRuleTokenStream stream_f = new RewriteRuleTokenStream(adaptor, "token f", f);
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 260:78: -> ^( DATE_LITERAL $d ( $f)? )
					{
						// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/
						// springframework/expression/spel/generated/SpringExpressions.g:260:81: ^( DATE_LITERAL $d (
						// $f)? )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(DATE_LITERAL, "DATE_LITERAL"), root_1);

							adaptor.addChild(root_1, stream_d.next());
							///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org
							// /springframework/expression/spel/generated/SpringExpressions.g:260:99: ( $f)?
							if (stream_f.hasNext()) {
								adaptor.addChild(root_1, stream_f.next());

							}
							stream_f.reset();

							adaptor.addChild(root_0, root_1);
						}

					}

				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end dateLiteral

	public static class relationalOperator_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start relationalOperator
	// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
	// expression/spel/generated/SpringExpressions.g:267:1: relationalOperator : ( EQUAL | NOT_EQUAL | LESS_THAN |
	// LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL | IN | IS | BETWEEN | MATCHES );
	public final relationalOperator_return relationalOperator() throws RecognitionException {
		relationalOperator_return retval = new relationalOperator_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token set169 = null;

		Object set169_tree = null;

		try {
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:268:5: ( EQUAL | NOT_EQUAL | LESS_THAN |
			// LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL | IN | IS | BETWEEN | MATCHES )
			///Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework
			// /expression/spel/generated/SpringExpressions.g:
			{
				root_0 = (Object) adaptor.nil();

				set169 = (Token) input.LT(1);
				if ((input.LA(1) >= EQUAL && input.LA(1) <= MATCHES)) {
					input.consume();
					if (backtracking == 0)
						adaptor.addChild(root_0, adaptor.create(set169));
					errorRecovery = false;
					failed = false;
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					MismatchedSetException mse = new MismatchedSetException(null, input);
					recoverFromMismatchedSet(input, mse, FOLLOW_set_in_relationalOperator0);
					throw mse;
				}

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end relationalOperator

	// $ANTLR start synpred1
	public final void synpred1_fragment() throws RecognitionException {
		// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
		// expression/spel/generated/SpringExpressions.g:137:7: ( POUND ID LPAREN )
		// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
		// expression/spel/generated/SpringExpressions.g:137:8: POUND ID LPAREN
		{
			match(input, POUND, FOLLOW_POUND_in_synpred1745);
			if (failed)
				return;
			match(input, ID, FOLLOW_ID_in_synpred1747);
			if (failed)
				return;
			match(input, LPAREN, FOLLOW_LPAREN_in_synpred1749);
			if (failed)
				return;

		}
	}

	// $ANTLR end synpred1

	// $ANTLR start synpred2
	public final void synpred2_fragment() throws RecognitionException {
		// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
		// expression/spel/generated/SpringExpressions.g:146:4: ( DOLLAR ID LPAREN )
		// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
		// expression/spel/generated/SpringExpressions.g:146:5: DOLLAR ID LPAREN
		{
			match(input, DOLLAR, FOLLOW_DOLLAR_in_synpred2828);
			if (failed)
				return;
			match(input, ID, FOLLOW_ID_in_synpred2830);
			if (failed)
				return;
			match(input, LPAREN, FOLLOW_LPAREN_in_synpred2832);
			if (failed)
				return;

		}
	}

	// $ANTLR end synpred2

	// $ANTLR start synpred3
	public final void synpred3_fragment() throws RecognitionException {
		// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
		// expression/spel/generated/SpringExpressions.g:154:4: ( ID LPAREN )
		// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
		// expression/spel/generated/SpringExpressions.g:154:5: ID LPAREN
		{
			match(input, ID, FOLLOW_ID_in_synpred3894);
			if (failed)
				return;
			match(input, LPAREN, FOLLOW_LPAREN_in_synpred3896);
			if (failed)
				return;

		}
	}

	// $ANTLR end synpred3

	// $ANTLR start synpred4
	public final void synpred4_fragment() throws RecognitionException {
		// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
		// expression/spel/generated/SpringExpressions.g:209:4: ( 'new' qualifiedId LPAREN )
		// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
		// expression/spel/generated/SpringExpressions.g:209:5: 'new' qualifiedId LPAREN
		{
			match(input, 91, FOLLOW_91_in_synpred41243);
			if (failed)
				return;
			pushFollow(FOLLOW_qualifiedId_in_synpred41245);
			qualifiedId();
			_fsp--;
			if (failed)
				return;
			match(input, LPAREN, FOLLOW_LPAREN_in_synpred41247);
			if (failed)
				return;

		}
	}

	// $ANTLR end synpred4

	// $ANTLR start synpred5
	public final void synpred5_fragment() throws RecognitionException {
		// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
		// expression/spel/generated/SpringExpressions.g:239:7: ( ID ASSIGN )
		// /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/
		// expression/spel/generated/SpringExpressions.g:239:8: ID ASSIGN
		{
			match(input, ID, FOLLOW_ID_in_synpred51499);
			if (failed)
				return;
			match(input, ASSIGN, FOLLOW_ASSIGN_in_synpred51501);
			if (failed)
				return;

		}
	}

	// $ANTLR end synpred5

	public final boolean synpred4() {
		backtracking++;
		int start = input.mark();
		try {
			synpred4_fragment(); // can never throw exception
		} catch (RecognitionException re) {
			System.err.println("impossible: " + re);
		}
		boolean success = !failed;
		input.rewind(start);
		backtracking--;
		failed = false;
		return success;
	}

	public final boolean synpred2() {
		backtracking++;
		int start = input.mark();
		try {
			synpred2_fragment(); // can never throw exception
		} catch (RecognitionException re) {
			System.err.println("impossible: " + re);
		}
		boolean success = !failed;
		input.rewind(start);
		backtracking--;
		failed = false;
		return success;
	}

	public final boolean synpred3() {
		backtracking++;
		int start = input.mark();
		try {
			synpred3_fragment(); // can never throw exception
		} catch (RecognitionException re) {
			System.err.println("impossible: " + re);
		}
		boolean success = !failed;
		input.rewind(start);
		backtracking--;
		failed = false;
		return success;
	}

	public final boolean synpred1() {
		backtracking++;
		int start = input.mark();
		try {
			synpred1_fragment(); // can never throw exception
		} catch (RecognitionException re) {
			System.err.println("impossible: " + re);
		}
		boolean success = !failed;
		input.rewind(start);
		backtracking--;
		failed = false;
		return success;
	}

	public final boolean synpred5() {
		backtracking++;
		int start = input.mark();
		try {
			synpred5_fragment(); // can never throw exception
		} catch (RecognitionException re) {
			System.err.println("impossible: " + re);
		}
		boolean success = !failed;
		input.rewind(start);
		backtracking--;
		failed = false;
		return success;
	}

	public static final BitSet FOLLOW_expression_in_expr181 = new BitSet(new long[] { 0x0000000000000000L });
	public static final BitSet FOLLOW_EOF_in_expr183 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_logicalOrExpression_in_expression212 = new BitSet(
			new long[] { 0x0000000380000002L });
	public static final BitSet FOLLOW_ASSIGN_in_expression221 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_logicalOrExpression_in_expression224 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_DEFAULT_in_expression234 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_logicalOrExpression_in_expression237 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_QMARK_in_expression247 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_expression_in_expression250 = new BitSet(new long[] { 0x0000000400000000L });
	public static final BitSet FOLLOW_COLON_in_expression252 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_expression_in_expression255 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LPAREN_in_parenExpr266 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_expression_in_parenExpr269 = new BitSet(new long[] { 0x0000001000000000L });
	public static final BitSet FOLLOW_RPAREN_in_parenExpr271 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_logicalAndExpression_in_logicalOrExpression284 = new BitSet(
			new long[] { 0x0000002000000002L });
	public static final BitSet FOLLOW_OR_in_logicalOrExpression287 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_logicalAndExpression_in_logicalOrExpression290 = new BitSet(
			new long[] { 0x0000002000000002L });
	public static final BitSet FOLLOW_relationalExpression_in_logicalAndExpression325 = new BitSet(
			new long[] { 0x0000004000000002L });
	public static final BitSet FOLLOW_AND_in_logicalAndExpression328 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_relationalExpression_in_logicalAndExpression331 = new BitSet(
			new long[] { 0x0000004000000002L });
	public static final BitSet FOLLOW_sumExpression_in_relationalExpression342 = new BitSet(new long[] {
			0x0000000000000002L, 0x000000000007FE00L });
	public static final BitSet FOLLOW_relationalOperator_in_relationalExpression345 = new BitSet(new long[] {
			0xDF5BA18800000020L, 0x000000001800003FL });
	public static final BitSet FOLLOW_sumExpression_in_relationalExpression348 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_productExpression_in_sumExpression359 = new BitSet(
			new long[] { 0x0000018000000002L });
	public static final BitSet FOLLOW_PLUS_in_sumExpression364 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_MINUS_in_sumExpression369 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_productExpression_in_sumExpression373 = new BitSet(
			new long[] { 0x0000018000000002L });
	public static final BitSet FOLLOW_powerExpr_in_productExpression385 = new BitSet(new long[] { 0x00000E0000000002L });
	public static final BitSet FOLLOW_STAR_in_productExpression389 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_DIV_in_productExpression394 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_MOD_in_productExpression398 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_powerExpr_in_productExpression402 = new BitSet(new long[] { 0x00000E0000000002L });
	public static final BitSet FOLLOW_unaryExpression_in_powerExpr414 = new BitSet(new long[] { 0x0000100000000002L });
	public static final BitSet FOLLOW_POWER_in_powerExpr417 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_unaryExpression_in_powerExpr420 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_PLUS_in_unaryExpression434 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_MINUS_in_unaryExpression439 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_BANG_in_unaryExpression444 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_unaryExpression_in_unaryExpression448 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_primaryExpression_in_unaryExpression454 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_startNode_in_primaryExpression468 = new BitSet(new long[] { 0x0010400000000002L });
	public static final BitSet FOLLOW_node_in_primaryExpression471 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_parenExpr_in_startNode504 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_methodOrProperty_in_startNode512 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_functionOrVar_in_startNode521 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_localFunctionOrVar_in_startNode529 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_reference_in_startNode537 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_indexer_in_startNode545 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_literal_in_startNode553 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_type_in_startNode561 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_constructor_in_startNode569 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_projection_in_startNode577 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_selection_in_startNode586 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_firstSelection_in_startNode595 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_lastSelection_in_startNode603 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_listInitializer_in_startNode611 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_mapInitializer_in_startNode619 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_lambda_in_startNode627 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_DOT_in_node647 = new BitSet(new long[] { 0x0741800000000000L });
	public static final BitSet FOLLOW_dottedNode_in_node649 = new BitSet(new long[] { 0x0010400000000002L });
	public static final BitSet FOLLOW_nonDottedNode_in_node654 = new BitSet(new long[] { 0x0010400000000002L });
	public static final BitSet FOLLOW_indexer_in_nonDottedNode666 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_methodOrProperty_in_dottedNode679 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_functionOrVar_in_dottedNode685 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_projection_in_dottedNode693 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_selection_in_dottedNode702 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_firstSelection_in_dottedNode711 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_lastSelection_in_dottedNode720 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_function_in_functionOrVar754 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_var_in_functionOrVar762 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_POUND_in_function779 = new BitSet(new long[] { 0x0001000000000000L });
	public static final BitSet FOLLOW_ID_in_function783 = new BitSet(new long[] { 0x0000000800000000L });
	public static final BitSet FOLLOW_methodArgs_in_function785 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_POUND_in_var806 = new BitSet(new long[] { 0x0001000000000000L });
	public static final BitSet FOLLOW_ID_in_var810 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_localFunction_in_localFunctionOrVar837 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_localVar_in_localFunctionOrVar842 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_DOLLAR_in_localFunction852 = new BitSet(new long[] { 0x0001000000000000L });
	public static final BitSet FOLLOW_ID_in_localFunction856 = new BitSet(new long[] { 0x0000000800000000L });
	public static final BitSet FOLLOW_methodArgs_in_localFunction858 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_DOLLAR_in_localVar873 = new BitSet(new long[] { 0x0001000000000000L });
	public static final BitSet FOLLOW_ID_in_localVar877 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_ID_in_methodOrProperty903 = new BitSet(new long[] { 0x0000000800000000L });
	public static final BitSet FOLLOW_methodArgs_in_methodOrProperty905 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_property_in_methodOrProperty919 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LPAREN_in_methodArgs934 = new BitSet(new long[] { 0xDF5BA19800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_argument_in_methodArgs938 = new BitSet(new long[] { 0x0004001000000000L });
	public static final BitSet FOLLOW_COMMA_in_methodArgs941 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_argument_in_methodArgs944 = new BitSet(new long[] { 0x0004001000000000L });
	public static final BitSet FOLLOW_COMMA_in_methodArgs949 = new BitSet(new long[] { 0x0000001000000000L });
	public static final BitSet FOLLOW_RPAREN_in_methodArgs956 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_ID_in_property969 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_AT_in_reference991 = new BitSet(new long[] { 0x0000000800000000L });
	public static final BitSet FOLLOW_LPAREN_in_reference995 = new BitSet(new long[] { 0x0001001000000000L });
	public static final BitSet FOLLOW_contextName_in_reference1000 = new BitSet(new long[] { 0x0000000400000000L });
	public static final BitSet FOLLOW_COLON_in_reference1002 = new BitSet(new long[] { 0x0001001000000000L });
	public static final BitSet FOLLOW_qualifiedId_in_reference1009 = new BitSet(new long[] { 0x0000001000000000L });
	public static final BitSet FOLLOW_RPAREN_in_reference1013 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LBRACKET_in_indexer1048 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_argument_in_indexer1052 = new BitSet(new long[] { 0x0024000000000000L });
	public static final BitSet FOLLOW_COMMA_in_indexer1055 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_argument_in_indexer1059 = new BitSet(new long[] { 0x0024000000000000L });
	public static final BitSet FOLLOW_RBRACKET_in_indexer1063 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_PROJECT_in_projection1090 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_expression_in_projection1093 = new BitSet(new long[] { 0x0080000000000000L });
	public static final BitSet FOLLOW_RCURLY_in_projection1095 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_SELECT_in_selection1103 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_expression_in_selection1106 = new BitSet(new long[] { 0x0080000000000000L });
	public static final BitSet FOLLOW_RCURLY_in_selection1108 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_SELECT_FIRST_in_firstSelection1116 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_expression_in_firstSelection1119 = new BitSet(new long[] { 0x0080000000000000L });
	public static final BitSet FOLLOW_RCURLY_in_firstSelection1121 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_SELECT_LAST_in_lastSelection1129 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_expression_in_lastSelection1132 = new BitSet(new long[] { 0x0080000000000000L });
	public static final BitSet FOLLOW_RCURLY_in_lastSelection1134 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_TYPE_in_type1143 = new BitSet(new long[] { 0x0001000000000000L });
	public static final BitSet FOLLOW_qualifiedId_in_type1145 = new BitSet(new long[] { 0x0000001000000000L });
	public static final BitSet FOLLOW_RPAREN_in_type1147 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LAMBDA_in_lambda1174 = new BitSet(new long[] { 0x2001000000000000L });
	public static final BitSet FOLLOW_argList_in_lambda1177 = new BitSet(new long[] { 0x2000000000000000L });
	public static final BitSet FOLLOW_PIPE_in_lambda1181 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_expression_in_lambda1183 = new BitSet(new long[] { 0x0080000000000000L });
	public static final BitSet FOLLOW_RCURLY_in_lambda1185 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_ID_in_argList1209 = new BitSet(new long[] { 0x0004000000000002L });
	public static final BitSet FOLLOW_COMMA_in_argList1212 = new BitSet(new long[] { 0x0001000000000000L });
	public static final BitSet FOLLOW_ID_in_argList1216 = new BitSet(new long[] { 0x0004000000000002L });
	public static final BitSet FOLLOW_91_in_constructor1252 = new BitSet(new long[] { 0x0001000000000000L });
	public static final BitSet FOLLOW_qualifiedId_in_constructor1254 = new BitSet(new long[] { 0x0000000800000000L });
	public static final BitSet FOLLOW_ctorArgs_in_constructor1256 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_arrayConstructor_in_constructor1273 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_91_in_arrayConstructor1284 = new BitSet(new long[] { 0x0001000000000000L });
	public static final BitSet FOLLOW_qualifiedId_in_arrayConstructor1286 = new BitSet(
			new long[] { 0x0010000000000000L });
	public static final BitSet FOLLOW_arrayRank_in_arrayConstructor1288 = new BitSet(new long[] { 0x4000000000000002L });
	public static final BitSet FOLLOW_listInitializer_in_arrayConstructor1291 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LBRACKET_in_arrayRank1326 = new BitSet(new long[] { 0xDF7BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_expression_in_arrayRank1329 = new BitSet(new long[] { 0x0024000000000000L });
	public static final BitSet FOLLOW_COMMA_in_arrayRank1332 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_expression_in_arrayRank1334 = new BitSet(new long[] { 0x0024000000000000L });
	public static final BitSet FOLLOW_RBRACKET_in_arrayRank1340 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LCURLY_in_listInitializer1365 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_expression_in_listInitializer1367 = new BitSet(new long[] { 0x0084000000000000L });
	public static final BitSet FOLLOW_COMMA_in_listInitializer1370 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_expression_in_listInitializer1372 = new BitSet(new long[] { 0x0084000000000000L });
	public static final BitSet FOLLOW_RCURLY_in_listInitializer1376 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_POUND_in_mapInitializer1404 = new BitSet(new long[] { 0x4000000000000000L });
	public static final BitSet FOLLOW_LCURLY_in_mapInitializer1406 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_mapEntry_in_mapInitializer1408 = new BitSet(new long[] { 0x0084000000000000L });
	public static final BitSet FOLLOW_COMMA_in_mapInitializer1411 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_mapEntry_in_mapInitializer1413 = new BitSet(new long[] { 0x0084000000000000L });
	public static final BitSet FOLLOW_RCURLY_in_mapInitializer1417 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_expression_in_mapEntry1438 = new BitSet(new long[] { 0x0000000400000000L });
	public static final BitSet FOLLOW_COLON_in_mapEntry1440 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_expression_in_mapEntry1442 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LPAREN_in_ctorArgs1460 = new BitSet(new long[] { 0xDF5BA19800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_namedArgument_in_ctorArgs1464 = new BitSet(new long[] { 0x0004001000000000L });
	public static final BitSet FOLLOW_COMMA_in_ctorArgs1467 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_namedArgument_in_ctorArgs1470 = new BitSet(new long[] { 0x0004001000000000L });
	public static final BitSet FOLLOW_RPAREN_in_ctorArgs1476 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_expression_in_argument1485 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_ID_in_namedArgument1508 = new BitSet(new long[] { 0x0000000080000000L });
	public static final BitSet FOLLOW_ASSIGN_in_namedArgument1510 = new BitSet(new long[] { 0xDF5BA18800000020L,
			0x000000001800003FL });
	public static final BitSet FOLLOW_expression_in_namedArgument1512 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_argument_in_namedArgument1548 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_ID_in_qualifiedId1560 = new BitSet(new long[] { 0x0000400000000002L });
	public static final BitSet FOLLOW_DOT_in_qualifiedId1563 = new BitSet(new long[] { 0x0001000000000000L });
	public static final BitSet FOLLOW_ID_in_qualifiedId1565 = new BitSet(new long[] { 0x0000400000000002L });
	public static final BitSet FOLLOW_ID_in_contextName1584 = new BitSet(new long[] { 0x0000040000000002L });
	public static final BitSet FOLLOW_DIV_in_contextName1587 = new BitSet(new long[] { 0x0001000000000000L });
	public static final BitSet FOLLOW_ID_in_contextName1589 = new BitSet(new long[] { 0x0000040000000002L });
	public static final BitSet FOLLOW_INTEGER_LITERAL_in_literal1610 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_STRING_LITERAL_in_literal1616 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_DQ_STRING_LITERAL_in_literal1621 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_boolLiteral_in_literal1626 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_NULL_LITERAL_in_literal1631 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_HEXADECIMAL_INTEGER_LITERAL_in_literal1636 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_REAL_LITERAL_in_literal1642 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_dateLiteral_in_literal1647 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_set_in_boolLiteral0 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_92_in_dateLiteral1668 = new BitSet(new long[] { 0x0000000800000000L });
	public static final BitSet FOLLOW_LPAREN_in_dateLiteral1670 = new BitSet(new long[] { 0x8000000000000000L });
	public static final BitSet FOLLOW_STRING_LITERAL_in_dateLiteral1674 = new BitSet(new long[] { 0x0004001000000000L });
	public static final BitSet FOLLOW_COMMA_in_dateLiteral1677 = new BitSet(new long[] { 0x8000000000000000L });
	public static final BitSet FOLLOW_STRING_LITERAL_in_dateLiteral1681 = new BitSet(new long[] { 0x0000001000000000L });
	public static final BitSet FOLLOW_RPAREN_in_dateLiteral1685 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_set_in_relationalOperator0 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_POUND_in_synpred1745 = new BitSet(new long[] { 0x0001000000000000L });
	public static final BitSet FOLLOW_ID_in_synpred1747 = new BitSet(new long[] { 0x0000000800000000L });
	public static final BitSet FOLLOW_LPAREN_in_synpred1749 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_DOLLAR_in_synpred2828 = new BitSet(new long[] { 0x0001000000000000L });
	public static final BitSet FOLLOW_ID_in_synpred2830 = new BitSet(new long[] { 0x0000000800000000L });
	public static final BitSet FOLLOW_LPAREN_in_synpred2832 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_ID_in_synpred3894 = new BitSet(new long[] { 0x0000000800000000L });
	public static final BitSet FOLLOW_LPAREN_in_synpred3896 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_91_in_synpred41243 = new BitSet(new long[] { 0x0001000000000000L });
	public static final BitSet FOLLOW_qualifiedId_in_synpred41245 = new BitSet(new long[] { 0x0000000800000000L });
	public static final BitSet FOLLOW_LPAREN_in_synpred41247 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_ID_in_synpred51499 = new BitSet(new long[] { 0x0000000080000000L });
	public static final BitSet FOLLOW_ASSIGN_in_synpred51501 = new BitSet(new long[] { 0x0000000000000002L });

}