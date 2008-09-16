// $ANTLR 3.0.1 /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g 2008-09-16 09:57:23
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
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "EXPRESSIONLIST", "INTEGER_LITERAL", "EXPRESSION", "QUALIFIED_IDENTIFIER", "REFERENCE", "PROPERTY_OR_FIELD", "INDEXER", "CONSTRUCTOR", "HOLDER", "CONSTRUCTOR_ARRAY", "NAMED_ARGUMENT", "FUNCTIONREF", "TYPEREF", "RANGE", "VARIABLEREF", "LIST_INITIALIZER", "MAP_INITIALIZER", "MAP_ENTRY", "METHOD", "ADD", "SUBTRACT", "NUMBER", "SEMIRPAREN", "ASSIGN", "DEFAULT", "QMARK", "COLON", "LPAREN", "RPAREN", "OR", "AND", "PLUS", "MINUS", "STAR", "DIV", "MOD", "POWER", "BANG", "DOT", "POUND", "ID", "COMMA", "AT", "LBRACKET", "RBRACKET", "PROJECT", "RCURLY", "SELECT", "SELECT_FIRST", "SELECT_LAST", "TYPE", "LCURLY", "STRING_LITERAL", "DQ_STRING_LITERAL", "NULL_LITERAL", "HEXADECIMAL_INTEGER_LITERAL", "REAL_LITERAL", "TRUE", "FALSE", "DECIMAL_DIGIT", "INTEGER_TYPE_SUFFIX", "HEX_DIGIT", "EQUAL", "NOT_EQUAL", "LESS_THAN", "LESS_THAN_OR_EQUAL", "GREATER_THAN", "GREATER_THAN_OR_EQUAL", "IN", "IS", "BETWEEN", "MATCHES", "SEMI", "PIPE", "APOS", "DOT_ESCAPED", "WS", "DOLLAR", "UPTO", "EXPONENT_PART", "REAL_TYPE_SUFFIX", "SIGN", "'new'"
    };
    public static final int GREATER_THAN_OR_EQUAL=71;
    public static final int SELECT_FIRST=52;
    public static final int COMMA=45;
    public static final int HOLDER=12;
    public static final int GREATER_THAN=70;
    public static final int TYPE=54;
    public static final int EXPRESSIONLIST=4;
    public static final int MINUS=36;
    public static final int MAP_ENTRY=21;
    public static final int SELECT_LAST=53;
    public static final int NUMBER=25;
    public static final int LESS_THAN=68;
    public static final int BANG=41;
    public static final int FALSE=62;
    public static final int METHOD=22;
    public static final int PROPERTY_OR_FIELD=9;
    public static final int LBRACKET=47;
    public static final int INDEXER=10;
    public static final int MOD=39;
    public static final int CONSTRUCTOR_ARRAY=13;
    public static final int FUNCTIONREF=15;
    public static final int NULL_LITERAL=58;
    public static final int NAMED_ARGUMENT=14;
    public static final int OR=33;
    public static final int PIPE=77;
    public static final int DOT=42;
    public static final int RCURLY=50;
    public static final int EXPRESSION=6;
    public static final int AND=34;
    public static final int LCURLY=55;
    public static final int REAL_TYPE_SUFFIX=84;
    public static final int STRING_LITERAL=56;
    public static final int SELECT=51;
    public static final int QUALIFIED_IDENTIFIER=7;
    public static final int RBRACKET=48;
    public static final int SUBTRACT=24;
    public static final int ASSIGN=27;
    public static final int BETWEEN=74;
    public static final int RPAREN=32;
    public static final int SIGN=85;
    public static final int LPAREN=31;
    public static final int HEX_DIGIT=65;
    public static final int PLUS=35;
    public static final int LIST_INITIALIZER=19;
    public static final int APOS=78;
    public static final int INTEGER_LITERAL=5;
    public static final int AT=46;
    public static final int ID=44;
    public static final int NOT_EQUAL=67;
    public static final int RANGE=17;
    public static final int POWER=40;
    public static final int TYPEREF=16;
    public static final int DECIMAL_DIGIT=63;
    public static final int WS=80;
    public static final int IS=73;
    public static final int DOLLAR=81;
    public static final int LESS_THAN_OR_EQUAL=69;
    public static final int SEMIRPAREN=26;
    public static final int DQ_STRING_LITERAL=57;
    public static final int HEXADECIMAL_INTEGER_LITERAL=59;
    public static final int MAP_INITIALIZER=20;
    public static final int IN=72;
    public static final int SEMI=76;
    public static final int CONSTRUCTOR=11;
    public static final int INTEGER_TYPE_SUFFIX=64;
    public static final int EQUAL=66;
    public static final int MATCHES=75;
    public static final int DOT_ESCAPED=79;
    public static final int UPTO=82;
    public static final int EOF=-1;
    public static final int QMARK=29;
    public static final int REFERENCE=8;
    public static final int PROJECT=49;
    public static final int DEFAULT=28;
    public static final int COLON=30;
    public static final int DIV=38;
    public static final int STAR=37;
    public static final int REAL_LITERAL=60;
    public static final int VARIABLEREF=18;
    public static final int EXPONENT_PART=83;
    public static final int TRUE=61;
    public static final int ADD=23;
    public static final int POUND=43;

        public SpringExpressionsParser(TokenStream input) {
            super(input);
            ruleMemo = new HashMap[46+1];
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:57:1: expr : expression EOF ;
    public final expr_return expr() throws RecognitionException {
        expr_return retval = new expr_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token EOF2=null;
        expression_return expression1 = null;


        Object EOF2_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:57:5: ( expression EOF )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:57:7: expression EOF
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_expression_in_expr165);
            expression1=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression1.getTree());
            EOF2=(Token)input.LT(1);
            match(input,EOF,FOLLOW_EOF_in_expr167); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:61:1: expression : logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )? ;
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
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:61:12: ( logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )? )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:62:5: logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )?
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_logicalOrExpression_in_expression196);
            logicalOrExpression3=logicalOrExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, logicalOrExpression3.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:63:5: ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )?
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
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:63:7: ( ASSIGN logicalOrExpression )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:63:7: ( ASSIGN logicalOrExpression )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:63:8: ASSIGN logicalOrExpression
                    {
                    ASSIGN4=(Token)input.LT(1);
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_expression205); if (failed) return retval;
                    if ( backtracking==0 ) {
                    ASSIGN4_tree = (Object)adaptor.create(ASSIGN4);
                    root_0 = (Object)adaptor.becomeRoot(ASSIGN4_tree, root_0);
                    }
                    pushFollow(FOLLOW_logicalOrExpression_in_expression208);
                    logicalOrExpression5=logicalOrExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, logicalOrExpression5.getTree());

                    }


                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:64:6: ( DEFAULT logicalOrExpression )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:64:6: ( DEFAULT logicalOrExpression )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:64:7: DEFAULT logicalOrExpression
                    {
                    DEFAULT6=(Token)input.LT(1);
                    match(input,DEFAULT,FOLLOW_DEFAULT_in_expression218); if (failed) return retval;
                    if ( backtracking==0 ) {
                    DEFAULT6_tree = (Object)adaptor.create(DEFAULT6);
                    root_0 = (Object)adaptor.becomeRoot(DEFAULT6_tree, root_0);
                    }
                    pushFollow(FOLLOW_logicalOrExpression_in_expression221);
                    logicalOrExpression7=logicalOrExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, logicalOrExpression7.getTree());

                    }


                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:65:6: ( QMARK expression COLON expression )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:65:6: ( QMARK expression COLON expression )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:65:7: QMARK expression COLON expression
                    {
                    QMARK8=(Token)input.LT(1);
                    match(input,QMARK,FOLLOW_QMARK_in_expression231); if (failed) return retval;
                    if ( backtracking==0 ) {
                    QMARK8_tree = (Object)adaptor.create(QMARK8);
                    root_0 = (Object)adaptor.becomeRoot(QMARK8_tree, root_0);
                    }
                    pushFollow(FOLLOW_expression_in_expression234);
                    expression9=expression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, expression9.getTree());
                    COLON10=(Token)input.LT(1);
                    match(input,COLON,FOLLOW_COLON_in_expression236); if (failed) return retval;
                    pushFollow(FOLLOW_expression_in_expression239);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:67:1: parenExpr : LPAREN expression RPAREN ;
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
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:67:11: ( LPAREN expression RPAREN )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:67:13: LPAREN expression RPAREN
            {
            root_0 = (Object)adaptor.nil();

            LPAREN12=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_parenExpr250); if (failed) return retval;
            pushFollow(FOLLOW_expression_in_parenExpr253);
            expression13=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression13.getTree());
            RPAREN14=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_parenExpr255); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:70:1: logicalOrExpression : logicalAndExpression ( OR logicalAndExpression )* ;
    public final logicalOrExpression_return logicalOrExpression() throws RecognitionException {
        logicalOrExpression_return retval = new logicalOrExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token OR16=null;
        logicalAndExpression_return logicalAndExpression15 = null;

        logicalAndExpression_return logicalAndExpression17 = null;


        Object OR16_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:72:1: ( logicalAndExpression ( OR logicalAndExpression )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:72:3: logicalAndExpression ( OR logicalAndExpression )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_logicalAndExpression_in_logicalOrExpression268);
            logicalAndExpression15=logicalAndExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, logicalAndExpression15.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:72:24: ( OR logicalAndExpression )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0==OR) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:72:25: OR logicalAndExpression
            	    {
            	    OR16=(Token)input.LT(1);
            	    match(input,OR,FOLLOW_OR_in_logicalOrExpression271); if (failed) return retval;
            	    if ( backtracking==0 ) {
            	    OR16_tree = (Object)adaptor.create(OR16);
            	    root_0 = (Object)adaptor.becomeRoot(OR16_tree, root_0);
            	    }
            	    pushFollow(FOLLOW_logicalAndExpression_in_logicalOrExpression274);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:74:1: logicalAndExpression : relationalExpression ( AND relationalExpression )* ;
    public final logicalAndExpression_return logicalAndExpression() throws RecognitionException {
        logicalAndExpression_return retval = new logicalAndExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token AND19=null;
        relationalExpression_return relationalExpression18 = null;

        relationalExpression_return relationalExpression20 = null;


        Object AND19_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:1: ( relationalExpression ( AND relationalExpression )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:3: relationalExpression ( AND relationalExpression )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_relationalExpression_in_logicalAndExpression309);
            relationalExpression18=relationalExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, relationalExpression18.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:24: ( AND relationalExpression )*
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( (LA3_0==AND) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:25: AND relationalExpression
            	    {
            	    AND19=(Token)input.LT(1);
            	    match(input,AND,FOLLOW_AND_in_logicalAndExpression312); if (failed) return retval;
            	    if ( backtracking==0 ) {
            	    AND19_tree = (Object)adaptor.create(AND19);
            	    root_0 = (Object)adaptor.becomeRoot(AND19_tree, root_0);
            	    }
            	    pushFollow(FOLLOW_relationalExpression_in_logicalAndExpression315);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:77:1: relationalExpression : sumExpression ( relationalOperator sumExpression )? ;
    public final relationalExpression_return relationalExpression() throws RecognitionException {
        relationalExpression_return retval = new relationalExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        sumExpression_return sumExpression21 = null;

        relationalOperator_return relationalOperator22 = null;

        sumExpression_return sumExpression23 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:77:22: ( sumExpression ( relationalOperator sumExpression )? )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:77:24: sumExpression ( relationalOperator sumExpression )?
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_sumExpression_in_relationalExpression326);
            sumExpression21=sumExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, sumExpression21.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:77:38: ( relationalOperator sumExpression )?
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( ((LA4_0>=EQUAL && LA4_0<=MATCHES)) ) {
                alt4=1;
            }
            switch (alt4) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:77:39: relationalOperator sumExpression
                    {
                    pushFollow(FOLLOW_relationalOperator_in_relationalExpression329);
                    relationalOperator22=relationalOperator();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) root_0 = (Object)adaptor.becomeRoot(relationalOperator22.getTree(), root_0);
                    pushFollow(FOLLOW_sumExpression_in_relationalExpression332);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:79:1: sumExpression : productExpression ( ( PLUS | MINUS ) productExpression )* ;
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
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:2: ( productExpression ( ( PLUS | MINUS ) productExpression )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:4: productExpression ( ( PLUS | MINUS ) productExpression )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_productExpression_in_sumExpression343);
            productExpression24=productExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, productExpression24.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:22: ( ( PLUS | MINUS ) productExpression )*
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( ((LA6_0>=PLUS && LA6_0<=MINUS)) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:24: ( PLUS | MINUS ) productExpression
            	    {
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:24: ( PLUS | MINUS )
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
            	            new NoViableAltException("80:24: ( PLUS | MINUS )", 5, 0, input);

            	        throw nvae;
            	    }
            	    switch (alt5) {
            	        case 1 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:25: PLUS
            	            {
            	            PLUS25=(Token)input.LT(1);
            	            match(input,PLUS,FOLLOW_PLUS_in_sumExpression348); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            PLUS25_tree = (Object)adaptor.create(PLUS25);
            	            root_0 = (Object)adaptor.becomeRoot(PLUS25_tree, root_0);
            	            }

            	            }
            	            break;
            	        case 2 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:33: MINUS
            	            {
            	            MINUS26=(Token)input.LT(1);
            	            match(input,MINUS,FOLLOW_MINUS_in_sumExpression353); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            MINUS26_tree = (Object)adaptor.create(MINUS26);
            	            root_0 = (Object)adaptor.becomeRoot(MINUS26_tree, root_0);
            	            }

            	            }
            	            break;

            	    }

            	    pushFollow(FOLLOW_productExpression_in_sumExpression357);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:83:1: productExpression : powerExpr ( ( STAR | DIV | MOD ) powerExpr )* ;
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
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:84:2: ( powerExpr ( ( STAR | DIV | MOD ) powerExpr )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:84:4: powerExpr ( ( STAR | DIV | MOD ) powerExpr )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_powerExpr_in_productExpression369);
            powerExpr28=powerExpr();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, powerExpr28.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:84:14: ( ( STAR | DIV | MOD ) powerExpr )*
            loop8:
            do {
                int alt8=2;
                int LA8_0 = input.LA(1);

                if ( ((LA8_0>=STAR && LA8_0<=MOD)) ) {
                    alt8=1;
                }


                switch (alt8) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:84:15: ( STAR | DIV | MOD ) powerExpr
            	    {
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:84:15: ( STAR | DIV | MOD )
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
            	            new NoViableAltException("84:15: ( STAR | DIV | MOD )", 7, 0, input);

            	        throw nvae;
            	    }

            	    switch (alt7) {
            	        case 1 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:84:16: STAR
            	            {
            	            STAR29=(Token)input.LT(1);
            	            match(input,STAR,FOLLOW_STAR_in_productExpression373); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            STAR29_tree = (Object)adaptor.create(STAR29);
            	            root_0 = (Object)adaptor.becomeRoot(STAR29_tree, root_0);
            	            }

            	            }
            	            break;
            	        case 2 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:84:24: DIV
            	            {
            	            DIV30=(Token)input.LT(1);
            	            match(input,DIV,FOLLOW_DIV_in_productExpression378); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            DIV30_tree = (Object)adaptor.create(DIV30);
            	            root_0 = (Object)adaptor.becomeRoot(DIV30_tree, root_0);
            	            }

            	            }
            	            break;
            	        case 3 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:84:30: MOD
            	            {
            	            MOD31=(Token)input.LT(1);
            	            match(input,MOD,FOLLOW_MOD_in_productExpression382); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            MOD31_tree = (Object)adaptor.create(MOD31);
            	            root_0 = (Object)adaptor.becomeRoot(MOD31_tree, root_0);
            	            }

            	            }
            	            break;

            	    }

            	    pushFollow(FOLLOW_powerExpr_in_productExpression386);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:86:1: powerExpr : unaryExpression ( POWER unaryExpression )? ;
    public final powerExpr_return powerExpr() throws RecognitionException {
        powerExpr_return retval = new powerExpr_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token POWER34=null;
        unaryExpression_return unaryExpression33 = null;

        unaryExpression_return unaryExpression35 = null;


        Object POWER34_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:86:12: ( unaryExpression ( POWER unaryExpression )? )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:86:14: unaryExpression ( POWER unaryExpression )?
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_unaryExpression_in_powerExpr398);
            unaryExpression33=unaryExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, unaryExpression33.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:86:30: ( POWER unaryExpression )?
            int alt9=2;
            int LA9_0 = input.LA(1);

            if ( (LA9_0==POWER) ) {
                alt9=1;
            }
            switch (alt9) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:86:31: POWER unaryExpression
                    {
                    POWER34=(Token)input.LT(1);
                    match(input,POWER,FOLLOW_POWER_in_powerExpr401); if (failed) return retval;
                    if ( backtracking==0 ) {
                    POWER34_tree = (Object)adaptor.create(POWER34);
                    root_0 = (Object)adaptor.becomeRoot(POWER34_tree, root_0);
                    }
                    pushFollow(FOLLOW_unaryExpression_in_powerExpr404);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:88:1: unaryExpression : ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression );
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
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:89:2: ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression )
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( ((LA11_0>=PLUS && LA11_0<=MINUS)||LA11_0==BANG) ) {
                alt11=1;
            }
            else if ( (LA11_0==INTEGER_LITERAL||LA11_0==LPAREN||(LA11_0>=POUND && LA11_0<=ID)||(LA11_0>=AT && LA11_0<=LBRACKET)||LA11_0==PROJECT||(LA11_0>=SELECT && LA11_0<=FALSE)||LA11_0==86) ) {
                alt11=2;
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("88:1: unaryExpression : ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression );", 11, 0, input);

                throw nvae;
            }
            switch (alt11) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:89:4: ( PLUS | MINUS | BANG ) unaryExpression
                    {
                    root_0 = (Object)adaptor.nil();

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:89:4: ( PLUS | MINUS | BANG )
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
                            new NoViableAltException("89:4: ( PLUS | MINUS | BANG )", 10, 0, input);

                        throw nvae;
                    }

                    switch (alt10) {
                        case 1 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:89:5: PLUS
                            {
                            PLUS36=(Token)input.LT(1);
                            match(input,PLUS,FOLLOW_PLUS_in_unaryExpression418); if (failed) return retval;
                            if ( backtracking==0 ) {
                            PLUS36_tree = (Object)adaptor.create(PLUS36);
                            root_0 = (Object)adaptor.becomeRoot(PLUS36_tree, root_0);
                            }

                            }
                            break;
                        case 2 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:89:13: MINUS
                            {
                            MINUS37=(Token)input.LT(1);
                            match(input,MINUS,FOLLOW_MINUS_in_unaryExpression423); if (failed) return retval;
                            if ( backtracking==0 ) {
                            MINUS37_tree = (Object)adaptor.create(MINUS37);
                            root_0 = (Object)adaptor.becomeRoot(MINUS37_tree, root_0);
                            }

                            }
                            break;
                        case 3 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:89:22: BANG
                            {
                            BANG38=(Token)input.LT(1);
                            match(input,BANG,FOLLOW_BANG_in_unaryExpression428); if (failed) return retval;
                            if ( backtracking==0 ) {
                            BANG38_tree = (Object)adaptor.create(BANG38);
                            root_0 = (Object)adaptor.becomeRoot(BANG38_tree, root_0);
                            }

                            }
                            break;

                    }

                    pushFollow(FOLLOW_unaryExpression_in_unaryExpression432);
                    unaryExpression39=unaryExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, unaryExpression39.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:90:4: primaryExpression
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_primaryExpression_in_unaryExpression438);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:92:1: primaryExpression : startNode ( node )? -> ^( EXPRESSION startNode ( node )? ) ;
    public final primaryExpression_return primaryExpression() throws RecognitionException {
        primaryExpression_return retval = new primaryExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        startNode_return startNode41 = null;

        node_return node42 = null;


        RewriteRuleSubtreeStream stream_node=new RewriteRuleSubtreeStream(adaptor,"rule node");
        RewriteRuleSubtreeStream stream_startNode=new RewriteRuleSubtreeStream(adaptor,"rule startNode");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:93:5: ( startNode ( node )? -> ^( EXPRESSION startNode ( node )? ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:93:7: startNode ( node )?
            {
            pushFollow(FOLLOW_startNode_in_primaryExpression452);
            startNode41=startNode();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_startNode.add(startNode41.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:93:17: ( node )?
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0==DOT||LA12_0==LBRACKET) ) {
                alt12=1;
            }
            switch (alt12) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:93:18: node
                    {
                    pushFollow(FOLLOW_node_in_primaryExpression455);
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
            // 93:25: -> ^( EXPRESSION startNode ( node )? )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:93:28: ^( EXPRESSION startNode ( node )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(EXPRESSION, "EXPRESSION"), root_1);

                adaptor.addChild(root_1, stream_startNode.next());
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:93:51: ( node )?
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:95:1: startNode : ( parenExpr | methodOrProperty | functionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer );
    public final startNode_return startNode() throws RecognitionException {
        startNode_return retval = new startNode_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        parenExpr_return parenExpr43 = null;

        methodOrProperty_return methodOrProperty44 = null;

        functionOrVar_return functionOrVar45 = null;

        reference_return reference46 = null;

        indexer_return indexer47 = null;

        literal_return literal48 = null;

        type_return type49 = null;

        constructor_return constructor50 = null;

        projection_return projection51 = null;

        selection_return selection52 = null;

        firstSelection_return firstSelection53 = null;

        lastSelection_return lastSelection54 = null;

        listInitializer_return listInitializer55 = null;

        mapInitializer_return mapInitializer56 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:96:5: ( parenExpr | methodOrProperty | functionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer )
            int alt13=14;
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
                    alt13=14;
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("95:1: startNode : ( parenExpr | methodOrProperty | functionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer );", 13, 3, input);

                    throw nvae;
                }
                }
                break;
            case AT:
                {
                alt13=4;
                }
                break;
            case LBRACKET:
                {
                alt13=5;
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
                alt13=6;
                }
                break;
            case TYPE:
                {
                alt13=7;
                }
                break;
            case 86:
                {
                alt13=8;
                }
                break;
            case PROJECT:
                {
                alt13=9;
                }
                break;
            case SELECT:
                {
                alt13=10;
                }
                break;
            case SELECT_FIRST:
                {
                alt13=11;
                }
                break;
            case SELECT_LAST:
                {
                alt13=12;
                }
                break;
            case LCURLY:
                {
                alt13=13;
                }
                break;
            default:
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("95:1: startNode : ( parenExpr | methodOrProperty | functionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer );", 13, 0, input);

                throw nvae;
            }

            switch (alt13) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:97:5: parenExpr
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_parenExpr_in_startNode488);
                    parenExpr43=parenExpr();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, parenExpr43.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:98:7: methodOrProperty
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_methodOrProperty_in_startNode496);
                    methodOrProperty44=methodOrProperty();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, methodOrProperty44.getTree());

                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:99:7: functionOrVar
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_functionOrVar_in_startNode505);
                    functionOrVar45=functionOrVar();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, functionOrVar45.getTree());

                    }
                    break;
                case 4 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:100:7: reference
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_reference_in_startNode513);
                    reference46=reference();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, reference46.getTree());

                    }
                    break;
                case 5 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:101:7: indexer
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_indexer_in_startNode521);
                    indexer47=indexer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, indexer47.getTree());

                    }
                    break;
                case 6 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:102:7: literal
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_literal_in_startNode529);
                    literal48=literal();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, literal48.getTree());

                    }
                    break;
                case 7 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:103:7: type
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_type_in_startNode537);
                    type49=type();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, type49.getTree());

                    }
                    break;
                case 8 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:104:7: constructor
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_constructor_in_startNode545);
                    constructor50=constructor();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, constructor50.getTree());

                    }
                    break;
                case 9 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:105:7: projection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_projection_in_startNode553);
                    projection51=projection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, projection51.getTree());

                    }
                    break;
                case 10 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:106:7: selection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_selection_in_startNode562);
                    selection52=selection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, selection52.getTree());

                    }
                    break;
                case 11 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:107:7: firstSelection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_firstSelection_in_startNode571);
                    firstSelection53=firstSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, firstSelection53.getTree());

                    }
                    break;
                case 12 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:108:7: lastSelection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_lastSelection_in_startNode579);
                    lastSelection54=lastSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, lastSelection54.getTree());

                    }
                    break;
                case 13 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:109:7: listInitializer
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_listInitializer_in_startNode587);
                    listInitializer55=listInitializer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, listInitializer55.getTree());

                    }
                    break;
                case 14 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:110:7: mapInitializer
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_mapInitializer_in_startNode595);
                    mapInitializer56=mapInitializer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, mapInitializer56.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:113:1: node : ( ( DOT dottedNode ) | nonDottedNode )+ ;
    public final node_return node() throws RecognitionException {
        node_return retval = new node_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token DOT57=null;
        dottedNode_return dottedNode58 = null;

        nonDottedNode_return nonDottedNode59 = null;


        Object DOT57_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:114:2: ( ( ( DOT dottedNode ) | nonDottedNode )+ )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:114:4: ( ( DOT dottedNode ) | nonDottedNode )+
            {
            root_0 = (Object)adaptor.nil();

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:114:4: ( ( DOT dottedNode ) | nonDottedNode )+
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
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:114:5: ( DOT dottedNode )
            	    {
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:114:5: ( DOT dottedNode )
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:114:6: DOT dottedNode
            	    {
            	    DOT57=(Token)input.LT(1);
            	    match(input,DOT,FOLLOW_DOT_in_node615); if (failed) return retval;
            	    if ( backtracking==0 ) {
            	    DOT57_tree = (Object)adaptor.create(DOT57);
            	    adaptor.addChild(root_0, DOT57_tree);
            	    }
            	    pushFollow(FOLLOW_dottedNode_in_node617);
            	    dottedNode58=dottedNode();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, dottedNode58.getTree());

            	    }


            	    }
            	    break;
            	case 2 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:114:24: nonDottedNode
            	    {
            	    pushFollow(FOLLOW_nonDottedNode_in_node622);
            	    nonDottedNode59=nonDottedNode();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, nonDottedNode59.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:116:1: nonDottedNode : indexer ;
    public final nonDottedNode_return nonDottedNode() throws RecognitionException {
        nonDottedNode_return retval = new nonDottedNode_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        indexer_return indexer60 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:117:2: ( indexer )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:117:4: indexer
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_indexer_in_nonDottedNode634);
            indexer60=indexer();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, indexer60.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:119:1: dottedNode : ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection ) ) ;
    public final dottedNode_return dottedNode() throws RecognitionException {
        dottedNode_return retval = new dottedNode_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        methodOrProperty_return methodOrProperty61 = null;

        functionOrVar_return functionOrVar62 = null;

        projection_return projection63 = null;

        selection_return selection64 = null;

        firstSelection_return firstSelection65 = null;

        lastSelection_return lastSelection66 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:120:2: ( ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection ) ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:121:2: ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection ) )
            {
            root_0 = (Object)adaptor.nil();

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:121:2: ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:121:3: ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection )
            {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:121:3: ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection )
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
                    new NoViableAltException("121:3: ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection )", 15, 0, input);

                throw nvae;
            }

            switch (alt15) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:121:4: methodOrProperty
                    {
                    pushFollow(FOLLOW_methodOrProperty_in_dottedNode647);
                    methodOrProperty61=methodOrProperty();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, methodOrProperty61.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:122:4: functionOrVar
                    {
                    pushFollow(FOLLOW_functionOrVar_in_dottedNode653);
                    functionOrVar62=functionOrVar();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, functionOrVar62.getTree());

                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:123:7: projection
                    {
                    pushFollow(FOLLOW_projection_in_dottedNode661);
                    projection63=projection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, projection63.getTree());

                    }
                    break;
                case 4 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:124:7: selection
                    {
                    pushFollow(FOLLOW_selection_in_dottedNode670);
                    selection64=selection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, selection64.getTree());

                    }
                    break;
                case 5 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:125:7: firstSelection
                    {
                    pushFollow(FOLLOW_firstSelection_in_dottedNode679);
                    firstSelection65=firstSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, firstSelection65.getTree());

                    }
                    break;
                case 6 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:126:7: lastSelection
                    {
                    pushFollow(FOLLOW_lastSelection_in_dottedNode688);
                    lastSelection66=lastSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, lastSelection66.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:130:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );
    public final functionOrVar_return functionOrVar() throws RecognitionException {
        functionOrVar_return retval = new functionOrVar_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        function_return function67 = null;

        var_return var68 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:131:5: ( ( POUND ID LPAREN )=> function | var )
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
                            new NoViableAltException("130:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 16, 2, input);

                        throw nvae;
                    }
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("130:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 16, 1, input);

                    throw nvae;
                }
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("130:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 16, 0, input);

                throw nvae;
            }
            switch (alt16) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:131:7: ( POUND ID LPAREN )=> function
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_function_in_functionOrVar722);
                    function67=function();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, function67.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:132:7: var
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_var_in_functionOrVar730);
                    var68=var();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, var68.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:135:1: function : POUND id= ID methodArgs -> ^( FUNCTIONREF[$id] methodArgs ) ;
    public final function_return function() throws RecognitionException {
        function_return retval = new function_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token POUND69=null;
        methodArgs_return methodArgs70 = null;


        Object id_tree=null;
        Object POUND69_tree=null;
        RewriteRuleTokenStream stream_POUND=new RewriteRuleTokenStream(adaptor,"token POUND");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleSubtreeStream stream_methodArgs=new RewriteRuleSubtreeStream(adaptor,"rule methodArgs");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:135:10: ( POUND id= ID methodArgs -> ^( FUNCTIONREF[$id] methodArgs ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:135:12: POUND id= ID methodArgs
            {
            POUND69=(Token)input.LT(1);
            match(input,POUND,FOLLOW_POUND_in_function747); if (failed) return retval;
            if ( backtracking==0 ) stream_POUND.add(POUND69);

            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_function751); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(id);

            pushFollow(FOLLOW_methodArgs_in_function753);
            methodArgs70=methodArgs();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_methodArgs.add(methodArgs70.getTree());

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
            // 135:35: -> ^( FUNCTIONREF[$id] methodArgs )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:135:38: ^( FUNCTIONREF[$id] methodArgs )
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:137:1: var : POUND id= ID -> ^( VARIABLEREF[$id] ) ;
    public final var_return var() throws RecognitionException {
        var_return retval = new var_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token POUND71=null;

        Object id_tree=null;
        Object POUND71_tree=null;
        RewriteRuleTokenStream stream_POUND=new RewriteRuleTokenStream(adaptor,"token POUND");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:137:5: ( POUND id= ID -> ^( VARIABLEREF[$id] ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:137:7: POUND id= ID
            {
            POUND71=(Token)input.LT(1);
            match(input,POUND,FOLLOW_POUND_in_var774); if (failed) return retval;
            if ( backtracking==0 ) stream_POUND.add(POUND71);

            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_var778); if (failed) return retval;
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
            // 137:19: -> ^( VARIABLEREF[$id] )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:137:22: ^( VARIABLEREF[$id] )
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:140:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );
    public final methodOrProperty_return methodOrProperty() throws RecognitionException {
        methodOrProperty_return retval = new methodOrProperty_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        methodArgs_return methodArgs72 = null;

        property_return property73 = null;


        Object id_tree=null;
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleSubtreeStream stream_methodArgs=new RewriteRuleSubtreeStream(adaptor,"rule methodArgs");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:141:2: ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property )
            int alt17=2;
            int LA17_0 = input.LA(1);

            if ( (LA17_0==ID) ) {
                int LA17_1 = input.LA(2);

                if ( (LA17_1==EOF||(LA17_1>=ASSIGN && LA17_1<=COLON)||(LA17_1>=RPAREN && LA17_1<=POWER)||LA17_1==DOT||LA17_1==COMMA||(LA17_1>=LBRACKET && LA17_1<=RBRACKET)||LA17_1==RCURLY||(LA17_1>=EQUAL && LA17_1<=MATCHES)) ) {
                    alt17=2;
                }
                else if ( (LA17_1==LPAREN) && (synpred2())) {
                    alt17=1;
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("140:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );", 17, 1, input);

                    throw nvae;
                }
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("140:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );", 17, 0, input);

                throw nvae;
            }
            switch (alt17) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:141:4: ( ID LPAREN )=>id= ID methodArgs
                    {
                    id=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_methodOrProperty806); if (failed) return retval;
                    if ( backtracking==0 ) stream_ID.add(id);

                    pushFollow(FOLLOW_methodArgs_in_methodOrProperty808);
                    methodArgs72=methodArgs();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_methodArgs.add(methodArgs72.getTree());

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
                    // 141:36: -> ^( METHOD[$id] methodArgs )
                    {
                        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:141:39: ^( METHOD[$id] methodArgs )
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
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:142:4: property
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_property_in_methodOrProperty822);
                    property73=property();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, property73.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:148:1: methodArgs : LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN ;
    public final methodArgs_return methodArgs() throws RecognitionException {
        methodArgs_return retval = new methodArgs_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LPAREN74=null;
        Token COMMA76=null;
        Token COMMA78=null;
        Token RPAREN79=null;
        argument_return argument75 = null;

        argument_return argument77 = null;


        Object LPAREN74_tree=null;
        Object COMMA76_tree=null;
        Object COMMA78_tree=null;
        Object RPAREN79_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:148:12: ( LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:148:15: LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN
            {
            root_0 = (Object)adaptor.nil();

            LPAREN74=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_methodArgs837); if (failed) return retval;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:148:23: ( argument ( COMMA argument )* ( COMMA )? )?
            int alt20=2;
            int LA20_0 = input.LA(1);

            if ( (LA20_0==INTEGER_LITERAL||LA20_0==LPAREN||(LA20_0>=PLUS && LA20_0<=MINUS)||LA20_0==BANG||(LA20_0>=POUND && LA20_0<=ID)||(LA20_0>=AT && LA20_0<=LBRACKET)||LA20_0==PROJECT||(LA20_0>=SELECT && LA20_0<=FALSE)||LA20_0==86) ) {
                alt20=1;
            }
            switch (alt20) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:148:24: argument ( COMMA argument )* ( COMMA )?
                    {
                    pushFollow(FOLLOW_argument_in_methodArgs841);
                    argument75=argument();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, argument75.getTree());
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:148:33: ( COMMA argument )*
                    loop18:
                    do {
                        int alt18=2;
                        int LA18_0 = input.LA(1);

                        if ( (LA18_0==COMMA) ) {
                            int LA18_1 = input.LA(2);

                            if ( (LA18_1==INTEGER_LITERAL||LA18_1==LPAREN||(LA18_1>=PLUS && LA18_1<=MINUS)||LA18_1==BANG||(LA18_1>=POUND && LA18_1<=ID)||(LA18_1>=AT && LA18_1<=LBRACKET)||LA18_1==PROJECT||(LA18_1>=SELECT && LA18_1<=FALSE)||LA18_1==86) ) {
                                alt18=1;
                            }


                        }


                        switch (alt18) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:148:34: COMMA argument
                    	    {
                    	    COMMA76=(Token)input.LT(1);
                    	    match(input,COMMA,FOLLOW_COMMA_in_methodArgs844); if (failed) return retval;
                    	    pushFollow(FOLLOW_argument_in_methodArgs847);
                    	    argument77=argument();
                    	    _fsp--;
                    	    if (failed) return retval;
                    	    if ( backtracking==0 ) adaptor.addChild(root_0, argument77.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop18;
                        }
                    } while (true);

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:148:52: ( COMMA )?
                    int alt19=2;
                    int LA19_0 = input.LA(1);

                    if ( (LA19_0==COMMA) ) {
                        alt19=1;
                    }
                    switch (alt19) {
                        case 1 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:148:53: COMMA
                            {
                            COMMA78=(Token)input.LT(1);
                            match(input,COMMA,FOLLOW_COMMA_in_methodArgs852); if (failed) return retval;

                            }
                            break;

                    }


                    }
                    break;

            }

            RPAREN79=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_methodArgs859); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:153:1: property : id= ID -> ^( PROPERTY_OR_FIELD[$id] ) ;
    public final property_return property() throws RecognitionException {
        property_return retval = new property_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;

        Object id_tree=null;
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:153:9: (id= ID -> ^( PROPERTY_OR_FIELD[$id] ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:153:11: id= ID
            {
            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_property872); if (failed) return retval;
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
            // 153:17: -> ^( PROPERTY_OR_FIELD[$id] )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:153:20: ^( PROPERTY_OR_FIELD[$id] )
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:160:1: reference : AT pos= LPAREN (cn= contextName COLON )? (q= qualifiedId )? RPAREN -> ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN ) ;
    public final reference_return reference() throws RecognitionException {
        reference_return retval = new reference_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token pos=null;
        Token AT80=null;
        Token COLON81=null;
        Token RPAREN82=null;
        contextName_return cn = null;

        qualifiedId_return q = null;


        Object pos_tree=null;
        Object AT80_tree=null;
        Object COLON81_tree=null;
        Object RPAREN82_tree=null;
        RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
        RewriteRuleTokenStream stream_COLON=new RewriteRuleTokenStream(adaptor,"token COLON");
        RewriteRuleTokenStream stream_LPAREN=new RewriteRuleTokenStream(adaptor,"token LPAREN");
        RewriteRuleTokenStream stream_AT=new RewriteRuleTokenStream(adaptor,"token AT");
        RewriteRuleSubtreeStream stream_contextName=new RewriteRuleSubtreeStream(adaptor,"rule contextName");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:161:2: ( AT pos= LPAREN (cn= contextName COLON )? (q= qualifiedId )? RPAREN -> ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:161:5: AT pos= LPAREN (cn= contextName COLON )? (q= qualifiedId )? RPAREN
            {
            AT80=(Token)input.LT(1);
            match(input,AT,FOLLOW_AT_in_reference894); if (failed) return retval;
            if ( backtracking==0 ) stream_AT.add(AT80);

            pos=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_reference898); if (failed) return retval;
            if ( backtracking==0 ) stream_LPAREN.add(pos);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:161:19: (cn= contextName COLON )?
            int alt21=2;
            int LA21_0 = input.LA(1);

            if ( (LA21_0==ID) ) {
                int LA21_1 = input.LA(2);

                if ( (LA21_1==COLON||LA21_1==DIV) ) {
                    alt21=1;
                }
            }
            switch (alt21) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:161:20: cn= contextName COLON
                    {
                    pushFollow(FOLLOW_contextName_in_reference903);
                    cn=contextName();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_contextName.add(cn.getTree());
                    COLON81=(Token)input.LT(1);
                    match(input,COLON,FOLLOW_COLON_in_reference905); if (failed) return retval;
                    if ( backtracking==0 ) stream_COLON.add(COLON81);


                    }
                    break;

            }

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:161:43: (q= qualifiedId )?
            int alt22=2;
            int LA22_0 = input.LA(1);

            if ( (LA22_0==ID) ) {
                alt22=1;
            }
            switch (alt22) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:161:44: q= qualifiedId
                    {
                    pushFollow(FOLLOW_qualifiedId_in_reference912);
                    q=qualifiedId();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_qualifiedId.add(q.getTree());

                    }
                    break;

            }

            RPAREN82=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_reference916); if (failed) return retval;
            if ( backtracking==0 ) stream_RPAREN.add(RPAREN82);


            // AST REWRITE
            // elements: RPAREN, cn, COLON, q
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
            // 162:4: -> ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:162:7: ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(REFERENCE, pos), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:162:25: ( $cn COLON )?
                if ( stream_cn.hasNext()||stream_COLON.hasNext() ) {
                    adaptor.addChild(root_1, stream_cn.next());
                    adaptor.addChild(root_1, stream_COLON.next());

                }
                stream_cn.reset();
                stream_COLON.reset();
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:162:38: ( $q)?
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:168:1: indexer : LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET -> ^( INDEXER $r1 ( $r2)* ) ;
    public final indexer_return indexer() throws RecognitionException {
        indexer_return retval = new indexer_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LBRACKET83=null;
        Token COMMA84=null;
        Token RBRACKET85=null;
        argument_return r1 = null;

        argument_return r2 = null;


        Object LBRACKET83_tree=null;
        Object COMMA84_tree=null;
        Object RBRACKET85_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_LBRACKET=new RewriteRuleTokenStream(adaptor,"token LBRACKET");
        RewriteRuleTokenStream stream_RBRACKET=new RewriteRuleTokenStream(adaptor,"token RBRACKET");
        RewriteRuleSubtreeStream stream_argument=new RewriteRuleSubtreeStream(adaptor,"rule argument");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:168:8: ( LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET -> ^( INDEXER $r1 ( $r2)* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:168:10: LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET
            {
            LBRACKET83=(Token)input.LT(1);
            match(input,LBRACKET,FOLLOW_LBRACKET_in_indexer951); if (failed) return retval;
            if ( backtracking==0 ) stream_LBRACKET.add(LBRACKET83);

            pushFollow(FOLLOW_argument_in_indexer955);
            r1=argument();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_argument.add(r1.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:168:31: ( COMMA r2= argument )*
            loop23:
            do {
                int alt23=2;
                int LA23_0 = input.LA(1);

                if ( (LA23_0==COMMA) ) {
                    alt23=1;
                }


                switch (alt23) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:168:32: COMMA r2= argument
            	    {
            	    COMMA84=(Token)input.LT(1);
            	    match(input,COMMA,FOLLOW_COMMA_in_indexer958); if (failed) return retval;
            	    if ( backtracking==0 ) stream_COMMA.add(COMMA84);

            	    pushFollow(FOLLOW_argument_in_indexer962);
            	    r2=argument();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) stream_argument.add(r2.getTree());

            	    }
            	    break;

            	default :
            	    break loop23;
                }
            } while (true);

            RBRACKET85=(Token)input.LT(1);
            match(input,RBRACKET,FOLLOW_RBRACKET_in_indexer966); if (failed) return retval;
            if ( backtracking==0 ) stream_RBRACKET.add(RBRACKET85);


            // AST REWRITE
            // elements: r1, r2
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
            // 168:61: -> ^( INDEXER $r1 ( $r2)* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:168:64: ^( INDEXER $r1 ( $r2)* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(INDEXER, "INDEXER"), root_1);

                adaptor.addChild(root_1, stream_r1.next());
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:168:78: ( $r2)*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:173:1: projection : PROJECT expression RCURLY ;
    public final projection_return projection() throws RecognitionException {
        projection_return retval = new projection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token PROJECT86=null;
        Token RCURLY88=null;
        expression_return expression87 = null;


        Object PROJECT86_tree=null;
        Object RCURLY88_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:173:11: ( PROJECT expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:173:13: PROJECT expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            PROJECT86=(Token)input.LT(1);
            match(input,PROJECT,FOLLOW_PROJECT_in_projection993); if (failed) return retval;
            if ( backtracking==0 ) {
            PROJECT86_tree = (Object)adaptor.create(PROJECT86);
            root_0 = (Object)adaptor.becomeRoot(PROJECT86_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_projection996);
            expression87=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression87.getTree());
            RCURLY88=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_projection998); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:175:1: selection : SELECT expression RCURLY ;
    public final selection_return selection() throws RecognitionException {
        selection_return retval = new selection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token SELECT89=null;
        Token RCURLY91=null;
        expression_return expression90 = null;


        Object SELECT89_tree=null;
        Object RCURLY91_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:175:10: ( SELECT expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:175:12: SELECT expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            SELECT89=(Token)input.LT(1);
            match(input,SELECT,FOLLOW_SELECT_in_selection1006); if (failed) return retval;
            if ( backtracking==0 ) {
            SELECT89_tree = (Object)adaptor.create(SELECT89);
            root_0 = (Object)adaptor.becomeRoot(SELECT89_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_selection1009);
            expression90=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression90.getTree());
            RCURLY91=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_selection1011); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:177:1: firstSelection : SELECT_FIRST expression RCURLY ;
    public final firstSelection_return firstSelection() throws RecognitionException {
        firstSelection_return retval = new firstSelection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token SELECT_FIRST92=null;
        Token RCURLY94=null;
        expression_return expression93 = null;


        Object SELECT_FIRST92_tree=null;
        Object RCURLY94_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:177:15: ( SELECT_FIRST expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:177:17: SELECT_FIRST expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            SELECT_FIRST92=(Token)input.LT(1);
            match(input,SELECT_FIRST,FOLLOW_SELECT_FIRST_in_firstSelection1019); if (failed) return retval;
            if ( backtracking==0 ) {
            SELECT_FIRST92_tree = (Object)adaptor.create(SELECT_FIRST92);
            root_0 = (Object)adaptor.becomeRoot(SELECT_FIRST92_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_firstSelection1022);
            expression93=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression93.getTree());
            RCURLY94=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_firstSelection1024); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:179:1: lastSelection : SELECT_LAST expression RCURLY ;
    public final lastSelection_return lastSelection() throws RecognitionException {
        lastSelection_return retval = new lastSelection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token SELECT_LAST95=null;
        Token RCURLY97=null;
        expression_return expression96 = null;


        Object SELECT_LAST95_tree=null;
        Object RCURLY97_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:179:14: ( SELECT_LAST expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:179:16: SELECT_LAST expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            SELECT_LAST95=(Token)input.LT(1);
            match(input,SELECT_LAST,FOLLOW_SELECT_LAST_in_lastSelection1032); if (failed) return retval;
            if ( backtracking==0 ) {
            SELECT_LAST95_tree = (Object)adaptor.create(SELECT_LAST95);
            root_0 = (Object)adaptor.becomeRoot(SELECT_LAST95_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_lastSelection1035);
            expression96=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression96.getTree());
            RCURLY97=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_lastSelection1037); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:182:1: type : TYPE qualifiedId RPAREN -> ^( TYPEREF qualifiedId ) ;
    public final type_return type() throws RecognitionException {
        type_return retval = new type_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token TYPE98=null;
        Token RPAREN100=null;
        qualifiedId_return qualifiedId99 = null;


        Object TYPE98_tree=null;
        Object RPAREN100_tree=null;
        RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
        RewriteRuleTokenStream stream_TYPE=new RewriteRuleTokenStream(adaptor,"token TYPE");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:182:5: ( TYPE qualifiedId RPAREN -> ^( TYPEREF qualifiedId ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:182:7: TYPE qualifiedId RPAREN
            {
            TYPE98=(Token)input.LT(1);
            match(input,TYPE,FOLLOW_TYPE_in_type1046); if (failed) return retval;
            if ( backtracking==0 ) stream_TYPE.add(TYPE98);

            pushFollow(FOLLOW_qualifiedId_in_type1048);
            qualifiedId99=qualifiedId();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_qualifiedId.add(qualifiedId99.getTree());
            RPAREN100=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_type1050); if (failed) return retval;
            if ( backtracking==0 ) stream_RPAREN.add(RPAREN100);


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
            // 182:31: -> ^( TYPEREF qualifiedId )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:182:34: ^( TYPEREF qualifiedId )
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:186:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );
    public final constructor_return constructor() throws RecognitionException {
        constructor_return retval = new constructor_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal101=null;
        qualifiedId_return qualifiedId102 = null;

        ctorArgs_return ctorArgs103 = null;

        arrayConstructor_return arrayConstructor104 = null;


        Object string_literal101_tree=null;
        RewriteRuleTokenStream stream_86=new RewriteRuleTokenStream(adaptor,"token 86");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        RewriteRuleSubtreeStream stream_ctorArgs=new RewriteRuleSubtreeStream(adaptor,"rule ctorArgs");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:187:2: ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor )
            int alt24=2;
            int LA24_0 = input.LA(1);

            if ( (LA24_0==86) ) {
                int LA24_1 = input.LA(2);

                if ( (LA24_1==ID) ) {
                    int LA24_2 = input.LA(3);

                    if ( (synpred3()) ) {
                        alt24=1;
                    }
                    else if ( (true) ) {
                        alt24=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("186:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );", 24, 2, input);

                        throw nvae;
                    }
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("186:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );", 24, 1, input);

                    throw nvae;
                }
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("186:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );", 24, 0, input);

                throw nvae;
            }
            switch (alt24) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:187:4: ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs
                    {
                    string_literal101=(Token)input.LT(1);
                    match(input,86,FOLLOW_86_in_constructor1081); if (failed) return retval;
                    if ( backtracking==0 ) stream_86.add(string_literal101);

                    pushFollow(FOLLOW_qualifiedId_in_constructor1083);
                    qualifiedId102=qualifiedId();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_qualifiedId.add(qualifiedId102.getTree());
                    pushFollow(FOLLOW_ctorArgs_in_constructor1085);
                    ctorArgs103=ctorArgs();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_ctorArgs.add(ctorArgs103.getTree());

                    // AST REWRITE
                    // elements: qualifiedId, ctorArgs
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    if ( backtracking==0 ) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 187:61: -> ^( CONSTRUCTOR qualifiedId ctorArgs )
                    {
                        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:187:64: ^( CONSTRUCTOR qualifiedId ctorArgs )
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
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:188:6: arrayConstructor
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_arrayConstructor_in_constructor1102);
                    arrayConstructor104=arrayConstructor();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, arrayConstructor104.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:191:1: arrayConstructor : 'new' qualifiedId arrayRank ( listInitializer )? -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? ) ;
    public final arrayConstructor_return arrayConstructor() throws RecognitionException {
        arrayConstructor_return retval = new arrayConstructor_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal105=null;
        qualifiedId_return qualifiedId106 = null;

        arrayRank_return arrayRank107 = null;

        listInitializer_return listInitializer108 = null;


        Object string_literal105_tree=null;
        RewriteRuleTokenStream stream_86=new RewriteRuleTokenStream(adaptor,"token 86");
        RewriteRuleSubtreeStream stream_listInitializer=new RewriteRuleSubtreeStream(adaptor,"rule listInitializer");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        RewriteRuleSubtreeStream stream_arrayRank=new RewriteRuleSubtreeStream(adaptor,"rule arrayRank");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:192:2: ( 'new' qualifiedId arrayRank ( listInitializer )? -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:192:4: 'new' qualifiedId arrayRank ( listInitializer )?
            {
            string_literal105=(Token)input.LT(1);
            match(input,86,FOLLOW_86_in_arrayConstructor1113); if (failed) return retval;
            if ( backtracking==0 ) stream_86.add(string_literal105);

            pushFollow(FOLLOW_qualifiedId_in_arrayConstructor1115);
            qualifiedId106=qualifiedId();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_qualifiedId.add(qualifiedId106.getTree());
            pushFollow(FOLLOW_arrayRank_in_arrayConstructor1117);
            arrayRank107=arrayRank();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_arrayRank.add(arrayRank107.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:192:32: ( listInitializer )?
            int alt25=2;
            int LA25_0 = input.LA(1);

            if ( (LA25_0==LCURLY) ) {
                alt25=1;
            }
            switch (alt25) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:192:33: listInitializer
                    {
                    pushFollow(FOLLOW_listInitializer_in_arrayConstructor1120);
                    listInitializer108=listInitializer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_listInitializer.add(listInitializer108.getTree());

                    }
                    break;

            }


            // AST REWRITE
            // elements: arrayRank, qualifiedId, listInitializer
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 193:4: -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:193:7: ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(CONSTRUCTOR_ARRAY, "CONSTRUCTOR_ARRAY"), root_1);

                adaptor.addChild(root_1, stream_qualifiedId.next());
                adaptor.addChild(root_1, stream_arrayRank.next());
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:193:49: ( listInitializer )?
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:196:1: arrayRank : LBRACKET ( expression ( COMMA expression )* )? RBRACKET -> ^( EXPRESSIONLIST ( expression )* ) ;
    public final arrayRank_return arrayRank() throws RecognitionException {
        arrayRank_return retval = new arrayRank_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LBRACKET109=null;
        Token COMMA111=null;
        Token RBRACKET113=null;
        expression_return expression110 = null;

        expression_return expression112 = null;


        Object LBRACKET109_tree=null;
        Object COMMA111_tree=null;
        Object RBRACKET113_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_LBRACKET=new RewriteRuleTokenStream(adaptor,"token LBRACKET");
        RewriteRuleTokenStream stream_RBRACKET=new RewriteRuleTokenStream(adaptor,"token RBRACKET");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:197:5: ( LBRACKET ( expression ( COMMA expression )* )? RBRACKET -> ^( EXPRESSIONLIST ( expression )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:197:7: LBRACKET ( expression ( COMMA expression )* )? RBRACKET
            {
            LBRACKET109=(Token)input.LT(1);
            match(input,LBRACKET,FOLLOW_LBRACKET_in_arrayRank1155); if (failed) return retval;
            if ( backtracking==0 ) stream_LBRACKET.add(LBRACKET109);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:197:16: ( expression ( COMMA expression )* )?
            int alt27=2;
            int LA27_0 = input.LA(1);

            if ( (LA27_0==INTEGER_LITERAL||LA27_0==LPAREN||(LA27_0>=PLUS && LA27_0<=MINUS)||LA27_0==BANG||(LA27_0>=POUND && LA27_0<=ID)||(LA27_0>=AT && LA27_0<=LBRACKET)||LA27_0==PROJECT||(LA27_0>=SELECT && LA27_0<=FALSE)||LA27_0==86) ) {
                alt27=1;
            }
            switch (alt27) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:197:17: expression ( COMMA expression )*
                    {
                    pushFollow(FOLLOW_expression_in_arrayRank1158);
                    expression110=expression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_expression.add(expression110.getTree());
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:197:28: ( COMMA expression )*
                    loop26:
                    do {
                        int alt26=2;
                        int LA26_0 = input.LA(1);

                        if ( (LA26_0==COMMA) ) {
                            alt26=1;
                        }


                        switch (alt26) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:197:29: COMMA expression
                    	    {
                    	    COMMA111=(Token)input.LT(1);
                    	    match(input,COMMA,FOLLOW_COMMA_in_arrayRank1161); if (failed) return retval;
                    	    if ( backtracking==0 ) stream_COMMA.add(COMMA111);

                    	    pushFollow(FOLLOW_expression_in_arrayRank1163);
                    	    expression112=expression();
                    	    _fsp--;
                    	    if (failed) return retval;
                    	    if ( backtracking==0 ) stream_expression.add(expression112.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop26;
                        }
                    } while (true);


                    }
                    break;

            }

            RBRACKET113=(Token)input.LT(1);
            match(input,RBRACKET,FOLLOW_RBRACKET_in_arrayRank1169); if (failed) return retval;
            if ( backtracking==0 ) stream_RBRACKET.add(RBRACKET113);


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
            // 197:59: -> ^( EXPRESSIONLIST ( expression )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:197:62: ^( EXPRESSIONLIST ( expression )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(EXPRESSIONLIST, "EXPRESSIONLIST"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:197:79: ( expression )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:199:1: listInitializer : LCURLY expression ( COMMA expression )* RCURLY -> ^( LIST_INITIALIZER ( expression )* ) ;
    public final listInitializer_return listInitializer() throws RecognitionException {
        listInitializer_return retval = new listInitializer_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LCURLY114=null;
        Token COMMA116=null;
        Token RCURLY118=null;
        expression_return expression115 = null;

        expression_return expression117 = null;


        Object LCURLY114_tree=null;
        Object COMMA116_tree=null;
        Object RCURLY118_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_RCURLY=new RewriteRuleTokenStream(adaptor,"token RCURLY");
        RewriteRuleTokenStream stream_LCURLY=new RewriteRuleTokenStream(adaptor,"token LCURLY");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:5: ( LCURLY expression ( COMMA expression )* RCURLY -> ^( LIST_INITIALIZER ( expression )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:7: LCURLY expression ( COMMA expression )* RCURLY
            {
            LCURLY114=(Token)input.LT(1);
            match(input,LCURLY,FOLLOW_LCURLY_in_listInitializer1194); if (failed) return retval;
            if ( backtracking==0 ) stream_LCURLY.add(LCURLY114);

            pushFollow(FOLLOW_expression_in_listInitializer1196);
            expression115=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression115.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:25: ( COMMA expression )*
            loop28:
            do {
                int alt28=2;
                int LA28_0 = input.LA(1);

                if ( (LA28_0==COMMA) ) {
                    alt28=1;
                }


                switch (alt28) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:26: COMMA expression
            	    {
            	    COMMA116=(Token)input.LT(1);
            	    match(input,COMMA,FOLLOW_COMMA_in_listInitializer1199); if (failed) return retval;
            	    if ( backtracking==0 ) stream_COMMA.add(COMMA116);

            	    pushFollow(FOLLOW_expression_in_listInitializer1201);
            	    expression117=expression();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) stream_expression.add(expression117.getTree());

            	    }
            	    break;

            	default :
            	    break loop28;
                }
            } while (true);

            RCURLY118=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_listInitializer1205); if (failed) return retval;
            if ( backtracking==0 ) stream_RCURLY.add(RCURLY118);


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
            // 200:52: -> ^( LIST_INITIALIZER ( expression )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:55: ^( LIST_INITIALIZER ( expression )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(LIST_INITIALIZER, "LIST_INITIALIZER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:200:74: ( expression )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:205:1: mapInitializer : POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY -> ^( MAP_INITIALIZER ( mapEntry )* ) ;
    public final mapInitializer_return mapInitializer() throws RecognitionException {
        mapInitializer_return retval = new mapInitializer_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token POUND119=null;
        Token LCURLY120=null;
        Token COMMA122=null;
        Token RCURLY124=null;
        mapEntry_return mapEntry121 = null;

        mapEntry_return mapEntry123 = null;


        Object POUND119_tree=null;
        Object LCURLY120_tree=null;
        Object COMMA122_tree=null;
        Object RCURLY124_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_RCURLY=new RewriteRuleTokenStream(adaptor,"token RCURLY");
        RewriteRuleTokenStream stream_LCURLY=new RewriteRuleTokenStream(adaptor,"token LCURLY");
        RewriteRuleTokenStream stream_POUND=new RewriteRuleTokenStream(adaptor,"token POUND");
        RewriteRuleSubtreeStream stream_mapEntry=new RewriteRuleSubtreeStream(adaptor,"rule mapEntry");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:206:5: ( POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY -> ^( MAP_INITIALIZER ( mapEntry )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:206:7: POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY
            {
            POUND119=(Token)input.LT(1);
            match(input,POUND,FOLLOW_POUND_in_mapInitializer1233); if (failed) return retval;
            if ( backtracking==0 ) stream_POUND.add(POUND119);

            LCURLY120=(Token)input.LT(1);
            match(input,LCURLY,FOLLOW_LCURLY_in_mapInitializer1235); if (failed) return retval;
            if ( backtracking==0 ) stream_LCURLY.add(LCURLY120);

            pushFollow(FOLLOW_mapEntry_in_mapInitializer1237);
            mapEntry121=mapEntry();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_mapEntry.add(mapEntry121.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:206:29: ( COMMA mapEntry )*
            loop29:
            do {
                int alt29=2;
                int LA29_0 = input.LA(1);

                if ( (LA29_0==COMMA) ) {
                    alt29=1;
                }


                switch (alt29) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:206:30: COMMA mapEntry
            	    {
            	    COMMA122=(Token)input.LT(1);
            	    match(input,COMMA,FOLLOW_COMMA_in_mapInitializer1240); if (failed) return retval;
            	    if ( backtracking==0 ) stream_COMMA.add(COMMA122);

            	    pushFollow(FOLLOW_mapEntry_in_mapInitializer1242);
            	    mapEntry123=mapEntry();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) stream_mapEntry.add(mapEntry123.getTree());

            	    }
            	    break;

            	default :
            	    break loop29;
                }
            } while (true);

            RCURLY124=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_mapInitializer1246); if (failed) return retval;
            if ( backtracking==0 ) stream_RCURLY.add(RCURLY124);


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
            // 206:54: -> ^( MAP_INITIALIZER ( mapEntry )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:206:57: ^( MAP_INITIALIZER ( mapEntry )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(MAP_INITIALIZER, "MAP_INITIALIZER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:206:75: ( mapEntry )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:208:1: mapEntry : expression COLON expression -> ^( MAP_ENTRY ( expression )* ) ;
    public final mapEntry_return mapEntry() throws RecognitionException {
        mapEntry_return retval = new mapEntry_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token COLON126=null;
        expression_return expression125 = null;

        expression_return expression127 = null;


        Object COLON126_tree=null;
        RewriteRuleTokenStream stream_COLON=new RewriteRuleTokenStream(adaptor,"token COLON");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:209:5: ( expression COLON expression -> ^( MAP_ENTRY ( expression )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:209:7: expression COLON expression
            {
            pushFollow(FOLLOW_expression_in_mapEntry1267);
            expression125=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression125.getTree());
            COLON126=(Token)input.LT(1);
            match(input,COLON,FOLLOW_COLON_in_mapEntry1269); if (failed) return retval;
            if ( backtracking==0 ) stream_COLON.add(COLON126);

            pushFollow(FOLLOW_expression_in_mapEntry1271);
            expression127=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression127.getTree());

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
            // 209:35: -> ^( MAP_ENTRY ( expression )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:209:38: ^( MAP_ENTRY ( expression )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(MAP_ENTRY, "MAP_ENTRY"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:209:50: ( expression )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:211:1: ctorArgs : LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN ;
    public final ctorArgs_return ctorArgs() throws RecognitionException {
        ctorArgs_return retval = new ctorArgs_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LPAREN128=null;
        Token COMMA130=null;
        Token RPAREN132=null;
        namedArgument_return namedArgument129 = null;

        namedArgument_return namedArgument131 = null;


        Object LPAREN128_tree=null;
        Object COMMA130_tree=null;
        Object RPAREN132_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:212:2: ( LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:212:4: LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN
            {
            root_0 = (Object)adaptor.nil();

            LPAREN128=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_ctorArgs1289); if (failed) return retval;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:212:12: ( namedArgument ( COMMA namedArgument )* )?
            int alt31=2;
            int LA31_0 = input.LA(1);

            if ( (LA31_0==INTEGER_LITERAL||LA31_0==LPAREN||(LA31_0>=PLUS && LA31_0<=MINUS)||LA31_0==BANG||(LA31_0>=POUND && LA31_0<=ID)||(LA31_0>=AT && LA31_0<=LBRACKET)||LA31_0==PROJECT||(LA31_0>=SELECT && LA31_0<=FALSE)||LA31_0==86) ) {
                alt31=1;
            }
            switch (alt31) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:212:13: namedArgument ( COMMA namedArgument )*
                    {
                    pushFollow(FOLLOW_namedArgument_in_ctorArgs1293);
                    namedArgument129=namedArgument();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, namedArgument129.getTree());
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:212:27: ( COMMA namedArgument )*
                    loop30:
                    do {
                        int alt30=2;
                        int LA30_0 = input.LA(1);

                        if ( (LA30_0==COMMA) ) {
                            alt30=1;
                        }


                        switch (alt30) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:212:28: COMMA namedArgument
                    	    {
                    	    COMMA130=(Token)input.LT(1);
                    	    match(input,COMMA,FOLLOW_COMMA_in_ctorArgs1296); if (failed) return retval;
                    	    pushFollow(FOLLOW_namedArgument_in_ctorArgs1299);
                    	    namedArgument131=namedArgument();
                    	    _fsp--;
                    	    if (failed) return retval;
                    	    if ( backtracking==0 ) adaptor.addChild(root_0, namedArgument131.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop30;
                        }
                    } while (true);


                    }
                    break;

            }

            RPAREN132=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_ctorArgs1305); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:214:1: argument : expression ;
    public final argument_return argument() throws RecognitionException {
        argument_return retval = new argument_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        expression_return expression133 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:214:10: ( expression )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:214:12: expression
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_expression_in_argument1314);
            expression133=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression133.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:216:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );
    public final namedArgument_return namedArgument() throws RecognitionException {
        namedArgument_return retval = new namedArgument_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token ASSIGN134=null;
        expression_return expression135 = null;

        argument_return argument136 = null;


        Object id_tree=null;
        Object ASSIGN134_tree=null;
        RewriteRuleTokenStream stream_ASSIGN=new RewriteRuleTokenStream(adaptor,"token ASSIGN");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:217:5: ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument )
            int alt32=2;
            int LA32_0 = input.LA(1);

            if ( (LA32_0==ID) ) {
                int LA32_1 = input.LA(2);

                if ( (LA32_1==ASSIGN) ) {
                    int LA32_23 = input.LA(3);

                    if ( (synpred4()) ) {
                        alt32=1;
                    }
                    else if ( (true) ) {
                        alt32=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("216:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );", 32, 23, input);

                        throw nvae;
                    }
                }
                else if ( ((LA32_1>=DEFAULT && LA32_1<=QMARK)||(LA32_1>=LPAREN && LA32_1<=POWER)||LA32_1==DOT||LA32_1==COMMA||LA32_1==LBRACKET||(LA32_1>=EQUAL && LA32_1<=MATCHES)) ) {
                    alt32=2;
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("216:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );", 32, 1, input);

                    throw nvae;
                }
            }
            else if ( (LA32_0==INTEGER_LITERAL||LA32_0==LPAREN||(LA32_0>=PLUS && LA32_0<=MINUS)||LA32_0==BANG||LA32_0==POUND||(LA32_0>=AT && LA32_0<=LBRACKET)||LA32_0==PROJECT||(LA32_0>=SELECT && LA32_0<=FALSE)||LA32_0==86) ) {
                alt32=2;
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("216:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );", 32, 0, input);

                throw nvae;
            }
            switch (alt32) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:217:7: ( ID ASSIGN )=>id= ID ASSIGN expression
                    {
                    id=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_namedArgument1337); if (failed) return retval;
                    if ( backtracking==0 ) stream_ID.add(id);

                    ASSIGN134=(Token)input.LT(1);
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_namedArgument1339); if (failed) return retval;
                    if ( backtracking==0 ) stream_ASSIGN.add(ASSIGN134);

                    pushFollow(FOLLOW_expression_in_namedArgument1341);
                    expression135=expression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_expression.add(expression135.getTree());

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
                    // 218:19: -> ^( NAMED_ARGUMENT[$id] expression )
                    {
                        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:22: ^( NAMED_ARGUMENT[$id] expression )
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
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:219:7: argument
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_argument_in_namedArgument1377);
                    argument136=argument();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, argument136.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:221:1: qualifiedId : ID ( DOT ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) ;
    public final qualifiedId_return qualifiedId() throws RecognitionException {
        qualifiedId_return retval = new qualifiedId_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID137=null;
        Token DOT138=null;
        Token ID139=null;

        Object ID137_tree=null;
        Object DOT138_tree=null;
        Object ID139_tree=null;
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleTokenStream stream_DOT=new RewriteRuleTokenStream(adaptor,"token DOT");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:221:13: ( ID ( DOT ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:221:15: ID ( DOT ID )*
            {
            ID137=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_qualifiedId1389); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(ID137);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:221:18: ( DOT ID )*
            loop33:
            do {
                int alt33=2;
                int LA33_0 = input.LA(1);

                if ( (LA33_0==DOT) ) {
                    alt33=1;
                }


                switch (alt33) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:221:19: DOT ID
            	    {
            	    DOT138=(Token)input.LT(1);
            	    match(input,DOT,FOLLOW_DOT_in_qualifiedId1392); if (failed) return retval;
            	    if ( backtracking==0 ) stream_DOT.add(DOT138);

            	    ID139=(Token)input.LT(1);
            	    match(input,ID,FOLLOW_ID_in_qualifiedId1394); if (failed) return retval;
            	    if ( backtracking==0 ) stream_ID.add(ID139);


            	    }
            	    break;

            	default :
            	    break loop33;
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
            // 221:28: -> ^( QUALIFIED_IDENTIFIER ( ID )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:221:31: ^( QUALIFIED_IDENTIFIER ( ID )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(QUALIFIED_IDENTIFIER, "QUALIFIED_IDENTIFIER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:221:54: ( ID )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:223:1: contextName : ID ( DIV ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) ;
    public final contextName_return contextName() throws RecognitionException {
        contextName_return retval = new contextName_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID140=null;
        Token DIV141=null;
        Token ID142=null;

        Object ID140_tree=null;
        Object DIV141_tree=null;
        Object ID142_tree=null;
        RewriteRuleTokenStream stream_DIV=new RewriteRuleTokenStream(adaptor,"token DIV");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:223:13: ( ID ( DIV ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:223:15: ID ( DIV ID )*
            {
            ID140=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_contextName1413); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(ID140);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:223:18: ( DIV ID )*
            loop34:
            do {
                int alt34=2;
                int LA34_0 = input.LA(1);

                if ( (LA34_0==DIV) ) {
                    alt34=1;
                }


                switch (alt34) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:223:19: DIV ID
            	    {
            	    DIV141=(Token)input.LT(1);
            	    match(input,DIV,FOLLOW_DIV_in_contextName1416); if (failed) return retval;
            	    if ( backtracking==0 ) stream_DIV.add(DIV141);

            	    ID142=(Token)input.LT(1);
            	    match(input,ID,FOLLOW_ID_in_contextName1418); if (failed) return retval;
            	    if ( backtracking==0 ) stream_ID.add(ID142);


            	    }
            	    break;

            	default :
            	    break loop34;
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
            // 223:28: -> ^( QUALIFIED_IDENTIFIER ( ID )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:223:31: ^( QUALIFIED_IDENTIFIER ( ID )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(QUALIFIED_IDENTIFIER, "QUALIFIED_IDENTIFIER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:223:54: ( ID )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:225:1: literal : ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL );
    public final literal_return literal() throws RecognitionException {
        literal_return retval = new literal_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token INTEGER_LITERAL143=null;
        Token STRING_LITERAL144=null;
        Token DQ_STRING_LITERAL145=null;
        Token NULL_LITERAL147=null;
        Token HEXADECIMAL_INTEGER_LITERAL148=null;
        Token REAL_LITERAL149=null;
        boolLiteral_return boolLiteral146 = null;


        Object INTEGER_LITERAL143_tree=null;
        Object STRING_LITERAL144_tree=null;
        Object DQ_STRING_LITERAL145_tree=null;
        Object NULL_LITERAL147_tree=null;
        Object HEXADECIMAL_INTEGER_LITERAL148_tree=null;
        Object REAL_LITERAL149_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:226:2: ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL )
            int alt35=7;
            switch ( input.LA(1) ) {
            case INTEGER_LITERAL:
                {
                alt35=1;
                }
                break;
            case STRING_LITERAL:
                {
                alt35=2;
                }
                break;
            case DQ_STRING_LITERAL:
                {
                alt35=3;
                }
                break;
            case TRUE:
            case FALSE:
                {
                alt35=4;
                }
                break;
            case NULL_LITERAL:
                {
                alt35=5;
                }
                break;
            case HEXADECIMAL_INTEGER_LITERAL:
                {
                alt35=6;
                }
                break;
            case REAL_LITERAL:
                {
                alt35=7;
                }
                break;
            default:
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("225:1: literal : ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL );", 35, 0, input);

                throw nvae;
            }

            switch (alt35) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:226:4: INTEGER_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    INTEGER_LITERAL143=(Token)input.LT(1);
                    match(input,INTEGER_LITERAL,FOLLOW_INTEGER_LITERAL_in_literal1439); if (failed) return retval;
                    if ( backtracking==0 ) {
                    INTEGER_LITERAL143_tree = (Object)adaptor.create(INTEGER_LITERAL143);
                    adaptor.addChild(root_0, INTEGER_LITERAL143_tree);
                    }

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:227:4: STRING_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    STRING_LITERAL144=(Token)input.LT(1);
                    match(input,STRING_LITERAL,FOLLOW_STRING_LITERAL_in_literal1445); if (failed) return retval;
                    if ( backtracking==0 ) {
                    STRING_LITERAL144_tree = (Object)adaptor.create(STRING_LITERAL144);
                    adaptor.addChild(root_0, STRING_LITERAL144_tree);
                    }

                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:228:4: DQ_STRING_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    DQ_STRING_LITERAL145=(Token)input.LT(1);
                    match(input,DQ_STRING_LITERAL,FOLLOW_DQ_STRING_LITERAL_in_literal1450); if (failed) return retval;
                    if ( backtracking==0 ) {
                    DQ_STRING_LITERAL145_tree = (Object)adaptor.create(DQ_STRING_LITERAL145);
                    adaptor.addChild(root_0, DQ_STRING_LITERAL145_tree);
                    }

                    }
                    break;
                case 4 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:229:4: boolLiteral
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_boolLiteral_in_literal1455);
                    boolLiteral146=boolLiteral();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, boolLiteral146.getTree());

                    }
                    break;
                case 5 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:230:4: NULL_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    NULL_LITERAL147=(Token)input.LT(1);
                    match(input,NULL_LITERAL,FOLLOW_NULL_LITERAL_in_literal1460); if (failed) return retval;
                    if ( backtracking==0 ) {
                    NULL_LITERAL147_tree = (Object)adaptor.create(NULL_LITERAL147);
                    adaptor.addChild(root_0, NULL_LITERAL147_tree);
                    }

                    }
                    break;
                case 6 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:231:4: HEXADECIMAL_INTEGER_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    HEXADECIMAL_INTEGER_LITERAL148=(Token)input.LT(1);
                    match(input,HEXADECIMAL_INTEGER_LITERAL,FOLLOW_HEXADECIMAL_INTEGER_LITERAL_in_literal1465); if (failed) return retval;
                    if ( backtracking==0 ) {
                    HEXADECIMAL_INTEGER_LITERAL148_tree = (Object)adaptor.create(HEXADECIMAL_INTEGER_LITERAL148);
                    adaptor.addChild(root_0, HEXADECIMAL_INTEGER_LITERAL148_tree);
                    }

                    }
                    break;
                case 7 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:232:4: REAL_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    REAL_LITERAL149=(Token)input.LT(1);
                    match(input,REAL_LITERAL,FOLLOW_REAL_LITERAL_in_literal1471); if (failed) return retval;
                    if ( backtracking==0 ) {
                    REAL_LITERAL149_tree = (Object)adaptor.create(REAL_LITERAL149);
                    adaptor.addChild(root_0, REAL_LITERAL149_tree);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:235:1: boolLiteral : ( TRUE | FALSE );
    public final boolLiteral_return boolLiteral() throws RecognitionException {
        boolLiteral_return retval = new boolLiteral_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set150=null;

        Object set150_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:235:12: ( TRUE | FALSE )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:
            {
            root_0 = (Object)adaptor.nil();

            set150=(Token)input.LT(1);
            if ( (input.LA(1)>=TRUE && input.LA(1)<=FALSE) ) {
                input.consume();
                if ( backtracking==0 ) adaptor.addChild(root_0, adaptor.create(set150));
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:242:1: relationalOperator : ( EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL | IN | IS | BETWEEN | MATCHES );
    public final relationalOperator_return relationalOperator() throws RecognitionException {
        relationalOperator_return retval = new relationalOperator_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set151=null;

        Object set151_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:243:5: ( EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL | IN | IS | BETWEEN | MATCHES )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:
            {
            root_0 = (Object)adaptor.nil();

            set151=(Token)input.LT(1);
            if ( (input.LA(1)>=EQUAL && input.LA(1)<=MATCHES) ) {
                input.consume();
                if ( backtracking==0 ) adaptor.addChild(root_0, adaptor.create(set151));
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
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:131:7: ( POUND ID LPAREN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:131:8: POUND ID LPAREN
        {
        match(input,POUND,FOLLOW_POUND_in_synpred1713); if (failed) return ;
        match(input,ID,FOLLOW_ID_in_synpred1715); if (failed) return ;
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred1717); if (failed) return ;

        }
    }
    // $ANTLR end synpred1

    // $ANTLR start synpred2
    public final void synpred2_fragment() throws RecognitionException {   
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:141:4: ( ID LPAREN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:141:5: ID LPAREN
        {
        match(input,ID,FOLLOW_ID_in_synpred2797); if (failed) return ;
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred2799); if (failed) return ;

        }
    }
    // $ANTLR end synpred2

    // $ANTLR start synpred3
    public final void synpred3_fragment() throws RecognitionException {   
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:187:4: ( 'new' qualifiedId LPAREN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:187:5: 'new' qualifiedId LPAREN
        {
        match(input,86,FOLLOW_86_in_synpred31072); if (failed) return ;
        pushFollow(FOLLOW_qualifiedId_in_synpred31074);
        qualifiedId();
        _fsp--;
        if (failed) return ;
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred31076); if (failed) return ;

        }
    }
    // $ANTLR end synpred3

    // $ANTLR start synpred4
    public final void synpred4_fragment() throws RecognitionException {   
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:217:7: ( ID ASSIGN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:217:8: ID ASSIGN
        {
        match(input,ID,FOLLOW_ID_in_synpred41328); if (failed) return ;
        match(input,ASSIGN,FOLLOW_ASSIGN_in_synpred41330); if (failed) return ;

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


 

    public static final BitSet FOLLOW_expression_in_expr165 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_expr167 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_logicalOrExpression_in_expression196 = new BitSet(new long[]{0x0000000038000002L});
    public static final BitSet FOLLOW_ASSIGN_in_expression205 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_logicalOrExpression_in_expression208 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DEFAULT_in_expression218 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_logicalOrExpression_in_expression221 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_QMARK_in_expression231 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_expression_in_expression234 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_COLON_in_expression236 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_expression_in_expression239 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_parenExpr250 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_expression_in_parenExpr253 = new BitSet(new long[]{0x0000000100000000L});
    public static final BitSet FOLLOW_RPAREN_in_parenExpr255 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_logicalAndExpression_in_logicalOrExpression268 = new BitSet(new long[]{0x0000000200000002L});
    public static final BitSet FOLLOW_OR_in_logicalOrExpression271 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_logicalAndExpression_in_logicalOrExpression274 = new BitSet(new long[]{0x0000000200000002L});
    public static final BitSet FOLLOW_relationalExpression_in_logicalAndExpression309 = new BitSet(new long[]{0x0000000400000002L});
    public static final BitSet FOLLOW_AND_in_logicalAndExpression312 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_relationalExpression_in_logicalAndExpression315 = new BitSet(new long[]{0x0000000400000002L});
    public static final BitSet FOLLOW_sumExpression_in_relationalExpression326 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000FFCL});
    public static final BitSet FOLLOW_relationalOperator_in_relationalExpression329 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_sumExpression_in_relationalExpression332 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_productExpression_in_sumExpression343 = new BitSet(new long[]{0x0000001800000002L});
    public static final BitSet FOLLOW_PLUS_in_sumExpression348 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_MINUS_in_sumExpression353 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_productExpression_in_sumExpression357 = new BitSet(new long[]{0x0000001800000002L});
    public static final BitSet FOLLOW_powerExpr_in_productExpression369 = new BitSet(new long[]{0x000000E000000002L});
    public static final BitSet FOLLOW_STAR_in_productExpression373 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_DIV_in_productExpression378 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_MOD_in_productExpression382 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_powerExpr_in_productExpression386 = new BitSet(new long[]{0x000000E000000002L});
    public static final BitSet FOLLOW_unaryExpression_in_powerExpr398 = new BitSet(new long[]{0x0000010000000002L});
    public static final BitSet FOLLOW_POWER_in_powerExpr401 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_unaryExpression_in_powerExpr404 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PLUS_in_unaryExpression418 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_MINUS_in_unaryExpression423 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_BANG_in_unaryExpression428 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_unaryExpression_in_unaryExpression432 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_primaryExpression_in_unaryExpression438 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_startNode_in_primaryExpression452 = new BitSet(new long[]{0x0000840000000002L});
    public static final BitSet FOLLOW_node_in_primaryExpression455 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_parenExpr_in_startNode488 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_methodOrProperty_in_startNode496 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_functionOrVar_in_startNode505 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_reference_in_startNode513 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_indexer_in_startNode521 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_literal_in_startNode529 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_type_in_startNode537 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_constructor_in_startNode545 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_projection_in_startNode553 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selection_in_startNode562 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_firstSelection_in_startNode571 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lastSelection_in_startNode579 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_listInitializer_in_startNode587 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_mapInitializer_in_startNode595 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_node615 = new BitSet(new long[]{0x003A180000000000L});
    public static final BitSet FOLLOW_dottedNode_in_node617 = new BitSet(new long[]{0x0000840000000002L});
    public static final BitSet FOLLOW_nonDottedNode_in_node622 = new BitSet(new long[]{0x0000840000000002L});
    public static final BitSet FOLLOW_indexer_in_nonDottedNode634 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_methodOrProperty_in_dottedNode647 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_functionOrVar_in_dottedNode653 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_projection_in_dottedNode661 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selection_in_dottedNode670 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_firstSelection_in_dottedNode679 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lastSelection_in_dottedNode688 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_function_in_functionOrVar722 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_var_in_functionOrVar730 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_function747 = new BitSet(new long[]{0x0000100000000000L});
    public static final BitSet FOLLOW_ID_in_function751 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_methodArgs_in_function753 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_var774 = new BitSet(new long[]{0x0000100000000000L});
    public static final BitSet FOLLOW_ID_in_var778 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_methodOrProperty806 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_methodArgs_in_methodOrProperty808 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_methodOrProperty822 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_methodArgs837 = new BitSet(new long[]{0x7FFADA1980000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_argument_in_methodArgs841 = new BitSet(new long[]{0x0000200100000000L});
    public static final BitSet FOLLOW_COMMA_in_methodArgs844 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_argument_in_methodArgs847 = new BitSet(new long[]{0x0000200100000000L});
    public static final BitSet FOLLOW_COMMA_in_methodArgs852 = new BitSet(new long[]{0x0000000100000000L});
    public static final BitSet FOLLOW_RPAREN_in_methodArgs859 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_property872 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_AT_in_reference894 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_LPAREN_in_reference898 = new BitSet(new long[]{0x0000100100000000L});
    public static final BitSet FOLLOW_contextName_in_reference903 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_COLON_in_reference905 = new BitSet(new long[]{0x0000100100000000L});
    public static final BitSet FOLLOW_qualifiedId_in_reference912 = new BitSet(new long[]{0x0000000100000000L});
    public static final BitSet FOLLOW_RPAREN_in_reference916 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACKET_in_indexer951 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_argument_in_indexer955 = new BitSet(new long[]{0x0001200000000000L});
    public static final BitSet FOLLOW_COMMA_in_indexer958 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_argument_in_indexer962 = new BitSet(new long[]{0x0001200000000000L});
    public static final BitSet FOLLOW_RBRACKET_in_indexer966 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PROJECT_in_projection993 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_expression_in_projection996 = new BitSet(new long[]{0x0004000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_projection998 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELECT_in_selection1006 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_expression_in_selection1009 = new BitSet(new long[]{0x0004000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_selection1011 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELECT_FIRST_in_firstSelection1019 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_expression_in_firstSelection1022 = new BitSet(new long[]{0x0004000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_firstSelection1024 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELECT_LAST_in_lastSelection1032 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_expression_in_lastSelection1035 = new BitSet(new long[]{0x0004000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_lastSelection1037 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TYPE_in_type1046 = new BitSet(new long[]{0x0000100000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_type1048 = new BitSet(new long[]{0x0000000100000000L});
    public static final BitSet FOLLOW_RPAREN_in_type1050 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_86_in_constructor1081 = new BitSet(new long[]{0x0000100000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_constructor1083 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_ctorArgs_in_constructor1085 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_arrayConstructor_in_constructor1102 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_86_in_arrayConstructor1113 = new BitSet(new long[]{0x0000100000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_arrayConstructor1115 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_arrayRank_in_arrayConstructor1117 = new BitSet(new long[]{0x0080000000000002L});
    public static final BitSet FOLLOW_listInitializer_in_arrayConstructor1120 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACKET_in_arrayRank1155 = new BitSet(new long[]{0x7FFBDA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_expression_in_arrayRank1158 = new BitSet(new long[]{0x0001200000000000L});
    public static final BitSet FOLLOW_COMMA_in_arrayRank1161 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_expression_in_arrayRank1163 = new BitSet(new long[]{0x0001200000000000L});
    public static final BitSet FOLLOW_RBRACKET_in_arrayRank1169 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LCURLY_in_listInitializer1194 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_expression_in_listInitializer1196 = new BitSet(new long[]{0x0004200000000000L});
    public static final BitSet FOLLOW_COMMA_in_listInitializer1199 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_expression_in_listInitializer1201 = new BitSet(new long[]{0x0004200000000000L});
    public static final BitSet FOLLOW_RCURLY_in_listInitializer1205 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_mapInitializer1233 = new BitSet(new long[]{0x0080000000000000L});
    public static final BitSet FOLLOW_LCURLY_in_mapInitializer1235 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_mapEntry_in_mapInitializer1237 = new BitSet(new long[]{0x0004200000000000L});
    public static final BitSet FOLLOW_COMMA_in_mapInitializer1240 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_mapEntry_in_mapInitializer1242 = new BitSet(new long[]{0x0004200000000000L});
    public static final BitSet FOLLOW_RCURLY_in_mapInitializer1246 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expression_in_mapEntry1267 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_COLON_in_mapEntry1269 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_expression_in_mapEntry1271 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_ctorArgs1289 = new BitSet(new long[]{0x7FFADA1980000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_namedArgument_in_ctorArgs1293 = new BitSet(new long[]{0x0000200100000000L});
    public static final BitSet FOLLOW_COMMA_in_ctorArgs1296 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_namedArgument_in_ctorArgs1299 = new BitSet(new long[]{0x0000200100000000L});
    public static final BitSet FOLLOW_RPAREN_in_ctorArgs1305 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expression_in_argument1314 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_namedArgument1337 = new BitSet(new long[]{0x0000000008000000L});
    public static final BitSet FOLLOW_ASSIGN_in_namedArgument1339 = new BitSet(new long[]{0x7FFADA1880000020L,0x0000000000400000L});
    public static final BitSet FOLLOW_expression_in_namedArgument1341 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_argument_in_namedArgument1377 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_qualifiedId1389 = new BitSet(new long[]{0x0000040000000002L});
    public static final BitSet FOLLOW_DOT_in_qualifiedId1392 = new BitSet(new long[]{0x0000100000000000L});
    public static final BitSet FOLLOW_ID_in_qualifiedId1394 = new BitSet(new long[]{0x0000040000000002L});
    public static final BitSet FOLLOW_ID_in_contextName1413 = new BitSet(new long[]{0x0000004000000002L});
    public static final BitSet FOLLOW_DIV_in_contextName1416 = new BitSet(new long[]{0x0000100000000000L});
    public static final BitSet FOLLOW_ID_in_contextName1418 = new BitSet(new long[]{0x0000004000000002L});
    public static final BitSet FOLLOW_INTEGER_LITERAL_in_literal1439 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_LITERAL_in_literal1445 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DQ_STRING_LITERAL_in_literal1450 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_boolLiteral_in_literal1455 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NULL_LITERAL_in_literal1460 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_HEXADECIMAL_INTEGER_LITERAL_in_literal1465 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_REAL_LITERAL_in_literal1471 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_boolLiteral0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_relationalOperator0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_synpred1713 = new BitSet(new long[]{0x0000100000000000L});
    public static final BitSet FOLLOW_ID_in_synpred1715 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred1717 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_synpred2797 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred2799 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_86_in_synpred31072 = new BitSet(new long[]{0x0000100000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_synpred31074 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred31076 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_synpred41328 = new BitSet(new long[]{0x0000000008000000L});
    public static final BitSet FOLLOW_ASSIGN_in_synpred41330 = new BitSet(new long[]{0x0000000000000002L});

}