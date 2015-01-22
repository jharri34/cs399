// File: TAChAsmLine.java
// Author: K R Sloan
// Last Modified: 16 December 2014
// Purpose: one line in assembler for TACh
//          Each line is a separate object
//          Collect in one place the input and info generated
//          by the assembler.  Provide formatted output for
//          listing and assembled output
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.FileNotFoundException;
public class TAChAsmLine
{
    private String cardImage;
    private boolean commentCard;
    private boolean hasInstruction;
    private int memoryLocation;
    private int instruction;
    private String label;
    private String opCode;
    private String addresses;
    private String[] address;
    private boolean[] indirect;
    private String restOfLine;
    private List<String> warnings;
    private List<String> errors;

    public String       getCardImage()            { return this.cardImage; }
    public boolean      getCommentCard()          { return this.commentCard; }
    public boolean      getHasInstruction()       { return this.hasInstruction; }
    public int          getMemoryLocation()       { return this.memoryLocation; }
    public int          getInstruction ()         { return this.instruction; }
    public String       getLabel()                { return this.label; }
    public String       getOpCode()               { return this.opCode; }
    public String       getAddresses()            { return this.addresses; }
    public String[]     getAddress()              { return this.address; }
    public boolean[]    getIndirect()             { return this.indirect; }
    public String       getRestOfLine()           { return this.restOfLine; }
    public void         setInstruction(int instr) 
    {
	this.instruction = instr; 
	this.hasInstruction = true;
    }
    public List<String> getWarnings()        { return this.warnings; }
    public List<String> getErrors()          { return this.errors; }
    public void addError(String error)
    {
	this.errors.add(error);
    }
    public void addWarning(String error)
    {
	this.warnings.add(error);
    }
    public TAChAsmLine(String input, int location)
    {
	this.cardImage = input;
	if (0 == this.cardImage.length()) this.cardImage = "*";
	this.memoryLocation = location;
	this.hasInstruction = false;
	this.instruction = 0;
	this.warnings = new ArrayList<String>();
	this.errors = new ArrayList<String>();
	if ('*' == this.cardImage.charAt(0))
	    { // comment card
		this.commentCard = true;
		this.hasInstruction = false;
		this.instruction = 0;
		this.label = "";
		this.opCode = "";
		this.addresses = "";
		this.address = null;
		this.indirect = null;
		this.restOfLine = "";
		return;
	    }

	this.commentCard = false;

	// parse and assemble non-comment card
	// extract label, if any
	this.label = "";
	int spaceX = this.cardImage.indexOf(' ');
	if(spaceX >= 0)	this.label = this.cardImage.substring(0,spaceX);
	else this.label = this.cardImage;

	// extract opCode
	// skip leading spaces
	this.opCode = "";
	int opX = spaceX;
	if (opX >= 0) 
	    {
		while(' ' == this.cardImage.charAt(opX)) opX++;
		spaceX = this.cardImage.indexOf(' ',opX);
		if (spaceX >= 0) this.opCode = this.cardImage.substring(opX,spaceX);
		else this.opCode = this.cardImage.substring(opX);
	    }

	// extract addresses
	this.addresses = "";
	int addrX = spaceX;
	if (addrX >= 0)
	    {
		while(' ' == this.cardImage.charAt(addrX)) addrX++;
		spaceX = this.cardImage.indexOf(' ',addrX);
		if (spaceX >= 0) this.addresses = this.cardImage.substring(addrX,spaceX);
		else 
		    {
			this.addresses = this.cardImage.substring(addrX);
		    }
	    }

	// remember the rest
	this.restOfLine = "";
	if (spaceX >= 0) this.restOfLine = this.cardImage.substring(spaceX);

	// parse the addresses

	if (!(this.addresses.equals("")))
	    {
		this.address = this.addresses.split(",");
		this.indirect = new boolean[this.address.length];
		for(int i = 0; i < this.address.length; i++)
		    {
			this.indirect[i] = false;
			String a = this.address[i];
			if (a.startsWith("[")
			    && a.endsWith("]"))
			    {
				this.indirect[i] = true;
				this.address[i]= a.substring(1,a.length()-1);
			    }
		    }
	    }

    }

    public static void main(String[] args)
	throws FileNotFoundException
    {
	String[]Fibonacci = {
	    "*",
	    "* Fibonacci sequence",
	    "*",
	    "LOOP   ADD [A],[B],[C]         A, B, C POINT TO F[I-2], F[I-1], F[I]",
	    "       ERR FAIL,FAIL,FAIL      ON OVERFLOW -> FAIL",
	    "       ADD A,ONE,A             INCREMENT POINTERS",
	    "       ADD B,ONE,B",
	    "       ADD C,ONE,C",
	    "       SUB END,C,TMP           COMPARE C to END",
	    "       BR  EXIT,LOOP,LOOP      IF C > D -> EXIT",
	    "FAIL   HALT 0                  OVERFLOW",
	    "EXIT   HALT -1                 FILLED MEMORY (HAH!)",
	    "ONE    DC   1",
	    "A      DC   F0                 -> F[I-2]",
	    "B      DC   F1                 -> F{I-1]",
	    "C      DC   F2                 -> F[I]",
	    "END    DC   255                LAST MEMORY ADDRESS",
	    "TMP    DC   0",
	    "       DC   -1                 MARKER FOR START OF SEQUENCE",
	    "F0     DC   1                  F[0]",
	    "F1     DC   1                  F[1]",
	    "F2     DC   0                  F[2]"
	};
	String[] program = Fibonacci;
	int location = 0;
	for(String cardImage : program)
	    {
		TAChAsmLine line = new TAChAsmLine(cardImage,location);
		if (line.getCommentCard())
		    System.out.printf("            %s\n",line.getCardImage());
		else if (line.getHasInstruction())
		    System.out.printf("%08x %02x %-10s %-5s %s %s\n",
				      line.getMemoryLocation(),
				      line.getInstruction(),
				      line.getLabel(), 
				      line.getOpCode(),
				      line.getAddresses(),
				      line.getRestOfLine());
		else 
		    System.out.printf("         %02x %-10s %-5s %s %s\n",
				      line.getMemoryLocation(),
				      line.getLabel(), 
				      line.getOpCode(),
				      line.getAddresses(),
				      line.getRestOfLine());
	    }
    }
}