/*
 * Generated on 12/13/23, 4:35 AM
 */
package se.redfield.knime.neo4j.ui.editor;

import java.io.*;
import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.*;


/**
 * 
 */
%%

%public
%class CypherTokenMaker
%extends AbstractJFlexTokenMaker
%unicode
%ignorecase
%type org.fife.ui.rsyntaxtextarea.Token


%{


	/**
	 * Constructor.  This must be here because JFlex does not generate a
	 * no-parameter constructor.
	 */
	public CypherTokenMaker() {
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 * @see #addToken(int, int, int)
	 */
	private void addHyperlinkToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so, true);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 */
	private void addToken(int tokenType) {
		addToken(zzStartRead, zzMarkedPos-1, tokenType);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 * @see #addHyperlinkToken(int, int, int)
	 */
	private void addToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so, false);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param array The character array.
	 * @param start The starting offset in the array.
	 * @param end The ending offset in the array.
	 * @param tokenType The token's type.
	 * @param startOffset The offset in the document at which this token
	 *        occurs.
	 * @param hyperlink Whether this token is a hyperlink.
	 */
	public void addToken(char[] array, int start, int end, int tokenType,
						int startOffset, boolean hyperlink) {
		super.addToken(array, start,end, tokenType, startOffset, hyperlink);
		zzStartRead = zzMarkedPos;
	}


	/**
	 * {@inheritDoc}
	 */
	public String[] getLineCommentStartAndEnd(int languageIndex) {
		return new String[] { "//", null };
	}


	/**
	 * Returns the first token in the linked list of tokens generated
	 * from <code>text</code>.  This method must be implemented by
	 * subclasses so they can correctly implement syntax highlighting.
	 *
	 * @param text The text from which to get tokens.
	 * @param initialTokenType The token type we should start with.
	 * @param startOffset The offset into the document at which
	 *        <code>text</code> starts.
	 * @return The first <code>Token</code> in a linked list representing
	 *         the syntax highlighted text.
	 */
	public Token getTokenList(Segment text, int initialTokenType, int startOffset) {

		resetTokenList();
		this.offsetShift = -text.offset + startOffset;

		// Start off in the proper state.
		int state = Token.NULL;
		switch (initialTokenType) {
			/* No multi-line comments */
			/* No documentation comments */
			default:
				state = Token.NULL;
		}

		s = text;
		try {
			yyreset(zzReader);
			yybegin(state);
			return yylex();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return new TokenImpl();
		}

	}


	/**
	 * Refills the input buffer.
	 *
	 * @return      <code>true</code> if EOF was reached, otherwise
	 *              <code>false</code>.
	 */
	private boolean zzRefill() {
		return zzCurrentPos>=s.offset+s.count;
	}


	/**
	 * Resets the scanner to read from a new input stream.
	 * Does not close the old reader.
	 *
	 * All internal variables are reset, the old input stream 
	 * <b>cannot</b> be reused (internal buffer is discarded and lost).
	 * Lexical state is set to <tt>YY_INITIAL</tt>.
	 *
	 * @param reader   the new input stream 
	 */
	public final void yyreset(Reader reader) {
		// 's' has been updated.
		zzBuffer = s.array;
		/*
		 * We replaced the line below with the two below it because zzRefill
		 * no longer "refills" the buffer (since the way we do it, it's always
		 * "full" the first time through, since it points to the segment's
		 * array).  So, we assign zzEndRead here.
		 */
		//zzStartRead = zzEndRead = s.offset;
		zzStartRead = s.offset;
		zzEndRead = zzStartRead + s.count - 1;
		zzCurrentPos = zzMarkedPos = zzPushbackPos = s.offset;
		zzLexicalState = YYINITIAL;
		zzReader = reader;
		zzAtBOL  = true;
		zzAtEOF  = false;
	}


%}

Letter							= [A-Za-z]
LetterOrUnderscore				= ({Letter}|"_")
NonzeroDigit						= [1-9]
Digit							= ("0"|{NonzeroDigit})
HexDigit							= ({Digit}|[A-Fa-f])
OctalDigit						= ([0-7])
AnyCharacterButApostropheOrBackSlash	= ([^\\'])
AnyCharacterButDoubleQuoteOrBackSlash	= ([^\\\"\n])
EscapedSourceCharacter				= ("u"{HexDigit}{HexDigit}{HexDigit}{HexDigit})
Escape							= ("\\"(([btnfr\"'\\])|([0123]{OctalDigit}?{OctalDigit}?)|({OctalDigit}{OctalDigit}?)|{EscapedSourceCharacter}))
NonSeparator						= ([^\t\f\r\n\ \(\)\{\}\[\]\;\,\.\=\>\<\!\~\?\:\+\-\*\/\&\|\^\%\"\']|"#"|"\\")
IdentifierStart					= ({LetterOrUnderscore}|"$")
IdentifierPart						= ({IdentifierStart}|{Digit}|("\\"{EscapedSourceCharacter}))

LineTerminator				= (\n)
WhiteSpace				= ([ \t\f]+)

CharLiteral	= ([\']({AnyCharacterButApostropheOrBackSlash}|{Escape})[\'])
UnclosedCharLiteral			= ([\'][^\'\n]*)
ErrorCharLiteral			= ({UnclosedCharLiteral}[\'])
StringLiteral				= ([\"]({AnyCharacterButDoubleQuoteOrBackSlash}|{Escape})*[\"])
UnclosedStringLiteral		= ([\"]([\\].|[^\\\"])*[^\"]?)
ErrorStringLiteral			= ({UnclosedStringLiteral}[\"])

/* No multi-line comments */
/* No documentation comments */
LineCommentBegin			= "//"

IntegerLiteral			= ({Digit}+)
HexLiteral			= (0x{HexDigit}+)
FloatLiteral			= (({Digit}+)("."{Digit}+)?(e[+-]?{Digit}+)? | ({Digit}+)?("."{Digit}+)(e[+-]?{Digit}+)?)
ErrorNumberFormat			= (({IntegerLiteral}|{HexLiteral}|{FloatLiteral}){NonSeparator}+)
BooleanLiteral				= ("true"|"false")

Separator					= ([\(\)\{\}\[\]])
Separator2				= ([\;,.])

Identifier				= ({IdentifierStart}{IdentifierPart}*)

URLGenDelim				= ([:\/\?#\[\]@])
URLSubDelim				= ([\!\$&'\(\)\*\+,;=])
URLUnreserved			= ({LetterOrUnderscore}|{Digit}|[\-\.\~])
URLCharacter			= ({URLGenDelim}|{URLSubDelim}|{URLUnreserved}|[%])
URLCharacters			= ({URLCharacter}*)
URLEndCharacter			= ([\/\$]|{Letter}|{Digit})
URL						= (((https?|f(tp|ile))"://"|"www.")({URLCharacters}{URLEndCharacter})?)


/* No string state */
/* No char state */
/* No MLC state */
/* No documentation comment state */
%state EOL_COMMENT

%%

<YYINITIAL> {

	/* Keywords */
	"ADD" |
"ALL" |
"AND" |
"AS" |
"ASC" |
"ASCENDING" |
"ASSERT" |
"BY" |
"CALL" |
"CASE" |
"CONSTRAINT" |
"CONTAINS" |
"COUNT" |
"CREATE" |
"CREATE" |
"CSV" |
"DELETE" |
"DESC" |
"DESCENDING" |
"DETACH" |
"DISTINCT" |
"DO" |
"DROP" |
"ELSE" |
"END" |
"ENDS" |
"EXISTS" |
"EXISTS" |
"FOR" |
"FOREACH" |
"IN" |
"INDEX" |
"INDEX" |
"IS" |
"JOIN" |
"KEY" |
"LIMIT" |
"LOAD" |
"MANDATORY" |
"MATCH" |
"MERGE" |
"NODE" |
"NOT" |
"OF" |
"ON" |
"OPTIONAL" |
"OR" |
"ORDER" |
"REMOVE" |
"REQUIRE" |
"RETURN" |
"SCALAR" |
"SCAN" |
"SET" |
"SKIP" |
"START" |
"STARTS" |
"THEN" |
"UNION" |
"UNIQUE" |
"UNWIND" |
"USING" |
"WHEN" |
"WHERE" |
"WITH" |
"XOR" |
"YIELD" |
"false" |
"null" |
"true"		{ addToken(Token.RESERVED_WORD); }

	/* Keywords 2 (just an optional set of keywords colored differently) */
	/* No keywords 2 */

	/* Data types */
	"ANY" |
"ANY VALUE" |
"ARRAY" |
"BOOL" |
"BOOLEAN" |
"DATE" |
"DURATION" |
"EDGE" |
"FLOAT" |
"INT" |
"INTEGER" |
"LIST" |
"LOCAL DATETIME" |
"LOCAL TIME" |
"MAP" |
"NODE" |
"NOTHING" |
"NULL" |
"PATH" |
"POINT" |
"PROPERTY VALUE" |
"RELATIONSHIP" |
"SIGNED INTEGER" |
"STRING" |
"TIME WITH TIMEZONE" |
"TIME WITHOUT TIMEZONE" |
"TIMESTAMP WITH TIMEZONE" |
"TIMESTAMP WITHOUT TIMEZONE" |
"VARCHAR" |
"VERTEX" |
"ZONED DATETIME" |
"ZONED TIME"		{ addToken(Token.DATA_TYPE); }

	/* Functions */
	"abs" |
"acos" |
"all" |
"any" |
"asin" |
"atan" |
"atan2" |
"avg" |
"ceil" |
"char_length" |
"character_length" |
"coalesce" |
"collect" |
"cos" |
"cot" |
"count" |
"date" |
"date.realtime" |
"date.statement" |
"date.transaction" |
"date.truncate" |
"datetime" |
"datetime.fromepoch" |
"datetime.fromepochmillis" |
"datetime.realtime" |
"datetime.statement" |
"datetime.transaction" |
"datetime.truncate" |
"db.nameFromElementId" |
"degrees" |
"duration" |
"duration.between" |
"duration.inDays" |
"duration.inMonths" |
"duration.inSeconds" |
"e" |
"endNode" |
"exists" |
"exp" |
"file" |
"floor" |
"graph.byElementId" |
"graph.byName" |
"graph.names" |
"graph.propertiesByName" |
"haversin" |
"head" |
"id" |
"isEmpty" |
"isNaN" |
"keys" |
"labels" |
"last" |
"left" |
"length" |
"linenumber" |
"localdatetime" |
"localdatetime.realtime" |
"localdatetime.statement" |
"localdatetime.transaction" |
"localdatetime.truncate" |
"localtime" |
"localtime.realtime" |
"localtime.statement" |
"localtime.transaction" |
"localtime.truncate" |
"log" |
"log10" |
"ltrim" |
"max" |
"min" |
"nodes" |
"none" |
"nullIf" |
"percentileCont" |
"percentileDisc" |
"pi" |
"point" |
"point.distance" |
"point.withinBBox" |
"properties" |
"radians" |
"rand" |
"randomUUID" |
"range" |
"reduce" |
"relationships" |
"replace" |
"reverse" |
"right" |
"round" |
"rtrim" |
"sign" |
"sin" |
"single" |
"size" |
"split" |
"sqrt" |
"startNode" |
"stdev" |
"stdevp" |
"substring" |
"sum" |
"tail" |
"tan" |
"time" |
"time.realtime" |
"time.statement" |
"time.transaction" |
"time.truncate" |
"toBoolean" |
"toBooleanList" |
"toBooleanOrNull" |
"toFloat" |
"toFloatList" |
"toFloatOrNull" |
"toInteger" |
"toIntegerList" |
"toIntegerOrNull" |
"toLower" |
"toString" |
"toStringList" |
"toStringOrNull" |
"toUpper" |
"trim" |
"type" |
"valueType"		{ addToken(Token.FUNCTION); }

	{BooleanLiteral}			{ addToken(Token.LITERAL_BOOLEAN); }

	{LineTerminator}				{ addNullToken(); return firstToken; }

	{Identifier}					{ addToken(Token.IDENTIFIER); }

	{WhiteSpace}					{ addToken(Token.WHITESPACE); }

	/* String/Character literals. */
	{CharLiteral}				{ addToken(Token.LITERAL_CHAR); }
{UnclosedCharLiteral}		{ addToken(Token.ERROR_CHAR); addNullToken(); return firstToken; }
{ErrorCharLiteral}			{ addToken(Token.ERROR_CHAR); }
	{StringLiteral}				{ addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }
{UnclosedStringLiteral}		{ addToken(Token.ERROR_STRING_DOUBLE); addNullToken(); return firstToken; }
{ErrorStringLiteral}			{ addToken(Token.ERROR_STRING_DOUBLE); }

	/* Comment literals. */
	/* No multi-line comments */
	/* No documentation comments */
	{LineCommentBegin}			{ start = zzMarkedPos-2; yybegin(EOL_COMMENT); }

	/* Separators. */
	{Separator}					{ addToken(Token.SEPARATOR); }
	{Separator2}					{ addToken(Token.IDENTIFIER); }

	/* Operators. */
	"%" |
"*" |
"+" |
"-" |
"/" |
"<" |
"<=" |
"<>" |
"=" |
"=~" |
">" |
">=" |
"AND" |
"CONTAINS" |
"DISTINCT" |
"ENDS WITH" |
"IS" |
"NOT" |
"OR" |
"STARTS WITH" |
"XOR" |
"^"		{ addToken(Token.OPERATOR); }

	/* Numbers */
	{IntegerLiteral}				{ addToken(Token.LITERAL_NUMBER_DECIMAL_INT); }
	{HexLiteral}					{ addToken(Token.LITERAL_NUMBER_HEXADECIMAL); }
	{FloatLiteral}					{ addToken(Token.LITERAL_NUMBER_FLOAT); }
	{ErrorNumberFormat}			{ addToken(Token.ERROR_NUMBER_FORMAT); }

	/* Ended with a line not in a string or comment. */
	<<EOF>>						{ addNullToken(); return firstToken; }

	/* Catch any other (unhandled) characters. */
	.							{ addToken(Token.IDENTIFIER); }

}


/* No char state */

/* No string state */

/* No multi-line comment state */

/* No documentation comment state */

<EOL_COMMENT> {
	[^hwf\n]+				{}
	{URL}					{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_EOL); addHyperlinkToken(temp,zzMarkedPos-1, Token.COMMENT_EOL); start = zzMarkedPos; }
	[hwf]					{}
	\n						{ addToken(start,zzStartRead-1, Token.COMMENT_EOL); addNullToken(); return firstToken; }
	<<EOF>>					{ addToken(start,zzStartRead-1, Token.COMMENT_EOL); addNullToken(); return firstToken; }
}

