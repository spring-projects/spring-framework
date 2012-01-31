grammar SpringExpressions;

options {
	language = Java;
	output=AST;
	k=2;
}

tokens {
	INTEGER_LITERAL;
	EXPRESSION;
	QUALIFIED_IDENTIFIER;
	PROPERTY_OR_FIELD;
	INDEXER;
	CONSTRUCTOR;
	HOLDER;
	NAMED_ARGUMENT;
	FUNCTIONREF;
	TYPEREF;
	VARIABLEREF;
	METHOD;
	ADD;
	SUBTRACT;
	NUMBER;
}

// applies only to the parser: 
@header {package org.springframework.expression.spel.generated;}

// applies only to the lexer:
@lexer::header {package org.springframework.expression.spel.generated;}

@members {
  // For collecting info whilst processing rules that can be used in messages
  protected Stack<String> paraphrase = new Stack<String>();
}
  
@rulecatch {
        catch(RecognitionException e) {
                reportError(e);
                throw e;
        }
}

expr: expression EOF!;
      
expression : 
    logicalOrExpression
    ( (ASSIGN^ logicalOrExpression) 
	  | (DEFAULT^ logicalOrExpression) 
	  | (QMARK^ expression COLON! expression))?;

parenExpr : LPAREN! expression RPAREN!;
	
logicalOrExpression 
: logicalAndExpression (OR^ logicalAndExpression)*;
                        
logicalAndExpression 
: relationalExpression (AND^ relationalExpression)*;
	
relationalExpression : sumExpression (relationalOperator^ sumExpression)?;

sumExpression
	: productExpression ( (PLUS^ | MINUS^) productExpression)*;

productExpression
	: powerExpr ((STAR^ | DIV^| MOD^) powerExpr)* ;

powerExpr  : unaryExpression (POWER^ unaryExpression)? ;

unaryExpression 
	:	(PLUS^ | MINUS^ | BANG^) unaryExpression	
	|	primaryExpression ;
	
primaryExpression
    : startNode (node)? -> ^(EXPRESSION startNode (node)?);

startNode 
    : 
    parenExpr
    | methodOrProperty 
    | functionOrVar
    | indexer
    | literal
    | type
    | constructor
    | projection 
    | selection 
    | firstSelection
    | lastSelection
    ;
    
node
	: ((DOT dottedNode) | nonDottedNode)+;
	
nonDottedNode
	:	indexer;

dottedNode
	:	
	((methodOrProperty 
	| functionOrVar
    | projection 
    | selection 
    | firstSelection 
    | lastSelection 
    ))
	;
	
functionOrVar 
    : (POUND ID LPAREN) => function
    | var
    ;
    
function : POUND id=ID methodArgs -> ^(FUNCTIONREF[$id] methodArgs);
    
var : POUND id=ID -> ^(VARIABLEREF[$id]); 


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


indexer: LBRACKET r1=argument (COMMA r2=argument)* RBRACKET -> ^(INDEXER $r1 ($r2)*);
	
// argument;
	// TODO make expression conditional with ? if want completion for when the RCURLY is missing
projection: PROJECT^ expression RBRACKET!;

selection: SELECT^ expression RBRACKET!;

firstSelection:	SELECT_FIRST^ expression RBRACKET!;

lastSelection: SELECT_LAST^ expression RBRACKET!;

// TODO cope with array types
type:	TYPE qualifiedId RPAREN -> ^(TYPEREF qualifiedId);
//type:   TYPE tn=qualifiedId (LBRACKET RBRACKET)? (COMMA qid=qualifiedId)? RPAREN


constructor  
	:	('new' qualifiedId LPAREN) => 'new' qualifiedId ctorArgs -> ^(CONSTRUCTOR qualifiedId ctorArgs)
	;
	
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
	;
	
boolLiteral: TRUE | FALSE;

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
    |   INSTANCEOF   
    |   BETWEEN   
    |   MATCHES
    ; 
          
ASSIGN: '=';
EQUAL: '==';
NOT_EQUAL: '!=';
LESS_THAN: '<';
LESS_THAN_OR_EQUAL: '<=';
GREATER_THAN: '>';
GREATER_THAN_OR_EQUAL: '>=';
INSTANCEOF:     'instanceof';
BETWEEN:'between';
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
PROJECT: '![';
SELECT: '?[';
SELECT_FIRST: '^[';
SELECT_LAST: '$[';
TYPE: 'T(';

STRING_LITERAL:	'\''! (APOS|~'\'')* '\''!;
DQ_STRING_LITERAL:	'"'! (~'"')* '"'!;
ID:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9'|DOT_ESCAPED)*;
DOT_ESCAPED: '\\.';
WS: ( ' ' | '\t' | '\n' |'\r')+ { $channel=HIDDEN; } ;
DOLLAR:	'$';
AT: '@';
UPTO: '..';
COLON: ':';


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
fragment REAL_TYPE_SUFFIX : 'F' | 'f' | 'D' | 'd';
