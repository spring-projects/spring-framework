lexer grammar SpringExpressions;
options {
  language=Java;

}
@header {package org.springframework.expression.spel.generated;}

T77 : 'new' ;

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 183
INTEGER_LITERAL
	: (DECIMAL_DIGIT)+ (INTEGER_TYPE_SUFFIX)?;

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 186
HEXADECIMAL_INTEGER_LITERAL : ('0x' | '0X') (HEX_DIGIT)+ (INTEGER_TYPE_SUFFIX)?;

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 200
ASSIGN: '=';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 201
EQUAL: '==';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 202
NOT_EQUAL: '!=';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 203
LESS_THAN: '<';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 204
LESS_THAN_OR_EQUAL: '<=';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 205
GREATER_THAN: '>';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 206
GREATER_THAN_OR_EQUAL: '>=';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 207
INSTANCEOF:     'instanceof';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 208
BETWEEN:'between';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 209
MATCHES:'matches';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 210
NULL_LITERAL: 'null';

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 212
SEMI: ';';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 213
DOT:    '.';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 214
COMMA:	',';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 215
LPAREN: '(';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 216
RPAREN: ')';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 217
LCURLY: '{';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 218
RCURLY: '}';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 219
LBRACKET: '[';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 220
RBRACKET: ']';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 221
PIPE:	'|';

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 223
AND:    'and';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 224
OR:     'or';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 225
FALSE:  'false';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 226
TRUE:   'true';

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 228
PLUS: '+';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 229
MINUS: '-';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 230
DIV: '/';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 231
STAR: '*';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 232
MOD: '%';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 233
POWER: '^';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 234
BANG: '!';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 235
POUND: '#';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 236
QMARK: '?';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 237
DEFAULT: '??';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 238
PROJECT: '!{';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 239
SELECT: '?{';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 240
SELECT_FIRST: '^{';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 241
SELECT_LAST: '${';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 242
TYPE: 'T(';

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 244
STRING_LITERAL:	'\''! (APOS|~'\'')* '\''!;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 245
DQ_STRING_LITERAL:	'"'! (~'"')* '"'!;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 246
ID:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9'|DOT_ESCAPED)*;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 247
DOT_ESCAPED: '\\.';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 248
WS: ( ' ' | '\t' | '\n' |'\r')+ { $channel=HIDDEN; } ;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 249
DOLLAR:	'$';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 250
AT: '@';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 251
UPTO: '..';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 252
COLON: ':';


// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 255
REAL_LITERAL :	
  ('.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ '.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ (EXPONENT_PART) (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ (REAL_TYPE_SUFFIX));

// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 261
fragment APOS : '\''! '\'';
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 262
fragment DECIMAL_DIGIT : '0'..'9' ;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 263
fragment INTEGER_TYPE_SUFFIX : ( 'L' | 'l' );
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 264
fragment HEX_DIGIT : '0'|'1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9'|'A'|'B'|'C'|'D'|'E'|'F'|'a'|'b'|'c'|'d'|'e'|'f';		
	
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 266
fragment EXPONENT_PART : 'e'  (SIGN)*  (DECIMAL_DIGIT)+ | 'E'  (SIGN)*  (DECIMAL_DIGIT)+ ;	
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 267
fragment SIGN :	'+' | '-' ;
// $ANTLR src "/Users/aclement/el2/spring-framework/trunk/org.springframework.expression/src/main/java/org/springframework/expression/spel/generated/SpringExpressions.g" 268
fragment REAL_TYPE_SUFFIX : 'F' | 'f' | 'D' | 'd';
