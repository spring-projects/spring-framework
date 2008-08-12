lexer grammar SpringExpressions;
options {
  language=Java;

}
@header {package org.springframework.expression.spel.generated;}

T94 : 'new' ;
T95 : 'date' ;

// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 62
SEMIRPAREN : ';)'; // recoveryrelated: allows us to cope with a rogue superfluous semicolon before the rparen in an expression list

// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 263
INTEGER_LITERAL
	: (DECIMAL_DIGIT)+ (INTEGER_TYPE_SUFFIX)?;

// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 266
HEXADECIMAL_INTEGER_LITERAL : '0x' (HEX_DIGIT)+ (INTEGER_TYPE_SUFFIX)?;

// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 284
ASSIGN: '=';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 285
EQUAL: '==';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 286
NOT_EQUAL: '!=';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 287
LESS_THAN: '<';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 288
LESS_THAN_OR_EQUAL: '<=';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 289
GREATER_THAN: '>';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 290
GREATER_THAN_OR_EQUAL: '>=';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 291
SOUNDSLIKE
	:	'soundslike';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 293
DISTANCETO
	:	'distanceto';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 295
IN:     'in';	
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 296
IS:     'is';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 297
BETWEEN:'between';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 298
LIKE:   'like';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 299
MATCHES:'matches';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 300
NULL_LITERAL: 'null';

// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 302
SEMI: ';';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 303
DOT:    '.';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 304
COMMA:	',';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 305
LPAREN: '(';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 306
RPAREN: ')';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 307
LCURLY: '{';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 308
RCURLY: '}';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 309
LBRACKET: '[';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 310
RBRACKET: ']';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 311
PIPE:	'|';

// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 313
AND:    'and';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 314
OR:     'or';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 315
FALSE:  'false';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 316
TRUE:   'true';

// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 318
PLUS: '+';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 319
MINUS: '-';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 320
DIV: '/';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 321
STAR: '*';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 322
MOD: '%';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 323
POWER: '^';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 324
BANG: '!';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 325
POUND: '#';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 326
QMARK: '?';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 327
DEFAULT: '??';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 328
LAMBDA: '{|';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 329
PROJECT: '!{';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 330
SELECT: '?{';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 331
SELECT_FIRST: '^{';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 332
SELECT_LAST: '${';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 333
TYPE: 'T(';

// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 335
STRING_LITERAL:	'\''! (APOS|~'\'')* '\''!;
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 336
DQ_STRING_LITERAL:	'"'! (~'"')* '"'!;
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 337
ID:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9'|DOT_ESCAPED)*;
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 338
DOT_ESCAPED: '\\.';
//DOUBLE_DOT: ':';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 340
WS: ( ' ' | '\t' | '\n' |'\r')+ { $channel=HIDDEN; } ;
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 341
DOLLAR:	'$';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 342
AT: '@';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 343
UPTO: '..';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 344
COLON: ':';

	/*
	// real - use syntactic predicates (guess mode)
	:	('.' DECIMAL_DIGIT) =>
		in= '.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?
			
	|	((DECIMAL_DIGIT)+ '.' DECIMAL_DIGIT) =>
		 in=(DECIMAL_DIGIT)+ '.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?
		
	|	((DECIMAL_DIGIT)+ (EXPONENT_PART)) =>
		in= (DECIMAL_DIGIT)+ (EXPONENT_PART) (REAL_TYPE_SUFFIX)?
		
	|	((DECIMAL_DIGIT)+ (REAL_TYPE_SUFFIX)) =>
		in= (DECIMAL_DIGIT)+ (REAL_TYPE_SUFFIX)		
*/		 

// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 361
REAL_LITERAL :	
  ('.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ '.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ (EXPONENT_PART) (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ (REAL_TYPE_SUFFIX));

// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 367
fragment APOS : '\''! '\'';
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 368
fragment DECIMAL_DIGIT : '0'..'9' ;
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 369
fragment INTEGER_TYPE_SUFFIX : ( 'UL' | 'LU' | 'ul' | 'lu' | 'uL' | 'lU' | 'U' | 'L' | 'u' | 'l' );
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 370
fragment HEX_DIGIT : '0'|'1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9'|'A'|'B'|'C'|'D'|'E'|'F'|'a'|'b'|'c'|'d'|'e'|'f';		
	
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 372
fragment EXPONENT_PART : 'e'  (SIGN)*  (DECIMAL_DIGIT)+ | 'E'  (SIGN)*  (DECIMAL_DIGIT)+ ;	
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 373
fragment SIGN :	'+' | '-' ;
// TODO what is M or m?
// $ANTLR src "/Users/aclement/el/spring3/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 375
fragment REAL_TYPE_SUFFIX : 'F' | 'f' | 'D' | 'd' | 'M' | 'm' ;
