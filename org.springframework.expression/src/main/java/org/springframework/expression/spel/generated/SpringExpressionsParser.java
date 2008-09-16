// $ANTLR 3.0.1 /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g 2008-09-16 09:47:51
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
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "EXPRESSIONLIST", "INTEGER_LITERAL", "EXPRESSION", "QUALIFIED_IDENTIFIER", "REFERENCE", "PROPERTY_OR_FIELD", "INDEXER", "ARGLIST", "CONSTRUCTOR", "HOLDER", "CONSTRUCTOR_ARRAY", "NAMED_ARGUMENT", "FUNCTIONREF", "TYPEREF", "RANGE", "VARIABLEREF", "LIST_INITIALIZER", "MAP_INITIALIZER", "LOCALVAR", "LOCALFUNC", "MAP_ENTRY", "METHOD", "ADD", "SUBTRACT", "NUMBER", "SEMIRPAREN", "ASSIGN", "DEFAULT", "QMARK", "COLON", "LPAREN", "RPAREN", "OR", "AND", "PLUS", "MINUS", "STAR", "DIV", "MOD", "POWER", "BANG", "DOT", "POUND", "ID", "DOLLAR", "COMMA", "AT", "LBRACKET", "RBRACKET", "PROJECT", "RCURLY", "SELECT", "SELECT_FIRST", "SELECT_LAST", "TYPE", "LAMBDA", "PIPE", "LCURLY", "STRING_LITERAL", "DQ_STRING_LITERAL", "NULL_LITERAL", "HEXADECIMAL_INTEGER_LITERAL", "REAL_LITERAL", "TRUE", "FALSE", "DECIMAL_DIGIT", "INTEGER_TYPE_SUFFIX", "HEX_DIGIT", "EQUAL", "NOT_EQUAL", "LESS_THAN", "LESS_THAN_OR_EQUAL", "GREATER_THAN", "GREATER_THAN_OR_EQUAL", "IN", "IS", "BETWEEN", "MATCHES", "SEMI", "APOS", "DOT_ESCAPED", "WS", "UPTO", "EXPONENT_PART", "REAL_TYPE_SUFFIX", "SIGN", "'new'"
    };
    public static final int GREATER_THAN_OR_EQUAL=77;
    public static final int SELECT_FIRST=56;
    public static final int COMMA=49;
    public static final int HOLDER=13;
    public static final int GREATER_THAN=76;
    public static final int TYPE=58;
    public static final int EXPRESSIONLIST=4;
    public static final int MINUS=39;
    public static final int MAP_ENTRY=24;
    public static final int SELECT_LAST=57;
    public static final int NUMBER=28;
    public static final int LESS_THAN=74;
    public static final int BANG=44;
    public static final int ARGLIST=11;
    public static final int FALSE=68;
    public static final int METHOD=25;
    public static final int PROPERTY_OR_FIELD=9;
    public static final int LBRACKET=51;
    public static final int INDEXER=10;
    public static final int MOD=42;
    public static final int CONSTRUCTOR_ARRAY=14;
    public static final int FUNCTIONREF=16;
    public static final int NULL_LITERAL=64;
    public static final int NAMED_ARGUMENT=15;
    public static final int OR=36;
    public static final int PIPE=60;
    public static final int DOT=45;
    public static final int RCURLY=54;
    public static final int EXPRESSION=6;
    public static final int AND=37;
    public static final int LCURLY=61;
    public static final int REAL_TYPE_SUFFIX=88;
    public static final int STRING_LITERAL=62;
    public static final int SELECT=55;
    public static final int QUALIFIED_IDENTIFIER=7;
    public static final int RBRACKET=52;
    public static final int SUBTRACT=27;
    public static final int ASSIGN=30;
    public static final int BETWEEN=80;
    public static final int RPAREN=35;
    public static final int SIGN=89;
    public static final int LPAREN=34;
    public static final int HEX_DIGIT=71;
    public static final int PLUS=38;
    public static final int LIST_INITIALIZER=20;
    public static final int APOS=83;
    public static final int INTEGER_LITERAL=5;
    public static final int AT=50;
    public static final int ID=47;
    public static final int NOT_EQUAL=73;
    public static final int RANGE=18;
    public static final int POWER=43;
    public static final int TYPEREF=17;
    public static final int DECIMAL_DIGIT=69;
    public static final int WS=85;
    public static final int IS=79;
    public static final int DOLLAR=48;
    public static final int LESS_THAN_OR_EQUAL=75;
    public static final int SEMIRPAREN=29;
    public static final int DQ_STRING_LITERAL=63;
    public static final int HEXADECIMAL_INTEGER_LITERAL=65;
    public static final int MAP_INITIALIZER=21;
    public static final int LAMBDA=59;
    public static final int LOCALFUNC=23;
    public static final int IN=78;
    public static final int SEMI=82;
    public static final int CONSTRUCTOR=12;
    public static final int INTEGER_TYPE_SUFFIX=70;
    public static final int EQUAL=72;
    public static final int MATCHES=81;
    public static final int DOT_ESCAPED=84;
    public static final int UPTO=86;
    public static final int EOF=-1;
    public static final int QMARK=32;
    public static final int REFERENCE=8;
    public static final int PROJECT=53;
    public static final int DEFAULT=31;
    public static final int COLON=33;
    public static final int DIV=41;
    public static final int LOCALVAR=22;
    public static final int STAR=40;
    public static final int REAL_LITERAL=66;
    public static final int VARIABLEREF=19;
    public static final int EXPONENT_PART=87;
    public static final int TRUE=67;
    public static final int ADD=26;
    public static final int POUND=46;

        public SpringExpressionsParser(TokenStream input) {
            super(input);
            ruleMemo = new HashMap[52+1];
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:60:1: expr : expression EOF ;
    public final expr_return expr() throws RecognitionException {
        expr_return retval = new expr_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token EOF2=null;
        expression_return expression1 = null;


        Object EOF2_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:60:5: ( expression EOF )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:60:7: expression EOF
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_expression_in_expr177);
            expression1=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression1.getTree());
            EOF2=(Token)input.LT(1);
            match(input,EOF,FOLLOW_EOF_in_expr179); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:64:1: expression : logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )? ;
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
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:64:12: ( logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )? )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:65:5: logicalOrExpression ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )?
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_logicalOrExpression_in_expression208);
            logicalOrExpression3=logicalOrExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, logicalOrExpression3.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:66:5: ( ( ASSIGN logicalOrExpression ) | ( DEFAULT logicalOrExpression ) | ( QMARK expression COLON expression ) )?
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
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:66:7: ( ASSIGN logicalOrExpression )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:66:7: ( ASSIGN logicalOrExpression )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:66:8: ASSIGN logicalOrExpression
                    {
                    ASSIGN4=(Token)input.LT(1);
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_expression217); if (failed) return retval;
                    if ( backtracking==0 ) {
                    ASSIGN4_tree = (Object)adaptor.create(ASSIGN4);
                    root_0 = (Object)adaptor.becomeRoot(ASSIGN4_tree, root_0);
                    }
                    pushFollow(FOLLOW_logicalOrExpression_in_expression220);
                    logicalOrExpression5=logicalOrExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, logicalOrExpression5.getTree());

                    }


                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:67:6: ( DEFAULT logicalOrExpression )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:67:6: ( DEFAULT logicalOrExpression )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:67:7: DEFAULT logicalOrExpression
                    {
                    DEFAULT6=(Token)input.LT(1);
                    match(input,DEFAULT,FOLLOW_DEFAULT_in_expression230); if (failed) return retval;
                    if ( backtracking==0 ) {
                    DEFAULT6_tree = (Object)adaptor.create(DEFAULT6);
                    root_0 = (Object)adaptor.becomeRoot(DEFAULT6_tree, root_0);
                    }
                    pushFollow(FOLLOW_logicalOrExpression_in_expression233);
                    logicalOrExpression7=logicalOrExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, logicalOrExpression7.getTree());

                    }


                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:68:6: ( QMARK expression COLON expression )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:68:6: ( QMARK expression COLON expression )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:68:7: QMARK expression COLON expression
                    {
                    QMARK8=(Token)input.LT(1);
                    match(input,QMARK,FOLLOW_QMARK_in_expression243); if (failed) return retval;
                    if ( backtracking==0 ) {
                    QMARK8_tree = (Object)adaptor.create(QMARK8);
                    root_0 = (Object)adaptor.becomeRoot(QMARK8_tree, root_0);
                    }
                    pushFollow(FOLLOW_expression_in_expression246);
                    expression9=expression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, expression9.getTree());
                    COLON10=(Token)input.LT(1);
                    match(input,COLON,FOLLOW_COLON_in_expression248); if (failed) return retval;
                    pushFollow(FOLLOW_expression_in_expression251);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:70:1: parenExpr : LPAREN expression RPAREN ;
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
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:70:11: ( LPAREN expression RPAREN )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:70:13: LPAREN expression RPAREN
            {
            root_0 = (Object)adaptor.nil();

            LPAREN12=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_parenExpr262); if (failed) return retval;
            pushFollow(FOLLOW_expression_in_parenExpr265);
            expression13=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression13.getTree());
            RPAREN14=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_parenExpr267); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:73:1: logicalOrExpression : logicalAndExpression ( OR logicalAndExpression )* ;
    public final logicalOrExpression_return logicalOrExpression() throws RecognitionException {
        logicalOrExpression_return retval = new logicalOrExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token OR16=null;
        logicalAndExpression_return logicalAndExpression15 = null;

        logicalAndExpression_return logicalAndExpression17 = null;


        Object OR16_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:1: ( logicalAndExpression ( OR logicalAndExpression )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:3: logicalAndExpression ( OR logicalAndExpression )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_logicalAndExpression_in_logicalOrExpression280);
            logicalAndExpression15=logicalAndExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, logicalAndExpression15.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:24: ( OR logicalAndExpression )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0==OR) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:75:25: OR logicalAndExpression
            	    {
            	    OR16=(Token)input.LT(1);
            	    match(input,OR,FOLLOW_OR_in_logicalOrExpression283); if (failed) return retval;
            	    if ( backtracking==0 ) {
            	    OR16_tree = (Object)adaptor.create(OR16);
            	    root_0 = (Object)adaptor.becomeRoot(OR16_tree, root_0);
            	    }
            	    pushFollow(FOLLOW_logicalAndExpression_in_logicalOrExpression286);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:77:1: logicalAndExpression : relationalExpression ( AND relationalExpression )* ;
    public final logicalAndExpression_return logicalAndExpression() throws RecognitionException {
        logicalAndExpression_return retval = new logicalAndExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token AND19=null;
        relationalExpression_return relationalExpression18 = null;

        relationalExpression_return relationalExpression20 = null;


        Object AND19_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:78:1: ( relationalExpression ( AND relationalExpression )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:78:3: relationalExpression ( AND relationalExpression )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_relationalExpression_in_logicalAndExpression321);
            relationalExpression18=relationalExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, relationalExpression18.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:78:24: ( AND relationalExpression )*
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( (LA3_0==AND) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:78:25: AND relationalExpression
            	    {
            	    AND19=(Token)input.LT(1);
            	    match(input,AND,FOLLOW_AND_in_logicalAndExpression324); if (failed) return retval;
            	    if ( backtracking==0 ) {
            	    AND19_tree = (Object)adaptor.create(AND19);
            	    root_0 = (Object)adaptor.becomeRoot(AND19_tree, root_0);
            	    }
            	    pushFollow(FOLLOW_relationalExpression_in_logicalAndExpression327);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:1: relationalExpression : sumExpression ( relationalOperator sumExpression )? ;
    public final relationalExpression_return relationalExpression() throws RecognitionException {
        relationalExpression_return retval = new relationalExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        sumExpression_return sumExpression21 = null;

        relationalOperator_return relationalOperator22 = null;

        sumExpression_return sumExpression23 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:22: ( sumExpression ( relationalOperator sumExpression )? )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:24: sumExpression ( relationalOperator sumExpression )?
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_sumExpression_in_relationalExpression338);
            sumExpression21=sumExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, sumExpression21.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:38: ( relationalOperator sumExpression )?
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( ((LA4_0>=EQUAL && LA4_0<=MATCHES)) ) {
                alt4=1;
            }
            switch (alt4) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:80:39: relationalOperator sumExpression
                    {
                    pushFollow(FOLLOW_relationalOperator_in_relationalExpression341);
                    relationalOperator22=relationalOperator();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) root_0 = (Object)adaptor.becomeRoot(relationalOperator22.getTree(), root_0);
                    pushFollow(FOLLOW_sumExpression_in_relationalExpression344);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:82:1: sumExpression : productExpression ( ( PLUS | MINUS ) productExpression )* ;
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
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:83:2: ( productExpression ( ( PLUS | MINUS ) productExpression )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:83:4: productExpression ( ( PLUS | MINUS ) productExpression )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_productExpression_in_sumExpression355);
            productExpression24=productExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, productExpression24.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:83:22: ( ( PLUS | MINUS ) productExpression )*
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( ((LA6_0>=PLUS && LA6_0<=MINUS)) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:83:24: ( PLUS | MINUS ) productExpression
            	    {
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:83:24: ( PLUS | MINUS )
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
            	            new NoViableAltException("83:24: ( PLUS | MINUS )", 5, 0, input);

            	        throw nvae;
            	    }
            	    switch (alt5) {
            	        case 1 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:83:25: PLUS
            	            {
            	            PLUS25=(Token)input.LT(1);
            	            match(input,PLUS,FOLLOW_PLUS_in_sumExpression360); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            PLUS25_tree = (Object)adaptor.create(PLUS25);
            	            root_0 = (Object)adaptor.becomeRoot(PLUS25_tree, root_0);
            	            }

            	            }
            	            break;
            	        case 2 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:83:33: MINUS
            	            {
            	            MINUS26=(Token)input.LT(1);
            	            match(input,MINUS,FOLLOW_MINUS_in_sumExpression365); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            MINUS26_tree = (Object)adaptor.create(MINUS26);
            	            root_0 = (Object)adaptor.becomeRoot(MINUS26_tree, root_0);
            	            }

            	            }
            	            break;

            	    }

            	    pushFollow(FOLLOW_productExpression_in_sumExpression369);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:86:1: productExpression : powerExpr ( ( STAR | DIV | MOD ) powerExpr )* ;
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
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:87:2: ( powerExpr ( ( STAR | DIV | MOD ) powerExpr )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:87:4: powerExpr ( ( STAR | DIV | MOD ) powerExpr )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_powerExpr_in_productExpression381);
            powerExpr28=powerExpr();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, powerExpr28.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:87:14: ( ( STAR | DIV | MOD ) powerExpr )*
            loop8:
            do {
                int alt8=2;
                int LA8_0 = input.LA(1);

                if ( ((LA8_0>=STAR && LA8_0<=MOD)) ) {
                    alt8=1;
                }


                switch (alt8) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:87:15: ( STAR | DIV | MOD ) powerExpr
            	    {
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:87:15: ( STAR | DIV | MOD )
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
            	            new NoViableAltException("87:15: ( STAR | DIV | MOD )", 7, 0, input);

            	        throw nvae;
            	    }

            	    switch (alt7) {
            	        case 1 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:87:16: STAR
            	            {
            	            STAR29=(Token)input.LT(1);
            	            match(input,STAR,FOLLOW_STAR_in_productExpression385); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            STAR29_tree = (Object)adaptor.create(STAR29);
            	            root_0 = (Object)adaptor.becomeRoot(STAR29_tree, root_0);
            	            }

            	            }
            	            break;
            	        case 2 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:87:24: DIV
            	            {
            	            DIV30=(Token)input.LT(1);
            	            match(input,DIV,FOLLOW_DIV_in_productExpression390); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            DIV30_tree = (Object)adaptor.create(DIV30);
            	            root_0 = (Object)adaptor.becomeRoot(DIV30_tree, root_0);
            	            }

            	            }
            	            break;
            	        case 3 :
            	            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:87:30: MOD
            	            {
            	            MOD31=(Token)input.LT(1);
            	            match(input,MOD,FOLLOW_MOD_in_productExpression394); if (failed) return retval;
            	            if ( backtracking==0 ) {
            	            MOD31_tree = (Object)adaptor.create(MOD31);
            	            root_0 = (Object)adaptor.becomeRoot(MOD31_tree, root_0);
            	            }

            	            }
            	            break;

            	    }

            	    pushFollow(FOLLOW_powerExpr_in_productExpression398);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:89:1: powerExpr : unaryExpression ( POWER unaryExpression )? ;
    public final powerExpr_return powerExpr() throws RecognitionException {
        powerExpr_return retval = new powerExpr_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token POWER34=null;
        unaryExpression_return unaryExpression33 = null;

        unaryExpression_return unaryExpression35 = null;


        Object POWER34_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:89:12: ( unaryExpression ( POWER unaryExpression )? )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:89:14: unaryExpression ( POWER unaryExpression )?
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_unaryExpression_in_powerExpr410);
            unaryExpression33=unaryExpression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, unaryExpression33.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:89:30: ( POWER unaryExpression )?
            int alt9=2;
            int LA9_0 = input.LA(1);

            if ( (LA9_0==POWER) ) {
                alt9=1;
            }
            switch (alt9) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:89:31: POWER unaryExpression
                    {
                    POWER34=(Token)input.LT(1);
                    match(input,POWER,FOLLOW_POWER_in_powerExpr413); if (failed) return retval;
                    if ( backtracking==0 ) {
                    POWER34_tree = (Object)adaptor.create(POWER34);
                    root_0 = (Object)adaptor.becomeRoot(POWER34_tree, root_0);
                    }
                    pushFollow(FOLLOW_unaryExpression_in_powerExpr416);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:91:1: unaryExpression : ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression );
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
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:92:2: ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression )
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( ((LA11_0>=PLUS && LA11_0<=MINUS)||LA11_0==BANG) ) {
                alt11=1;
            }
            else if ( (LA11_0==INTEGER_LITERAL||LA11_0==LPAREN||(LA11_0>=POUND && LA11_0<=DOLLAR)||(LA11_0>=AT && LA11_0<=LBRACKET)||LA11_0==PROJECT||(LA11_0>=SELECT && LA11_0<=LAMBDA)||(LA11_0>=LCURLY && LA11_0<=FALSE)||LA11_0==90) ) {
                alt11=2;
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("91:1: unaryExpression : ( ( PLUS | MINUS | BANG ) unaryExpression | primaryExpression );", 11, 0, input);

                throw nvae;
            }
            switch (alt11) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:92:4: ( PLUS | MINUS | BANG ) unaryExpression
                    {
                    root_0 = (Object)adaptor.nil();

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:92:4: ( PLUS | MINUS | BANG )
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
                            new NoViableAltException("92:4: ( PLUS | MINUS | BANG )", 10, 0, input);

                        throw nvae;
                    }

                    switch (alt10) {
                        case 1 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:92:5: PLUS
                            {
                            PLUS36=(Token)input.LT(1);
                            match(input,PLUS,FOLLOW_PLUS_in_unaryExpression430); if (failed) return retval;
                            if ( backtracking==0 ) {
                            PLUS36_tree = (Object)adaptor.create(PLUS36);
                            root_0 = (Object)adaptor.becomeRoot(PLUS36_tree, root_0);
                            }

                            }
                            break;
                        case 2 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:92:13: MINUS
                            {
                            MINUS37=(Token)input.LT(1);
                            match(input,MINUS,FOLLOW_MINUS_in_unaryExpression435); if (failed) return retval;
                            if ( backtracking==0 ) {
                            MINUS37_tree = (Object)adaptor.create(MINUS37);
                            root_0 = (Object)adaptor.becomeRoot(MINUS37_tree, root_0);
                            }

                            }
                            break;
                        case 3 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:92:22: BANG
                            {
                            BANG38=(Token)input.LT(1);
                            match(input,BANG,FOLLOW_BANG_in_unaryExpression440); if (failed) return retval;
                            if ( backtracking==0 ) {
                            BANG38_tree = (Object)adaptor.create(BANG38);
                            root_0 = (Object)adaptor.becomeRoot(BANG38_tree, root_0);
                            }

                            }
                            break;

                    }

                    pushFollow(FOLLOW_unaryExpression_in_unaryExpression444);
                    unaryExpression39=unaryExpression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, unaryExpression39.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:93:4: primaryExpression
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_primaryExpression_in_unaryExpression450);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:95:1: primaryExpression : startNode ( node )? -> ^( EXPRESSION startNode ( node )? ) ;
    public final primaryExpression_return primaryExpression() throws RecognitionException {
        primaryExpression_return retval = new primaryExpression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        startNode_return startNode41 = null;

        node_return node42 = null;


        RewriteRuleSubtreeStream stream_node=new RewriteRuleSubtreeStream(adaptor,"rule node");
        RewriteRuleSubtreeStream stream_startNode=new RewriteRuleSubtreeStream(adaptor,"rule startNode");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:96:5: ( startNode ( node )? -> ^( EXPRESSION startNode ( node )? ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:96:7: startNode ( node )?
            {
            pushFollow(FOLLOW_startNode_in_primaryExpression464);
            startNode41=startNode();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_startNode.add(startNode41.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:96:17: ( node )?
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0==DOT||LA12_0==LBRACKET) ) {
                alt12=1;
            }
            switch (alt12) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:96:18: node
                    {
                    pushFollow(FOLLOW_node_in_primaryExpression467);
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
            // 96:25: -> ^( EXPRESSION startNode ( node )? )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:96:28: ^( EXPRESSION startNode ( node )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(EXPRESSION, "EXPRESSION"), root_1);

                adaptor.addChild(root_1, stream_startNode.next());
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:96:51: ( node )?
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:98:1: startNode : ( parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );
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
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:99:5: ( parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda )
            int alt13=16;
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

                if ( (LA13_3==LCURLY) ) {
                    alt13=15;
                }
                else if ( (LA13_3==ID) ) {
                    alt13=3;
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("98:1: startNode : ( parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 13, 3, input);

                    throw nvae;
                }
                }
                break;
            case DOLLAR:
                {
                alt13=4;
                }
                break;
            case AT:
                {
                alt13=5;
                }
                break;
            case LBRACKET:
                {
                alt13=6;
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
                alt13=7;
                }
                break;
            case TYPE:
                {
                alt13=8;
                }
                break;
            case 90:
                {
                alt13=9;
                }
                break;
            case PROJECT:
                {
                alt13=10;
                }
                break;
            case SELECT:
                {
                alt13=11;
                }
                break;
            case SELECT_FIRST:
                {
                alt13=12;
                }
                break;
            case SELECT_LAST:
                {
                alt13=13;
                }
                break;
            case LCURLY:
                {
                alt13=14;
                }
                break;
            case LAMBDA:
                {
                alt13=16;
                }
                break;
            default:
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("98:1: startNode : ( parenExpr | methodOrProperty | functionOrVar | localFunctionOrVar | reference | indexer | literal | type | constructor | projection | selection | firstSelection | lastSelection | listInitializer | mapInitializer | lambda );", 13, 0, input);

                throw nvae;
            }

            switch (alt13) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:100:5: parenExpr
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_parenExpr_in_startNode500);
                    parenExpr43=parenExpr();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, parenExpr43.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:101:7: methodOrProperty
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_methodOrProperty_in_startNode508);
                    methodOrProperty44=methodOrProperty();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, methodOrProperty44.getTree());

                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:102:7: functionOrVar
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_functionOrVar_in_startNode517);
                    functionOrVar45=functionOrVar();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, functionOrVar45.getTree());

                    }
                    break;
                case 4 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:103:7: localFunctionOrVar
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_localFunctionOrVar_in_startNode525);
                    localFunctionOrVar46=localFunctionOrVar();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, localFunctionOrVar46.getTree());

                    }
                    break;
                case 5 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:104:7: reference
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_reference_in_startNode533);
                    reference47=reference();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, reference47.getTree());

                    }
                    break;
                case 6 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:105:7: indexer
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_indexer_in_startNode541);
                    indexer48=indexer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, indexer48.getTree());

                    }
                    break;
                case 7 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:106:7: literal
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_literal_in_startNode549);
                    literal49=literal();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, literal49.getTree());

                    }
                    break;
                case 8 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:107:7: type
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_type_in_startNode557);
                    type50=type();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, type50.getTree());

                    }
                    break;
                case 9 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:108:7: constructor
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_constructor_in_startNode565);
                    constructor51=constructor();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, constructor51.getTree());

                    }
                    break;
                case 10 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:109:7: projection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_projection_in_startNode573);
                    projection52=projection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, projection52.getTree());

                    }
                    break;
                case 11 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:110:7: selection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_selection_in_startNode582);
                    selection53=selection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, selection53.getTree());

                    }
                    break;
                case 12 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:111:7: firstSelection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_firstSelection_in_startNode591);
                    firstSelection54=firstSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, firstSelection54.getTree());

                    }
                    break;
                case 13 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:112:7: lastSelection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_lastSelection_in_startNode599);
                    lastSelection55=lastSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, lastSelection55.getTree());

                    }
                    break;
                case 14 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:113:7: listInitializer
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_listInitializer_in_startNode607);
                    listInitializer56=listInitializer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, listInitializer56.getTree());

                    }
                    break;
                case 15 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:114:7: mapInitializer
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_mapInitializer_in_startNode615);
                    mapInitializer57=mapInitializer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, mapInitializer57.getTree());

                    }
                    break;
                case 16 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:115:7: lambda
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_lambda_in_startNode623);
                    lambda58=lambda();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, lambda58.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:118:1: node : ( ( DOT dottedNode ) | nonDottedNode )+ ;
    public final node_return node() throws RecognitionException {
        node_return retval = new node_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token DOT59=null;
        dottedNode_return dottedNode60 = null;

        nonDottedNode_return nonDottedNode61 = null;


        Object DOT59_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:119:2: ( ( ( DOT dottedNode ) | nonDottedNode )+ )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:119:4: ( ( DOT dottedNode ) | nonDottedNode )+
            {
            root_0 = (Object)adaptor.nil();

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:119:4: ( ( DOT dottedNode ) | nonDottedNode )+
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
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:119:5: ( DOT dottedNode )
            	    {
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:119:5: ( DOT dottedNode )
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:119:6: DOT dottedNode
            	    {
            	    DOT59=(Token)input.LT(1);
            	    match(input,DOT,FOLLOW_DOT_in_node643); if (failed) return retval;
            	    if ( backtracking==0 ) {
            	    DOT59_tree = (Object)adaptor.create(DOT59);
            	    adaptor.addChild(root_0, DOT59_tree);
            	    }
            	    pushFollow(FOLLOW_dottedNode_in_node645);
            	    dottedNode60=dottedNode();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, dottedNode60.getTree());

            	    }


            	    }
            	    break;
            	case 2 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:119:24: nonDottedNode
            	    {
            	    pushFollow(FOLLOW_nonDottedNode_in_node650);
            	    nonDottedNode61=nonDottedNode();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) adaptor.addChild(root_0, nonDottedNode61.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:121:1: nonDottedNode : indexer ;
    public final nonDottedNode_return nonDottedNode() throws RecognitionException {
        nonDottedNode_return retval = new nonDottedNode_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        indexer_return indexer62 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:122:2: ( indexer )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:122:4: indexer
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_indexer_in_nonDottedNode662);
            indexer62=indexer();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, indexer62.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:124:1: dottedNode : ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection ) ) ;
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
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:125:2: ( ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection ) ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:126:2: ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection ) )
            {
            root_0 = (Object)adaptor.nil();

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:126:2: ( ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:126:3: ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection )
            {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:126:3: ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection )
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
                    new NoViableAltException("126:3: ( methodOrProperty | functionOrVar | projection | selection | firstSelection | lastSelection )", 15, 0, input);

                throw nvae;
            }

            switch (alt15) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:126:4: methodOrProperty
                    {
                    pushFollow(FOLLOW_methodOrProperty_in_dottedNode675);
                    methodOrProperty63=methodOrProperty();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, methodOrProperty63.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:127:4: functionOrVar
                    {
                    pushFollow(FOLLOW_functionOrVar_in_dottedNode681);
                    functionOrVar64=functionOrVar();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, functionOrVar64.getTree());

                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:128:7: projection
                    {
                    pushFollow(FOLLOW_projection_in_dottedNode689);
                    projection65=projection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, projection65.getTree());

                    }
                    break;
                case 4 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:129:7: selection
                    {
                    pushFollow(FOLLOW_selection_in_dottedNode698);
                    selection66=selection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, selection66.getTree());

                    }
                    break;
                case 5 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:130:7: firstSelection
                    {
                    pushFollow(FOLLOW_firstSelection_in_dottedNode707);
                    firstSelection67=firstSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, firstSelection67.getTree());

                    }
                    break;
                case 6 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:131:7: lastSelection
                    {
                    pushFollow(FOLLOW_lastSelection_in_dottedNode716);
                    lastSelection68=lastSelection();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, lastSelection68.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:135:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );
    public final functionOrVar_return functionOrVar() throws RecognitionException {
        functionOrVar_return retval = new functionOrVar_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        function_return function69 = null;

        var_return var70 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:5: ( ( POUND ID LPAREN )=> function | var )
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
                            new NoViableAltException("135:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 16, 2, input);

                        throw nvae;
                    }
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("135:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 16, 1, input);

                    throw nvae;
                }
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("135:1: functionOrVar : ( ( POUND ID LPAREN )=> function | var );", 16, 0, input);

                throw nvae;
            }
            switch (alt16) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:7: ( POUND ID LPAREN )=> function
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_function_in_functionOrVar750);
                    function69=function();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, function69.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:137:7: var
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_var_in_functionOrVar758);
                    var70=var();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, var70.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:140:1: function : POUND id= ID methodArgs -> ^( FUNCTIONREF[$id] methodArgs ) ;
    public final function_return function() throws RecognitionException {
        function_return retval = new function_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token POUND71=null;
        methodArgs_return methodArgs72 = null;


        Object id_tree=null;
        Object POUND71_tree=null;
        RewriteRuleTokenStream stream_POUND=new RewriteRuleTokenStream(adaptor,"token POUND");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleSubtreeStream stream_methodArgs=new RewriteRuleSubtreeStream(adaptor,"rule methodArgs");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:140:10: ( POUND id= ID methodArgs -> ^( FUNCTIONREF[$id] methodArgs ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:140:12: POUND id= ID methodArgs
            {
            POUND71=(Token)input.LT(1);
            match(input,POUND,FOLLOW_POUND_in_function775); if (failed) return retval;
            if ( backtracking==0 ) stream_POUND.add(POUND71);

            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_function779); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(id);

            pushFollow(FOLLOW_methodArgs_in_function781);
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
            // 140:35: -> ^( FUNCTIONREF[$id] methodArgs )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:140:38: ^( FUNCTIONREF[$id] methodArgs )
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:142:1: var : POUND id= ID -> ^( VARIABLEREF[$id] ) ;
    public final var_return var() throws RecognitionException {
        var_return retval = new var_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token POUND73=null;

        Object id_tree=null;
        Object POUND73_tree=null;
        RewriteRuleTokenStream stream_POUND=new RewriteRuleTokenStream(adaptor,"token POUND");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:142:5: ( POUND id= ID -> ^( VARIABLEREF[$id] ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:142:7: POUND id= ID
            {
            POUND73=(Token)input.LT(1);
            match(input,POUND,FOLLOW_POUND_in_var802); if (failed) return retval;
            if ( backtracking==0 ) stream_POUND.add(POUND73);

            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_var806); if (failed) return retval;
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
            // 142:19: -> ^( VARIABLEREF[$id] )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:142:22: ^( VARIABLEREF[$id] )
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:144:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );
    public final localFunctionOrVar_return localFunctionOrVar() throws RecognitionException {
        localFunctionOrVar_return retval = new localFunctionOrVar_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        localFunction_return localFunction74 = null;

        localVar_return localVar75 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:145:2: ( ( DOLLAR ID LPAREN )=> localFunction | localVar )
            int alt17=2;
            int LA17_0 = input.LA(1);

            if ( (LA17_0==DOLLAR) ) {
                int LA17_1 = input.LA(2);

                if ( (LA17_1==ID) ) {
                    int LA17_2 = input.LA(3);

                    if ( (synpred2()) ) {
                        alt17=1;
                    }
                    else if ( (true) ) {
                        alt17=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("144:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );", 17, 2, input);

                        throw nvae;
                    }
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("144:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );", 17, 1, input);

                    throw nvae;
                }
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("144:1: localFunctionOrVar : ( ( DOLLAR ID LPAREN )=> localFunction | localVar );", 17, 0, input);

                throw nvae;
            }
            switch (alt17) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:145:4: ( DOLLAR ID LPAREN )=> localFunction
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_localFunction_in_localFunctionOrVar833);
                    localFunction74=localFunction();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, localFunction74.getTree());

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:146:4: localVar
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_localVar_in_localFunctionOrVar838);
                    localVar75=localVar();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, localVar75.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:149:1: localFunction : DOLLAR id= ID methodArgs -> ^( LOCALFUNC[$id] methodArgs ) ;
    public final localFunction_return localFunction() throws RecognitionException {
        localFunction_return retval = new localFunction_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token DOLLAR76=null;
        methodArgs_return methodArgs77 = null;


        Object id_tree=null;
        Object DOLLAR76_tree=null;
        RewriteRuleTokenStream stream_DOLLAR=new RewriteRuleTokenStream(adaptor,"token DOLLAR");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleSubtreeStream stream_methodArgs=new RewriteRuleSubtreeStream(adaptor,"rule methodArgs");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:149:15: ( DOLLAR id= ID methodArgs -> ^( LOCALFUNC[$id] methodArgs ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:149:17: DOLLAR id= ID methodArgs
            {
            DOLLAR76=(Token)input.LT(1);
            match(input,DOLLAR,FOLLOW_DOLLAR_in_localFunction848); if (failed) return retval;
            if ( backtracking==0 ) stream_DOLLAR.add(DOLLAR76);

            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_localFunction852); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(id);

            pushFollow(FOLLOW_methodArgs_in_localFunction854);
            methodArgs77=methodArgs();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_methodArgs.add(methodArgs77.getTree());

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
            // 149:41: -> ^( LOCALFUNC[$id] methodArgs )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:149:44: ^( LOCALFUNC[$id] methodArgs )
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:150:1: localVar : DOLLAR id= ID -> ^( LOCALVAR[$id] ) ;
    public final localVar_return localVar() throws RecognitionException {
        localVar_return retval = new localVar_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token DOLLAR78=null;

        Object id_tree=null;
        Object DOLLAR78_tree=null;
        RewriteRuleTokenStream stream_DOLLAR=new RewriteRuleTokenStream(adaptor,"token DOLLAR");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:150:9: ( DOLLAR id= ID -> ^( LOCALVAR[$id] ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:150:11: DOLLAR id= ID
            {
            DOLLAR78=(Token)input.LT(1);
            match(input,DOLLAR,FOLLOW_DOLLAR_in_localVar869); if (failed) return retval;
            if ( backtracking==0 ) stream_DOLLAR.add(DOLLAR78);

            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_localVar873); if (failed) return retval;
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
            // 150:24: -> ^( LOCALVAR[$id] )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:150:27: ^( LOCALVAR[$id] )
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:152:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );
    public final methodOrProperty_return methodOrProperty() throws RecognitionException {
        methodOrProperty_return retval = new methodOrProperty_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        methodArgs_return methodArgs79 = null;

        property_return property80 = null;


        Object id_tree=null;
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleSubtreeStream stream_methodArgs=new RewriteRuleSubtreeStream(adaptor,"rule methodArgs");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:153:2: ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property )
            int alt18=2;
            int LA18_0 = input.LA(1);

            if ( (LA18_0==ID) ) {
                int LA18_1 = input.LA(2);

                if ( (LA18_1==EOF||(LA18_1>=ASSIGN && LA18_1<=COLON)||(LA18_1>=RPAREN && LA18_1<=POWER)||LA18_1==DOT||LA18_1==COMMA||(LA18_1>=LBRACKET && LA18_1<=RBRACKET)||LA18_1==RCURLY||(LA18_1>=EQUAL && LA18_1<=MATCHES)) ) {
                    alt18=2;
                }
                else if ( (LA18_1==LPAREN) && (synpred3())) {
                    alt18=1;
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("152:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );", 18, 1, input);

                    throw nvae;
                }
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("152:1: methodOrProperty : ( ( ID LPAREN )=>id= ID methodArgs -> ^( METHOD[$id] methodArgs ) | property );", 18, 0, input);

                throw nvae;
            }
            switch (alt18) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:153:4: ( ID LPAREN )=>id= ID methodArgs
                    {
                    id=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_methodOrProperty899); if (failed) return retval;
                    if ( backtracking==0 ) stream_ID.add(id);

                    pushFollow(FOLLOW_methodArgs_in_methodOrProperty901);
                    methodArgs79=methodArgs();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_methodArgs.add(methodArgs79.getTree());

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
                    // 153:36: -> ^( METHOD[$id] methodArgs )
                    {
                        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:153:39: ^( METHOD[$id] methodArgs )
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
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:154:4: property
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_property_in_methodOrProperty915);
                    property80=property();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, property80.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:160:1: methodArgs : LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN ;
    public final methodArgs_return methodArgs() throws RecognitionException {
        methodArgs_return retval = new methodArgs_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LPAREN81=null;
        Token COMMA83=null;
        Token COMMA85=null;
        Token RPAREN86=null;
        argument_return argument82 = null;

        argument_return argument84 = null;


        Object LPAREN81_tree=null;
        Object COMMA83_tree=null;
        Object COMMA85_tree=null;
        Object RPAREN86_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:160:12: ( LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:160:15: LPAREN ( argument ( COMMA argument )* ( COMMA )? )? RPAREN
            {
            root_0 = (Object)adaptor.nil();

            LPAREN81=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_methodArgs930); if (failed) return retval;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:160:23: ( argument ( COMMA argument )* ( COMMA )? )?
            int alt21=2;
            int LA21_0 = input.LA(1);

            if ( (LA21_0==INTEGER_LITERAL||LA21_0==LPAREN||(LA21_0>=PLUS && LA21_0<=MINUS)||LA21_0==BANG||(LA21_0>=POUND && LA21_0<=DOLLAR)||(LA21_0>=AT && LA21_0<=LBRACKET)||LA21_0==PROJECT||(LA21_0>=SELECT && LA21_0<=LAMBDA)||(LA21_0>=LCURLY && LA21_0<=FALSE)||LA21_0==90) ) {
                alt21=1;
            }
            switch (alt21) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:160:24: argument ( COMMA argument )* ( COMMA )?
                    {
                    pushFollow(FOLLOW_argument_in_methodArgs934);
                    argument82=argument();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, argument82.getTree());
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:160:33: ( COMMA argument )*
                    loop19:
                    do {
                        int alt19=2;
                        int LA19_0 = input.LA(1);

                        if ( (LA19_0==COMMA) ) {
                            int LA19_1 = input.LA(2);

                            if ( (LA19_1==INTEGER_LITERAL||LA19_1==LPAREN||(LA19_1>=PLUS && LA19_1<=MINUS)||LA19_1==BANG||(LA19_1>=POUND && LA19_1<=DOLLAR)||(LA19_1>=AT && LA19_1<=LBRACKET)||LA19_1==PROJECT||(LA19_1>=SELECT && LA19_1<=LAMBDA)||(LA19_1>=LCURLY && LA19_1<=FALSE)||LA19_1==90) ) {
                                alt19=1;
                            }


                        }


                        switch (alt19) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:160:34: COMMA argument
                    	    {
                    	    COMMA83=(Token)input.LT(1);
                    	    match(input,COMMA,FOLLOW_COMMA_in_methodArgs937); if (failed) return retval;
                    	    pushFollow(FOLLOW_argument_in_methodArgs940);
                    	    argument84=argument();
                    	    _fsp--;
                    	    if (failed) return retval;
                    	    if ( backtracking==0 ) adaptor.addChild(root_0, argument84.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop19;
                        }
                    } while (true);

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:160:52: ( COMMA )?
                    int alt20=2;
                    int LA20_0 = input.LA(1);

                    if ( (LA20_0==COMMA) ) {
                        alt20=1;
                    }
                    switch (alt20) {
                        case 1 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:160:53: COMMA
                            {
                            COMMA85=(Token)input.LT(1);
                            match(input,COMMA,FOLLOW_COMMA_in_methodArgs945); if (failed) return retval;

                            }
                            break;

                    }


                    }
                    break;

            }

            RPAREN86=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_methodArgs952); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:165:1: property : id= ID -> ^( PROPERTY_OR_FIELD[$id] ) ;
    public final property_return property() throws RecognitionException {
        property_return retval = new property_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;

        Object id_tree=null;
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:165:9: (id= ID -> ^( PROPERTY_OR_FIELD[$id] ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:165:11: id= ID
            {
            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_property965); if (failed) return retval;
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
            // 165:17: -> ^( PROPERTY_OR_FIELD[$id] )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:165:20: ^( PROPERTY_OR_FIELD[$id] )
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:172:1: reference : AT pos= LPAREN (cn= contextName COLON )? (q= qualifiedId )? RPAREN -> ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN ) ;
    public final reference_return reference() throws RecognitionException {
        reference_return retval = new reference_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token pos=null;
        Token AT87=null;
        Token COLON88=null;
        Token RPAREN89=null;
        contextName_return cn = null;

        qualifiedId_return q = null;


        Object pos_tree=null;
        Object AT87_tree=null;
        Object COLON88_tree=null;
        Object RPAREN89_tree=null;
        RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
        RewriteRuleTokenStream stream_COLON=new RewriteRuleTokenStream(adaptor,"token COLON");
        RewriteRuleTokenStream stream_LPAREN=new RewriteRuleTokenStream(adaptor,"token LPAREN");
        RewriteRuleTokenStream stream_AT=new RewriteRuleTokenStream(adaptor,"token AT");
        RewriteRuleSubtreeStream stream_contextName=new RewriteRuleSubtreeStream(adaptor,"rule contextName");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:173:2: ( AT pos= LPAREN (cn= contextName COLON )? (q= qualifiedId )? RPAREN -> ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:173:5: AT pos= LPAREN (cn= contextName COLON )? (q= qualifiedId )? RPAREN
            {
            AT87=(Token)input.LT(1);
            match(input,AT,FOLLOW_AT_in_reference987); if (failed) return retval;
            if ( backtracking==0 ) stream_AT.add(AT87);

            pos=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_reference991); if (failed) return retval;
            if ( backtracking==0 ) stream_LPAREN.add(pos);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:173:19: (cn= contextName COLON )?
            int alt22=2;
            int LA22_0 = input.LA(1);

            if ( (LA22_0==ID) ) {
                int LA22_1 = input.LA(2);

                if ( (LA22_1==COLON||LA22_1==DIV) ) {
                    alt22=1;
                }
            }
            switch (alt22) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:173:20: cn= contextName COLON
                    {
                    pushFollow(FOLLOW_contextName_in_reference996);
                    cn=contextName();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_contextName.add(cn.getTree());
                    COLON88=(Token)input.LT(1);
                    match(input,COLON,FOLLOW_COLON_in_reference998); if (failed) return retval;
                    if ( backtracking==0 ) stream_COLON.add(COLON88);


                    }
                    break;

            }

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:173:43: (q= qualifiedId )?
            int alt23=2;
            int LA23_0 = input.LA(1);

            if ( (LA23_0==ID) ) {
                alt23=1;
            }
            switch (alt23) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:173:44: q= qualifiedId
                    {
                    pushFollow(FOLLOW_qualifiedId_in_reference1005);
                    q=qualifiedId();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_qualifiedId.add(q.getTree());

                    }
                    break;

            }

            RPAREN89=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_reference1009); if (failed) return retval;
            if ( backtracking==0 ) stream_RPAREN.add(RPAREN89);


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
            // 174:4: -> ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:174:7: ^( REFERENCE[$pos] ( $cn COLON )? ( $q)? RPAREN )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(REFERENCE, pos), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:174:25: ( $cn COLON )?
                if ( stream_cn.hasNext()||stream_COLON.hasNext() ) {
                    adaptor.addChild(root_1, stream_cn.next());
                    adaptor.addChild(root_1, stream_COLON.next());

                }
                stream_cn.reset();
                stream_COLON.reset();
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:174:38: ( $q)?
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:180:1: indexer : LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET -> ^( INDEXER $r1 ( $r2)* ) ;
    public final indexer_return indexer() throws RecognitionException {
        indexer_return retval = new indexer_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LBRACKET90=null;
        Token COMMA91=null;
        Token RBRACKET92=null;
        argument_return r1 = null;

        argument_return r2 = null;


        Object LBRACKET90_tree=null;
        Object COMMA91_tree=null;
        Object RBRACKET92_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_LBRACKET=new RewriteRuleTokenStream(adaptor,"token LBRACKET");
        RewriteRuleTokenStream stream_RBRACKET=new RewriteRuleTokenStream(adaptor,"token RBRACKET");
        RewriteRuleSubtreeStream stream_argument=new RewriteRuleSubtreeStream(adaptor,"rule argument");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:180:8: ( LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET -> ^( INDEXER $r1 ( $r2)* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:180:10: LBRACKET r1= argument ( COMMA r2= argument )* RBRACKET
            {
            LBRACKET90=(Token)input.LT(1);
            match(input,LBRACKET,FOLLOW_LBRACKET_in_indexer1044); if (failed) return retval;
            if ( backtracking==0 ) stream_LBRACKET.add(LBRACKET90);

            pushFollow(FOLLOW_argument_in_indexer1048);
            r1=argument();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_argument.add(r1.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:180:31: ( COMMA r2= argument )*
            loop24:
            do {
                int alt24=2;
                int LA24_0 = input.LA(1);

                if ( (LA24_0==COMMA) ) {
                    alt24=1;
                }


                switch (alt24) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:180:32: COMMA r2= argument
            	    {
            	    COMMA91=(Token)input.LT(1);
            	    match(input,COMMA,FOLLOW_COMMA_in_indexer1051); if (failed) return retval;
            	    if ( backtracking==0 ) stream_COMMA.add(COMMA91);

            	    pushFollow(FOLLOW_argument_in_indexer1055);
            	    r2=argument();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) stream_argument.add(r2.getTree());

            	    }
            	    break;

            	default :
            	    break loop24;
                }
            } while (true);

            RBRACKET92=(Token)input.LT(1);
            match(input,RBRACKET,FOLLOW_RBRACKET_in_indexer1059); if (failed) return retval;
            if ( backtracking==0 ) stream_RBRACKET.add(RBRACKET92);


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
            // 180:61: -> ^( INDEXER $r1 ( $r2)* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:180:64: ^( INDEXER $r1 ( $r2)* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(INDEXER, "INDEXER"), root_1);

                adaptor.addChild(root_1, stream_r1.next());
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:180:78: ( $r2)*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:185:1: projection : PROJECT expression RCURLY ;
    public final projection_return projection() throws RecognitionException {
        projection_return retval = new projection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token PROJECT93=null;
        Token RCURLY95=null;
        expression_return expression94 = null;


        Object PROJECT93_tree=null;
        Object RCURLY95_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:185:11: ( PROJECT expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:185:13: PROJECT expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            PROJECT93=(Token)input.LT(1);
            match(input,PROJECT,FOLLOW_PROJECT_in_projection1086); if (failed) return retval;
            if ( backtracking==0 ) {
            PROJECT93_tree = (Object)adaptor.create(PROJECT93);
            root_0 = (Object)adaptor.becomeRoot(PROJECT93_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_projection1089);
            expression94=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression94.getTree());
            RCURLY95=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_projection1091); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:187:1: selection : SELECT expression RCURLY ;
    public final selection_return selection() throws RecognitionException {
        selection_return retval = new selection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token SELECT96=null;
        Token RCURLY98=null;
        expression_return expression97 = null;


        Object SELECT96_tree=null;
        Object RCURLY98_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:187:10: ( SELECT expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:187:12: SELECT expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            SELECT96=(Token)input.LT(1);
            match(input,SELECT,FOLLOW_SELECT_in_selection1099); if (failed) return retval;
            if ( backtracking==0 ) {
            SELECT96_tree = (Object)adaptor.create(SELECT96);
            root_0 = (Object)adaptor.becomeRoot(SELECT96_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_selection1102);
            expression97=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression97.getTree());
            RCURLY98=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_selection1104); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:189:1: firstSelection : SELECT_FIRST expression RCURLY ;
    public final firstSelection_return firstSelection() throws RecognitionException {
        firstSelection_return retval = new firstSelection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token SELECT_FIRST99=null;
        Token RCURLY101=null;
        expression_return expression100 = null;


        Object SELECT_FIRST99_tree=null;
        Object RCURLY101_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:189:15: ( SELECT_FIRST expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:189:17: SELECT_FIRST expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            SELECT_FIRST99=(Token)input.LT(1);
            match(input,SELECT_FIRST,FOLLOW_SELECT_FIRST_in_firstSelection1112); if (failed) return retval;
            if ( backtracking==0 ) {
            SELECT_FIRST99_tree = (Object)adaptor.create(SELECT_FIRST99);
            root_0 = (Object)adaptor.becomeRoot(SELECT_FIRST99_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_firstSelection1115);
            expression100=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression100.getTree());
            RCURLY101=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_firstSelection1117); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:191:1: lastSelection : SELECT_LAST expression RCURLY ;
    public final lastSelection_return lastSelection() throws RecognitionException {
        lastSelection_return retval = new lastSelection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token SELECT_LAST102=null;
        Token RCURLY104=null;
        expression_return expression103 = null;


        Object SELECT_LAST102_tree=null;
        Object RCURLY104_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:191:14: ( SELECT_LAST expression RCURLY )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:191:16: SELECT_LAST expression RCURLY
            {
            root_0 = (Object)adaptor.nil();

            SELECT_LAST102=(Token)input.LT(1);
            match(input,SELECT_LAST,FOLLOW_SELECT_LAST_in_lastSelection1125); if (failed) return retval;
            if ( backtracking==0 ) {
            SELECT_LAST102_tree = (Object)adaptor.create(SELECT_LAST102);
            root_0 = (Object)adaptor.becomeRoot(SELECT_LAST102_tree, root_0);
            }
            pushFollow(FOLLOW_expression_in_lastSelection1128);
            expression103=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression103.getTree());
            RCURLY104=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_lastSelection1130); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:194:1: type : TYPE qualifiedId RPAREN -> ^( TYPEREF qualifiedId ) ;
    public final type_return type() throws RecognitionException {
        type_return retval = new type_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token TYPE105=null;
        Token RPAREN107=null;
        qualifiedId_return qualifiedId106 = null;


        Object TYPE105_tree=null;
        Object RPAREN107_tree=null;
        RewriteRuleTokenStream stream_RPAREN=new RewriteRuleTokenStream(adaptor,"token RPAREN");
        RewriteRuleTokenStream stream_TYPE=new RewriteRuleTokenStream(adaptor,"token TYPE");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:194:5: ( TYPE qualifiedId RPAREN -> ^( TYPEREF qualifiedId ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:194:7: TYPE qualifiedId RPAREN
            {
            TYPE105=(Token)input.LT(1);
            match(input,TYPE,FOLLOW_TYPE_in_type1139); if (failed) return retval;
            if ( backtracking==0 ) stream_TYPE.add(TYPE105);

            pushFollow(FOLLOW_qualifiedId_in_type1141);
            qualifiedId106=qualifiedId();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_qualifiedId.add(qualifiedId106.getTree());
            RPAREN107=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_type1143); if (failed) return retval;
            if ( backtracking==0 ) stream_RPAREN.add(RPAREN107);


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
            // 194:31: -> ^( TYPEREF qualifiedId )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:194:34: ^( TYPEREF qualifiedId )
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:202:1: lambda : LAMBDA ( argList )? PIPE expression RCURLY -> ^( LAMBDA ( argList )? expression ) ;
    public final lambda_return lambda() throws RecognitionException {
        lambda_return retval = new lambda_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LAMBDA108=null;
        Token PIPE110=null;
        Token RCURLY112=null;
        argList_return argList109 = null;

        expression_return expression111 = null;


        Object LAMBDA108_tree=null;
        Object PIPE110_tree=null;
        Object RCURLY112_tree=null;
        RewriteRuleTokenStream stream_RCURLY=new RewriteRuleTokenStream(adaptor,"token RCURLY");
        RewriteRuleTokenStream stream_PIPE=new RewriteRuleTokenStream(adaptor,"token PIPE");
        RewriteRuleTokenStream stream_LAMBDA=new RewriteRuleTokenStream(adaptor,"token LAMBDA");
        RewriteRuleSubtreeStream stream_argList=new RewriteRuleSubtreeStream(adaptor,"rule argList");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:203:4: ( LAMBDA ( argList )? PIPE expression RCURLY -> ^( LAMBDA ( argList )? expression ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:203:8: LAMBDA ( argList )? PIPE expression RCURLY
            {
            LAMBDA108=(Token)input.LT(1);
            match(input,LAMBDA,FOLLOW_LAMBDA_in_lambda1170); if (failed) return retval;
            if ( backtracking==0 ) stream_LAMBDA.add(LAMBDA108);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:203:15: ( argList )?
            int alt25=2;
            int LA25_0 = input.LA(1);

            if ( (LA25_0==ID) ) {
                alt25=1;
            }
            switch (alt25) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:203:16: argList
                    {
                    pushFollow(FOLLOW_argList_in_lambda1173);
                    argList109=argList();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_argList.add(argList109.getTree());

                    }
                    break;

            }

            PIPE110=(Token)input.LT(1);
            match(input,PIPE,FOLLOW_PIPE_in_lambda1177); if (failed) return retval;
            if ( backtracking==0 ) stream_PIPE.add(PIPE110);

            pushFollow(FOLLOW_expression_in_lambda1179);
            expression111=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression111.getTree());
            RCURLY112=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_lambda1181); if (failed) return retval;
            if ( backtracking==0 ) stream_RCURLY.add(RCURLY112);


            // AST REWRITE
            // elements: LAMBDA, argList, expression
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 203:49: -> ^( LAMBDA ( argList )? expression )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:203:52: ^( LAMBDA ( argList )? expression )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(stream_LAMBDA.next(), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:203:61: ( argList )?
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:205:1: argList : (id+= ID ( COMMA id+= ID )* ) -> ^( ARGLIST ( $id)* ) ;
    public final argList_return argList() throws RecognitionException {
        argList_return retval = new argList_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token COMMA113=null;
        Token id=null;
        List list_id=null;

        Object COMMA113_tree=null;
        Object id_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:205:9: ( (id+= ID ( COMMA id+= ID )* ) -> ^( ARGLIST ( $id)* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:205:11: (id+= ID ( COMMA id+= ID )* )
            {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:205:11: (id+= ID ( COMMA id+= ID )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:205:12: id+= ID ( COMMA id+= ID )*
            {
            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_argList1205); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(id);

            if (list_id==null) list_id=new ArrayList();
            list_id.add(id);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:205:19: ( COMMA id+= ID )*
            loop26:
            do {
                int alt26=2;
                int LA26_0 = input.LA(1);

                if ( (LA26_0==COMMA) ) {
                    alt26=1;
                }


                switch (alt26) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:205:20: COMMA id+= ID
            	    {
            	    COMMA113=(Token)input.LT(1);
            	    match(input,COMMA,FOLLOW_COMMA_in_argList1208); if (failed) return retval;
            	    if ( backtracking==0 ) stream_COMMA.add(COMMA113);

            	    id=(Token)input.LT(1);
            	    match(input,ID,FOLLOW_ID_in_argList1212); if (failed) return retval;
            	    if ( backtracking==0 ) stream_ID.add(id);

            	    if (list_id==null) list_id=new ArrayList();
            	    list_id.add(id);


            	    }
            	    break;

            	default :
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
            if ( backtracking==0 ) {
            retval.tree = root_0;
            RewriteRuleTokenStream stream_id=new RewriteRuleTokenStream(adaptor,"token id", list_id);
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 205:36: -> ^( ARGLIST ( $id)* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:205:39: ^( ARGLIST ( $id)* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(ARGLIST, "ARGLIST"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:205:49: ( $id)*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:207:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );
    public final constructor_return constructor() throws RecognitionException {
        constructor_return retval = new constructor_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal114=null;
        qualifiedId_return qualifiedId115 = null;

        ctorArgs_return ctorArgs116 = null;

        arrayConstructor_return arrayConstructor117 = null;


        Object string_literal114_tree=null;
        RewriteRuleTokenStream stream_90=new RewriteRuleTokenStream(adaptor,"token 90");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        RewriteRuleSubtreeStream stream_ctorArgs=new RewriteRuleSubtreeStream(adaptor,"rule ctorArgs");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:208:2: ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor )
            int alt27=2;
            int LA27_0 = input.LA(1);

            if ( (LA27_0==90) ) {
                int LA27_1 = input.LA(2);

                if ( (LA27_1==ID) ) {
                    int LA27_2 = input.LA(3);

                    if ( (synpred4()) ) {
                        alt27=1;
                    }
                    else if ( (true) ) {
                        alt27=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("207:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );", 27, 2, input);

                        throw nvae;
                    }
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("207:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );", 27, 1, input);

                    throw nvae;
                }
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("207:1: constructor : ( ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs -> ^( CONSTRUCTOR qualifiedId ctorArgs ) | arrayConstructor );", 27, 0, input);

                throw nvae;
            }
            switch (alt27) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:208:4: ( 'new' qualifiedId LPAREN )=> 'new' qualifiedId ctorArgs
                    {
                    string_literal114=(Token)input.LT(1);
                    match(input,90,FOLLOW_90_in_constructor1248); if (failed) return retval;
                    if ( backtracking==0 ) stream_90.add(string_literal114);

                    pushFollow(FOLLOW_qualifiedId_in_constructor1250);
                    qualifiedId115=qualifiedId();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_qualifiedId.add(qualifiedId115.getTree());
                    pushFollow(FOLLOW_ctorArgs_in_constructor1252);
                    ctorArgs116=ctorArgs();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_ctorArgs.add(ctorArgs116.getTree());

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
                    // 208:61: -> ^( CONSTRUCTOR qualifiedId ctorArgs )
                    {
                        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:208:64: ^( CONSTRUCTOR qualifiedId ctorArgs )
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
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:209:6: arrayConstructor
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_arrayConstructor_in_constructor1269);
                    arrayConstructor117=arrayConstructor();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, arrayConstructor117.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:212:1: arrayConstructor : 'new' qualifiedId arrayRank ( listInitializer )? -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? ) ;
    public final arrayConstructor_return arrayConstructor() throws RecognitionException {
        arrayConstructor_return retval = new arrayConstructor_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal118=null;
        qualifiedId_return qualifiedId119 = null;

        arrayRank_return arrayRank120 = null;

        listInitializer_return listInitializer121 = null;


        Object string_literal118_tree=null;
        RewriteRuleTokenStream stream_90=new RewriteRuleTokenStream(adaptor,"token 90");
        RewriteRuleSubtreeStream stream_listInitializer=new RewriteRuleSubtreeStream(adaptor,"rule listInitializer");
        RewriteRuleSubtreeStream stream_qualifiedId=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedId");
        RewriteRuleSubtreeStream stream_arrayRank=new RewriteRuleSubtreeStream(adaptor,"rule arrayRank");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:213:2: ( 'new' qualifiedId arrayRank ( listInitializer )? -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:213:4: 'new' qualifiedId arrayRank ( listInitializer )?
            {
            string_literal118=(Token)input.LT(1);
            match(input,90,FOLLOW_90_in_arrayConstructor1280); if (failed) return retval;
            if ( backtracking==0 ) stream_90.add(string_literal118);

            pushFollow(FOLLOW_qualifiedId_in_arrayConstructor1282);
            qualifiedId119=qualifiedId();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_qualifiedId.add(qualifiedId119.getTree());
            pushFollow(FOLLOW_arrayRank_in_arrayConstructor1284);
            arrayRank120=arrayRank();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_arrayRank.add(arrayRank120.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:213:32: ( listInitializer )?
            int alt28=2;
            int LA28_0 = input.LA(1);

            if ( (LA28_0==LCURLY) ) {
                alt28=1;
            }
            switch (alt28) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:213:33: listInitializer
                    {
                    pushFollow(FOLLOW_listInitializer_in_arrayConstructor1287);
                    listInitializer121=listInitializer();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_listInitializer.add(listInitializer121.getTree());

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
            // 214:4: -> ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:214:7: ^( CONSTRUCTOR_ARRAY qualifiedId arrayRank ( listInitializer )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(CONSTRUCTOR_ARRAY, "CONSTRUCTOR_ARRAY"), root_1);

                adaptor.addChild(root_1, stream_qualifiedId.next());
                adaptor.addChild(root_1, stream_arrayRank.next());
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:214:49: ( listInitializer )?
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:217:1: arrayRank : LBRACKET ( expression ( COMMA expression )* )? RBRACKET -> ^( EXPRESSIONLIST ( expression )* ) ;
    public final arrayRank_return arrayRank() throws RecognitionException {
        arrayRank_return retval = new arrayRank_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LBRACKET122=null;
        Token COMMA124=null;
        Token RBRACKET126=null;
        expression_return expression123 = null;

        expression_return expression125 = null;


        Object LBRACKET122_tree=null;
        Object COMMA124_tree=null;
        Object RBRACKET126_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_LBRACKET=new RewriteRuleTokenStream(adaptor,"token LBRACKET");
        RewriteRuleTokenStream stream_RBRACKET=new RewriteRuleTokenStream(adaptor,"token RBRACKET");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:5: ( LBRACKET ( expression ( COMMA expression )* )? RBRACKET -> ^( EXPRESSIONLIST ( expression )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:7: LBRACKET ( expression ( COMMA expression )* )? RBRACKET
            {
            LBRACKET122=(Token)input.LT(1);
            match(input,LBRACKET,FOLLOW_LBRACKET_in_arrayRank1322); if (failed) return retval;
            if ( backtracking==0 ) stream_LBRACKET.add(LBRACKET122);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:16: ( expression ( COMMA expression )* )?
            int alt30=2;
            int LA30_0 = input.LA(1);

            if ( (LA30_0==INTEGER_LITERAL||LA30_0==LPAREN||(LA30_0>=PLUS && LA30_0<=MINUS)||LA30_0==BANG||(LA30_0>=POUND && LA30_0<=DOLLAR)||(LA30_0>=AT && LA30_0<=LBRACKET)||LA30_0==PROJECT||(LA30_0>=SELECT && LA30_0<=LAMBDA)||(LA30_0>=LCURLY && LA30_0<=FALSE)||LA30_0==90) ) {
                alt30=1;
            }
            switch (alt30) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:17: expression ( COMMA expression )*
                    {
                    pushFollow(FOLLOW_expression_in_arrayRank1325);
                    expression123=expression();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) stream_expression.add(expression123.getTree());
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:28: ( COMMA expression )*
                    loop29:
                    do {
                        int alt29=2;
                        int LA29_0 = input.LA(1);

                        if ( (LA29_0==COMMA) ) {
                            alt29=1;
                        }


                        switch (alt29) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:29: COMMA expression
                    	    {
                    	    COMMA124=(Token)input.LT(1);
                    	    match(input,COMMA,FOLLOW_COMMA_in_arrayRank1328); if (failed) return retval;
                    	    if ( backtracking==0 ) stream_COMMA.add(COMMA124);

                    	    pushFollow(FOLLOW_expression_in_arrayRank1330);
                    	    expression125=expression();
                    	    _fsp--;
                    	    if (failed) return retval;
                    	    if ( backtracking==0 ) stream_expression.add(expression125.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop29;
                        }
                    } while (true);


                    }
                    break;

            }

            RBRACKET126=(Token)input.LT(1);
            match(input,RBRACKET,FOLLOW_RBRACKET_in_arrayRank1336); if (failed) return retval;
            if ( backtracking==0 ) stream_RBRACKET.add(RBRACKET126);


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
            // 218:59: -> ^( EXPRESSIONLIST ( expression )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:62: ^( EXPRESSIONLIST ( expression )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(EXPRESSIONLIST, "EXPRESSIONLIST"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:218:79: ( expression )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:220:1: listInitializer : LCURLY expression ( COMMA expression )* RCURLY -> ^( LIST_INITIALIZER ( expression )* ) ;
    public final listInitializer_return listInitializer() throws RecognitionException {
        listInitializer_return retval = new listInitializer_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LCURLY127=null;
        Token COMMA129=null;
        Token RCURLY131=null;
        expression_return expression128 = null;

        expression_return expression130 = null;


        Object LCURLY127_tree=null;
        Object COMMA129_tree=null;
        Object RCURLY131_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_RCURLY=new RewriteRuleTokenStream(adaptor,"token RCURLY");
        RewriteRuleTokenStream stream_LCURLY=new RewriteRuleTokenStream(adaptor,"token LCURLY");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:221:5: ( LCURLY expression ( COMMA expression )* RCURLY -> ^( LIST_INITIALIZER ( expression )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:221:7: LCURLY expression ( COMMA expression )* RCURLY
            {
            LCURLY127=(Token)input.LT(1);
            match(input,LCURLY,FOLLOW_LCURLY_in_listInitializer1361); if (failed) return retval;
            if ( backtracking==0 ) stream_LCURLY.add(LCURLY127);

            pushFollow(FOLLOW_expression_in_listInitializer1363);
            expression128=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression128.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:221:25: ( COMMA expression )*
            loop31:
            do {
                int alt31=2;
                int LA31_0 = input.LA(1);

                if ( (LA31_0==COMMA) ) {
                    alt31=1;
                }


                switch (alt31) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:221:26: COMMA expression
            	    {
            	    COMMA129=(Token)input.LT(1);
            	    match(input,COMMA,FOLLOW_COMMA_in_listInitializer1366); if (failed) return retval;
            	    if ( backtracking==0 ) stream_COMMA.add(COMMA129);

            	    pushFollow(FOLLOW_expression_in_listInitializer1368);
            	    expression130=expression();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) stream_expression.add(expression130.getTree());

            	    }
            	    break;

            	default :
            	    break loop31;
                }
            } while (true);

            RCURLY131=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_listInitializer1372); if (failed) return retval;
            if ( backtracking==0 ) stream_RCURLY.add(RCURLY131);


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
            // 221:52: -> ^( LIST_INITIALIZER ( expression )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:221:55: ^( LIST_INITIALIZER ( expression )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(LIST_INITIALIZER, "LIST_INITIALIZER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:221:74: ( expression )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:226:1: mapInitializer : POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY -> ^( MAP_INITIALIZER ( mapEntry )* ) ;
    public final mapInitializer_return mapInitializer() throws RecognitionException {
        mapInitializer_return retval = new mapInitializer_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token POUND132=null;
        Token LCURLY133=null;
        Token COMMA135=null;
        Token RCURLY137=null;
        mapEntry_return mapEntry134 = null;

        mapEntry_return mapEntry136 = null;


        Object POUND132_tree=null;
        Object LCURLY133_tree=null;
        Object COMMA135_tree=null;
        Object RCURLY137_tree=null;
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleTokenStream stream_RCURLY=new RewriteRuleTokenStream(adaptor,"token RCURLY");
        RewriteRuleTokenStream stream_LCURLY=new RewriteRuleTokenStream(adaptor,"token LCURLY");
        RewriteRuleTokenStream stream_POUND=new RewriteRuleTokenStream(adaptor,"token POUND");
        RewriteRuleSubtreeStream stream_mapEntry=new RewriteRuleSubtreeStream(adaptor,"rule mapEntry");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:227:5: ( POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY -> ^( MAP_INITIALIZER ( mapEntry )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:227:7: POUND LCURLY mapEntry ( COMMA mapEntry )* RCURLY
            {
            POUND132=(Token)input.LT(1);
            match(input,POUND,FOLLOW_POUND_in_mapInitializer1400); if (failed) return retval;
            if ( backtracking==0 ) stream_POUND.add(POUND132);

            LCURLY133=(Token)input.LT(1);
            match(input,LCURLY,FOLLOW_LCURLY_in_mapInitializer1402); if (failed) return retval;
            if ( backtracking==0 ) stream_LCURLY.add(LCURLY133);

            pushFollow(FOLLOW_mapEntry_in_mapInitializer1404);
            mapEntry134=mapEntry();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_mapEntry.add(mapEntry134.getTree());
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:227:29: ( COMMA mapEntry )*
            loop32:
            do {
                int alt32=2;
                int LA32_0 = input.LA(1);

                if ( (LA32_0==COMMA) ) {
                    alt32=1;
                }


                switch (alt32) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:227:30: COMMA mapEntry
            	    {
            	    COMMA135=(Token)input.LT(1);
            	    match(input,COMMA,FOLLOW_COMMA_in_mapInitializer1407); if (failed) return retval;
            	    if ( backtracking==0 ) stream_COMMA.add(COMMA135);

            	    pushFollow(FOLLOW_mapEntry_in_mapInitializer1409);
            	    mapEntry136=mapEntry();
            	    _fsp--;
            	    if (failed) return retval;
            	    if ( backtracking==0 ) stream_mapEntry.add(mapEntry136.getTree());

            	    }
            	    break;

            	default :
            	    break loop32;
                }
            } while (true);

            RCURLY137=(Token)input.LT(1);
            match(input,RCURLY,FOLLOW_RCURLY_in_mapInitializer1413); if (failed) return retval;
            if ( backtracking==0 ) stream_RCURLY.add(RCURLY137);


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
            // 227:54: -> ^( MAP_INITIALIZER ( mapEntry )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:227:57: ^( MAP_INITIALIZER ( mapEntry )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(MAP_INITIALIZER, "MAP_INITIALIZER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:227:75: ( mapEntry )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:229:1: mapEntry : expression COLON expression -> ^( MAP_ENTRY ( expression )* ) ;
    public final mapEntry_return mapEntry() throws RecognitionException {
        mapEntry_return retval = new mapEntry_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token COLON139=null;
        expression_return expression138 = null;

        expression_return expression140 = null;


        Object COLON139_tree=null;
        RewriteRuleTokenStream stream_COLON=new RewriteRuleTokenStream(adaptor,"token COLON");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:230:5: ( expression COLON expression -> ^( MAP_ENTRY ( expression )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:230:7: expression COLON expression
            {
            pushFollow(FOLLOW_expression_in_mapEntry1434);
            expression138=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression138.getTree());
            COLON139=(Token)input.LT(1);
            match(input,COLON,FOLLOW_COLON_in_mapEntry1436); if (failed) return retval;
            if ( backtracking==0 ) stream_COLON.add(COLON139);

            pushFollow(FOLLOW_expression_in_mapEntry1438);
            expression140=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) stream_expression.add(expression140.getTree());

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
            // 230:35: -> ^( MAP_ENTRY ( expression )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:230:38: ^( MAP_ENTRY ( expression )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(MAP_ENTRY, "MAP_ENTRY"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:230:50: ( expression )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:232:1: ctorArgs : LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN ;
    public final ctorArgs_return ctorArgs() throws RecognitionException {
        ctorArgs_return retval = new ctorArgs_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LPAREN141=null;
        Token COMMA143=null;
        Token RPAREN145=null;
        namedArgument_return namedArgument142 = null;

        namedArgument_return namedArgument144 = null;


        Object LPAREN141_tree=null;
        Object COMMA143_tree=null;
        Object RPAREN145_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:233:2: ( LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:233:4: LPAREN ( namedArgument ( COMMA namedArgument )* )? RPAREN
            {
            root_0 = (Object)adaptor.nil();

            LPAREN141=(Token)input.LT(1);
            match(input,LPAREN,FOLLOW_LPAREN_in_ctorArgs1456); if (failed) return retval;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:233:12: ( namedArgument ( COMMA namedArgument )* )?
            int alt34=2;
            int LA34_0 = input.LA(1);

            if ( (LA34_0==INTEGER_LITERAL||LA34_0==LPAREN||(LA34_0>=PLUS && LA34_0<=MINUS)||LA34_0==BANG||(LA34_0>=POUND && LA34_0<=DOLLAR)||(LA34_0>=AT && LA34_0<=LBRACKET)||LA34_0==PROJECT||(LA34_0>=SELECT && LA34_0<=LAMBDA)||(LA34_0>=LCURLY && LA34_0<=FALSE)||LA34_0==90) ) {
                alt34=1;
            }
            switch (alt34) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:233:13: namedArgument ( COMMA namedArgument )*
                    {
                    pushFollow(FOLLOW_namedArgument_in_ctorArgs1460);
                    namedArgument142=namedArgument();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, namedArgument142.getTree());
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:233:27: ( COMMA namedArgument )*
                    loop33:
                    do {
                        int alt33=2;
                        int LA33_0 = input.LA(1);

                        if ( (LA33_0==COMMA) ) {
                            alt33=1;
                        }


                        switch (alt33) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:233:28: COMMA namedArgument
                    	    {
                    	    COMMA143=(Token)input.LT(1);
                    	    match(input,COMMA,FOLLOW_COMMA_in_ctorArgs1463); if (failed) return retval;
                    	    pushFollow(FOLLOW_namedArgument_in_ctorArgs1466);
                    	    namedArgument144=namedArgument();
                    	    _fsp--;
                    	    if (failed) return retval;
                    	    if ( backtracking==0 ) adaptor.addChild(root_0, namedArgument144.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop33;
                        }
                    } while (true);


                    }
                    break;

            }

            RPAREN145=(Token)input.LT(1);
            match(input,RPAREN,FOLLOW_RPAREN_in_ctorArgs1472); if (failed) return retval;

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:235:1: argument : expression ;
    public final argument_return argument() throws RecognitionException {
        argument_return retval = new argument_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        expression_return expression146 = null;



        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:235:10: ( expression )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:235:12: expression
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_expression_in_argument1481);
            expression146=expression();
            _fsp--;
            if (failed) return retval;
            if ( backtracking==0 ) adaptor.addChild(root_0, expression146.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:237:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );
    public final namedArgument_return namedArgument() throws RecognitionException {
        namedArgument_return retval = new namedArgument_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token ASSIGN147=null;
        expression_return expression148 = null;

        argument_return argument149 = null;


        Object id_tree=null;
        Object ASSIGN147_tree=null;
        RewriteRuleTokenStream stream_ASSIGN=new RewriteRuleTokenStream(adaptor,"token ASSIGN");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:238:5: ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument )
            int alt35=2;
            int LA35_0 = input.LA(1);

            if ( (LA35_0==ID) ) {
                int LA35_1 = input.LA(2);

                if ( (LA35_1==ASSIGN) ) {
                    int LA35_25 = input.LA(3);

                    if ( (synpred5()) ) {
                        alt35=1;
                    }
                    else if ( (true) ) {
                        alt35=2;
                    }
                    else {
                        if (backtracking>0) {failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("237:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );", 35, 25, input);

                        throw nvae;
                    }
                }
                else if ( ((LA35_1>=DEFAULT && LA35_1<=QMARK)||(LA35_1>=LPAREN && LA35_1<=POWER)||LA35_1==DOT||LA35_1==COMMA||LA35_1==LBRACKET||(LA35_1>=EQUAL && LA35_1<=MATCHES)) ) {
                    alt35=2;
                }
                else {
                    if (backtracking>0) {failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("237:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );", 35, 1, input);

                    throw nvae;
                }
            }
            else if ( (LA35_0==INTEGER_LITERAL||LA35_0==LPAREN||(LA35_0>=PLUS && LA35_0<=MINUS)||LA35_0==BANG||LA35_0==POUND||LA35_0==DOLLAR||(LA35_0>=AT && LA35_0<=LBRACKET)||LA35_0==PROJECT||(LA35_0>=SELECT && LA35_0<=LAMBDA)||(LA35_0>=LCURLY && LA35_0<=FALSE)||LA35_0==90) ) {
                alt35=2;
            }
            else {
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("237:1: namedArgument : ( ( ID ASSIGN )=>id= ID ASSIGN expression -> ^( NAMED_ARGUMENT[$id] expression ) | argument );", 35, 0, input);

                throw nvae;
            }
            switch (alt35) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:238:7: ( ID ASSIGN )=>id= ID ASSIGN expression
                    {
                    id=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_namedArgument1504); if (failed) return retval;
                    if ( backtracking==0 ) stream_ID.add(id);

                    ASSIGN147=(Token)input.LT(1);
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_namedArgument1506); if (failed) return retval;
                    if ( backtracking==0 ) stream_ASSIGN.add(ASSIGN147);

                    pushFollow(FOLLOW_expression_in_namedArgument1508);
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
                    // 239:19: -> ^( NAMED_ARGUMENT[$id] expression )
                    {
                        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:239:22: ^( NAMED_ARGUMENT[$id] expression )
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
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:240:7: argument
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_argument_in_namedArgument1544);
                    argument149=argument();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, argument149.getTree());

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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:242:1: qualifiedId : ID ( DOT ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) ;
    public final qualifiedId_return qualifiedId() throws RecognitionException {
        qualifiedId_return retval = new qualifiedId_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID150=null;
        Token DOT151=null;
        Token ID152=null;

        Object ID150_tree=null;
        Object DOT151_tree=null;
        Object ID152_tree=null;
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");
        RewriteRuleTokenStream stream_DOT=new RewriteRuleTokenStream(adaptor,"token DOT");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:242:13: ( ID ( DOT ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:242:15: ID ( DOT ID )*
            {
            ID150=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_qualifiedId1556); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(ID150);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:242:18: ( DOT ID )*
            loop36:
            do {
                int alt36=2;
                int LA36_0 = input.LA(1);

                if ( (LA36_0==DOT) ) {
                    alt36=1;
                }


                switch (alt36) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:242:19: DOT ID
            	    {
            	    DOT151=(Token)input.LT(1);
            	    match(input,DOT,FOLLOW_DOT_in_qualifiedId1559); if (failed) return retval;
            	    if ( backtracking==0 ) stream_DOT.add(DOT151);

            	    ID152=(Token)input.LT(1);
            	    match(input,ID,FOLLOW_ID_in_qualifiedId1561); if (failed) return retval;
            	    if ( backtracking==0 ) stream_ID.add(ID152);


            	    }
            	    break;

            	default :
            	    break loop36;
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
            // 242:28: -> ^( QUALIFIED_IDENTIFIER ( ID )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:242:31: ^( QUALIFIED_IDENTIFIER ( ID )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(QUALIFIED_IDENTIFIER, "QUALIFIED_IDENTIFIER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:242:54: ( ID )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:244:1: contextName : ID ( DIV ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) ;
    public final contextName_return contextName() throws RecognitionException {
        contextName_return retval = new contextName_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID153=null;
        Token DIV154=null;
        Token ID155=null;

        Object ID153_tree=null;
        Object DIV154_tree=null;
        Object ID155_tree=null;
        RewriteRuleTokenStream stream_DIV=new RewriteRuleTokenStream(adaptor,"token DIV");
        RewriteRuleTokenStream stream_ID=new RewriteRuleTokenStream(adaptor,"token ID");

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:244:13: ( ID ( DIV ID )* -> ^( QUALIFIED_IDENTIFIER ( ID )* ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:244:15: ID ( DIV ID )*
            {
            ID153=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_contextName1580); if (failed) return retval;
            if ( backtracking==0 ) stream_ID.add(ID153);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:244:18: ( DIV ID )*
            loop37:
            do {
                int alt37=2;
                int LA37_0 = input.LA(1);

                if ( (LA37_0==DIV) ) {
                    alt37=1;
                }


                switch (alt37) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:244:19: DIV ID
            	    {
            	    DIV154=(Token)input.LT(1);
            	    match(input,DIV,FOLLOW_DIV_in_contextName1583); if (failed) return retval;
            	    if ( backtracking==0 ) stream_DIV.add(DIV154);

            	    ID155=(Token)input.LT(1);
            	    match(input,ID,FOLLOW_ID_in_contextName1585); if (failed) return retval;
            	    if ( backtracking==0 ) stream_ID.add(ID155);


            	    }
            	    break;

            	default :
            	    break loop37;
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
            // 244:28: -> ^( QUALIFIED_IDENTIFIER ( ID )* )
            {
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:244:31: ^( QUALIFIED_IDENTIFIER ( ID )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(QUALIFIED_IDENTIFIER, "QUALIFIED_IDENTIFIER"), root_1);

                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:244:54: ( ID )*
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:246:1: literal : ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL );
    public final literal_return literal() throws RecognitionException {
        literal_return retval = new literal_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token INTEGER_LITERAL156=null;
        Token STRING_LITERAL157=null;
        Token DQ_STRING_LITERAL158=null;
        Token NULL_LITERAL160=null;
        Token HEXADECIMAL_INTEGER_LITERAL161=null;
        Token REAL_LITERAL162=null;
        boolLiteral_return boolLiteral159 = null;


        Object INTEGER_LITERAL156_tree=null;
        Object STRING_LITERAL157_tree=null;
        Object DQ_STRING_LITERAL158_tree=null;
        Object NULL_LITERAL160_tree=null;
        Object HEXADECIMAL_INTEGER_LITERAL161_tree=null;
        Object REAL_LITERAL162_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:247:2: ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL )
            int alt38=7;
            switch ( input.LA(1) ) {
            case INTEGER_LITERAL:
                {
                alt38=1;
                }
                break;
            case STRING_LITERAL:
                {
                alt38=2;
                }
                break;
            case DQ_STRING_LITERAL:
                {
                alt38=3;
                }
                break;
            case TRUE:
            case FALSE:
                {
                alt38=4;
                }
                break;
            case NULL_LITERAL:
                {
                alt38=5;
                }
                break;
            case HEXADECIMAL_INTEGER_LITERAL:
                {
                alt38=6;
                }
                break;
            case REAL_LITERAL:
                {
                alt38=7;
                }
                break;
            default:
                if (backtracking>0) {failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("246:1: literal : ( INTEGER_LITERAL | STRING_LITERAL | DQ_STRING_LITERAL | boolLiteral | NULL_LITERAL | HEXADECIMAL_INTEGER_LITERAL | REAL_LITERAL );", 38, 0, input);

                throw nvae;
            }

            switch (alt38) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:247:4: INTEGER_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    INTEGER_LITERAL156=(Token)input.LT(1);
                    match(input,INTEGER_LITERAL,FOLLOW_INTEGER_LITERAL_in_literal1606); if (failed) return retval;
                    if ( backtracking==0 ) {
                    INTEGER_LITERAL156_tree = (Object)adaptor.create(INTEGER_LITERAL156);
                    adaptor.addChild(root_0, INTEGER_LITERAL156_tree);
                    }

                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:248:4: STRING_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    STRING_LITERAL157=(Token)input.LT(1);
                    match(input,STRING_LITERAL,FOLLOW_STRING_LITERAL_in_literal1612); if (failed) return retval;
                    if ( backtracking==0 ) {
                    STRING_LITERAL157_tree = (Object)adaptor.create(STRING_LITERAL157);
                    adaptor.addChild(root_0, STRING_LITERAL157_tree);
                    }

                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:249:4: DQ_STRING_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    DQ_STRING_LITERAL158=(Token)input.LT(1);
                    match(input,DQ_STRING_LITERAL,FOLLOW_DQ_STRING_LITERAL_in_literal1617); if (failed) return retval;
                    if ( backtracking==0 ) {
                    DQ_STRING_LITERAL158_tree = (Object)adaptor.create(DQ_STRING_LITERAL158);
                    adaptor.addChild(root_0, DQ_STRING_LITERAL158_tree);
                    }

                    }
                    break;
                case 4 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:250:4: boolLiteral
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_boolLiteral_in_literal1622);
                    boolLiteral159=boolLiteral();
                    _fsp--;
                    if (failed) return retval;
                    if ( backtracking==0 ) adaptor.addChild(root_0, boolLiteral159.getTree());

                    }
                    break;
                case 5 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:251:4: NULL_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    NULL_LITERAL160=(Token)input.LT(1);
                    match(input,NULL_LITERAL,FOLLOW_NULL_LITERAL_in_literal1627); if (failed) return retval;
                    if ( backtracking==0 ) {
                    NULL_LITERAL160_tree = (Object)adaptor.create(NULL_LITERAL160);
                    adaptor.addChild(root_0, NULL_LITERAL160_tree);
                    }

                    }
                    break;
                case 6 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:252:4: HEXADECIMAL_INTEGER_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    HEXADECIMAL_INTEGER_LITERAL161=(Token)input.LT(1);
                    match(input,HEXADECIMAL_INTEGER_LITERAL,FOLLOW_HEXADECIMAL_INTEGER_LITERAL_in_literal1632); if (failed) return retval;
                    if ( backtracking==0 ) {
                    HEXADECIMAL_INTEGER_LITERAL161_tree = (Object)adaptor.create(HEXADECIMAL_INTEGER_LITERAL161);
                    adaptor.addChild(root_0, HEXADECIMAL_INTEGER_LITERAL161_tree);
                    }

                    }
                    break;
                case 7 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:253:4: REAL_LITERAL
                    {
                    root_0 = (Object)adaptor.nil();

                    REAL_LITERAL162=(Token)input.LT(1);
                    match(input,REAL_LITERAL,FOLLOW_REAL_LITERAL_in_literal1638); if (failed) return retval;
                    if ( backtracking==0 ) {
                    REAL_LITERAL162_tree = (Object)adaptor.create(REAL_LITERAL162);
                    adaptor.addChild(root_0, REAL_LITERAL162_tree);
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:256:1: boolLiteral : ( TRUE | FALSE );
    public final boolLiteral_return boolLiteral() throws RecognitionException {
        boolLiteral_return retval = new boolLiteral_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set163=null;

        Object set163_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:256:12: ( TRUE | FALSE )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:
            {
            root_0 = (Object)adaptor.nil();

            set163=(Token)input.LT(1);
            if ( (input.LA(1)>=TRUE && input.LA(1)<=FALSE) ) {
                input.consume();
                if ( backtracking==0 ) adaptor.addChild(root_0, adaptor.create(set163));
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
    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:263:1: relationalOperator : ( EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL | IN | IS | BETWEEN | MATCHES );
    public final relationalOperator_return relationalOperator() throws RecognitionException {
        relationalOperator_return retval = new relationalOperator_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set164=null;

        Object set164_tree=null;

        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:264:5: ( EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL | IN | IS | BETWEEN | MATCHES )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:
            {
            root_0 = (Object)adaptor.nil();

            set164=(Token)input.LT(1);
            if ( (input.LA(1)>=EQUAL && input.LA(1)<=MATCHES) ) {
                input.consume();
                if ( backtracking==0 ) adaptor.addChild(root_0, adaptor.create(set164));
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
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:7: ( POUND ID LPAREN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:136:8: POUND ID LPAREN
        {
        match(input,POUND,FOLLOW_POUND_in_synpred1741); if (failed) return ;
        match(input,ID,FOLLOW_ID_in_synpred1743); if (failed) return ;
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred1745); if (failed) return ;

        }
    }
    // $ANTLR end synpred1

    // $ANTLR start synpred2
    public final void synpred2_fragment() throws RecognitionException {   
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:145:4: ( DOLLAR ID LPAREN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:145:5: DOLLAR ID LPAREN
        {
        match(input,DOLLAR,FOLLOW_DOLLAR_in_synpred2824); if (failed) return ;
        match(input,ID,FOLLOW_ID_in_synpred2826); if (failed) return ;
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred2828); if (failed) return ;

        }
    }
    // $ANTLR end synpred2

    // $ANTLR start synpred3
    public final void synpred3_fragment() throws RecognitionException {   
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:153:4: ( ID LPAREN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:153:5: ID LPAREN
        {
        match(input,ID,FOLLOW_ID_in_synpred3890); if (failed) return ;
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred3892); if (failed) return ;

        }
    }
    // $ANTLR end synpred3

    // $ANTLR start synpred4
    public final void synpred4_fragment() throws RecognitionException {   
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:208:4: ( 'new' qualifiedId LPAREN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:208:5: 'new' qualifiedId LPAREN
        {
        match(input,90,FOLLOW_90_in_synpred41239); if (failed) return ;
        pushFollow(FOLLOW_qualifiedId_in_synpred41241);
        qualifiedId();
        _fsp--;
        if (failed) return ;
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred41243); if (failed) return ;

        }
    }
    // $ANTLR end synpred4

    // $ANTLR start synpred5
    public final void synpred5_fragment() throws RecognitionException {   
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:238:7: ( ID ASSIGN )
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:238:8: ID ASSIGN
        {
        match(input,ID,FOLLOW_ID_in_synpred51495); if (failed) return ;
        match(input,ASSIGN,FOLLOW_ASSIGN_in_synpred51497); if (failed) return ;

        }
    }
    // $ANTLR end synpred5

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


 

    public static final BitSet FOLLOW_expression_in_expr177 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_expr179 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_logicalOrExpression_in_expression208 = new BitSet(new long[]{0x00000001C0000002L});
    public static final BitSet FOLLOW_ASSIGN_in_expression217 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_logicalOrExpression_in_expression220 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DEFAULT_in_expression230 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_logicalOrExpression_in_expression233 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_QMARK_in_expression243 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_expression_in_expression246 = new BitSet(new long[]{0x0000000200000000L});
    public static final BitSet FOLLOW_COLON_in_expression248 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_expression_in_expression251 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_parenExpr262 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_expression_in_parenExpr265 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_RPAREN_in_parenExpr267 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_logicalAndExpression_in_logicalOrExpression280 = new BitSet(new long[]{0x0000001000000002L});
    public static final BitSet FOLLOW_OR_in_logicalOrExpression283 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_logicalAndExpression_in_logicalOrExpression286 = new BitSet(new long[]{0x0000001000000002L});
    public static final BitSet FOLLOW_relationalExpression_in_logicalAndExpression321 = new BitSet(new long[]{0x0000002000000002L});
    public static final BitSet FOLLOW_AND_in_logicalAndExpression324 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_relationalExpression_in_logicalAndExpression327 = new BitSet(new long[]{0x0000002000000002L});
    public static final BitSet FOLLOW_sumExpression_in_relationalExpression338 = new BitSet(new long[]{0x0000000000000002L,0x000000000003FF00L});
    public static final BitSet FOLLOW_relationalOperator_in_relationalExpression341 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_sumExpression_in_relationalExpression344 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_productExpression_in_sumExpression355 = new BitSet(new long[]{0x000000C000000002L});
    public static final BitSet FOLLOW_PLUS_in_sumExpression360 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_MINUS_in_sumExpression365 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_productExpression_in_sumExpression369 = new BitSet(new long[]{0x000000C000000002L});
    public static final BitSet FOLLOW_powerExpr_in_productExpression381 = new BitSet(new long[]{0x0000070000000002L});
    public static final BitSet FOLLOW_STAR_in_productExpression385 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_DIV_in_productExpression390 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_MOD_in_productExpression394 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_powerExpr_in_productExpression398 = new BitSet(new long[]{0x0000070000000002L});
    public static final BitSet FOLLOW_unaryExpression_in_powerExpr410 = new BitSet(new long[]{0x0000080000000002L});
    public static final BitSet FOLLOW_POWER_in_powerExpr413 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_unaryExpression_in_powerExpr416 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PLUS_in_unaryExpression430 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_MINUS_in_unaryExpression435 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_BANG_in_unaryExpression440 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_unaryExpression_in_unaryExpression444 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_primaryExpression_in_unaryExpression450 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_startNode_in_primaryExpression464 = new BitSet(new long[]{0x0008200000000002L});
    public static final BitSet FOLLOW_node_in_primaryExpression467 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_parenExpr_in_startNode500 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_methodOrProperty_in_startNode508 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_functionOrVar_in_startNode517 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_localFunctionOrVar_in_startNode525 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_reference_in_startNode533 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_indexer_in_startNode541 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_literal_in_startNode549 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_type_in_startNode557 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_constructor_in_startNode565 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_projection_in_startNode573 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selection_in_startNode582 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_firstSelection_in_startNode591 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lastSelection_in_startNode599 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_listInitializer_in_startNode607 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_mapInitializer_in_startNode615 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lambda_in_startNode623 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_node643 = new BitSet(new long[]{0x03A0C00000000000L});
    public static final BitSet FOLLOW_dottedNode_in_node645 = new BitSet(new long[]{0x0008200000000002L});
    public static final BitSet FOLLOW_nonDottedNode_in_node650 = new BitSet(new long[]{0x0008200000000002L});
    public static final BitSet FOLLOW_indexer_in_nonDottedNode662 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_methodOrProperty_in_dottedNode675 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_functionOrVar_in_dottedNode681 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_projection_in_dottedNode689 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selection_in_dottedNode698 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_firstSelection_in_dottedNode707 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lastSelection_in_dottedNode716 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_function_in_functionOrVar750 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_var_in_functionOrVar758 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_function775 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_ID_in_function779 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_methodArgs_in_function781 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_var802 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_ID_in_var806 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_localFunction_in_localFunctionOrVar833 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_localVar_in_localFunctionOrVar838 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOLLAR_in_localFunction848 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_ID_in_localFunction852 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_methodArgs_in_localFunction854 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOLLAR_in_localVar869 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_ID_in_localVar873 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_methodOrProperty899 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_methodArgs_in_methodOrProperty901 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_methodOrProperty915 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_methodArgs930 = new BitSet(new long[]{0xEFADD0CC00000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_argument_in_methodArgs934 = new BitSet(new long[]{0x0002000800000000L});
    public static final BitSet FOLLOW_COMMA_in_methodArgs937 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_argument_in_methodArgs940 = new BitSet(new long[]{0x0002000800000000L});
    public static final BitSet FOLLOW_COMMA_in_methodArgs945 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_RPAREN_in_methodArgs952 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_property965 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_AT_in_reference987 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_LPAREN_in_reference991 = new BitSet(new long[]{0x0000800800000000L});
    public static final BitSet FOLLOW_contextName_in_reference996 = new BitSet(new long[]{0x0000000200000000L});
    public static final BitSet FOLLOW_COLON_in_reference998 = new BitSet(new long[]{0x0000800800000000L});
    public static final BitSet FOLLOW_qualifiedId_in_reference1005 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_RPAREN_in_reference1009 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACKET_in_indexer1044 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_argument_in_indexer1048 = new BitSet(new long[]{0x0012000000000000L});
    public static final BitSet FOLLOW_COMMA_in_indexer1051 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_argument_in_indexer1055 = new BitSet(new long[]{0x0012000000000000L});
    public static final BitSet FOLLOW_RBRACKET_in_indexer1059 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PROJECT_in_projection1086 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_expression_in_projection1089 = new BitSet(new long[]{0x0040000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_projection1091 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELECT_in_selection1099 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_expression_in_selection1102 = new BitSet(new long[]{0x0040000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_selection1104 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELECT_FIRST_in_firstSelection1112 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_expression_in_firstSelection1115 = new BitSet(new long[]{0x0040000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_firstSelection1117 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELECT_LAST_in_lastSelection1125 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_expression_in_lastSelection1128 = new BitSet(new long[]{0x0040000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_lastSelection1130 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TYPE_in_type1139 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_type1141 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_RPAREN_in_type1143 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LAMBDA_in_lambda1170 = new BitSet(new long[]{0x1000800000000000L});
    public static final BitSet FOLLOW_argList_in_lambda1173 = new BitSet(new long[]{0x1000000000000000L});
    public static final BitSet FOLLOW_PIPE_in_lambda1177 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_expression_in_lambda1179 = new BitSet(new long[]{0x0040000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_lambda1181 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_argList1205 = new BitSet(new long[]{0x0002000000000002L});
    public static final BitSet FOLLOW_COMMA_in_argList1208 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_ID_in_argList1212 = new BitSet(new long[]{0x0002000000000002L});
    public static final BitSet FOLLOW_90_in_constructor1248 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_constructor1250 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_ctorArgs_in_constructor1252 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_arrayConstructor_in_constructor1269 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_90_in_arrayConstructor1280 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_arrayConstructor1282 = new BitSet(new long[]{0x0008000000000000L});
    public static final BitSet FOLLOW_arrayRank_in_arrayConstructor1284 = new BitSet(new long[]{0x2000000000000002L});
    public static final BitSet FOLLOW_listInitializer_in_arrayConstructor1287 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACKET_in_arrayRank1322 = new BitSet(new long[]{0xEFBDD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_expression_in_arrayRank1325 = new BitSet(new long[]{0x0012000000000000L});
    public static final BitSet FOLLOW_COMMA_in_arrayRank1328 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_expression_in_arrayRank1330 = new BitSet(new long[]{0x0012000000000000L});
    public static final BitSet FOLLOW_RBRACKET_in_arrayRank1336 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LCURLY_in_listInitializer1361 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_expression_in_listInitializer1363 = new BitSet(new long[]{0x0042000000000000L});
    public static final BitSet FOLLOW_COMMA_in_listInitializer1366 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_expression_in_listInitializer1368 = new BitSet(new long[]{0x0042000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_listInitializer1372 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_mapInitializer1400 = new BitSet(new long[]{0x2000000000000000L});
    public static final BitSet FOLLOW_LCURLY_in_mapInitializer1402 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_mapEntry_in_mapInitializer1404 = new BitSet(new long[]{0x0042000000000000L});
    public static final BitSet FOLLOW_COMMA_in_mapInitializer1407 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_mapEntry_in_mapInitializer1409 = new BitSet(new long[]{0x0042000000000000L});
    public static final BitSet FOLLOW_RCURLY_in_mapInitializer1413 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expression_in_mapEntry1434 = new BitSet(new long[]{0x0000000200000000L});
    public static final BitSet FOLLOW_COLON_in_mapEntry1436 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_expression_in_mapEntry1438 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_ctorArgs1456 = new BitSet(new long[]{0xEFADD0CC00000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_namedArgument_in_ctorArgs1460 = new BitSet(new long[]{0x0002000800000000L});
    public static final BitSet FOLLOW_COMMA_in_ctorArgs1463 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_namedArgument_in_ctorArgs1466 = new BitSet(new long[]{0x0002000800000000L});
    public static final BitSet FOLLOW_RPAREN_in_ctorArgs1472 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expression_in_argument1481 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_namedArgument1504 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_ASSIGN_in_namedArgument1506 = new BitSet(new long[]{0xEFADD0C400000020L,0x000000000400001FL});
    public static final BitSet FOLLOW_expression_in_namedArgument1508 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_argument_in_namedArgument1544 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_qualifiedId1556 = new BitSet(new long[]{0x0000200000000002L});
    public static final BitSet FOLLOW_DOT_in_qualifiedId1559 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_ID_in_qualifiedId1561 = new BitSet(new long[]{0x0000200000000002L});
    public static final BitSet FOLLOW_ID_in_contextName1580 = new BitSet(new long[]{0x0000020000000002L});
    public static final BitSet FOLLOW_DIV_in_contextName1583 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_ID_in_contextName1585 = new BitSet(new long[]{0x0000020000000002L});
    public static final BitSet FOLLOW_INTEGER_LITERAL_in_literal1606 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_LITERAL_in_literal1612 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DQ_STRING_LITERAL_in_literal1617 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_boolLiteral_in_literal1622 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NULL_LITERAL_in_literal1627 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_HEXADECIMAL_INTEGER_LITERAL_in_literal1632 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_REAL_LITERAL_in_literal1638 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_boolLiteral0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_relationalOperator0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_POUND_in_synpred1741 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_ID_in_synpred1743 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred1745 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOLLAR_in_synpred2824 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_ID_in_synpred2826 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred2828 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_synpred3890 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred3892 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_90_in_synpred41239 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_qualifiedId_in_synpred41241 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred41243 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_synpred51495 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_ASSIGN_in_synpred51497 = new BitSet(new long[]{0x0000000000000002L});

}