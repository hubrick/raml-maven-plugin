// $ANTLR 3.5.2 com/hubrick/raml/mojo/antlr/Doc.g 2015-07-20 16:44:20
 package com.hubrick.raml.mojo.antlr; 

import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@SuppressWarnings("all")
public class Doc extends Lexer {
	public static final int EOF=-1;
	public static final int ID=4;
	public static final int TAG=5;
	public static final int TYPE=6;
	public static final int WS=7;

	// delegates
	// delegators
	public Lexer[] getDelegates() {
		return new Lexer[] {};
	}

	public Doc() {} 
	public Doc(CharStream input) {
		this(input, new RecognizerSharedState());
	}
	public Doc(CharStream input, RecognizerSharedState state) {
		super(input,state);
	}
	@Override public String getGrammarFileName() { return "com/hubrick/raml/mojo/antlr/Doc.g"; }

	@Override
	public Token nextToken() {
		while (true) {
			if ( input.LA(1)==CharStream.EOF ) {
				Token eof = new CommonToken(input,Token.EOF,
											Token.DEFAULT_CHANNEL,
											input.index(),input.index());
				eof.setLine(getLine());
				eof.setCharPositionInLine(getCharPositionInLine());
				return eof;
			}
			state.token = null;
		state.channel = Token.DEFAULT_CHANNEL;
			state.tokenStartCharIndex = input.index();
			state.tokenStartCharPositionInLine = input.getCharPositionInLine();
			state.tokenStartLine = input.getLine();
		state.text = null;
			try {
				int m = input.mark();
				state.backtracking=1; 
				state.failed=false;
				mTokens();
				state.backtracking=0;
				if ( state.failed ) {
					input.rewind(m);
					input.consume(); 
				}
				else {
					emit();
					return state.token;
				}
			}
			catch (RecognitionException re) {
				// shouldn't happen in backtracking mode, but...
				reportError(re);
				recover(re);
			}
		}
	}

	@Override
	public void memoize(IntStream input,
			int ruleIndex,
			int ruleStartIndex)
	{
	if ( state.backtracking>1 ) super.memoize(input, ruleIndex, ruleStartIndex);
	}

	@Override
	public boolean alreadyParsedRule(IntStream input, int ruleIndex) {
	if ( state.backtracking>1 ) return super.alreadyParsedRule(input, ruleIndex);
	return false;
	}
	// $ANTLR start "TAG"
	public final void mTAG() throws RecognitionException {
		try {
			// com/hubrick/raml/mojo/antlr/Doc.g:7:6: ( '@' ID ( WS ID )? )
			// com/hubrick/raml/mojo/antlr/Doc.g:7:8: '@' ID ( WS ID )?
			{
			match('@'); if (state.failed) return;
			mID(); if (state.failed) return;

			// com/hubrick/raml/mojo/antlr/Doc.g:7:15: ( WS ID )?
			int alt1=2;
			int LA1_0 = input.LA(1);
			if ( ((LA1_0 >= '\t' && LA1_0 <= '\n')||LA1_0==' ') ) {
				alt1=1;
			}
			switch (alt1) {
				case 1 :
					// com/hubrick/raml/mojo/antlr/Doc.g:7:16: WS ID
					{
					mWS(); if (state.failed) return;

					mID(); if (state.failed) return;

					}
					break;

			}

			}

		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "TAG"

	// $ANTLR start "TYPE"
	public final void mTYPE() throws RecognitionException {
		try {
			// com/hubrick/raml/mojo/antlr/Doc.g:11:7: ( ID ( '.' ID )* )
			// com/hubrick/raml/mojo/antlr/Doc.g:11:11: ID ( '.' ID )*
			{
			mID(); if (state.failed) return;

			// com/hubrick/raml/mojo/antlr/Doc.g:11:14: ( '.' ID )*
			loop2:
			while (true) {
				int alt2=2;
				int LA2_0 = input.LA(1);
				if ( (LA2_0=='.') ) {
					alt2=1;
				}

				switch (alt2) {
				case 1 :
					// com/hubrick/raml/mojo/antlr/Doc.g:11:15: '.' ID
					{
					match('.'); if (state.failed) return;
					mID(); if (state.failed) return;

					}
					break;

				default :
					break loop2;
				}
			}

			}

		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "TYPE"

	// $ANTLR start "ID"
	public final void mID() throws RecognitionException {
		try {
			// com/hubrick/raml/mojo/antlr/Doc.g:15:5: ( ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )* )
			// com/hubrick/raml/mojo/antlr/Doc.g:15:9: ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )*
			{
			if ( (input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
				input.consume();
				state.failed=false;
			}
			else {
				if (state.backtracking>0) {state.failed=true; return;}
				MismatchedSetException mse = new MismatchedSetException(null,input);
				recover(mse);
				throw mse;
			}
			// com/hubrick/raml/mojo/antlr/Doc.g:15:33: ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )*
			loop3:
			while (true) {
				int alt3=2;
				int LA3_0 = input.LA(1);
				if ( ((LA3_0 >= '0' && LA3_0 <= '9')||(LA3_0 >= 'A' && LA3_0 <= 'Z')||LA3_0=='_'||(LA3_0 >= 'a' && LA3_0 <= 'z')) ) {
					alt3=1;
				}

				switch (alt3) {
				case 1 :
					// com/hubrick/raml/mojo/antlr/Doc.g:
					{
					if ( (input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
						input.consume();
						state.failed=false;
					}
					else {
						if (state.backtracking>0) {state.failed=true; return;}
						MismatchedSetException mse = new MismatchedSetException(null,input);
						recover(mse);
						throw mse;
					}
					}
					break;

				default :
					break loop3;
				}
			}

			}

		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "ID"

	// $ANTLR start "WS"
	public final void mWS() throws RecognitionException {
		try {
			int _type = WS;
			int _channel = DEFAULT_TOKEN_CHANNEL;
			// com/hubrick/raml/mojo/antlr/Doc.g:18:5: ( ( ' ' | '\\t' | '\\n' )+ )
			// com/hubrick/raml/mojo/antlr/Doc.g:18:9: ( ' ' | '\\t' | '\\n' )+
			{
			// com/hubrick/raml/mojo/antlr/Doc.g:18:9: ( ' ' | '\\t' | '\\n' )+
			int cnt4=0;
			loop4:
			while (true) {
				int alt4=2;
				int LA4_0 = input.LA(1);
				if ( ((LA4_0 >= '\t' && LA4_0 <= '\n')||LA4_0==' ') ) {
					alt4=1;
				}

				switch (alt4) {
				case 1 :
					// com/hubrick/raml/mojo/antlr/Doc.g:
					{
					if ( (input.LA(1) >= '\t' && input.LA(1) <= '\n')||input.LA(1)==' ' ) {
						input.consume();
						state.failed=false;
					}
					else {
						if (state.backtracking>0) {state.failed=true; return;}
						MismatchedSetException mse = new MismatchedSetException(null,input);
						recover(mse);
						throw mse;
					}
					}
					break;

				default :
					if ( cnt4 >= 1 ) break loop4;
					if (state.backtracking>0) {state.failed=true; return;}
					EarlyExitException eee = new EarlyExitException(4, input);
					throw eee;
				}
				cnt4++;
			}

			}

			state.type = _type;
			state.channel = _channel;
		}
		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "WS"

	@Override
	public void mTokens() throws RecognitionException {
		// com/hubrick/raml/mojo/antlr/Doc.g:1:39: ( WS )
		// com/hubrick/raml/mojo/antlr/Doc.g:1:41: WS
		{
		mWS(); if (state.failed) return;

		}

	}



}
