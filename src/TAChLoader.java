// File: TAChLoader.java
// Author: K R Sloan
// Last Modified: 21 December 2014
// Purpose: program loader for TACh
//   This version has speed control for the CPU
//   Multi-threading the Memory makes it too fast!
//   So...we now have global speed control, and soon will have
//   a SVC instruction to WAIT under TACh program control
import java.util.regex.Pattern;
import java.util.Scanner;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
public class TAChLoader
{
    private TAChCPU cpu;

    public String toString()
    {
	return "\n" + this.cpu;
    }
    public TAChLoader(TAChCPU cpu)
    {
	this.cpu = cpu;
    }

    public void load(InputStream in)
    {
	Pattern hexPattern = Pattern.compile("[0-9a-fA-F]{8}");
	Scanner s = new Scanner(in);
	int addr = 0;
	TAChMemory sm = this.cpu.getMemory();
	while(s.hasNextLine())
	    {
		String line = s.nextLine();
		Scanner sl = new Scanner(line);
		if (!sl.hasNext(hexPattern)) continue;
		String token = sl.next().toUpperCase();
		int instruction = (int)Long.parseLong(token,16);
		sm.store(addr,instruction);
		addr++;
	    }
    }

    public void load(int[] program)
    {
	TAChMemory sm = this.cpu.getMemory();
	for(int addr=0; addr<program.length; addr++) sm.store(addr,program[addr]);
    }

    //    public void run(int addr)
    //    {
    //	this.cpu.run(addr);
    //    }

    private static int[]program1 = {
	0x00101111, // 00  ADD 10,11,11
	0xc0070809, // 01  ERR 7,8,9
	0x00101110, // 02  ADD 10,11,10
	0xc00a0b0c, // 03  ERR A,B,C
	0xd00d0e00, // 04  BR  D,E,0
	0xf0000000, // 05  HALT
	0xf0000000, // 06  HALT
	0xf0000000, // 07  HALT
	0xf0000000, // 08  HALT
	0xf0000000, // 09  HALT
	0xf0000000, // 0a  HALT
	0xf0000000, // 0b  HALT
	0xf0000000, // 0c  HALT
	0xf0000000, // 0d  HALT
	0xf0000000, // 0e  HALT
	0xf0000000, // 0f  HALT
	0x00000001, // 10  DC 1
	0x00000001  // 11  DC 1
    };

    private static int[]Fibonacci = {
	0x0e0a0b0c, // 00 LOOP   ADD [A],[B],[C]
	0xc0070707, // 01        ERR FAIL,FAIL,FAIL
	0x000a090a, // 02        ADD A,ONE,A
	0x000b090b, // 03        ADD B,ONE,B
	0x000c090c, // 04        ADD C,ONE,C
	0x100d0c0e, // 05        SUB D,C,TMP
	0xd0080000, // 06        BR  EXIT,LOOP,LOOP
	0xf0000000, // 07 FAIL   HALT
	0xf0000000, // 08 EXIT   HALT
	0x00000001, // 09 ONE    DC   1
	0x00000010, // 0a A      DC   10
	0x00000011, // 0b B      DC   11
	0x00000012, // 0c C      DC   12
	0x000000ff, // 0d END    DC   FF
	0x00000000, // 0e TMP    DC   0
	0xffffffff, // 0f        DC   FFFFFFFF
	0x00000001, // 10 F0     DC   1
	0x00000001, // 11 F1     DC   1
	0x00000000, // 12 F2     DC   0
    };

    private static int[] Horner = {
	0xE0010420, // 00         CALL  RETURN1,DATA1,POLY
	0xE0020820, // 01 RETURN1 CALL  RETURN2,DATA2,POLY 
	0xE0030D20, // 02 RETURN2 CALL  RETURN3,DATA3,POLY
	0xF0000000, // 03 RETURN3 HALT 0,0,0
	0x00000000, // 04 DATA1   DC   0
	0x00000002, // 05 X1      DC   2
	0x00000001, // 06 N1      DC   1
	0x00000005, // 07 TABLE1  DC   5
	0x00000000, // 08 DATA2   DC   0
	0x00000002, // 09 X2      DC   2
	0x00000002, // 0A N2      DC   2
	0x00000005, // 0B TABLE2  DC   5
	0x00000008, // 0C         DC   8
	0x00000000, // 0D DATA3   DC   0
	0x00000002, // 0E X3      DC   2
	0x00000003, // 0F N3      DC   3
	0x00000005, // 10 TABLE3  DC   5
	0x00000008, // 11         DC   8
	0x0000000D, // 12         DC   D
	0x00000000, // 13
	0x00000000, // 14
	0x00000000, // 15
	0x00000000, // 16
	0x00000000, // 17
	0x00000000, // 18
	0x00000000, // 19
	0x00000000, // 1A
	0x00000000, // 1B
	0x00000000, // 1C
	0x00000000, // 1D
	0x00000000, // 1E
	0x00000000, // 1F
	0x00000000, // 20 RETURN  DC   0
	0x00000000, // 21 PDATA   DC   0
	0x12333321, // 22 ENTRY   SUB  ONE,ONE,[PDATA]
	0x00213334, // 23         ADD  PDATA,ONE,PX
	0x00343335, // 24         ADD  PX,ONE,PN
	0x00353336, // 25         ADD  PN,ONE,PTABLE
	0x10363337, // 26         SUB  PTABLE,ONE,PT
	0x04373538, // 27         ADD  PT,[PN],LAST
	0x00373337, // 28 LOOP    ADD  PT,ONE,PT
	0x10373832, // 29         SUB  PT,LAST,TMP
	0xD02B2B31, // 2A         BR   MORE,MORE,EXIT
	0x2E213421, // 2B MORE    MUL  [PDATA],[PX],[PDATA]
	0xC0303030, // 2C         ERR  FAIL,FAIL,FAIL
	0x0E213721, // 2D         ADD  [PDATA],[PT],[PDATA]
	0xC0303030, // 2E         ERR  FAIL,FAIL,FAIL
	0xD0282828, // 2F         BR   LOOP,LOOP,LOOP
	0xF0000000, // 30 FAIL    HALT 0,0,0
	0xDE202020, // 31 EXIT    BR   [RETURN],[RETURN],[RETURN]
	0x00000000, // 32 TMP     DC   0
	0x00000001, // 33 ONE     DC   1
	0x00000000, // 34 PX      DC   0
	0x00000000, // 35 PN      DC   0
	0x00000000, // 36 PTABLE  DC   0
	0x00000000, // 37 PT      DC   0
	0x00000000, // 38 LAST    DC   0
	0x00000000  // 39 TMP     DC   0
    };
    
    public static void main(String[] args)
	throws FileNotFoundException
    {
	// in this version, remove the option to use a canned program
	// the Assembler now works, so require one argment for the
	// program name (no extension - default to TACh)
	// and allow a second argument for global speed control
	// speed is in nanoseconds per instruction
	//	int[]program = Fibonacci;  // default program
	int speed = 0;       // as fast as possible
	int startAddr = 0;   // begin execution here
	if (0 == args.length) throw new RuntimeException("need a program to load");
	String programName = args[0] + ".TACh";

	// speed control is flakey (FIX!)
	// by experiment, values between 0 and 50000 work
	// enforce this!

	if (1 < args.length) speed = Integer.parseInt(args[1]);
	if (speed < 0) 
	    {
		System.err.printf("speed %d reset to 0\n",speed);
		speed = 0;
	    }
	else if (speed > 50000)
	    {
		System.err.printf("speed %d reset fo 50000\n",speed);
		speed = 50000;
	    }

	TAChMemory tm = new TAChMemory();
	Thread t = new Thread(tm);  // Memory runs in parallel
	t.start();
	TAChCPU cpu = new TAChCPU(tm);

	Thread tcpu = new Thread(cpu);

	TAChLoader loader = new TAChLoader(cpu);
	loader.load(new FileInputStream(programName));

	cpu.setPC(startAddr);
	cpu.setSpeed(speed);
	tcpu.start(); 

	//	if (0 < args.length) loader.load(new FileInputStream(args[0]));
	//	else 	             loader.load(program);
    }
}