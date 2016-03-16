lexer grammar ExtensionTags;
options {filter=true;}

@lexer::header { package com.hubrick.raml.mojo.antlr; }

JAVA_TYPE_TAG
    : '@' 'x-javaType' (WS REF)
        ;

REF_TAG
    : '@' 'x-ref' (WS REF)
        ;

fragment
REF
    : ID ('.' ID)*
    ;

fragment
ID
    :   ('a'..'z'|'A'..'Z'|'_'|'$') ('a'..'z'|'A'..'Z'|'_'|'$'|'0'..'9')*
    ;

fragment
WS
    :   (' '|'\t'|'\n')+
    ;
