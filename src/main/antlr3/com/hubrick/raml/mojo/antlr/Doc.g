lexer grammar Doc;
options {filter=true;}

@lexer::header { package com.hubrick.raml.mojo.antlr; }

fragment
TAG 	: '@' ID (WS ID)?
	;

fragment
TYPE 	:   ID ('.' ID)*
        ;

fragment
ID  :   ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*
    ;

WS  :   (' '|'\t'|'\n')+
    ;