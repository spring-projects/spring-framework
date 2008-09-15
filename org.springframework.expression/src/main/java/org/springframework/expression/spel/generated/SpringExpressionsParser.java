// $ANTLR 3.0.1 /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g 2008-09-15 13:20:03
package org.springframework.expression.spel.generated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

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

@SuppressWarnings({"unused","cast","unchecked"})
public class SpringExpressionsParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "EXPRESSIONLIST", "INTEGER_LITERAL", "EXPRESSION", "QUALIFIED_IDENTIFIER", "REFERENCE", "PROPERTY_OR_FIELD", "INDEXER", "ARGLIST", "CONSTRUCTOR", "DATE_LITERAL", "HOLDER", "CONSTRUCTOR_ARRAY", "NAMED_ARGUMENT", "FUNCTIONREF", "TYPEREF", "RANGE", "VARIABLEREF", "LIST_INITIALIZER", "MAP_INITIALIZER", "LOCALVAR", "LOCALFUNC", "MAP_ENTRY", "METHOD", "ADD", "SUBTRACT", "NUMBER", "LPAREN", "SEMI", "SEMIRPAREN", "RPAREN", "ASSIGN", "DEFAULT", "QMARK", "COLON", "OR", "AND", "PLUS", "MINUS", "STAR", "DIV", "MOD", "POWER", "BANG", "DOT", "POUND", "ID", "DOLLAR", "COMMA", "AT", "LBRACKET", "RBRACKET", "PROJECT", "RCURLY", "SELECT", "SELECT_FIRST", "SELECT_LAST", "TYPE", "LAMBDA", "PIPE", "LCURLY", "STRING_LITERAL", "DQ_STRING_LITERAL", "NULL_LITERAL", "HEXADECIMAL_INTEGER_LITERAL", "REAL_LITERAL", "TRUE", "FALSE", "DECIMAL_DIGIT", "INTEGER_TYPE_SUFFIX", "HEX_DIGIT", "EQUAL", "NOT_EQUAL", "LESS_THAN", "LESS_THAN_OR_EQUAL", "GREATER_THAN", "GREATER_THAN_OR_EQUAL", "IN", "IS", "BETWEEN", "MATCHES", "APOS", "DOT_ESCAPED", "WS", "UPTO", "EXPONENT_PART", "REAL_TYPE_SUFFIX", "SIGN", "'new'", "'date'"
    };
    public static final int GREATER_THAN_OR_EQUAL=79;
    public static final int SELECT_FIRST=58;
    public static final int COMMA=51;
    public static final int HOLDER=14;
    public static final int GREATER_THAN=78;
    public static final int TYPE=60;
    public static final int EXPRESSIONLIST=4;
    public static final int MINUS=41;
    public static final int MAP_ENTRY=25;
    public static final int SELECT_LAST=59;
    public static final int NUMBER=29;
    public static final int LESS_THAN=76;
    public static final int BANG=46;
    public static final int ARGLIST=11;
    public static final int FALSE=70;
    public static final int METHOD=26;
    public static final int PROPERTY_OR_FIELD=9;
    public static final int LBRACKET=53;
    public static final int MOD=44;
    public static final int INDEXER=10;
    public static final int CONSTRUCTOR_ARRAY=15;
    public static final int FUNCTIONREF=17;
    public static final int NULL_LITERAL=66;
    public static final int NAMED_ARGUMENT=16;
    public static final int OR=38;
    public static final int PIPE=62;
    public static final int DOT=47;
    public static final int RCURLY=56;
    public static final int EXPRESSION=6;
    public static final int AND=39;
    public static final int LCURLY=63;
    public static final int DATE_LITERAL=13;
    public static final int REAL_TYPE_SUFFIX=89;
    public static final int STRING_LITERAL=64;
    public static final int SELECT=57;
    public static final int QUALIFIED_IDENTIFIER=7;
    public static final int RBRACKET=54;
    public static final int SUBTRACT=28;
    public static final int ASSIGN=34;
    public static final int BETWEEN=82;
    public static final int RPAREN=33;
    public static final int SIGN=90;
    public static final int LPAREN=30;
    public static final int HEX_DIGIT=73;
    public static final int PLUS=40;
    public static final int LIST_INITIALIZER=21;
    public static final int APOS=84;
    public static final int INTEGER_LITERAL=5;
    public static final int AT=52;
    public static final int ID=49;
    public static final int NOT_EQUAL=75;
    public static final int RANGE=19;
    public static final int POWER=45;
    public static final int TYPEREF=18;
    public static final int DECIMAL_DIGIT=71;
    public static final int WS=86;
    public static final int IS=81;
    public static final int DOLLAR=50;
    public static final int LESS_THAN_OR_EQUAL=77;
    public static final int SEMIRPAREN=32;
    public static final int DQ_STRING_LITERAL=65;
    public static final int HEXADECIMAL_INTEGER_LITERAL=67;
    public static final int MAP_INITIALIZER=22;
    public static final int LAMBDA=61;
    public static final int LOCALFUNC=24;
    public static final int IN=80;
    public static final int CONSTRUCTOR=12;
    public static final int SEMI=31;
    public static final int INTEGER_TYPE_SUFFIX=72;
    public static final int EQUAL=74;
    public static final int MATCHES=83;
    public static final int DOT_ESCAPED=85;
    public static final int UPTO=87;
    public static final int EOF=-1;
    public static final int QMARK=36;
    public static final int REFERENCE=8;
    public static final int PROJECT=55;
    public static final int DEFAULT=35;
    public static final int COLON=37;
    public static final int DIV=43;
    public static final int LOCALVAR=23;
    public static final int STAR=42;
    public static final int REAL_LITERAL=68;
    public static final int VARIABLEREF=20;
    public static final int EXPONENT_PART=88;
    public static final int TRUE=69;
    public static final int ADD=27;
    public static final int POUND=48;

        public SpringExpressionsParser(TokenStream input) {
            super(input);
            ruleMemo = new HashMap[55+1];
         }

    protected TreeAdaptor adaptor = new CommonTreeAdaptor();

    public void setTreeAdaptor(TreeAdaptor adaptor) {
        this.adaptor = adaptor;
    }
    public TreeAdaptor getTreeAdaptor() {
        return adaptor;
    }

    @Override
	public String[] getTokenNames() { return tokenNames; }
    @Override
	public String getGrammarFileName() { return "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g"; }


      // For collecting info whilst processing rules that can be used in messages
      protected Stack<String> paraphrase = new Stack<String>();


    public static class expr_return extends ParserRuleReturnScope {
        Object tree;
        @Override
		public Object getTree() { return tree; }
    };

    // $ANTLR start expr
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:61:1: expr : expression EOF ;
    public final expr_return expr() throws RecognitionException {
        expr_return retval = new expr_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token EOF2=null;
        expression_return expression1 = null;


        Object EOF2_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:61:5: ( expression EOF )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:61:7: expression EOF
            {
            root_0 = adaptor.nil();

            pushFollow(FOLLOW_expression_in_expr181);
            expression1=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression1.getTree());
            EOF2=input.LT(1);
            match(input,EOF,FOLLOW_EOF_in_expr183); if (failed) return retval;

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end expr

    public static class exprList_return extends ParserRuleReturnScope {
        Object tree;
        @Override
		public Object getTree() { return tree; }
    };

    // $ANTLR start exprList
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:63:1: exprList : LPAREN expression ( SEMI expression )+ ( SEMIRPAREN | RPAREN ) -> ^( EXPRESSIONLIST ( expression )+ ) ;
    public final exprList_return exprList() throws RecognitionException {
        exprList_return retval = new exprList_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LPAREN3=null;
        Token SEMI5=null;
        Token SEMIRPAREN7=null;
        Token RPAREN8=null;
        expression_return expression4 = null;

        expression_return expression6 = null;


		Object LPAREN3_tree=null;
        Object SEMI5_tree=null;
        Object SEMIRPAREN7_tree=null;
        Object RPAREN8_tree=null;
        RewriteRuleTokenStream stream_SEMI=new RewriteRuleTokenStream(adaptor,"token SEMI");
        RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
        RewriteRuleTokenStream stream_LPAREN=new RewriteRuleTokenStream(adaptor,"token LPAREN");
        RewriteRuleTokenStream stream_SEMIRPAREN=new RewriteRuleTokenStream(adaptor,"token SEMIRPAREN");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:64:5: ( LPAREN expression ( SEMI expression )+ ( SEMIRPAREN | RPAREN ) -> ^( EXPRESSIONLIST ( expression )+ ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:64:7: LPAREN expression ( SEMI expression )+ ( SEMIRPAREN | RPAREN )
            {
            LPAREN3=input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_exprList196); if (failed) return retval;
            if ( backtracking==0 ) stream_LPAREN.add(LPAREN3);

            pushFollow(FOLLOW_expression_in_exprList198);
            expression4=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression4.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:64:25: ( SEMI expression )+
            int cnt1=0;
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0==SEMI) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:64:26: SEMI expression
            	    {
            	    SEMI5=input.LT(1);
            	    match(input,SEMI,FOLLOW_SEMI_in_exprList201); if (failed) return retval;
            	    if ( backtracking==0 ) stream_SEMI.add(SEMI5);

            	    pushFollow(FOLLOW_expression_in_exprList203);
            	    expression6=expression();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) stream_expression.add(expression6.getTree());

            	    }
            	    break;

            	default :
            	    if ( cnt1 >= 1 ) break loop1;
            	    if (backtracking>0) {failed=true; return retval;}
                        EarlyExitException eee =
                            new EarlyExitException(1, input);
                        throw eee;
                }
                cnt1++;
            } while (true);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:64:44: ( SEMIRPAREN | RPAREN )
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==SEMIRPAREN) ) {
                alt2=1;
            }
            else if ( (LA2_0==RPAREN) ) {
                alt2=2;
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("64:44: ( SEMIRPAREN | RPAREN )", 2, 0, input);

                throw nvae;
            }
            switch (alt2) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:64:45: SEMIRPAREN
                    {
                    SEMIRPAREN7=(Token)input.LT(1);
                    match(input,SEMIRPAREN,FOLLOW_SEMIRPAREN_in_exprList208); if (failed) return retval;
                    if ( backtracking==0 ) stream_SEMIRPAREN.add(SEMIRPAREN7);


                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:64:58: RPAREN
                    {
                    RPAREN8=(Token)input.LT(1);
                    match(input,RPAREN,FOLLOW_RPAREN_in_exprList212); if (failed) return retval;
                    if ( backtracking==0 ) stream_RPAREN.add(RPAREN8);


                    }
                    break;

            }


            // AST REWRITE
            // elements: expression
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 65:7: -> ^( EXPRESSIONLIST ( expression )+ )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:65:10: ^( EXPRESSIONLIST ( expression )+ )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(EXPRESSIONLIST, "EXPRESSIONLIST"), root_1);

                if ( !(stream_expression.hasNext()) ) {
                    throw new RewriteEarlyExitException();
                }
                while ( stream_expression.hasNext() ) {
                    adaptor.addChild(root_1, stream_expression.next());

                }
                stream_expression.reset();

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end exprList

    public static class expression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start expression
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:69:1: expression : logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )? ;
    public final expression_return expression() throws RecognitionException {
        expression_return retval = new expression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ASSIGN10=null;
        Token DEFAULT12=null;
        Token QMARK14=null;
        Token COLON16=null;
        logicalOrExpression_return logicalOrExpression9 = null;

        logicalOrExpression_return logicalOrExpression11 = null;

        logicalOrExpression_return logicalOrExpression13 = null;

        expression_return expression15 = null;

        expression_return expression17 = null;


        Object ASSIGN10_tree=null;
        Object DEFAULT12_tree=null;
        Object QMARK14_tree=null;
        Object COLON16_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:69:12: ( logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )? )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:70:5: logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )?
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_logicalOrExpression_in_expression256);
            logicalOrExpression9=logicalOrExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, logicalOrExpression9.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:71:5: ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )?
            int alt3=4;
            switch ( input.LA(1) ) {
                case ASSIGN:
                    {
                    alt3=1;
                    }
                    break;
                case DEFAULT:
                    {
                    alt3=2;
                    }
                    break;
                case QMARK:
                    {
                    alt3=3;
                    }
                    break;
            }

            switch (alt3) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:71:7: ( ASSIGN logicalOrExpression )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:71:7: ( ASSIGN logicalOrExpression )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:71:8: ASSIGN logicalOrExpression
                    {
                    ASSIGN10=(Token)input.LT(1);
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_expression265); if (failed) return retval;
                    if ( backtracking==0 ) {
                    ASSIGN10_tree = (Object)adaptor.create(ASSIGN10);
                    root_0 = (Object)adaptor.becomeRoot(ASSIGN10_tree, root_0);
                    }
                    pushFollow(FOLLOW_logicalOrExpression_in_expression268);
                    logicalOrExpression11=logicalOrExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, logicalOrExpression11.getTree());

                    }


                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:72:6: ( DEFAULT logicalOrExpression )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:72:6: ( DEFAULT logicalOrExpression )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:72:7: DEFAULT logicalOrExpression
                    {
                    DEFAULT12=(Token)input.LT(1);
                    match(input,DEFAULT,FOLLOW_DEFAULT_in_expression278); if (failed) return retval;
                    if ( backtracking==0 ) {
                    DEFAULT12_tree = (Object)adaptor.create(DEFAULT12);
                    root_0 = (Object)adaptor.becomeRoot(DEFAULT12_tree, root_0);
                    }
                    pushFollow(FOLLOW_logicalOrExpression_in_expression281);
                    logicalOrExpression13=logicalOrExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, logicalOrExpression13.getTree());

                    }


                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:73:6: ( QMARK expression COLON expression )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:73:6: ( QMARK expression COLON expression )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:73:7: QMARK expression COLON expression
                    {
                    QMARK14=(Token)input.LT(1);
                    match(input,QMARK,FOLLOW_QMARK_in_expression291); if (failed) return retval;
                    if ( backtracking==0 ) {
                    QMARK14_tree = (Object)adaptor.create(QMARK14);
                    root_0 = (Object)adaptor.becomeRoot(QMARK14_tree, root_0);
                    }
                    pushFollow(FOLLOW_expression_in_expression294);
                    expression15=expression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, expression15.getTree());
                    COLON16=(Token)input.LT(1);
                    match(input,COLON,FOLLOW_COLON_in_expression296); if (failed) return retval;
                    pushFollow(FOLLOW_expression_in_expression299);
                    expression17=expression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, expression17.getTree());

                    }


                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end expression

    public static class parenExpr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start parenExpr
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:1: parenExpr : LPAREN expression RPAREN ;
    public final parenExpr_return parenExpr() throws RecognitionException {
        parenExpr_return retval = new parenExpr_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LPAREN18=null;
        Token RPAREN20=null;
        expression_return expression19 = null;


        Object LPAREN18_tree=null;
        Object RPAREN20_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:11: ( LPAREN expression RPAREN )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:13: LPAREN expression RPAREN
            {
            root_0 = (Object)adaptor.nil();

            LPAREN18=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_parenExpr310); if (failed) return retval;
            pushFollow(FOLLOW_expression_in_parenExpr313);
            expression19=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression19.getTree());
            RPAREN20=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_parenExpr315); if (failed) return retval;

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end parenExpr

    public static class logicalOrExpression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start logicalOrExpression
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:78:1: logicalOrExpression : logicalAndExpression ( OR logicalAndExpression )* ;
    public final logicalOrExpression_return logicalOrExpression() throws RecognitionException {
        logicalOrExpression_return retval = new logicalOrExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token OR22=null;
        logicalAndExpression_return logicalAndExpression21 = null;

        logicalAndExpression_return logicalAndExpression23 = null;


        Object OR22_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:1: ( logicalAndExpression ( OR logicalAndExpression )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:3: logicalAndExpression ( OR logicalAndExpression )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_logicalAndExpression_in_logicalOrExpression328);
            logicalAndExpression21=logicalAndExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, logicalAndExpression21.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:24: ( OR logicalAndExpression )*
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);

                if ( (LA4_0==OR) ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:25: OR logicalAndExpression
            	    {
            	    OR22=(Token)input.LT(1);
            	    match(input,OR,FOLLOW_OR_in_logicalOrExpression331); if (failed) return retval;
            	    if ( backtracking==0 ) {
            	    OR22_tree = (Object)adaptor.create(OR22);
            	    root_0 = (Object)adaptor.becomeRoot(OR22_tree, root_0);
            	    }
            	    pushFollow(FOLLOW_logicalAndExpression_in_logicalOrExpression334);
            	    logicalAndExpression23=logicalAndExpression();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, logicalAndExpression23.getTree());

            	    }
            	    break;

            	default :
            	    break loop4;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end logicalOrExpression

    public static class logicalAndExpression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start logicalAndExpression
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:82:1: logicalAndExpression : relationalExpression ( AND relationalExpression )* ;
    public final logicalAndExpression_return logicalAndExpression() throws RecognitionException {
        logicalAndExpression_return retval = new logicalAndExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token AND25=null;
        relationalExpression_return relationalExpression24 = null;

        relationalExpression_return relationalExpression26 = null;


        Object AND25_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:83:1: ( relationalExpression ( AND relationalExpression )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:83:3: relationalExpression ( AND relationalExpression )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_relationalExpression_in_logicalAndExpression369);
            relationalExpression24=relationalExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, relationalExpression24.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:83:24: ( AND relationalExpression )*
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);

                if ( (LA5_0==AND) ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:83:25: AND relationalExpression
            	    {
            	    AND25=(Token)input.LT(1);
            	    match(input,AND,FOLLOW_AND_in_logicalAndExpression372); if (failed) return retval;
            	    if ( backtracking==0 ) {
            	    AND25_tree = (Object)adaptor.create(AND25);
            	    root_0 = (Object)adaptor.becomeRoot(AND25_tree, root_0);
            	    }
            	    pushFollow(FOLLOW_relationalExpression_in_logicalAndExpression375);
            	    relationalExpression26=relationalExpression();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, relationalExpression26.getTree());

            	    }
            	    break;

            	default :
            	    break loop5;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end logicalAndExpression

    public static class relationalExpression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start relationalExpression
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:85:1: relationalExpression : sumExpression ( relationalOperator sumExpression )? ;
    public final relationalExpression_return relationalExpression() throws RecognitionException {
        relationalExpression_return retval = new relationalExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        sumExpression_return sumExpression27 = null;

        relationalOperator_return relationalOperator28 = null;

        sumExpression_return sumExpression29 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:85:22: ( sumExpression ( relationalOperator sumExpression )? )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:85:24: sumExpression ( relationalOperator sumExpression )?
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_sumExpression_in_relationalExpression386);
            sumExpression27=sumExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, sumExpression27.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:85:38: ( relationalOperator sumExpression )?
            int alt6=2;
            int LA6_0 = input.LA(1);

            if ( ((LA6_0>=EQUAL && LA6_0<=MATCHES)) ) {
                alt6=1;
            }
            switch (alt6) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:85:39: relationalOperator sumExpression
                    {
                    pushFollow(FOLLOW_relationalOperator_in_relationalExpression389);
                    relationalOperator28=relationalOperator();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) root_0 = (Object)adaptor.becomeRoot(relationalOperator28.getTree(), root_0);
                    pushFollow(FOLLOW_sumExpression_in_relationalExpression392);
                    sumExpression29=sumExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, sumExpression29.getTree());

                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end relationalExpression

    public static class sumExpression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start sumExpression
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:87:1: sumExpression : productExpression ( ( PLUS | MINUS ) productExpression )* ;
    public final sumExpression_return sumExpression() throws RecognitionException {
        sumExpression_return retval = new sumExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token PLUS31=null;
        Token MINUS32=null;
        productExpression_return productExpression30 = null;

        productExpression_return productExpression33 = null;


        Object PLUS31_tree=null;
        Object MINUS32_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:88:2: ( productExpression ( ( PLUS | MINUS ) productExpression )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:88:4: productExpression ( ( PLUS | MINUS ) productExpression )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_productExpression_in_sumExpression403);
            productExpression30=productExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, productExpression30.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:88:22: ( ( PLUS | MINUS ) productExpression )*
            loop8:
            do {
                int alt8=2;
                int LA8_0 = input.LA(1);

                if ( ((LA8_0>=PLUS && LA8_0<=MINUS)) ) {
                    alt8=1;
                }


                switch (alt8) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:88:24: ( PLUS | MINUS ) productExpression
            	    {
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:88:24: ( PLUS | MINUS )
            	    int alt7=2;
            	    int LA7_0 = input.LA(1);

            	    if ( (LA7_0==PLUS) ) {
            	        alt7=1;
            	    }
            	    else if ( (LA7_0==MINUS) ) {
            	        alt7=2;
            	    }
            	    else {
            	        if (backtracking>0) {failed=true; return retval;}
            	        NoViableAltException nvae =
            	            new NoViableAltException("88:24: ( PLUS | MINUS )", 7, 0, input);

            	        throw nvae;
            	    }
            	    switch (alt7) {
            	        case 1 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:88:25: PLUS
            	            {
            	            PLUS31=(Token)input.LT(1);
            	            match(input,PLUS,FOLLOW_PLUS_in_sumExpression408); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            PLUS31_tree = (Object)adaptor.create(PLUS31);
            	            root_0 = (Object)adaptor.becomeRoot(PLUS31_tree, root_0);
            	            }

            	            }
            	            break;
            	        case 2 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:88:33: MINUS
            	            {
            	            MINUS32=(Token)input.LT(1);
            	            match(input,MINUS,FOLLOW_MINUS_in_sumExpression413); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            MINUS32_tree = (Object)adaptor.create(MINUS32);
            	            root_0 = (Object)adaptor.becomeRoot(MINUS32_tree, root_0);
            	            }

            	            }
            	            break;

            	    }

            	    pushFollow(FOLLOW_productExpression_in_sumExpression417);
            	    productExpression33=productExpression();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, productExpression33.getTree());

            	    }
            	    break;

            	default :
            	    break loop8;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end sumExpression

    public static class productExpression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start productExpression
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:94:1: productExpression : powerExpr ( ( STAR | DIV | MOD ) powerExpr )* ;
    public final productExpression_return productExpression() throws RecognitionException {
        productExpression_return retval = new productExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token STAR35=null;
        Token DIV36=null;
        Token MOD37=null;
        powerExpr_return powerExpr34 = null;

        powerExpr_return powerExpr38 = null;


        Object STAR35_tree=null;
        Object DIV36_tree=null;
        Object MOD37_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:95:2: ( powerExpr ( ( STAR | DIV | MOD ) powerExpr )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:95:4: powerExpr ( ( STAR | DIV | MOD ) powerExpr )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_powerExpr_in_productExpression432);
            powerExpr34=powerExpr();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, powerExpr34.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:95:14: ( ( STAR | DIV | MOD ) powerExpr )*
            loop10:
            do {
                int alt10=2;
                int LA10_0 = input.LA(1);

                if ( ((LA10_0>=STAR && LA10_0<=MOD)) ) {
                    alt10=1;
                }


                switch (alt10) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:95:15: ( STAR | DIV | MOD ) powerExpr
            	    {
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:95:15: ( STAR | DIV | MOD )
            	    int alt9=3;
            	    switch ( input.LA(1) ) {
            	    case STAR:
            	        {
            	        alt9=1;
            	        }
            	        break;
            	    case DIV:
            	        {
            	        alt9=2;
            	        }
            	        break;
            	    case MOD:
            	        {
            	        alt9=3;
            	        }
            	        break;
            	    default:
            	        if (backtracking>0) {failed=true; return retval;}
            	        NoViableAltException nvae =
            	            new NoViableAltException("95:15: ( STAR | DIV | MOD )", 9, 0, input);

            	        throw nvae;
            	    }

            	    switch (alt9) {
            	        case 1 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:95:16: STAR
            	            {
            	            STAR35=(Token)input.LT(1);
            	            match(input,STAR,FOLLOW_STAR_in_productExpression436); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            STAR35_tree = (Object)adaptor.create(STAR35);
            	            root_0 = (Object)adaptor.becomeRoot(STAR35_tree, root_0);
            	            }

            	            }
            	            break;
            	        case 2 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:95:24: DIV
            	            {
            	            DIV36=(Token)input.LT(1);
            	            match(input,DIV,FOLLOW_DIV_in_productExpression441); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            DIV36_tree = (Object)adaptor.create(DIV36);
            	            root_0 = (Object)adaptor.becomeRoot(DIV36_tree, root_0);
            	            }

            	            }
            	            break;
            	        case 3 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:95:30: MOD
            	            {
            	            MOD37=(Token)input.LT(1);
            	            match(input,MOD,FOLLOW_MOD_in_productExpression445); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            MOD37_tree = (Object)adaptor.create(MOD37);
            	            root_0 = (Object)adaptor.becomeRoot(MOD37_tree, root_0);
            	            }

            	            }
            	            break;

            	    }

            	    pushFollow(FOLLOW_powerExpr_in_productExpression449);
            	    powerExpr38=powerExpr();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, powerExpr38.getTree());

            	    }
            	    break;

            	default :
            	    break loop10;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end productExpression

    public static class powerExpr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start powerExpr
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:101:1: powerExpr : unaryExpression ( POWER unaryExpression )? ;
    public final powerExpr_return powerExpr() throws RecognitionException {
        powerExpr_return retval = new powerExpr_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token POWER40=null;
        unaryExpression_return unaryExpression39 = null;

        unaryExpression_return unaryExpression41 = null;


        Object POWER40_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:101:12: ( unaryExpression ( POWER unaryExpression )? )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:101:14: unaryExpression ( POWER unaryExpression )?
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_unaryExpression_in_powerExpr465);
            unaryExpression39=unaryExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, unaryExpression39.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:101:30: ( POWER unaryExpression )?
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( (LA11_0==POWER) ) {
                alt11=1;
            }
            switch (alt11) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:101:31: POWER unaryExpression
                    {
                    POWER40=(Token)input.LT(1);
                    match(input,POWER,FOLLOW_POWER_in_powerExpr468); if (failed) return retval;
                    if ( backtracking==0 ) {
                    POWER40_tree = (Object)adaptor.create(POWER40);
                    root_0 = (Object)adaptor.becomeRoot(POWER40_tree, root_0);
                    }
                    pushFollow(FOLLOW_unaryExpression_in_powerExpr471);
                    unaryExpression41=unaryExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, unaryExpression41.getTree());

                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end powerExpr

    public static class unaryExpression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start unaryExpression
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:103:1: unaryExpression : ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression );
    public final unaryExpression_return unaryExpression() throws RecognitionException {
        unaryExpression_return retval = new unaryExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token PLUS42=null;
        Token MINUS43=null;
        Token BANG44=null;
        unaryExpression_return unaryExpression45 = null;

        primaryExpression_return primaryExpression46 = null;


        Object PLUS42_tree=null;
        Object MINUS43_tree=null;
        Object BANG44_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:104:2: ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression )
            int alt13=2;
            int LA13_0 = input.LA(1);

            if ( ((LA13_0>=PLUS && LA13_0<=MINUS)||LA13_0==BANG) ) {
                alt13=1;
            }
            else if ( (LA13_0==INTEGER_LITERAL||LA13_0==LPAREN||(LA13_0>=POUND && LA13_0<=DOLLAR)||(LA13_0>=AT && LA13_0<=LBRACKET)||LA13_0==PROJECT||(LA13_0>=SELECT && LA13_0<=LAMBDA)||(LA13_0>=LCURLY && LA13_0<=FALSE)||(LA13_0>=91 && LA13_0<=92)) ) {
                alt13=2;
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("103:1: unaryExpression : ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression );", 13, 0, input);

                throw nvae;
            }
            switch (alt13) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:104:4: ( PLUS | MINUS | BANG ) unaryExpression
                    {
                    root_0 = (Object)adaptor.nil();

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:104:4: ( PLUS | MINUS | BANG )
                    int alt12=3;
                    switch ( input.LA(1) ) {
                    case PLUS:
                        {
                        alt12=1;
                        }
                        break;
                    case MINUS:
                        {
                        alt12=2;
                        }
                        break;
                    case BANG:
                        {
                        alt12=3;
                        }
                        break;
                    default:
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("104:4: ( PLUS | MINUS | BANG )", 12, 0, input);

                        throw nvae;
                    }

                    switch (alt12) {
                        case 1 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:104:5: PLUS
                            {
                            PLUS42=(Token)input.LT(1);
                            match(input,PLUS,FOLLOW_PLUS_in_unaryExpression485); if (failed) return retval;
                            if ( backtracking==0 ) {
                            PLUS42_tree = (Object)adaptor.create(PLUS42);
                            root_0 = (Object)adaptor.becomeRoot(PLUS42_tree, root_0);
                            }

                            }
                            break;
                        case 2 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:104:13: MINUS
                            {
                            MINUS43=(Token)input.LT(1);
                            match(input,MINUS,FOLLOW_MINUS_in_unaryExpression490); if (failed) return retval;
                            if ( backtracking==0 ) {
                            MINUS43_tree = (Object)adaptor.create(MINUS43);
                            root_0 = (Object)adaptor.becomeRoot(MINUS43_tree, root_0);
                            }

                            }
                            break;
                        case 3 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:104:22: BANG
                            {
                            BANG44=(Token)input.LT(1);
                            match(input,BANG,FOLLOW_BANG_in_unaryExpression495); if (failed) return retval;
                            if ( backtracking==0 ) {
                            BANG44_tree = (Object)adaptor.create(BANG44);
                            root_0 = (Object)adaptor.becomeRoot(BANG44_tree, root_0);
                            }

                            }
                            break;

                    }

                    pushFollow(FOLLOW_unaryExpression_in_unaryExpression499);
                    unaryExpression45=unaryExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, unaryExpression45.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:105:4: primaryExpression
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_primaryExpression_in_unaryExpression505);
                    primaryExpression46=primaryExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, primaryExpression46.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end unaryExpression

    public static class primaryExpression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start primaryExpression
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:107:1: primaryExpression : startNode ( node )? -> ^( EXPRESSION startNode ( node )? ) ;
    public final primaryExpression_return primaryExpression() throws RecognitionException {
        primaryExpression_return retval = new primaryExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        startNode_return startNode47 = null;

        node_return node48 = null;


        RewriteRuleSubtreeStream stream_node=new RewriteRuleSubtreeStream(adaptor,"rule node");
        RewriteRuleSubtreeStream stream_startNode=new RewriteRuleSubtreeStream(adaptor,"rule startNode");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:108:5: ( startNode ( node )? -> ^( EXPRESSION startNode ( node )? ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:108:7: startNode ( node )?
            {
            pushFollow(FOLLOW_startNode_in_primaryExpression519);
            startNode47=startNode();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_startNode.add(startNode47.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:108:17: ( node )?
            int alt14=2;
            int LA14_0 = input.LA(1);

            if ( (LA14_0==DOT||LA14_0==LBRACKET) ) {
                alt14=1;
            }
            switch (alt14) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:108:18: node
                    {
                    pushFollow(FOLLOW_node_in_primaryExpression522);
                    node48=node();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_node.add(node48.getTree());

                    }
                    break;

            }


            // AST REWRITE
            // elements: node, startNode
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 108:25: -> ^( EXPRESSION startNode ( node )? )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:108:28: ^( EXPRESSION startNode ( node )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(EXPRESSION, "EXPRESSION"), root_1);

                adaptor.addChild(root_1, stream_startNode.next());
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:108:51: ( node )?
                if ( stream_node.hasNext() ) {
                    adaptor.addChild(root_1, stream_node.next());

                }
                stream_node.reset();

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end primaryExpression

    public static class startNode_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start startNode
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );
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
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:111:5: ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda )
            int alt15=17;
            switch ( input.LA(1) ) {
            case LPAREN:
                {
                switch ( input.LA(2) ) {
                case PLUS:
                    {
                    int LA15_23 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 23, input);

                        throw nvae;
                    }
                    }
                    break;
                case MINUS:
                    {
                    int LA15_24 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 24, input);

                        throw nvae;
                    }
                    }
                    break;
                case BANG:
                    {
                    int LA15_25 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 25, input);

                        throw nvae;
                    }
                    }
                    break;
                case LPAREN:
                    {
                    int LA15_26 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 26, input);

                        throw nvae;
                    }
                    }
                    break;
                case ID:
                    {
                    int LA15_27 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 27, input);

                        throw nvae;
                    }
                    }
                    break;
                case POUND:
                    {
                    int LA15_28 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 28, input);

                        throw nvae;
                    }
                    }
                    break;
                case DOLLAR:
                    {
                    int LA15_29 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 29, input);

                        throw nvae;
                    }
                    }
                    break;
                case AT:
                    {
                    int LA15_30 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 30, input);

                        throw nvae;
                    }
                    }
                    break;
                case LBRACKET:
                    {
                    int LA15_31 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 31, input);

                        throw nvae;
                    }
                    }
                    break;
                case INTEGER_LITERAL:
                    {
                    int LA15_32 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 32, input);

                        throw nvae;
                    }
                    }
                    break;
                case STRING_LITERAL:
                    {
                    int LA15_33 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 33, input);

                        throw nvae;
                    }
                    }
                    break;
                case DQ_STRING_LITERAL:
                    {
                    int LA15_34 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 34, input);

                        throw nvae;
                    }
                    }
                    break;
                case TRUE:
                case FALSE:
                    {
                    int LA15_35 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 35, input);

                        throw nvae;
                    }
                    }
                    break;
                case NULL_LITERAL:
                    {
                    int LA15_36 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 36, input);

                        throw nvae;
                    }
                    }
                    break;
                case HEXADECIMAL_INTEGER_LITERAL:
                    {
                    int LA15_37 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 37, input);

                        throw nvae;
                    }
                    }
                    break;
                case REAL_LITERAL:
                    {
                    int LA15_38 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 38, input);

                        throw nvae;
                    }
                    }
                    break;
                case 92:
                    {
                    int LA15_39 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 39, input);

                        throw nvae;
                    }
                    }
                    break;
                case TYPE:
                    {
                    int LA15_40 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 40, input);

                        throw nvae;
                    }
                    }
                    break;
                case 91:
                    {
                    int LA15_41 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 41, input);

                        throw nvae;
                    }
                    }
                    break;
                case PROJECT:
                    {
                    int LA15_42 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 42, input);

                        throw nvae;
                    }
                    }
                    break;
                case SELECT:
                    {
                    int LA15_43 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 43, input);

                        throw nvae;
                    }
                    }
                    break;
                case SELECT_FIRST:
                    {
                    int LA15_44 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 44, input);

                        throw nvae;
                    }
                    }
                    break;
                case SELECT_LAST:
                    {
                    int LA15_45 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 45, input);

                        throw nvae;
                    }
                    }
                    break;
                case LCURLY:
                    {
                    int LA15_46 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 46, input);

                        throw nvae;
                    }
                    }
                    break;
                case LAMBDA:
                    {
                    int LA15_47 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt15=1;
                    }
                    else if ( (true) ) {
                        alt15=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 47, input);

                        throw nvae;
                    }
                    }
                    break;
                default:
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 1, input);

                    throw nvae;
                }

                }
                break;
            case ID:
                {
                alt15=3;
                }
                break;
            case POUND:
                {
                int LA15_3 = input.LA(2);

                if ( (LA15_3==ID) ) {
                    alt15=4;
                }
                else if ( (LA15_3==LCURLY) ) {
                    alt15=16;
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 3, input);

                    throw nvae;
                }
                }
                break;
            case DOLLAR:
                {
                alt15=5;
                }
                break;
            case AT:
                {
                alt15=6;
                }
                break;
            case LBRACKET:
                {
                alt15=7;
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
            case 92:
                {
                alt15=8;
                }
                break;
            case TYPE:
                {
                alt15=9;
                }
                break;
            case 91:
                {
                alt15=10;
                }
                break;
            case PROJECT:
                {
                alt15=11;
                }
                break;
            case SELECT:
                {
                alt15=12;
                }
                break;
            case SELECT_FIRST:
                {
                alt15=13;
                }
                break;
            case SELECT_LAST:
                {
                alt15=14;
                }
                break;
            case LCURLY:
                {
                alt15=15;
                }
                break;
            case LAMBDA:
                {
                alt15=17;
                }
                break;
            default:
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("110:1: startNode : ( ( LPAREN expression SEMI )=> exprList | parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 15, 0, input);

                throw nvae;
            }

            switch (alt15) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:112:5: ( LPAREN expression SEMI )=> exprList
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_exprList_in_startNode565);
                    exprList49=exprList();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, exprList49.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:113:7: parenExpr
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_parenExpr_in_startNode574);
                    parenExpr50=parenExpr();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, parenExpr50.getTree());

                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:114:7: methodOrProperty
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_methodOrProperty_in_startNode582);
                    methodOrProperty51=methodOrProperty();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, methodOrProperty51.getTree());

                    }
                    break;
                case 4 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:115:7: functionOrVar
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_functionOrVar_in_startNode591);
                    functionOrVar52=functionOrVar();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, functionOrVar52.getTree());

                    }
                    break;
                case 5 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:116:7: localFunctionOrVar
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_localFunctionOrVar_in_startNode599);
                    localFunctionOrVar53=localFunctionOrVar();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, localFunctionOrVar53.getTree());

                    }
                    break;
                case 6 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:117:7: reference
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_reference_in_startNode607);
                    reference54=reference();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, reference54.getTree());

                    }
                    break;
                case 7 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:118:7: indexer
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_indexer_in_startNode615);
                    indexer55=indexer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, indexer55.getTree());

                    }
                    break;
                case 8 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:119:7: literal
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_literal_in_startNode623);
                    literal56=literal();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, literal56.getTree());

                    }
                    break;
                case 9 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:120:7: type
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_type_in_startNode631);
                    type57=type();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, type57.getTree());

                    }
                    break;
                case 10 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:121:7: constructor
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_constructor_in_startNode639);
                    constructor58=constructor();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, constructor58.getTree());

                    }
                    break;
                case 11 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:122:7: projection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_projection_in_startNode647);
                    projection59=projection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, projection59.getTree());

                    }
                    break;
                case 12 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:123:7: selection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_selection_in_startNode656);
                    selection60=selection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, selection60.getTree());

                    }
                    break;
                case 13 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:124:7: firstSelection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_firstSelection_in_startNode665);
                    firstSelection61=firstSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, firstSelection61.getTree());

                    }
                    break;
                case 14 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:125:7: lastSelection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_lastSelection_in_startNode673);
                    lastSelection62=lastSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, lastSelection62.getTree());

                    }
                    break;
                case 15 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:126:7: listInitializer
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_listInitializer_in_startNode681);
                    listInitializer63=listInitializer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, listInitializer63.getTree());

                    }
                    break;
                case 16 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:127:7: mapInitializer
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_mapInitializer_in_startNode689);
                    mapInitializer64=mapInitializer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, mapInitializer64.getTree());

                    }
                    break;
                case 17 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:128:7: lambda
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_lambda_in_startNode697);
                    lambda65=lambda();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, lambda65.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end startNode

    public static class node_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start node
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:132:1: node : ( ( DOT dottedNode ) | nonDottedNode )+ ;
    public final node_return node() throws RecognitionException {
        node_return retval = new node_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token DOT66=null;
        dottedNode_return dottedNode67 = null;

        nonDottedNode_return nonDottedNode68 = null;


        Object DOT66_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:133:2: ( ( ( DOT dottedNode ) | nonDottedNode )+ )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:133:4: ( ( DOT dottedNode ) | nonDottedNode )+
            {
            root_0 = (Object)adaptor.nil();

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:133:4: ( ( DOT dottedNode ) | nonDottedNode )+
            int cnt16=0;
            loop16:
            do {
                int alt16=3;
                int LA16_0 = input.LA(1);

                if ( (LA16_0==DOT) ) {
                    alt16=1;
                }
                else if ( (LA16_0==LBRACKET) ) {
                    alt16=2;
                }


                switch (alt16) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:133:5: ( DOT dottedNode )
            	    {
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:133:5: ( DOT dottedNode )
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:133:6: DOT dottedNode
            	    {
            	    DOT66=(Token)input.LT(1);
            	    match(input,DOT,FOLLOW_DOT_in_node718); if (failed) return retval;
            	    if ( backtracking==0 ) {
            	    DOT66_tree = (Object)adaptor.create(DOT66);
            	    adaptor.addChild(root_0, DOT66_tree);
            	    }
            	    pushFollow(FOLLOW_dottedNode_in_node720);
            	    dottedNode67=dottedNode();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, dottedNode67.getTree());

            	    }


            	    }
            	    break;
            	case 2 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:133:24: nonDottedNode
            	    {
            	    pushFollow(FOLLOW_nonDottedNode_in_node725);
            	    nonDottedNode68=nonDottedNode();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, nonDottedNode68.getTree());

            	    }
            	    break;

            	default :
            	    if ( cnt16 >= 1 ) break loop16;
            	    if (backtracking>0) {failed=true; return retval;}
                        EarlyExitException eee =
                            new EarlyExitException(16, input);
                        throw eee;
                }
                cnt16++;
            } while (true);


            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end node

    public static class nonDottedNode_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start nonDottedNode
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:135:1: nonDottedNode : indexer ;
    public final nonDottedNode_return nonDottedNode() throws RecognitionException {
        nonDottedNode_return retval = new nonDottedNode_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        indexer_return indexer69 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:2: ( indexer )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:4: indexer
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_indexer_in_nonDottedNode737);
            indexer69=indexer();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, indexer69.getTree());

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end nonDottedNode

    public static class dottedNode_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start dottedNode
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:138:1: dottedNode : ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection | exprList ) ) ;
    public final dottedNode_return dottedNode() throws RecognitionException {
        dottedNode_return retval = new dottedNode_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        methodOrProperty_return methodOrProperty70 = null;

        functionOrVar_return functionOrVar71 = null;

        projection_return projection72 = null;

        selection_return selection73 = null;

        firstSelection_return firstSelection74 = null;

        lastSelection_return lastSelection75 = null;

        exprList_return exprList76 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:139:2: ( ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection | exprList ) ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:140:2: ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection | exprList ) )
            {
            root_0 = (Object)adaptor.nil();

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:140:2: ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection | exprList ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:140:3: ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection | exprList )
            {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:140:3: ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection | exprList )
            int alt17=7;
            switch ( input.LA(1) ) {
            case ID:
                {
                alt17=1;
                }
                break;
            case POUND:
                {
                alt17=2;
                }
                break;
            case PROJECT:
                {
                alt17=3;
                }
                break;
            case SELECT:
                {
                alt17=4;
                }
                break;
            case SELECT_FIRST:
                {
                alt17=5;
                }
                break;
            case SELECT_LAST:
                {
                alt17=6;
                }
                break;
            case LPAREN:
                {
                alt17=7;
                }
                break;
            default:
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("140:3: ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection | exprList )", 17, 0, input);

                throw nvae;
            }

            switch (alt17) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:140:4: methodOrProperty
                    {
                    pushFollow(FOLLOW_methodOrProperty_in_dottedNode750);
                    methodOrProperty70=methodOrProperty();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, methodOrProperty70.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:141:4: functionOrVar
                    {
                    pushFollow(FOLLOW_functionOrVar_in_dottedNode756);
                    functionOrVar71=functionOrVar();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, functionOrVar71.getTree());

                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:142:7: projection
                    {
                    pushFollow(FOLLOW_projection_in_dottedNode764);
                    projection72=projection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, projection72.getTree());

                    }
                    break;
                case 4 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:143:7: selection
                    {
                    pushFollow(FOLLOW_selection_in_dottedNode773);
                    selection73=selection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, selection73.getTree());

                    }
                    break;
                case 5 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:144:7: firstSelection
                    {
                    pushFollow(FOLLOW_firstSelection_in_dottedNode782);
                    firstSelection74=firstSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, firstSelection74.getTree());

                    }
                    break;
                case 6 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:145:7: lastSelection
                    {
                    pushFollow(FOLLOW_lastSelection_in_dottedNode791);
                    lastSelection75=lastSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, lastSelection75.getTree());

                    }
                    break;
                case 7 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:146:7: exprList
                    {
                    pushFollow(FOLLOW_exprList_in_dottedNode800);
                    exprList76=exprList();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, exprList76.getTree());

                    }
                    break;

            }


            }


            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end dottedNode

    public static class functionOrVar_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start functionOrVar
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:150:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );
    public final functionOrVar_return functionOrVar() throws RecognitionException {
        functionOrVar_return retval = new functionOrVar_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        function_return function77 = null;

        var_return var78 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:151:5: ( ( POUND ID LPAREN )=> function | var )
            int alt18=2;
            int LA18_0 = input.LA(1);

            if ( (LA18_0==POUND) ) {
                int LA18_1 = input.LA(2);

                if ( (LA18_1==ID) ) {
                    int LA18_2 = input.LA(3);

                    if ( (synpred2()) ) {
                        alt18=1;
                    }
                    else if ( (true) ) {
                        alt18=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("150:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 18, 2, input);

                        throw nvae;
                    }
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("150:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 18, 1, input);

                    throw nvae;
                }
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("150:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 18, 0, input);

                throw nvae;
            }
            switch (alt18) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:151:7: ( POUND ID LPAREN )=> function
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_function_in_functionOrVar833);
                    function77=function();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, function77.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:152:7: var
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_var_in_functionOrVar841);
                    var78=var();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, var78.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end functionOrVar

    public static class function_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start function
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:155:1: function : POUND id= ID methodArgs -> ^( FUNCTIONREF[$id] methodArgs ) ;
    public final function_return function() throws RecognitionException {
        function_return retval = new function_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token POUND79=null;
        methodArgs_return methodArgs80 = null;


        Object id_tree=null;
        Object POUND79_tree=null;
        RewriteRuleTokenStream stream_POUND=new RewriteRuleTokenStream(adaptor,"token POUND");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleSubtreeStream stream_methodArgs=new RewriteRuleSubtreeStream(adaptor,"rule methodArgs");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:155:10: ( POUND id= ID methodArgs -> ^( FUNCTIONREF[$id] methodArgs ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:155:12: POUND id= ID methodArgs
            {
            POUND79=(Token)input.LT(1);
            match(input,POUND,FOLLOW_POUND_in_function858); if (failed) return retval;
            if ( backtracking==0 ) stream_POUND.add(POUND79);

            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_function862); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(id);

            pushFollow(FOLLOW_methodArgs_in_function864);
            methodArgs80=methodArgs();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_methodArgs.add(methodArgs80.getTree());

            // AST REWRITE
            // elements: methodArgs
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 155:35: -> ^( FUNCTIONREF[$id] methodArgs )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:155:38: ^( FUNCTIONREF[$id] methodArgs )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(FUNCTIONREF, id), root_1);

                adaptor.addChild(root_1, stream_methodArgs.next());

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end function

    public static class var_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start var
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:157:1: var : POUND id= ID -> ^( VARIABLEREF[$id] ) ;
    public final var_return var() throws RecognitionException {
        var_return retval = new var_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token POUND81=null;

        Object id_tree=null;
        Object POUND81_tree=null;
        RewriteRuleTokenStream stream_POUND=new RewriteRuleTokenStream(adaptor,"token POUND");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:157:5: ( POUND id= ID -> ^( VARIABLEREF[$id] ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:157:7: POUND id= ID
            {
            POUND81=(Token)input.LT(1);
            match(input,POUND,FOLLOW_POUND_in_var885); if (failed) return retval;
            if ( backtracking==0 ) stream_POUND.add(POUND81);

            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_var889); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(id);


            // AST REWRITE
            // elements:
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 157:19: -> ^( VARIABLEREF[$id] )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:157:22: ^( VARIABLEREF[$id] )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(VARIABLEREF, id), root_1);

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end var

    public static class localFunctionOrVar_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start localFunctionOrVar
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:159:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );
    public final localFunctionOrVar_return localFunctionOrVar() throws RecognitionException {
        localFunctionOrVar_return retval = new localFunctionOrVar_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        localFunction_return localFunction82 = null;

        localVar_return localVar83 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:160:2: ( ( DOLLAR ID LPAREN )=> localFunction | localVar )
            int alt19=2;
            int LA19_0 = input.LA(1);

            if ( (LA19_0==DOLLAR) ) {
                int LA19_1 = input.LA(2);

                if ( (LA19_1==ID) ) {
                    int LA19_2 = input.LA(3);

                    if ( (synpred3()) ) {
                        alt19=1;
                    }
                    else if ( (true) ) {
                        alt19=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("159:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );", 19, 2, input);

                        throw nvae;
                    }
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("159:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );", 19, 1, input);

                    throw nvae;
                }
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("159:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );", 19, 0, input);

                throw nvae;
            }
            switch (alt19) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:160:4: ( DOLLAR ID LPAREN )=> localFunction
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_localFunction_in_localFunctionOrVar916);
                    localFunction82=localFunction();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, localFunction82.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:161:4: localVar
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_localVar_in_localFunctionOrVar921);
                    localVar83=localVar();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, localVar83.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end localFunctionOrVar

    public static class localFunction_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start localFunction
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:164:1: localFunction : DOLLAR id= ID methodArgs -> ^( LOCALFUNC[$id] methodArgs ) ;
    public final localFunction_return localFunction() throws RecognitionException {
        localFunction_return retval = new localFunction_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token DOLLAR84=null;
        methodArgs_return methodArgs85 = null;


        Object id_tree=null;
        Object DOLLAR84_tree=null;
        RewriteRuleTokenStream stream_DOLLAR=new RewriteRuleTokenStream(adaptor,"token DOLLAR");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleSubtreeStream stream_methodArgs=new RewriteRuleSubtreeStream(adaptor,"rule methodArgs");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:164:15: ( DOLLAR id= ID methodArgs -> ^( LOCALFUNC[$id] methodArgs ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:164:17: DOLLAR id= ID methodArgs
            {
            DOLLAR84=(Token)input.LT(1);
            match(input,DOLLAR,FOLLOW_DOLLAR_in_localFunction931); if (failed) return retval;
            if ( backtracking==0 ) stream_DOLLAR.add(DOLLAR84);

            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_localFunction935); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(id);

            pushFollow(FOLLOW_methodArgs_in_localFunction937);
            methodArgs85=methodArgs();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_methodArgs.add(methodArgs85.getTree());

            // AST REWRITE
            // elements: methodArgs
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 164:41: -> ^( LOCALFUNC[$id] methodArgs )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:164:44: ^( LOCALFUNC[$id] methodArgs )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(LOCALFUNC, id), root_1);

                adaptor.addChild(root_1, stream_methodArgs.next());

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end localFunction

    public static class localVar_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start localVar
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:165:1: localVar : DOLLAR id= ID -> ^( LOCALVAR[$id] ) ;
    public final localVar_return localVar() throws RecognitionException {
        localVar_return retval = new localVar_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token DOLLAR86=null;

        Object id_tree=null;
        Object DOLLAR86_tree=null;
        RewriteRuleTokenStream stream_DOLLAR=new RewriteRuleTokenStream(adaptor,"token DOLLAR");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:165:9: ( DOLLAR id= ID -> ^( LOCALVAR[$id] ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:165:11: DOLLAR id= ID
            {
            DOLLAR86=(Token)input.LT(1);
            match(input,DOLLAR,FOLLOW_DOLLAR_in_localVar952); if (failed) return retval;
            if ( backtracking==0 ) stream_DOLLAR.add(DOLLAR86);

            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_localVar956); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(id);


            // AST REWRITE
            // elements:
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 165:24: -> ^( LOCALVAR[$id] )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:165:27: ^( LOCALVAR[$id] )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(LOCALVAR, id), root_1);

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end localVar

    public static class methodOrProperty_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start methodOrProperty
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:167:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );
    public final methodOrProperty_return methodOrProperty() throws RecognitionException {
        methodOrProperty_return retval = new methodOrProperty_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        methodArgs_return methodArgs87 = null;

        property_return property88 = null;


        Object id_tree=null;
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleSubtreeStream stream_methodArgs=new RewriteRuleSubtreeStream(adaptor,"rule methodArgs");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:168:2: ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property )
            int alt20=2;
            int LA20_0 = input.LA(1);

            if ( (LA20_0==ID) ) {
                int LA20_1 = input.LA(2);

                if ( (LA20_1==LPAREN) && (synpred4())) {
                    alt20=1;
                }
                else if ( (LA20_1==EOF||(LA20_1>=SEMI && LA20_1<=POWER)||LA20_1==DOT||LA20_1==COMMA||(LA20_1>=LBRACKET && LA20_1<=RBRACKET)||LA20_1==RCURLY||(LA20_1>=EQUAL && LA20_1<=MATCHES)) ) {
                    alt20=2;
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("167:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );", 20, 1, input);

                    throw nvae;
                }
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("167:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );", 20, 0, input);

                throw nvae;
            }
            switch (alt20) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:168:4: ( ID LPAREN )=>id= ID methodArgs
                    {
                    id=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_methodOrProperty982); if (failed) return retval;
                    if ( backtracking==0 ) stream_ID.add(id);

                    pushFollow(FOLLOW_methodArgs_in_methodOrProperty984);
                    methodArgs87=methodArgs();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_methodArgs.add(methodArgs87.getTree());

                    // AST REWRITE
                    // elements: methodArgs
                    // token labels:
                    // rule labels: retval
                    // token list labels:
                    // rule list labels:
                    if ( backtracking==0 ) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 168:36: -> ^( METHOD[$id] methodArgs )
                    {
                        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:168:39: ^( METHOD[$id] methodArgs )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(adaptor.create(METHOD, id), root_1);

                        adaptor.addChild(root_1, stream_methodArgs.next());

                        adaptor.addChild(root_0, root_1);
                        }

                    }

                    }

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:169:4: property
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_property_in_methodOrProperty998);
                    property88=property();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, property88.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end methodOrProperty

    public static class methodArgs_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start methodArgs
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:175:1: methodArgs : LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN ;
    public final methodArgs_return methodArgs() throws RecognitionException {
        methodArgs_return retval = new methodArgs_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LPAREN89=null;
        Token COMMA91=null;
        Token COMMA93=null;
        Token RPAREN94=null;
        argument_return argument90 = null;

        argument_return argument92 = null;


        Object LPAREN89_tree=null;
        Object COMMA91_tree=null;
        Object COMMA93_tree=null;
        Object RPAREN94_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:175:12: ( LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:175:15: LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN
            {
            root_0 = (Object)adaptor.nil();

            LPAREN89=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_methodArgs1013); if (failed) return retval;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:175:23: ( argument ( COMMA argument )* ( COMMA )? )?
            int alt23=2;
            int LA23_0 = input.LA(1);

            if ( (LA23_0==INTEGER_LITERAL||LA23_0==LPAREN||(LA23_0>=PLUS && LA23_0<=MINUS)||LA23_0==BANG||(LA23_0>=POUND && LA23_0<=DOLLAR)||(LA23_0>=AT && LA23_0<=LBRACKET)||LA23_0==PROJECT||(LA23_0>=SELECT && LA23_0<=LAMBDA)||(LA23_0>=LCURLY && LA23_0<=FALSE)||(LA23_0>=91 && LA23_0<=92)) ) {
                alt23=1;
            }
            switch (alt23) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:175:24: argument ( COMMA argument )* ( COMMA )?
                    {
                    pushFollow(FOLLOW_argument_in_methodArgs1017);
                    argument90=argument();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, argument90.getTree());
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:175:33: ( COMMA argument )*
                    loop21:
                    do {
                        int alt21=2;
                        int LA21_0 = input.LA(1);

                        if ( (LA21_0==COMMA) ) {
                            int LA21_1 = input.LA(2);

                            if ( (LA21_1==INTEGER_LITERAL||LA21_1==LPAREN||(LA21_1>=PLUS && LA21_1<=MINUS)||LA21_1==BANG||(LA21_1>=POUND && LA21_1<=DOLLAR)||(LA21_1>=AT && LA21_1<=LBRACKET)||LA21_1==PROJECT||(LA21_1>=SELECT && LA21_1<=LAMBDA)||(LA21_1>=LCURLY && LA21_1<=FALSE)||(LA21_1>=91 && LA21_1<=92)) ) {
                                alt21=1;
                            }


                        }


                        switch (alt21) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:175:34: COMMA argument
                    	    {
                    	    COMMA91=(Token)input.LT(1);
                    	    match(input,COMMA,FOLLOW_COMMA_in_methodArgs1020); if (failed) return retval;
                    	    pushFollow(FOLLOW_argument_in_methodArgs1023);
                    	    argument92=argument();
                    	    _fsp--;
                    	    if (failed) return retval;
                    	    if ( backtracking==0 ) adaptor.addChild(root_0, argument92.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop21;
                        }
                    } while (true);

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:175:52: ( COMMA )?
                    int alt22=2;
                    int LA22_0 = input.LA(1);

                    if ( (LA22_0==COMMA) ) {
                        alt22=1;
                    }
                    switch (alt22) {
                        case 1 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:175:53: COMMA
                            {
                            COMMA93=(Token)input.LT(1);
                            match(input,COMMA,FOLLOW_COMMA_in_methodArgs1028); if (failed) return retval;

                            }
                            break;

                    }


                    }
                    break;

            }

            RPAREN94=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_methodArgs1035); if (failed) return retval;

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end methodArgs

    public static class property_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start property
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:180:1: property : id= ID -> ^( PROPERTY_OR_FIELD[$id] ) ;
    public final property_return property() throws RecognitionException {
        property_return retval = new property_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;

        Object id_tree=null;
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:180:9: (id= ID -> ^( PROPERTY_OR_FIELD[$id] ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:180:11: id= ID
            {
            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_property1048); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(id);


            // AST REWRITE
            // elements:
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 180:17: -> ^( PROPERTY_OR_FIELD[$id] )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:180:20: ^( PROPERTY_OR_FIELD[$id] )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(PROPERTY_OR_FIELD, id), root_1);

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end property

    public static class reference_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start reference
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:187:1: reference : AT pos= LPAREN (cn= contextName COLON )? (q= qualifiedId )? RPAREN -> ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN ) ;
    public final reference_return reference() throws RecognitionException {
        reference_return retval = new reference_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token pos=null;
        Token AT95=null;
        Token COLON96=null;
        Token RPAREN97=null;
        contextName_return cn = null;

        qualifiedId_return q = null;


        Object pos_tree=null;
        Object AT95_tree=null;
        Object COLON96_tree=null;
        Object RPAREN97_tree=null;
        RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
        RewriteRuleTokenStream stream_COLON=new RewriteRuleTokenStream(adaptor,"token COLON");
        RewriteRuleTokenStream stream_LPAREN=new RewriteRuleTokenStream(adaptor,"token LPAREN");
        RewriteRuleTokenStream stream_AT=new RewriteRuleTokenStream(adaptor,"token AT");
        RewriteRuleSubtreeStream stream_contextName=new RewriteRuleSubtreeStream(adaptor,"rule contextName");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:188:2: ( AT pos= LPAREN (cn= contextName COLON )? (q= qualifiedId )? RPAREN -> ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:188:5: AT pos= LPAREN (cn= contextName COLON )? (q= qualifiedId )? RPAREN
            {
            AT95=(Token)input.LT(1);
            match(input,AT,FOLLOW_AT_in_reference1070); if (failed) return retval;
            if ( backtracking==0 ) stream_AT.add(AT95);

            pos=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_reference1074); if (failed) return retval;
            if ( backtracking==0 ) stream_LPAREN.add(pos);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:188:19: (cn= contextName COLON )?
            int alt24=2;
            int LA24_0 = input.LA(1);

            if ( (LA24_0==ID) ) {
                int LA24_1 = input.LA(2);

                if ( (LA24_1==COLON||LA24_1==DIV) ) {
                    alt24=1;
                }
            }
            switch (alt24) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:188:20: cn= contextName COLON
                    {
                    pushFollow(FOLLOW_contextName_in_reference1079);
                    cn=contextName();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_contextName.add(cn.getTree());
                    COLON96=(Token)input.LT(1);
                    match(input,COLON,FOLLOW_COLON_in_reference1081); if (failed) return retval;
                    if ( backtracking==0 ) stream_COLON.add(COLON96);


                    }
                    break;

            }

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:188:43: (q= qualifiedId )?
            int alt25=2;
            int LA25_0 = input.LA(1);

            if ( (LA25_0==ID) ) {
                alt25=1;
            }
            switch (alt25) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:188:44: q= qualifiedId
                    {
                    pushFollow(FOLLOW_qualifiedId_in_reference1088);
                    q=qualifiedId();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_qualifiedId.add(q.getTree());

                    }
                    break;

            }

            RPAREN97=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_reference1092); if (failed) return retval;
            if ( backtracking==0 ) stream_RPAREN.add(RPAREN97);


            // AST REWRITE
            // elements: cn, RPAREN, COLON, q
            // token labels:
            // rule labels: cn, retval, q
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_cn=new RewriteRuleSubtreeStream(adaptor,"token cn",cn!=null?cn.tree:null);
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);
            RewriteRuleSubtreeStream stream_q=new RewriteRuleSubtreeStream(adaptor,"token q",q!=null?q.tree:null);

            root_0 = (Object)adaptor.nil();
            // 189:4: -> ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:189:7: ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(REFERENCE, pos), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:189:25: ( $cn COLON )?
                if ( stream_cn.hasNext()||stream_COLON.hasNext() ) {
                    adaptor.addChild(root_1, stream_cn.next());
                    adaptor.addChild(root_1, stream_COLON.next());

                }
                stream_cn.reset();
                stream_COLON.reset();
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:189:38: ( $q)?
                if ( stream_q.hasNext() ) {
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

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end reference

    public static class indexer_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start indexer
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:195:1: indexer : LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET -> ^( INDEXER $r1 ( $r2)* ) ;
    public final indexer_return indexer() throws RecognitionException {
        indexer_return retval = new indexer_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LBRACKET98=null;
        Token COMMA99=null;
        Token RBRACKET100=null;
        argument_return r1 = null;

        argument_return r2 = null;


        Object LBRACKET98_tree=null;
        Object COMMA99_tree=null;
        Object RBRACKET100_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_LBRACKET=new RewriteRuleTokenStream(adaptor,"token LBRACKET");
        RewriteRuleTokenStream stream_RBRACKET=new RewriteRuleTokenStream(adaptor,"token RBRACKET");
        RewriteRuleSubtreeStream stream_argument=new RewriteRuleSubtreeStream(adaptor,"rule argument");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:195:8: ( LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET -> ^( INDEXER $r1 ( $r2)* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:195:10: LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET
            {
            LBRACKET98=(Token)input.LT(1);
            match(input,LBRACKET,FOLLOW_LBRACKET_in_indexer1127); if (failed) return retval;
            if ( backtracking==0 ) stream_LBRACKET.add(LBRACKET98);

            pushFollow(FOLLOW_argument_in_indexer1131);
            r1=argument();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_argument.add(r1.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:195:31: ( COMMA r2= argument )*
            loop26:
            do {
                int alt26=2;
                int LA26_0 = input.LA(1);

                if ( (LA26_0==COMMA) ) {
                    alt26=1;
                }


                switch (alt26) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:195:32: COMMA r2= argument
            	    {
            	    COMMA99=(Token)input.LT(1);
            	    match(input,COMMA,FOLLOW_COMMA_in_indexer1134); if (failed) return retval;
            	    if ( backtracking==0 ) stream_COMMA.add(COMMA99);

            	    pushFollow(FOLLOW_argument_in_indexer1138);
            	    r2=argument();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) stream_argument.add(r2.getTree());

            	    }
            	    break;

            	default :
            	    break loop26;
                }
            } while (true);

            RBRACKET100=(Token)input.LT(1);
            match(input,RBRACKET,FOLLOW_RBRACKET_in_indexer1142); if (failed) return retval;
            if ( backtracking==0 ) stream_RBRACKET.add(RBRACKET100);


            // AST REWRITE
            // elements: r2, r1
            // token labels:
            // rule labels: r2, retval, r1
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_r2=new RewriteRuleSubtreeStream(adaptor,"token r2",r2!=null?r2.tree:null);
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);
            RewriteRuleSubtreeStream stream_r1=new RewriteRuleSubtreeStream(adaptor,"token r1",r1!=null?r1.tree:null);

            root_0 = (Object)adaptor.nil();
            // 195:61: -> ^( INDEXER $r1 ( $r2)* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:195:64: ^( INDEXER $r1 ( $r2)* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(INDEXER, "INDEXER"), root_1);

                adaptor.addChild(root_1, stream_r1.next());
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:195:78: ( $r2)*
                while ( stream_r2.hasNext() ) {
                    adaptor.addChild(root_1, stream_r2.next());

                }
                stream_r2.reset();

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end indexer

    public static class projection_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start projection
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:1: projection : PROJECT expression RCURLY ;
    public final projection_return projection() throws RecognitionException {
        projection_return retval = new projection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token PROJECT101=null;
        Token RCURLY103=null;
        expression_return expression102 = null;


        Object PROJECT101_tree=null;
        Object RCURLY103_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:11: ( PROJECT expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:13: PROJECT expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            PROJECT101=(Token)input.LT(1);
            match(input,PROJECT,FOLLOW_PROJECT_in_projection1169); if (failed) return retval;
            if ( backtracking==0 ) {
            PROJECT101_tree = (Object)adaptor.create(PROJECT101);
            root_0 = (Object)adaptor.becomeRoot(PROJECT101_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_projection1172);
            expression102=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression102.getTree());
            RCURLY103=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_projection1174); if (failed) return retval;

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end projection

    public static class selection_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start selection
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:202:1: selection : SELECT expression RCURLY ;
    public final selection_return selection() throws RecognitionException {
        selection_return retval = new selection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token SELECT104=null;
        Token RCURLY106=null;
        expression_return expression105 = null;


        Object SELECT104_tree=null;
        Object RCURLY106_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:202:10: ( SELECT expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:202:12: SELECT expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            SELECT104=(Token)input.LT(1);
            match(input,SELECT,FOLLOW_SELECT_in_selection1182); if (failed) return retval;
            if ( backtracking==0 ) {
            SELECT104_tree = (Object)adaptor.create(SELECT104);
            root_0 = (Object)adaptor.becomeRoot(SELECT104_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_selection1185);
            expression105=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression105.getTree());
            RCURLY106=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_selection1187); if (failed) return retval;

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end selection

    public static class firstSelection_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start firstSelection
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:204:1: firstSelection : SELECT_FIRST expression RCURLY ;
    public final firstSelection_return firstSelection() throws RecognitionException {
        firstSelection_return retval = new firstSelection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token SELECT_FIRST107=null;
        Token RCURLY109=null;
        expression_return expression108 = null;


        Object SELECT_FIRST107_tree=null;
        Object RCURLY109_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:204:15: ( SELECT_FIRST expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:204:17: SELECT_FIRST expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            SELECT_FIRST107=(Token)input.LT(1);
            match(input,SELECT_FIRST,FOLLOW_SELECT_FIRST_in_firstSelection1195); if (failed) return retval;
            if ( backtracking==0 ) {
            SELECT_FIRST107_tree = (Object)adaptor.create(SELECT_FIRST107);
            root_0 = (Object)adaptor.becomeRoot(SELECT_FIRST107_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_firstSelection1198);
            expression108=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression108.getTree());
            RCURLY109=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_firstSelection1200); if (failed) return retval;

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end firstSelection

    public static class lastSelection_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start lastSelection
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:206:1: lastSelection : SELECT_LAST expression RCURLY ;
    public final lastSelection_return lastSelection() throws RecognitionException {
        lastSelection_return retval = new lastSelection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token SELECT_LAST110=null;
        Token RCURLY112=null;
        expression_return expression111 = null;


        Object SELECT_LAST110_tree=null;
        Object RCURLY112_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:206:14: ( SELECT_LAST expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:206:16: SELECT_LAST expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            SELECT_LAST110=(Token)input.LT(1);
            match(input,SELECT_LAST,FOLLOW_SELECT_LAST_in_lastSelection1208); if (failed) return retval;
            if ( backtracking==0 ) {
            SELECT_LAST110_tree = (Object)adaptor.create(SELECT_LAST110);
            root_0 = (Object)adaptor.becomeRoot(SELECT_LAST110_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_lastSelection1211);
            expression111=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression111.getTree());
            RCURLY112=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_lastSelection1213); if (failed) return retval;

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end lastSelection

    public static class type_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start type
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:209:1: type : TYPE qualifiedId RPAREN -> ^( TYPEREF qualifiedId ) ;
    public final type_return type() throws RecognitionException {
        type_return retval = new type_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token TYPE113=null;
        Token RPAREN115=null;
        qualifiedId_return qualifiedId114 = null;


        Object TYPE113_tree=null;
        Object RPAREN115_tree=null;
        RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
        RewriteRuleTokenStream stream_TYPE=new RewriteRuleTokenStream(adaptor,"token TYPE");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:209:5: ( TYPE qualifiedId RPAREN -> ^( TYPEREF qualifiedId ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:209:7: TYPE qualifiedId RPAREN
            {
            TYPE113=(Token)input.LT(1);
            match(input,TYPE,FOLLOW_TYPE_in_type1222); if (failed) return retval;
            if ( backtracking==0 ) stream_TYPE.add(TYPE113);

            pushFollow(FOLLOW_qualifiedId_in_type1224);
            qualifiedId114=qualifiedId();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_qualifiedId.add(qualifiedId114.getTree());
            RPAREN115=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_type1226); if (failed) return retval;
            if ( backtracking==0 ) stream_RPAREN.add(RPAREN115);


            // AST REWRITE
            // elements: qualifiedId
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 209:31: -> ^( TYPEREF qualifiedId )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:209:34: ^( TYPEREF qualifiedId )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(TYPEREF, "TYPEREF"), root_1);

                adaptor.addChild(root_1, stream_qualifiedId.next());

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end type

    public static class lambda_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start lambda
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:217:1: lambda : LAMBDA ( argList )? PIPE expression RCURLY -> ^( LAMBDA ( argList )? expression ) ;
    public final lambda_return lambda() throws RecognitionException {
        lambda_return retval = new lambda_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LAMBDA116=null;
        Token PIPE118=null;
        Token RCURLY120=null;
        argList_return argList117 = null;

        expression_return expression119 = null;


        Object LAMBDA116_tree=null;
        Object PIPE118_tree=null;
        Object RCURLY120_tree=null;
        RewriteRuleTokenStream stream_RCURLY=new RewriteRuleTokenStream(adaptor,"token RCURLY");
        RewriteRuleTokenStream stream_PIPE=new RewriteRuleTokenStream(adaptor,"token PIPE");
        RewriteRuleTokenStream stream_LAMBDA=new RewriteRuleTokenStream(adaptor,"token LAMBDA");
        RewriteRuleSubtreeStream stream_argList=new RewriteRuleSubtreeStream(adaptor,"rule argList");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:4: ( LAMBDA ( argList )? PIPE expression RCURLY -> ^( LAMBDA ( argList )? expression ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:8: LAMBDA ( argList )? PIPE expression RCURLY
            {
            LAMBDA116=(Token)input.LT(1);
            match(input,LAMBDA,FOLLOW_LAMBDA_in_lambda1253); if (failed) return retval;
            if ( backtracking==0 ) stream_LAMBDA.add(LAMBDA116);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:15: ( argList )?
            int alt27=2;
            int LA27_0 = input.LA(1);

            if ( (LA27_0==ID) ) {
                alt27=1;
            }
            switch (alt27) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:16: argList
                    {
                    pushFollow(FOLLOW_argList_in_lambda1256);
                    argList117=argList();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_argList.add(argList117.getTree());

                    }
                    break;

            }

            PIPE118=(Token)input.LT(1);
            match(input,PIPE,FOLLOW_PIPE_in_lambda1260); if (failed) return retval;
            if ( backtracking==0 ) stream_PIPE.add(PIPE118);

            pushFollow(FOLLOW_expression_in_lambda1262);
            expression119=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression119.getTree());
            RCURLY120=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_lambda1264); if (failed) return retval;
            if ( backtracking==0 ) stream_RCURLY.add(RCURLY120);


            // AST REWRITE
            // elements: argList, expression, LAMBDA
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 218:49: -> ^( LAMBDA ( argList )? expression )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:52: ^( LAMBDA ( argList )? expression )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(stream_LAMBDA.next(), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:61: ( argList )?
                if ( stream_argList.hasNext() ) {
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

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end lambda

    public static class argList_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start argList
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:220:1: argList : (id+= ID ( COMMA id+= ID )* ) -> ^( ARGLIST ( $id)* ) ;
    public final argList_return argList() throws RecognitionException {
        argList_return retval = new argList_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token COMMA121=null;
        Token id=null;
        List list_id=null;

        Object COMMA121_tree=null;
        Object id_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:220:9: ( (id+= ID ( COMMA id+= ID )* ) -> ^( ARGLIST ( $id)* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:220:11: (id+= ID ( COMMA id+= ID )* )
            {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:220:11: (id+= ID ( COMMA id+= ID )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:220:12: id+= ID ( COMMA id+= ID )*
            {
            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_argList1288); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(id);

            if (list_id==null) list_id=new ArrayList();
            list_id.add(id);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:220:19: ( COMMA id+= ID )*
            loop28:
            do {
                int alt28=2;
                int LA28_0 = input.LA(1);

                if ( (LA28_0==COMMA) ) {
                    alt28=1;
                }


                switch (alt28) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:220:20: COMMA id+= ID
            	    {
            	    COMMA121=(Token)input.LT(1);
            	    match(input,COMMA,FOLLOW_COMMA_in_argList1291); if (failed) return retval;
            	    if ( backtracking==0 ) stream_COMMA.add(COMMA121);

            	    id=(Token)input.LT(1);
            	    match(input,ID,FOLLOW_ID_in_argList1295); if (failed) return retval;
            	    if ( backtracking==0 ) stream_ID.add(id);

            	    if (list_id==null) list_id=new ArrayList();
            	    list_id.add(id);


            	    }
            	    break;

            	default :
            	    break loop28;
                }
            } while (true);


            }


            // AST REWRITE
            // elements: id
            // token labels:
            // rule labels: retval
            // token list labels: id
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleTokenStream stream_id=new RewriteRuleTokenStream(adaptor,"token id", list_id);
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 220:36: -> ^( ARGLIST ( $id)* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:220:39: ^( ARGLIST ( $id)* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(ARGLIST, "ARGLIST"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:220:49: ( $id)*
                while ( stream_id.hasNext() ) {
                    adaptor.addChild(root_1, stream_id.next());

                }
                stream_id.reset();

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end argList

    public static class constructor_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start constructor
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:222:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );
    public final constructor_return constructor() throws RecognitionException {
        constructor_return retval = new constructor_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal122=null;
        qualifiedId_return qualifiedId123 = null;

        ctorArgs_return ctorArgs124 = null;

        arrayConstructor_return arrayConstructor125 = null;


        Object string_literal122_tree=null;
        RewriteRuleTokenStream stream_91=new RewriteRuleTokenStream(adaptor,"token 91");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        RewriteRuleSubtreeStream stream_ctorArgs=new RewriteRuleSubtreeStream(adaptor,"rule ctorArgs");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:223:2: ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor )
            int alt29=2;
            int LA29_0 = input.LA(1);

            if ( (LA29_0==91) ) {
                int LA29_1 = input.LA(2);

                if ( (LA29_1==ID) ) {
                    int LA29_2 = input.LA(3);

                    if ( (synpred5()) ) {
                        alt29=1;
                    }
                    else if ( (true) ) {
                        alt29=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("222:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );", 29, 2, input);

                        throw nvae;
                    }
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("222:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );", 29, 1, input);

                    throw nvae;
                }
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("222:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );", 29, 0, input);

                throw nvae;
            }
            switch (alt29) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:223:4: ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs
                    {
                    string_literal122=(Token)input.LT(1);
                    match(input,91,FOLLOW_91_in_constructor1331); if (failed) return retval;
                    if ( backtracking==0 ) stream_91.add(string_literal122);

                    pushFollow(FOLLOW_qualifiedId_in_constructor1333);
                    qualifiedId123=qualifiedId();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_qualifiedId.add(qualifiedId123.getTree());
                    pushFollow(FOLLOW_ctorArgs_in_constructor1335);
                    ctorArgs124=ctorArgs();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_ctorArgs.add(ctorArgs124.getTree());

                    // AST REWRITE
                    // elements: ctorArgs, qualifiedId
                    // token labels:
                    // rule labels: retval
                    // token list labels:
                    // rule list labels:
                    if ( backtracking==0 ) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 223:61: -> ^( CONSTRUCTOR qualifiedId ctorArgs )
                    {
                        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:223:64: ^( CONSTRUCTOR qualifiedId ctorArgs )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(adaptor.create(CONSTRUCTOR, "CONSTRUCTOR"), root_1);

                        adaptor.addChild(root_1, stream_qualifiedId.next());
                        adaptor.addChild(root_1, stream_ctorArgs.next());

                        adaptor.addChild(root_0, root_1);
                        }

                    }

                    }

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:224:6: arrayConstructor
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_arrayConstructor_in_constructor1352);
                    arrayConstructor125=arrayConstructor();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, arrayConstructor125.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end constructor

    public static class arrayConstructor_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start arrayConstructor
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:227:1: arrayConstructor : 'new' qualifiedId arrayRank ( listInitializer )? -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? ) ;
    public final arrayConstructor_return arrayConstructor() throws RecognitionException {
        arrayConstructor_return retval = new arrayConstructor_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal126=null;
        qualifiedId_return qualifiedId127 = null;

        arrayRank_return arrayRank128 = null;

        listInitializer_return listInitializer129 = null;


        Object string_literal126_tree=null;
        RewriteRuleTokenStream stream_91=new RewriteRuleTokenStream(adaptor,"token 91");
        RewriteRuleSubtreeStream stream_listInitializer=new RewriteRuleSubtreeStream(adaptor,"rule listInitializer");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        RewriteRuleSubtreeStream stream_arrayRank=new RewriteRuleSubtreeStream(adaptor,"rule arrayRank");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:228:2: ( 'new' qualifiedId arrayRank ( listInitializer )? -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:228:4: 'new' qualifiedId arrayRank ( listInitializer )?
            {
            string_literal126=(Token)input.LT(1);
            match(input,91,FOLLOW_91_in_arrayConstructor1363); if (failed) return retval;
            if ( backtracking==0 ) stream_91.add(string_literal126);

            pushFollow(FOLLOW_qualifiedId_in_arrayConstructor1365);
            qualifiedId127=qualifiedId();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_qualifiedId.add(qualifiedId127.getTree());
            pushFollow(FOLLOW_arrayRank_in_arrayConstructor1367);
            arrayRank128=arrayRank();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_arrayRank.add(arrayRank128.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:228:32: ( listInitializer )?
            int alt30=2;
            int LA30_0 = input.LA(1);

            if ( (LA30_0==LCURLY) ) {
                alt30=1;
            }
            switch (alt30) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:228:33: listInitializer
                    {
                    pushFollow(FOLLOW_listInitializer_in_arrayConstructor1370);
                    listInitializer129=listInitializer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_listInitializer.add(listInitializer129.getTree());

                    }
                    break;

            }


            // AST REWRITE
            // elements: qualifiedId, arrayRank, listInitializer
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 229:4: -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:229:7: ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(CONSTRUCTOR_ARRAY, "CONSTRUCTOR_ARRAY"), root_1);

                adaptor.addChild(root_1, stream_qualifiedId.next());
                adaptor.addChild(root_1, stream_arrayRank.next());
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:229:49: ( listInitializer )?
                if ( stream_listInitializer.hasNext() ) {
                    adaptor.addChild(root_1, stream_listInitializer.next());

                }
                stream_listInitializer.reset();

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end arrayConstructor

    public static class arrayRank_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start arrayRank
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:232:1: arrayRank : LBRACKET ( expression ( COMMA expression )* )? RBRACKET -> ^( EXPRESSIONLIST ( expression )* ) ;
    public final arrayRank_return arrayRank() throws RecognitionException {
        arrayRank_return retval = new arrayRank_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LBRACKET130=null;
        Token COMMA132=null;
        Token RBRACKET134=null;
        expression_return expression131 = null;

        expression_return expression133 = null;


        Object LBRACKET130_tree=null;
        Object COMMA132_tree=null;
        Object RBRACKET134_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_LBRACKET=new RewriteRuleTokenStream(adaptor,"token LBRACKET");
        RewriteRuleTokenStream stream_RBRACKET=new RewriteRuleTokenStream(adaptor,"token RBRACKET");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:233:5: ( LBRACKET ( expression ( COMMA expression )* )? RBRACKET -> ^( EXPRESSIONLIST ( expression )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:233:7: LBRACKET ( expression ( COMMA expression )* )? RBRACKET
            {
            LBRACKET130=(Token)input.LT(1);
            match(input,LBRACKET,FOLLOW_LBRACKET_in_arrayRank1405); if (failed) return retval;
            if ( backtracking==0 ) stream_LBRACKET.add(LBRACKET130);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:233:16: ( expression ( COMMA expression )* )?
            int alt32=2;
            int LA32_0 = input.LA(1);

            if ( (LA32_0==INTEGER_LITERAL||LA32_0==LPAREN||(LA32_0>=PLUS && LA32_0<=MINUS)||LA32_0==BANG||(LA32_0>=POUND && LA32_0<=DOLLAR)||(LA32_0>=AT && LA32_0<=LBRACKET)||LA32_0==PROJECT||(LA32_0>=SELECT && LA32_0<=LAMBDA)||(LA32_0>=LCURLY && LA32_0<=FALSE)||(LA32_0>=91 && LA32_0<=92)) ) {
                alt32=1;
            }
            switch (alt32) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:233:17: expression ( COMMA expression )*
                    {
                    pushFollow(FOLLOW_expression_in_arrayRank1408);
                    expression131=expression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_expression.add(expression131.getTree());
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:233:28: ( COMMA expression )*
                    loop31:
                    do {
                        int alt31=2;
                        int LA31_0 = input.LA(1);

                        if ( (LA31_0==COMMA) ) {
                            alt31=1;
                        }


                        switch (alt31) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:233:29: COMMA expression
                    	    {
                    	    COMMA132=(Token)input.LT(1);
                    	    match(input,COMMA,FOLLOW_COMMA_in_arrayRank1411); if (failed) return retval;
                    	    if ( backtracking==0 ) stream_COMMA.add(COMMA132);

                    	    pushFollow(FOLLOW_expression_in_arrayRank1413);
                    	    expression133=expression();
                    	    _fsp--;
                    	    if (failed) return retval;
                    	    if ( backtracking==0 ) stream_expression.add(expression133.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop31;
                        }
                    } while (true);


                    }
                    break;

            }

            RBRACKET134=(Token)input.LT(1);
            match(input,RBRACKET,FOLLOW_RBRACKET_in_arrayRank1419); if (failed) return retval;
            if ( backtracking==0 ) stream_RBRACKET.add(RBRACKET134);


            // AST REWRITE
            // elements: expression
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 233:59: -> ^( EXPRESSIONLIST ( expression )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:233:62: ^( EXPRESSIONLIST ( expression )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(EXPRESSIONLIST, "EXPRESSIONLIST"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:233:79: ( expression )*
                while ( stream_expression.hasNext() ) {
                    adaptor.addChild(root_1, stream_expression.next());

                }
                stream_expression.reset();

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end arrayRank

    public static class listInitializer_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start listInitializer
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:235:1: listInitializer : LCURLY expression ( COMMA expression )* RCURLY -> ^( LIST_INITIALIZER ( expression )* ) ;
    public final listInitializer_return listInitializer() throws RecognitionException {
        listInitializer_return retval = new listInitializer_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LCURLY135=null;
        Token COMMA137=null;
        Token RCURLY139=null;
        expression_return expression136 = null;

        expression_return expression138 = null;


        Object LCURLY135_tree=null;
        Object COMMA137_tree=null;
        Object RCURLY139_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_RCURLY=new RewriteRuleTokenStream(adaptor,"token RCURLY");
        RewriteRuleTokenStream stream_LCURLY=new RewriteRuleTokenStream(adaptor,"token LCURLY");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:236:5: ( LCURLY expression ( COMMA expression )* RCURLY -> ^( LIST_INITIALIZER ( expression )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:236:7: LCURLY expression ( COMMA expression )* RCURLY
            {
            LCURLY135=(Token)input.LT(1);
            match(input,LCURLY,FOLLOW_LCURLY_in_listInitializer1444); if (failed) return retval;
            if ( backtracking==0 ) stream_LCURLY.add(LCURLY135);

            pushFollow(FOLLOW_expression_in_listInitializer1446);
            expression136=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression136.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:236:25: ( COMMA expression )*
            loop33:
            do {
                int alt33=2;
                int LA33_0 = input.LA(1);

                if ( (LA33_0==COMMA) ) {
                    alt33=1;
                }


                switch (alt33) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:236:26: COMMA expression
            	    {
            	    COMMA137=(Token)input.LT(1);
            	    match(input,COMMA,FOLLOW_COMMA_in_listInitializer1449); if (failed) return retval;
            	    if ( backtracking==0 ) stream_COMMA.add(COMMA137);

            	    pushFollow(FOLLOW_expression_in_listInitializer1451);
            	    expression138=expression();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) stream_expression.add(expression138.getTree());

            	    }
            	    break;

            	default :
            	    break loop33;
                }
            } while (true);

            RCURLY139=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_listInitializer1455); if (failed) return retval;
            if ( backtracking==0 ) stream_RCURLY.add(RCURLY139);


            // AST REWRITE
            // elements: expression
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 236:52: -> ^( LIST_INITIALIZER ( expression )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:236:55: ^( LIST_INITIALIZER ( expression )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(LIST_INITIALIZER, "LIST_INITIALIZER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:236:74: ( expression )*
                while ( stream_expression.hasNext() ) {
                    adaptor.addChild(root_1, stream_expression.next());

                }
                stream_expression.reset();

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end listInitializer

    public static class mapInitializer_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start mapInitializer
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:241:1: mapInitializer : POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY -> ^( MAP_INITIALIZER ( mapEntry )* ) ;
    public final mapInitializer_return mapInitializer() throws RecognitionException {
        mapInitializer_return retval = new mapInitializer_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token POUND140=null;
        Token LCURLY141=null;
        Token COMMA143=null;
        Token RCURLY145=null;
        mapEntry_return mapEntry142 = null;

        mapEntry_return mapEntry144 = null;


        Object POUND140_tree=null;
        Object LCURLY141_tree=null;
        Object COMMA143_tree=null;
        Object RCURLY145_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_RCURLY=new RewriteRuleTokenStream(adaptor,"token RCURLY");
        RewriteRuleTokenStream stream_LCURLY=new RewriteRuleTokenStream(adaptor,"token LCURLY");
        RewriteRuleTokenStream stream_POUND=new RewriteRuleTokenStream(adaptor,"token POUND");
        RewriteRuleSubtreeStream stream_mapEntry=new RewriteRuleSubtreeStream(adaptor,"rule mapEntry");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:242:5: ( POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY -> ^( MAP_INITIALIZER ( mapEntry )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:242:7: POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY
            {
            POUND140=(Token)input.LT(1);
            match(input,POUND,FOLLOW_POUND_in_mapInitializer1483); if (failed) return retval;
            if ( backtracking==0 ) stream_POUND.add(POUND140);

            LCURLY141=(Token)input.LT(1);
            match(input,LCURLY,FOLLOW_LCURLY_in_mapInitializer1485); if (failed) return retval;
            if ( backtracking==0 ) stream_LCURLY.add(LCURLY141);

            pushFollow(FOLLOW_mapEntry_in_mapInitializer1487);
            mapEntry142=mapEntry();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_mapEntry.add(mapEntry142.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:242:29: ( COMMA mapEntry )*
            loop34:
            do {
                int alt34=2;
                int LA34_0 = input.LA(1);

                if ( (LA34_0==COMMA) ) {
                    alt34=1;
                }


                switch (alt34) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:242:30: COMMA mapEntry
            	    {
            	    COMMA143=(Token)input.LT(1);
            	    match(input,COMMA,FOLLOW_COMMA_in_mapInitializer1490); if (failed) return retval;
            	    if ( backtracking==0 ) stream_COMMA.add(COMMA143);

            	    pushFollow(FOLLOW_mapEntry_in_mapInitializer1492);
            	    mapEntry144=mapEntry();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) stream_mapEntry.add(mapEntry144.getTree());

            	    }
            	    break;

            	default :
            	    break loop34;
                }
            } while (true);

            RCURLY145=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_mapInitializer1496); if (failed) return retval;
            if ( backtracking==0 ) stream_RCURLY.add(RCURLY145);


            // AST REWRITE
            // elements: mapEntry
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 242:54: -> ^( MAP_INITIALIZER ( mapEntry )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:242:57: ^( MAP_INITIALIZER ( mapEntry )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(MAP_INITIALIZER, "MAP_INITIALIZER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:242:75: ( mapEntry )*
                while ( stream_mapEntry.hasNext() ) {
                    adaptor.addChild(root_1, stream_mapEntry.next());

                }
                stream_mapEntry.reset();

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end mapInitializer

    public static class mapEntry_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start mapEntry
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:244:1: mapEntry : expression COLON expression -> ^( MAP_ENTRY ( expression )* ) ;
    public final mapEntry_return mapEntry() throws RecognitionException {
        mapEntry_return retval = new mapEntry_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token COLON147=null;
        expression_return expression146 = null;

        expression_return expression148 = null;


        Object COLON147_tree=null;
        RewriteRuleTokenStream stream_COLON=new RewriteRuleTokenStream(adaptor,"token COLON");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:245:5: ( expression COLON expression -> ^( MAP_ENTRY ( expression )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:245:7: expression COLON expression
            {
            pushFollow(FOLLOW_expression_in_mapEntry1517);
            expression146=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression146.getTree());
            COLON147=(Token)input.LT(1);
            match(input,COLON,FOLLOW_COLON_in_mapEntry1519); if (failed) return retval;
            if ( backtracking==0 ) stream_COLON.add(COLON147);

            pushFollow(FOLLOW_expression_in_mapEntry1521);
            expression148=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression148.getTree());

            // AST REWRITE
            // elements: expression
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 245:35: -> ^( MAP_ENTRY ( expression )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:245:38: ^( MAP_ENTRY ( expression )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(MAP_ENTRY, "MAP_ENTRY"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:245:50: ( expression )*
                while ( stream_expression.hasNext() ) {
                    adaptor.addChild(root_1, stream_expression.next());

                }
                stream_expression.reset();

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end mapEntry

    public static class ctorArgs_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start ctorArgs
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:247:1: ctorArgs : LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN ;
    public final ctorArgs_return ctorArgs() throws RecognitionException {
        ctorArgs_return retval = new ctorArgs_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LPAREN149=null;
        Token COMMA151=null;
        Token RPAREN153=null;
        namedArgument_return namedArgument150 = null;

        namedArgument_return namedArgument152 = null;


        Object LPAREN149_tree=null;
        Object COMMA151_tree=null;
        Object RPAREN153_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:248:2: ( LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:248:4: LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN
            {
            root_0 = (Object)adaptor.nil();

            LPAREN149=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_ctorArgs1539); if (failed) return retval;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:248:12: ( namedArgument ( COMMA namedArgument )* )?
            int alt36=2;
            int LA36_0 = input.LA(1);

            if ( (LA36_0==INTEGER_LITERAL||LA36_0==LPAREN||(LA36_0>=PLUS && LA36_0<=MINUS)||LA36_0==BANG||(LA36_0>=POUND && LA36_0<=DOLLAR)||(LA36_0>=AT && LA36_0<=LBRACKET)||LA36_0==PROJECT||(LA36_0>=SELECT && LA36_0<=LAMBDA)||(LA36_0>=LCURLY && LA36_0<=FALSE)||(LA36_0>=91 && LA36_0<=92)) ) {
                alt36=1;
            }
            switch (alt36) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:248:13: namedArgument ( COMMA namedArgument )*
                    {
                    pushFollow(FOLLOW_namedArgument_in_ctorArgs1543);
                    namedArgument150=namedArgument();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, namedArgument150.getTree());
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:248:27: ( COMMA namedArgument )*
                    loop35:
                    do {
                        int alt35=2;
                        int LA35_0 = input.LA(1);

                        if ( (LA35_0==COMMA) ) {
                            alt35=1;
                        }


                        switch (alt35) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:248:28: COMMA namedArgument
                    	    {
                    	    COMMA151=(Token)input.LT(1);
                    	    match(input,COMMA,FOLLOW_COMMA_in_ctorArgs1546); if (failed) return retval;
                    	    pushFollow(FOLLOW_namedArgument_in_ctorArgs1549);
                    	    namedArgument152=namedArgument();
                    	    _fsp--;
                    	    if (failed) return retval;
                    	    if ( backtracking==0 ) adaptor.addChild(root_0, namedArgument152.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop35;
                        }
                    } while (true);


                    }
                    break;

            }

            RPAREN153=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_ctorArgs1555); if (failed) return retval;

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end ctorArgs

    public static class argument_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start argument
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:250:1: argument : expression ;
    public final argument_return argument() throws RecognitionException {
        argument_return retval = new argument_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        expression_return expression154 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:250:10: ( expression )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:250:12: expression
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_expression_in_argument1564);
            expression154=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression154.getTree());

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end argument

    public static class namedArgument_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start namedArgument
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:252:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );
    public final namedArgument_return namedArgument() throws RecognitionException {
        namedArgument_return retval = new namedArgument_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token ASSIGN155=null;
        expression_return expression156 = null;

        argument_return argument157 = null;


        Object id_tree=null;
        Object ASSIGN155_tree=null;
        RewriteRuleTokenStream stream_ASSIGN=new RewriteRuleTokenStream(adaptor,"token ASSIGN");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:253:5: ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument )
            int alt37=2;
            int LA37_0 = input.LA(1);

            if ( (LA37_0==ID) ) {
                int LA37_1 = input.LA(2);

                if ( (LA37_1==ASSIGN) ) {
                    int LA37_26 = input.LA(3);

                    if ( (synpred6()) ) {
                        alt37=1;
                    }
                    else if ( (true) ) {
                        alt37=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("252:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );", 37, 26, input);

                        throw nvae;
                    }
                }
                else if ( (LA37_1==LPAREN||LA37_1==RPAREN||(LA37_1>=DEFAULT && LA37_1<=QMARK)||(LA37_1>=OR && LA37_1<=POWER)||LA37_1==DOT||LA37_1==COMMA||LA37_1==LBRACKET||(LA37_1>=EQUAL && LA37_1<=MATCHES)) ) {
                    alt37=2;
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("252:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );", 37, 1, input);

                    throw nvae;
                }
            }
            else if ( (LA37_0==INTEGER_LITERAL||LA37_0==LPAREN||(LA37_0>=PLUS && LA37_0<=MINUS)||LA37_0==BANG||LA37_0==POUND||LA37_0==DOLLAR||(LA37_0>=AT && LA37_0<=LBRACKET)||LA37_0==PROJECT||(LA37_0>=SELECT && LA37_0<=LAMBDA)||(LA37_0>=LCURLY && LA37_0<=FALSE)||(LA37_0>=91 && LA37_0<=92)) ) {
                alt37=2;
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("252:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );", 37, 0, input);

                throw nvae;
            }
            switch (alt37) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:253:7: ( ID ASSIGN )=>id= ID ASSIGN expression
                    {
                    id=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_namedArgument1587); if (failed) return retval;
                    if ( backtracking==0 ) stream_ID.add(id);

                    ASSIGN155=(Token)input.LT(1);
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_namedArgument1589); if (failed) return retval;
                    if ( backtracking==0 ) stream_ASSIGN.add(ASSIGN155);

                    pushFollow(FOLLOW_expression_in_namedArgument1591);
                    expression156=expression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_expression.add(expression156.getTree());

                    // AST REWRITE
                    // elements: expression
                    // token labels:
                    // rule labels: retval
                    // token list labels:
                    // rule list labels:
                    if ( backtracking==0 ) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 254:19: -> ^( NAMED_ARGUMENT[$id] expression )
                    {
                        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:254:22: ^( NAMED_ARGUMENT[$id] expression )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(adaptor.create(NAMED_ARGUMENT, id), root_1);

                        adaptor.addChild(root_1, stream_expression.next());

                        adaptor.addChild(root_0, root_1);
                        }

                    }

                    }

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:255:7: argument
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_argument_in_namedArgument1627);
                    argument157=argument();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, argument157.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end namedArgument

    public static class qualifiedId_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start qualifiedId
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:257:1: qualifiedId : ID ( DOT ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) ;
    public final qualifiedId_return qualifiedId() throws RecognitionException {
        qualifiedId_return retval = new qualifiedId_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID158=null;
        Token DOT159=null;
        Token ID160=null;

        Object ID158_tree=null;
        Object DOT159_tree=null;
        Object ID160_tree=null;
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleTokenStream stream_DOT=new RewriteRuleTokenStream(adaptor,"token DOT");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:257:13: ( ID ( DOT ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:257:15: ID ( DOT ID )*
            {
            ID158=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_qualifiedId1639); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(ID158);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:257:18: ( DOT ID )*
            loop38:
            do {
                int alt38=2;
                int LA38_0 = input.LA(1);

                if ( (LA38_0==DOT) ) {
                    alt38=1;
                }


                switch (alt38) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:257:19: DOT ID
            	    {
            	    DOT159=(Token)input.LT(1);
            	    match(input,DOT,FOLLOW_DOT_in_qualifiedId1642); if (failed) return retval;
            	    if ( backtracking==0 ) stream_DOT.add(DOT159);

            	    ID160=(Token)input.LT(1);
            	    match(input,ID,FOLLOW_ID_in_qualifiedId1644); if (failed) return retval;
            	    if ( backtracking==0 ) stream_ID.add(ID160);


            	    }
            	    break;

            	default :
            	    break loop38;
                }
            } while (true);


            // AST REWRITE
            // elements: ID
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 257:28: -> ^( QUALIFIED_IDENTIFIER ( ID )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:257:31: ^( QUALIFIED_IDENTIFIER ( ID )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(QUALIFIED_IDENTIFIER, "QUALIFIED_IDENTIFIER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:257:54: ( ID )*
                while ( stream_ID.hasNext() ) {
                    adaptor.addChild(root_1, stream_ID.next());

                }
                stream_ID.reset();

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end qualifiedId

    public static class contextName_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start contextName
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:259:1: contextName : ID ( DIV ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) ;
    public final contextName_return contextName() throws RecognitionException {
        contextName_return retval = new contextName_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID161=null;
        Token DIV162=null;
        Token ID163=null;

        Object ID161_tree=null;
        Object DIV162_tree=null;
        Object ID163_tree=null;
        RewriteRuleTokenStream stream_DIV=new RewriteRuleTokenStream(adaptor,"token DIV");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:259:13: ( ID ( DIV ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:259:15: ID ( DIV ID )*
            {
            ID161=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_contextName1663); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(ID161);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:259:18: ( DIV ID )*
            loop39:
            do {
                int alt39=2;
                int LA39_0 = input.LA(1);

                if ( (LA39_0==DIV) ) {
                    alt39=1;
                }


                switch (alt39) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:259:19: DIV ID
            	    {
            	    DIV162=(Token)input.LT(1);
            	    match(input,DIV,FOLLOW_DIV_in_contextName1666); if (failed) return retval;
            	    if ( backtracking==0 ) stream_DIV.add(DIV162);

            	    ID163=(Token)input.LT(1);
            	    match(input,ID,FOLLOW_ID_in_contextName1668); if (failed) return retval;
            	    if ( backtracking==0 ) stream_ID.add(ID163);


            	    }
            	    break;

            	default :
            	    break loop39;
                }
            } while (true);


            // AST REWRITE
            // elements: ID
            // token labels:
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 259:28: -> ^( QUALIFIED_IDENTIFIER ( ID )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:259:31: ^( QUALIFIED_IDENTIFIER ( ID )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(QUALIFIED_IDENTIFIER, "QUALIFIED_IDENTIFIER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:259:54: ( ID )*
                while ( stream_ID.hasNext() ) {
                    adaptor.addChild(root_1, stream_ID.next());

                }
                stream_ID.reset();

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end contextName

    public static class literal_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start literal
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:261:1: literal : ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL | dateLiteral );
    public final literal_return literal() throws RecognitionException {
        literal_return retval = new literal_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token INTEGER_LITERAL164=null;
        Token STRING_LITERAL165=null;
        Token DQ_STRING_LITERAL166=null;
        Token NULL_LITERAL168=null;
        Token HEXADECIMAL_INTEGER_LITERAL169=null;
        Token REAL_LITERAL170=null;
        boolLiteral_return boolLiteral167 = null;

        dateLiteral_return dateLiteral171 = null;


        Object INTEGER_LITERAL164_tree=null;
        Object STRING_LITERAL165_tree=null;
        Object DQ_STRING_LITERAL166_tree=null;
        Object NULL_LITERAL168_tree=null;
        Object HEXADECIMAL_INTEGER_LITERAL169_tree=null;
        Object REAL_LITERAL170_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:262:2: ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL | dateLiteral )
            int alt40=8;
            switch ( input.LA(1) ) {
            case INTEGER_LITERAL:
                {
                alt40=1;
                }
                break;
            case STRING_LITERAL:
                {
                alt40=2;
                }
                break;
            case DQ_STRING_LITERAL:
                {
                alt40=3;
                }
                break;
            case TRUE:
            case FALSE:
                {
                alt40=4;
                }
                break;
            case NULL_LITERAL:
                {
                alt40=5;
                }
                break;
            case HEXADECIMAL_INTEGER_LITERAL:
                {
                alt40=6;
                }
                break;
            case REAL_LITERAL:
                {
                alt40=7;
                }
                break;
            case 92:
                {
                alt40=8;
                }
                break;
            default:
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("261:1: literal : ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL | dateLiteral );", 40, 0, input);

                throw nvae;
            }

            switch (alt40) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:262:4: INTEGER_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    INTEGER_LITERAL164=(Token)input.LT(1);
                    match(input,INTEGER_LITERAL,FOLLOW_INTEGER_LITERAL_in_literal1689); if (failed) return retval;
                    if ( backtracking==0 ) {
                    INTEGER_LITERAL164_tree = (Object)adaptor.create(INTEGER_LITERAL164);
                    adaptor.addChild(root_0, INTEGER_LITERAL164_tree);
                    }

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:263:4: STRING_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    STRING_LITERAL165=(Token)input.LT(1);
                    match(input,STRING_LITERAL,FOLLOW_STRING_LITERAL_in_literal1695); if (failed) return retval;
                    if ( backtracking==0 ) {
                    STRING_LITERAL165_tree = (Object)adaptor.create(STRING_LITERAL165);
                    adaptor.addChild(root_0, STRING_LITERAL165_tree);
                    }

                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:264:4: DQ_STRING_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    DQ_STRING_LITERAL166=(Token)input.LT(1);
                    match(input,DQ_STRING_LITERAL,FOLLOW_DQ_STRING_LITERAL_in_literal1700); if (failed) return retval;
                    if ( backtracking==0 ) {
                    DQ_STRING_LITERAL166_tree = (Object)adaptor.create(DQ_STRING_LITERAL166);
                    adaptor.addChild(root_0, DQ_STRING_LITERAL166_tree);
                    }

                    }
                    break;
                case 4 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:265:4: boolLiteral
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_boolLiteral_in_literal1705);
                    boolLiteral167=boolLiteral();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, boolLiteral167.getTree());

                    }
                    break;
                case 5 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:266:4: NULL_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    NULL_LITERAL168=(Token)input.LT(1);
                    match(input,NULL_LITERAL,FOLLOW_NULL_LITERAL_in_literal1710); if (failed) return retval;
                    if ( backtracking==0 ) {
                    NULL_LITERAL168_tree = (Object)adaptor.create(NULL_LITERAL168);
                    adaptor.addChild(root_0, NULL_LITERAL168_tree);
                    }

                    }
                    break;
                case 6 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:267:4: HEXADECIMAL_INTEGER_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    HEXADECIMAL_INTEGER_LITERAL169=(Token)input.LT(1);
                    match(input,HEXADECIMAL_INTEGER_LITERAL,FOLLOW_HEXADECIMAL_INTEGER_LITERAL_in_literal1715); if (failed) return retval;
                    if ( backtracking==0 ) {
                    HEXADECIMAL_INTEGER_LITERAL169_tree = (Object)adaptor.create(HEXADECIMAL_INTEGER_LITERAL169);
                    adaptor.addChild(root_0, HEXADECIMAL_INTEGER_LITERAL169_tree);
                    }

                    }
                    break;
                case 7 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:268:4: REAL_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    REAL_LITERAL170=(Token)input.LT(1);
                    match(input,REAL_LITERAL,FOLLOW_REAL_LITERAL_in_literal1721); if (failed) return retval;
                    if ( backtracking==0 ) {
                    REAL_LITERAL170_tree = (Object)adaptor.create(REAL_LITERAL170);
                    adaptor.addChild(root_0, REAL_LITERAL170_tree);
                    }

                    }
                    break;
                case 8 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:269:4: dateLiteral
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_dateLiteral_in_literal1726);
                    dateLiteral171=dateLiteral();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, dateLiteral171.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end literal

    public static class boolLiteral_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start boolLiteral
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:272:1: boolLiteral : ( TRUE | FALSE );
    public final boolLiteral_return boolLiteral() throws RecognitionException {
        boolLiteral_return retval = new boolLiteral_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set172=null;

        Object set172_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:272:12: ( TRUE | FALSE )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:
            {
            root_0 = (Object)adaptor.nil();

            set172=(Token)input.LT(1);
            if ( (input.LA(1)>=TRUE && input.LA(1)<=FALSE) ) {
                input.consume();
                if ( backtracking==0 ) adaptor.addChild(root_0, adaptor.create(set172));
                errorRecovery=false;failed=false;
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recoverFromMismatchedSet(input,mse,FOLLOW_set_in_boolLiteral0);    throw mse;
            }


            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end boolLiteral

    public static class dateLiteral_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start dateLiteral
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:274:1: dateLiteral : 'date' LPAREN d= STRING_LITERAL ( COMMA f= STRING_LITERAL )? RPAREN -> ^( DATE_LITERAL $d ( $f)? ) ;
    public final dateLiteral_return dateLiteral() throws RecognitionException {
        dateLiteral_return retval = new dateLiteral_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token d=null;
        Token f=null;
        Token string_literal173=null;
        Token LPAREN174=null;
        Token COMMA175=null;
        Token RPAREN176=null;

        Object d_tree=null;
        Object f_tree=null;
        Object string_literal173_tree=null;
        Object LPAREN174_tree=null;
        Object COMMA175_tree=null;
        Object RPAREN176_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
        RewriteRuleTokenStream stream_LPAREN=new RewriteRuleTokenStream(adaptor,"token LPAREN");
        RewriteRuleTokenStream stream_92=new RewriteRuleTokenStream(adaptor,"token 92");
        RewriteRuleTokenStream stream_STRING_LITERAL=new RewriteRuleTokenStream(adaptor,"token STRING_LITERAL");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:274:12: ( 'date' LPAREN d= STRING_LITERAL ( COMMA f= STRING_LITERAL )? RPAREN -> ^( DATE_LITERAL $d ( $f)? ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:274:14: 'date' LPAREN d= STRING_LITERAL ( COMMA f= STRING_LITERAL )? RPAREN
            {
            string_literal173=(Token)input.LT(1);
            match(input,92,FOLLOW_92_in_dateLiteral1747); if (failed) return retval;
            if ( backtracking==0 ) stream_92.add(string_literal173);

            LPAREN174=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_dateLiteral1749); if (failed) return retval;
            if ( backtracking==0 ) stream_LPAREN.add(LPAREN174);

            d=(Token)input.LT(1);
            match(input,STRING_LITERAL,FOLLOW_STRING_LITERAL_in_dateLiteral1753); if (failed) return retval;
            if ( backtracking==0 ) stream_STRING_LITERAL.add(d);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:274:45: ( COMMA f= STRING_LITERAL )?
            int alt41=2;
            int LA41_0 = input.LA(1);

            if ( (LA41_0==COMMA) ) {
                alt41=1;
            }
            switch (alt41) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:274:46: COMMA f= STRING_LITERAL
                    {
                    COMMA175=(Token)input.LT(1);
                    match(input,COMMA,FOLLOW_COMMA_in_dateLiteral1756); if (failed) return retval;
                    if ( backtracking==0 ) stream_COMMA.add(COMMA175);

                    f=(Token)input.LT(1);
                    match(input,STRING_LITERAL,FOLLOW_STRING_LITERAL_in_dateLiteral1760); if (failed) return retval;
                    if ( backtracking==0 ) stream_STRING_LITERAL.add(f);


                    }
                    break;

            }

            RPAREN176=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_dateLiteral1764); if (failed) return retval;
            if ( backtracking==0 ) stream_RPAREN.add(RPAREN176);


            // AST REWRITE
            // elements: d, f
            // token labels: d, f
            // rule labels: retval
            // token list labels:
            // rule list labels:
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleTokenStream stream_d=new RewriteRuleTokenStream(adaptor,"token d",d);
            RewriteRuleTokenStream stream_f=new RewriteRuleTokenStream(adaptor,"token f",f);
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 274:78: -> ^( DATE_LITERAL $d ( $f)? )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:274:81: ^( DATE_LITERAL $d ( $f)? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(DATE_LITERAL, "DATE_LITERAL"), root_1);

                adaptor.addChild(root_1, stream_d.next());
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:274:99: ( $f)?
                if ( stream_f.hasNext() ) {
                    adaptor.addChild(root_1, stream_f.next());

                }
                stream_f.reset();

                adaptor.addChild(root_0, root_1);
                }

            }

            }

            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end dateLiteral

    public static class relationalOperator_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start relationalOperator
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:281:1: relationalOperator : ( EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL | IN | IS | BETWEEN | MATCHES );
    public final relationalOperator_return relationalOperator() throws RecognitionException {
        relationalOperator_return retval = new relationalOperator_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set177=null;

        Object set177_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:282:5: ( EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL | IN | IS | BETWEEN | MATCHES )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:
            {
            root_0 = (Object)adaptor.nil();

            set177=(Token)input.LT(1);
            if ( (input.LA(1)>=EQUAL && input.LA(1)<=MATCHES) ) {
                input.consume();
                if ( backtracking==0 ) adaptor.addChild(root_0, adaptor.create(set177));
                errorRecovery=false;failed=false;
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recoverFromMismatchedSet(input,mse,FOLLOW_set_in_relationalOperator0);    throw mse;
            }


            }

            retval.stop = input.LT(-1);

            if ( backtracking==0 ) {
                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }

                catch(RecognitionException e) {
                        reportError(e);
                        throw e;
                }
        finally {
        }
        return retval;
    }
    // $ANTLR end relationalOperator

    // $ANTLR start synpred1
    public final void synpred1_fragment() throws RecognitionException {
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:112:5: ( LPAREN expression SEMI )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:112:6: LPAREN expression SEMI
        {
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred1556); if (failed) return ;
        pushFollow(FOLLOW_expression_in_synpred1558);
        expression();
        _fsp--;
        if (failed) return ;
        match(input,SEMI,FOLLOW_SEMI_in_synpred1560); if (failed) return ;

        }
    }
    // $ANTLR end synpred1

    // $ANTLR start synpred2
    public final void synpred2_fragment() throws RecognitionException {
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:151:7: ( POUND ID LPAREN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:151:8: POUND ID LPAREN
        {
        match(input,POUND,FOLLOW_POUND_in_synpred2824); if (failed) return ;
        match(input,ID,FOLLOW_ID_in_synpred2826); if (failed) return ;
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred2828); if (failed) return ;

        }
    }
    // $ANTLR end synpred2

    // $ANTLR start synpred3
    public final void synpred3_fragment() throws RecognitionException {
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:160:4: ( DOLLAR ID LPAREN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:160:5: DOLLAR ID LPAREN
        {
        match(input,DOLLAR,FOLLOW_DOLLAR_in_synpred3907); if (failed) return ;
        match(input,ID,FOLLOW_ID_in_synpred3909); if (failed) return ;
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred3911); if (failed) return ;

        }
    }
    // $ANTLR end synpred3

    // $ANTLR start synpred4
    public final void synpred4_fragment() throws RecognitionException {
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:168:4: ( ID LPAREN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:168:5: ID LPAREN
        {
        match(input,ID,FOLLOW_ID_in_synpred4973); if (failed) return ;
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred4975); if (failed) return ;

        }
    }
    // $ANTLR end synpred4

    // $ANTLR start synpred5
    public final void synpred5_fragment() throws RecognitionException {
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:223:4: ( 'new' qualifiedId LPAREN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:223:5: 'new' qualifiedId LPAREN
        {
        match(input,91,FOLLOW_91_in_synpred51322); if (failed) return ;
        pushFollow(FOLLOW_qualifiedId_in_synpred51324);
        qualifiedId();
        _fsp--;
        if (failed) return ;
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred51326); if (failed) return ;

        }
    }
    // $ANTLR end synpred5

    // $ANTLR start synpred6
    public final void synpred6_fragment() throws RecognitionException {
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:253:7: ( ID ASSIGN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:253:8: ID ASSIGN
        {
        match(input,ID,FOLLOW_ID_in_synpred61578); if (failed) return ;
        match(input,ASSIGN,FOLLOW_ASSIGN_in_synpred61580); if (failed) return ;

        }
    }
    // $ANTLR end synpred6

    public final boolean synpred4() {
        backtracking++;
        int start = input.mark();
        try {
            synpred4_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !failed;
        input.rewind(start);
        backtracking--;
        failed=false;
        return success;
    }
    public final boolean synpred2() {
        backtracking++;
        int start = input.mark();
        try {
            synpred2_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !failed;
        input.rewind(start);
        backtracking--;
        failed=false;
        return success;
    }
    public final boolean synpred3() {
        backtracking++;
        int start = input.mark();
        try {
            synpred3_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !failed;
        input.rewind(start);
        backtracking--;
        failed=false;
        return success;
    }
    public final boolean synpred1() {
        backtracking++;
        int start = input.mark();
        try {
            synpred1_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !failed;
        input.rewind(start);
        backtracking--;
        failed=false;
        return success;
    }
    public final boolean synpred5() {
        backtracking++;
        int start = input.mark();
        try {
            synpred5_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !failed;
        input.rewind(start);
        backtracking--;
        failed=false;
        return success;
    }
    public final boolean synpred6() {
        backtracking++;
        int start = input.mark();
        try {
            synpred6_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !failed;
        input.rewind(start);
        backtracking--;
        failed=false;
        return success;
    }




    public static final BitSet FOLLOW_expression_in_expr181 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_expr183 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_exprList196 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_exprList198 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_SEMI_in_exprList201 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_exprList203 = new BitSet(new long[]{0x0000000380000000L});
    public static final BitSet FOLLOW_SEMIRPAREN_in_exprList208 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RPAREN_in_exprList212 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_logicalOrExpression_in_expression256 = new BitSet(new long[]{0x0000001C00000002L});
    public static final BitSet FOLLOW_ASSIGN_in_expression265 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_logicalOrExpression_in_expression268 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DEFAULT_in_expression278 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_logicalOrExpression_in_expression281 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_QMARK_in_expression291 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_expression294 = new BitSet(new long[]{0x0000002000000000L});
    public static final BitSet FOLLOW_COLON_in_expression296 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_expression299 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_parenExpr310 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_parenExpr313 = new BitSet(new long[]{0x0000000200000000L});
    public static final BitSet FOLLOW_RPAREN_in_parenExpr315 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_logicalAndExpression_in_logicalOrExpression328 = new BitSet(new long[]{0x0000004000000002L});
    public static final BitSet FOLLOW_OR_in_logicalOrExpression331 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_logicalAndExpression_in_logicalOrExpression334 = new BitSet(new long[]{0x0000004000000002L});
    public static final BitSet FOLLOW_relationalExpression_in_logicalAndExpression369 = new BitSet(new long[]{0x0000008000000002L});
    public static final BitSet FOLLOW_AND_in_logicalAndExpression372 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_relationalExpression_in_logicalAndExpression375 = new BitSet(new long[]{0x0000008000000002L});
    public static final BitSet FOLLOW_sumExpression_in_relationalExpression386 = new BitSet(new long[]{0x0000000000000002L,0x00000000000FFC00L});
    public static final BitSet FOLLOW_relationalOperator_in_relationalExpression389 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_sumExpression_in_relationalExpression392 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_productExpression_in_sumExpression403 = new BitSet(new long[]{0x0000030000000002L});
    public static final BitSet FOLLOW_PLUS_in_sumExpression408 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_MINUS_in_sumExpression413 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_productExpression_in_sumExpression417 = new BitSet(new long[]{0x0000030000000002L});
    public static final BitSet FOLLOW_powerExpr_in_productExpression432 = new BitSet(new long[]{0x00001C0000000002L});
    public static final BitSet FOLLOW_STAR_in_productExpression436 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_DIV_in_productExpression441 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_MOD_in_productExpression445 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_powerExpr_in_productExpression449 = new BitSet(new long[]{0x00001C0000000002L});
    public static final BitSet FOLLOW_unaryExpression_in_powerExpr465 = new BitSet(new long[]{0x0000200000000002L});
    public static final BitSet FOLLOW_POWER_in_powerExpr468 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_unaryExpression_in_powerExpr471 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PLUS_in_unaryExpression485 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_MINUS_in_unaryExpression490 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_BANG_in_unaryExpression495 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_unaryExpression_in_unaryExpression499 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_primaryExpression_in_unaryExpression505 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_startNode_in_primaryExpression519 = new BitSet(new long[]{0x0020800000000002L});
    public static final BitSet FOLLOW_node_in_primaryExpression522 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_exprList_in_startNode565 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_parenExpr_in_startNode574 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_methodOrProperty_in_startNode582 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_functionOrVar_in_startNode591 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_localFunctionOrVar_in_startNode599 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_reference_in_startNode607 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_indexer_in_startNode615 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_literal_in_startNode623 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_type_in_startNode631 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_constructor_in_startNode639 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_projection_in_startNode647 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selection_in_startNode656 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_firstSelection_in_startNode665 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lastSelection_in_startNode673 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_listInitializer_in_startNode681 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_mapInitializer_in_startNode689 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lambda_in_startNode697 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_node718 = new BitSet(new long[]{0x0E83000040000000L});
    public static final BitSet FOLLOW_dottedNode_in_node720 = new BitSet(new long[]{0x0020800000000002L});
    public static final BitSet FOLLOW_nonDottedNode_in_node725 = new BitSet(new long[]{0x0020800000000002L});
    public static final BitSet FOLLOW_indexer_in_nonDottedNode737 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_methodOrProperty_in_dottedNode750 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_functionOrVar_in_dottedNode756 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_projection_in_dottedNode764 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selection_in_dottedNode773 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_firstSelection_in_dottedNode782 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lastSelection_in_dottedNode791 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_exprList_in_dottedNode800 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_function_in_functionOrVar833 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_var_in_functionOrVar841 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_function858 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_ID_in_function862 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_methodArgs_in_function864 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_var885 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_ID_in_var889 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_localFunction_in_localFunctionOrVar916 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_localVar_in_localFunctionOrVar921 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOLLAR_in_localFunction931 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_ID_in_localFunction935 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_methodArgs_in_localFunction937 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOLLAR_in_localVar952 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_ID_in_localVar956 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_methodOrProperty982 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_methodArgs_in_methodOrProperty984 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_methodOrProperty998 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_methodArgs1013 = new BitSet(new long[]{0xBEB7430240000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_argument_in_methodArgs1017 = new BitSet(new long[]{0x0008000200000000L});
    public static final BitSet FOLLOW_COMMA_in_methodArgs1020 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_argument_in_methodArgs1023 = new BitSet(new long[]{0x0008000200000000L});
    public static final BitSet FOLLOW_COMMA_in_methodArgs1028 = new BitSet(new long[]{0x0000000200000000L});
    public static final BitSet FOLLOW_RPAREN_in_methodArgs1035 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_property1048 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_AT_in_reference1070 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_LPAREN_in_reference1074 = new BitSet(new long[]{0x0002000200000000L});
    public static final BitSet FOLLOW_contextName_in_reference1079 = new BitSet(new long[]{0x0000002000000000L});
    public static final BitSet FOLLOW_COLON_in_reference1081 = new BitSet(new long[]{0x0002000200000000L});
    public static final BitSet FOLLOW_qualifiedId_in_reference1088 = new BitSet(new long[]{0x0000000200000000L});
    public static final BitSet FOLLOW_RPAREN_in_reference1092 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACKET_in_indexer1127 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_argument_in_indexer1131 = new BitSet(new long[]{0x0048000000000000L});
    public static final BitSet FOLLOW_COMMA_in_indexer1134 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_argument_in_indexer1138 = new BitSet(new long[]{0x0048000000000000L});
    public static final BitSet FOLLOW_RBRACKET_in_indexer1142 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PROJECT_in_projection1169 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_projection1172 = new BitSet(new long[]{0x0100000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_projection1174 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELECT_in_selection1182 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_selection1185 = new BitSet(new long[]{0x0100000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_selection1187 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELECT_FIRST_in_firstSelection1195 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_firstSelection1198 = new BitSet(new long[]{0x0100000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_firstSelection1200 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELECT_LAST_in_lastSelection1208 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_lastSelection1211 = new BitSet(new long[]{0x0100000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_lastSelection1213 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TYPE_in_type1222 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_type1224 = new BitSet(new long[]{0x0000000200000000L});
    public static final BitSet FOLLOW_RPAREN_in_type1226 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LAMBDA_in_lambda1253 = new BitSet(new long[]{0x4002000000000000L});
    public static final BitSet FOLLOW_argList_in_lambda1256 = new BitSet(new long[]{0x4000000000000000L});
    public static final BitSet FOLLOW_PIPE_in_lambda1260 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_lambda1262 = new BitSet(new long[]{0x0100000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_lambda1264 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_argList1288 = new BitSet(new long[]{0x0008000000000002L});
    public static final BitSet FOLLOW_COMMA_in_argList1291 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_ID_in_argList1295 = new BitSet(new long[]{0x0008000000000002L});
    public static final BitSet FOLLOW_91_in_constructor1331 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_constructor1333 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_ctorArgs_in_constructor1335 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_arrayConstructor_in_constructor1352 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_91_in_arrayConstructor1363 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_arrayConstructor1365 = new BitSet(new long[]{0x0020000000000000L});
    public static final BitSet FOLLOW_arrayRank_in_arrayConstructor1367 = new BitSet(new long[]{0x8000000000000002L});
    public static final BitSet FOLLOW_listInitializer_in_arrayConstructor1370 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACKET_in_arrayRank1405 = new BitSet(new long[]{0xBEF7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_arrayRank1408 = new BitSet(new long[]{0x0048000000000000L});
    public static final BitSet FOLLOW_COMMA_in_arrayRank1411 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_arrayRank1413 = new BitSet(new long[]{0x0048000000000000L});
    public static final BitSet FOLLOW_RBRACKET_in_arrayRank1419 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LCURLY_in_listInitializer1444 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_listInitializer1446 = new BitSet(new long[]{0x0108000000000000L});
    public static final BitSet FOLLOW_COMMA_in_listInitializer1449 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_listInitializer1451 = new BitSet(new long[]{0x0108000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_listInitializer1455 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_mapInitializer1483 = new BitSet(new long[]{0x8000000000000000L});
    public static final BitSet FOLLOW_LCURLY_in_mapInitializer1485 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_mapEntry_in_mapInitializer1487 = new BitSet(new long[]{0x0108000000000000L});
    public static final BitSet FOLLOW_COMMA_in_mapInitializer1490 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_mapEntry_in_mapInitializer1492 = new BitSet(new long[]{0x0108000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_mapInitializer1496 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expression_in_mapEntry1517 = new BitSet(new long[]{0x0000002000000000L});
    public static final BitSet FOLLOW_COLON_in_mapEntry1519 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_mapEntry1521 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_ctorArgs1539 = new BitSet(new long[]{0xBEB7430240000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_namedArgument_in_ctorArgs1543 = new BitSet(new long[]{0x0008000200000000L});
    public static final BitSet FOLLOW_COMMA_in_ctorArgs1546 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_namedArgument_in_ctorArgs1549 = new BitSet(new long[]{0x0008000200000000L});
    public static final BitSet FOLLOW_RPAREN_in_ctorArgs1555 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expression_in_argument1564 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_namedArgument1587 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_ASSIGN_in_namedArgument1589 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_namedArgument1591 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_argument_in_namedArgument1627 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_qualifiedId1639 = new BitSet(new long[]{0x0000800000000002L});
    public static final BitSet FOLLOW_DOT_in_qualifiedId1642 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_ID_in_qualifiedId1644 = new BitSet(new long[]{0x0000800000000002L});
    public static final BitSet FOLLOW_ID_in_contextName1663 = new BitSet(new long[]{0x0000080000000002L});
    public static final BitSet FOLLOW_DIV_in_contextName1666 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_ID_in_contextName1668 = new BitSet(new long[]{0x0000080000000002L});
    public static final BitSet FOLLOW_INTEGER_LITERAL_in_literal1689 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_LITERAL_in_literal1695 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DQ_STRING_LITERAL_in_literal1700 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_boolLiteral_in_literal1705 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NULL_LITERAL_in_literal1710 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_HEXADECIMAL_INTEGER_LITERAL_in_literal1715 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_REAL_LITERAL_in_literal1721 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_dateLiteral_in_literal1726 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_boolLiteral0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_92_in_dateLiteral1747 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_LPAREN_in_dateLiteral1749 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000001L});
    public static final BitSet FOLLOW_STRING_LITERAL_in_dateLiteral1753 = new BitSet(new long[]{0x0008000200000000L});
    public static final BitSet FOLLOW_COMMA_in_dateLiteral1756 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000001L});
    public static final BitSet FOLLOW_STRING_LITERAL_in_dateLiteral1760 = new BitSet(new long[]{0x0000000200000000L});
    public static final BitSet FOLLOW_RPAREN_in_dateLiteral1764 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_relationalOperator0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_synpred1556 = new BitSet(new long[]{0xBEB7430040000020L,0x000000001800007FL});
    public static final BitSet FOLLOW_expression_in_synpred1558 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_SEMI_in_synpred1560 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_synpred2824 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_ID_in_synpred2826 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred2828 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOLLAR_in_synpred3907 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_ID_in_synpred3909 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred3911 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_synpred4973 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred4975 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_91_in_synpred51322 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_synpred51324 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred51326 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_synpred61578 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_ASSIGN_in_synpred61580 = new BitSet(new long[]{0x0000000000000002L});

}