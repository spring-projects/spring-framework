// $ANTLR 3.0.1 /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g 2008-08-14 13:26:41
package org.springframework.expression.spel.generated;

import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class SpringExpressionsLexer extends Lexer {
    public static final int GREATER_THAN_OR_EQUAL=79;
    public static final int COMMA=51;
    public static final int GREATER_THAN=78;
    public static final int EXPRESSIONLIST=4;
    public static final int MINUS=41;
    public static final int NUMBER=29;
    public static final int ARGLIST=11;
    public static final int BANG=46;
    public static final int LESS_THAN=76;
    public static final int METHOD=26;
    public static final int FALSE=70;
    public static final int PROPERTY_OR_FIELD=9;
    public static final int INDEXER=10;
    public static final int CONSTRUCTOR_ARRAY=15;
    public static final int NULL_LITERAL=66;
    public static final int NAMED_ARGUMENT=16;
    public static final int PIPE=62;
    public static final int DOT=47;
    public static final int EXPRESSION=6;
    public static final int AND=39;
    public static final int LCURLY=63;
    public static final int DATE_LITERAL=13;
    public static final int STRING_LITERAL=64;
    public static final int SELECT=57;
    public static final int QUALIFIED_IDENTIFIER=7;
    public static final int REAL_TYPE_SUFFIX=92;
    public static final int RBRACKET=54;
    public static final int SUBTRACT=28;
    public static final int BETWEEN=82;
    public static final int RPAREN=33;
    public static final int SIGN=93;
    public static final int PLUS=40;
    public static final int INTEGER_LITERAL=5;
    public static final int AT=52;
    public static final int RANGE=19;
    public static final int SOUNDSLIKE=85;
    public static final int WS=89;
    public static final int DOLLAR=50;
    public static final int LESS_THAN_OR_EQUAL=77;
    public static final int T94=94;
    public static final int HEXADECIMAL_INTEGER_LITERAL=67;
    public static final int LAMBDA=61;
    public static final int SEMI=31;
    public static final int EQUAL=74;
    public static final int DOT_ESCAPED=88;
    public static final int QMARK=36;
    public static final int COLON=37;
    public static final int PROJECT=55;
    public static final int DIV=43;
    public static final int REAL_LITERAL=68;
    public static final int ADD=27;
    public static final int TRUE=69;
    public static final int EXPONENT_PART=91;
    public static final int POUND=48;
    public static final int HOLDER=14;
    public static final int SELECT_FIRST=58;
    public static final int TYPE=60;
    public static final int MAP_ENTRY=25;
    public static final int SELECT_LAST=59;
    public static final int LBRACKET=53;
    public static final int MOD=44;
    public static final int FUNCTIONREF=17;
    public static final int OR=38;
    public static final int RCURLY=56;
    public static final int ASSIGN=34;
    public static final int LPAREN=30;
    public static final int HEX_DIGIT=73;
    public static final int LIST_INITIALIZER=21;
    public static final int APOS=87;
    public static final int ID=49;
    public static final int NOT_EQUAL=75;
    public static final int T95=95;
    public static final int POWER=45;
    public static final int TYPEREF=18;
    public static final int DISTANCETO=86;
    public static final int DECIMAL_DIGIT=71;
    public static final int IS=81;
    public static final int SEMIRPAREN=32;
    public static final int DQ_STRING_LITERAL=65;
    public static final int LIKE=83;
    public static final int MAP_INITIALIZER=22;
    public static final int LOCALFUNC=24;
    public static final int IN=80;
    public static final int CONSTRUCTOR=12;
    public static final int INTEGER_TYPE_SUFFIX=72;
    public static final int MATCHES=84;
    public static final int EOF=-1;
    public static final int UPTO=90;
    public static final int REFERENCE=8;
    public static final int Tokens=96;
    public static final int DEFAULT=35;
    public static final int LOCALVAR=23;
    public static final int STAR=42;
    public static final int VARIABLEREF=20;
    public SpringExpressionsLexer() {;} 
    public SpringExpressionsLexer(CharStream input) {
        super(input);
    }
    public String getGrammarFileName() { return "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g"; }

    // $ANTLR start T94
    public final void mT94() throws RecognitionException {
        try {
            int _type = T94;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:8:5: ( 'new' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:8:7: 'new'
            {
            match("new"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T94

    // $ANTLR start T95
    public final void mT95() throws RecognitionException {
        try {
            int _type = T95;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:9:5: ( 'date' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:9:7: 'date'
            {
            match("date"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T95

    // $ANTLR start SEMIRPAREN
    public final void mSEMIRPAREN() throws RecognitionException {
        try {
            int _type = SEMIRPAREN;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:62:12: ( ';)' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:62:14: ';)'
            {
            match(";)"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end SEMIRPAREN

    // $ANTLR start INTEGER_LITERAL
    public final void mINTEGER_LITERAL() throws RecognitionException {
        try {
            int _type = INTEGER_LITERAL;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:264:2: ( ( DECIMAL_DIGIT )+ ( INTEGER_TYPE_SUFFIX )? )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:264:4: ( DECIMAL_DIGIT )+ ( INTEGER_TYPE_SUFFIX )?
            {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:264:4: ( DECIMAL_DIGIT )+
            int cnt1=0;
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( ((LA1_0>='0' && LA1_0<='9')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:264:5: DECIMAL_DIGIT
            	    {
            	    mDECIMAL_DIGIT(); 

            	    }
            	    break;

            	default :
            	    if ( cnt1 >= 1 ) break loop1;
                        EarlyExitException eee =
                            new EarlyExitException(1, input);
                        throw eee;
                }
                cnt1++;
            } while (true);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:264:21: ( INTEGER_TYPE_SUFFIX )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0=='L'||LA2_0=='l') ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:264:22: INTEGER_TYPE_SUFFIX
                    {
                    mINTEGER_TYPE_SUFFIX(); 

                    }
                    break;

            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end INTEGER_LITERAL

    // $ANTLR start HEXADECIMAL_INTEGER_LITERAL
    public final void mHEXADECIMAL_INTEGER_LITERAL() throws RecognitionException {
        try {
            int _type = HEXADECIMAL_INTEGER_LITERAL;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:266:29: ( ( '0x' | '0X' ) ( HEX_DIGIT )+ ( INTEGER_TYPE_SUFFIX )? )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:266:31: ( '0x' | '0X' ) ( HEX_DIGIT )+ ( INTEGER_TYPE_SUFFIX )?
            {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:266:31: ( '0x' | '0X' )
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0=='0') ) {
                int LA3_1 = input.LA(2);

                if ( (LA3_1=='x') ) {
                    alt3=1;
                }
                else if ( (LA3_1=='X') ) {
                    alt3=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("266:31: ( '0x' | '0X' )", 3, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("266:31: ( '0x' | '0X' )", 3, 0, input);

                throw nvae;
            }
            switch (alt3) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:266:32: '0x'
                    {
                    match("0x"); 


                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:266:39: '0X'
                    {
                    match("0X"); 


                    }
                    break;

            }

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:266:45: ( HEX_DIGIT )+
            int cnt4=0;
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);

                if ( ((LA4_0>='0' && LA4_0<='9')||(LA4_0>='A' && LA4_0<='F')||(LA4_0>='a' && LA4_0<='f')) ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:266:46: HEX_DIGIT
            	    {
            	    mHEX_DIGIT(); 

            	    }
            	    break;

            	default :
            	    if ( cnt4 >= 1 ) break loop4;
                        EarlyExitException eee =
                            new EarlyExitException(4, input);
                        throw eee;
                }
                cnt4++;
            } while (true);

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:266:58: ( INTEGER_TYPE_SUFFIX )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0=='L'||LA5_0=='l') ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:266:59: INTEGER_TYPE_SUFFIX
                    {
                    mINTEGER_TYPE_SUFFIX(); 

                    }
                    break;

            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end HEXADECIMAL_INTEGER_LITERAL

    // $ANTLR start ASSIGN
    public final void mASSIGN() throws RecognitionException {
        try {
            int _type = ASSIGN;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:284:7: ( '=' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:284:9: '='
            {
            match('='); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end ASSIGN

    // $ANTLR start EQUAL
    public final void mEQUAL() throws RecognitionException {
        try {
            int _type = EQUAL;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:285:6: ( '==' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:285:8: '=='
            {
            match("=="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end EQUAL

    // $ANTLR start NOT_EQUAL
    public final void mNOT_EQUAL() throws RecognitionException {
        try {
            int _type = NOT_EQUAL;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:286:10: ( '!=' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:286:12: '!='
            {
            match("!="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end NOT_EQUAL

    // $ANTLR start LESS_THAN
    public final void mLESS_THAN() throws RecognitionException {
        try {
            int _type = LESS_THAN;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:287:10: ( '<' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:287:12: '<'
            {
            match('<'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LESS_THAN

    // $ANTLR start LESS_THAN_OR_EQUAL
    public final void mLESS_THAN_OR_EQUAL() throws RecognitionException {
        try {
            int _type = LESS_THAN_OR_EQUAL;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:288:19: ( '<=' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:288:21: '<='
            {
            match("<="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LESS_THAN_OR_EQUAL

    // $ANTLR start GREATER_THAN
    public final void mGREATER_THAN() throws RecognitionException {
        try {
            int _type = GREATER_THAN;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:289:13: ( '>' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:289:15: '>'
            {
            match('>'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end GREATER_THAN

    // $ANTLR start GREATER_THAN_OR_EQUAL
    public final void mGREATER_THAN_OR_EQUAL() throws RecognitionException {
        try {
            int _type = GREATER_THAN_OR_EQUAL;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:290:22: ( '>=' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:290:24: '>='
            {
            match(">="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end GREATER_THAN_OR_EQUAL

    // $ANTLR start SOUNDSLIKE
    public final void mSOUNDSLIKE() throws RecognitionException {
        try {
            int _type = SOUNDSLIKE;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:292:2: ( 'soundslike' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:292:4: 'soundslike'
            {
            match("soundslike"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end SOUNDSLIKE

    // $ANTLR start DISTANCETO
    public final void mDISTANCETO() throws RecognitionException {
        try {
            int _type = DISTANCETO;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:294:2: ( 'distanceto' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:294:4: 'distanceto'
            {
            match("distanceto"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DISTANCETO

    // $ANTLR start IN
    public final void mIN() throws RecognitionException {
        try {
            int _type = IN;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:295:3: ( 'in' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:295:9: 'in'
            {
            match("in"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end IN

    // $ANTLR start IS
    public final void mIS() throws RecognitionException {
        try {
            int _type = IS;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:296:3: ( 'is' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:296:9: 'is'
            {
            match("is"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end IS

    // $ANTLR start BETWEEN
    public final void mBETWEEN() throws RecognitionException {
        try {
            int _type = BETWEEN;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:297:8: ( 'between' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:297:9: 'between'
            {
            match("between"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end BETWEEN

    // $ANTLR start LIKE
    public final void mLIKE() throws RecognitionException {
        try {
            int _type = LIKE;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:298:5: ( 'like' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:298:9: 'like'
            {
            match("like"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LIKE

    // $ANTLR start MATCHES
    public final void mMATCHES() throws RecognitionException {
        try {
            int _type = MATCHES;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:299:8: ( 'matches' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:299:9: 'matches'
            {
            match("matches"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end MATCHES

    // $ANTLR start NULL_LITERAL
    public final void mNULL_LITERAL() throws RecognitionException {
        try {
            int _type = NULL_LITERAL;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:300:13: ( 'null' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:300:15: 'null'
            {
            match("null"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end NULL_LITERAL

    // $ANTLR start SEMI
    public final void mSEMI() throws RecognitionException {
        try {
            int _type = SEMI;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:302:5: ( ';' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:302:7: ';'
            {
            match(';'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end SEMI

    // $ANTLR start DOT
    public final void mDOT() throws RecognitionException {
        try {
            int _type = DOT;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:303:4: ( '.' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:303:9: '.'
            {
            match('.'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DOT

    // $ANTLR start COMMA
    public final void mCOMMA() throws RecognitionException {
        try {
            int _type = COMMA;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:304:6: ( ',' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:304:8: ','
            {
            match(','); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end COMMA

    // $ANTLR start LPAREN
    public final void mLPAREN() throws RecognitionException {
        try {
            int _type = LPAREN;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:305:7: ( '(' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:305:9: '('
            {
            match('('); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LPAREN

    // $ANTLR start RPAREN
    public final void mRPAREN() throws RecognitionException {
        try {
            int _type = RPAREN;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:306:7: ( ')' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:306:9: ')'
            {
            match(')'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end RPAREN

    // $ANTLR start LCURLY
    public final void mLCURLY() throws RecognitionException {
        try {
            int _type = LCURLY;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:307:7: ( '{' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:307:9: '{'
            {
            match('{'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LCURLY

    // $ANTLR start RCURLY
    public final void mRCURLY() throws RecognitionException {
        try {
            int _type = RCURLY;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:308:7: ( '}' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:308:9: '}'
            {
            match('}'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end RCURLY

    // $ANTLR start LBRACKET
    public final void mLBRACKET() throws RecognitionException {
        try {
            int _type = LBRACKET;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:309:9: ( '[' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:309:11: '['
            {
            match('['); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LBRACKET

    // $ANTLR start RBRACKET
    public final void mRBRACKET() throws RecognitionException {
        try {
            int _type = RBRACKET;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:310:9: ( ']' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:310:11: ']'
            {
            match(']'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end RBRACKET

    // $ANTLR start PIPE
    public final void mPIPE() throws RecognitionException {
        try {
            int _type = PIPE;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:311:5: ( '|' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:311:7: '|'
            {
            match('|'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end PIPE

    // $ANTLR start AND
    public final void mAND() throws RecognitionException {
        try {
            int _type = AND;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:313:4: ( 'and' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:313:9: 'and'
            {
            match("and"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end AND

    // $ANTLR start OR
    public final void mOR() throws RecognitionException {
        try {
            int _type = OR;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:314:3: ( 'or' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:314:9: 'or'
            {
            match("or"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end OR

    // $ANTLR start FALSE
    public final void mFALSE() throws RecognitionException {
        try {
            int _type = FALSE;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:315:6: ( 'false' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:315:9: 'false'
            {
            match("false"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end FALSE

    // $ANTLR start TRUE
    public final void mTRUE() throws RecognitionException {
        try {
            int _type = TRUE;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:316:5: ( 'true' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:316:9: 'true'
            {
            match("true"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end TRUE

    // $ANTLR start PLUS
    public final void mPLUS() throws RecognitionException {
        try {
            int _type = PLUS;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:318:5: ( '+' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:318:7: '+'
            {
            match('+'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end PLUS

    // $ANTLR start MINUS
    public final void mMINUS() throws RecognitionException {
        try {
            int _type = MINUS;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:319:6: ( '-' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:319:8: '-'
            {
            match('-'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end MINUS

    // $ANTLR start DIV
    public final void mDIV() throws RecognitionException {
        try {
            int _type = DIV;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:320:4: ( '/' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:320:6: '/'
            {
            match('/'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DIV

    // $ANTLR start STAR
    public final void mSTAR() throws RecognitionException {
        try {
            int _type = STAR;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:321:5: ( '*' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:321:7: '*'
            {
            match('*'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end STAR

    // $ANTLR start MOD
    public final void mMOD() throws RecognitionException {
        try {
            int _type = MOD;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:322:4: ( '%' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:322:6: '%'
            {
            match('%'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end MOD

    // $ANTLR start POWER
    public final void mPOWER() throws RecognitionException {
        try {
            int _type = POWER;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:323:6: ( '^' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:323:8: '^'
            {
            match('^'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end POWER

    // $ANTLR start BANG
    public final void mBANG() throws RecognitionException {
        try {
            int _type = BANG;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:324:5: ( '!' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:324:7: '!'
            {
            match('!'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end BANG

    // $ANTLR start POUND
    public final void mPOUND() throws RecognitionException {
        try {
            int _type = POUND;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:325:6: ( '#' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:325:8: '#'
            {
            match('#'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end POUND

    // $ANTLR start QMARK
    public final void mQMARK() throws RecognitionException {
        try {
            int _type = QMARK;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:326:6: ( '?' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:326:8: '?'
            {
            match('?'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end QMARK

    // $ANTLR start DEFAULT
    public final void mDEFAULT() throws RecognitionException {
        try {
            int _type = DEFAULT;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:327:8: ( '??' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:327:10: '??'
            {
            match("??"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DEFAULT

    // $ANTLR start LAMBDA
    public final void mLAMBDA() throws RecognitionException {
        try {
            int _type = LAMBDA;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:328:7: ( '{|' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:328:9: '{|'
            {
            match("{|"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LAMBDA

    // $ANTLR start PROJECT
    public final void mPROJECT() throws RecognitionException {
        try {
            int _type = PROJECT;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:329:8: ( '!{' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:329:10: '!{'
            {
            match("!{"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end PROJECT

    // $ANTLR start SELECT
    public final void mSELECT() throws RecognitionException {
        try {
            int _type = SELECT;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:330:7: ( '?{' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:330:9: '?{'
            {
            match("?{"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end SELECT

    // $ANTLR start SELECT_FIRST
    public final void mSELECT_FIRST() throws RecognitionException {
        try {
            int _type = SELECT_FIRST;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:331:13: ( '^{' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:331:15: '^{'
            {
            match("^{"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end SELECT_FIRST

    // $ANTLR start SELECT_LAST
    public final void mSELECT_LAST() throws RecognitionException {
        try {
            int _type = SELECT_LAST;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:332:12: ( '${' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:332:14: '${'
            {
            match("${"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end SELECT_LAST

    // $ANTLR start TYPE
    public final void mTYPE() throws RecognitionException {
        try {
            int _type = TYPE;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:333:5: ( 'T(' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:333:7: 'T('
            {
            match("T("); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end TYPE

    // $ANTLR start STRING_LITERAL
    public final void mSTRING_LITERAL() throws RecognitionException {
        try {
            int _type = STRING_LITERAL;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:335:15: ( '\\'' ( APOS | ~ '\\'' )* '\\'' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:335:17: '\\'' ( APOS | ~ '\\'' )* '\\''
            {
            match('\''); 
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:335:23: ( APOS | ~ '\\'' )*
            loop6:
            do {
                int alt6=3;
                int LA6_0 = input.LA(1);

                if ( (LA6_0=='\'') ) {
                    int LA6_1 = input.LA(2);

                    if ( (LA6_1=='\'') ) {
                        alt6=1;
                    }


                }
                else if ( ((LA6_0>='\u0000' && LA6_0<='&')||(LA6_0>='(' && LA6_0<='\uFFFE')) ) {
                    alt6=2;
                }


                switch (alt6) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:335:24: APOS
            	    {
            	    mAPOS(); 

            	    }
            	    break;
            	case 2 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:335:29: ~ '\\''
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='&')||(input.LA(1)>='(' && input.LA(1)<='\uFFFE') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop6;
                }
            } while (true);

            match('\''); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end STRING_LITERAL

    // $ANTLR start DQ_STRING_LITERAL
    public final void mDQ_STRING_LITERAL() throws RecognitionException {
        try {
            int _type = DQ_STRING_LITERAL;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:336:18: ( '\"' (~ '\"' )* '\"' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:336:20: '\"' (~ '\"' )* '\"'
            {
            match('\"'); 
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:336:25: (~ '\"' )*
            loop7:
            do {
                int alt7=2;
                int LA7_0 = input.LA(1);

                if ( ((LA7_0>='\u0000' && LA7_0<='!')||(LA7_0>='#' && LA7_0<='\uFFFE')) ) {
                    alt7=1;
                }


                switch (alt7) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:336:26: ~ '\"'
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='!')||(input.LA(1)>='#' && input.LA(1)<='\uFFFE') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop7;
                }
            } while (true);

            match('\"'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DQ_STRING_LITERAL

    // $ANTLR start ID
    public final void mID() throws RecognitionException {
        try {
            int _type = ID;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:337:3: ( ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' | DOT_ESCAPED )* )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:337:5: ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' | DOT_ESCAPED )*
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:337:29: ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' | DOT_ESCAPED )*
            loop8:
            do {
                int alt8=6;
                switch ( input.LA(1) ) {
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'n':
                case 'o':
                case 'p':
                case 'q':
                case 'r':
                case 's':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                    {
                    alt8=1;
                    }
                    break;
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'G':
                case 'H':
                case 'I':
                case 'J':
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                case 'Y':
                case 'Z':
                    {
                    alt8=2;
                    }
                    break;
                case '_':
                    {
                    alt8=3;
                    }
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    {
                    alt8=4;
                    }
                    break;
                case '\\':
                    {
                    alt8=5;
                    }
                    break;

                }

                switch (alt8) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:337:30: 'a' .. 'z'
            	    {
            	    matchRange('a','z'); 

            	    }
            	    break;
            	case 2 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:337:39: 'A' .. 'Z'
            	    {
            	    matchRange('A','Z'); 

            	    }
            	    break;
            	case 3 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:337:48: '_'
            	    {
            	    match('_'); 

            	    }
            	    break;
            	case 4 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:337:52: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

            	    }
            	    break;
            	case 5 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:337:61: DOT_ESCAPED
            	    {
            	    mDOT_ESCAPED(); 

            	    }
            	    break;

            	default :
            	    break loop8;
                }
            } while (true);


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end ID

    // $ANTLR start DOT_ESCAPED
    public final void mDOT_ESCAPED() throws RecognitionException {
        try {
            int _type = DOT_ESCAPED;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:338:12: ( '\\\\.' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:338:14: '\\\\.'
            {
            match("\\."); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DOT_ESCAPED

    // $ANTLR start WS
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:340:3: ( ( ' ' | '\\t' | '\\n' | '\\r' )+ )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:340:5: ( ' ' | '\\t' | '\\n' | '\\r' )+
            {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:340:5: ( ' ' | '\\t' | '\\n' | '\\r' )+
            int cnt9=0;
            loop9:
            do {
                int alt9=2;
                int LA9_0 = input.LA(1);

                if ( ((LA9_0>='\t' && LA9_0<='\n')||LA9_0=='\r'||LA9_0==' ') ) {
                    alt9=1;
                }


                switch (alt9) {
            	case 1 :
            	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:
            	    {
            	    if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)=='\r'||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt9 >= 1 ) break loop9;
                        EarlyExitException eee =
                            new EarlyExitException(9, input);
                        throw eee;
                }
                cnt9++;
            } while (true);

             channel=HIDDEN; 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end WS

    // $ANTLR start DOLLAR
    public final void mDOLLAR() throws RecognitionException {
        try {
            int _type = DOLLAR;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:341:7: ( '$' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:341:9: '$'
            {
            match('$'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DOLLAR

    // $ANTLR start AT
    public final void mAT() throws RecognitionException {
        try {
            int _type = AT;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:342:3: ( '@' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:342:5: '@'
            {
            match('@'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end AT

    // $ANTLR start UPTO
    public final void mUPTO() throws RecognitionException {
        try {
            int _type = UPTO;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:343:5: ( '..' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:343:7: '..'
            {
            match(".."); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end UPTO

    // $ANTLR start COLON
    public final void mCOLON() throws RecognitionException {
        try {
            int _type = COLON;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:344:6: ( ':' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:344:8: ':'
            {
            match(':'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end COLON

    // $ANTLR start REAL_LITERAL
    public final void mREAL_LITERAL() throws RecognitionException {
        try {
            int _type = REAL_LITERAL;
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:361:14: ( ( '.' ( DECIMAL_DIGIT )+ ( EXPONENT_PART )? ( REAL_TYPE_SUFFIX )? ) | ( ( DECIMAL_DIGIT )+ '.' ( DECIMAL_DIGIT )+ ( EXPONENT_PART )? ( REAL_TYPE_SUFFIX )? ) | ( ( DECIMAL_DIGIT )+ ( EXPONENT_PART ) ( REAL_TYPE_SUFFIX )? ) | ( ( DECIMAL_DIGIT )+ ( REAL_TYPE_SUFFIX ) ) )
            int alt20=4;
            alt20 = dfa20.predict(input);
            switch (alt20) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:362:3: ( '.' ( DECIMAL_DIGIT )+ ( EXPONENT_PART )? ( REAL_TYPE_SUFFIX )? )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:362:3: ( '.' ( DECIMAL_DIGIT )+ ( EXPONENT_PART )? ( REAL_TYPE_SUFFIX )? )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:362:4: '.' ( DECIMAL_DIGIT )+ ( EXPONENT_PART )? ( REAL_TYPE_SUFFIX )?
                    {
                    match('.'); 
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:362:8: ( DECIMAL_DIGIT )+
                    int cnt10=0;
                    loop10:
                    do {
                        int alt10=2;
                        int LA10_0 = input.LA(1);

                        if ( ((LA10_0>='0' && LA10_0<='9')) ) {
                            alt10=1;
                        }


                        switch (alt10) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:362:9: DECIMAL_DIGIT
                    	    {
                    	    mDECIMAL_DIGIT(); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt10 >= 1 ) break loop10;
                                EarlyExitException eee =
                                    new EarlyExitException(10, input);
                                throw eee;
                        }
                        cnt10++;
                    } while (true);

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:362:25: ( EXPONENT_PART )?
                    int alt11=2;
                    int LA11_0 = input.LA(1);

                    if ( (LA11_0=='E'||LA11_0=='e') ) {
                        alt11=1;
                    }
                    switch (alt11) {
                        case 1 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:362:26: EXPONENT_PART
                            {
                            mEXPONENT_PART(); 

                            }
                            break;

                    }

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:362:42: ( REAL_TYPE_SUFFIX )?
                    int alt12=2;
                    int LA12_0 = input.LA(1);

                    if ( (LA12_0=='D'||LA12_0=='F'||LA12_0=='M'||LA12_0=='d'||LA12_0=='f'||LA12_0=='m') ) {
                        alt12=1;
                    }
                    switch (alt12) {
                        case 1 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:362:43: REAL_TYPE_SUFFIX
                            {
                            mREAL_TYPE_SUFFIX(); 

                            }
                            break;

                    }


                    }


                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:363:2: ( ( DECIMAL_DIGIT )+ '.' ( DECIMAL_DIGIT )+ ( EXPONENT_PART )? ( REAL_TYPE_SUFFIX )? )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:363:2: ( ( DECIMAL_DIGIT )+ '.' ( DECIMAL_DIGIT )+ ( EXPONENT_PART )? ( REAL_TYPE_SUFFIX )? )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:363:3: ( DECIMAL_DIGIT )+ '.' ( DECIMAL_DIGIT )+ ( EXPONENT_PART )? ( REAL_TYPE_SUFFIX )?
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:363:3: ( DECIMAL_DIGIT )+
                    int cnt13=0;
                    loop13:
                    do {
                        int alt13=2;
                        int LA13_0 = input.LA(1);

                        if ( ((LA13_0>='0' && LA13_0<='9')) ) {
                            alt13=1;
                        }


                        switch (alt13) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:363:4: DECIMAL_DIGIT
                    	    {
                    	    mDECIMAL_DIGIT(); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt13 >= 1 ) break loop13;
                                EarlyExitException eee =
                                    new EarlyExitException(13, input);
                                throw eee;
                        }
                        cnt13++;
                    } while (true);

                    match('.'); 
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:363:24: ( DECIMAL_DIGIT )+
                    int cnt14=0;
                    loop14:
                    do {
                        int alt14=2;
                        int LA14_0 = input.LA(1);

                        if ( ((LA14_0>='0' && LA14_0<='9')) ) {
                            alt14=1;
                        }


                        switch (alt14) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:363:25: DECIMAL_DIGIT
                    	    {
                    	    mDECIMAL_DIGIT(); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt14 >= 1 ) break loop14;
                                EarlyExitException eee =
                                    new EarlyExitException(14, input);
                                throw eee;
                        }
                        cnt14++;
                    } while (true);

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:363:41: ( EXPONENT_PART )?
                    int alt15=2;
                    int LA15_0 = input.LA(1);

                    if ( (LA15_0=='E'||LA15_0=='e') ) {
                        alt15=1;
                    }
                    switch (alt15) {
                        case 1 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:363:42: EXPONENT_PART
                            {
                            mEXPONENT_PART(); 

                            }
                            break;

                    }

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:363:58: ( REAL_TYPE_SUFFIX )?
                    int alt16=2;
                    int LA16_0 = input.LA(1);

                    if ( (LA16_0=='D'||LA16_0=='F'||LA16_0=='M'||LA16_0=='d'||LA16_0=='f'||LA16_0=='m') ) {
                        alt16=1;
                    }
                    switch (alt16) {
                        case 1 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:363:59: REAL_TYPE_SUFFIX
                            {
                            mREAL_TYPE_SUFFIX(); 

                            }
                            break;

                    }


                    }


                    }
                    break;
                case 3 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:364:2: ( ( DECIMAL_DIGIT )+ ( EXPONENT_PART ) ( REAL_TYPE_SUFFIX )? )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:364:2: ( ( DECIMAL_DIGIT )+ ( EXPONENT_PART ) ( REAL_TYPE_SUFFIX )? )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:364:3: ( DECIMAL_DIGIT )+ ( EXPONENT_PART ) ( REAL_TYPE_SUFFIX )?
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:364:3: ( DECIMAL_DIGIT )+
                    int cnt17=0;
                    loop17:
                    do {
                        int alt17=2;
                        int LA17_0 = input.LA(1);

                        if ( ((LA17_0>='0' && LA17_0<='9')) ) {
                            alt17=1;
                        }


                        switch (alt17) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:364:4: DECIMAL_DIGIT
                    	    {
                    	    mDECIMAL_DIGIT(); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt17 >= 1 ) break loop17;
                                EarlyExitException eee =
                                    new EarlyExitException(17, input);
                                throw eee;
                        }
                        cnt17++;
                    } while (true);

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:364:20: ( EXPONENT_PART )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:364:21: EXPONENT_PART
                    {
                    mEXPONENT_PART(); 

                    }

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:364:36: ( REAL_TYPE_SUFFIX )?
                    int alt18=2;
                    int LA18_0 = input.LA(1);

                    if ( (LA18_0=='D'||LA18_0=='F'||LA18_0=='M'||LA18_0=='d'||LA18_0=='f'||LA18_0=='m') ) {
                        alt18=1;
                    }
                    switch (alt18) {
                        case 1 :
                            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:364:37: REAL_TYPE_SUFFIX
                            {
                            mREAL_TYPE_SUFFIX(); 

                            }
                            break;

                    }


                    }


                    }
                    break;
                case 4 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:365:2: ( ( DECIMAL_DIGIT )+ ( REAL_TYPE_SUFFIX ) )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:365:2: ( ( DECIMAL_DIGIT )+ ( REAL_TYPE_SUFFIX ) )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:365:3: ( DECIMAL_DIGIT )+ ( REAL_TYPE_SUFFIX )
                    {
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:365:3: ( DECIMAL_DIGIT )+
                    int cnt19=0;
                    loop19:
                    do {
                        int alt19=2;
                        int LA19_0 = input.LA(1);

                        if ( ((LA19_0>='0' && LA19_0<='9')) ) {
                            alt19=1;
                        }


                        switch (alt19) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:365:4: DECIMAL_DIGIT
                    	    {
                    	    mDECIMAL_DIGIT(); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt19 >= 1 ) break loop19;
                                EarlyExitException eee =
                                    new EarlyExitException(19, input);
                                throw eee;
                        }
                        cnt19++;
                    } while (true);

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:365:20: ( REAL_TYPE_SUFFIX )
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:365:21: REAL_TYPE_SUFFIX
                    {
                    mREAL_TYPE_SUFFIX(); 

                    }


                    }


                    }
                    break;

            }
            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end REAL_LITERAL

    // $ANTLR start APOS
    public final void mAPOS() throws RecognitionException {
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:367:15: ( '\\'' '\\'' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:367:17: '\\'' '\\''
            {
            match('\''); 
            match('\''); 

            }

        }
        finally {
        }
    }
    // $ANTLR end APOS

    // $ANTLR start DECIMAL_DIGIT
    public final void mDECIMAL_DIGIT() throws RecognitionException {
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:368:24: ( '0' .. '9' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:368:26: '0' .. '9'
            {
            matchRange('0','9'); 

            }

        }
        finally {
        }
    }
    // $ANTLR end DECIMAL_DIGIT

    // $ANTLR start INTEGER_TYPE_SUFFIX
    public final void mINTEGER_TYPE_SUFFIX() throws RecognitionException {
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:369:30: ( ( 'L' | 'l' ) )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:369:32: ( 'L' | 'l' )
            {
            if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

        }
        finally {
        }
    }
    // $ANTLR end INTEGER_TYPE_SUFFIX

    // $ANTLR start HEX_DIGIT
    public final void mHEX_DIGIT() throws RecognitionException {
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:370:20: ( '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | 'A' | 'B' | 'C' | 'D' | 'E' | 'F' | 'a' | 'b' | 'c' | 'd' | 'e' | 'f' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:
            {
            if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='F')||(input.LA(1)>='a' && input.LA(1)<='f') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

        }
        finally {
        }
    }
    // $ANTLR end HEX_DIGIT

    // $ANTLR start EXPONENT_PART
    public final void mEXPONENT_PART() throws RecognitionException {
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:372:24: ( 'e' ( SIGN )* ( DECIMAL_DIGIT )+ | 'E' ( SIGN )* ( DECIMAL_DIGIT )+ )
            int alt25=2;
            int LA25_0 = input.LA(1);

            if ( (LA25_0=='e') ) {
                alt25=1;
            }
            else if ( (LA25_0=='E') ) {
                alt25=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("372:10: fragment EXPONENT_PART : ( 'e' ( SIGN )* ( DECIMAL_DIGIT )+ | 'E' ( SIGN )* ( DECIMAL_DIGIT )+ );", 25, 0, input);

                throw nvae;
            }
            switch (alt25) {
                case 1 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:372:26: 'e' ( SIGN )* ( DECIMAL_DIGIT )+
                    {
                    match('e'); 
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:372:31: ( SIGN )*
                    loop21:
                    do {
                        int alt21=2;
                        int LA21_0 = input.LA(1);

                        if ( (LA21_0=='+'||LA21_0=='-') ) {
                            alt21=1;
                        }


                        switch (alt21) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:372:32: SIGN
                    	    {
                    	    mSIGN(); 

                    	    }
                    	    break;

                    	default :
                    	    break loop21;
                        }
                    } while (true);

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:372:40: ( DECIMAL_DIGIT )+
                    int cnt22=0;
                    loop22:
                    do {
                        int alt22=2;
                        int LA22_0 = input.LA(1);

                        if ( ((LA22_0>='0' && LA22_0<='9')) ) {
                            alt22=1;
                        }


                        switch (alt22) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:372:41: DECIMAL_DIGIT
                    	    {
                    	    mDECIMAL_DIGIT(); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt22 >= 1 ) break loop22;
                                EarlyExitException eee =
                                    new EarlyExitException(22, input);
                                throw eee;
                        }
                        cnt22++;
                    } while (true);


                    }
                    break;
                case 2 :
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:372:59: 'E' ( SIGN )* ( DECIMAL_DIGIT )+
                    {
                    match('E'); 
                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:372:64: ( SIGN )*
                    loop23:
                    do {
                        int alt23=2;
                        int LA23_0 = input.LA(1);

                        if ( (LA23_0=='+'||LA23_0=='-') ) {
                            alt23=1;
                        }


                        switch (alt23) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:372:65: SIGN
                    	    {
                    	    mSIGN(); 

                    	    }
                    	    break;

                    	default :
                    	    break loop23;
                        }
                    } while (true);

                    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:372:73: ( DECIMAL_DIGIT )+
                    int cnt24=0;
                    loop24:
                    do {
                        int alt24=2;
                        int LA24_0 = input.LA(1);

                        if ( ((LA24_0>='0' && LA24_0<='9')) ) {
                            alt24=1;
                        }


                        switch (alt24) {
                    	case 1 :
                    	    // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:372:74: DECIMAL_DIGIT
                    	    {
                    	    mDECIMAL_DIGIT(); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt24 >= 1 ) break loop24;
                                EarlyExitException eee =
                                    new EarlyExitException(24, input);
                                throw eee;
                        }
                        cnt24++;
                    } while (true);


                    }
                    break;

            }
        }
        finally {
        }
    }
    // $ANTLR end EXPONENT_PART

    // $ANTLR start SIGN
    public final void mSIGN() throws RecognitionException {
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:373:15: ( '+' | '-' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:
            {
            if ( input.LA(1)=='+'||input.LA(1)=='-' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

        }
        finally {
        }
    }
    // $ANTLR end SIGN

    // $ANTLR start REAL_TYPE_SUFFIX
    public final void mREAL_TYPE_SUFFIX() throws RecognitionException {
        try {
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:375:27: ( 'F' | 'f' | 'D' | 'd' | 'M' | 'm' )
            // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:
            {
            if ( input.LA(1)=='D'||input.LA(1)=='F'||input.LA(1)=='M'||input.LA(1)=='d'||input.LA(1)=='f'||input.LA(1)=='m' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

        }
        finally {
        }
    }
    // $ANTLR end REAL_TYPE_SUFFIX

    public void mTokens() throws RecognitionException {
        // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:8: ( T94 | T95 | SEMIRPAREN | INTEGER_LITERAL | HEXADECIMAL_INTEGER_LITERAL | ASSIGN | EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL | SOUNDSLIKE | DISTANCETO | IN | IS | BETWEEN | LIKE | MATCHES | NULL_LITERAL | SEMI | DOT | COMMA | LPAREN | RPAREN | LCURLY | RCURLY | LBRACKET | RBRACKET | PIPE | AND | OR | FALSE | TRUE | PLUS | MINUS | DIV | STAR | MOD | POWER | BANG | POUND | QMARK | DEFAULT | LAMBDA | PROJECT | SELECT | SELECT_FIRST | SELECT_LAST | TYPE | STRING_LITERAL | DQ_STRING_LITERAL | ID | DOT_ESCAPED | WS | DOLLAR | AT | UPTO | COLON | REAL_LITERAL )
        int alt26=60;
        alt26 = dfa26.predict(input);
        switch (alt26) {
            case 1 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:10: T94
                {
                mT94(); 

                }
                break;
            case 2 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:14: T95
                {
                mT95(); 

                }
                break;
            case 3 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:18: SEMIRPAREN
                {
                mSEMIRPAREN(); 

                }
                break;
            case 4 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:29: INTEGER_LITERAL
                {
                mINTEGER_LITERAL(); 

                }
                break;
            case 5 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:45: HEXADECIMAL_INTEGER_LITERAL
                {
                mHEXADECIMAL_INTEGER_LITERAL(); 

                }
                break;
            case 6 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:73: ASSIGN
                {
                mASSIGN(); 

                }
                break;
            case 7 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:80: EQUAL
                {
                mEQUAL(); 

                }
                break;
            case 8 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:86: NOT_EQUAL
                {
                mNOT_EQUAL(); 

                }
                break;
            case 9 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:96: LESS_THAN
                {
                mLESS_THAN(); 

                }
                break;
            case 10 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:106: LESS_THAN_OR_EQUAL
                {
                mLESS_THAN_OR_EQUAL(); 

                }
                break;
            case 11 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:125: GREATER_THAN
                {
                mGREATER_THAN(); 

                }
                break;
            case 12 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:138: GREATER_THAN_OR_EQUAL
                {
                mGREATER_THAN_OR_EQUAL(); 

                }
                break;
            case 13 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:160: SOUNDSLIKE
                {
                mSOUNDSLIKE(); 

                }
                break;
            case 14 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:171: DISTANCETO
                {
                mDISTANCETO(); 

                }
                break;
            case 15 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:182: IN
                {
                mIN(); 

                }
                break;
            case 16 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:185: IS
                {
                mIS(); 

                }
                break;
            case 17 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:188: BETWEEN
                {
                mBETWEEN(); 

                }
                break;
            case 18 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:196: LIKE
                {
                mLIKE(); 

                }
                break;
            case 19 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:201: MATCHES
                {
                mMATCHES(); 

                }
                break;
            case 20 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:209: NULL_LITERAL
                {
                mNULL_LITERAL(); 

                }
                break;
            case 21 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:222: SEMI
                {
                mSEMI(); 

                }
                break;
            case 22 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:227: DOT
                {
                mDOT(); 

                }
                break;
            case 23 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:231: COMMA
                {
                mCOMMA(); 

                }
                break;
            case 24 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:237: LPAREN
                {
                mLPAREN(); 

                }
                break;
            case 25 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:244: RPAREN
                {
                mRPAREN(); 

                }
                break;
            case 26 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:251: LCURLY
                {
                mLCURLY(); 

                }
                break;
            case 27 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:258: RCURLY
                {
                mRCURLY(); 

                }
                break;
            case 28 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:265: LBRACKET
                {
                mLBRACKET(); 

                }
                break;
            case 29 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:274: RBRACKET
                {
                mRBRACKET(); 

                }
                break;
            case 30 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:283: PIPE
                {
                mPIPE(); 

                }
                break;
            case 31 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:288: AND
                {
                mAND(); 

                }
                break;
            case 32 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:292: OR
                {
                mOR(); 

                }
                break;
            case 33 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:295: FALSE
                {
                mFALSE(); 

                }
                break;
            case 34 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:301: TRUE
                {
                mTRUE(); 

                }
                break;
            case 35 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:306: PLUS
                {
                mPLUS(); 

                }
                break;
            case 36 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:311: MINUS
                {
                mMINUS(); 

                }
                break;
            case 37 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:317: DIV
                {
                mDIV(); 

                }
                break;
            case 38 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:321: STAR
                {
                mSTAR(); 

                }
                break;
            case 39 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:326: MOD
                {
                mMOD(); 

                }
                break;
            case 40 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:330: POWER
                {
                mPOWER(); 

                }
                break;
            case 41 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:336: BANG
                {
                mBANG(); 

                }
                break;
            case 42 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:341: POUND
                {
                mPOUND(); 

                }
                break;
            case 43 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:347: QMARK
                {
                mQMARK(); 

                }
                break;
            case 44 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:353: DEFAULT
                {
                mDEFAULT(); 

                }
                break;
            case 45 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:361: LAMBDA
                {
                mLAMBDA(); 

                }
                break;
            case 46 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:368: PROJECT
                {
                mPROJECT(); 

                }
                break;
            case 47 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:376: SELECT
                {
                mSELECT(); 

                }
                break;
            case 48 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:383: SELECT_FIRST
                {
                mSELECT_FIRST(); 

                }
                break;
            case 49 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:396: SELECT_LAST
                {
                mSELECT_LAST(); 

                }
                break;
            case 50 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:408: TYPE
                {
                mTYPE(); 

                }
                break;
            case 51 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:413: STRING_LITERAL
                {
                mSTRING_LITERAL(); 

                }
                break;
            case 52 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:428: DQ_STRING_LITERAL
                {
                mDQ_STRING_LITERAL(); 

                }
                break;
            case 53 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:446: ID
                {
                mID(); 

                }
                break;
            case 54 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:449: DOT_ESCAPED
                {
                mDOT_ESCAPED(); 

                }
                break;
            case 55 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:461: WS
                {
                mWS(); 

                }
                break;
            case 56 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:464: DOLLAR
                {
                mDOLLAR(); 

                }
                break;
            case 57 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:471: AT
                {
                mAT(); 

                }
                break;
            case 58 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:474: UPTO
                {
                mUPTO(); 

                }
                break;
            case 59 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:479: COLON
                {
                mCOLON(); 

                }
                break;
            case 60 :
                // /Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g:1:485: REAL_LITERAL
                {
                mREAL_LITERAL(); 

                }
                break;

        }

    }


    protected DFA20 dfa20 = new DFA20(this);
    protected DFA26 dfa26 = new DFA26(this);
    static final String DFA20_eotS =
        "\6\uffff";
    static final String DFA20_eofS =
        "\6\uffff";
    static final String DFA20_minS =
        "\1\56\1\uffff\1\56\3\uffff";
    static final String DFA20_maxS =
        "\1\71\1\uffff\1\155\3\uffff";
    static final String DFA20_acceptS =
        "\1\uffff\1\1\1\uffff\1\4\1\2\1\3";
    static final String DFA20_specialS =
        "\6\uffff}>";
    static final String[] DFA20_transitionS = {
            "\1\1\1\uffff\12\2",
            "",
            "\1\4\1\uffff\12\2\12\uffff\1\3\1\5\1\3\6\uffff\1\3\26\uffff"+
            "\1\3\1\5\1\3\6\uffff\1\3",
            "",
            "",
            ""
    };

    static final short[] DFA20_eot = DFA.unpackEncodedString(DFA20_eotS);
    static final short[] DFA20_eof = DFA.unpackEncodedString(DFA20_eofS);
    static final char[] DFA20_min = DFA.unpackEncodedStringToUnsignedChars(DFA20_minS);
    static final char[] DFA20_max = DFA.unpackEncodedStringToUnsignedChars(DFA20_maxS);
    static final short[] DFA20_accept = DFA.unpackEncodedString(DFA20_acceptS);
    static final short[] DFA20_special = DFA.unpackEncodedString(DFA20_specialS);
    static final short[][] DFA20_transition;

    static {
        int numStates = DFA20_transitionS.length;
        DFA20_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA20_transition[i] = DFA.unpackEncodedString(DFA20_transitionS[i]);
        }
    }

    class DFA20 extends DFA {

        public DFA20(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 20;
            this.eot = DFA20_eot;
            this.eof = DFA20_eof;
            this.min = DFA20_min;
            this.max = DFA20_max;
            this.accept = DFA20_accept;
            this.special = DFA20_special;
            this.transition = DFA20_transition;
        }
        public String getDescription() {
            return "361:1: REAL_LITERAL : ( ( '.' ( DECIMAL_DIGIT )+ ( EXPONENT_PART )? ( REAL_TYPE_SUFFIX )? ) | ( ( DECIMAL_DIGIT )+ '.' ( DECIMAL_DIGIT )+ ( EXPONENT_PART )? ( REAL_TYPE_SUFFIX )? ) | ( ( DECIMAL_DIGIT )+ ( EXPONENT_PART ) ( REAL_TYPE_SUFFIX )? ) | ( ( DECIMAL_DIGIT )+ ( REAL_TYPE_SUFFIX ) ) );";
        }
    }
    static final String DFA26_eotS =
        "\1\uffff\2\50\1\62\2\64\1\67\1\72\1\74\1\76\5\50\1\106\3\uffff\1"+
        "\110\4\uffff\4\50\5\uffff\1\116\1\uffff\1\121\1\123\1\50\7\uffff"+
        "\4\50\16\uffff\1\50\1\132\1\133\3\50\4\uffff\1\50\1\140\2\50\10"+
        "\uffff\1\50\1\144\3\50\2\uffff\3\50\1\153\1\uffff\2\50\1\156\1\uffff"+
        "\1\50\1\160\2\50\1\163\1\50\1\uffff\1\50\1\166\1\uffff\1\50\1\uffff"+
        "\2\50\1\uffff\1\50\1\173\1\uffff\4\50\1\uffff\2\50\1\u0082\1\u0083"+
        "\2\50\2\uffff\2\50\1\u0088\1\u0089\2\uffff";
    static final String DFA26_eofS =
        "\u008a\uffff";
    static final String DFA26_minS =
        "\1\11\1\145\1\141\1\51\2\56\4\75\1\157\1\156\1\145\1\151\1\141\1"+
        "\56\3\uffff\1\174\4\uffff\1\156\1\162\1\141\1\162\5\uffff\1\173"+
        "\1\uffff\1\77\1\173\1\50\7\uffff\1\154\1\167\1\163\1\164\16\uffff"+
        "\1\165\2\60\1\164\1\153\1\164\4\uffff\1\144\1\60\1\154\1\165\10"+
        "\uffff\1\154\1\60\1\164\1\145\1\156\2\uffff\1\167\1\145\1\143\1"+
        "\60\1\uffff\1\163\1\145\1\60\1\uffff\1\141\1\60\1\144\1\145\1\60"+
        "\1\150\1\uffff\1\145\1\60\1\uffff\1\156\1\uffff\1\163\1\145\1\uffff"+
        "\1\145\1\60\1\uffff\1\143\1\154\1\156\1\163\1\uffff\1\145\1\151"+
        "\2\60\1\164\1\153\2\uffff\1\157\1\145\2\60\2\uffff";
    static final String DFA26_maxS =
        "\1\175\1\165\1\151\1\51\1\170\1\155\1\75\1\173\2\75\1\157\1\163"+
        "\1\145\1\151\1\141\1\71\3\uffff\1\174\4\uffff\1\156\1\162\1\141"+
        "\1\162\5\uffff\1\173\1\uffff\2\173\1\50\7\uffff\1\154\1\167\1\163"+
        "\1\164\16\uffff\1\165\2\172\1\164\1\153\1\164\4\uffff\1\144\1\172"+
        "\1\154\1\165\10\uffff\1\154\1\172\1\164\1\145\1\156\2\uffff\1\167"+
        "\1\145\1\143\1\172\1\uffff\1\163\1\145\1\172\1\uffff\1\141\1\172"+
        "\1\144\1\145\1\172\1\150\1\uffff\1\145\1\172\1\uffff\1\156\1\uffff"+
        "\1\163\1\145\1\uffff\1\145\1\172\1\uffff\1\143\1\154\1\156\1\163"+
        "\1\uffff\1\145\1\151\2\172\1\164\1\153\2\uffff\1\157\1\145\2\172"+
        "\2\uffff";
    static final String DFA26_acceptS =
        "\20\uffff\1\27\1\30\1\31\1\uffff\1\33\1\34\1\35\1\36\4\uffff\1\43"+
        "\1\44\1\45\1\46\1\47\1\uffff\1\52\3\uffff\1\63\1\64\1\65\1\66\1"+
        "\67\1\71\1\73\4\uffff\1\3\1\25\1\5\1\4\1\74\1\7\1\6\1\56\1\10\1"+
        "\51\1\12\1\11\1\14\1\13\6\uffff\1\72\1\26\1\55\1\32\4\uffff\1\60"+
        "\1\50\1\57\1\54\1\53\1\61\1\70\1\62\5\uffff\1\20\1\17\4\uffff\1"+
        "\40\3\uffff\1\1\6\uffff\1\37\2\uffff\1\24\1\uffff\1\2\2\uffff\1"+
        "\22\2\uffff\1\42\4\uffff\1\41\6\uffff\1\21\1\23\4\uffff\1\16\1\15";
    static final String DFA26_specialS =
        "\u008a\uffff}>";
    static final String[] DFA26_transitionS = {
            "\2\52\2\uffff\1\52\22\uffff\1\52\1\7\1\47\1\42\1\44\1\40\1\uffff"+
            "\1\46\1\21\1\22\1\37\1\34\1\20\1\35\1\17\1\36\1\4\11\5\1\54"+
            "\1\3\1\10\1\6\1\11\1\43\1\53\23\50\1\45\6\50\1\25\1\51\1\26"+
            "\1\41\1\50\1\uffff\1\30\1\14\1\50\1\2\1\50\1\32\2\50\1\13\2"+
            "\50\1\15\1\16\1\1\1\31\3\50\1\12\1\33\6\50\1\23\1\27\1\24",
            "\1\56\17\uffff\1\55",
            "\1\60\7\uffff\1\57",
            "\1\61",
            "\1\65\1\uffff\12\5\12\uffff\3\65\6\uffff\1\65\12\uffff\1\63"+
            "\13\uffff\3\65\6\uffff\1\65\12\uffff\1\63",
            "\1\65\1\uffff\12\5\12\uffff\3\65\6\uffff\1\65\26\uffff\3\65"+
            "\6\uffff\1\65",
            "\1\66",
            "\1\71\75\uffff\1\70",
            "\1\73",
            "\1\75",
            "\1\77",
            "\1\101\4\uffff\1\100",
            "\1\102",
            "\1\103",
            "\1\104",
            "\1\105\1\uffff\12\65",
            "",
            "",
            "",
            "\1\107",
            "",
            "",
            "",
            "",
            "\1\111",
            "\1\112",
            "\1\113",
            "\1\114",
            "",
            "",
            "",
            "",
            "",
            "\1\115",
            "",
            "\1\120\73\uffff\1\117",
            "\1\122",
            "\1\124",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\125",
            "\1\126",
            "\1\127",
            "\1\130",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\131",
            "\12\50\7\uffff\32\50\1\uffff\1\50\2\uffff\1\50\1\uffff\32\50",
            "\12\50\7\uffff\32\50\1\uffff\1\50\2\uffff\1\50\1\uffff\32\50",
            "\1\134",
            "\1\135",
            "\1\136",
            "",
            "",
            "",
            "",
            "\1\137",
            "\12\50\7\uffff\32\50\1\uffff\1\50\2\uffff\1\50\1\uffff\32\50",
            "\1\141",
            "\1\142",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\143",
            "\12\50\7\uffff\32\50\1\uffff\1\50\2\uffff\1\50\1\uffff\32\50",
            "\1\145",
            "\1\146",
            "\1\147",
            "",
            "",
            "\1\150",
            "\1\151",
            "\1\152",
            "\12\50\7\uffff\32\50\1\uffff\1\50\2\uffff\1\50\1\uffff\32\50",
            "",
            "\1\154",
            "\1\155",
            "\12\50\7\uffff\32\50\1\uffff\1\50\2\uffff\1\50\1\uffff\32\50",
            "",
            "\1\157",
            "\12\50\7\uffff\32\50\1\uffff\1\50\2\uffff\1\50\1\uffff\32\50",
            "\1\161",
            "\1\162",
            "\12\50\7\uffff\32\50\1\uffff\1\50\2\uffff\1\50\1\uffff\32\50",
            "\1\164",
            "",
            "\1\165",
            "\12\50\7\uffff\32\50\1\uffff\1\50\2\uffff\1\50\1\uffff\32\50",
            "",
            "\1\167",
            "",
            "\1\170",
            "\1\171",
            "",
            "\1\172",
            "\12\50\7\uffff\32\50\1\uffff\1\50\2\uffff\1\50\1\uffff\32\50",
            "",
            "\1\174",
            "\1\175",
            "\1\176",
            "\1\177",
            "",
            "\1\u0080",
            "\1\u0081",
            "\12\50\7\uffff\32\50\1\uffff\1\50\2\uffff\1\50\1\uffff\32\50",
            "\12\50\7\uffff\32\50\1\uffff\1\50\2\uffff\1\50\1\uffff\32\50",
            "\1\u0084",
            "\1\u0085",
            "",
            "",
            "\1\u0086",
            "\1\u0087",
            "\12\50\7\uffff\32\50\1\uffff\1\50\2\uffff\1\50\1\uffff\32\50",
            "\12\50\7\uffff\32\50\1\uffff\1\50\2\uffff\1\50\1\uffff\32\50",
            "",
            ""
    };

    static final short[] DFA26_eot = DFA.unpackEncodedString(DFA26_eotS);
    static final short[] DFA26_eof = DFA.unpackEncodedString(DFA26_eofS);
    static final char[] DFA26_min = DFA.unpackEncodedStringToUnsignedChars(DFA26_minS);
    static final char[] DFA26_max = DFA.unpackEncodedStringToUnsignedChars(DFA26_maxS);
    static final short[] DFA26_accept = DFA.unpackEncodedString(DFA26_acceptS);
    static final short[] DFA26_special = DFA.unpackEncodedString(DFA26_specialS);
    static final short[][] DFA26_transition;

    static {
        int numStates = DFA26_transitionS.length;
        DFA26_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA26_transition[i] = DFA.unpackEncodedString(DFA26_transitionS[i]);
        }
    }

    class DFA26 extends DFA {

        public DFA26(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 26;
            this.eot = DFA26_eot;
            this.eof = DFA26_eof;
            this.min = DFA26_min;
            this.max = DFA26_max;
            this.accept = DFA26_accept;
            this.special = DFA26_special;
            this.transition = DFA26_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T94 | T95 | SEMIRPAREN | INTEGER_LITERAL | HEXADECIMAL_INTEGER_LITERAL | ASSIGN | EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN | GREATER_THAN_OR_EQUAL | SOUNDSLIKE | DISTANCETO | IN | IS | BETWEEN | LIKE | MATCHES | NULL_LITERAL | SEMI | DOT | COMMA | LPAREN | RPAREN | LCURLY | RCURLY | LBRACKET | RBRACKET | PIPE | AND | OR | FALSE | TRUE | PLUS | MINUS | DIV | STAR | MOD | POWER | BANG | POUND | QMARK | DEFAULT | LAMBDA | PROJECT | SELECT | SELECT_FIRST | SELECT_LAST | TYPE | STRING_LITERAL | DQ_STRING_LITERAL | ID | DOT_ESCAPED | WS | DOLLAR | AT | UPTO | COLON | REAL_LITERAL );";
        }
    }
 

}