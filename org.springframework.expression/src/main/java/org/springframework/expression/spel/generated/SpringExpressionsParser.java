// $ANTLR 3.0.1 /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g 2008-05-22 13:48:03
package org.springframework.expression.spel.generated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.antlr.runtime.BitSet;
import org.antlr.runtime.EarlyExitException;
import org.antlr.runtime.MismatchedSetException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.Parser;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.antlr.runtime.tree.RewriteEarlyExitException;
import org.antlr.runtime.tree.RewriteRuleSubtreeStream;
import org.antlr.runtime.tree.RewriteRuleTokenStream;
import org.antlr.runtime.tree.TreeAdaptor;

@SuppressWarnings("unused")
public class SpringExpressionsParser extends Parser {
	public static final String[] tokenNames = new String[] { "<invalid>", "<EOR>", "<DOWN>", "<UP>", "EXPRESSIONLIST",
			"INTEGER_LITERAL", "EXPRESSION", "QUALIFIED_IDENTIFIER", "REFERENCE", "PROPERTY_OR_FIELD", "INDEXER",
			"ARGLIST", "CONSTRUCTOR", "DATE_LITERAL", "HOLDER", "CONSTRUCTOR_ARRAY", "NAMED_ARGUMENT", "FUNCTIONREF",
			"TYPEREF", "RANGE", "VARIABLEREF", "LIST_INITIALIZER", "MAP_INITIALIZER", "LOCALVAR", "LOCALFUNC",
			"MAP_ENTRY", "METHOD", "ADD", "SUBTRACT", "NUMBER", "LPAREN", "SEMI", "SEMIRPAREN", "RPAREN", "ASSIGN",
			"DEFAULT", "QMARK", "COLON", "OR", "AND", "PLUS", "MINUS", "STAR", "DIV", "MOD", "POWER", "BANG", "DOT",
			"POUND", "ID", "DOLLAR", "COMMA", "AT", "LBRACKET", "RBRACKET", "PROJECT", "RCURLY", "SELECT",
			"SELECT_FIRST", "SELECT_LAST", "TYPE", "LAMBDA", "PIPE", "LCURLY", "STRING_LITERAL", "DQ_STRING_LITERAL",
			"NULL_LITERAL", "HEXADECIMAL_INTEGER_LITERAL", "REAL_LITERAL", "TRUE", "FALSE", "DECIMAL_DIGIT",
			"INTEGER_TYPE_SUFFIX", "HEX_DIGIT", "EQUAL", "NOT_EQUAL", "LESS_THAN", "LESS_THAN_OR_EQUAL",
			"GREATER_THAN", "GREATER_THAN_OR_EQUAL", "IN", "IS", "BETWEEN", "LIKE", "MATCHES", "SOUNDSLIKE",
			"DISTANCETO", "APOS", "DOT_ESCAPED", "WS", "UPTO", "EXPONENT_PART", "REAL_TYPE_SUFFIX", "SIGN", "'new'",
			"'date'" };
	public static final int COMMA = 51;
	public static final int GREATER_THAN_OR_EQUAL = 79;
	public static final int EXPRESSIONLIST = 4;
	public static final int GREATER_THAN = 78;
	public static final int MINUS = 41;
	public static final int NUMBER = 29;
	public static final int ARGLIST = 11;
	public static final int BANG = 46;
	public static final int LESS_THAN = 76;
	public static final int METHOD = 26;
	public static final int FALSE = 70;
	public static final int PROPERTY_OR_FIELD = 9;
	public static final int INDEXER = 10;
	public static final int CONSTRUCTOR_ARRAY = 15;
	public static final int NULL_LITERAL = 66;
	public static final int NAMED_ARGUMENT = 16;
	public static final int PIPE = 62;
	public static final int DOT = 47;
	public static final int AND = 39;
	public static final int EXPRESSION = 6;
	public static final int LCURLY = 63;
	public static final int DATE_LITERAL = 13;
	public static final int REAL_TYPE_SUFFIX = 92;
	public static final int QUALIFIED_IDENTIFIER = 7;
	public static final int SELECT = 57;
	public static final int STRING_LITERAL = 64;
	public static final int SUBTRACT = 28;
	public static final int RBRACKET = 54;
	public static final int RPAREN = 33;
	public static final int BETWEEN = 82;
	public static final int SIGN = 93;
	public static final int PLUS = 40;
	public static final int INTEGER_LITERAL = 5;
	public static final int AT = 52;
	public static final int RANGE = 19;
	public static final int SOUNDSLIKE = 85;
	public static final int WS = 89;
	public static final int DOLLAR = 50;
	public static final int LESS_THAN_OR_EQUAL = 77;
	public static final int HEXADECIMAL_INTEGER_LITERAL = 67;
	public static final int LAMBDA = 61;
	public static final int SEMI = 31;
	public static final int EQUAL = 74;
	public static final int DOT_ESCAPED = 88;
	public static final int QMARK = 36;
	public static final int COLON = 37;
	public static final int PROJECT = 55;
	public static final int DIV = 43;
	public static final int REAL_LITERAL = 68;
	public static final int ADD = 27;
	public static final int TRUE = 69;
	public static final int EXPONENT_PART = 91;
	public static final int POUND = 48;
	public static final int HOLDER = 14;
	public static final int SELECT_FIRST = 58;
	public static final int TYPE = 60;
	public static final int MAP_ENTRY = 25;
	public static final int SELECT_LAST = 59;
	public static final int LBRACKET = 53;
	public static final int MOD = 44;
	public static final int FUNCTIONREF = 17;
	public static final int OR = 38;
	public static final int RCURLY = 56;
	public static final int ASSIGN = 34;
	public static final int LPAREN = 30;
	public static final int HEX_DIGIT = 73;
	public static final int LIST_INITIALIZER = 21;
	public static final int APOS = 87;
	public static final int ID = 49;
	public static final int NOT_EQUAL = 75;
	public static final int POWER = 45;
	public static final int TYPEREF = 18;
	public static final int DISTANCETO = 86;
	public static final int DECIMAL_DIGIT = 71;
	public static final int IS = 81;
	public static final int SEMIRPAREN = 32;
	public static final int DQ_STRING_LITERAL = 65;
	public static final int LIKE = 83;
	public static final int MAP_INITIALIZER = 22;
	public static final int LOCALFUNC = 24;
	public static final int IN = 80;
	public static final int CONSTRUCTOR = 12;
	public static final int INTEGER_TYPE_SUFFIX = 72;
	public static final int MATCHES = 84;
	public static final int EOF = -1;
	public static final int UPTO = 90;
	public static final int REFERENCE = 8;
	public static final int DEFAULT = 35;
	public static final int LOCALVAR = 23;
	public static final int STAR = 42;
	public static final int VARIABLEREF = 20;

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

	public String[] getTokenNames() {
		return tokenNames;
	}

	public String getGrammarFileName() {
		return "/Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g";
	}

	public static class expr_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start expr
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:56:1:
	// expr : expression EOF ;
	public final expr_return expr() throws RecognitionException {
		expr_return retval = new expr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token EOF2 = null;
		expression_return expression1 = null;

		Object EOF2_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:56:5:
			// ( expression EOF )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:56:7:
			// expression EOF
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_expression_in_expr173);
				expression1 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, expression1.getTree());
				EOF2 = (Token) input.LT(1);
				match(input, EOF, FOLLOW_EOF_in_expr175);
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
			// reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end expr

	public static class exprList_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start exprList
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:58:1:
	// exprList : LPAREN expression ( SEMI expression )+ ( SEMIRPAREN | RPAREN ) -> ^( EXPRESSIONLIST ( expression )+ )
	// ;
	public final exprList_return exprList() throws RecognitionException {
		exprList_return retval = new exprList_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LPAREN3 = null;
		Token SEMI5 = null;
		Token SEMIRPAREN7 = null;
		Token RPAREN8 = null;
		expression_return expression4 = null;

		expression_return expression6 = null;

		Object LPAREN3_tree = null;
		Object SEMI5_tree = null;
		Object SEMIRPAREN7_tree = null;
		Object RPAREN8_tree = null;
		RewriteRuleTokenStream stream_SEMI = new RewriteRuleTokenStream(adaptor, "token SEMI");
		RewriteRuleTokenStream stream_RPAREN = new RewriteRuleTokenStream(adaptor, "token RPAREN");
		RewriteRuleTokenStream stream_LPAREN = new RewriteRuleTokenStream(adaptor, "token LPAREN");
		RewriteRuleTokenStream stream_SEMIRPAREN = new RewriteRuleTokenStream(adaptor, "token SEMIRPAREN");
		RewriteRuleSubtreeStream stream_expression = new RewriteRuleSubtreeStream(adaptor, "rule expression");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:59:5:
			// ( LPAREN expression ( SEMI expression )+ ( SEMIRPAREN | RPAREN ) -> ^( EXPRESSIONLIST ( expression )+ ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:59:7:
			// LPAREN expression ( SEMI expression )+ ( SEMIRPAREN | RPAREN )
			{
				LPAREN3 = (Token) input.LT(1);
				match(input, LPAREN, FOLLOW_LPAREN_in_exprList188);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LPAREN.add(LPAREN3);

				pushFollow(FOLLOW_expression_in_exprList190);
				expression4 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_expression.add(expression4.getTree());
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:59:25:
				// ( SEMI expression )+
				int cnt1 = 0;
				loop1: do {
					int alt1 = 2;
					int LA1_0 = input.LA(1);

					if ((LA1_0 == SEMI)) {
						alt1 = 1;
					}

					switch (alt1) {
					case 1:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:59:26:
						// SEMI expression
					{
						SEMI5 = (Token) input.LT(1);
						match(input, SEMI, FOLLOW_SEMI_in_exprList193);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_SEMI.add(SEMI5);

						pushFollow(FOLLOW_expression_in_exprList195);
						expression6 = expression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_expression.add(expression6.getTree());

					}
						break;

					default:
						if (cnt1 >= 1)
							break loop1;
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						EarlyExitException eee = new EarlyExitException(1, input);
						throw eee;
					}
					cnt1++;
				} while (true);

				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:59:44:
				// ( SEMIRPAREN | RPAREN )
				int alt2 = 2;
				int LA2_0 = input.LA(1);

				if ((LA2_0 == SEMIRPAREN)) {
					alt2 = 1;
				} else if ((LA2_0 == RPAREN)) {
					alt2 = 2;
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException("59:44: ( SEMIRPAREN | RPAREN )", 2, 0, input);

					throw nvae;
				}
				switch (alt2) {
				case 1:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:59:45:
					// SEMIRPAREN
				{
					SEMIRPAREN7 = (Token) input.LT(1);
					match(input, SEMIRPAREN, FOLLOW_SEMIRPAREN_in_exprList200);
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_SEMIRPAREN.add(SEMIRPAREN7);

				}
					break;
				case 2:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:59:58:
					// RPAREN
				{
					RPAREN8 = (Token) input.LT(1);
					match(input, RPAREN, FOLLOW_RPAREN_in_exprList204);
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_RPAREN.add(RPAREN8);

				}
					break;

				}

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
					// 60:7: -> ^( EXPRESSIONLIST ( expression )+ )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:60:10:
						// ^( EXPRESSIONLIST ( expression )+ )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(EXPRESSIONLIST, "EXPRESSIONLIST"),
									root_1);

							if (!(stream_expression.hasNext())) {
								throw new RewriteEarlyExitException();
							}
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
			// reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end exprList

	public static class expression_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start expression
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:64:1:
	// expression : logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK
	// expression COLON expression ) )? ;
	public final expression_return expression() throws RecognitionException {
		expression_return retval = new expression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token ASSIGN10 = null;
		Token DEFAULT12 = null;
		Token QMARK14 = null;
		Token COLON16 = null;
		logicalOrExpression_return logicalOrExpression9 = null;

		logicalOrExpression_return logicalOrExpression11 = null;

		logicalOrExpression_return logicalOrExpression13 = null;

		expression_return expression15 = null;

		expression_return expression17 = null;

		Object ASSIGN10_tree = null;
		Object DEFAULT12_tree = null;
		Object QMARK14_tree = null;
		Object COLON16_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:64:12:
			// ( logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK
			// expression COLON expression ) )? )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:65:5:
			// logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK
			// expression COLON expression ) )?
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_logicalOrExpression_in_expression248);
				logicalOrExpression9 = logicalOrExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, logicalOrExpression9.getTree());
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:66:5:
				// ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON
				// expression ) )?
				int alt3 = 4;
				switch (input.LA(1)) {
				case ASSIGN: {
					alt3 = 1;
				}
					break;
				case DEFAULT: {
					alt3 = 2;
				}
					break;
				case QMARK: {
					alt3 = 3;
				}
					break;
				}

				switch (alt3) {
				case 1:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:66:7:
					// ( ASSIGN logicalOrExpression )
				{
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:66:7:
					// ( ASSIGN logicalOrExpression )
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:66:8:
					// ASSIGN logicalOrExpression
					{
						ASSIGN10 = (Token) input.LT(1);
						match(input, ASSIGN, FOLLOW_ASSIGN_in_expression257);
						if (failed)
							return retval;
						if (backtracking == 0) {
							ASSIGN10_tree = (Object) adaptor.create(ASSIGN10);
							root_0 = (Object) adaptor.becomeRoot(ASSIGN10_tree, root_0);
						}
						pushFollow(FOLLOW_logicalOrExpression_in_expression260);
						logicalOrExpression11 = logicalOrExpression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, logicalOrExpression11.getTree());

					}

				}
					break;
				case 2:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:67:6:
					// ( DEFAULT logicalOrExpression )
				{
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:67:6:
					// ( DEFAULT logicalOrExpression )
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:67:7:
					// DEFAULT logicalOrExpression
					{
						DEFAULT12 = (Token) input.LT(1);
						match(input, DEFAULT, FOLLOW_DEFAULT_in_expression270);
						if (failed)
							return retval;
						if (backtracking == 0) {
							DEFAULT12_tree = (Object) adaptor.create(DEFAULT12);
							root_0 = (Object) adaptor.becomeRoot(DEFAULT12_tree, root_0);
						}
						pushFollow(FOLLOW_logicalOrExpression_in_expression273);
						logicalOrExpression13 = logicalOrExpression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, logicalOrExpression13.getTree());

					}

				}
					break;
				case 3:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:68:6:
					// ( QMARK expression COLON expression )
				{
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:68:6:
					// ( QMARK expression COLON expression )
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:68:7:
					// QMARK expression COLON expression
					{
						QMARK14 = (Token) input.LT(1);
						match(input, QMARK, FOLLOW_QMARK_in_expression283);
						if (failed)
							return retval;
						if (backtracking == 0) {
							QMARK14_tree = (Object) adaptor.create(QMARK14);
							root_0 = (Object) adaptor.becomeRoot(QMARK14_tree, root_0);
						}
						pushFollow(FOLLOW_expression_in_expression286);
						expression15 = expression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, expression15.getTree());
						COLON16 = (Token) input.LT(1);
						match(input, COLON, FOLLOW_COLON_in_expression288);
						if (failed)
							return retval;
						pushFollow(FOLLOW_expression_in_expression291);
						expression17 = expression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, expression17.getTree());

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
			// reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end expression

	public static class parenExpr_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start parenExpr
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:70:1:
	// parenExpr : LPAREN expression RPAREN ;
	public final parenExpr_return parenExpr() throws RecognitionException {
		parenExpr_return retval = new parenExpr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LPAREN18 = null;
		Token RPAREN20 = null;
		expression_return expression19 = null;

		Object LPAREN18_tree = null;
		Object RPAREN20_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:70:11:
			// ( LPAREN expression RPAREN )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:70:13:
			// LPAREN expression RPAREN
			{
				root_0 = (Object) adaptor.nil();

				LPAREN18 = (Token) input.LT(1);
				match(input, LPAREN, FOLLOW_LPAREN_in_parenExpr302);
				if (failed)
					return retval;
				pushFollow(FOLLOW_expression_in_parenExpr305);
				expression19 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, expression19.getTree());
				RPAREN20 = (Token) input.LT(1);
				match(input, RPAREN, FOLLOW_RPAREN_in_parenExpr307);
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
			// reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end parenExpr

	public static class logicalOrExpression_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start logicalOrExpression
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:73:1:
	// logicalOrExpression : logicalAndExpression ( OR logicalAndExpression )* ;
	public final logicalOrExpression_return logicalOrExpression() throws RecognitionException {
		logicalOrExpression_return retval = new logicalOrExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token OR22 = null;
		logicalAndExpression_return logicalAndExpression21 = null;

		logicalAndExpression_return logicalAndExpression23 = null;

		Object OR22_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:73:21:
			// ( logicalAndExpression ( OR logicalAndExpression )* )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:73:23:
			// logicalAndExpression ( OR logicalAndExpression )*
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_logicalAndExpression_in_logicalOrExpression318);
				logicalAndExpression21 = logicalAndExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, logicalAndExpression21.getTree());
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:73:44:
				// ( OR logicalAndExpression )*
				loop4: do {
					int alt4 = 2;
					int LA4_0 = input.LA(1);

					if ((LA4_0 == OR)) {
						alt4 = 1;
					}

					switch (alt4) {
					case 1:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:73:45:
						// OR logicalAndExpression
					{
						OR22 = (Token) input.LT(1);
						match(input, OR, FOLLOW_OR_in_logicalOrExpression321);
						if (failed)
							return retval;
						if (backtracking == 0) {
							OR22_tree = (Object) adaptor.create(OR22);
							root_0 = (Object) adaptor.becomeRoot(OR22_tree, root_0);
						}
						pushFollow(FOLLOW_logicalAndExpression_in_logicalOrExpression324);
						logicalAndExpression23 = logicalAndExpression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, logicalAndExpression23.getTree());

					}
						break;

					default:
						break loop4;
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:75:1:
	// logicalAndExpression : relationalExpression ( AND relationalExpression )* ;
	public final logicalAndExpression_return logicalAndExpression() throws RecognitionException {
		logicalAndExpression_return retval = new logicalAndExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token AND25 = null;
		relationalExpression_return relationalExpression24 = null;

		relationalExpression_return relationalExpression26 = null;

		Object AND25_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:75:22:
			// ( relationalExpression ( AND relationalExpression )* )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:75:24:
			// relationalExpression ( AND relationalExpression )*
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_relationalExpression_in_logicalAndExpression358);
				relationalExpression24 = relationalExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, relationalExpression24.getTree());
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:75:45:
				// ( AND relationalExpression )*
				loop5: do {
					int alt5 = 2;
					int LA5_0 = input.LA(1);

					if ((LA5_0 == AND)) {
						alt5 = 1;
					}

					switch (alt5) {
					case 1:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:75:46:
						// AND relationalExpression
					{
						AND25 = (Token) input.LT(1);
						match(input, AND, FOLLOW_AND_in_logicalAndExpression361);
						if (failed)
							return retval;
						if (backtracking == 0) {
							AND25_tree = (Object) adaptor.create(AND25);
							root_0 = (Object) adaptor.becomeRoot(AND25_tree, root_0);
						}
						pushFollow(FOLLOW_relationalExpression_in_logicalAndExpression364);
						relationalExpression26 = relationalExpression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, relationalExpression26.getTree());

					}
						break;

					default:
						break loop5;
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:77:1:
	// relationalExpression : sumExpression ( relationalOperator sumExpression )? ;
	public final relationalExpression_return relationalExpression() throws RecognitionException {
		relationalExpression_return retval = new relationalExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		sumExpression_return sumExpression27 = null;

		relationalOperator_return relationalOperator28 = null;

		sumExpression_return sumExpression29 = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:77:22:
			// ( sumExpression ( relationalOperator sumExpression )? )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:77:24:
			// sumExpression ( relationalOperator sumExpression )?
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_sumExpression_in_relationalExpression375);
				sumExpression27 = sumExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, sumExpression27.getTree());
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:77:38:
				// ( relationalOperator sumExpression )?
				int alt6 = 2;
				int LA6_0 = input.LA(1);

				if (((LA6_0 >= EQUAL && LA6_0 <= DISTANCETO))) {
					alt6 = 1;
				}
				switch (alt6) {
				case 1:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:77:39:
					// relationalOperator sumExpression
				{
					pushFollow(FOLLOW_relationalOperator_in_relationalExpression378);
					relationalOperator28 = relationalOperator();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						root_0 = (Object) adaptor.becomeRoot(relationalOperator28.getTree(), root_0);
					pushFollow(FOLLOW_sumExpression_in_relationalExpression381);
					sumExpression29 = sumExpression();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						adaptor.addChild(root_0, sumExpression29.getTree());

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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:79:1:
	// sumExpression : productExpression ( ( PLUS | MINUS ) productExpression )* ;
	public final sumExpression_return sumExpression() throws RecognitionException {
		sumExpression_return retval = new sumExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token PLUS31 = null;
		Token MINUS32 = null;
		productExpression_return productExpression30 = null;

		productExpression_return productExpression33 = null;

		Object PLUS31_tree = null;
		Object MINUS32_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:80:2:
			// ( productExpression ( ( PLUS | MINUS ) productExpression )* )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:80:4:
			// productExpression ( ( PLUS | MINUS ) productExpression )*
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_productExpression_in_sumExpression392);
				productExpression30 = productExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, productExpression30.getTree());
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:80:22:
				// ( ( PLUS | MINUS ) productExpression )*
				loop8: do {
					int alt8 = 2;
					int LA8_0 = input.LA(1);

					if (((LA8_0 >= PLUS && LA8_0 <= MINUS))) {
						alt8 = 1;
					}

					switch (alt8) {
					case 1:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:80:24:
						// ( PLUS | MINUS ) productExpression
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:80:24:
						// ( PLUS | MINUS )
						int alt7 = 2;
						int LA7_0 = input.LA(1);

						if ((LA7_0 == PLUS)) {
							alt7 = 1;
						} else if ((LA7_0 == MINUS)) {
							alt7 = 2;
						} else {
							if (backtracking > 0) {
								failed = true;
								return retval;
							}
							NoViableAltException nvae = new NoViableAltException("80:24: ( PLUS | MINUS )", 7, 0, input);

							throw nvae;
						}
						switch (alt7) {
						case 1:
							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:80:25:
							// PLUS
						{
							PLUS31 = (Token) input.LT(1);
							match(input, PLUS, FOLLOW_PLUS_in_sumExpression397);
							if (failed)
								return retval;
							if (backtracking == 0) {
								PLUS31_tree = (Object) adaptor.create(PLUS31);
								root_0 = (Object) adaptor.becomeRoot(PLUS31_tree, root_0);
							}

						}
							break;
						case 2:
							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:80:33:
							// MINUS
						{
							MINUS32 = (Token) input.LT(1);
							match(input, MINUS, FOLLOW_MINUS_in_sumExpression402);
							if (failed)
								return retval;
							if (backtracking == 0) {
								MINUS32_tree = (Object) adaptor.create(MINUS32);
								root_0 = (Object) adaptor.becomeRoot(MINUS32_tree, root_0);
							}

						}
							break;

						}

						pushFollow(FOLLOW_productExpression_in_sumExpression406);
						productExpression33 = productExpression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, productExpression33.getTree());

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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:86:1:
	// productExpression : powerExpr ( ( STAR | DIV | MOD ) powerExpr )* ;
	public final productExpression_return productExpression() throws RecognitionException {
		productExpression_return retval = new productExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token STAR35 = null;
		Token DIV36 = null;
		Token MOD37 = null;
		powerExpr_return powerExpr34 = null;

		powerExpr_return powerExpr38 = null;

		Object STAR35_tree = null;
		Object DIV36_tree = null;
		Object MOD37_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:87:2:
			// ( powerExpr ( ( STAR | DIV | MOD ) powerExpr )* )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:87:4:
			// powerExpr ( ( STAR | DIV | MOD ) powerExpr )*
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_powerExpr_in_productExpression421);
				powerExpr34 = powerExpr();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, powerExpr34.getTree());
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:87:14:
				// ( ( STAR | DIV | MOD ) powerExpr )*
				loop10: do {
					int alt10 = 2;
					int LA10_0 = input.LA(1);

					if (((LA10_0 >= STAR && LA10_0 <= MOD))) {
						alt10 = 1;
					}

					switch (alt10) {
					case 1:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:87:15:
						// ( STAR | DIV | MOD ) powerExpr
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:87:15:
						// ( STAR | DIV | MOD )
						int alt9 = 3;
						switch (input.LA(1)) {
						case STAR: {
							alt9 = 1;
						}
							break;
						case DIV: {
							alt9 = 2;
						}
							break;
						case MOD: {
							alt9 = 3;
						}
							break;
						default:
							if (backtracking > 0) {
								failed = true;
								return retval;
							}
							NoViableAltException nvae = new NoViableAltException("87:15: ( STAR | DIV | MOD )", 9, 0,
									input);

							throw nvae;
						}

						switch (alt9) {
						case 1:
							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:87:16:
							// STAR
						{
							STAR35 = (Token) input.LT(1);
							match(input, STAR, FOLLOW_STAR_in_productExpression425);
							if (failed)
								return retval;
							if (backtracking == 0) {
								STAR35_tree = (Object) adaptor.create(STAR35);
								root_0 = (Object) adaptor.becomeRoot(STAR35_tree, root_0);
							}

						}
							break;
						case 2:
							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:87:24:
							// DIV
						{
							DIV36 = (Token) input.LT(1);
							match(input, DIV, FOLLOW_DIV_in_productExpression430);
							if (failed)
								return retval;
							if (backtracking == 0) {
								DIV36_tree = (Object) adaptor.create(DIV36);
								root_0 = (Object) adaptor.becomeRoot(DIV36_tree, root_0);
							}

						}
							break;
						case 3:
							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:87:30:
							// MOD
						{
							MOD37 = (Token) input.LT(1);
							match(input, MOD, FOLLOW_MOD_in_productExpression434);
							if (failed)
								return retval;
							if (backtracking == 0) {
								MOD37_tree = (Object) adaptor.create(MOD37);
								root_0 = (Object) adaptor.becomeRoot(MOD37_tree, root_0);
							}

						}
							break;

						}

						pushFollow(FOLLOW_powerExpr_in_productExpression438);
						powerExpr38 = powerExpr();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, powerExpr38.getTree());

					}
						break;

					default:
						break loop10;
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:93:1:
	// powerExpr : unaryExpression ( POWER unaryExpression )? ;
	public final powerExpr_return powerExpr() throws RecognitionException {
		powerExpr_return retval = new powerExpr_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token POWER40 = null;
		unaryExpression_return unaryExpression39 = null;

		unaryExpression_return unaryExpression41 = null;

		Object POWER40_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:93:12:
			// ( unaryExpression ( POWER unaryExpression )? )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:93:14:
			// unaryExpression ( POWER unaryExpression )?
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_unaryExpression_in_powerExpr454);
				unaryExpression39 = unaryExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, unaryExpression39.getTree());
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:93:30:
				// ( POWER unaryExpression )?
				int alt11 = 2;
				int LA11_0 = input.LA(1);

				if ((LA11_0 == POWER)) {
					alt11 = 1;
				}
				switch (alt11) {
				case 1:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:93:31:
					// POWER unaryExpression
				{
					POWER40 = (Token) input.LT(1);
					match(input, POWER, FOLLOW_POWER_in_powerExpr457);
					if (failed)
						return retval;
					if (backtracking == 0) {
						POWER40_tree = (Object) adaptor.create(POWER40);
						root_0 = (Object) adaptor.becomeRoot(POWER40_tree, root_0);
					}
					pushFollow(FOLLOW_unaryExpression_in_powerExpr460);
					unaryExpression41 = unaryExpression();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						adaptor.addChild(root_0, unaryExpression41.getTree());

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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:95:1:
	// unaryExpression : ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression );
	public final unaryExpression_return unaryExpression() throws RecognitionException {
		unaryExpression_return retval = new unaryExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token PLUS42 = null;
		Token MINUS43 = null;
		Token BANG44 = null;
		unaryExpression_return unaryExpression45 = null;

		primaryExpression_return primaryExpression46 = null;

		Object PLUS42_tree = null;
		Object MINUS43_tree = null;
		Object BANG44_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:96:2:
			// ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression )
			int alt13 = 2;
			int LA13_0 = input.LA(1);

			if (((LA13_0 >= PLUS && LA13_0 <= MINUS) || LA13_0 == BANG)) {
				alt13 = 1;
			} else if ((LA13_0 == INTEGER_LITERAL || LA13_0 == LPAREN || (LA13_0 >= POUND && LA13_0 <= DOLLAR)
					|| (LA13_0 >= AT && LA13_0 <= LBRACKET) || LA13_0 == PROJECT
					|| (LA13_0 >= SELECT && LA13_0 <= LAMBDA) || (LA13_0 >= LCURLY && LA13_0 <= FALSE) || (LA13_0 >= 94 && LA13_0 <= 95))) {
				alt13 = 2;
			} else {
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"95:1: unaryExpression : ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression );", 13,
						0, input);

				throw nvae;
			}
			switch (alt13) {
			case 1:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:96:4:
				// ( PLUS | MINUS | BANG ) unaryExpression
			{
				root_0 = (Object) adaptor.nil();

				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:96:4:
				// ( PLUS | MINUS | BANG )
				int alt12 = 3;
				switch (input.LA(1)) {
				case PLUS: {
					alt12 = 1;
				}
					break;
				case MINUS: {
					alt12 = 2;
				}
					break;
				case BANG: {
					alt12 = 3;
				}
					break;
				default:
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException("96:4: ( PLUS | MINUS | BANG )", 12, 0, input);

					throw nvae;
				}

				switch (alt12) {
				case 1:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:96:5:
					// PLUS
				{
					PLUS42 = (Token) input.LT(1);
					match(input, PLUS, FOLLOW_PLUS_in_unaryExpression474);
					if (failed)
						return retval;
					if (backtracking == 0) {
						PLUS42_tree = (Object) adaptor.create(PLUS42);
						root_0 = (Object) adaptor.becomeRoot(PLUS42_tree, root_0);
					}

				}
					break;
				case 2:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:96:13:
					// MINUS
				{
					MINUS43 = (Token) input.LT(1);
					match(input, MINUS, FOLLOW_MINUS_in_unaryExpression479);
					if (failed)
						return retval;
					if (backtracking == 0) {
						MINUS43_tree = (Object) adaptor.create(MINUS43);
						root_0 = (Object) adaptor.becomeRoot(MINUS43_tree, root_0);
					}

				}
					break;
				case 3:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:96:22:
					// BANG
				{
					BANG44 = (Token) input.LT(1);
					match(input, BANG, FOLLOW_BANG_in_unaryExpression484);
					if (failed)
						return retval;
					if (backtracking == 0) {
						BANG44_tree = (Object) adaptor.create(BANG44);
						root_0 = (Object) adaptor.becomeRoot(BANG44_tree, root_0);
					}

				}
					break;

				}

				pushFollow(FOLLOW_unaryExpression_in_unaryExpression488);
				unaryExpression45 = unaryExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, unaryExpression45.getTree());

			}
				break;
			case 2:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:97:4:
				// primaryExpression
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_primaryExpression_in_unaryExpression494);
				primaryExpression46 = primaryExpression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, primaryExpression46.getTree());

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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:99:1:
	// primaryExpression : startNode ( node )? -> ^( EXPRESSION startNode ( node )? ) ;
	public final primaryExpression_return primaryExpression() throws RecognitionException {
		primaryExpression_return retval = new primaryExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		startNode_return startNode47 = null;

		node_return node48 = null;

		RewriteRuleSubtreeStream stream_node = new RewriteRuleSubtreeStream(adaptor, "rule node");
		RewriteRuleSubtreeStream stream_startNode = new RewriteRuleSubtreeStream(adaptor, "rule startNode");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:100:5:
			// ( startNode ( node )? -> ^( EXPRESSION startNode ( node )? ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:100:7:
			// startNode ( node )?
			{
				pushFollow(FOLLOW_startNode_in_primaryExpression508);
				startNode47 = startNode();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_startNode.add(startNode47.getTree());
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:100:17:
				// ( node )?
				int alt14 = 2;
				int LA14_0 = input.LA(1);

				if ((LA14_0 == LPAREN || (LA14_0 >= DOT && LA14_0 <= ID) || LA14_0 == LBRACKET || LA14_0 == PROJECT || (LA14_0 >= SELECT && LA14_0 <= SELECT_LAST))) {
					alt14 = 1;
				}
				switch (alt14) {
				case 1:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:100:18:
					// node
				{
					pushFollow(FOLLOW_node_in_primaryExpression511);
					node48 = node();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_node.add(node48.getTree());

				}
					break;

				}

				// AST REWRITE
				// elements: startNode, node
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 100:25: -> ^( EXPRESSION startNode ( node )? )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:100:28:
						// ^( EXPRESSION startNode ( node )? )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(EXPRESSION, "EXPRESSION"), root_1);

							adaptor.addChild(root_1, stream_startNode.next());
							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:100:51:
							// ( node )?
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:102:1:
	// startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar |
	// localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection
	// | lastSelection | listInitializer | mapInitializer | lambda );
	public final startNode_return startNode() throws RecognitionException {
		startNode_return retval = new startNode_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		exprList_return exprList49 = null;

		parenExpr_return parenExpr50 = null;

		methodOrProperty_return methodOrProperty51 = null;

		functionOrVar_return functionOrVar52 = null;

		localFunctionOrVar_return localFunctionOrVar53 = null;

		reference_return reference54 = null;

		indexer_return indexer55 = null;

		literal_return literal56 = null;

		type_return type57 = null;

		constructor_return constructor58 = null;

		projection_return projection59 = null;

		selection_return selection60 = null;

		firstSelection_return firstSelection61 = null;

		lastSelection_return lastSelection62 = null;

		listInitializer_return listInitializer63 = null;

		mapInitializer_return mapInitializer64 = null;

		lambda_return lambda65 = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:103:5:
			// ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar |
			// localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection |
			// firstSelection | lastSelection | listInitializer | mapInitializer | lambda )
			int alt15 = 17;
			switch (input.LA(1)) {
			case LPAREN: {
				switch (input.LA(2)) {
				case PLUS: {
					int LA15_23 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 23, input);

						throw nvae;
					}
				}
					break;
				case MINUS: {
					int LA15_24 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 24, input);

						throw nvae;
					}
				}
					break;
				case BANG: {
					int LA15_25 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 25, input);

						throw nvae;
					}
				}
					break;
				case LPAREN: {
					int LA15_26 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 26, input);

						throw nvae;
					}
				}
					break;
				case ID: {
					int LA15_27 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 27, input);

						throw nvae;
					}
				}
					break;
				case POUND: {
					int LA15_28 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 28, input);

						throw nvae;
					}
				}
					break;
				case DOLLAR: {
					int LA15_29 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 29, input);

						throw nvae;
					}
				}
					break;
				case AT: {
					int LA15_30 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 30, input);

						throw nvae;
					}
				}
					break;
				case LBRACKET: {
					int LA15_31 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 31, input);

						throw nvae;
					}
				}
					break;
				case INTEGER_LITERAL: {
					int LA15_32 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 32, input);

						throw nvae;
					}
				}
					break;
				case STRING_LITERAL: {
					int LA15_33 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 33, input);

						throw nvae;
					}
				}
					break;
				case DQ_STRING_LITERAL: {
					int LA15_34 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 34, input);

						throw nvae;
					}
				}
					break;
				case TRUE:
				case FALSE: {
					int LA15_35 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 35, input);

						throw nvae;
					}
				}
					break;
				case NULL_LITERAL: {
					int LA15_36 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 36, input);

						throw nvae;
					}
				}
					break;
				case HEXADECIMAL_INTEGER_LITERAL: {
					int LA15_37 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 37, input);

						throw nvae;
					}
				}
					break;
				case REAL_LITERAL: {
					int LA15_38 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 38, input);

						throw nvae;
					}
				}
					break;
				case 95: {
					int LA15_39 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 39, input);

						throw nvae;
					}
				}
					break;
				case TYPE: {
					int LA15_40 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 40, input);

						throw nvae;
					}
				}
					break;
				case 94: {
					int LA15_41 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 41, input);

						throw nvae;
					}
				}
					break;
				case PROJECT: {
					int LA15_42 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 42, input);

						throw nvae;
					}
				}
					break;
				case SELECT: {
					int LA15_43 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 43, input);

						throw nvae;
					}
				}
					break;
				case SELECT_FIRST: {
					int LA15_44 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 44, input);

						throw nvae;
					}
				}
					break;
				case SELECT_LAST: {
					int LA15_45 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 45, input);

						throw nvae;
					}
				}
					break;
				case LCURLY: {
					int LA15_46 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 46, input);

						throw nvae;
					}
				}
					break;
				case LAMBDA: {
					int LA15_47 = input.LA(3);

					if ((synpred1())) {
						alt15 = 1;
					} else if ((true)) {
						alt15 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
								15, 47, input);

						throw nvae;
					}
				}
					break;
				default:
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException(
							"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
							15, 1, input);

					throw nvae;
				}

			}
				break;
			case ID: {
				alt15 = 3;
			}
				break;
			case POUND: {
				int LA15_3 = input.LA(2);

				if ((LA15_3 == ID)) {
					alt15 = 4;
				} else if ((LA15_3 == LCURLY)) {
					alt15 = 16;
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException(
							"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
							15, 3, input);

					throw nvae;
				}
			}
				break;
			case DOLLAR: {
				alt15 = 5;
			}
				break;
			case AT: {
				alt15 = 6;
			}
				break;
			case LBRACKET: {
				alt15 = 7;
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
			case 95: {
				alt15 = 8;
			}
				break;
			case TYPE: {
				alt15 = 9;
			}
				break;
			case 94: {
				alt15 = 10;
			}
				break;
			case PROJECT: {
				alt15 = 11;
			}
				break;
			case SELECT: {
				alt15 = 12;
			}
				break;
			case SELECT_FIRST: {
				alt15 = 13;
			}
				break;
			case SELECT_LAST: {
				alt15 = 14;
			}
				break;
			case LCURLY: {
				alt15 = 15;
			}
				break;
			case LAMBDA: {
				alt15 = 17;
			}
				break;
			default:
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"102:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );",
						15, 0, input);

				throw nvae;
			}

			switch (alt15) {
			case 1:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:104:5:
				// ( LPAREN expression SEMI )=> exprList
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_exprList_in_startNode554);
				exprList49 = exprList();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, exprList49.getTree());

			}
				break;
			case 2:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:105:7:
				// parenExpr
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_parenExpr_in_startNode563);
				parenExpr50 = parenExpr();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, parenExpr50.getTree());

			}
				break;
			case 3:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:106:7:
				// methodOrProperty
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_methodOrProperty_in_startNode571);
				methodOrProperty51 = methodOrProperty();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, methodOrProperty51.getTree());

			}
				break;
			case 4:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:107:7:
				// functionOrVar
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_functionOrVar_in_startNode580);
				functionOrVar52 = functionOrVar();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, functionOrVar52.getTree());

			}
				break;
			case 5:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:108:7:
				// localFunctionOrVar
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_localFunctionOrVar_in_startNode588);
				localFunctionOrVar53 = localFunctionOrVar();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, localFunctionOrVar53.getTree());

			}
				break;
			case 6:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:109:7:
				// reference
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_reference_in_startNode596);
				reference54 = reference();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, reference54.getTree());

			}
				break;
			case 7:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:110:7:
				// indexer
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_indexer_in_startNode604);
				indexer55 = indexer();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, indexer55.getTree());

			}
				break;
			case 8:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:111:7:
				// literal
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_literal_in_startNode612);
				literal56 = literal();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, literal56.getTree());

			}
				break;
			case 9:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:112:7:
				// type
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_type_in_startNode620);
				type57 = type();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, type57.getTree());

			}
				break;
			case 10:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:113:7:
				// constructor
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_constructor_in_startNode628);
				constructor58 = constructor();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, constructor58.getTree());

			}
				break;
			case 11:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:114:7:
				// projection
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_projection_in_startNode636);
				projection59 = projection();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, projection59.getTree());

			}
				break;
			case 12:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:115:7:
				// selection
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_selection_in_startNode645);
				selection60 = selection();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, selection60.getTree());

			}
				break;
			case 13:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:116:7:
				// firstSelection
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_firstSelection_in_startNode654);
				firstSelection61 = firstSelection();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, firstSelection61.getTree());

			}
				break;
			case 14:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:117:7:
				// lastSelection
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_lastSelection_in_startNode662);
				lastSelection62 = lastSelection();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, lastSelection62.getTree());

			}
				break;
			case 15:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:118:7:
				// listInitializer
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_listInitializer_in_startNode670);
				listInitializer63 = listInitializer();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, listInitializer63.getTree());

			}
				break;
			case 16:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:119:7:
				// mapInitializer
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_mapInitializer_in_startNode678);
				mapInitializer64 = mapInitializer();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, mapInitializer64.getTree());

			}
				break;
			case 17:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:120:7:
				// lambda
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_lambda_in_startNode686);
				lambda65 = lambda();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, lambda65.getTree());

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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:124:1:
	// node : ( methodOrProperty | functionOrVar | indexer | projection | selection | firstSelection | lastSelection |
	// exprList | DOT )+ ;
	public final node_return node() throws RecognitionException {
		node_return retval = new node_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token DOT74 = null;
		methodOrProperty_return methodOrProperty66 = null;

		functionOrVar_return functionOrVar67 = null;

		indexer_return indexer68 = null;

		projection_return projection69 = null;

		selection_return selection70 = null;

		firstSelection_return firstSelection71 = null;

		lastSelection_return lastSelection72 = null;

		exprList_return exprList73 = null;

		Object DOT74_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:124:5:
			// ( ( methodOrProperty | functionOrVar | indexer | projection | selection | firstSelection | lastSelection
			// | exprList | DOT )+ )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:125:2:
			// ( methodOrProperty | functionOrVar | indexer | projection | selection | firstSelection | lastSelection |
			// exprList | DOT )+
			{
				root_0 = (Object) adaptor.nil();

				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:125:2:
				// ( methodOrProperty | functionOrVar | indexer | projection | selection | firstSelection |
				// lastSelection | exprList | DOT )+
				int cnt16 = 0;
				loop16: do {
					int alt16 = 10;
					switch (input.LA(1)) {
					case ID: {
						alt16 = 1;
					}
						break;
					case POUND: {
						alt16 = 2;
					}
						break;
					case LBRACKET: {
						alt16 = 3;
					}
						break;
					case PROJECT: {
						alt16 = 4;
					}
						break;
					case SELECT: {
						alt16 = 5;
					}
						break;
					case SELECT_FIRST: {
						alt16 = 6;
					}
						break;
					case SELECT_LAST: {
						alt16 = 7;
					}
						break;
					case LPAREN: {
						alt16 = 8;
					}
						break;
					case DOT: {
						alt16 = 9;
					}
						break;

					}

					switch (alt16) {
					case 1:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:125:4:
						// methodOrProperty
					{
						pushFollow(FOLLOW_methodOrProperty_in_node707);
						methodOrProperty66 = methodOrProperty();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, methodOrProperty66.getTree());

					}
						break;
					case 2:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:126:4:
						// functionOrVar
					{
						pushFollow(FOLLOW_functionOrVar_in_node713);
						functionOrVar67 = functionOrVar();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, functionOrVar67.getTree());

					}
						break;
					case 3:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:127:7:
						// indexer
					{
						pushFollow(FOLLOW_indexer_in_node721);
						indexer68 = indexer();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, indexer68.getTree());

					}
						break;
					case 4:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:128:7:
						// projection
					{
						pushFollow(FOLLOW_projection_in_node729);
						projection69 = projection();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, projection69.getTree());

					}
						break;
					case 5:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:129:7:
						// selection
					{
						pushFollow(FOLLOW_selection_in_node738);
						selection70 = selection();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, selection70.getTree());

					}
						break;
					case 6:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:130:7:
						// firstSelection
					{
						pushFollow(FOLLOW_firstSelection_in_node747);
						firstSelection71 = firstSelection();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, firstSelection71.getTree());

					}
						break;
					case 7:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:131:7:
						// lastSelection
					{
						pushFollow(FOLLOW_lastSelection_in_node756);
						lastSelection72 = lastSelection();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, lastSelection72.getTree());

					}
						break;
					case 8:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:132:7:
						// exprList
					{
						pushFollow(FOLLOW_exprList_in_node765);
						exprList73 = exprList();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							adaptor.addChild(root_0, exprList73.getTree());

					}
						break;
					case 9:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:133:7:
						// DOT
					{
						DOT74 = (Token) input.LT(1);
						match(input, DOT, FOLLOW_DOT_in_node773);
						if (failed)
							return retval;
						if (backtracking == 0) {
							DOT74_tree = (Object) adaptor.create(DOT74);
							adaptor.addChild(root_0, DOT74_tree);
						}

					}
						break;

					default:
						if (cnt16 >= 1)
							break loop16;
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						EarlyExitException eee = new EarlyExitException(16, input);
						throw eee;
					}
					cnt16++;
				} while (true);

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			// reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end node

	public static class functionOrVar_return extends ParserRuleReturnScope {
		Object tree;

		public Object getTree() {
			return tree;
		}
	};

	// $ANTLR start functionOrVar
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:137:1:
	// functionOrVar : ( ( POUND ID LPAREN )=> function | var );
	public final functionOrVar_return functionOrVar() throws RecognitionException {
		functionOrVar_return retval = new functionOrVar_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		function_return function75 = null;

		var_return var76 = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:138:5:
			// ( ( POUND ID LPAREN )=> function | var )
			int alt17 = 2;
			int LA17_0 = input.LA(1);

			if ((LA17_0 == POUND)) {
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
								"137:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 17, 2, input);

						throw nvae;
					}
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException(
							"137:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 17, 1, input);

					throw nvae;
				}
			} else {
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"137:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 17, 0, input);

				throw nvae;
			}
			switch (alt17) {
			case 1:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:138:7:
				// ( POUND ID LPAREN )=> function
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_function_in_functionOrVar806);
				function75 = function();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, function75.getTree());

			}
				break;
			case 2:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:139:7:
				// var
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_var_in_functionOrVar814);
				var76 = var();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, var76.getTree());

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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:142:1:
	// function : POUND id= ID methodArgs -> ^( FUNCTIONREF[$id] methodArgs ) ;
	public final function_return function() throws RecognitionException {
		function_return retval = new function_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token id = null;
		Token POUND77 = null;
		methodArgs_return methodArgs78 = null;

		Object id_tree = null;
		Object POUND77_tree = null;
		RewriteRuleTokenStream stream_POUND = new RewriteRuleTokenStream(adaptor, "token POUND");
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");
		RewriteRuleSubtreeStream stream_methodArgs = new RewriteRuleSubtreeStream(adaptor, "rule methodArgs");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:142:10:
			// ( POUND id= ID methodArgs -> ^( FUNCTIONREF[$id] methodArgs ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:142:12:
			// POUND id= ID methodArgs
			{
				POUND77 = (Token) input.LT(1);
				match(input, POUND, FOLLOW_POUND_in_function831);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_POUND.add(POUND77);

				id = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_function835);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(id);

				pushFollow(FOLLOW_methodArgs_in_function837);
				methodArgs78 = methodArgs();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_methodArgs.add(methodArgs78.getTree());

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
					// 142:35: -> ^( FUNCTIONREF[$id] methodArgs )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:142:38:
						// ^( FUNCTIONREF[$id] methodArgs )
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:144:1:
	// var : POUND id= ID -> ^( VARIABLEREF[$id] ) ;
	public final var_return var() throws RecognitionException {
		var_return retval = new var_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token id = null;
		Token POUND79 = null;

		Object id_tree = null;
		Object POUND79_tree = null;
		RewriteRuleTokenStream stream_POUND = new RewriteRuleTokenStream(adaptor, "token POUND");
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:144:5:
			// ( POUND id= ID -> ^( VARIABLEREF[$id] ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:144:7:
			// POUND id= ID
			{
				POUND79 = (Token) input.LT(1);
				match(input, POUND, FOLLOW_POUND_in_var858);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_POUND.add(POUND79);

				id = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_var862);
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
					// 144:19: -> ^( VARIABLEREF[$id] )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:144:22:
						// ^( VARIABLEREF[$id] )
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:146:1:
	// localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );
	public final localFunctionOrVar_return localFunctionOrVar() throws RecognitionException {
		localFunctionOrVar_return retval = new localFunctionOrVar_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		localFunction_return localFunction80 = null;

		localVar_return localVar81 = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:147:2:
			// ( ( DOLLAR ID LPAREN )=> localFunction | localVar )
			int alt18 = 2;
			int LA18_0 = input.LA(1);

			if ((LA18_0 == DOLLAR)) {
				int LA18_1 = input.LA(2);

				if ((LA18_1 == ID)) {
					int LA18_2 = input.LA(3);

					if ((synpred3())) {
						alt18 = 1;
					} else if ((true)) {
						alt18 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"146:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );", 18,
								2, input);

						throw nvae;
					}
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException(
							"146:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );", 18, 1,
							input);

					throw nvae;
				}
			} else {
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"146:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );", 18, 0,
						input);

				throw nvae;
			}
			switch (alt18) {
			case 1:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:147:4:
				// ( DOLLAR ID LPAREN )=> localFunction
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_localFunction_in_localFunctionOrVar889);
				localFunction80 = localFunction();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, localFunction80.getTree());

			}
				break;
			case 2:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:148:4:
				// localVar
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_localVar_in_localFunctionOrVar894);
				localVar81 = localVar();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, localVar81.getTree());

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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:151:1:
	// localFunction : DOLLAR id= ID methodArgs -> ^( LOCALFUNC[$id] methodArgs ) ;
	public final localFunction_return localFunction() throws RecognitionException {
		localFunction_return retval = new localFunction_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token id = null;
		Token DOLLAR82 = null;
		methodArgs_return methodArgs83 = null;

		Object id_tree = null;
		Object DOLLAR82_tree = null;
		RewriteRuleTokenStream stream_DOLLAR = new RewriteRuleTokenStream(adaptor, "token DOLLAR");
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");
		RewriteRuleSubtreeStream stream_methodArgs = new RewriteRuleSubtreeStream(adaptor, "rule methodArgs");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:151:15:
			// ( DOLLAR id= ID methodArgs -> ^( LOCALFUNC[$id] methodArgs ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:151:17:
			// DOLLAR id= ID methodArgs
			{
				DOLLAR82 = (Token) input.LT(1);
				match(input, DOLLAR, FOLLOW_DOLLAR_in_localFunction904);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_DOLLAR.add(DOLLAR82);

				id = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_localFunction908);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(id);

				pushFollow(FOLLOW_methodArgs_in_localFunction910);
				methodArgs83 = methodArgs();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_methodArgs.add(methodArgs83.getTree());

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
					// 151:41: -> ^( LOCALFUNC[$id] methodArgs )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:151:44:
						// ^( LOCALFUNC[$id] methodArgs )
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:152:1:
	// localVar : DOLLAR id= ID -> ^( LOCALVAR[$id] ) ;
	public final localVar_return localVar() throws RecognitionException {
		localVar_return retval = new localVar_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token id = null;
		Token DOLLAR84 = null;

		Object id_tree = null;
		Object DOLLAR84_tree = null;
		RewriteRuleTokenStream stream_DOLLAR = new RewriteRuleTokenStream(adaptor, "token DOLLAR");
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:152:9:
			// ( DOLLAR id= ID -> ^( LOCALVAR[$id] ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:152:11:
			// DOLLAR id= ID
			{
				DOLLAR84 = (Token) input.LT(1);
				match(input, DOLLAR, FOLLOW_DOLLAR_in_localVar925);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_DOLLAR.add(DOLLAR84);

				id = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_localVar929);
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
					// 152:24: -> ^( LOCALVAR[$id] )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:152:27:
						// ^( LOCALVAR[$id] )
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:154:1:
	// methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );
	public final methodOrProperty_return methodOrProperty() throws RecognitionException {
		methodOrProperty_return retval = new methodOrProperty_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token id = null;
		methodArgs_return methodArgs85 = null;

		property_return property86 = null;

		Object id_tree = null;
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");
		RewriteRuleSubtreeStream stream_methodArgs = new RewriteRuleSubtreeStream(adaptor, "rule methodArgs");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:155:2:
			// ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property )
			int alt19 = 2;
			int LA19_0 = input.LA(1);

			if ((LA19_0 == ID)) {
				int LA19_1 = input.LA(2);

				if ((LA19_1 == LPAREN)) {
					int LA19_2 = input.LA(3);

					if ((synpred4())) {
						alt19 = 1;
					} else if ((true)) {
						alt19 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"154:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );",
								19, 2, input);

						throw nvae;
					}
				} else if ((LA19_1 == EOF || (LA19_1 >= SEMI && LA19_1 <= POWER) || (LA19_1 >= DOT && LA19_1 <= ID)
						|| LA19_1 == COMMA || (LA19_1 >= LBRACKET && LA19_1 <= SELECT_LAST) || (LA19_1 >= EQUAL && LA19_1 <= DISTANCETO))) {
					alt19 = 2;
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException(
							"154:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );",
							19, 1, input);

					throw nvae;
				}
			} else {
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"154:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );",
						19, 0, input);

				throw nvae;
			}
			switch (alt19) {
			case 1:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:155:4:
				// ( ID LPAREN )=>id= ID methodArgs
			{
				id = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_methodOrProperty955);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(id);

				pushFollow(FOLLOW_methodArgs_in_methodOrProperty957);
				methodArgs85 = methodArgs();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_methodArgs.add(methodArgs85.getTree());

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
					// 155:36: -> ^( METHOD[$id] methodArgs )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:155:39:
						// ^( METHOD[$id] methodArgs )
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
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:156:4:
				// property
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_property_in_methodOrProperty971);
				property86 = property();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, property86.getTree());

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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:162:1:
	// methodArgs : LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN ;
	public final methodArgs_return methodArgs() throws RecognitionException {
		methodArgs_return retval = new methodArgs_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LPAREN87 = null;
		Token COMMA89 = null;
		Token COMMA91 = null;
		Token RPAREN92 = null;
		argument_return argument88 = null;

		argument_return argument90 = null;

		Object LPAREN87_tree = null;
		Object COMMA89_tree = null;
		Object COMMA91_tree = null;
		Object RPAREN92_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:162:12:
			// ( LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:162:15:
			// LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN
			{
				root_0 = (Object) adaptor.nil();

				LPAREN87 = (Token) input.LT(1);
				match(input, LPAREN, FOLLOW_LPAREN_in_methodArgs986);
				if (failed)
					return retval;
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:162:23:
				// ( argument ( COMMA argument )* ( COMMA )? )?
				int alt22 = 2;
				int LA22_0 = input.LA(1);

				if ((LA22_0 == INTEGER_LITERAL || LA22_0 == LPAREN || (LA22_0 >= PLUS && LA22_0 <= MINUS)
						|| LA22_0 == BANG || (LA22_0 >= POUND && LA22_0 <= DOLLAR)
						|| (LA22_0 >= AT && LA22_0 <= LBRACKET) || LA22_0 == PROJECT
						|| (LA22_0 >= SELECT && LA22_0 <= LAMBDA) || (LA22_0 >= LCURLY && LA22_0 <= FALSE) || (LA22_0 >= 94 && LA22_0 <= 95))) {
					alt22 = 1;
				}
				switch (alt22) {
				case 1:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:162:24:
					// argument ( COMMA argument )* ( COMMA )?
				{
					pushFollow(FOLLOW_argument_in_methodArgs990);
					argument88 = argument();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						adaptor.addChild(root_0, argument88.getTree());
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:162:33:
					// ( COMMA argument )*
					loop20: do {
						int alt20 = 2;
						int LA20_0 = input.LA(1);

						if ((LA20_0 == COMMA)) {
							int LA20_1 = input.LA(2);

							if ((LA20_1 == INTEGER_LITERAL || LA20_1 == LPAREN || (LA20_1 >= PLUS && LA20_1 <= MINUS)
									|| LA20_1 == BANG || (LA20_1 >= POUND && LA20_1 <= DOLLAR)
									|| (LA20_1 >= AT && LA20_1 <= LBRACKET) || LA20_1 == PROJECT
									|| (LA20_1 >= SELECT && LA20_1 <= LAMBDA) || (LA20_1 >= LCURLY && LA20_1 <= FALSE) || (LA20_1 >= 94 && LA20_1 <= 95))) {
								alt20 = 1;
							}

						}

						switch (alt20) {
						case 1:
							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:162:34:
							// COMMA argument
						{
							COMMA89 = (Token) input.LT(1);
							match(input, COMMA, FOLLOW_COMMA_in_methodArgs993);
							if (failed)
								return retval;
							pushFollow(FOLLOW_argument_in_methodArgs996);
							argument90 = argument();
							_fsp--;
							if (failed)
								return retval;
							if (backtracking == 0)
								adaptor.addChild(root_0, argument90.getTree());

						}
							break;

						default:
							break loop20;
						}
					} while (true);

					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:162:52:
					// ( COMMA )?
					int alt21 = 2;
					int LA21_0 = input.LA(1);

					if ((LA21_0 == COMMA)) {
						alt21 = 1;
					}
					switch (alt21) {
					case 1:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:162:53:
						// COMMA
					{
						COMMA91 = (Token) input.LT(1);
						match(input, COMMA, FOLLOW_COMMA_in_methodArgs1001);
						if (failed)
							return retval;

					}
						break;

					}

				}
					break;

				}

				RPAREN92 = (Token) input.LT(1);
				match(input, RPAREN, FOLLOW_RPAREN_in_methodArgs1008);
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:167:1:
	// property : id= ID -> ^( PROPERTY_OR_FIELD[$id] ) ;
	public final property_return property() throws RecognitionException {
		property_return retval = new property_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token id = null;

		Object id_tree = null;
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:167:9:
			// (id= ID -> ^( PROPERTY_OR_FIELD[$id] ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:167:11:
			// id= ID
			{
				id = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_property1021);
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
					// 167:17: -> ^( PROPERTY_OR_FIELD[$id] )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:167:20:
						// ^( PROPERTY_OR_FIELD[$id] )
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:174:1:
	// reference : AT pos= LPAREN (cn= contextName COLON )? (q= qualifiedId )? RPAREN -> ^( REFERENCE[$pos] ( $cn COLON
	// )? ( $q)? RPAREN ) ;
	public final reference_return reference() throws RecognitionException {
		reference_return retval = new reference_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token pos = null;
		Token AT93 = null;
		Token COLON94 = null;
		Token RPAREN95 = null;
		contextName_return cn = null;

		qualifiedId_return q = null;

		Object pos_tree = null;
		Object AT93_tree = null;
		Object COLON94_tree = null;
		Object RPAREN95_tree = null;
		RewriteRuleTokenStream stream_RPAREN = new RewriteRuleTokenStream(adaptor, "token RPAREN");
		RewriteRuleTokenStream stream_COLON = new RewriteRuleTokenStream(adaptor, "token COLON");
		RewriteRuleTokenStream stream_LPAREN = new RewriteRuleTokenStream(adaptor, "token LPAREN");
		RewriteRuleTokenStream stream_AT = new RewriteRuleTokenStream(adaptor, "token AT");
		RewriteRuleSubtreeStream stream_contextName = new RewriteRuleSubtreeStream(adaptor, "rule contextName");
		RewriteRuleSubtreeStream stream_qualifiedId = new RewriteRuleSubtreeStream(adaptor, "rule qualifiedId");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:175:2:
			// ( AT pos= LPAREN (cn= contextName COLON )? (q= qualifiedId )? RPAREN -> ^( REFERENCE[$pos] ( $cn COLON )?
			// ( $q)? RPAREN ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:175:5:
			// AT pos= LPAREN (cn= contextName COLON )? (q= qualifiedId )? RPAREN
			{
				AT93 = (Token) input.LT(1);
				match(input, AT, FOLLOW_AT_in_reference1043);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_AT.add(AT93);

				pos = (Token) input.LT(1);
				match(input, LPAREN, FOLLOW_LPAREN_in_reference1047);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LPAREN.add(pos);

				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:175:19:
				// (cn= contextName COLON )?
				int alt23 = 2;
				int LA23_0 = input.LA(1);

				if ((LA23_0 == ID)) {
					int LA23_1 = input.LA(2);

					if ((LA23_1 == COLON || LA23_1 == DIV)) {
						alt23 = 1;
					}
				}
				switch (alt23) {
				case 1:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:175:20:
					// cn= contextName COLON
				{
					pushFollow(FOLLOW_contextName_in_reference1052);
					cn = contextName();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_contextName.add(cn.getTree());
					COLON94 = (Token) input.LT(1);
					match(input, COLON, FOLLOW_COLON_in_reference1054);
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_COLON.add(COLON94);

				}
					break;

				}

				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:175:43:
				// (q= qualifiedId )?
				int alt24 = 2;
				int LA24_0 = input.LA(1);

				if ((LA24_0 == ID)) {
					alt24 = 1;
				}
				switch (alt24) {
				case 1:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:175:44:
					// q= qualifiedId
				{
					pushFollow(FOLLOW_qualifiedId_in_reference1061);
					q = qualifiedId();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_qualifiedId.add(q.getTree());

				}
					break;

				}

				RPAREN95 = (Token) input.LT(1);
				match(input, RPAREN, FOLLOW_RPAREN_in_reference1065);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RPAREN.add(RPAREN95);

				// AST REWRITE
				// elements: COLON, cn, q, RPAREN
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
					// 176:4: -> ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:176:7:
						// ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(REFERENCE, pos), root_1);

							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:176:25:
							// ( $cn COLON )?
							if (stream_COLON.hasNext() || stream_cn.hasNext()) {
								adaptor.addChild(root_1, stream_cn.next());
								adaptor.addChild(root_1, stream_COLON.next());

							}
							stream_COLON.reset();
							stream_cn.reset();
							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:176:38:
							// ( $q)?
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:182:1:
	// indexer : LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET -> ^( INDEXER $r1 ( $r2)* ) ;
	public final indexer_return indexer() throws RecognitionException {
		indexer_return retval = new indexer_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LBRACKET96 = null;
		Token COMMA97 = null;
		Token RBRACKET98 = null;
		argument_return r1 = null;

		argument_return r2 = null;

		Object LBRACKET96_tree = null;
		Object COMMA97_tree = null;
		Object RBRACKET98_tree = null;
		RewriteRuleTokenStream stream_COMMA = new RewriteRuleTokenStream(adaptor, "token COMMA");
		RewriteRuleTokenStream stream_LBRACKET = new RewriteRuleTokenStream(adaptor, "token LBRACKET");
		RewriteRuleTokenStream stream_RBRACKET = new RewriteRuleTokenStream(adaptor, "token RBRACKET");
		RewriteRuleSubtreeStream stream_argument = new RewriteRuleSubtreeStream(adaptor, "rule argument");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:182:8:
			// ( LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET -> ^( INDEXER $r1 ( $r2)* ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:182:10:
			// LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET
			{
				LBRACKET96 = (Token) input.LT(1);
				match(input, LBRACKET, FOLLOW_LBRACKET_in_indexer1100);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LBRACKET.add(LBRACKET96);

				pushFollow(FOLLOW_argument_in_indexer1104);
				r1 = argument();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_argument.add(r1.getTree());
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:182:31:
				// ( COMMA r2= argument )*
				loop25: do {
					int alt25 = 2;
					int LA25_0 = input.LA(1);

					if ((LA25_0 == COMMA)) {
						alt25 = 1;
					}

					switch (alt25) {
					case 1:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:182:32:
						// COMMA r2= argument
					{
						COMMA97 = (Token) input.LT(1);
						match(input, COMMA, FOLLOW_COMMA_in_indexer1107);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_COMMA.add(COMMA97);

						pushFollow(FOLLOW_argument_in_indexer1111);
						r2 = argument();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_argument.add(r2.getTree());

					}
						break;

					default:
						break loop25;
					}
				} while (true);

				RBRACKET98 = (Token) input.LT(1);
				match(input, RBRACKET, FOLLOW_RBRACKET_in_indexer1115);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RBRACKET.add(RBRACKET98);

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
					// 182:61: -> ^( INDEXER $r1 ( $r2)* )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:182:64:
						// ^( INDEXER $r1 ( $r2)* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(INDEXER, "INDEXER"), root_1);

							adaptor.addChild(root_1, stream_r1.next());
							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:182:78:
							// ( $r2)*
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:187:1:
	// projection : PROJECT expression RCURLY ;
	public final projection_return projection() throws RecognitionException {
		projection_return retval = new projection_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token PROJECT99 = null;
		Token RCURLY101 = null;
		expression_return expression100 = null;

		Object PROJECT99_tree = null;
		Object RCURLY101_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:187:11:
			// ( PROJECT expression RCURLY )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:187:13:
			// PROJECT expression RCURLY
			{
				root_0 = (Object) adaptor.nil();

				PROJECT99 = (Token) input.LT(1);
				match(input, PROJECT, FOLLOW_PROJECT_in_projection1142);
				if (failed)
					return retval;
				if (backtracking == 0) {
					PROJECT99_tree = (Object) adaptor.create(PROJECT99);
					root_0 = (Object) adaptor.becomeRoot(PROJECT99_tree, root_0);
				}
				pushFollow(FOLLOW_expression_in_projection1145);
				expression100 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, expression100.getTree());
				RCURLY101 = (Token) input.LT(1);
				match(input, RCURLY, FOLLOW_RCURLY_in_projection1147);
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:189:1:
	// selection : SELECT expression RCURLY ;
	public final selection_return selection() throws RecognitionException {
		selection_return retval = new selection_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token SELECT102 = null;
		Token RCURLY104 = null;
		expression_return expression103 = null;

		Object SELECT102_tree = null;
		Object RCURLY104_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:189:10:
			// ( SELECT expression RCURLY )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:189:12:
			// SELECT expression RCURLY
			{
				root_0 = (Object) adaptor.nil();

				SELECT102 = (Token) input.LT(1);
				match(input, SELECT, FOLLOW_SELECT_in_selection1155);
				if (failed)
					return retval;
				if (backtracking == 0) {
					SELECT102_tree = (Object) adaptor.create(SELECT102);
					root_0 = (Object) adaptor.becomeRoot(SELECT102_tree, root_0);
				}
				pushFollow(FOLLOW_expression_in_selection1158);
				expression103 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, expression103.getTree());
				RCURLY104 = (Token) input.LT(1);
				match(input, RCURLY, FOLLOW_RCURLY_in_selection1160);
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:191:1:
	// firstSelection : SELECT_FIRST expression RCURLY ;
	public final firstSelection_return firstSelection() throws RecognitionException {
		firstSelection_return retval = new firstSelection_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token SELECT_FIRST105 = null;
		Token RCURLY107 = null;
		expression_return expression106 = null;

		Object SELECT_FIRST105_tree = null;
		Object RCURLY107_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:191:15:
			// ( SELECT_FIRST expression RCURLY )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:191:17:
			// SELECT_FIRST expression RCURLY
			{
				root_0 = (Object) adaptor.nil();

				SELECT_FIRST105 = (Token) input.LT(1);
				match(input, SELECT_FIRST, FOLLOW_SELECT_FIRST_in_firstSelection1168);
				if (failed)
					return retval;
				if (backtracking == 0) {
					SELECT_FIRST105_tree = (Object) adaptor.create(SELECT_FIRST105);
					root_0 = (Object) adaptor.becomeRoot(SELECT_FIRST105_tree, root_0);
				}
				pushFollow(FOLLOW_expression_in_firstSelection1171);
				expression106 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, expression106.getTree());
				RCURLY107 = (Token) input.LT(1);
				match(input, RCURLY, FOLLOW_RCURLY_in_firstSelection1173);
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:193:1:
	// lastSelection : SELECT_LAST expression RCURLY ;
	public final lastSelection_return lastSelection() throws RecognitionException {
		lastSelection_return retval = new lastSelection_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token SELECT_LAST108 = null;
		Token RCURLY110 = null;
		expression_return expression109 = null;

		Object SELECT_LAST108_tree = null;
		Object RCURLY110_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:193:14:
			// ( SELECT_LAST expression RCURLY )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:193:16:
			// SELECT_LAST expression RCURLY
			{
				root_0 = (Object) adaptor.nil();

				SELECT_LAST108 = (Token) input.LT(1);
				match(input, SELECT_LAST, FOLLOW_SELECT_LAST_in_lastSelection1181);
				if (failed)
					return retval;
				if (backtracking == 0) {
					SELECT_LAST108_tree = (Object) adaptor.create(SELECT_LAST108);
					root_0 = (Object) adaptor.becomeRoot(SELECT_LAST108_tree, root_0);
				}
				pushFollow(FOLLOW_expression_in_lastSelection1184);
				expression109 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, expression109.getTree());
				RCURLY110 = (Token) input.LT(1);
				match(input, RCURLY, FOLLOW_RCURLY_in_lastSelection1186);
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:196:1:
	// type : TYPE qualifiedId RPAREN -> ^( TYPEREF qualifiedId ) ;
	public final type_return type() throws RecognitionException {
		type_return retval = new type_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token TYPE111 = null;
		Token RPAREN113 = null;
		qualifiedId_return qualifiedId112 = null;

		Object TYPE111_tree = null;
		Object RPAREN113_tree = null;
		RewriteRuleTokenStream stream_RPAREN = new RewriteRuleTokenStream(adaptor, "token RPAREN");
		RewriteRuleTokenStream stream_TYPE = new RewriteRuleTokenStream(adaptor, "token TYPE");
		RewriteRuleSubtreeStream stream_qualifiedId = new RewriteRuleSubtreeStream(adaptor, "rule qualifiedId");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:196:5:
			// ( TYPE qualifiedId RPAREN -> ^( TYPEREF qualifiedId ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:196:7:
			// TYPE qualifiedId RPAREN
			{
				TYPE111 = (Token) input.LT(1);
				match(input, TYPE, FOLLOW_TYPE_in_type1195);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_TYPE.add(TYPE111);

				pushFollow(FOLLOW_qualifiedId_in_type1197);
				qualifiedId112 = qualifiedId();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_qualifiedId.add(qualifiedId112.getTree());
				RPAREN113 = (Token) input.LT(1);
				match(input, RPAREN, FOLLOW_RPAREN_in_type1199);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RPAREN.add(RPAREN113);

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
					// 196:31: -> ^( TYPEREF qualifiedId )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:196:34:
						// ^( TYPEREF qualifiedId )
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:204:1:
	// lambda : LAMBDA ( argList )? PIPE expression RCURLY -> ^( LAMBDA ( argList )? expression ) ;
	public final lambda_return lambda() throws RecognitionException {
		lambda_return retval = new lambda_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LAMBDA114 = null;
		Token PIPE116 = null;
		Token RCURLY118 = null;
		argList_return argList115 = null;

		expression_return expression117 = null;

		Object LAMBDA114_tree = null;
		Object PIPE116_tree = null;
		Object RCURLY118_tree = null;
		RewriteRuleTokenStream stream_RCURLY = new RewriteRuleTokenStream(adaptor, "token RCURLY");
		RewriteRuleTokenStream stream_PIPE = new RewriteRuleTokenStream(adaptor, "token PIPE");
		RewriteRuleTokenStream stream_LAMBDA = new RewriteRuleTokenStream(adaptor, "token LAMBDA");
		RewriteRuleSubtreeStream stream_argList = new RewriteRuleSubtreeStream(adaptor, "rule argList");
		RewriteRuleSubtreeStream stream_expression = new RewriteRuleSubtreeStream(adaptor, "rule expression");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:205:4:
			// ( LAMBDA ( argList )? PIPE expression RCURLY -> ^( LAMBDA ( argList )? expression ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:205:8:
			// LAMBDA ( argList )? PIPE expression RCURLY
			{
				LAMBDA114 = (Token) input.LT(1);
				match(input, LAMBDA, FOLLOW_LAMBDA_in_lambda1226);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LAMBDA.add(LAMBDA114);

				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:205:15:
				// ( argList )?
				int alt26 = 2;
				int LA26_0 = input.LA(1);

				if ((LA26_0 == ID)) {
					alt26 = 1;
				}
				switch (alt26) {
				case 1:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:205:16:
					// argList
				{
					pushFollow(FOLLOW_argList_in_lambda1229);
					argList115 = argList();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_argList.add(argList115.getTree());

				}
					break;

				}

				PIPE116 = (Token) input.LT(1);
				match(input, PIPE, FOLLOW_PIPE_in_lambda1233);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_PIPE.add(PIPE116);

				pushFollow(FOLLOW_expression_in_lambda1235);
				expression117 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_expression.add(expression117.getTree());
				RCURLY118 = (Token) input.LT(1);
				match(input, RCURLY, FOLLOW_RCURLY_in_lambda1237);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RCURLY.add(RCURLY118);

				// AST REWRITE
				// elements: argList, expression, LAMBDA
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 205:49: -> ^( LAMBDA ( argList )? expression )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:205:52:
						// ^( LAMBDA ( argList )? expression )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(stream_LAMBDA.next(), root_1);

							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:205:61:
							// ( argList )?
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:207:1:
	// argList : (id+= ID ( COMMA id+= ID )* ) -> ^( ARGLIST ( $id)* ) ;
	public final argList_return argList() throws RecognitionException {
		argList_return retval = new argList_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token COMMA119 = null;
		Token id = null;
		List list_id = null;

		Object COMMA119_tree = null;
		Object id_tree = null;
		RewriteRuleTokenStream stream_COMMA = new RewriteRuleTokenStream(adaptor, "token COMMA");
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:207:9:
			// ( (id+= ID ( COMMA id+= ID )* ) -> ^( ARGLIST ( $id)* ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:207:11:
			// (id+= ID ( COMMA id+= ID )* )
			{
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:207:11:
				// (id+= ID ( COMMA id+= ID )* )
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:207:12:
				// id+= ID ( COMMA id+= ID )*
				{
					id = (Token) input.LT(1);
					match(input, ID, FOLLOW_ID_in_argList1261);
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_ID.add(id);

					if (list_id == null)
						list_id = new ArrayList();
					list_id.add(id);

					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:207:19:
					// ( COMMA id+= ID )*
					loop27: do {
						int alt27 = 2;
						int LA27_0 = input.LA(1);

						if ((LA27_0 == COMMA)) {
							alt27 = 1;
						}

						switch (alt27) {
						case 1:
							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:207:20:
							// COMMA id+= ID
						{
							COMMA119 = (Token) input.LT(1);
							match(input, COMMA, FOLLOW_COMMA_in_argList1264);
							if (failed)
								return retval;
							if (backtracking == 0)
								stream_COMMA.add(COMMA119);

							id = (Token) input.LT(1);
							match(input, ID, FOLLOW_ID_in_argList1268);
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
							break loop27;
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
					// 207:36: -> ^( ARGLIST ( $id)* )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:207:39:
						// ^( ARGLIST ( $id)* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(ARGLIST, "ARGLIST"), root_1);

							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:207:49:
							// ( $id)*
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:209:1:
	// constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs
	// ) | arrayConstructor );
	public final constructor_return constructor() throws RecognitionException {
		constructor_return retval = new constructor_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token string_literal120 = null;
		qualifiedId_return qualifiedId121 = null;

		ctorArgs_return ctorArgs122 = null;

		arrayConstructor_return arrayConstructor123 = null;

		Object string_literal120_tree = null;
		RewriteRuleTokenStream stream_94 = new RewriteRuleTokenStream(adaptor, "token 94");
		RewriteRuleSubtreeStream stream_qualifiedId = new RewriteRuleSubtreeStream(adaptor, "rule qualifiedId");
		RewriteRuleSubtreeStream stream_ctorArgs = new RewriteRuleSubtreeStream(adaptor, "rule ctorArgs");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:210:2:
			// ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) |
			// arrayConstructor )
			int alt28 = 2;
			int LA28_0 = input.LA(1);

			if ((LA28_0 == 94)) {
				int LA28_1 = input.LA(2);

				if ((LA28_1 == ID)) {
					int LA28_2 = input.LA(3);

					if ((synpred5())) {
						alt28 = 1;
					} else if ((true)) {
						alt28 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"209:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );",
								28, 2, input);

						throw nvae;
					}
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException(
							"209:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );",
							28, 1, input);

					throw nvae;
				}
			} else {
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"209:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );",
						28, 0, input);

				throw nvae;
			}
			switch (alt28) {
			case 1:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:210:4:
				// ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs
			{
				string_literal120 = (Token) input.LT(1);
				match(input, 94, FOLLOW_94_in_constructor1304);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_94.add(string_literal120);

				pushFollow(FOLLOW_qualifiedId_in_constructor1306);
				qualifiedId121 = qualifiedId();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_qualifiedId.add(qualifiedId121.getTree());
				pushFollow(FOLLOW_ctorArgs_in_constructor1308);
				ctorArgs122 = ctorArgs();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ctorArgs.add(ctorArgs122.getTree());

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
					// 210:61: -> ^( CONSTRUCTOR qualifiedId ctorArgs )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:210:64:
						// ^( CONSTRUCTOR qualifiedId ctorArgs )
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
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:211:6:
				// arrayConstructor
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_arrayConstructor_in_constructor1325);
				arrayConstructor123 = arrayConstructor();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, arrayConstructor123.getTree());

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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:214:1:
	// arrayConstructor : 'new' qualifiedId arrayRank ( listInitializer )? -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank
	// ( listInitializer )? ) ;
	public final arrayConstructor_return arrayConstructor() throws RecognitionException {
		arrayConstructor_return retval = new arrayConstructor_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token string_literal124 = null;
		qualifiedId_return qualifiedId125 = null;

		arrayRank_return arrayRank126 = null;

		listInitializer_return listInitializer127 = null;

		Object string_literal124_tree = null;
		RewriteRuleTokenStream stream_94 = new RewriteRuleTokenStream(adaptor, "token 94");
		RewriteRuleSubtreeStream stream_listInitializer = new RewriteRuleSubtreeStream(adaptor, "rule listInitializer");
		RewriteRuleSubtreeStream stream_qualifiedId = new RewriteRuleSubtreeStream(adaptor, "rule qualifiedId");
		RewriteRuleSubtreeStream stream_arrayRank = new RewriteRuleSubtreeStream(adaptor, "rule arrayRank");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:215:2:
			// ( 'new' qualifiedId arrayRank ( listInitializer )? -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank (
			// listInitializer )? ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:215:4:
			// 'new' qualifiedId arrayRank ( listInitializer )?
			{
				string_literal124 = (Token) input.LT(1);
				match(input, 94, FOLLOW_94_in_arrayConstructor1336);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_94.add(string_literal124);

				pushFollow(FOLLOW_qualifiedId_in_arrayConstructor1338);
				qualifiedId125 = qualifiedId();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_qualifiedId.add(qualifiedId125.getTree());
				pushFollow(FOLLOW_arrayRank_in_arrayConstructor1340);
				arrayRank126 = arrayRank();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_arrayRank.add(arrayRank126.getTree());
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:215:32:
				// ( listInitializer )?
				int alt29 = 2;
				int LA29_0 = input.LA(1);

				if ((LA29_0 == LCURLY)) {
					alt29 = 1;
				}
				switch (alt29) {
				case 1:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:215:33:
					// listInitializer
				{
					pushFollow(FOLLOW_listInitializer_in_arrayConstructor1343);
					listInitializer127 = listInitializer();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_listInitializer.add(listInitializer127.getTree());

				}
					break;

				}

				// AST REWRITE
				// elements: qualifiedId, arrayRank, listInitializer
				// token labels:
				// rule labels: retval
				// token list labels:
				// rule list labels:
				if (backtracking == 0) {
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "token retval",
							retval != null ? retval.tree : null);

					root_0 = (Object) adaptor.nil();
					// 216:4: -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:216:7:
						// ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(
									adaptor.create(CONSTRUCTOR_ARRAY, "CONSTRUCTOR_ARRAY"), root_1);

							adaptor.addChild(root_1, stream_qualifiedId.next());
							adaptor.addChild(root_1, stream_arrayRank.next());
							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:216:49:
							// ( listInitializer )?
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:219:1:
	// arrayRank : LBRACKET ( expression ( COMMA expression )* )? RBRACKET -> ^( EXPRESSIONLIST ( expression )* ) ;
	public final arrayRank_return arrayRank() throws RecognitionException {
		arrayRank_return retval = new arrayRank_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LBRACKET128 = null;
		Token COMMA130 = null;
		Token RBRACKET132 = null;
		expression_return expression129 = null;

		expression_return expression131 = null;

		Object LBRACKET128_tree = null;
		Object COMMA130_tree = null;
		Object RBRACKET132_tree = null;
		RewriteRuleTokenStream stream_COMMA = new RewriteRuleTokenStream(adaptor, "token COMMA");
		RewriteRuleTokenStream stream_LBRACKET = new RewriteRuleTokenStream(adaptor, "token LBRACKET");
		RewriteRuleTokenStream stream_RBRACKET = new RewriteRuleTokenStream(adaptor, "token RBRACKET");
		RewriteRuleSubtreeStream stream_expression = new RewriteRuleSubtreeStream(adaptor, "rule expression");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:220:5:
			// ( LBRACKET ( expression ( COMMA expression )* )? RBRACKET -> ^( EXPRESSIONLIST ( expression )* ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:220:7:
			// LBRACKET ( expression ( COMMA expression )* )? RBRACKET
			{
				LBRACKET128 = (Token) input.LT(1);
				match(input, LBRACKET, FOLLOW_LBRACKET_in_arrayRank1378);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LBRACKET.add(LBRACKET128);

				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:220:16:
				// ( expression ( COMMA expression )* )?
				int alt31 = 2;
				int LA31_0 = input.LA(1);

				if ((LA31_0 == INTEGER_LITERAL || LA31_0 == LPAREN || (LA31_0 >= PLUS && LA31_0 <= MINUS)
						|| LA31_0 == BANG || (LA31_0 >= POUND && LA31_0 <= DOLLAR)
						|| (LA31_0 >= AT && LA31_0 <= LBRACKET) || LA31_0 == PROJECT
						|| (LA31_0 >= SELECT && LA31_0 <= LAMBDA) || (LA31_0 >= LCURLY && LA31_0 <= FALSE) || (LA31_0 >= 94 && LA31_0 <= 95))) {
					alt31 = 1;
				}
				switch (alt31) {
				case 1:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:220:17:
					// expression ( COMMA expression )*
				{
					pushFollow(FOLLOW_expression_in_arrayRank1381);
					expression129 = expression();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_expression.add(expression129.getTree());
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:220:28:
					// ( COMMA expression )*
					loop30: do {
						int alt30 = 2;
						int LA30_0 = input.LA(1);

						if ((LA30_0 == COMMA)) {
							alt30 = 1;
						}

						switch (alt30) {
						case 1:
							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:220:29:
							// COMMA expression
						{
							COMMA130 = (Token) input.LT(1);
							match(input, COMMA, FOLLOW_COMMA_in_arrayRank1384);
							if (failed)
								return retval;
							if (backtracking == 0)
								stream_COMMA.add(COMMA130);

							pushFollow(FOLLOW_expression_in_arrayRank1386);
							expression131 = expression();
							_fsp--;
							if (failed)
								return retval;
							if (backtracking == 0)
								stream_expression.add(expression131.getTree());

						}
							break;

						default:
							break loop30;
						}
					} while (true);

				}
					break;

				}

				RBRACKET132 = (Token) input.LT(1);
				match(input, RBRACKET, FOLLOW_RBRACKET_in_arrayRank1392);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RBRACKET.add(RBRACKET132);

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
					// 220:59: -> ^( EXPRESSIONLIST ( expression )* )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:220:62:
						// ^( EXPRESSIONLIST ( expression )* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(EXPRESSIONLIST, "EXPRESSIONLIST"),
									root_1);

							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:220:79:
							// ( expression )*
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:222:1:
	// listInitializer : LCURLY expression ( COMMA expression )* RCURLY -> ^( LIST_INITIALIZER ( expression )* ) ;
	public final listInitializer_return listInitializer() throws RecognitionException {
		listInitializer_return retval = new listInitializer_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LCURLY133 = null;
		Token COMMA135 = null;
		Token RCURLY137 = null;
		expression_return expression134 = null;

		expression_return expression136 = null;

		Object LCURLY133_tree = null;
		Object COMMA135_tree = null;
		Object RCURLY137_tree = null;
		RewriteRuleTokenStream stream_COMMA = new RewriteRuleTokenStream(adaptor, "token COMMA");
		RewriteRuleTokenStream stream_RCURLY = new RewriteRuleTokenStream(adaptor, "token RCURLY");
		RewriteRuleTokenStream stream_LCURLY = new RewriteRuleTokenStream(adaptor, "token LCURLY");
		RewriteRuleSubtreeStream stream_expression = new RewriteRuleSubtreeStream(adaptor, "rule expression");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:223:5:
			// ( LCURLY expression ( COMMA expression )* RCURLY -> ^( LIST_INITIALIZER ( expression )* ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:223:7:
			// LCURLY expression ( COMMA expression )* RCURLY
			{
				LCURLY133 = (Token) input.LT(1);
				match(input, LCURLY, FOLLOW_LCURLY_in_listInitializer1417);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LCURLY.add(LCURLY133);

				pushFollow(FOLLOW_expression_in_listInitializer1419);
				expression134 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_expression.add(expression134.getTree());
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:223:25:
				// ( COMMA expression )*
				loop32: do {
					int alt32 = 2;
					int LA32_0 = input.LA(1);

					if ((LA32_0 == COMMA)) {
						alt32 = 1;
					}

					switch (alt32) {
					case 1:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:223:26:
						// COMMA expression
					{
						COMMA135 = (Token) input.LT(1);
						match(input, COMMA, FOLLOW_COMMA_in_listInitializer1422);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_COMMA.add(COMMA135);

						pushFollow(FOLLOW_expression_in_listInitializer1424);
						expression136 = expression();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_expression.add(expression136.getTree());

					}
						break;

					default:
						break loop32;
					}
				} while (true);

				RCURLY137 = (Token) input.LT(1);
				match(input, RCURLY, FOLLOW_RCURLY_in_listInitializer1428);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RCURLY.add(RCURLY137);

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
					// 223:52: -> ^( LIST_INITIALIZER ( expression )* )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:223:55:
						// ^( LIST_INITIALIZER ( expression )* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(LIST_INITIALIZER, "LIST_INITIALIZER"),
									root_1);

							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:223:74:
							// ( expression )*
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:228:1:
	// mapInitializer : POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY -> ^( MAP_INITIALIZER ( mapEntry )* ) ;
	public final mapInitializer_return mapInitializer() throws RecognitionException {
		mapInitializer_return retval = new mapInitializer_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token POUND138 = null;
		Token LCURLY139 = null;
		Token COMMA141 = null;
		Token RCURLY143 = null;
		mapEntry_return mapEntry140 = null;

		mapEntry_return mapEntry142 = null;

		Object POUND138_tree = null;
		Object LCURLY139_tree = null;
		Object COMMA141_tree = null;
		Object RCURLY143_tree = null;
		RewriteRuleTokenStream stream_COMMA = new RewriteRuleTokenStream(adaptor, "token COMMA");
		RewriteRuleTokenStream stream_RCURLY = new RewriteRuleTokenStream(adaptor, "token RCURLY");
		RewriteRuleTokenStream stream_LCURLY = new RewriteRuleTokenStream(adaptor, "token LCURLY");
		RewriteRuleTokenStream stream_POUND = new RewriteRuleTokenStream(adaptor, "token POUND");
		RewriteRuleSubtreeStream stream_mapEntry = new RewriteRuleSubtreeStream(adaptor, "rule mapEntry");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:229:5:
			// ( POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY -> ^( MAP_INITIALIZER ( mapEntry )* ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:229:7:
			// POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY
			{
				POUND138 = (Token) input.LT(1);
				match(input, POUND, FOLLOW_POUND_in_mapInitializer1456);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_POUND.add(POUND138);

				LCURLY139 = (Token) input.LT(1);
				match(input, LCURLY, FOLLOW_LCURLY_in_mapInitializer1458);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LCURLY.add(LCURLY139);

				pushFollow(FOLLOW_mapEntry_in_mapInitializer1460);
				mapEntry140 = mapEntry();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_mapEntry.add(mapEntry140.getTree());
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:229:29:
				// ( COMMA mapEntry )*
				loop33: do {
					int alt33 = 2;
					int LA33_0 = input.LA(1);

					if ((LA33_0 == COMMA)) {
						alt33 = 1;
					}

					switch (alt33) {
					case 1:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:229:30:
						// COMMA mapEntry
					{
						COMMA141 = (Token) input.LT(1);
						match(input, COMMA, FOLLOW_COMMA_in_mapInitializer1463);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_COMMA.add(COMMA141);

						pushFollow(FOLLOW_mapEntry_in_mapInitializer1465);
						mapEntry142 = mapEntry();
						_fsp--;
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_mapEntry.add(mapEntry142.getTree());

					}
						break;

					default:
						break loop33;
					}
				} while (true);

				RCURLY143 = (Token) input.LT(1);
				match(input, RCURLY, FOLLOW_RCURLY_in_mapInitializer1469);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RCURLY.add(RCURLY143);

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
					// 229:54: -> ^( MAP_INITIALIZER ( mapEntry )* )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:229:57:
						// ^( MAP_INITIALIZER ( mapEntry )* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(MAP_INITIALIZER, "MAP_INITIALIZER"),
									root_1);

							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:229:75:
							// ( mapEntry )*
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:231:1:
	// mapEntry : expression COLON expression -> ^( MAP_ENTRY ( expression )* ) ;
	public final mapEntry_return mapEntry() throws RecognitionException {
		mapEntry_return retval = new mapEntry_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token COLON145 = null;
		expression_return expression144 = null;

		expression_return expression146 = null;

		Object COLON145_tree = null;
		RewriteRuleTokenStream stream_COLON = new RewriteRuleTokenStream(adaptor, "token COLON");
		RewriteRuleSubtreeStream stream_expression = new RewriteRuleSubtreeStream(adaptor, "rule expression");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:232:5:
			// ( expression COLON expression -> ^( MAP_ENTRY ( expression )* ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:232:7:
			// expression COLON expression
			{
				pushFollow(FOLLOW_expression_in_mapEntry1490);
				expression144 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_expression.add(expression144.getTree());
				COLON145 = (Token) input.LT(1);
				match(input, COLON, FOLLOW_COLON_in_mapEntry1492);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_COLON.add(COLON145);

				pushFollow(FOLLOW_expression_in_mapEntry1494);
				expression146 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_expression.add(expression146.getTree());

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
					// 232:35: -> ^( MAP_ENTRY ( expression )* )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:232:38:
						// ^( MAP_ENTRY ( expression )* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(MAP_ENTRY, "MAP_ENTRY"), root_1);

							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:232:50:
							// ( expression )*
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:234:1:
	// ctorArgs : LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN ;
	public final ctorArgs_return ctorArgs() throws RecognitionException {
		ctorArgs_return retval = new ctorArgs_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token LPAREN147 = null;
		Token COMMA149 = null;
		Token RPAREN151 = null;
		namedArgument_return namedArgument148 = null;

		namedArgument_return namedArgument150 = null;

		Object LPAREN147_tree = null;
		Object COMMA149_tree = null;
		Object RPAREN151_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:235:2:
			// ( LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:235:4:
			// LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN
			{
				root_0 = (Object) adaptor.nil();

				LPAREN147 = (Token) input.LT(1);
				match(input, LPAREN, FOLLOW_LPAREN_in_ctorArgs1512);
				if (failed)
					return retval;
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:235:12:
				// ( namedArgument ( COMMA namedArgument )* )?
				int alt35 = 2;
				int LA35_0 = input.LA(1);

				if ((LA35_0 == INTEGER_LITERAL || LA35_0 == LPAREN || (LA35_0 >= PLUS && LA35_0 <= MINUS)
						|| LA35_0 == BANG || (LA35_0 >= POUND && LA35_0 <= DOLLAR)
						|| (LA35_0 >= AT && LA35_0 <= LBRACKET) || LA35_0 == PROJECT
						|| (LA35_0 >= SELECT && LA35_0 <= LAMBDA) || (LA35_0 >= LCURLY && LA35_0 <= FALSE) || (LA35_0 >= 94 && LA35_0 <= 95))) {
					alt35 = 1;
				}
				switch (alt35) {
				case 1:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:235:13:
					// namedArgument ( COMMA namedArgument )*
				{
					pushFollow(FOLLOW_namedArgument_in_ctorArgs1516);
					namedArgument148 = namedArgument();
					_fsp--;
					if (failed)
						return retval;
					if (backtracking == 0)
						adaptor.addChild(root_0, namedArgument148.getTree());
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:235:27:
					// ( COMMA namedArgument )*
					loop34: do {
						int alt34 = 2;
						int LA34_0 = input.LA(1);

						if ((LA34_0 == COMMA)) {
							alt34 = 1;
						}

						switch (alt34) {
						case 1:
							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:235:28:
							// COMMA namedArgument
						{
							COMMA149 = (Token) input.LT(1);
							match(input, COMMA, FOLLOW_COMMA_in_ctorArgs1519);
							if (failed)
								return retval;
							pushFollow(FOLLOW_namedArgument_in_ctorArgs1522);
							namedArgument150 = namedArgument();
							_fsp--;
							if (failed)
								return retval;
							if (backtracking == 0)
								adaptor.addChild(root_0, namedArgument150.getTree());

						}
							break;

						default:
							break loop34;
						}
					} while (true);

				}
					break;

				}

				RPAREN151 = (Token) input.LT(1);
				match(input, RPAREN, FOLLOW_RPAREN_in_ctorArgs1528);
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:237:1:
	// argument : expression ;
	public final argument_return argument() throws RecognitionException {
		argument_return retval = new argument_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		expression_return expression152 = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:237:10:
			// ( expression )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:237:12:
			// expression
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_expression_in_argument1537);
				expression152 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, expression152.getTree());

			}

			retval.stop = input.LT(-1);

			if (backtracking == 0) {
				retval.tree = (Object) adaptor.rulePostProcessing(root_0);
				adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
			}
		}

		catch (RecognitionException e) {
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:239:1:
	// namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );
	public final namedArgument_return namedArgument() throws RecognitionException {
		namedArgument_return retval = new namedArgument_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token id = null;
		Token ASSIGN153 = null;
		expression_return expression154 = null;

		argument_return argument155 = null;

		Object id_tree = null;
		Object ASSIGN153_tree = null;
		RewriteRuleTokenStream stream_ASSIGN = new RewriteRuleTokenStream(adaptor, "token ASSIGN");
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");
		RewriteRuleSubtreeStream stream_expression = new RewriteRuleSubtreeStream(adaptor, "rule expression");
		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:240:5:
			// ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument )
			int alt36 = 2;
			int LA36_0 = input.LA(1);

			if ((LA36_0 == ID)) {
				int LA36_1 = input.LA(2);

				if ((LA36_1 == ASSIGN)) {
					int LA36_26 = input.LA(3);

					if ((synpred6())) {
						alt36 = 1;
					} else if ((true)) {
						alt36 = 2;
					} else {
						if (backtracking > 0) {
							failed = true;
							return retval;
						}
						NoViableAltException nvae = new NoViableAltException(
								"239:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );",
								36, 26, input);

						throw nvae;
					}
				} else if ((LA36_1 == LPAREN || LA36_1 == RPAREN || (LA36_1 >= DEFAULT && LA36_1 <= QMARK)
						|| (LA36_1 >= OR && LA36_1 <= POWER) || (LA36_1 >= DOT && LA36_1 <= ID) || LA36_1 == COMMA
						|| LA36_1 == LBRACKET || LA36_1 == PROJECT || (LA36_1 >= SELECT && LA36_1 <= SELECT_LAST) || (LA36_1 >= EQUAL && LA36_1 <= DISTANCETO))) {
					alt36 = 2;
				} else {
					if (backtracking > 0) {
						failed = true;
						return retval;
					}
					NoViableAltException nvae = new NoViableAltException(
							"239:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );",
							36, 1, input);

					throw nvae;
				}
			} else if ((LA36_0 == INTEGER_LITERAL || LA36_0 == LPAREN || (LA36_0 >= PLUS && LA36_0 <= MINUS)
					|| LA36_0 == BANG || LA36_0 == POUND || LA36_0 == DOLLAR || (LA36_0 >= AT && LA36_0 <= LBRACKET)
					|| LA36_0 == PROJECT || (LA36_0 >= SELECT && LA36_0 <= LAMBDA)
					|| (LA36_0 >= LCURLY && LA36_0 <= FALSE) || (LA36_0 >= 94 && LA36_0 <= 95))) {
				alt36 = 2;
			} else {
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"239:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );",
						36, 0, input);

				throw nvae;
			}
			switch (alt36) {
			case 1:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:240:7:
				// ( ID ASSIGN )=>id= ID ASSIGN expression
			{
				id = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_namedArgument1560);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(id);

				ASSIGN153 = (Token) input.LT(1);
				match(input, ASSIGN, FOLLOW_ASSIGN_in_namedArgument1562);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ASSIGN.add(ASSIGN153);

				pushFollow(FOLLOW_expression_in_namedArgument1564);
				expression154 = expression();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_expression.add(expression154.getTree());

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
					// 241:19: -> ^( NAMED_ARGUMENT[$id] expression )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:241:22:
						// ^( NAMED_ARGUMENT[$id] expression )
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
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:242:7:
				// argument
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_argument_in_namedArgument1600);
				argument155 = argument();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, argument155.getTree());

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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:244:1:
	// qualifiedId : ID ( DOT ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) ;
	public final qualifiedId_return qualifiedId() throws RecognitionException {
		qualifiedId_return retval = new qualifiedId_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token ID156 = null;
		Token DOT157 = null;
		Token ID158 = null;

		Object ID156_tree = null;
		Object DOT157_tree = null;
		Object ID158_tree = null;
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");
		RewriteRuleTokenStream stream_DOT = new RewriteRuleTokenStream(adaptor, "token DOT");

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:244:13:
			// ( ID ( DOT ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:244:15:
			// ID ( DOT ID )*
			{
				ID156 = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_qualifiedId1612);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(ID156);

				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:244:18:
				// ( DOT ID )*
				loop37: do {
					int alt37 = 2;
					int LA37_0 = input.LA(1);

					if ((LA37_0 == DOT)) {
						alt37 = 1;
					}

					switch (alt37) {
					case 1:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:244:19:
						// DOT ID
					{
						DOT157 = (Token) input.LT(1);
						match(input, DOT, FOLLOW_DOT_in_qualifiedId1615);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_DOT.add(DOT157);

						ID158 = (Token) input.LT(1);
						match(input, ID, FOLLOW_ID_in_qualifiedId1617);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_ID.add(ID158);

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
					// 244:28: -> ^( QUALIFIED_IDENTIFIER ( ID )* )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:244:31:
						// ^( QUALIFIED_IDENTIFIER ( ID )* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(QUALIFIED_IDENTIFIER,
									"QUALIFIED_IDENTIFIER"), root_1);

							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:244:54:
							// ( ID )*
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:246:1:
	// contextName : ID ( DIV ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) ;
	public final contextName_return contextName() throws RecognitionException {
		contextName_return retval = new contextName_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token ID159 = null;
		Token DIV160 = null;
		Token ID161 = null;

		Object ID159_tree = null;
		Object DIV160_tree = null;
		Object ID161_tree = null;
		RewriteRuleTokenStream stream_DIV = new RewriteRuleTokenStream(adaptor, "token DIV");
		RewriteRuleTokenStream stream_ID = new RewriteRuleTokenStream(adaptor, "token ID");

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:246:13:
			// ( ID ( DIV ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:246:15:
			// ID ( DIV ID )*
			{
				ID159 = (Token) input.LT(1);
				match(input, ID, FOLLOW_ID_in_contextName1636);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_ID.add(ID159);

				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:246:18:
				// ( DIV ID )*
				loop38: do {
					int alt38 = 2;
					int LA38_0 = input.LA(1);

					if ((LA38_0 == DIV)) {
						alt38 = 1;
					}

					switch (alt38) {
					case 1:
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:246:19:
						// DIV ID
					{
						DIV160 = (Token) input.LT(1);
						match(input, DIV, FOLLOW_DIV_in_contextName1639);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_DIV.add(DIV160);

						ID161 = (Token) input.LT(1);
						match(input, ID, FOLLOW_ID_in_contextName1641);
						if (failed)
							return retval;
						if (backtracking == 0)
							stream_ID.add(ID161);

					}
						break;

					default:
						break loop38;
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
					// 246:28: -> ^( QUALIFIED_IDENTIFIER ( ID )* )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:246:31:
						// ^( QUALIFIED_IDENTIFIER ( ID )* )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(QUALIFIED_IDENTIFIER,
									"QUALIFIED_IDENTIFIER"), root_1);

							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:246:54:
							// ( ID )*
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:248:1:
	// literal : ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL |
	// HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL | dateLiteral );
	public final literal_return literal() throws RecognitionException {
		literal_return retval = new literal_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token INTEGER_LITERAL162 = null;
		Token STRING_LITERAL163 = null;
		Token DQ_STRING_LITERAL164 = null;
		Token NULL_LITERAL166 = null;
		Token HEXADECIMAL_INTEGER_LITERAL167 = null;
		Token REAL_LITERAL168 = null;
		boolLiteral_return boolLiteral165 = null;

		dateLiteral_return dateLiteral169 = null;

		Object INTEGER_LITERAL162_tree = null;
		Object STRING_LITERAL163_tree = null;
		Object DQ_STRING_LITERAL164_tree = null;
		Object NULL_LITERAL166_tree = null;
		Object HEXADECIMAL_INTEGER_LITERAL167_tree = null;
		Object REAL_LITERAL168_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:249:2:
			// ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL |
			// HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL | dateLiteral )
			int alt39 = 8;
			switch (input.LA(1)) {
			case INTEGER_LITERAL: {
				alt39 = 1;
			}
				break;
			case STRING_LITERAL: {
				alt39 = 2;
			}
				break;
			case DQ_STRING_LITERAL: {
				alt39 = 3;
			}
				break;
			case TRUE:
			case FALSE: {
				alt39 = 4;
			}
				break;
			case NULL_LITERAL: {
				alt39 = 5;
			}
				break;
			case HEXADECIMAL_INTEGER_LITERAL: {
				alt39 = 6;
			}
				break;
			case REAL_LITERAL: {
				alt39 = 7;
			}
				break;
			case 95: {
				alt39 = 8;
			}
				break;
			default:
				if (backtracking > 0) {
					failed = true;
					return retval;
				}
				NoViableAltException nvae = new NoViableAltException(
						"248:1: literal : ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL | dateLiteral );",
						39, 0, input);

				throw nvae;
			}

			switch (alt39) {
			case 1:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:249:4:
				// INTEGER_LITERAL
			{
				root_0 = (Object) adaptor.nil();

				INTEGER_LITERAL162 = (Token) input.LT(1);
				match(input, INTEGER_LITERAL, FOLLOW_INTEGER_LITERAL_in_literal1662);
				if (failed)
					return retval;
				if (backtracking == 0) {
					INTEGER_LITERAL162_tree = (Object) adaptor.create(INTEGER_LITERAL162);
					adaptor.addChild(root_0, INTEGER_LITERAL162_tree);
				}

			}
				break;
			case 2:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:250:4:
				// STRING_LITERAL
			{
				root_0 = (Object) adaptor.nil();

				STRING_LITERAL163 = (Token) input.LT(1);
				match(input, STRING_LITERAL, FOLLOW_STRING_LITERAL_in_literal1668);
				if (failed)
					return retval;
				if (backtracking == 0) {
					STRING_LITERAL163_tree = (Object) adaptor.create(STRING_LITERAL163);
					adaptor.addChild(root_0, STRING_LITERAL163_tree);
				}

			}
				break;
			case 3:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:251:4:
				// DQ_STRING_LITERAL
			{
				root_0 = (Object) adaptor.nil();

				DQ_STRING_LITERAL164 = (Token) input.LT(1);
				match(input, DQ_STRING_LITERAL, FOLLOW_DQ_STRING_LITERAL_in_literal1673);
				if (failed)
					return retval;
				if (backtracking == 0) {
					DQ_STRING_LITERAL164_tree = (Object) adaptor.create(DQ_STRING_LITERAL164);
					adaptor.addChild(root_0, DQ_STRING_LITERAL164_tree);
				}

			}
				break;
			case 4:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:252:4:
				// boolLiteral
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_boolLiteral_in_literal1678);
				boolLiteral165 = boolLiteral();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, boolLiteral165.getTree());

			}
				break;
			case 5:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:253:4:
				// NULL_LITERAL
			{
				root_0 = (Object) adaptor.nil();

				NULL_LITERAL166 = (Token) input.LT(1);
				match(input, NULL_LITERAL, FOLLOW_NULL_LITERAL_in_literal1683);
				if (failed)
					return retval;
				if (backtracking == 0) {
					NULL_LITERAL166_tree = (Object) adaptor.create(NULL_LITERAL166);
					adaptor.addChild(root_0, NULL_LITERAL166_tree);
				}

			}
				break;
			case 6:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:254:4:
				// HEXADECIMAL_INTEGER_LITERAL
			{
				root_0 = (Object) adaptor.nil();

				HEXADECIMAL_INTEGER_LITERAL167 = (Token) input.LT(1);
				match(input, HEXADECIMAL_INTEGER_LITERAL, FOLLOW_HEXADECIMAL_INTEGER_LITERAL_in_literal1688);
				if (failed)
					return retval;
				if (backtracking == 0) {
					HEXADECIMAL_INTEGER_LITERAL167_tree = (Object) adaptor.create(HEXADECIMAL_INTEGER_LITERAL167);
					adaptor.addChild(root_0, HEXADECIMAL_INTEGER_LITERAL167_tree);
				}

			}
				break;
			case 7:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:255:4:
				// REAL_LITERAL
			{
				root_0 = (Object) adaptor.nil();

				REAL_LITERAL168 = (Token) input.LT(1);
				match(input, REAL_LITERAL, FOLLOW_REAL_LITERAL_in_literal1694);
				if (failed)
					return retval;
				if (backtracking == 0) {
					REAL_LITERAL168_tree = (Object) adaptor.create(REAL_LITERAL168);
					adaptor.addChild(root_0, REAL_LITERAL168_tree);
				}

			}
				break;
			case 8:
				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:256:4:
				// dateLiteral
			{
				root_0 = (Object) adaptor.nil();

				pushFollow(FOLLOW_dateLiteral_in_literal1699);
				dateLiteral169 = dateLiteral();
				_fsp--;
				if (failed)
					return retval;
				if (backtracking == 0)
					adaptor.addChild(root_0, dateLiteral169.getTree());

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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:259:1:
	// boolLiteral : ( TRUE | FALSE );
	public final boolLiteral_return boolLiteral() throws RecognitionException {
		boolLiteral_return retval = new boolLiteral_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token set170 = null;

		Object set170_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:259:12:
			// ( TRUE | FALSE )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:
			{
				root_0 = (Object) adaptor.nil();

				set170 = (Token) input.LT(1);
				if ((input.LA(1) >= TRUE && input.LA(1) <= FALSE)) {
					input.consume();
					if (backtracking == 0)
						adaptor.addChild(root_0, adaptor.create(set170));
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:261:1:
	// dateLiteral : 'date' LPAREN d= STRING_LITERAL ( COMMA f= STRING_LITERAL )? RPAREN -> ^( DATE_LITERAL $d ( $f)? )
	// ;
	public final dateLiteral_return dateLiteral() throws RecognitionException {
		dateLiteral_return retval = new dateLiteral_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token d = null;
		Token f = null;
		Token string_literal171 = null;
		Token LPAREN172 = null;
		Token COMMA173 = null;
		Token RPAREN174 = null;

		Object d_tree = null;
		Object f_tree = null;
		Object string_literal171_tree = null;
		Object LPAREN172_tree = null;
		Object COMMA173_tree = null;
		Object RPAREN174_tree = null;
		RewriteRuleTokenStream stream_COMMA = new RewriteRuleTokenStream(adaptor, "token COMMA");
		RewriteRuleTokenStream stream_RPAREN = new RewriteRuleTokenStream(adaptor, "token RPAREN");
		RewriteRuleTokenStream stream_LPAREN = new RewriteRuleTokenStream(adaptor, "token LPAREN");
		RewriteRuleTokenStream stream_95 = new RewriteRuleTokenStream(adaptor, "token 95");
		RewriteRuleTokenStream stream_STRING_LITERAL = new RewriteRuleTokenStream(adaptor, "token STRING_LITERAL");

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:261:12:
			// ( 'date' LPAREN d= STRING_LITERAL ( COMMA f= STRING_LITERAL )? RPAREN -> ^( DATE_LITERAL $d ( $f)? ) )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:261:14:
			// 'date' LPAREN d= STRING_LITERAL ( COMMA f= STRING_LITERAL )? RPAREN
			{
				string_literal171 = (Token) input.LT(1);
				match(input, 95, FOLLOW_95_in_dateLiteral1720);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_95.add(string_literal171);

				LPAREN172 = (Token) input.LT(1);
				match(input, LPAREN, FOLLOW_LPAREN_in_dateLiteral1722);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_LPAREN.add(LPAREN172);

				d = (Token) input.LT(1);
				match(input, STRING_LITERAL, FOLLOW_STRING_LITERAL_in_dateLiteral1726);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_STRING_LITERAL.add(d);

				// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:261:45:
				// ( COMMA f= STRING_LITERAL )?
				int alt40 = 2;
				int LA40_0 = input.LA(1);

				if ((LA40_0 == COMMA)) {
					alt40 = 1;
				}
				switch (alt40) {
				case 1:
					// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:261:46:
					// COMMA f= STRING_LITERAL
				{
					COMMA173 = (Token) input.LT(1);
					match(input, COMMA, FOLLOW_COMMA_in_dateLiteral1729);
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_COMMA.add(COMMA173);

					f = (Token) input.LT(1);
					match(input, STRING_LITERAL, FOLLOW_STRING_LITERAL_in_dateLiteral1733);
					if (failed)
						return retval;
					if (backtracking == 0)
						stream_STRING_LITERAL.add(f);

				}
					break;

				}

				RPAREN174 = (Token) input.LT(1);
				match(input, RPAREN, FOLLOW_RPAREN_in_dateLiteral1737);
				if (failed)
					return retval;
				if (backtracking == 0)
					stream_RPAREN.add(RPAREN174);

				// AST REWRITE
				// elements: f, d
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
					// 261:78: -> ^( DATE_LITERAL $d ( $f)? )
					{
						// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:261:81:
						// ^( DATE_LITERAL $d ( $f)? )
						{
							Object root_1 = (Object) adaptor.nil();
							root_1 = (Object) adaptor.becomeRoot(adaptor.create(DATE_LITERAL, "DATE_LITERAL"), root_1);

							adaptor.addChild(root_1, stream_d.next());
							// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:261:99:
							// ( $f)?
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
			// reportError(e);
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
	// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:268:1:
	// relationalOperator : ( EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL
	// | IN | IS | BETWEEN | LIKE | MATCHES | SOUNDSLIKE | DISTANCETO );
	public final relationalOperator_return relationalOperator() throws RecognitionException {
		relationalOperator_return retval = new relationalOperator_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token set175 = null;

		Object set175_tree = null;

		try {
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:269:5:
			// ( EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL | IN | IS |
			// BETWEEN | LIKE | MATCHES | SOUNDSLIKE | DISTANCETO )
			// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:
			{
				root_0 = (Object) adaptor.nil();

				set175 = (Token) input.LT(1);
				if ((input.LA(1) >= EQUAL && input.LA(1) <= DISTANCETO)) {
					input.consume();
					if (backtracking == 0)
						adaptor.addChild(root_0, adaptor.create(set175));
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
			// reportError(e);
			throw e;
		} finally {
		}
		return retval;
	}

	// $ANTLR end relationalOperator

	// $ANTLR start synpred1
	public final void synpred1_fragment() throws RecognitionException {
		// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:104:5:
		// ( LPAREN expression SEMI )
		// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:104:6:
		// LPAREN expression SEMI
		{
			match(input, LPAREN, FOLLOW_LPAREN_in_synpred1545);
			if (failed)
				return;
			pushFollow(FOLLOW_expression_in_synpred1547);
			expression();
			_fsp--;
			if (failed)
				return;
			match(input, SEMI, FOLLOW_SEMI_in_synpred1549);
			if (failed)
				return;

		}
	}

	// $ANTLR end synpred1

	// $ANTLR start synpred2
	public final void synpred2_fragment() throws RecognitionException {
		// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:138:7:
		// ( POUND ID LPAREN )
		// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:138:8:
		// POUND ID LPAREN
		{
			match(input, POUND, FOLLOW_POUND_in_synpred2797);
			if (failed)
				return;
			match(input, ID, FOLLOW_ID_in_synpred2799);
			if (failed)
				return;
			match(input, LPAREN, FOLLOW_LPAREN_in_synpred2801);
			if (failed)
				return;

		}
	}

	// $ANTLR end synpred2

	// $ANTLR start synpred3
	public final void synpred3_fragment() throws RecognitionException {
		// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:147:4:
		// ( DOLLAR ID LPAREN )
		// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:147:5:
		// DOLLAR ID LPAREN
		{
			match(input, DOLLAR, FOLLOW_DOLLAR_in_synpred3880);
			if (failed)
				return;
			match(input, ID, FOLLOW_ID_in_synpred3882);
			if (failed)
				return;
			match(input, LPAREN, FOLLOW_LPAREN_in_synpred3884);
			if (failed)
				return;

		}
	}

	// $ANTLR end synpred3

	// $ANTLR start synpred4
	public final void synpred4_fragment() throws RecognitionException {
		// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:155:4:
		// ( ID LPAREN )
		// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:155:5:
		// ID LPAREN
		{
			match(input, ID, FOLLOW_ID_in_synpred4946);
			if (failed)
				return;
			match(input, LPAREN, FOLLOW_LPAREN_in_synpred4948);
			if (failed)
				return;

		}
	}

	// $ANTLR end synpred4

	// $ANTLR start synpred5
	public final void synpred5_fragment() throws RecognitionException {
		// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:210:4:
		// ( 'new' qualifiedId LPAREN )
		// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:210:5:
		// 'new' qualifiedId LPAREN
		{
			match(input, 94, FOLLOW_94_in_synpred51295);
			if (failed)
				return;
			pushFollow(FOLLOW_qualifiedId_in_synpred51297);
			qualifiedId();
			_fsp--;
			if (failed)
				return;
			match(input, LPAREN, FOLLOW_LPAREN_in_synpred51299);
			if (failed)
				return;

		}
	}

	// $ANTLR end synpred5

	// $ANTLR start synpred6
	public final void synpred6_fragment() throws RecognitionException {
		// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:240:7:
		// ( ID ASSIGN )
		// /Users/aclement/spring-pieces/spring-el/trunk/org.springframework.el/src/main/java/org/springframework/el/generated/SpringExpressions.g:240:8:
		// ID ASSIGN
		{
			match(input, ID, FOLLOW_ID_in_synpred61551);
			if (failed)
				return;
			match(input, ASSIGN, FOLLOW_ASSIGN_in_synpred61553);
			if (failed)
				return;

		}
	}

	// $ANTLR end synpred6

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

	public final boolean synpred6() {
		backtracking++;
		int start = input.mark();
		try {
			synpred6_fragment(); // can never throw exception
		} catch (RecognitionException re) {
			System.err.println("impossible: " + re);
		}
		boolean success = !failed;
		input.rewind(start);
		backtracking--;
		failed = false;
		return success;
	}

	public static final BitSet FOLLOW_expression_in_expr173 = new BitSet(new long[] { 0x0000000000000000L });
	public static final BitSet FOLLOW_EOF_in_expr175 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LPAREN_in_exprList188 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_exprList190 = new BitSet(new long[] { 0x0000000080000000L });
	public static final BitSet FOLLOW_SEMI_in_exprList193 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_exprList195 = new BitSet(new long[] { 0x0000000380000000L });
	public static final BitSet FOLLOW_SEMIRPAREN_in_exprList200 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_RPAREN_in_exprList204 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_logicalOrExpression_in_expression248 = new BitSet(
			new long[] { 0x0000001C00000002L });
	public static final BitSet FOLLOW_ASSIGN_in_expression257 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_logicalOrExpression_in_expression260 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_DEFAULT_in_expression270 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_logicalOrExpression_in_expression273 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_QMARK_in_expression283 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_expression286 = new BitSet(new long[] { 0x0000002000000000L });
	public static final BitSet FOLLOW_COLON_in_expression288 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_expression291 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LPAREN_in_parenExpr302 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_parenExpr305 = new BitSet(new long[] { 0x0000000200000000L });
	public static final BitSet FOLLOW_RPAREN_in_parenExpr307 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_logicalAndExpression_in_logicalOrExpression318 = new BitSet(
			new long[] { 0x0000004000000002L });
	public static final BitSet FOLLOW_OR_in_logicalOrExpression321 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_logicalAndExpression_in_logicalOrExpression324 = new BitSet(
			new long[] { 0x0000004000000002L });
	public static final BitSet FOLLOW_relationalExpression_in_logicalAndExpression358 = new BitSet(
			new long[] { 0x0000008000000002L });
	public static final BitSet FOLLOW_AND_in_logicalAndExpression361 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_relationalExpression_in_logicalAndExpression364 = new BitSet(
			new long[] { 0x0000008000000002L });
	public static final BitSet FOLLOW_sumExpression_in_relationalExpression375 = new BitSet(new long[] {
			0x0000000000000002L, 0x00000000007FFC00L });
	public static final BitSet FOLLOW_relationalOperator_in_relationalExpression378 = new BitSet(new long[] {
			0xBEB7430040000020L, 0x00000000C000007FL });
	public static final BitSet FOLLOW_sumExpression_in_relationalExpression381 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_productExpression_in_sumExpression392 = new BitSet(
			new long[] { 0x0000030000000002L });
	public static final BitSet FOLLOW_PLUS_in_sumExpression397 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_MINUS_in_sumExpression402 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_productExpression_in_sumExpression406 = new BitSet(
			new long[] { 0x0000030000000002L });
	public static final BitSet FOLLOW_powerExpr_in_productExpression421 = new BitSet(new long[] { 0x00001C0000000002L });
	public static final BitSet FOLLOW_STAR_in_productExpression425 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_DIV_in_productExpression430 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_MOD_in_productExpression434 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_powerExpr_in_productExpression438 = new BitSet(new long[] { 0x00001C0000000002L });
	public static final BitSet FOLLOW_unaryExpression_in_powerExpr454 = new BitSet(new long[] { 0x0000200000000002L });
	public static final BitSet FOLLOW_POWER_in_powerExpr457 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_unaryExpression_in_powerExpr460 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_PLUS_in_unaryExpression474 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_MINUS_in_unaryExpression479 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_BANG_in_unaryExpression484 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_unaryExpression_in_unaryExpression488 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_primaryExpression_in_unaryExpression494 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_startNode_in_primaryExpression508 = new BitSet(new long[] { 0x0EA3800040000002L });
	public static final BitSet FOLLOW_node_in_primaryExpression511 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_exprList_in_startNode554 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_parenExpr_in_startNode563 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_methodOrProperty_in_startNode571 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_functionOrVar_in_startNode580 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_localFunctionOrVar_in_startNode588 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_reference_in_startNode596 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_indexer_in_startNode604 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_literal_in_startNode612 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_type_in_startNode620 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_constructor_in_startNode628 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_projection_in_startNode636 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_selection_in_startNode645 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_firstSelection_in_startNode654 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_lastSelection_in_startNode662 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_listInitializer_in_startNode670 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_mapInitializer_in_startNode678 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_lambda_in_startNode686 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_methodOrProperty_in_node707 = new BitSet(new long[] { 0x0EA3800040000002L });
	public static final BitSet FOLLOW_functionOrVar_in_node713 = new BitSet(new long[] { 0x0EA3800040000002L });
	public static final BitSet FOLLOW_indexer_in_node721 = new BitSet(new long[] { 0x0EA3800040000002L });
	public static final BitSet FOLLOW_projection_in_node729 = new BitSet(new long[] { 0x0EA3800040000002L });
	public static final BitSet FOLLOW_selection_in_node738 = new BitSet(new long[] { 0x0EA3800040000002L });
	public static final BitSet FOLLOW_firstSelection_in_node747 = new BitSet(new long[] { 0x0EA3800040000002L });
	public static final BitSet FOLLOW_lastSelection_in_node756 = new BitSet(new long[] { 0x0EA3800040000002L });
	public static final BitSet FOLLOW_exprList_in_node765 = new BitSet(new long[] { 0x0EA3800040000002L });
	public static final BitSet FOLLOW_DOT_in_node773 = new BitSet(new long[] { 0x0EA3800040000002L });
	public static final BitSet FOLLOW_function_in_functionOrVar806 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_var_in_functionOrVar814 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_POUND_in_function831 = new BitSet(new long[] { 0x0002000000000000L });
	public static final BitSet FOLLOW_ID_in_function835 = new BitSet(new long[] { 0x0000000040000000L });
	public static final BitSet FOLLOW_methodArgs_in_function837 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_POUND_in_var858 = new BitSet(new long[] { 0x0002000000000000L });
	public static final BitSet FOLLOW_ID_in_var862 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_localFunction_in_localFunctionOrVar889 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_localVar_in_localFunctionOrVar894 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_DOLLAR_in_localFunction904 = new BitSet(new long[] { 0x0002000000000000L });
	public static final BitSet FOLLOW_ID_in_localFunction908 = new BitSet(new long[] { 0x0000000040000000L });
	public static final BitSet FOLLOW_methodArgs_in_localFunction910 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_DOLLAR_in_localVar925 = new BitSet(new long[] { 0x0002000000000000L });
	public static final BitSet FOLLOW_ID_in_localVar929 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_ID_in_methodOrProperty955 = new BitSet(new long[] { 0x0000000040000000L });
	public static final BitSet FOLLOW_methodArgs_in_methodOrProperty957 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_property_in_methodOrProperty971 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LPAREN_in_methodArgs986 = new BitSet(new long[] { 0xBEB7430240000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_argument_in_methodArgs990 = new BitSet(new long[] { 0x0008000200000000L });
	public static final BitSet FOLLOW_COMMA_in_methodArgs993 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_argument_in_methodArgs996 = new BitSet(new long[] { 0x0008000200000000L });
	public static final BitSet FOLLOW_COMMA_in_methodArgs1001 = new BitSet(new long[] { 0x0000000200000000L });
	public static final BitSet FOLLOW_RPAREN_in_methodArgs1008 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_ID_in_property1021 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_AT_in_reference1043 = new BitSet(new long[] { 0x0000000040000000L });
	public static final BitSet FOLLOW_LPAREN_in_reference1047 = new BitSet(new long[] { 0x0002000200000000L });
	public static final BitSet FOLLOW_contextName_in_reference1052 = new BitSet(new long[] { 0x0000002000000000L });
	public static final BitSet FOLLOW_COLON_in_reference1054 = new BitSet(new long[] { 0x0002000200000000L });
	public static final BitSet FOLLOW_qualifiedId_in_reference1061 = new BitSet(new long[] { 0x0000000200000000L });
	public static final BitSet FOLLOW_RPAREN_in_reference1065 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LBRACKET_in_indexer1100 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_argument_in_indexer1104 = new BitSet(new long[] { 0x0048000000000000L });
	public static final BitSet FOLLOW_COMMA_in_indexer1107 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_argument_in_indexer1111 = new BitSet(new long[] { 0x0048000000000000L });
	public static final BitSet FOLLOW_RBRACKET_in_indexer1115 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_PROJECT_in_projection1142 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_projection1145 = new BitSet(new long[] { 0x0100000000000000L });
	public static final BitSet FOLLOW_RCURLY_in_projection1147 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_SELECT_in_selection1155 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_selection1158 = new BitSet(new long[] { 0x0100000000000000L });
	public static final BitSet FOLLOW_RCURLY_in_selection1160 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_SELECT_FIRST_in_firstSelection1168 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_firstSelection1171 = new BitSet(new long[] { 0x0100000000000000L });
	public static final BitSet FOLLOW_RCURLY_in_firstSelection1173 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_SELECT_LAST_in_lastSelection1181 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_lastSelection1184 = new BitSet(new long[] { 0x0100000000000000L });
	public static final BitSet FOLLOW_RCURLY_in_lastSelection1186 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_TYPE_in_type1195 = new BitSet(new long[] { 0x0002000000000000L });
	public static final BitSet FOLLOW_qualifiedId_in_type1197 = new BitSet(new long[] { 0x0000000200000000L });
	public static final BitSet FOLLOW_RPAREN_in_type1199 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LAMBDA_in_lambda1226 = new BitSet(new long[] { 0x4002000000000000L });
	public static final BitSet FOLLOW_argList_in_lambda1229 = new BitSet(new long[] { 0x4000000000000000L });
	public static final BitSet FOLLOW_PIPE_in_lambda1233 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_lambda1235 = new BitSet(new long[] { 0x0100000000000000L });
	public static final BitSet FOLLOW_RCURLY_in_lambda1237 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_ID_in_argList1261 = new BitSet(new long[] { 0x0008000000000002L });
	public static final BitSet FOLLOW_COMMA_in_argList1264 = new BitSet(new long[] { 0x0002000000000000L });
	public static final BitSet FOLLOW_ID_in_argList1268 = new BitSet(new long[] { 0x0008000000000002L });
	public static final BitSet FOLLOW_94_in_constructor1304 = new BitSet(new long[] { 0x0002000000000000L });
	public static final BitSet FOLLOW_qualifiedId_in_constructor1306 = new BitSet(new long[] { 0x0000000040000000L });
	public static final BitSet FOLLOW_ctorArgs_in_constructor1308 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_arrayConstructor_in_constructor1325 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_94_in_arrayConstructor1336 = new BitSet(new long[] { 0x0002000000000000L });
	public static final BitSet FOLLOW_qualifiedId_in_arrayConstructor1338 = new BitSet(
			new long[] { 0x0020000000000000L });
	public static final BitSet FOLLOW_arrayRank_in_arrayConstructor1340 = new BitSet(new long[] { 0x8000000000000002L });
	public static final BitSet FOLLOW_listInitializer_in_arrayConstructor1343 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LBRACKET_in_arrayRank1378 = new BitSet(new long[] { 0xBEF7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_arrayRank1381 = new BitSet(new long[] { 0x0048000000000000L });
	public static final BitSet FOLLOW_COMMA_in_arrayRank1384 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_arrayRank1386 = new BitSet(new long[] { 0x0048000000000000L });
	public static final BitSet FOLLOW_RBRACKET_in_arrayRank1392 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LCURLY_in_listInitializer1417 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_listInitializer1419 = new BitSet(new long[] { 0x0108000000000000L });
	public static final BitSet FOLLOW_COMMA_in_listInitializer1422 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_listInitializer1424 = new BitSet(new long[] { 0x0108000000000000L });
	public static final BitSet FOLLOW_RCURLY_in_listInitializer1428 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_POUND_in_mapInitializer1456 = new BitSet(new long[] { 0x8000000000000000L });
	public static final BitSet FOLLOW_LCURLY_in_mapInitializer1458 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_mapEntry_in_mapInitializer1460 = new BitSet(new long[] { 0x0108000000000000L });
	public static final BitSet FOLLOW_COMMA_in_mapInitializer1463 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_mapEntry_in_mapInitializer1465 = new BitSet(new long[] { 0x0108000000000000L });
	public static final BitSet FOLLOW_RCURLY_in_mapInitializer1469 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_expression_in_mapEntry1490 = new BitSet(new long[] { 0x0000002000000000L });
	public static final BitSet FOLLOW_COLON_in_mapEntry1492 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_mapEntry1494 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LPAREN_in_ctorArgs1512 = new BitSet(new long[] { 0xBEB7430240000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_namedArgument_in_ctorArgs1516 = new BitSet(new long[] { 0x0008000200000000L });
	public static final BitSet FOLLOW_COMMA_in_ctorArgs1519 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_namedArgument_in_ctorArgs1522 = new BitSet(new long[] { 0x0008000200000000L });
	public static final BitSet FOLLOW_RPAREN_in_ctorArgs1528 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_expression_in_argument1537 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_ID_in_namedArgument1560 = new BitSet(new long[] { 0x0000000400000000L });
	public static final BitSet FOLLOW_ASSIGN_in_namedArgument1562 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_namedArgument1564 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_argument_in_namedArgument1600 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_ID_in_qualifiedId1612 = new BitSet(new long[] { 0x0000800000000002L });
	public static final BitSet FOLLOW_DOT_in_qualifiedId1615 = new BitSet(new long[] { 0x0002000000000000L });
	public static final BitSet FOLLOW_ID_in_qualifiedId1617 = new BitSet(new long[] { 0x0000800000000002L });
	public static final BitSet FOLLOW_ID_in_contextName1636 = new BitSet(new long[] { 0x0000080000000002L });
	public static final BitSet FOLLOW_DIV_in_contextName1639 = new BitSet(new long[] { 0x0002000000000000L });
	public static final BitSet FOLLOW_ID_in_contextName1641 = new BitSet(new long[] { 0x0000080000000002L });
	public static final BitSet FOLLOW_INTEGER_LITERAL_in_literal1662 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_STRING_LITERAL_in_literal1668 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_DQ_STRING_LITERAL_in_literal1673 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_boolLiteral_in_literal1678 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_NULL_LITERAL_in_literal1683 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_HEXADECIMAL_INTEGER_LITERAL_in_literal1688 = new BitSet(
			new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_REAL_LITERAL_in_literal1694 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_dateLiteral_in_literal1699 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_set_in_boolLiteral0 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_95_in_dateLiteral1720 = new BitSet(new long[] { 0x0000000040000000L });
	public static final BitSet FOLLOW_LPAREN_in_dateLiteral1722 = new BitSet(new long[] { 0x0000000000000000L,
			0x0000000000000001L });
	public static final BitSet FOLLOW_STRING_LITERAL_in_dateLiteral1726 = new BitSet(new long[] { 0x0008000200000000L });
	public static final BitSet FOLLOW_COMMA_in_dateLiteral1729 = new BitSet(new long[] { 0x0000000000000000L,
			0x0000000000000001L });
	public static final BitSet FOLLOW_STRING_LITERAL_in_dateLiteral1733 = new BitSet(new long[] { 0x0000000200000000L });
	public static final BitSet FOLLOW_RPAREN_in_dateLiteral1737 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_set_in_relationalOperator0 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_LPAREN_in_synpred1545 = new BitSet(new long[] { 0xBEB7430040000020L,
			0x00000000C000007FL });
	public static final BitSet FOLLOW_expression_in_synpred1547 = new BitSet(new long[] { 0x0000000080000000L });
	public static final BitSet FOLLOW_SEMI_in_synpred1549 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_POUND_in_synpred2797 = new BitSet(new long[] { 0x0002000000000000L });
	public static final BitSet FOLLOW_ID_in_synpred2799 = new BitSet(new long[] { 0x0000000040000000L });
	public static final BitSet FOLLOW_LPAREN_in_synpred2801 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_DOLLAR_in_synpred3880 = new BitSet(new long[] { 0x0002000000000000L });
	public static final BitSet FOLLOW_ID_in_synpred3882 = new BitSet(new long[] { 0x0000000040000000L });
	public static final BitSet FOLLOW_LPAREN_in_synpred3884 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_ID_in_synpred4946 = new BitSet(new long[] { 0x0000000040000000L });
	public static final BitSet FOLLOW_LPAREN_in_synpred4948 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_94_in_synpred51295 = new BitSet(new long[] { 0x0002000000000000L });
	public static final BitSet FOLLOW_qualifiedId_in_synpred51297 = new BitSet(new long[] { 0x0000000040000000L });
	public static final BitSet FOLLOW_LPAREN_in_synpred51299 = new BitSet(new long[] { 0x0000000000000002L });
	public static final BitSet FOLLOW_ID_in_synpred61551 = new BitSet(new long[] { 0x0000000400000000L });
	public static final BitSet FOLLOW_ASSIGN_in_synpred61553 = new BitSet(new long[] { 0x0000000000000002L });

}