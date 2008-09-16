// $ANTLR 3.0.1 /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g 2008-09-16 10:16:33
package org.springframework.expression.spel.generated;

import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.antlr.runtime.tree.*;

public class SpringExpressionsParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "EXPRESSIONLIST", "INTEGER_LITERAL", "EXPRESSION", "QUALIFIED_IDENTIFIER", "PROPERTY_OR_FIELD", "INDEXER", "CONSTRUCTOR", "HOLDER", "CONSTRUCTOR_ARRAY", "NAMED_ARGUMENT", "FUNCTIONREF", "TYPEREF", "RANGE", "VARIABLEREF", "LIST_INITIALIZER", "MAP_INITIALIZER", "MAP_ENTRY", "METHOD", "ADD", "SUBTRACT", "NUMBER", "ASSIGN", "DEFAULT", "QMARK", "COLON", "LPAREN", "RPAREN", "OR", "AND", "PLUS", "MINUS", "STAR", "DIV", "MOD", "POWER", "BANG", "DOT", "POUND", "ID", "COMMA", "LBRACKET", "RBRACKET", "PROJECT", "RCURLY", "SELECT", "SELECT_FIRST", "SELECT_LAST", "TYPE", "LCURLY", "STRING_LITERAL", "DQ_STRING_LITERAL", "NULL_LITERAL", "HEXADECIMAL_INTEGER_LITERAL", "REAL_LITERAL", "TRUE", "FALSE", "DECIMAL_DIGIT", "INTEGER_TYPE_SUFFIX", "HEX_DIGIT", "EQUAL", "NOT_EQUAL", "LESS_THAN", "LESS_THAN_OR_EQUAL", "GREATER_THAN", "GREATER_THAN_OR_EQUAL", "IN", "INSTANCEOF", "BETWEEN", "MATCHES", "SEMI", "PIPE", "APOS", "DOT_ESCAPED", "WS", "DOLLAR", "AT", "UPTO", "EXPONENT_PART", "REAL_TYPE_SUFFIX", "SIGN", "'new'"
    };
    public static final int GREATER_THAN_OR_EQUAL=68;
    public static final int SELECT_FIRST=49;
    public static final int HOLDER=11;
    public static final int COMMA=43;
    public static final int GREATER_THAN=67;
    public static final int TYPE=51;
    public static final int EXPRESSIONLIST=4;
    public static final int MINUS=34;
    public static final int MAP_ENTRY=20;
    public static final int SELECT_LAST=50;
    public static final int NUMBER=24;
    public static final int LESS_THAN=65;
    public static final int BANG=39;
    public static final int FALSE=59;
    public static final int METHOD=21;
    public static final int PROPERTY_OR_FIELD=8;
    public static final int LBRACKET=44;
    public static final int INDEXER=9;
    public static final int MOD=37;
    public static final int CONSTRUCTOR_ARRAY=12;
    public static final int FUNCTIONREF=14;
    public static final int NULL_LITERAL=55;
    public static final int NAMED_ARGUMENT=13;
    public static final int OR=31;
    public static final int PIPE=74;
    public static final int DOT=40;
    public static final int RCURLY=47;
    public static final int EXPRESSION=6;
    public static final int AND=32;
    public static final int LCURLY=52;
    public static final int REAL_TYPE_SUFFIX=82;
    public static final int STRING_LITERAL=53;
    public static final int SELECT=48;
    public static final int QUALIFIED_IDENTIFIER=7;
    public static final int RBRACKET=45;
    public static final int SUBTRACT=23;
    public static final int ASSIGN=25;
    public static final int INSTANCEOF=70;
    public static final int BETWEEN=71;
    public static final int RPAREN=30;
    public static final int SIGN=83;
    public static final int LPAREN=29;
    public static final int HEX_DIGIT=62;
    public static final int PLUS=33;
    public static final int LIST_INITIALIZER=18;
    public static final int APOS=75;
    public static final int INTEGER_LITERAL=5;
    public static final int AT=79;
    public static final int ID=42;
    public static final int NOT_EQUAL=64;
    public static final int RANGE=16;
    public static final int POWER=38;
    public static final int TYPEREF=15;
    public static final int DECIMAL_DIGIT=60;
    public static final int WS=77;
    public static final int DOLLAR=78;
    public static final int LESS_THAN_OR_EQUAL=66;
    public static final int DQ_STRING_LITERAL=54;
    public static final int HEXADECIMAL_INTEGER_LITERAL=56;
    public static final int MAP_INITIALIZER=19;
    public static final int IN=69;
    public static final int SEMI=73;
    public static final int CONSTRUCTOR=10;
    public static final int INTEGER_TYPE_SUFFIX=61;
    public static final int EQUAL=63;
    public static final int MATCHES=72;
    public static final int DOT_ESCAPED=76;
    public static final int UPTO=80;
    public static final int EOF=-1;
    public static final int QMARK=27;
    public static final int PROJECT=46;
    public static final int DEFAULT=26;
    public static final int COLON=28;
    public static final int DIV=36;
    public static final int STAR=35;
    public static final int REAL_LITERAL=57;
    public static final int VARIABLEREF=17;
    public static final int EXPONENT_PART=81;
    public static final int TRUE=58;
    public static final int ADD=22;
    public static final int POUND=41;

        public SpringExpressionsParser(TokenStream input) {
            super(input);
            ruleMemo = new HashMap[45+1];
         }
        
    protected TreeAdaptor adaptor = new CommonTreeAdaptor();

    public void setTreeAdaptor(TreeAdaptor adaptor) {
        this.adaptor = adaptor;
    }
    public TreeAdaptor getTreeAdaptor() {
        return adaptor;
    }

    public String[] getTokenNames() { return tokenNames; }
    public String getGrammarFileName() { return "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g"; }


      // For collecting info whilst processing rules that can be used in messages
      protected Stack<String> paraphrase = new Stack<String>();


    public static class expr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start expr
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:51:1: expr : expression EOF ;
    public final expr_return expr() throws RecognitionException {
        expr_return retval = new expr_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token EOF2=null;
        expression_return expression1 = null;


        Object EOF2_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:51:5: ( expression EOF )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:51:7: expression EOF
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_expression_in_expr154);
            expression1=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression1.getTree());
            EOF2=(Token)input.LT(1);
            match(input,EOF,FOLLOW_EOF_in_expr156); if (failed) return retval;

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
    // $ANTLR end expr

    public static class expression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start expression
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:53:1: expression : logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )? ;
    public final expression_return expression() throws RecognitionException {
        expression_return retval = new expression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ASSIGN4=null;
        Token DEFAULT6=null;
        Token QMARK8=null;
        Token COLON10=null;
        logicalOrExpression_return logicalOrExpression3 = null;

        logicalOrExpression_return logicalOrExpression5 = null;

        logicalOrExpression_return logicalOrExpression7 = null;

        expression_return expression9 = null;

        expression_return expression11 = null;


        Object ASSIGN4_tree=null;
        Object DEFAULT6_tree=null;
        Object QMARK8_tree=null;
        Object COLON10_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:53:12: ( logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )? )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:54:5: logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )?
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_logicalOrExpression_in_expression176);
            logicalOrExpression3=logicalOrExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, logicalOrExpression3.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:55:5: ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )?
            int alt1=4;
            switch ( input.LA(1) ) {
                case ASSIGN:
                    {
                    alt1=1;
                    }
                    break;
                case DEFAULT:
                    {
                    alt1=2;
                    }
                    break;
                case QMARK:
                    {
                    alt1=3;
                    }
                    break;
            }

            switch (alt1) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:55:7: ( ASSIGN logicalOrExpression )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:55:7: ( ASSIGN logicalOrExpression )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:55:8: ASSIGN logicalOrExpression
                    {
                    ASSIGN4=(Token)input.LT(1);
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_expression185); if (failed) return retval;
                    if ( backtracking==0 ) {
                    ASSIGN4_tree = (Object)adaptor.create(ASSIGN4);
                    root_0 = (Object)adaptor.becomeRoot(ASSIGN4_tree, root_0);
                    }
                    pushFollow(FOLLOW_logicalOrExpression_in_expression188);
                    logicalOrExpression5=logicalOrExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, logicalOrExpression5.getTree());

                    }


                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:56:6: ( DEFAULT logicalOrExpression )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:56:6: ( DEFAULT logicalOrExpression )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:56:7: DEFAULT logicalOrExpression
                    {
                    DEFAULT6=(Token)input.LT(1);
                    match(input,DEFAULT,FOLLOW_DEFAULT_in_expression198); if (failed) return retval;
                    if ( backtracking==0 ) {
                    DEFAULT6_tree = (Object)adaptor.create(DEFAULT6);
                    root_0 = (Object)adaptor.becomeRoot(DEFAULT6_tree, root_0);
                    }
                    pushFollow(FOLLOW_logicalOrExpression_in_expression201);
                    logicalOrExpression7=logicalOrExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, logicalOrExpression7.getTree());

                    }


                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:57:6: ( QMARK expression COLON expression )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:57:6: ( QMARK expression COLON expression )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:57:7: QMARK expression COLON expression
                    {
                    QMARK8=(Token)input.LT(1);
                    match(input,QMARK,FOLLOW_QMARK_in_expression211); if (failed) return retval;
                    if ( backtracking==0 ) {
                    QMARK8_tree = (Object)adaptor.create(QMARK8);
                    root_0 = (Object)adaptor.becomeRoot(QMARK8_tree, root_0);
                    }
                    pushFollow(FOLLOW_expression_in_expression214);
                    expression9=expression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, expression9.getTree());
                    COLON10=(Token)input.LT(1);
                    match(input,COLON,FOLLOW_COLON_in_expression216); if (failed) return retval;
                    pushFollow(FOLLOW_expression_in_expression219);
                    expression11=expression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, expression11.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:59:1: parenExpr : LPAREN expression RPAREN ;
    public final parenExpr_return parenExpr() throws RecognitionException {
        parenExpr_return retval = new parenExpr_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LPAREN12=null;
        Token RPAREN14=null;
        expression_return expression13 = null;


        Object LPAREN12_tree=null;
        Object RPAREN14_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:59:11: ( LPAREN expression RPAREN )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:59:13: LPAREN expression RPAREN
            {
            root_0 = (Object)adaptor.nil();

            LPAREN12=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_parenExpr230); if (failed) return retval;
            pushFollow(FOLLOW_expression_in_parenExpr233);
            expression13=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression13.getTree());
            RPAREN14=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_parenExpr235); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:61:1: logicalOrExpression : logicalAndExpression ( OR logicalAndExpression )* ;
    public final logicalOrExpression_return logicalOrExpression() throws RecognitionException {
        logicalOrExpression_return retval = new logicalOrExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token OR16=null;
        logicalAndExpression_return logicalAndExpression15 = null;

        logicalAndExpression_return logicalAndExpression17 = null;


        Object OR16_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:62:1: ( logicalAndExpression ( OR logicalAndExpression )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:62:3: logicalAndExpression ( OR logicalAndExpression )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_logicalAndExpression_in_logicalOrExpression246);
            logicalAndExpression15=logicalAndExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, logicalAndExpression15.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:62:24: ( OR logicalAndExpression )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0==OR) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:62:25: OR logicalAndExpression
            	    {
            	    OR16=(Token)input.LT(1);
            	    match(input,OR,FOLLOW_OR_in_logicalOrExpression249); if (failed) return retval;
            	    if ( backtracking==0 ) {
            	    OR16_tree = (Object)adaptor.create(OR16);
            	    root_0 = (Object)adaptor.becomeRoot(OR16_tree, root_0);
            	    }
            	    pushFollow(FOLLOW_logicalAndExpression_in_logicalOrExpression252);
            	    logicalAndExpression17=logicalAndExpression();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, logicalAndExpression17.getTree());

            	    }
            	    break;

            	default :
            	    break loop2;
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:64:1: logicalAndExpression : relationalExpression ( AND relationalExpression )* ;
    public final logicalAndExpression_return logicalAndExpression() throws RecognitionException {
        logicalAndExpression_return retval = new logicalAndExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token AND19=null;
        relationalExpression_return relationalExpression18 = null;

        relationalExpression_return relationalExpression20 = null;


        Object AND19_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:65:1: ( relationalExpression ( AND relationalExpression )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:65:3: relationalExpression ( AND relationalExpression )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_relationalExpression_in_logicalAndExpression287);
            relationalExpression18=relationalExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, relationalExpression18.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:65:24: ( AND relationalExpression )*
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( (LA3_0==AND) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:65:25: AND relationalExpression
            	    {
            	    AND19=(Token)input.LT(1);
            	    match(input,AND,FOLLOW_AND_in_logicalAndExpression290); if (failed) return retval;
            	    if ( backtracking==0 ) {
            	    AND19_tree = (Object)adaptor.create(AND19);
            	    root_0 = (Object)adaptor.becomeRoot(AND19_tree, root_0);
            	    }
            	    pushFollow(FOLLOW_relationalExpression_in_logicalAndExpression293);
            	    relationalExpression20=relationalExpression();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, relationalExpression20.getTree());

            	    }
            	    break;

            	default :
            	    break loop3;
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:67:1: relationalExpression : sumExpression ( relationalOperator sumExpression )? ;
    public final relationalExpression_return relationalExpression() throws RecognitionException {
        relationalExpression_return retval = new relationalExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        sumExpression_return sumExpression21 = null;

        relationalOperator_return relationalOperator22 = null;

        sumExpression_return sumExpression23 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:67:22: ( sumExpression ( relationalOperator sumExpression )? )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:67:24: sumExpression ( relationalOperator sumExpression )?
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_sumExpression_in_relationalExpression304);
            sumExpression21=sumExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, sumExpression21.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:67:38: ( relationalOperator sumExpression )?
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( ((LA4_0>=EQUAL && LA4_0<=MATCHES)) ) {
                alt4=1;
            }
            switch (alt4) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:67:39: relationalOperator sumExpression
                    {
                    pushFollow(FOLLOW_relationalOperator_in_relationalExpression307);
                    relationalOperator22=relationalOperator();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) root_0 = (Object)adaptor.becomeRoot(relationalOperator22.getTree(), root_0);
                    pushFollow(FOLLOW_sumExpression_in_relationalExpression310);
                    sumExpression23=sumExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, sumExpression23.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:69:1: sumExpression : productExpression ( ( PLUS | MINUS ) productExpression )* ;
    public final sumExpression_return sumExpression() throws RecognitionException {
        sumExpression_return retval = new sumExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token PLUS25=null;
        Token MINUS26=null;
        productExpression_return productExpression24 = null;

        productExpression_return productExpression27 = null;


        Object PLUS25_tree=null;
        Object MINUS26_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:70:2: ( productExpression ( ( PLUS | MINUS ) productExpression )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:70:4: productExpression ( ( PLUS | MINUS ) productExpression )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_productExpression_in_sumExpression321);
            productExpression24=productExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, productExpression24.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:70:22: ( ( PLUS | MINUS ) productExpression )*
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( ((LA6_0>=PLUS && LA6_0<=MINUS)) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:70:24: ( PLUS | MINUS ) productExpression
            	    {
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:70:24: ( PLUS | MINUS )
            	    int alt5=2;
            	    int LA5_0 = input.LA(1);

            	    if ( (LA5_0==PLUS) ) {
            	        alt5=1;
            	    }
            	    else if ( (LA5_0==MINUS) ) {
            	        alt5=2;
            	    }
            	    else {
            	        if (backtracking>0) {failed=true; return retval;}
            	        NoViableAltException nvae =
            	            new NoViableAltException("70:24: ( PLUS | MINUS )", 5, 0, input);

            	        throw nvae;
            	    }
            	    switch (alt5) {
            	        case 1 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:70:25: PLUS
            	            {
            	            PLUS25=(Token)input.LT(1);
            	            match(input,PLUS,FOLLOW_PLUS_in_sumExpression326); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            PLUS25_tree = (Object)adaptor.create(PLUS25);
            	            root_0 = (Object)adaptor.becomeRoot(PLUS25_tree, root_0);
            	            }

            	            }
            	            break;
            	        case 2 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:70:33: MINUS
            	            {
            	            MINUS26=(Token)input.LT(1);
            	            match(input,MINUS,FOLLOW_MINUS_in_sumExpression331); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            MINUS26_tree = (Object)adaptor.create(MINUS26);
            	            root_0 = (Object)adaptor.becomeRoot(MINUS26_tree, root_0);
            	            }

            	            }
            	            break;

            	    }

            	    pushFollow(FOLLOW_productExpression_in_sumExpression335);
            	    productExpression27=productExpression();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, productExpression27.getTree());

            	    }
            	    break;

            	default :
            	    break loop6;
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:72:1: productExpression : powerExpr ( ( STAR | DIV | MOD ) powerExpr )* ;
    public final productExpression_return productExpression() throws RecognitionException {
        productExpression_return retval = new productExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token STAR29=null;
        Token DIV30=null;
        Token MOD31=null;
        powerExpr_return powerExpr28 = null;

        powerExpr_return powerExpr32 = null;


        Object STAR29_tree=null;
        Object DIV30_tree=null;
        Object MOD31_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:73:2: ( powerExpr ( ( STAR | DIV | MOD ) powerExpr )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:73:4: powerExpr ( ( STAR | DIV | MOD ) powerExpr )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_powerExpr_in_productExpression346);
            powerExpr28=powerExpr();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, powerExpr28.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:73:14: ( ( STAR | DIV | MOD ) powerExpr )*
            loop8:
            do {
                int alt8=2;
                int LA8_0 = input.LA(1);

                if ( ((LA8_0>=STAR && LA8_0<=MOD)) ) {
                    alt8=1;
                }


                switch (alt8) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:73:15: ( STAR | DIV | MOD ) powerExpr
            	    {
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:73:15: ( STAR | DIV | MOD )
            	    int alt7=3;
            	    switch ( input.LA(1) ) {
            	    case STAR:
            	        {
            	        alt7=1;
            	        }
            	        break;
            	    case DIV:
            	        {
            	        alt7=2;
            	        }
            	        break;
            	    case MOD:
            	        {
            	        alt7=3;
            	        }
            	        break;
            	    default:
            	        if (backtracking>0) {failed=true; return retval;}
            	        NoViableAltException nvae =
            	            new NoViableAltException("73:15: ( STAR | DIV | MOD )", 7, 0, input);

            	        throw nvae;
            	    }

            	    switch (alt7) {
            	        case 1 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:73:16: STAR
            	            {
            	            STAR29=(Token)input.LT(1);
            	            match(input,STAR,FOLLOW_STAR_in_productExpression350); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            STAR29_tree = (Object)adaptor.create(STAR29);
            	            root_0 = (Object)adaptor.becomeRoot(STAR29_tree, root_0);
            	            }

            	            }
            	            break;
            	        case 2 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:73:24: DIV
            	            {
            	            DIV30=(Token)input.LT(1);
            	            match(input,DIV,FOLLOW_DIV_in_productExpression355); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            DIV30_tree = (Object)adaptor.create(DIV30);
            	            root_0 = (Object)adaptor.becomeRoot(DIV30_tree, root_0);
            	            }

            	            }
            	            break;
            	        case 3 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:73:30: MOD
            	            {
            	            MOD31=(Token)input.LT(1);
            	            match(input,MOD,FOLLOW_MOD_in_productExpression359); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            MOD31_tree = (Object)adaptor.create(MOD31);
            	            root_0 = (Object)adaptor.becomeRoot(MOD31_tree, root_0);
            	            }

            	            }
            	            break;

            	    }

            	    pushFollow(FOLLOW_powerExpr_in_productExpression363);
            	    powerExpr32=powerExpr();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, powerExpr32.getTree());

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
    // $ANTLR end productExpression

    public static class powerExpr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start powerExpr
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:1: powerExpr : unaryExpression ( POWER unaryExpression )? ;
    public final powerExpr_return powerExpr() throws RecognitionException {
        powerExpr_return retval = new powerExpr_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token POWER34=null;
        unaryExpression_return unaryExpression33 = null;

        unaryExpression_return unaryExpression35 = null;


        Object POWER34_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:12: ( unaryExpression ( POWER unaryExpression )? )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:14: unaryExpression ( POWER unaryExpression )?
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_unaryExpression_in_powerExpr375);
            unaryExpression33=unaryExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, unaryExpression33.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:30: ( POWER unaryExpression )?
            int alt9=2;
            int LA9_0 = input.LA(1);

            if ( (LA9_0==POWER) ) {
                alt9=1;
            }
            switch (alt9) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:31: POWER unaryExpression
                    {
                    POWER34=(Token)input.LT(1);
                    match(input,POWER,FOLLOW_POWER_in_powerExpr378); if (failed) return retval;
                    if ( backtracking==0 ) {
                    POWER34_tree = (Object)adaptor.create(POWER34);
                    root_0 = (Object)adaptor.becomeRoot(POWER34_tree, root_0);
                    }
                    pushFollow(FOLLOW_unaryExpression_in_powerExpr381);
                    unaryExpression35=unaryExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, unaryExpression35.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:77:1: unaryExpression : ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression );
    public final unaryExpression_return unaryExpression() throws RecognitionException {
        unaryExpression_return retval = new unaryExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token PLUS36=null;
        Token MINUS37=null;
        Token BANG38=null;
        unaryExpression_return unaryExpression39 = null;

        primaryExpression_return primaryExpression40 = null;


        Object PLUS36_tree=null;
        Object MINUS37_tree=null;
        Object BANG38_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:78:2: ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression )
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( ((LA11_0>=PLUS && LA11_0<=MINUS)||LA11_0==BANG) ) {
                alt11=1;
            }
            else if ( (LA11_0==INTEGER_LITERAL||LA11_0==LPAREN||(LA11_0>=POUND && LA11_0<=ID)||LA11_0==LBRACKET||LA11_0==PROJECT||(LA11_0>=SELECT && LA11_0<=FALSE)||LA11_0==84) ) {
                alt11=2;
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("77:1: unaryExpression : ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression );", 11, 0, input);

                throw nvae;
            }
            switch (alt11) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:78:4: ( PLUS | MINUS | BANG ) unaryExpression
                    {
                    root_0 = (Object)adaptor.nil();

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:78:4: ( PLUS | MINUS | BANG )
                    int alt10=3;
                    switch ( input.LA(1) ) {
                    case PLUS:
                        {
                        alt10=1;
                        }
                        break;
                    case MINUS:
                        {
                        alt10=2;
                        }
                        break;
                    case BANG:
                        {
                        alt10=3;
                        }
                        break;
                    default:
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("78:4: ( PLUS | MINUS | BANG )", 10, 0, input);

                        throw nvae;
                    }

                    switch (alt10) {
                        case 1 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:78:5: PLUS
                            {
                            PLUS36=(Token)input.LT(1);
                            match(input,PLUS,FOLLOW_PLUS_in_unaryExpression395); if (failed) return retval;
                            if ( backtracking==0 ) {
                            PLUS36_tree = (Object)adaptor.create(PLUS36);
                            root_0 = (Object)adaptor.becomeRoot(PLUS36_tree, root_0);
                            }

                            }
                            break;
                        case 2 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:78:13: MINUS
                            {
                            MINUS37=(Token)input.LT(1);
                            match(input,MINUS,FOLLOW_MINUS_in_unaryExpression400); if (failed) return retval;
                            if ( backtracking==0 ) {
                            MINUS37_tree = (Object)adaptor.create(MINUS37);
                            root_0 = (Object)adaptor.becomeRoot(MINUS37_tree, root_0);
                            }

                            }
                            break;
                        case 3 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:78:22: BANG
                            {
                            BANG38=(Token)input.LT(1);
                            match(input,BANG,FOLLOW_BANG_in_unaryExpression405); if (failed) return retval;
                            if ( backtracking==0 ) {
                            BANG38_tree = (Object)adaptor.create(BANG38);
                            root_0 = (Object)adaptor.becomeRoot(BANG38_tree, root_0);
                            }

                            }
                            break;

                    }

                    pushFollow(FOLLOW_unaryExpression_in_unaryExpression409);
                    unaryExpression39=unaryExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, unaryExpression39.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:79:4: primaryExpression
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_primaryExpression_in_unaryExpression415);
                    primaryExpression40=primaryExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, primaryExpression40.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:81:1: primaryExpression : startNode ( node )? -> ^( EXPRESSION startNode ( node )? ) ;
    public final primaryExpression_return primaryExpression() throws RecognitionException {
        primaryExpression_return retval = new primaryExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        startNode_return startNode41 = null;

        node_return node42 = null;


        RewriteRuleSubtreeStream stream_node=new RewriteRuleSubtreeStream(adaptor,"rule node");
        RewriteRuleSubtreeStream stream_startNode=new RewriteRuleSubtreeStream(adaptor,"rule startNode");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:82:5: ( startNode ( node )? -> ^( EXPRESSION startNode ( node )? ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:82:7: startNode ( node )?
            {
            pushFollow(FOLLOW_startNode_in_primaryExpression429);
            startNode41=startNode();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_startNode.add(startNode41.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:82:17: ( node )?
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0==DOT||LA12_0==LBRACKET) ) {
                alt12=1;
            }
            switch (alt12) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:82:18: node
                    {
                    pushFollow(FOLLOW_node_in_primaryExpression432);
                    node42=node();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_node.add(node42.getTree());

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
            // 82:25: -> ^( EXPRESSION startNode ( node )? )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:82:28: ^( EXPRESSION startNode ( node )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(EXPRESSION, "EXPRESSION"), root_1);

                adaptor.addChild(root_1, stream_startNode.next());
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:82:51: ( node )?
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:84:1: startNode : ( parenExpr | methodOrProperty | functionOrVar | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer );
    public final startNode_return startNode() throws RecognitionException {
        startNode_return retval = new startNode_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        parenExpr_return parenExpr43 = null;

        methodOrProperty_return methodOrProperty44 = null;

        functionOrVar_return functionOrVar45 = null;

        indexer_return indexer46 = null;

        literal_return literal47 = null;

        type_return type48 = null;

        constructor_return constructor49 = null;

        projection_return projection50 = null;

        selection_return selection51 = null;

        firstSelection_return firstSelection52 = null;

        lastSelection_return lastSelection53 = null;

        listInitializer_return listInitializer54 = null;

        mapInitializer_return mapInitializer55 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:85:5: ( parenExpr | methodOrProperty | functionOrVar | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer )
            int alt13=13;
            switch ( input.LA(1) ) {
            case LPAREN:
                {
                alt13=1;
                }
                break;
            case ID:
                {
                alt13=2;
                }
                break;
            case POUND:
                {
                int LA13_3 = input.LA(2);

                if ( (LA13_3==ID) ) {
                    alt13=3;
                }
                else if ( (LA13_3==LCURLY) ) {
                    alt13=13;
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("84:1: startNode : ( parenExpr | methodOrProperty | functionOrVar | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer );", 13, 3, input);

                    throw nvae;
                }
                }
                break;
            case LBRACKET:
                {
                alt13=4;
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
                {
                alt13=5;
                }
                break;
            case TYPE:
                {
                alt13=6;
                }
                break;
            case 84:
                {
                alt13=7;
                }
                break;
            case PROJECT:
                {
                alt13=8;
                }
                break;
            case SELECT:
                {
                alt13=9;
                }
                break;
            case SELECT_FIRST:
                {
                alt13=10;
                }
                break;
            case SELECT_LAST:
                {
                alt13=11;
                }
                break;
            case LCURLY:
                {
                alt13=12;
                }
                break;
            default:
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("84:1: startNode : ( parenExpr | methodOrProperty | functionOrVar | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer );", 13, 0, input);

                throw nvae;
            }

            switch (alt13) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:86:5: parenExpr
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_parenExpr_in_startNode465);
                    parenExpr43=parenExpr();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, parenExpr43.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:87:7: methodOrProperty
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_methodOrProperty_in_startNode473);
                    methodOrProperty44=methodOrProperty();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, methodOrProperty44.getTree());

                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:88:7: functionOrVar
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_functionOrVar_in_startNode482);
                    functionOrVar45=functionOrVar();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, functionOrVar45.getTree());

                    }
                    break;
                case 4 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:89:7: indexer
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_indexer_in_startNode490);
                    indexer46=indexer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, indexer46.getTree());

                    }
                    break;
                case 5 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:90:7: literal
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_literal_in_startNode498);
                    literal47=literal();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, literal47.getTree());

                    }
                    break;
                case 6 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:91:7: type
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_type_in_startNode506);
                    type48=type();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, type48.getTree());

                    }
                    break;
                case 7 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:92:7: constructor
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_constructor_in_startNode514);
                    constructor49=constructor();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, constructor49.getTree());

                    }
                    break;
                case 8 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:93:7: projection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_projection_in_startNode522);
                    projection50=projection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, projection50.getTree());

                    }
                    break;
                case 9 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:94:7: selection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_selection_in_startNode531);
                    selection51=selection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, selection51.getTree());

                    }
                    break;
                case 10 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:95:7: firstSelection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_firstSelection_in_startNode540);
                    firstSelection52=firstSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, firstSelection52.getTree());

                    }
                    break;
                case 11 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:96:7: lastSelection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_lastSelection_in_startNode548);
                    lastSelection53=lastSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, lastSelection53.getTree());

                    }
                    break;
                case 12 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:97:7: listInitializer
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_listInitializer_in_startNode556);
                    listInitializer54=listInitializer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, listInitializer54.getTree());

                    }
                    break;
                case 13 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:98:7: mapInitializer
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_mapInitializer_in_startNode564);
                    mapInitializer55=mapInitializer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, mapInitializer55.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:101:1: node : ( ( DOT dottedNode ) | nonDottedNode )+ ;
    public final node_return node() throws RecognitionException {
        node_return retval = new node_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token DOT56=null;
        dottedNode_return dottedNode57 = null;

        nonDottedNode_return nonDottedNode58 = null;


        Object DOT56_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:102:2: ( ( ( DOT dottedNode ) | nonDottedNode )+ )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:102:4: ( ( DOT dottedNode ) | nonDottedNode )+
            {
            root_0 = (Object)adaptor.nil();

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:102:4: ( ( DOT dottedNode ) | nonDottedNode )+
            int cnt14=0;
            loop14:
            do {
                int alt14=3;
                int LA14_0 = input.LA(1);

                if ( (LA14_0==DOT) ) {
                    alt14=1;
                }
                else if ( (LA14_0==LBRACKET) ) {
                    alt14=2;
                }


                switch (alt14) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:102:5: ( DOT dottedNode )
            	    {
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:102:5: ( DOT dottedNode )
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:102:6: DOT dottedNode
            	    {
            	    DOT56=(Token)input.LT(1);
            	    match(input,DOT,FOLLOW_DOT_in_node584); if (failed) return retval;
            	    if ( backtracking==0 ) {
            	    DOT56_tree = (Object)adaptor.create(DOT56);
            	    adaptor.addChild(root_0, DOT56_tree);
            	    }
            	    pushFollow(FOLLOW_dottedNode_in_node586);
            	    dottedNode57=dottedNode();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, dottedNode57.getTree());

            	    }


            	    }
            	    break;
            	case 2 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:102:24: nonDottedNode
            	    {
            	    pushFollow(FOLLOW_nonDottedNode_in_node591);
            	    nonDottedNode58=nonDottedNode();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, nonDottedNode58.getTree());

            	    }
            	    break;

            	default :
            	    if ( cnt14 >= 1 ) break loop14;
            	    if (backtracking>0) {failed=true; return retval;}
                        EarlyExitException eee =
                            new EarlyExitException(14, input);
                        throw eee;
                }
                cnt14++;
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:104:1: nonDottedNode : indexer ;
    public final nonDottedNode_return nonDottedNode() throws RecognitionException {
        nonDottedNode_return retval = new nonDottedNode_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        indexer_return indexer59 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:105:2: ( indexer )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:105:4: indexer
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_indexer_in_nonDottedNode603);
            indexer59=indexer();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, indexer59.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:107:1: dottedNode : ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection ) ) ;
    public final dottedNode_return dottedNode() throws RecognitionException {
        dottedNode_return retval = new dottedNode_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        methodOrProperty_return methodOrProperty60 = null;

        functionOrVar_return functionOrVar61 = null;

        projection_return projection62 = null;

        selection_return selection63 = null;

        firstSelection_return firstSelection64 = null;

        lastSelection_return lastSelection65 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:108:2: ( ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection ) ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:109:2: ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection ) )
            {
            root_0 = (Object)adaptor.nil();

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:109:2: ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:109:3: ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection )
            {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:109:3: ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection )
            int alt15=6;
            switch ( input.LA(1) ) {
            case ID:
                {
                alt15=1;
                }
                break;
            case POUND:
                {
                alt15=2;
                }
                break;
            case PROJECT:
                {
                alt15=3;
                }
                break;
            case SELECT:
                {
                alt15=4;
                }
                break;
            case SELECT_FIRST:
                {
                alt15=5;
                }
                break;
            case SELECT_LAST:
                {
                alt15=6;
                }
                break;
            default:
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("109:3: ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection )", 15, 0, input);

                throw nvae;
            }

            switch (alt15) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:109:4: methodOrProperty
                    {
                    pushFollow(FOLLOW_methodOrProperty_in_dottedNode616);
                    methodOrProperty60=methodOrProperty();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, methodOrProperty60.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:110:4: functionOrVar
                    {
                    pushFollow(FOLLOW_functionOrVar_in_dottedNode622);
                    functionOrVar61=functionOrVar();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, functionOrVar61.getTree());

                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:111:7: projection
                    {
                    pushFollow(FOLLOW_projection_in_dottedNode630);
                    projection62=projection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, projection62.getTree());

                    }
                    break;
                case 4 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:112:7: selection
                    {
                    pushFollow(FOLLOW_selection_in_dottedNode639);
                    selection63=selection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, selection63.getTree());

                    }
                    break;
                case 5 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:113:7: firstSelection
                    {
                    pushFollow(FOLLOW_firstSelection_in_dottedNode648);
                    firstSelection64=firstSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, firstSelection64.getTree());

                    }
                    break;
                case 6 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:114:7: lastSelection
                    {
                    pushFollow(FOLLOW_lastSelection_in_dottedNode657);
                    lastSelection65=lastSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, lastSelection65.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:118:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );
    public final functionOrVar_return functionOrVar() throws RecognitionException {
        functionOrVar_return retval = new functionOrVar_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        function_return function66 = null;

        var_return var67 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:119:5: ( ( POUND ID LPAREN )=> function | var )
            int alt16=2;
            int LA16_0 = input.LA(1);

            if ( (LA16_0==POUND) ) {
                int LA16_1 = input.LA(2);

                if ( (LA16_1==ID) ) {
                    int LA16_2 = input.LA(3);

                    if ( (synpred1()) ) {
                        alt16=1;
                    }
                    else if ( (true) ) {
                        alt16=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("118:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 16, 2, input);

                        throw nvae;
                    }
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("118:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 16, 1, input);

                    throw nvae;
                }
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("118:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 16, 0, input);

                throw nvae;
            }
            switch (alt16) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:119:7: ( POUND ID LPAREN )=> function
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_function_in_functionOrVar691);
                    function66=function();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, function66.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:120:7: var
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_var_in_functionOrVar699);
                    var67=var();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, var67.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:123:1: function : POUND id= ID methodArgs -> ^( FUNCTIONREF[$id] methodArgs ) ;
    public final function_return function() throws RecognitionException {
        function_return retval = new function_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token POUND68=null;
        methodArgs_return methodArgs69 = null;


        Object id_tree=null;
        Object POUND68_tree=null;
        RewriteRuleTokenStream stream_POUND=new RewriteRuleTokenStream(adaptor,"token POUND");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleSubtreeStream stream_methodArgs=new RewriteRuleSubtreeStream(adaptor,"rule methodArgs");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:123:10: ( POUND id= ID methodArgs -> ^( FUNCTIONREF[$id] methodArgs ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:123:12: POUND id= ID methodArgs
            {
            POUND68=(Token)input.LT(1);
            match(input,POUND,FOLLOW_POUND_in_function716); if (failed) return retval;
            if ( backtracking==0 ) stream_POUND.add(POUND68);

            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_function720); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(id);

            pushFollow(FOLLOW_methodArgs_in_function722);
            methodArgs69=methodArgs();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_methodArgs.add(methodArgs69.getTree());

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
            // 123:35: -> ^( FUNCTIONREF[$id] methodArgs )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:123:38: ^( FUNCTIONREF[$id] methodArgs )
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:125:1: var : POUND id= ID -> ^( VARIABLEREF[$id] ) ;
    public final var_return var() throws RecognitionException {
        var_return retval = new var_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token POUND70=null;

        Object id_tree=null;
        Object POUND70_tree=null;
        RewriteRuleTokenStream stream_POUND=new RewriteRuleTokenStream(adaptor,"token POUND");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:125:5: ( POUND id= ID -> ^( VARIABLEREF[$id] ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:125:7: POUND id= ID
            {
            POUND70=(Token)input.LT(1);
            match(input,POUND,FOLLOW_POUND_in_var743); if (failed) return retval;
            if ( backtracking==0 ) stream_POUND.add(POUND70);

            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_var747); if (failed) return retval;
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
            // 125:19: -> ^( VARIABLEREF[$id] )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:125:22: ^( VARIABLEREF[$id] )
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

    public static class methodOrProperty_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start methodOrProperty
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:128:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );
    public final methodOrProperty_return methodOrProperty() throws RecognitionException {
        methodOrProperty_return retval = new methodOrProperty_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        methodArgs_return methodArgs71 = null;

        property_return property72 = null;


        Object id_tree=null;
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleSubtreeStream stream_methodArgs=new RewriteRuleSubtreeStream(adaptor,"rule methodArgs");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:129:2: ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property )
            int alt17=2;
            int LA17_0 = input.LA(1);

            if ( (LA17_0==ID) ) {
                int LA17_1 = input.LA(2);

                if ( (LA17_1==EOF||(LA17_1>=ASSIGN && LA17_1<=COLON)||(LA17_1>=RPAREN && LA17_1<=POWER)||LA17_1==DOT||(LA17_1>=COMMA && LA17_1<=RBRACKET)||LA17_1==RCURLY||(LA17_1>=EQUAL && LA17_1<=MATCHES)) ) {
                    alt17=2;
                }
                else if ( (LA17_1==LPAREN) && (synpred2())) {
                    alt17=1;
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("128:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );", 17, 1, input);

                    throw nvae;
                }
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("128:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );", 17, 0, input);

                throw nvae;
            }
            switch (alt17) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:129:4: ( ID LPAREN )=>id= ID methodArgs
                    {
                    id=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_methodOrProperty775); if (failed) return retval;
                    if ( backtracking==0 ) stream_ID.add(id);

                    pushFollow(FOLLOW_methodArgs_in_methodOrProperty777);
                    methodArgs71=methodArgs();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_methodArgs.add(methodArgs71.getTree());

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
                    // 129:36: -> ^( METHOD[$id] methodArgs )
                    {
                        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:129:39: ^( METHOD[$id] methodArgs )
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
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:130:4: property
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_property_in_methodOrProperty791);
                    property72=property();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, property72.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:1: methodArgs : LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN ;
    public final methodArgs_return methodArgs() throws RecognitionException {
        methodArgs_return retval = new methodArgs_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LPAREN73=null;
        Token COMMA75=null;
        Token COMMA77=null;
        Token RPAREN78=null;
        argument_return argument74 = null;

        argument_return argument76 = null;


        Object LPAREN73_tree=null;
        Object COMMA75_tree=null;
        Object COMMA77_tree=null;
        Object RPAREN78_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:12: ( LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:15: LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN
            {
            root_0 = (Object)adaptor.nil();

            LPAREN73=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_methodArgs806); if (failed) return retval;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:23: ( argument ( COMMA argument )* ( COMMA )? )?
            int alt20=2;
            int LA20_0 = input.LA(1);

            if ( (LA20_0==INTEGER_LITERAL||LA20_0==LPAREN||(LA20_0>=PLUS && LA20_0<=MINUS)||LA20_0==BANG||(LA20_0>=POUND && LA20_0<=ID)||LA20_0==LBRACKET||LA20_0==PROJECT||(LA20_0>=SELECT && LA20_0<=FALSE)||LA20_0==84) ) {
                alt20=1;
            }
            switch (alt20) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:24: argument ( COMMA argument )* ( COMMA )?
                    {
                    pushFollow(FOLLOW_argument_in_methodArgs810);
                    argument74=argument();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, argument74.getTree());
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:33: ( COMMA argument )*
                    loop18:
                    do {
                        int alt18=2;
                        int LA18_0 = input.LA(1);

                        if ( (LA18_0==COMMA) ) {
                            int LA18_1 = input.LA(2);

                            if ( (LA18_1==INTEGER_LITERAL||LA18_1==LPAREN||(LA18_1>=PLUS && LA18_1<=MINUS)||LA18_1==BANG||(LA18_1>=POUND && LA18_1<=ID)||LA18_1==LBRACKET||LA18_1==PROJECT||(LA18_1>=SELECT && LA18_1<=FALSE)||LA18_1==84) ) {
                                alt18=1;
                            }


                        }


                        switch (alt18) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:34: COMMA argument
                    	    {
                    	    COMMA75=(Token)input.LT(1);
                    	    match(input,COMMA,FOLLOW_COMMA_in_methodArgs813); if (failed) return retval;
                    	    pushFollow(FOLLOW_argument_in_methodArgs816);
                    	    argument76=argument();
                    	    _fsp--;
                    	    if (failed) return retval;
                    	    if ( backtracking==0 ) adaptor.addChild(root_0, argument76.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop18;
                        }
                    } while (true);

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:52: ( COMMA )?
                    int alt19=2;
                    int LA19_0 = input.LA(1);

                    if ( (LA19_0==COMMA) ) {
                        alt19=1;
                    }
                    switch (alt19) {
                        case 1 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:53: COMMA
                            {
                            COMMA77=(Token)input.LT(1);
                            match(input,COMMA,FOLLOW_COMMA_in_methodArgs821); if (failed) return retval;

                            }
                            break;

                    }


                    }
                    break;

            }

            RPAREN78=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_methodArgs828); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:141:1: property : id= ID -> ^( PROPERTY_OR_FIELD[$id] ) ;
    public final property_return property() throws RecognitionException {
        property_return retval = new property_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;

        Object id_tree=null;
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:141:9: (id= ID -> ^( PROPERTY_OR_FIELD[$id] ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:141:11: id= ID
            {
            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_property841); if (failed) return retval;
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
            // 141:17: -> ^( PROPERTY_OR_FIELD[$id] )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:141:20: ^( PROPERTY_OR_FIELD[$id] )
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

    public static class indexer_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start indexer
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:145:1: indexer : LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET -> ^( INDEXER $r1 ( $r2)* ) ;
    public final indexer_return indexer() throws RecognitionException {
        indexer_return retval = new indexer_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LBRACKET79=null;
        Token COMMA80=null;
        Token RBRACKET81=null;
        argument_return r1 = null;

        argument_return r2 = null;


        Object LBRACKET79_tree=null;
        Object COMMA80_tree=null;
        Object RBRACKET81_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_LBRACKET=new RewriteRuleTokenStream(adaptor,"token LBRACKET");
        RewriteRuleTokenStream stream_RBRACKET=new RewriteRuleTokenStream(adaptor,"token RBRACKET");
        RewriteRuleSubtreeStream stream_argument=new RewriteRuleSubtreeStream(adaptor,"rule argument");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:145:8: ( LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET -> ^( INDEXER $r1 ( $r2)* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:145:10: LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET
            {
            LBRACKET79=(Token)input.LT(1);
            match(input,LBRACKET,FOLLOW_LBRACKET_in_indexer857); if (failed) return retval;
            if ( backtracking==0 ) stream_LBRACKET.add(LBRACKET79);

            pushFollow(FOLLOW_argument_in_indexer861);
            r1=argument();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_argument.add(r1.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:145:31: ( COMMA r2= argument )*
            loop21:
            do {
                int alt21=2;
                int LA21_0 = input.LA(1);

                if ( (LA21_0==COMMA) ) {
                    alt21=1;
                }


                switch (alt21) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:145:32: COMMA r2= argument
            	    {
            	    COMMA80=(Token)input.LT(1);
            	    match(input,COMMA,FOLLOW_COMMA_in_indexer864); if (failed) return retval;
            	    if ( backtracking==0 ) stream_COMMA.add(COMMA80);

            	    pushFollow(FOLLOW_argument_in_indexer868);
            	    r2=argument();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) stream_argument.add(r2.getTree());

            	    }
            	    break;

            	default :
            	    break loop21;
                }
            } while (true);

            RBRACKET81=(Token)input.LT(1);
            match(input,RBRACKET,FOLLOW_RBRACKET_in_indexer872); if (failed) return retval;
            if ( backtracking==0 ) stream_RBRACKET.add(RBRACKET81);


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
            // 145:61: -> ^( INDEXER $r1 ( $r2)* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:145:64: ^( INDEXER $r1 ( $r2)* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(INDEXER, "INDEXER"), root_1);

                adaptor.addChild(root_1, stream_r1.next());
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:145:78: ( $r2)*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:150:1: projection : PROJECT expression RCURLY ;
    public final projection_return projection() throws RecognitionException {
        projection_return retval = new projection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token PROJECT82=null;
        Token RCURLY84=null;
        expression_return expression83 = null;


        Object PROJECT82_tree=null;
        Object RCURLY84_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:150:11: ( PROJECT expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:150:13: PROJECT expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            PROJECT82=(Token)input.LT(1);
            match(input,PROJECT,FOLLOW_PROJECT_in_projection899); if (failed) return retval;
            if ( backtracking==0 ) {
            PROJECT82_tree = (Object)adaptor.create(PROJECT82);
            root_0 = (Object)adaptor.becomeRoot(PROJECT82_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_projection902);
            expression83=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression83.getTree());
            RCURLY84=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_projection904); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:152:1: selection : SELECT expression RCURLY ;
    public final selection_return selection() throws RecognitionException {
        selection_return retval = new selection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token SELECT85=null;
        Token RCURLY87=null;
        expression_return expression86 = null;


        Object SELECT85_tree=null;
        Object RCURLY87_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:152:10: ( SELECT expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:152:12: SELECT expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            SELECT85=(Token)input.LT(1);
            match(input,SELECT,FOLLOW_SELECT_in_selection912); if (failed) return retval;
            if ( backtracking==0 ) {
            SELECT85_tree = (Object)adaptor.create(SELECT85);
            root_0 = (Object)adaptor.becomeRoot(SELECT85_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_selection915);
            expression86=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression86.getTree());
            RCURLY87=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_selection917); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:154:1: firstSelection : SELECT_FIRST expression RCURLY ;
    public final firstSelection_return firstSelection() throws RecognitionException {
        firstSelection_return retval = new firstSelection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token SELECT_FIRST88=null;
        Token RCURLY90=null;
        expression_return expression89 = null;


        Object SELECT_FIRST88_tree=null;
        Object RCURLY90_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:154:15: ( SELECT_FIRST expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:154:17: SELECT_FIRST expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            SELECT_FIRST88=(Token)input.LT(1);
            match(input,SELECT_FIRST,FOLLOW_SELECT_FIRST_in_firstSelection925); if (failed) return retval;
            if ( backtracking==0 ) {
            SELECT_FIRST88_tree = (Object)adaptor.create(SELECT_FIRST88);
            root_0 = (Object)adaptor.becomeRoot(SELECT_FIRST88_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_firstSelection928);
            expression89=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression89.getTree());
            RCURLY90=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_firstSelection930); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:156:1: lastSelection : SELECT_LAST expression RCURLY ;
    public final lastSelection_return lastSelection() throws RecognitionException {
        lastSelection_return retval = new lastSelection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token SELECT_LAST91=null;
        Token RCURLY93=null;
        expression_return expression92 = null;


        Object SELECT_LAST91_tree=null;
        Object RCURLY93_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:156:14: ( SELECT_LAST expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:156:16: SELECT_LAST expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            SELECT_LAST91=(Token)input.LT(1);
            match(input,SELECT_LAST,FOLLOW_SELECT_LAST_in_lastSelection938); if (failed) return retval;
            if ( backtracking==0 ) {
            SELECT_LAST91_tree = (Object)adaptor.create(SELECT_LAST91);
            root_0 = (Object)adaptor.becomeRoot(SELECT_LAST91_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_lastSelection941);
            expression92=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression92.getTree());
            RCURLY93=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_lastSelection943); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:159:1: type : TYPE qualifiedId RPAREN -> ^( TYPEREF qualifiedId ) ;
    public final type_return type() throws RecognitionException {
        type_return retval = new type_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token TYPE94=null;
        Token RPAREN96=null;
        qualifiedId_return qualifiedId95 = null;


        Object TYPE94_tree=null;
        Object RPAREN96_tree=null;
        RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
        RewriteRuleTokenStream stream_TYPE=new RewriteRuleTokenStream(adaptor,"token TYPE");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:159:5: ( TYPE qualifiedId RPAREN -> ^( TYPEREF qualifiedId ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:159:7: TYPE qualifiedId RPAREN
            {
            TYPE94=(Token)input.LT(1);
            match(input,TYPE,FOLLOW_TYPE_in_type952); if (failed) return retval;
            if ( backtracking==0 ) stream_TYPE.add(TYPE94);

            pushFollow(FOLLOW_qualifiedId_in_type954);
            qualifiedId95=qualifiedId();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_qualifiedId.add(qualifiedId95.getTree());
            RPAREN96=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_type956); if (failed) return retval;
            if ( backtracking==0 ) stream_RPAREN.add(RPAREN96);


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
            // 159:31: -> ^( TYPEREF qualifiedId )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:159:34: ^( TYPEREF qualifiedId )
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

    public static class constructor_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start constructor
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:163:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );
    public final constructor_return constructor() throws RecognitionException {
        constructor_return retval = new constructor_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal97=null;
        qualifiedId_return qualifiedId98 = null;

        ctorArgs_return ctorArgs99 = null;

        arrayConstructor_return arrayConstructor100 = null;


        Object string_literal97_tree=null;
        RewriteRuleTokenStream stream_84=new RewriteRuleTokenStream(adaptor,"token 84");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        RewriteRuleSubtreeStream stream_ctorArgs=new RewriteRuleSubtreeStream(adaptor,"rule ctorArgs");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:164:2: ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor )
            int alt22=2;
            int LA22_0 = input.LA(1);

            if ( (LA22_0==84) ) {
                int LA22_1 = input.LA(2);

                if ( (LA22_1==ID) ) {
                    int LA22_2 = input.LA(3);

                    if ( (synpred3()) ) {
                        alt22=1;
                    }
                    else if ( (true) ) {
                        alt22=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("163:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );", 22, 2, input);

                        throw nvae;
                    }
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("163:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );", 22, 1, input);

                    throw nvae;
                }
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("163:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );", 22, 0, input);

                throw nvae;
            }
            switch (alt22) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:164:4: ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs
                    {
                    string_literal97=(Token)input.LT(1);
                    match(input,84,FOLLOW_84_in_constructor987); if (failed) return retval;
                    if ( backtracking==0 ) stream_84.add(string_literal97);

                    pushFollow(FOLLOW_qualifiedId_in_constructor989);
                    qualifiedId98=qualifiedId();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_qualifiedId.add(qualifiedId98.getTree());
                    pushFollow(FOLLOW_ctorArgs_in_constructor991);
                    ctorArgs99=ctorArgs();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_ctorArgs.add(ctorArgs99.getTree());

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
                    // 164:61: -> ^( CONSTRUCTOR qualifiedId ctorArgs )
                    {
                        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:164:64: ^( CONSTRUCTOR qualifiedId ctorArgs )
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
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:165:6: arrayConstructor
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_arrayConstructor_in_constructor1008);
                    arrayConstructor100=arrayConstructor();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, arrayConstructor100.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:168:1: arrayConstructor : 'new' qualifiedId arrayRank ( listInitializer )? -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? ) ;
    public final arrayConstructor_return arrayConstructor() throws RecognitionException {
        arrayConstructor_return retval = new arrayConstructor_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal101=null;
        qualifiedId_return qualifiedId102 = null;

        arrayRank_return arrayRank103 = null;

        listInitializer_return listInitializer104 = null;


        Object string_literal101_tree=null;
        RewriteRuleTokenStream stream_84=new RewriteRuleTokenStream(adaptor,"token 84");
        RewriteRuleSubtreeStream stream_listInitializer=new RewriteRuleSubtreeStream(adaptor,"rule listInitializer");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        RewriteRuleSubtreeStream stream_arrayRank=new RewriteRuleSubtreeStream(adaptor,"rule arrayRank");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:169:2: ( 'new' qualifiedId arrayRank ( listInitializer )? -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:169:4: 'new' qualifiedId arrayRank ( listInitializer )?
            {
            string_literal101=(Token)input.LT(1);
            match(input,84,FOLLOW_84_in_arrayConstructor1019); if (failed) return retval;
            if ( backtracking==0 ) stream_84.add(string_literal101);

            pushFollow(FOLLOW_qualifiedId_in_arrayConstructor1021);
            qualifiedId102=qualifiedId();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_qualifiedId.add(qualifiedId102.getTree());
            pushFollow(FOLLOW_arrayRank_in_arrayConstructor1023);
            arrayRank103=arrayRank();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_arrayRank.add(arrayRank103.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:169:32: ( listInitializer )?
            int alt23=2;
            int LA23_0 = input.LA(1);

            if ( (LA23_0==LCURLY) ) {
                alt23=1;
            }
            switch (alt23) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:169:33: listInitializer
                    {
                    pushFollow(FOLLOW_listInitializer_in_arrayConstructor1026);
                    listInitializer104=listInitializer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_listInitializer.add(listInitializer104.getTree());

                    }
                    break;

            }


            // AST REWRITE
            // elements: listInitializer, qualifiedId, arrayRank
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 170:4: -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:170:7: ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(CONSTRUCTOR_ARRAY, "CONSTRUCTOR_ARRAY"), root_1);

                adaptor.addChild(root_1, stream_qualifiedId.next());
                adaptor.addChild(root_1, stream_arrayRank.next());
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:170:49: ( listInitializer )?
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:173:1: arrayRank : LBRACKET ( expression ( COMMA expression )* )? RBRACKET -> ^( EXPRESSIONLIST ( expression )* ) ;
    public final arrayRank_return arrayRank() throws RecognitionException {
        arrayRank_return retval = new arrayRank_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LBRACKET105=null;
        Token COMMA107=null;
        Token RBRACKET109=null;
        expression_return expression106 = null;

        expression_return expression108 = null;


        Object LBRACKET105_tree=null;
        Object COMMA107_tree=null;
        Object RBRACKET109_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_LBRACKET=new RewriteRuleTokenStream(adaptor,"token LBRACKET");
        RewriteRuleTokenStream stream_RBRACKET=new RewriteRuleTokenStream(adaptor,"token RBRACKET");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:174:5: ( LBRACKET ( expression ( COMMA expression )* )? RBRACKET -> ^( EXPRESSIONLIST ( expression )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:174:7: LBRACKET ( expression ( COMMA expression )* )? RBRACKET
            {
            LBRACKET105=(Token)input.LT(1);
            match(input,LBRACKET,FOLLOW_LBRACKET_in_arrayRank1061); if (failed) return retval;
            if ( backtracking==0 ) stream_LBRACKET.add(LBRACKET105);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:174:16: ( expression ( COMMA expression )* )?
            int alt25=2;
            int LA25_0 = input.LA(1);

            if ( (LA25_0==INTEGER_LITERAL||LA25_0==LPAREN||(LA25_0>=PLUS && LA25_0<=MINUS)||LA25_0==BANG||(LA25_0>=POUND && LA25_0<=ID)||LA25_0==LBRACKET||LA25_0==PROJECT||(LA25_0>=SELECT && LA25_0<=FALSE)||LA25_0==84) ) {
                alt25=1;
            }
            switch (alt25) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:174:17: expression ( COMMA expression )*
                    {
                    pushFollow(FOLLOW_expression_in_arrayRank1064);
                    expression106=expression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_expression.add(expression106.getTree());
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:174:28: ( COMMA expression )*
                    loop24:
                    do {
                        int alt24=2;
                        int LA24_0 = input.LA(1);

                        if ( (LA24_0==COMMA) ) {
                            alt24=1;
                        }


                        switch (alt24) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:174:29: COMMA expression
                    	    {
                    	    COMMA107=(Token)input.LT(1);
                    	    match(input,COMMA,FOLLOW_COMMA_in_arrayRank1067); if (failed) return retval;
                    	    if ( backtracking==0 ) stream_COMMA.add(COMMA107);

                    	    pushFollow(FOLLOW_expression_in_arrayRank1069);
                    	    expression108=expression();
                    	    _fsp--;
                    	    if (failed) return retval;
                    	    if ( backtracking==0 ) stream_expression.add(expression108.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop24;
                        }
                    } while (true);


                    }
                    break;

            }

            RBRACKET109=(Token)input.LT(1);
            match(input,RBRACKET,FOLLOW_RBRACKET_in_arrayRank1075); if (failed) return retval;
            if ( backtracking==0 ) stream_RBRACKET.add(RBRACKET109);


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
            // 174:59: -> ^( EXPRESSIONLIST ( expression )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:174:62: ^( EXPRESSIONLIST ( expression )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(EXPRESSIONLIST, "EXPRESSIONLIST"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:174:79: ( expression )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:176:1: listInitializer : LCURLY expression ( COMMA expression )* RCURLY -> ^( LIST_INITIALIZER ( expression )* ) ;
    public final listInitializer_return listInitializer() throws RecognitionException {
        listInitializer_return retval = new listInitializer_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LCURLY110=null;
        Token COMMA112=null;
        Token RCURLY114=null;
        expression_return expression111 = null;

        expression_return expression113 = null;


        Object LCURLY110_tree=null;
        Object COMMA112_tree=null;
        Object RCURLY114_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_RCURLY=new RewriteRuleTokenStream(adaptor,"token RCURLY");
        RewriteRuleTokenStream stream_LCURLY=new RewriteRuleTokenStream(adaptor,"token LCURLY");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:177:5: ( LCURLY expression ( COMMA expression )* RCURLY -> ^( LIST_INITIALIZER ( expression )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:177:7: LCURLY expression ( COMMA expression )* RCURLY
            {
            LCURLY110=(Token)input.LT(1);
            match(input,LCURLY,FOLLOW_LCURLY_in_listInitializer1100); if (failed) return retval;
            if ( backtracking==0 ) stream_LCURLY.add(LCURLY110);

            pushFollow(FOLLOW_expression_in_listInitializer1102);
            expression111=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression111.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:177:25: ( COMMA expression )*
            loop26:
            do {
                int alt26=2;
                int LA26_0 = input.LA(1);

                if ( (LA26_0==COMMA) ) {
                    alt26=1;
                }


                switch (alt26) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:177:26: COMMA expression
            	    {
            	    COMMA112=(Token)input.LT(1);
            	    match(input,COMMA,FOLLOW_COMMA_in_listInitializer1105); if (failed) return retval;
            	    if ( backtracking==0 ) stream_COMMA.add(COMMA112);

            	    pushFollow(FOLLOW_expression_in_listInitializer1107);
            	    expression113=expression();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) stream_expression.add(expression113.getTree());

            	    }
            	    break;

            	default :
            	    break loop26;
                }
            } while (true);

            RCURLY114=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_listInitializer1111); if (failed) return retval;
            if ( backtracking==0 ) stream_RCURLY.add(RCURLY114);


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
            // 177:52: -> ^( LIST_INITIALIZER ( expression )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:177:55: ^( LIST_INITIALIZER ( expression )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(LIST_INITIALIZER, "LIST_INITIALIZER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:177:74: ( expression )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:182:1: mapInitializer : POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY -> ^( MAP_INITIALIZER ( mapEntry )* ) ;
    public final mapInitializer_return mapInitializer() throws RecognitionException {
        mapInitializer_return retval = new mapInitializer_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token POUND115=null;
        Token LCURLY116=null;
        Token COMMA118=null;
        Token RCURLY120=null;
        mapEntry_return mapEntry117 = null;

        mapEntry_return mapEntry119 = null;


        Object POUND115_tree=null;
        Object LCURLY116_tree=null;
        Object COMMA118_tree=null;
        Object RCURLY120_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_RCURLY=new RewriteRuleTokenStream(adaptor,"token RCURLY");
        RewriteRuleTokenStream stream_LCURLY=new RewriteRuleTokenStream(adaptor,"token LCURLY");
        RewriteRuleTokenStream stream_POUND=new RewriteRuleTokenStream(adaptor,"token POUND");
        RewriteRuleSubtreeStream stream_mapEntry=new RewriteRuleSubtreeStream(adaptor,"rule mapEntry");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:183:5: ( POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY -> ^( MAP_INITIALIZER ( mapEntry )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:183:7: POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY
            {
            POUND115=(Token)input.LT(1);
            match(input,POUND,FOLLOW_POUND_in_mapInitializer1139); if (failed) return retval;
            if ( backtracking==0 ) stream_POUND.add(POUND115);

            LCURLY116=(Token)input.LT(1);
            match(input,LCURLY,FOLLOW_LCURLY_in_mapInitializer1141); if (failed) return retval;
            if ( backtracking==0 ) stream_LCURLY.add(LCURLY116);

            pushFollow(FOLLOW_mapEntry_in_mapInitializer1143);
            mapEntry117=mapEntry();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_mapEntry.add(mapEntry117.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:183:29: ( COMMA mapEntry )*
            loop27:
            do {
                int alt27=2;
                int LA27_0 = input.LA(1);

                if ( (LA27_0==COMMA) ) {
                    alt27=1;
                }


                switch (alt27) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:183:30: COMMA mapEntry
            	    {
            	    COMMA118=(Token)input.LT(1);
            	    match(input,COMMA,FOLLOW_COMMA_in_mapInitializer1146); if (failed) return retval;
            	    if ( backtracking==0 ) stream_COMMA.add(COMMA118);

            	    pushFollow(FOLLOW_mapEntry_in_mapInitializer1148);
            	    mapEntry119=mapEntry();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) stream_mapEntry.add(mapEntry119.getTree());

            	    }
            	    break;

            	default :
            	    break loop27;
                }
            } while (true);

            RCURLY120=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_mapInitializer1152); if (failed) return retval;
            if ( backtracking==0 ) stream_RCURLY.add(RCURLY120);


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
            // 183:54: -> ^( MAP_INITIALIZER ( mapEntry )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:183:57: ^( MAP_INITIALIZER ( mapEntry )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(MAP_INITIALIZER, "MAP_INITIALIZER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:183:75: ( mapEntry )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:185:1: mapEntry : expression COLON expression -> ^( MAP_ENTRY ( expression )* ) ;
    public final mapEntry_return mapEntry() throws RecognitionException {
        mapEntry_return retval = new mapEntry_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token COLON122=null;
        expression_return expression121 = null;

        expression_return expression123 = null;


        Object COLON122_tree=null;
        RewriteRuleTokenStream stream_COLON=new RewriteRuleTokenStream(adaptor,"token COLON");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:186:5: ( expression COLON expression -> ^( MAP_ENTRY ( expression )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:186:7: expression COLON expression
            {
            pushFollow(FOLLOW_expression_in_mapEntry1173);
            expression121=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression121.getTree());
            COLON122=(Token)input.LT(1);
            match(input,COLON,FOLLOW_COLON_in_mapEntry1175); if (failed) return retval;
            if ( backtracking==0 ) stream_COLON.add(COLON122);

            pushFollow(FOLLOW_expression_in_mapEntry1177);
            expression123=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression123.getTree());

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
            // 186:35: -> ^( MAP_ENTRY ( expression )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:186:38: ^( MAP_ENTRY ( expression )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(MAP_ENTRY, "MAP_ENTRY"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:186:50: ( expression )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:188:1: ctorArgs : LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN ;
    public final ctorArgs_return ctorArgs() throws RecognitionException {
        ctorArgs_return retval = new ctorArgs_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LPAREN124=null;
        Token COMMA126=null;
        Token RPAREN128=null;
        namedArgument_return namedArgument125 = null;

        namedArgument_return namedArgument127 = null;


        Object LPAREN124_tree=null;
        Object COMMA126_tree=null;
        Object RPAREN128_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:189:2: ( LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:189:4: LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN
            {
            root_0 = (Object)adaptor.nil();

            LPAREN124=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_ctorArgs1195); if (failed) return retval;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:189:12: ( namedArgument ( COMMA namedArgument )* )?
            int alt29=2;
            int LA29_0 = input.LA(1);

            if ( (LA29_0==INTEGER_LITERAL||LA29_0==LPAREN||(LA29_0>=PLUS && LA29_0<=MINUS)||LA29_0==BANG||(LA29_0>=POUND && LA29_0<=ID)||LA29_0==LBRACKET||LA29_0==PROJECT||(LA29_0>=SELECT && LA29_0<=FALSE)||LA29_0==84) ) {
                alt29=1;
            }
            switch (alt29) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:189:13: namedArgument ( COMMA namedArgument )*
                    {
                    pushFollow(FOLLOW_namedArgument_in_ctorArgs1199);
                    namedArgument125=namedArgument();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, namedArgument125.getTree());
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:189:27: ( COMMA namedArgument )*
                    loop28:
                    do {
                        int alt28=2;
                        int LA28_0 = input.LA(1);

                        if ( (LA28_0==COMMA) ) {
                            alt28=1;
                        }


                        switch (alt28) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:189:28: COMMA namedArgument
                    	    {
                    	    COMMA126=(Token)input.LT(1);
                    	    match(input,COMMA,FOLLOW_COMMA_in_ctorArgs1202); if (failed) return retval;
                    	    pushFollow(FOLLOW_namedArgument_in_ctorArgs1205);
                    	    namedArgument127=namedArgument();
                    	    _fsp--;
                    	    if (failed) return retval;
                    	    if ( backtracking==0 ) adaptor.addChild(root_0, namedArgument127.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop28;
                        }
                    } while (true);


                    }
                    break;

            }

            RPAREN128=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_ctorArgs1211); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:191:1: argument : expression ;
    public final argument_return argument() throws RecognitionException {
        argument_return retval = new argument_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        expression_return expression129 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:191:10: ( expression )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:191:12: expression
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_expression_in_argument1220);
            expression129=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression129.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:193:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );
    public final namedArgument_return namedArgument() throws RecognitionException {
        namedArgument_return retval = new namedArgument_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token ASSIGN130=null;
        expression_return expression131 = null;

        argument_return argument132 = null;


        Object id_tree=null;
        Object ASSIGN130_tree=null;
        RewriteRuleTokenStream stream_ASSIGN=new RewriteRuleTokenStream(adaptor,"token ASSIGN");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:194:5: ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument )
            int alt30=2;
            int LA30_0 = input.LA(1);

            if ( (LA30_0==ID) ) {
                int LA30_1 = input.LA(2);

                if ( (LA30_1==ASSIGN) ) {
                    int LA30_22 = input.LA(3);

                    if ( (synpred4()) ) {
                        alt30=1;
                    }
                    else if ( (true) ) {
                        alt30=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("193:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );", 30, 22, input);

                        throw nvae;
                    }
                }
                else if ( ((LA30_1>=DEFAULT && LA30_1<=QMARK)||(LA30_1>=LPAREN && LA30_1<=POWER)||LA30_1==DOT||(LA30_1>=COMMA && LA30_1<=LBRACKET)||(LA30_1>=EQUAL && LA30_1<=MATCHES)) ) {
                    alt30=2;
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("193:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );", 30, 1, input);

                    throw nvae;
                }
            }
            else if ( (LA30_0==INTEGER_LITERAL||LA30_0==LPAREN||(LA30_0>=PLUS && LA30_0<=MINUS)||LA30_0==BANG||LA30_0==POUND||LA30_0==LBRACKET||LA30_0==PROJECT||(LA30_0>=SELECT && LA30_0<=FALSE)||LA30_0==84) ) {
                alt30=2;
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("193:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );", 30, 0, input);

                throw nvae;
            }
            switch (alt30) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:194:7: ( ID ASSIGN )=>id= ID ASSIGN expression
                    {
                    id=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_namedArgument1243); if (failed) return retval;
                    if ( backtracking==0 ) stream_ID.add(id);

                    ASSIGN130=(Token)input.LT(1);
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_namedArgument1245); if (failed) return retval;
                    if ( backtracking==0 ) stream_ASSIGN.add(ASSIGN130);

                    pushFollow(FOLLOW_expression_in_namedArgument1247);
                    expression131=expression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_expression.add(expression131.getTree());

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
                    // 195:19: -> ^( NAMED_ARGUMENT[$id] expression )
                    {
                        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:195:22: ^( NAMED_ARGUMENT[$id] expression )
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
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:196:7: argument
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_argument_in_namedArgument1283);
                    argument132=argument();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, argument132.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:198:1: qualifiedId : ID ( DOT ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) ;
    public final qualifiedId_return qualifiedId() throws RecognitionException {
        qualifiedId_return retval = new qualifiedId_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID133=null;
        Token DOT134=null;
        Token ID135=null;

        Object ID133_tree=null;
        Object DOT134_tree=null;
        Object ID135_tree=null;
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleTokenStream stream_DOT=new RewriteRuleTokenStream(adaptor,"token DOT");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:198:13: ( ID ( DOT ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:198:15: ID ( DOT ID )*
            {
            ID133=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_qualifiedId1295); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(ID133);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:198:18: ( DOT ID )*
            loop31:
            do {
                int alt31=2;
                int LA31_0 = input.LA(1);

                if ( (LA31_0==DOT) ) {
                    alt31=1;
                }


                switch (alt31) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:198:19: DOT ID
            	    {
            	    DOT134=(Token)input.LT(1);
            	    match(input,DOT,FOLLOW_DOT_in_qualifiedId1298); if (failed) return retval;
            	    if ( backtracking==0 ) stream_DOT.add(DOT134);

            	    ID135=(Token)input.LT(1);
            	    match(input,ID,FOLLOW_ID_in_qualifiedId1300); if (failed) return retval;
            	    if ( backtracking==0 ) stream_ID.add(ID135);


            	    }
            	    break;

            	default :
            	    break loop31;
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
            // 198:28: -> ^( QUALIFIED_IDENTIFIER ( ID )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:198:31: ^( QUALIFIED_IDENTIFIER ( ID )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(QUALIFIED_IDENTIFIER, "QUALIFIED_IDENTIFIER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:198:54: ( ID )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:1: contextName : ID ( DIV ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) ;
    public final contextName_return contextName() throws RecognitionException {
        contextName_return retval = new contextName_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID136=null;
        Token DIV137=null;
        Token ID138=null;

        Object ID136_tree=null;
        Object DIV137_tree=null;
        Object ID138_tree=null;
        RewriteRuleTokenStream stream_DIV=new RewriteRuleTokenStream(adaptor,"token DIV");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:13: ( ID ( DIV ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:15: ID ( DIV ID )*
            {
            ID136=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_contextName1319); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(ID136);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:18: ( DIV ID )*
            loop32:
            do {
                int alt32=2;
                int LA32_0 = input.LA(1);

                if ( (LA32_0==DIV) ) {
                    alt32=1;
                }


                switch (alt32) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:19: DIV ID
            	    {
            	    DIV137=(Token)input.LT(1);
            	    match(input,DIV,FOLLOW_DIV_in_contextName1322); if (failed) return retval;
            	    if ( backtracking==0 ) stream_DIV.add(DIV137);

            	    ID138=(Token)input.LT(1);
            	    match(input,ID,FOLLOW_ID_in_contextName1324); if (failed) return retval;
            	    if ( backtracking==0 ) stream_ID.add(ID138);


            	    }
            	    break;

            	default :
            	    break loop32;
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
            // 200:28: -> ^( QUALIFIED_IDENTIFIER ( ID )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:31: ^( QUALIFIED_IDENTIFIER ( ID )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(QUALIFIED_IDENTIFIER, "QUALIFIED_IDENTIFIER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:54: ( ID )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:202:1: literal : ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL );
    public final literal_return literal() throws RecognitionException {
        literal_return retval = new literal_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token INTEGER_LITERAL139=null;
        Token STRING_LITERAL140=null;
        Token DQ_STRING_LITERAL141=null;
        Token NULL_LITERAL143=null;
        Token HEXADECIMAL_INTEGER_LITERAL144=null;
        Token REAL_LITERAL145=null;
        boolLiteral_return boolLiteral142 = null;


        Object INTEGER_LITERAL139_tree=null;
        Object STRING_LITERAL140_tree=null;
        Object DQ_STRING_LITERAL141_tree=null;
        Object NULL_LITERAL143_tree=null;
        Object HEXADECIMAL_INTEGER_LITERAL144_tree=null;
        Object REAL_LITERAL145_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:203:2: ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL )
            int alt33=7;
            switch ( input.LA(1) ) {
            case INTEGER_LITERAL:
                {
                alt33=1;
                }
                break;
            case STRING_LITERAL:
                {
                alt33=2;
                }
                break;
            case DQ_STRING_LITERAL:
                {
                alt33=3;
                }
                break;
            case TRUE:
            case FALSE:
                {
                alt33=4;
                }
                break;
            case NULL_LITERAL:
                {
                alt33=5;
                }
                break;
            case HEXADECIMAL_INTEGER_LITERAL:
                {
                alt33=6;
                }
                break;
            case REAL_LITERAL:
                {
                alt33=7;
                }
                break;
            default:
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("202:1: literal : ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL );", 33, 0, input);

                throw nvae;
            }

            switch (alt33) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:203:4: INTEGER_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    INTEGER_LITERAL139=(Token)input.LT(1);
                    match(input,INTEGER_LITERAL,FOLLOW_INTEGER_LITERAL_in_literal1345); if (failed) return retval;
                    if ( backtracking==0 ) {
                    INTEGER_LITERAL139_tree = (Object)adaptor.create(INTEGER_LITERAL139);
                    adaptor.addChild(root_0, INTEGER_LITERAL139_tree);
                    }

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:204:4: STRING_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    STRING_LITERAL140=(Token)input.LT(1);
                    match(input,STRING_LITERAL,FOLLOW_STRING_LITERAL_in_literal1351); if (failed) return retval;
                    if ( backtracking==0 ) {
                    STRING_LITERAL140_tree = (Object)adaptor.create(STRING_LITERAL140);
                    adaptor.addChild(root_0, STRING_LITERAL140_tree);
                    }

                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:205:4: DQ_STRING_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    DQ_STRING_LITERAL141=(Token)input.LT(1);
                    match(input,DQ_STRING_LITERAL,FOLLOW_DQ_STRING_LITERAL_in_literal1356); if (failed) return retval;
                    if ( backtracking==0 ) {
                    DQ_STRING_LITERAL141_tree = (Object)adaptor.create(DQ_STRING_LITERAL141);
                    adaptor.addChild(root_0, DQ_STRING_LITERAL141_tree);
                    }

                    }
                    break;
                case 4 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:206:4: boolLiteral
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_boolLiteral_in_literal1361);
                    boolLiteral142=boolLiteral();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, boolLiteral142.getTree());

                    }
                    break;
                case 5 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:207:4: NULL_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    NULL_LITERAL143=(Token)input.LT(1);
                    match(input,NULL_LITERAL,FOLLOW_NULL_LITERAL_in_literal1366); if (failed) return retval;
                    if ( backtracking==0 ) {
                    NULL_LITERAL143_tree = (Object)adaptor.create(NULL_LITERAL143);
                    adaptor.addChild(root_0, NULL_LITERAL143_tree);
                    }

                    }
                    break;
                case 6 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:208:4: HEXADECIMAL_INTEGER_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    HEXADECIMAL_INTEGER_LITERAL144=(Token)input.LT(1);
                    match(input,HEXADECIMAL_INTEGER_LITERAL,FOLLOW_HEXADECIMAL_INTEGER_LITERAL_in_literal1371); if (failed) return retval;
                    if ( backtracking==0 ) {
                    HEXADECIMAL_INTEGER_LITERAL144_tree = (Object)adaptor.create(HEXADECIMAL_INTEGER_LITERAL144);
                    adaptor.addChild(root_0, HEXADECIMAL_INTEGER_LITERAL144_tree);
                    }

                    }
                    break;
                case 7 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:209:4: REAL_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    REAL_LITERAL145=(Token)input.LT(1);
                    match(input,REAL_LITERAL,FOLLOW_REAL_LITERAL_in_literal1377); if (failed) return retval;
                    if ( backtracking==0 ) {
                    REAL_LITERAL145_tree = (Object)adaptor.create(REAL_LITERAL145);
                    adaptor.addChild(root_0, REAL_LITERAL145_tree);
                    }

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:212:1: boolLiteral : ( TRUE | FALSE );
    public final boolLiteral_return boolLiteral() throws RecognitionException {
        boolLiteral_return retval = new boolLiteral_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set146=null;

        Object set146_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:212:12: ( TRUE | FALSE )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:
            {
            root_0 = (Object)adaptor.nil();

            set146=(Token)input.LT(1);
            if ( (input.LA(1)>=TRUE && input.LA(1)<=FALSE) ) {
                input.consume();
                if ( backtracking==0 ) adaptor.addChild(root_0, adaptor.create(set146));
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

    public static class relationalOperator_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start relationalOperator
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:219:1: relationalOperator : ( EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL | IN | INSTANCEOF | BETWEEN | MATCHES );
    public final relationalOperator_return relationalOperator() throws RecognitionException {
        relationalOperator_return retval = new relationalOperator_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set147=null;

        Object set147_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:220:5: ( EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL | IN | INSTANCEOF | BETWEEN | MATCHES )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:
            {
            root_0 = (Object)adaptor.nil();

            set147=(Token)input.LT(1);
            if ( (input.LA(1)>=EQUAL && input.LA(1)<=MATCHES) ) {
                input.consume();
                if ( backtracking==0 ) adaptor.addChild(root_0, adaptor.create(set147));
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
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:119:7: ( POUND ID LPAREN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:119:8: POUND ID LPAREN
        {
        match(input,POUND,FOLLOW_POUND_in_synpred1682); if (failed) return ;
        match(input,ID,FOLLOW_ID_in_synpred1684); if (failed) return ;
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred1686); if (failed) return ;

        }
    }
    // $ANTLR end synpred1

    // $ANTLR start synpred2
    public final void synpred2_fragment() throws RecognitionException {   
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:129:4: ( ID LPAREN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:129:5: ID LPAREN
        {
        match(input,ID,FOLLOW_ID_in_synpred2766); if (failed) return ;
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred2768); if (failed) return ;

        }
    }
    // $ANTLR end synpred2

    // $ANTLR start synpred3
    public final void synpred3_fragment() throws RecognitionException {   
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:164:4: ( 'new' qualifiedId LPAREN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:164:5: 'new' qualifiedId LPAREN
        {
        match(input,84,FOLLOW_84_in_synpred3978); if (failed) return ;
        pushFollow(FOLLOW_qualifiedId_in_synpred3980);
        qualifiedId();
        _fsp--;
        if (failed) return ;
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred3982); if (failed) return ;

        }
    }
    // $ANTLR end synpred3

    // $ANTLR start synpred4
    public final void synpred4_fragment() throws RecognitionException {   
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:194:7: ( ID ASSIGN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:194:8: ID ASSIGN
        {
        match(input,ID,FOLLOW_ID_in_synpred41234); if (failed) return ;
        match(input,ASSIGN,FOLLOW_ASSIGN_in_synpred41236); if (failed) return ;

        }
    }
    // $ANTLR end synpred4

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


 

    public static final BitSet FOLLOW_expression_in_expr154 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_expr156 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_logicalOrExpression_in_expression176 = new BitSet(new long[]{0x000000000E000002L});
    public static final BitSet FOLLOW_ASSIGN_in_expression185 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_logicalOrExpression_in_expression188 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DEFAULT_in_expression198 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_logicalOrExpression_in_expression201 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_QMARK_in_expression211 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_expression_in_expression214 = new BitSet(new long[]{0x0000000010000000L});
    public static final BitSet FOLLOW_COLON_in_expression216 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_expression_in_expression219 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_parenExpr230 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_expression_in_parenExpr233 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_RPAREN_in_parenExpr235 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_logicalAndExpression_in_logicalOrExpression246 = new BitSet(new long[]{0x0000000080000002L});
    public static final BitSet FOLLOW_OR_in_logicalOrExpression249 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_logicalAndExpression_in_logicalOrExpression252 = new BitSet(new long[]{0x0000000080000002L});
    public static final BitSet FOLLOW_relationalExpression_in_logicalAndExpression287 = new BitSet(new long[]{0x0000000100000002L});
    public static final BitSet FOLLOW_AND_in_logicalAndExpression290 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_relationalExpression_in_logicalAndExpression293 = new BitSet(new long[]{0x0000000100000002L});
    public static final BitSet FOLLOW_sumExpression_in_relationalExpression304 = new BitSet(new long[]{0x8000000000000002L,0x00000000000001FFL});
    public static final BitSet FOLLOW_relationalOperator_in_relationalExpression307 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_sumExpression_in_relationalExpression310 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_productExpression_in_sumExpression321 = new BitSet(new long[]{0x0000000600000002L});
    public static final BitSet FOLLOW_PLUS_in_sumExpression326 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_MINUS_in_sumExpression331 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_productExpression_in_sumExpression335 = new BitSet(new long[]{0x0000000600000002L});
    public static final BitSet FOLLOW_powerExpr_in_productExpression346 = new BitSet(new long[]{0x0000003800000002L});
    public static final BitSet FOLLOW_STAR_in_productExpression350 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_DIV_in_productExpression355 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_MOD_in_productExpression359 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_powerExpr_in_productExpression363 = new BitSet(new long[]{0x0000003800000002L});
    public static final BitSet FOLLOW_unaryExpression_in_powerExpr375 = new BitSet(new long[]{0x0000004000000002L});
    public static final BitSet FOLLOW_POWER_in_powerExpr378 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_unaryExpression_in_powerExpr381 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PLUS_in_unaryExpression395 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_MINUS_in_unaryExpression400 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_BANG_in_unaryExpression405 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_unaryExpression_in_unaryExpression409 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_primaryExpression_in_unaryExpression415 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_startNode_in_primaryExpression429 = new BitSet(new long[]{0x0000110000000002L});
    public static final BitSet FOLLOW_node_in_primaryExpression432 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_parenExpr_in_startNode465 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_methodOrProperty_in_startNode473 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_functionOrVar_in_startNode482 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_indexer_in_startNode490 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_literal_in_startNode498 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_type_in_startNode506 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_constructor_in_startNode514 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_projection_in_startNode522 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selection_in_startNode531 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_firstSelection_in_startNode540 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lastSelection_in_startNode548 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_listInitializer_in_startNode556 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_mapInitializer_in_startNode564 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_node584 = new BitSet(new long[]{0x0007460000000000L});
    public static final BitSet FOLLOW_dottedNode_in_node586 = new BitSet(new long[]{0x0000110000000002L});
    public static final BitSet FOLLOW_nonDottedNode_in_node591 = new BitSet(new long[]{0x0000110000000002L});
    public static final BitSet FOLLOW_indexer_in_nonDottedNode603 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_methodOrProperty_in_dottedNode616 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_functionOrVar_in_dottedNode622 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_projection_in_dottedNode630 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selection_in_dottedNode639 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_firstSelection_in_dottedNode648 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lastSelection_in_dottedNode657 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_function_in_functionOrVar691 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_var_in_functionOrVar699 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_function716 = new BitSet(new long[]{0x0000040000000000L});
    public static final BitSet FOLLOW_ID_in_function720 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_methodArgs_in_function722 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_var743 = new BitSet(new long[]{0x0000040000000000L});
    public static final BitSet FOLLOW_ID_in_var747 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_methodOrProperty775 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_methodArgs_in_methodOrProperty777 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_methodOrProperty791 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_methodArgs806 = new BitSet(new long[]{0x0FFF568660000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_argument_in_methodArgs810 = new BitSet(new long[]{0x0000080040000000L});
    public static final BitSet FOLLOW_COMMA_in_methodArgs813 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_argument_in_methodArgs816 = new BitSet(new long[]{0x0000080040000000L});
    public static final BitSet FOLLOW_COMMA_in_methodArgs821 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_RPAREN_in_methodArgs828 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_property841 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACKET_in_indexer857 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_argument_in_indexer861 = new BitSet(new long[]{0x0000280000000000L});
    public static final BitSet FOLLOW_COMMA_in_indexer864 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_argument_in_indexer868 = new BitSet(new long[]{0x0000280000000000L});
    public static final BitSet FOLLOW_RBRACKET_in_indexer872 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PROJECT_in_projection899 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_expression_in_projection902 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_RCURLY_in_projection904 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELECT_in_selection912 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_expression_in_selection915 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_RCURLY_in_selection917 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELECT_FIRST_in_firstSelection925 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_expression_in_firstSelection928 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_RCURLY_in_firstSelection930 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELECT_LAST_in_lastSelection938 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_expression_in_lastSelection941 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_RCURLY_in_lastSelection943 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TYPE_in_type952 = new BitSet(new long[]{0x0000040000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_type954 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_RPAREN_in_type956 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_84_in_constructor987 = new BitSet(new long[]{0x0000040000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_constructor989 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_ctorArgs_in_constructor991 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_arrayConstructor_in_constructor1008 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_84_in_arrayConstructor1019 = new BitSet(new long[]{0x0000040000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_arrayConstructor1021 = new BitSet(new long[]{0x0000100000000000L});
    public static final BitSet FOLLOW_arrayRank_in_arrayConstructor1023 = new BitSet(new long[]{0x0010000000000002L});
    public static final BitSet FOLLOW_listInitializer_in_arrayConstructor1026 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACKET_in_arrayRank1061 = new BitSet(new long[]{0x0FFF768620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_expression_in_arrayRank1064 = new BitSet(new long[]{0x0000280000000000L});
    public static final BitSet FOLLOW_COMMA_in_arrayRank1067 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_expression_in_arrayRank1069 = new BitSet(new long[]{0x0000280000000000L});
    public static final BitSet FOLLOW_RBRACKET_in_arrayRank1075 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LCURLY_in_listInitializer1100 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_expression_in_listInitializer1102 = new BitSet(new long[]{0x0000880000000000L});
    public static final BitSet FOLLOW_COMMA_in_listInitializer1105 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_expression_in_listInitializer1107 = new BitSet(new long[]{0x0000880000000000L});
    public static final BitSet FOLLOW_RCURLY_in_listInitializer1111 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_mapInitializer1139 = new BitSet(new long[]{0x0010000000000000L});
    public static final BitSet FOLLOW_LCURLY_in_mapInitializer1141 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_mapEntry_in_mapInitializer1143 = new BitSet(new long[]{0x0000880000000000L});
    public static final BitSet FOLLOW_COMMA_in_mapInitializer1146 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_mapEntry_in_mapInitializer1148 = new BitSet(new long[]{0x0000880000000000L});
    public static final BitSet FOLLOW_RCURLY_in_mapInitializer1152 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expression_in_mapEntry1173 = new BitSet(new long[]{0x0000000010000000L});
    public static final BitSet FOLLOW_COLON_in_mapEntry1175 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_expression_in_mapEntry1177 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_ctorArgs1195 = new BitSet(new long[]{0x0FFF568660000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_namedArgument_in_ctorArgs1199 = new BitSet(new long[]{0x0000080040000000L});
    public static final BitSet FOLLOW_COMMA_in_ctorArgs1202 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_namedArgument_in_ctorArgs1205 = new BitSet(new long[]{0x0000080040000000L});
    public static final BitSet FOLLOW_RPAREN_in_ctorArgs1211 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expression_in_argument1220 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_namedArgument1243 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_ASSIGN_in_namedArgument1245 = new BitSet(new long[]{0x0FFF568620000020L,0x0000000000100000L});
    public static final BitSet FOLLOW_expression_in_namedArgument1247 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_argument_in_namedArgument1283 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_qualifiedId1295 = new BitSet(new long[]{0x0000010000000002L});
    public static final BitSet FOLLOW_DOT_in_qualifiedId1298 = new BitSet(new long[]{0x0000040000000000L});
    public static final BitSet FOLLOW_ID_in_qualifiedId1300 = new BitSet(new long[]{0x0000010000000002L});
    public static final BitSet FOLLOW_ID_in_contextName1319 = new BitSet(new long[]{0x0000001000000002L});
    public static final BitSet FOLLOW_DIV_in_contextName1322 = new BitSet(new long[]{0x0000040000000000L});
    public static final BitSet FOLLOW_ID_in_contextName1324 = new BitSet(new long[]{0x0000001000000002L});
    public static final BitSet FOLLOW_INTEGER_LITERAL_in_literal1345 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_LITERAL_in_literal1351 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DQ_STRING_LITERAL_in_literal1356 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_boolLiteral_in_literal1361 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NULL_LITERAL_in_literal1366 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_HEXADECIMAL_INTEGER_LITERAL_in_literal1371 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_REAL_LITERAL_in_literal1377 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_boolLiteral0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_relationalOperator0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_synpred1682 = new BitSet(new long[]{0x0000040000000000L});
    public static final BitSet FOLLOW_ID_in_synpred1684 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred1686 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_synpred2766 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred2768 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_84_in_synpred3978 = new BitSet(new long[]{0x0000040000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_synpred3980 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred3982 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_synpred41234 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_ASSIGN_in_synpred41236 = new BitSet(new long[]{0x0000000000000002L});

}