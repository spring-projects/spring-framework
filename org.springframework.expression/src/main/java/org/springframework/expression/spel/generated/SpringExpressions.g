grammar SpringExpressions;

options {
	language = Java;
	output=AST;
	k=2;
	//caseSensitive = false;
	//backtrack=true;
}

tokens {
	EXPRESSIONLIST;
	INTEGER_LITERAL;
	EXPRESSION;
	QUALIFIED_IDENTIFIER;
	REFERENCE;
	PROPERTY_OR_FIELD;
	INDEXER;
	ARGLIST;
	CONSTRUCTOR;
	DATE_LITERAL;
	HOLDER;
	CONSTRUCTOR_ARRAY;
	NAMED_ARGUMENT;
	FUNCTIONREF;
	TYPEREF;
	RANGE;
	VARIABLEREF;
	LIST_INITIALIZER;
	MAP_INITIALIZER;
	LOCALVAR;
	LOCALFUNC;
	MAP_ENTRY;
	METHOD;
	ADD;
	SUBTRACT;
//	MULTIPLY;
//	DIVIDE;
//	MODULUS;
	NUMBER;
}

// applies only to the parser: 
@header {package org.springframework.expression.spel.generated;}

// applies only to the lexer:
@lexer::header {package org.springframework.expression.spel.generated;}

@rulecatch {
        catch(RecognitionException e) {
                //reportError(e);
                throw e;
        }
}

expr: expression EOF!;

exprList
    : LPAREN expression (SEMI expression)+ (SEMIRPAREN | RPAREN)
      -> ^(EXPRESSIONLIST expression+);
      
SEMIRPAREN : ';)'; // recoveryrelated: allows us to cope with a rogue superfluous semicolon before the rparen in an expression list

expression : 
    logicalOrExpression
    ( (ASSIGN^ logicalOrExpression) 
	  | (DEFAULT^ logicalOrExpression) 
	  | (QMARK^ expression COLON! expression))?;

parenExpr : LPAREN! expression RPAREN!;// (ROGUE! | RPAREN!);

	
logicalOrExpression : logicalAndExpression (OR^ logicalAndExpression)*;
                        
logicalAndExpression : relationalExpression (AND^ relationalExpression)*;
	
relationalExpression : sumExpression (relationalOperator^ sumExpression)?;

sumExpression
	: productExpression ( (PLUS^ | MINUS^) productExpression)*;
//	: left=productExpression (PLUS right+=productExpression)+ -> ^(ADD $left $right)
//	| left=productExpression (MINUS right+=productExpression)+ -> ^(SUBTRACT $left $right)
//	| productExpression;

// TODO could really do with changing ast node types here
productExpression
	: powerExpr ((STAR^ | DIV^| MOD^) powerExpr)* ;
//	: left=powerExpr (STAR right+=powerExpr) -> ^(MULTIPLY $left $right)
//	| left=powerExpr (DIV right+=powerExpr) -> ^(DIVIDE $left $right)
//	| left=powerExpr (MOD right+=powerExpr) -> ^(MODULUS $left $right)
//	| powerExpr;

powerExpr  : unaryExpression (POWER^ unaryExpression)? ;

unaryExpression 
	:	(PLUS^ | MINUS^ | BANG^) unaryExpression	
	|	primaryExpression ;
	
primaryExpression
    : startNode (node)? -> ^(EXPRESSION startNode (node)?);

startNode 
    : 
    (LPAREN expression SEMI) => exprList 
    | parenExpr
    | methodOrProperty 
    | functionOrVar
    | localFunctionOrVar
    | reference
    | indexer
    | literal
    | type
    | constructor
    | projection 
    | selection 
    | firstSelection
    | lastSelection
    | listInitializer
    | mapInitializer
    | lambda
//  | attribute
    ;
    
node:	
	( methodOrProperty 
	| functionOrVar
    | indexer
    | projection 
    | selection 
    | firstSelection 
    | lastSelection 
    | exprList
    | DOT
    )+
	;
	
functionOrVar 
    : (POUND ID LPAREN) => function
    | var
    ;
    
function : POUND id=ID methodArgs -> ^(FUNCTIONREF[$id] methodArgs);
    
var : POUND id=ID -> ^(VARIABLEREF[$id]); 

localFunctionOrVar
	: (DOLLAR ID LPAREN) => localFunction
	| localVar
	;

localFunction : DOLLAR id=ID methodArgs -> ^(LOCALFUNC[$id] methodArgs);
localVar: DOLLAR id=ID -> ^(LOCALVAR[$id]);

methodOrProperty
	:	(ID LPAREN) => id=ID methodArgs -> ^(METHOD[$id] methodArgs)
	|	property
	;
	
// may have to preserve these commas to make it easier to offer suggestions in the right place
// mod at 9th feb 19:13 - added the second 'COMMA?' to allow for code completion "foo(A,"
// TODO need to preserve commas and then check for badly formed call later (optimizing tree walk) to disallow "foo(a,b,c,)"
methodArgs :  LPAREN! (argument (COMMA! argument)* (COMMA!)?)? RPAREN!;

// If we match ID then create a node called PROPERTY_OR_FIELD and copy the id info into it.
// this means the propertyOrField.text is what id.text would have been, rather than having to
// access id as a child of the new node.
property: id=ID -> ^(PROPERTY_OR_FIELD[$id]);

// start - in this block there are changes to help parser recovery and code completion

// fiddled with to support better code completion
// we preserve the colon and rparen to give positional info and the qualifiedId is optional to cope with
// code completing in @() (which is really an invalid expression)
reference
	:  AT pos=LPAREN (cn=contextName COLON)? (q=qualifiedId)? RPAREN
  	-> ^(REFERENCE[$pos] ($cn COLON)? $q? RPAREN);
// what I really want here is: was there a colon? position of the right paren

// end - in this block there are changes to help parser recovery and code completion

//indexer: LBRACKET r1=range (COMMA r2=range)* RBRACKET -> ^(INDEXER $r1 ($r2)*);
indexer: LBRACKET r1=argument (COMMA r2=argument)* RBRACKET -> ^(INDEXER $r1 ($r2)*);
	
//range: INTEGER_LITERAL UPTO^ INTEGER_LITERAL |
// argument;
	// TODO make expression conditional with ? if want completion for when the RCURLY is missing
projection: PROJECT^ expression RCURLY!;

selection: SELECT^ expression RCURLY!;

firstSelection:	SELECT_FIRST^ expression RCURLY!;

lastSelection: SELECT_LAST^ expression RCURLY!;

// TODO cope with array types
type:	TYPE qualifiedId RPAREN -> ^(TYPEREF qualifiedId);
//type:   TYPE tn=qualifiedId (LBRACKET RBRACKET)? (COMMA qid=qualifiedId)? RPAREN

//attribute
//	:	AT! LBRACKET! tn:qualifiedId! (ctorArgs)? RBRACKET!
//		   { #attribute = #([EXPR, tn_AST.getText(), "Spring.Expressions.AttributeNode"], #attribute); }
//	;

lambda
   :   LAMBDA (argList)? PIPE expression RCURLY -> ^(LAMBDA (argList)? expression);

argList : (id+=ID (COMMA id+=ID)*) -> ^(ARGLIST ($id)*);

constructor  
	:	('new' qualifiedId LPAREN) => 'new' qualifiedId ctorArgs -> ^(CONSTRUCTOR qualifiedId ctorArgs)
	|   arrayConstructor
	;

arrayConstructor
	: 'new' qualifiedId arrayRank (listInitializer)?
	  -> ^(CONSTRUCTOR_ARRAY qualifiedId arrayRank (listInitializer)?)
	;
	
arrayRank
    : LBRACKET (expression (COMMA expression)*)? RBRACKET -> ^(EXPRESSIONLIST expression*);
    
listInitializer
    : LCURLY expression (COMMA expression)* RCURLY -> ^(LIST_INITIALIZER expression*);

//arrayInitializer
//    : LCURLY expression (COMMA expression)* RCURLY -> ^(ARRAY_INITIALIZER expression*);
    
mapInitializer
    : POUND LCURLY mapEntry (COMMA mapEntry)* RCURLY -> ^(MAP_INITIALIZER mapEntry*);

mapEntry
    : expression COLON expression -> ^(MAP_ENTRY expression*);

ctorArgs
	: LPAREN! (namedArgument (COMMA! namedArgument)*)? RPAREN!;

argument : expression;

namedArgument 
    : (ID ASSIGN) => id=ID ASSIGN expression 
                  -> ^(NAMED_ARGUMENT[$id] expression)
    | argument ;
  	
qualifiedId : ID (DOT ID)* -> ^(QUALIFIED_IDENTIFIER ID*);

contextName : ID (DIV ID)* -> ^(QUALIFIED_IDENTIFIER ID*);
	
literal
	: INTEGER_LITERAL 
	| STRING_LITERAL
	| DQ_STRING_LITERAL
	| boolLiteral
	| NULL_LITERAL
	| HEXADECIMAL_INTEGER_LITERAL 
	| REAL_LITERAL
	| dateLiteral
	;
	
boolLiteral: TRUE | FALSE;

dateLiteral: 'date' LPAREN d=STRING_LITERAL (COMMA f=STRING_LITERAL)? RPAREN -> ^(DATE_LITERAL $d ($f)?);

INTEGER_LITERAL
	: (DECIMAL_DIGIT)+ (INTEGER_TYPE_SUFFIX)?;

HEXADECIMAL_INTEGER_LITERAL : ('0x' | '0X') (HEX_DIGIT)+ (INTEGER_TYPE_SUFFIX)?;

relationalOperator
    :   EQUAL 
    |   NOT_EQUAL
    |   LESS_THAN
    |   LESS_THAN_OR_EQUAL      
    |   GREATER_THAN            
    |   GREATER_THAN_OR_EQUAL 
    |   IN   
    |   IS   
    |   BETWEEN   
    |   LIKE   
    |   MATCHES
    |   SOUNDSLIKE
    |   DISTANCETO   
    ; 
          
ASSIGN: '=';
EQUAL: '==';
NOT_EQUAL: '!=';
LESS_THAN: '<';
LESS_THAN_OR_EQUAL: '<=';
GREATER_THAN: '>';
GREATER_THAN_OR_EQUAL: '>=';
SOUNDSLIKE
	:	'soundslike';
DISTANCETO
	:	'distanceto';
IN:     'in';	
IS:     'is';
BETWEEN:'between';
LIKE:   'like';
MATCHES:'matches';
NULL_LITERAL: 'null';

SEMI: ';';
DOT:    '.';
COMMA:	',';
LPAREN: '(';
RPAREN: ')';
LCURLY: '{';
RCURLY: '}';
LBRACKET: '[';
RBRACKET: ']';
PIPE:	'|';

AND:    'and';
OR:     'or';
FALSE:  'false';
TRUE:   'true';

PLUS: '+';
MINUS: '-';
DIV: '/';
STAR: '*';
MOD: '%';
POWER: '^';
BANG: '!';
POUND: '#';
QMARK: '?';
DEFAULT: '??';
LAMBDA: '{|';
PROJECT: '!{';
SELECT: '?{';
SELECT_FIRST: '^{';
SELECT_LAST: '${';
TYPE: 'T(';

STRING_LITERAL:	'\''! (APOS|~'\'')* '\''!;
DQ_STRING_LITERAL:	'"'! (~'"')* '"'!;
ID:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9'|DOT_ESCAPED)*;
DOT_ESCAPED: '\\.';
//DOUBLE_DOT: ':';
WS: ( ' ' | '\t' | '\n' |'\r')+ { $channel=HIDDEN; } ;
DOLLAR:	'$';
AT: '@';
UPTO: '..';
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

REAL_LITERAL :	
  ('.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ '.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ (EXPONENT_PART) (REAL_TYPE_SUFFIX)?) |
	((DECIMAL_DIGIT)+ (REAL_TYPE_SUFFIX));

fragment APOS : '\''! '\'';
fragment DECIMAL_DIGIT : '0'..'9' ;
fragment INTEGER_TYPE_SUFFIX : ( 'L' | 'l' );
fragment HEX_DIGIT : '0'|'1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9'|'A'|'B'|'C'|'D'|'E'|'F'|'a'|'b'|'c'|'d'|'e'|'f';		
	
fragment EXPONENT_PART : 'e'  (SIGN)*  (DECIMAL_DIGIT)+ | 'E'  (SIGN)*  (DECIMAL_DIGIT)+ ;	
fragment SIGN :	'+' | '-' ;
// TODO what is M or m?
fragment REAL_TYPE_SUFFIX : 'F' | 'f' | 'D' | 'd' | 'M' | 'm' ;
