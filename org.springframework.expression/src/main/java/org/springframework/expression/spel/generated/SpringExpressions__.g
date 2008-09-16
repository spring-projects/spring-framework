lexer grammar SpringExpressions;
options {
  language=Java;

}
@header {package org.springframework.expression.spel.generated;}

T84 : 'new' ;

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 214
INTEGER_LITERAL
	: (DECIMAL_DIGIT)+ (INTEGER_TYPE_SUFFIX)?;

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 217
HEXADECIMAL_INTEGER_LITERAL : ('0x' | '0X') (HEX_DIGIT)+ (INTEGER_TYPE_SUFFIX)?;

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 232
ASSIGN: '=';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 233
EQUAL: '==';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 234
NOT_EQUAL: '!=';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 235
LESS_THAN: '<';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 236
LESS_THAN_OR_EQUAL: '<=';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 237
GREATER_THAN: '>';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 238
GREATER_THAN_OR_EQUAL: '>=';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 239
IN:     'in';	
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 240
INSTANCEOF:     'instanceof';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 241
BETWEEN:'between';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 242
MATCHES:'matches';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 243
NULL_LITERAL: 'null';

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 245
SEMI: ';';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 246
DOT:    '.';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 247
COMMA:	',';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 248
LPAREN: '(';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 249
RPAREN: ')';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 250
LCURLY: '{';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 251
RCURLY: '}';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 252
LBRACKET: '[';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 253
RBRACKET: ']';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 254
PIPE:	'|';

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 256
AND:    'and';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 257
OR:     'or';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 258
FALSE:  'false';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 259
TRUE:   'true';

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 261
PLUS: '+';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 262
MINUS: '-';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 263
DIV: '/';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 264
STAR: '*';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 265
MOD: '%';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 266
POWER: '^';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 267
BANG: '!';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 268
POUND: '#';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 269
QMARK: '?';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 270
DEFAULT: '??';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 271
PROJECT: '!{';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 272
SELECT: '?{';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 273
SELECT_FIRST: '^{';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 274
SELECT_LAST: '${';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 275
TYPE: 'T(';

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 277
STRING_LITERAL:	'\''! (APOS|~'\'')* '\''!;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 278
DQ_STRING_LITERAL:	'"'! (~'"')* '"'!;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 279
ID:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9'|DOT_ESCAPED)*;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 280
DOT_ESCAPED: '\\.';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 281
WS: ( ' ' | '\t' | '\n' |'\r')+ { $channel=HIDDEN; } ;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 282
DOLLAR:	'$';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 283
AT: '@';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 284
UPTO: '..';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 285
COLON: ':';


// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 288
REAL_LITERAL :	
  ('.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ '.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ (EXPONENT_PART) (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ (REAL_TYPE_SUFFIX));

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 294
fragment APOS : '\''! '\'';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 295
fragment DECIMAL_DIGIT : '0'..'9' ;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 296
fragment INTEGER_TYPE_SUFFIX : ( 'L' | 'l' );
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 297
fragment HEX_DIGIT : '0'|'1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9'|'A'|'B'|'C'|'D'|'E'|'F'|'a'|'b'|'c'|'d'|'e'|'f';		
	
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 299
fragment EXPONENT_PART : 'e'  (SIGN)*  (DECIMAL_DIGIT)+ | 'E'  (SIGN)*  (DECIMAL_DIGIT)+ ;	
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 300
fragment SIGN :	'+' | '-' ;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 301
fragment REAL_TYPE_SUFFIX : 'F' | 'f' | 'D' | 'd';
