// File: TAChAssembler.java
// Author: K R Sloan
// Last Modified: 21 December 2014
// Purpose: assembler for TACh
// Input: assembler language
// Output: a) .TACh file suitable for SexaTACloader
//         b) listing (to stdout)
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.TreeMap;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.FileNotFoundException;
public class TAChAssembler
{
    private static Map<String,Integer>  memoryUsed;
    private static Map<String,Integer> initializeMemoryUsed()
    {
	Map<String,Integer> result = new TreeMap<String,Integer>();
	result.put("ADD",1);
	result.put("ADDI",1);
	result.put("SUB",1);
	result.put("SUBI",1);
	result.put("MUL",1);
	result.put("MULI",1);
	result.put("DIV",1);
	result.put("DIVI",1);
	result.put("FADD",1);
	result.put("FADDI",1);
	result.put("FSUB",1);
	result.put("FSUBI",1);
	result.put("FMUL",1);
	result.put("FMULI",1);
	result.put("FDIV",1);
	result.put("FDIVI",1);
	result.put("AND",1);
	result.put("ANDI",1);
	result.put("OR",1);
	result.put("ORI",1);
	result.put("XOR",1);
	result.put("XORI",1);
	result.put("ROT",1);
	result.put("ROTI",1);
	result.put("ERR",1);
	result.put("BR",1);
	result.put("CALL",1);
	result.put("HALT",1);
        result.put("PDUMP",1);
        result.put("TRACE",1);
	result.put("PAUSE",1);
	result.put("DC",1);
        result.put("LABEL",0);
	return result;
    }
    private static int memoryUsedBy(String opCode)
    {
	if (memoryUsed.containsKey(opCode)) return memoryUsed.get(opCode);
	else return 0;
    }
    
    private static Map<String,Integer> symbolTable;
    
    public static void assemble(InputStream pgm, 
				PrintStream TACh,
				PrintStream TAChList)
    {
	Scanner sc = new Scanner(pgm);
	List<TAChAsmLine> deck = new ArrayList<TAChAsmLine>();
	memoryUsed = initializeMemoryUsed();
	symbolTable = new TreeMap<String, Integer>();
	int location = 0;
	// pass 1 - parse input and fill symbol table
	while(sc.hasNextLine())
	    {
		String cardImage = sc.nextLine();
		if (0 ==cardImage.length()) continue;
		TAChAsmLine line = new TAChAsmLine(cardImage,location);
		deck.add(line);
		String label = line.getLabel();
		if (!(label.equals("")))
		{
		    if (symbolTable.containsKey(label))
			{
			    line.getErrors().add(String.format("%s already defined!",label));
			}
		    symbolTable.put(label,location);
		}
		location += memoryUsedBy(line.getOpCode());
	    }

	// pass 2 - assemble instructions and output listing

	int memoryLocation = 0;
	for(TAChAsmLine line : deck)
	    {
		if (line.getCommentCard())
		    {
			TAChList.printf("            %s\n",line.getCardImage());
			TACh.printf("            %s\n",line.getCardImage());
			continue;
		    }

		String opCode = line.getOpCode();
		if      (opCode.equals("ADD"))  add(line);
		else if (opCode.equals("ADDI")) addi(line);
		else if (opCode.equals("SUB"))  sub(line);
		else if (opCode.equals("SUBI")) subi(line);
		else if (opCode.equals("MUL"))  mul(line);
		else if (opCode.equals("MULI")) muli(line);
		else if (opCode.equals("DIV"))  div(line);
		else if (opCode.equals("DIVI")) divi(line);
		else if (opCode.equals("FADD")) fadd(line);
		else if (opCode.equals("FADDI"))faddi(line);
		else if (opCode.equals("FSUB")) fsub(line);
		else if (opCode.equals("FSUBI"))fsubi(line);
		else if (opCode.equals("FMUL")) fmul(line);
		else if (opCode.equals("FMULI"))fmuli(line);
		else if (opCode.equals("FDIV")) fdiv(line);
		else if (opCode.equals("FDIVI"))fdivi(line);
		else if (opCode.equals("AND"))  and(line);
		else if (opCode.equals("ANDI")) andi(line);
		else if (opCode.equals("OR"))   or(line);
		else if (opCode.equals("ORI"))  ori(line);
		else if (opCode.equals("XOR"))  xor(line);
		else if (opCode.equals("XORI")) xori(line);
		else if (opCode.equals("ROT"))  rot(line);
		else if (opCode.equals("ROTI")) roti(line);
		else if (opCode.equals("ERR"))  err(line);
		else if (opCode.equals("BR"))   br(line);
		else if (opCode.equals("CALL")) call(line);
		else if (opCode.equals("HALT")) halt(line);
                else if (opCode.equals("PDUMP"))pdump(line);
                else if (opCode.equals("TRACE"))trace(line);
                else if (opCode.equals("PAUSE"))pause(line);
		else if (opCode.equals("SVC"))  svc(line);
		else if (opCode.equals("DC"))   dc(line);
		else if (opCode.equals("LABEL"))label(line);
		else line.getErrors().add(String.format("%s is not a valid OpCode!",opCode));

		if (line.getHasInstruction())
		    {
			TACh.printf("%08x %02x %s\n",
				    line.getInstruction(),
				    line.getMemoryLocation(),
				    line.getCardImage());
			
			TAChList.printf("%08x %02x %-10s %-5s %s %s\n",
					line.getInstruction(),
					line.getMemoryLocation(),
					line.getLabel(), 
					line.getOpCode(),
					line.getAddresses(),
					line.getRestOfLine());
		    }
		else
		    {
			TACh.printf("         %02x %s\n",
				    line.getMemoryLocation(),
				    line.getCardImage());
			
			TAChList.printf("         %02x %-10s %-5s %s %s\n",
					line.getMemoryLocation(),
					line.getLabel(), 
					line.getOpCode(),
					line.getAddresses(),
					line.getRestOfLine());
		    }


		for(String error : line.getErrors())
		    {
			TAChList.printf("\n***** ERROR: %s *****\n\n",error);
		    }
		for(String warning : line.getWarnings())
		    {
			TAChList.printf("\n*****WARNING: %s\n *****\n\n",warning);
		    }

	    }
    }
    
    private static void threeAddressInstruction(String opCode,
						int opCodeBits,
						TAChAsmLine line)
    {
	String[] address = line.getAddress();
	if ((null == address) || (3 != address.length))
	    {
		line.addError(String.format("%s requires 3 addresses",opCode));
		return;
	    }
	boolean[] indirect = line.getIndirect();
	
	int addressBits = 0;
	int indirectBits = 0;
	for(int i = 0; i < 3; i++)
	    {
		String addr = address[i];
		int addrLocation = 0;
		// addr can be a label, but it can also be:
		//    - an integer in decimal notation (or anything parsable?)
		//    - * (meaning - the current location) 
		//    - expressions, e.g. "*+1", would be nice - left as an extension
		if (!symbolTable.containsKey(addr))
		    {
			Scanner sc = new Scanner(addr);
			if (sc.hasNextInt())
			    {
				addrLocation = sc.nextInt();
			    }
			else
			    {
				line.addError(String.format("%s not defined",addr));
			    }
		    }
		else
		    {
			addrLocation = symbolTable.get(addr);
		    }
		addressBits = (addressBits << 8) | (addrLocation & 0xff);
		int indirectBit = 0;
		if (indirect[i]) indirectBit = 1;
		indirectBits = (indirectBits << 1) | indirectBit;
	    }
	int instruction = (opCodeBits << 28) | (indirectBits << 25) | addressBits;
	line.setInstruction(instruction);
    }
 
    private static void immediateInstruction(String opCode,
						int opCodeBits,
						TAChAsmLine line)
    {
	String[] address = line.getAddress();
	if ((null == address) || (2 != address.length))
	    {
		line.addError(String.format(
			"%s requires address + immediate constant",opCode));
		return;
	    }
	boolean[] indirect = line.getIndirect();
	int addressBits = 0;
	for(int i = 0; i < 2; i++)
	    {
		String addr = address[i];
		int addrLocation = 0;
		// addr can be a label, but it can also be:
		//    - an integer in decimal notation (or anything parsable?)
		//    - * (meaning - the current location) 
		//    - expressions, e.g. "*+1", would be nice - left as an extension
		if (!symbolTable.containsKey(addr))
		    {
			Scanner sc = new Scanner(addr);
			if (sc.hasNextInt())
			    {
				addrLocation = sc.nextInt();
			    }
			else
			    {
				line.addError(String.format("%s not defined",addr));
			    }
		    }
		else
		    {
			addrLocation = symbolTable.get(addr);
		    }
		addressBits = (addressBits << 16) | (addrLocation & 0xffff);
	    }
	addressBits = addressBits & 0xffffff;
	int indirectBits = 1;
	if(indirect[0]) indirectBits = 9;
	if(indirect[1]) line.addError(String.format(
			    "second address may not be indirect"));
	int instruction = (opCodeBits << 28) | (indirectBits << 24) | addressBits;
	line.setInstruction(instruction);
    }
 
   private static void svcTwoAddressInstruction(String opCode,
						int AAbits,
						TAChAsmLine line)
    {
	int opCodeBits = 0x0f;
	String[] address = line.getAddress();
	if ((null == address) || (2 != address.length))
	    {
		line.addError(String.format("%s requires 2 addresses",opCode));
		return;
	    }
	boolean[] indirect = line.getIndirect();
	
	int addressBits = 0;
	int indirectBits = 0;
	for(int i = 0; i < 2; i++)
	    {
		String addr = address[i];
		int addrLocation = 0;
		// addr can be a label, but it can also be:
		//    - an integer in decimal notation (or anything parsable?)
		//    - * (meaning - the current location) 
		//    - expressions, e.g. "*+1", would be nice - left as an extension
		if (!symbolTable.containsKey(addr))
		    {
			Scanner sc = new Scanner(addr);
			if (sc.hasNextInt())
			    {
				addrLocation = sc.nextInt();
			    }
			else
			    {
				line.addError(String.format("%s not defined",addr));
			    }
		    }
		else
		    {
			addrLocation = symbolTable.get(addr);
		    }
		addressBits = (addressBits << 8) | addrLocation;
		int indirectBit = 0;
		if (indirect[i]) indirectBit = 1;
		indirectBits = (indirectBits << 1) | indirectBit;
	    }
	int instruction = (opCodeBits << 28) | (indirectBits << 25)
	    | AAbits << 16 |  addressBits;
	line.setInstruction(instruction);
    }


    private static void svcOneAddressInstruction(String opCode,
						int AAbits,
						TAChAsmLine line)
    {
	int opCodeBits = 0x0f;
	String[] address = line.getAddress();
	if ((null == address) || (1 != address.length))
	    {
		line.addError(String.format("%s requires 1 address",opCode));
		return;
	    }
	boolean[] indirect = line.getIndirect();
	
	int addressBits = 0;
	int indirectBits = 0;
	String addr = address[0];
	int addrLocation = 0;
	if (!symbolTable.containsKey(addr))
	    {
		Scanner sc = new Scanner(addr);
		if (sc.hasNextInt())
		    {
			addrLocation = sc.nextInt();
		    }
		else
		    {
			line.addError(String.format("%s not defined",addr));
		    }
	    }
	else
	    {
		addrLocation = symbolTable.get(addr);
	    }
	addressBits = addrLocation << 8;  // The one address goes in the BB slot
	int indirectBit = 0;
	if (indirect[0]) indirectBit = 1;
	indirectBits = indirectBit << 1;
	int instruction = (opCodeBits << 28) | (indirectBits << 25) 
	    | AAbits << 16 | addressBits ;
	line.setInstruction(instruction);
}

    private static void fullWordConstant(String opCode,
					TAChAsmLine line)
    {
	String[] address = line.getAddress();
	if ((null == address) || (1 != address.length)) 
	    {
		line.addError(String.format("%s requires 1 value",opCode));
		return;
	    }
	boolean[] indirect = line.getIndirect();
	if(indirect[0])
	    {
		line.addError(String.format("%s value cannot be indirect",opCode));
		return;
	    }
	String addr = address[0];
	int addrLocation = 0;
	if (!symbolTable.containsKey(addr))
	    {
		Scanner sc = new Scanner(addr);
		if (sc.hasNextInt())
		    {
			addrLocation = sc.nextInt();
		    }
		else
		    {
			line.addError(String.format("%s not defined",addr));
		    }
	    }
	else
	    {
		addrLocation = symbolTable.get(addr);
	    }
	line.setInstruction(addrLocation);
    }

    private static void justLabel(String opCode,
				  TAChAsmLine line)
    {
	String[] address = line.getAddress();
	if (null != address)
	    {
		line.addError(String.format("%s requires 0 values",opCode));
		return;
	    }
    }



    private static void add(TAChAsmLine line)
    {
	threeAddressInstruction("ADD", 0x00, line);
    }
    private static void addi(TAChAsmLine line)
    {
	immediateInstruction("ADDI", 0x00, line);
    }
    private static void sub(TAChAsmLine line)
    {
	threeAddressInstruction("SUB", 0x01, line);
    }
    private static void subi(TAChAsmLine line)
    {
	immediateInstruction("SUBI", 0x01, line);
    }
    private static void mul(TAChAsmLine line)
    {
	threeAddressInstruction("MUL", 0x02, line);
    }
    private static void muli(TAChAsmLine line)
    {
	immediateInstruction("MULI", 0x02, line);
    }
    private static void div(TAChAsmLine line)
    {
	threeAddressInstruction("DIV", 0x03, line);
    }
    private static void divi(TAChAsmLine line)
    {
	immediateInstruction("DIVI", 0x03, line);
    }
    private static void fadd(TAChAsmLine line)
    {
	threeAddressInstruction("FADD", 0x04, line);
    }
    private static void faddi(TAChAsmLine line)
    {
	immediateInstruction("FADDI", 0x04, line);
    }
    private static void fsub(TAChAsmLine line)
    {
	threeAddressInstruction("FSUB", 0x05, line);
    }
    private static void fsubi(TAChAsmLine line)
    {
	immediateInstruction("FSUBI", 0x05, line);
    }
    private static void fmul(TAChAsmLine line)
    {
	threeAddressInstruction("FMUL", 0x06, line);
    }
    private static void fmuli(TAChAsmLine line)
    {
	immediateInstruction("FMULI", 0x06, line);
    }
    private static void fdiv(TAChAsmLine line)
    {
	threeAddressInstruction("FDIV", 0x07, line);
    }
    private static void fdivi(TAChAsmLine line)
    {
	immediateInstruction("FDIVI", 0x07, line);
    }
    private static void and(TAChAsmLine line)
    {
	threeAddressInstruction("AND", 0x08, line);
    }
    private static void andi(TAChAsmLine line)
    {
	immediateInstruction("FANDI", 0x08, line);
    }
    private static void or(TAChAsmLine line)
    {
	threeAddressInstruction("OR", 0x09, line);
    }
    private static void ori(TAChAsmLine line)
    {
	immediateInstruction("ORI", 0x09, line);
    }
    private static void xor(TAChAsmLine line)
    {
	threeAddressInstruction("XOR", 0x0A, line);
    }
    private static void xori(TAChAsmLine line)
    {
	immediateInstruction("XORI", 0x0A, line);
    }
    private static void rot(TAChAsmLine line)
    {
	threeAddressInstruction("ROT", 0x0B, line);
    }
    private static void roti(TAChAsmLine line)
    {
	immediateInstruction("ROTI", 0x0B, line);
    }
    private static void err(TAChAsmLine line)
    {
	threeAddressInstruction("ERR", 0x0C, line);
    }
    private static void br(TAChAsmLine line)
    {
	threeAddressInstruction("BR", 0x0D, line);
    }
    private static void call(TAChAsmLine line)
    {
	threeAddressInstruction("CALL", 0x0E, line);
    }
    private static void halt(TAChAsmLine line)
    {
	svcTwoAddressInstruction("HALT", 0x00, line);
    }
    private static void pdump(TAChAsmLine line)
    {
	svcTwoAddressInstruction("PDUMP", 0x01, line);
    }
    private static void trace(TAChAsmLine line)
    {
	svcOneAddressInstruction("TRACE", 0x02, line);
    }
    private static void pause(TAChAsmLine line)
    {
	svcOneAddressInstruction("PAUSE", 0x03, line);
    }
    private static void dc(TAChAsmLine line)
    {
	fullWordConstant("DC", line);
    }
    private static void label(TAChAsmLine line)
    {
	justLabel("LABEL", line);
    }
    private static void svc(TAChAsmLine line)
    {
	threeAddressInstruction("SVC", 0x0F, line);
    }

    public static void main(String[] args)
	throws FileNotFoundException
    {
	// one argument - file name of input file
	String pgmFilename = args[0].split("\\.")[0];
	InputStream pgm = new FileInputStream(pgmFilename + ".sasm");
	PrintStream TACh = new PrintStream(pgmFilename + ".TACh");
	PrintStream  TAChList = new PrintStream(pgmFilename + ".TAChList");
	assemble(pgm,TACh,TAChList);
    }
}