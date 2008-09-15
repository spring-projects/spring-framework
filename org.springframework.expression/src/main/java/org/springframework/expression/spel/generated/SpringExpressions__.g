lexer grammar SpringExpressions;
options {
  language=Java;

}
@header {package org.springframework.expression.spel.generated;}

T91 : 'new' ;
T92 : 'date' ;

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 63
SEMIRPAREN : ';)'; // recoveryrelated: allows us to cope with a rogue superfluous semicolon before the rparen in an expression list

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 262
INTEGER_LITERAL
	: (DECIMAL_DIGIT)+ (INTEGER_TYPE_SUFFIX)?;

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 265
HEXADECIMAL_INTEGER_LITERAL : ('0x' | '0X') (HEX_DIGIT)+ (INTEGER_TYPE_SUFFIX)?;

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 280
ASSIGN: '=';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 281
EQUAL: '==';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 282
NOT_EQUAL: '!=';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 283
LESS_THAN: '<';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 284
LESS_THAN_OR_EQUAL: '<=';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 285
GREATER_THAN: '>';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 286
GREATER_THAN_OR_EQUAL: '>=';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 287
IN:     'in';	
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 288
IS:     'is';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 289
BETWEEN:'between';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 290
MATCHES:'matches';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 291
NULL_LITERAL: 'null';

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 293
SEMI: ';';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 294
DOT:    '.';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 295
COMMA:	',';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 296
LPAREN: '(';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 297
RPAREN: ')';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 298
LCURLY: '{';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 299
RCURLY: '}';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 300
LBRACKET: '[';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 301
RBRACKET: ']';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 302
PIPE:	'|';

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 304
AND:    'and';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 305
OR:     'or';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 306
FALSE:  'false';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 307
TRUE:   'true';

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 309
PLUS: '+';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 310
MINUS: '-';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 311
DIV: '/';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 312
STAR: '*';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 313
MOD: '%';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 314
POWER: '^';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 315
BANG: '!';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 316
POUND: '#';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 317
QMARK: '?';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 318
DEFAULT: '??';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 319
LAMBDA: '{|';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 320
PROJECT: '!{';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 321
SELECT: '?{';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 322
SELECT_FIRST: '^{';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 323
SELECT_LAST: '${';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 324
TYPE: 'T(';

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 326
STRING_LITERAL:	'\''! (APOS|~'\'')* '\''!;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 327
DQ_STRING_LITERAL:	'"'! (~'"')* '"'!;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 328
ID:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9'|DOT_ESCAPED)*;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 329
DOT_ESCAPED: '\\.';
//DOUBLE_DOT: ':';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 331
WS: ( ' ' | '\t' | '\n' |'\r')+ { $channel=HIDDEN; } ;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 332
DOLLAR:	'$';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 333
AT: '@';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 334
UPTO: '..';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 335
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

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 352
REAL_LITERAL :	
  ('.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ '.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ (EXPONENT_PART) (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ (REAL_TYPE_SUFFIX));

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 358
fragment APOS : '\''! '\'';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 359
fragment DECIMAL_DIGIT : '0'..'9' ;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 360
fragment INTEGER_TYPE_SUFFIX : ( 'L' | 'l' );
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 361
fragment HEX_DIGIT : '0'|'1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9'|'A'|'B'|'C'|'D'|'E'|'F'|'a'|'b'|'c'|'d'|'e'|'f';		
	
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 363
fragment EXPONENT_PART : 'e'  (SIGN)*  (DECIMAL_DIGIT)+ | 'E'  (SIGN)*  (DECIMAL_DIGIT)+ ;	
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 364
fragment SIGN :	'+' | '-' ;
// TODO what is M or m?
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 366
fragment REAL_TYPE_SUFFIX : 'F' | 'f' | 'D' | 'd' | 'M' | 'm' ;
