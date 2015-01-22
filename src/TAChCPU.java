// File: TAChCPU.java
// Author: K R Sloan
// Last Modified: 21 December 2014
// Purpose: CPU for TACh
//          This version includes SVC (instead of HALT)
//          with subcases: HALT, DUMP, and TRACE
//   This version has speed control for the CPU
//   Multi-threading the Memory makes it too fast!
//   So...we now have global speed control, and soon will have
//   a SVC instruction to WAIT under TACh program control

public class TAChCPU implements Runnable
{
private TAChMemory tm;
private int speed;      // amount of time for each instruction, in nano-seconds
private int PC;
private boolean RUNNING;
private int instruction;
private int opCode;
private boolean[] flags;
private int[] addresses;
private int lastStoredValue;
private boolean underflow, overflow, divideByZero;
private boolean trace;
private int TAChOMETER;

public String toString()
{
        // Note: PC decremented to compensate for increment
        // report on the state of the machine as of the time the
        // last instruction was fetched
        String status = "ABEND!";
        if (this.RUNNING) status = "RUNNING";
        String result = status + " with PC = " + String.format("%02X",(PC-1))
                        + " Instruction = " + String.format("%08X",instruction)
                        + "    TAChOMETER = " + String.format("%08X",TAChOMETER)
                        + "\n"
                        + "lastStoredValue = " + String.format("%08X",lastStoredValue)
                        + "    underflow = " + String.format("%5B",underflow)
                        + "    overflow = " + String.format("%5B",overflow)
                        + "    divideByZero = " + String.format("%5B",divideByZero)
                        + "    tracing = " + String.format("%5B",trace)
                        + "\n"
                        + tm;
        return result;
}

public String toString(int lo, int hi)
{
        int startAddr = (lo/8)*8;
        int endAddr = 7 + (hi/8)*8;
        // Note: PC decremented to compensate for increment
        // report on the state of the machine as of the time the
        // last instruction was fetched
        String status = "ABEND!";
        if (this.RUNNING) status = "RUNNING";
        String result = status + " with PC = " + String.format("%02X",(PC-1))
                        + " Instruction = " + String.format("%08X",instruction)
                        + "    TAChOMETER = " + String.format("%08X",TAChOMETER)
                        + "\n"
                        + "lastStoredValue = " + String.format("%08X",lastStoredValue)
                        + "    underflow = " + String.format("%5B",underflow)
                        + "    overflow = " + String.format("%5B",overflow)
                        + "    divideByZero = " + String.format("%5B",divideByZero)
                        + "    tracing = " + String.format("%5B",trace)
                        + "\n"
                        + tm.toString(startAddr,endAddr);
        return result;
}

public TAChMemory getMemory() {
        return tm;
}

public TAChCPU(TAChMemory tm)
{
        this.tm = tm;
        this.PC = 0;
        this.speed = 0; // as fast as possible
        this.RUNNING = false;
        this.flags = new boolean[4];
        this.addresses = new int[3];
        this.lastStoredValue = 0;
        this.underflow = false;
        this.overflow = false;
        this.divideByZero = false;
        this.trace = false;
        this.TAChOMETER = 0;
}

private void parse()
{
        this.opCode = (instruction >> 28) & 0x0000000f;
        for(int f=0; f<4; f++)
        {
                flags[f] = ((instruction << f) & 0x08000000) > 0;
        }
        for(int a=0; a<3; a++)
        {
                addresses[a] = (instruction >> ((2-a)*8)) & 0xff;
        }
}

private void execute()
{
        switch(this.opCode)
        {
        case 0x0: add(); break;
        case 0x1: sub(); break;
        case 0x2: mul(); break;
        case 0x3: div(); break;
        case 0x4: fadd(); break;
        case 0x5: fsub(); break;
        case 0x6: fmul(); break;
        case 0x7: fdiv(); break;
        case 0x8: and(); break;
        case 0x9: or(); break;
        case 0xA: xor(); break;
        case 0xB: rot(); break;
        case 0xC: err(); break;
        case 0xD: br(); break;
        case 0xE: call(); break;
        default: System.err.printf("opCode = %x???\n",this.opCode);
                halt(0,255); break;
        case 0xF: svc(); break;
        }
}

private void clearConditions()
{
        this.overflow = false;
        this.underflow = false;
        this.divideByZero = false;
}

private int catchIntErrors(long x)
{
        if (x > Integer.MAX_VALUE)
        {
                this.overflow = true;
                return Integer.MAX_VALUE;
        }
        if (x < Integer.MIN_VALUE)
        {
                this.underflow = true;
                return Integer.MIN_VALUE;
        }
        return (int)x;
}

private int fetchOperand(int addr, boolean flag)
{
        int result = tm.fetch(addr);
        if (flag) result = tm.fetch(result & 0xff);
        return result;
}

private int fetchImmediate(int b, boolean bFlag,
                           int c, boolean cFlag)
{
        int bc = (b << 16) | (c & 0xff);
        if (!bFlag) bc = bc & 0xffff;
        if (cFlag)  bc = (new TAChFloat(4,bc)).getValue();
        return bc;
}


private int fetchAddress(int addr, boolean flag)
{
        int result = addr;
        if (flag) result = tm.fetch(addr & 0xff);
        return result & 0xff; // wrap addresses
}

private void traceBefore(int pc, String op, boolean fi,
                         int a, boolean fa,
                         int b, boolean fb,
                         int c, boolean fc)
{
        this.TAChOMETER++;
        if (!this.trace) return;
        if (fi)
        {
                System.err.printf("\nPC = %02X  Op = %5si ",pc,op);
                if (fa) System.err.printf("A = [%8x] ",a);
                else    System.err.printf("A =  %8x  ",a);
                System.err.printf("I =  %4X%04X  ",b,c);
                if (fb || fc) System.err.printf("    EXTEND    ");
                else          System.err.printf("              ");
        }
        else
        {
                System.err.printf("\nPC = %02X  Op = %5s ",pc,op);
                if (fa) System.err.printf("A = [%8x] ",a);
                else    System.err.printf("A =  %8x  ",a);
                if (fb) System.err.printf("B = [%8x] ",b);
                else    System.err.printf("B =  %8x  ",b);
                if (fc) System.err.printf("C = [%8x] ",c);
                else    System.err.printf("C =  %8x  ",c);
        }
        if (this.underflow)    System.err.printf("UNDERFLOW     ");
        if (this.overflow)     System.err.printf("OVERFLOW      ");
        if (this.divideByZero) System.err.printf("DIVIDE BY ZERO");
        System.err.printf("\n");
}

private void traceAfter(int a, int b, int c, int result)
{
        if (!this.trace) return;
        System.err.printf("         Effective A =  %8x  B =  %8x  C =  %8x  result = %8x\n",
                          a,b,c,result);
}
private void svc ()
{
        traceBefore(PC-1,
                    "svc", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int A = fetchAddress(this.addresses[0],flags[0]);
        int B = fetchAddress(this.addresses[1],flags[1]);
        int C = fetchAddress(this.addresses[2],flags[2]);
        traceAfter(A,B,C,0);
        switch(A)
        {
        case 0x00: halt(B,C); break;
        case 0x01: pdump(B,C); break;
        case 0x02: trace(B); break;
        case 0x03: pause(B); break;
        default:
                System.err.printf("SVC opCode = %x???\n",A);
                halt(0,255); break;
        }
}

private void dump()
{
        System.err.printf("\n%s\n", this);
        System.out.printf("\n%s\n", this);
}

private void pdump(int B, int C)
{
        System.err.printf("\n%s\n",this.toString(B,C));
        System.out.printf("\n%s\n",this.toString(B,C));
}

private void trace(int B)
{
        this.trace = !(B == 0);
        if(this.trace)
                System.err.printf("\n\n***TRACE ON STARTING AT PC = %02x***\n\n",PC);
        else
                System.err.printf("\n\n***TRACE OFF STARTING AT PC = %02x***\n\n",PC);
}

// Sleep doesn't work - the TAChMemory thread freezes
// so, try to do it ourselves
private void pause(int B)
{
        for(int i = 0; i < B*750; i++)
                wait(1000);
}

private int [] operands()
{
        int[] result  = new int[3];
        result[0] = fetchOperand(this.addresses[0],flags[0]);
        if (flags[3])
        {
                result[1] = fetchImmediate(this.addresses[1],flags[1],
                                           this.addresses[2],flags[2]);
                result[2] = fetchAddress(this.addresses[0],flags[0]);
        }
        else
        {
                result[1] = fetchOperand(this.addresses[1],flags[1]);
                result[2] = fetchAddress(this.addresses[2],flags[2]);
        }
        return result;
}

private void add()
{
        clearConditions();
        traceBefore(PC-1,
                    "add", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int[] ops = operands();
        int result = catchIntErrors((long)ops[0] + (long)ops[1]);
        tm.store(ops[2],result);
        this.lastStoredValue = result;
        traceAfter(ops[0],ops[1],ops[2],result);
}

private void sub()
{
        clearConditions();
        traceBefore(PC-1,
                    "sub", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int[] ops = operands();
        int result = catchIntErrors((long)ops[0] - (long)ops[1]);
        tm.store(ops[2],result);
        this.lastStoredValue = result;
        traceAfter(ops[0],ops[1],ops[2],result);
}

private void mul()
{
        clearConditions();
        traceBefore(PC-1,
                    "mul", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int[] ops = operands();
        int result = catchIntErrors((long)ops[0] * (long)ops[1]);
        tm.store(ops[2],result);
        this.lastStoredValue = result;
        traceAfter(ops[0],ops[1],ops[2],result);
}

private void div()
{
        clearConditions();
        traceBefore(PC-1,
                    "div", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int[] ops = operands();
        int result2 =  catchIntErrors((long)ops[0] % (long)ops[1]);
        int result = catchIntErrors((long)ops[0] / (long)ops[1]); // codes?
        tm.store(ops[2],result);
        tm.store(ops[2]+1,result2);
        this.lastStoredValue = result;
        traceAfter(ops[0],ops[1],ops[2],result);
}

private void and()
{
        clearConditions();
        traceBefore(PC-1,
                    "and", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int[] ops = operands();
        int result = ops[0] & ops[1];
        tm.store(ops[2],result);
        this.lastStoredValue = result;
        traceAfter(ops[0],ops[1],ops[2],result);
}


private void or()
{
        clearConditions();
        traceBefore(PC-1,
                    "or ", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int[] ops = operands();
        int result = ops[0] | ops[1];
        tm.store(ops[2],result);
        this.lastStoredValue = result;
        traceAfter(ops[0],ops[1],ops[2],result);
}

private void xor()
{
        clearConditions();
        traceBefore(PC-1,
                    "xor", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int[] ops = operands();
        int result = ops[0] ^ ops[1];
        tm.store(ops[2],result);
        this.lastStoredValue = result;
        traceAfter(ops[0],ops[1],ops[2],result);
}

private void rot()
{
        clearConditions();
        traceBefore(PC-1,
                    "rot", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int[] ops = operands();
        int result = Integer.rotateRight(ops[0], ops[1]);
        tm.store(ops[2],result);
        this.lastStoredValue = result;
        traceAfter(ops[0],ops[1],ops[2],result);
}

private void setFloatConditions(TAChFloat F)
{
        underflow = F.getUnderflow();
        overflow  = F.getOverflow();
        divideByZero = F.getDivideByZero();
}

private void fadd()
{
        clearConditions();
        traceBefore(PC-1,
                    "fadd", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int[] ops = operands();
        TAChFloat fA = new TAChFloat(ops[0]);
        TAChFloat fB = new TAChFloat(ops[1]);
        TAChFloat fC = fA.plus(fB);
        setFloatConditions(fC);
        int result = fC.getValue();
        tm.store(ops[2],result);
        this.lastStoredValue = result;
        traceAfter(ops[0],ops[1],ops[2],result);
}

private void fsub()
{
        clearConditions();
        traceBefore(PC-1,
                    "fsub", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int[] ops = operands();
        TAChFloat fA = new TAChFloat(ops[0]);
        TAChFloat fB = new TAChFloat(ops[1]);
        TAChFloat fC = fA.minus(fB);
        setFloatConditions(fC);
        int result = fC.getValue();
        tm.store(ops[2],result);
        this.lastStoredValue = result;
        traceAfter(ops[0],ops[1],ops[2],result);
}

private void fmul()
{
        clearConditions();
        traceBefore(PC-1,
                    "fmul", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int[] ops = operands();
        TAChFloat fA = new TAChFloat(ops[0]);
        TAChFloat fB = new TAChFloat(ops[1]);
        TAChFloat fC = fA.times(fB);
        setFloatConditions(fC);
        int result = fC.getValue();
        tm.store(ops[2],result);
        this.lastStoredValue = result;
        traceAfter(ops[0],ops[1],ops[2],result);
}

private void fdiv()
{
        clearConditions();
        traceBefore(PC-1,
                    "fdiv", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int[] ops = operands();
        TAChFloat fA = new TAChFloat(ops[0]);
        TAChFloat fB = new TAChFloat(ops[1]);
        TAChFloat fC = fA.dividedBy(fB);
        setFloatConditions(fC);
        int result = fC.getValue();
        tm.store(ops[2],result);
        this.lastStoredValue = result;
        traceAfter(ops[0],ops[1],ops[2],result);
}

private void err()
{
        traceBefore(PC-1,
                    "err", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int A = fetchAddress(this.addresses[0],flags[0]);
        int B = fetchAddress(this.addresses[1],flags[1]);
        int C = fetchAddress(this.addresses[2],flags[2]);
        if (underflow) PC = A;
        else if (overflow) PC = C;
        else if (divideByZero) PC = B;
        traceAfter(A,B,C,PC);
}

private void br()
{
        traceBefore(PC-1,
                    "br ", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int A = fetchAddress(this.addresses[0],flags[0]);
        int B = fetchAddress(this.addresses[1],flags[1]);
        int C = fetchAddress(this.addresses[2],flags[2]);
        if (lastStoredValue < 0) PC = A;
        else if (lastStoredValue > 0) PC = C;
        else PC = B;
        traceAfter(A,B,C,PC);
}

private void call()
{
        traceBefore(PC-1,
                    "call", flags[3],
                    this.addresses[0], flags[0],
                    this.addresses[1], flags[1],
                    this.addresses[2], flags[2]);
        int A = fetchAddress(this.addresses[0],flags[0]);
        int B = fetchAddress(this.addresses[1],flags[1]);
        int C = fetchAddress(this.addresses[2],flags[2]);
        tm.store(C,A);
        tm.store(C+1,B);
        PC = C + 2;
        traceAfter(A,B,C,PC);
}

private void halt(int B, int C)
{
        this.RUNNING = false;
        traceAfter(0,B,C,PC-1);
        pdump(B,C);
}

public void setPC(int addr)
{
        this.PC = addr;
}

public void setSpeed(int speed)
{
        this.speed = speed;
}

public void run()
{
        this.RUNNING = true;
        fie();
}

private void wait(int delay)
{
        for (int i = 0; i < delay; i++)
        {
                this.tm.store(0,this.tm.fetch(0));
        }
}

private void fie()
{
        int z = -1;
        while(this.RUNNING)
        {
                // fetch
                instruction = tm.fetch(PC);
                // increment
                PC = (PC+1) & 0xff;
                // parse
                parse();
                // execute
                execute();
                if (speed > 0) wait(this.speed);
        }
}

private static int[] program1 = {
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
        0x00000001 // 11  DC 1
};

private static int[] Fibonacci = {
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
        0x00000000 // 39 TMP     DC   0
};

public static void main(String[] args)
{
        int[] program = Fibonacci;

        TAChMemory tm = new TAChMemory();
        Thread t = new Thread(tm);
        t.start();
        for(int addr=0; addr<program.length; addr++) tm.store(addr,program[addr]);

        System.err.printf("%s\n",tm);
        TAChCPU cpu = new TAChCPU(tm);
        Thread tcpu = new Thread(cpu);
        cpu.setPC(0);
        cpu.setSpeed(1000);
        tcpu.start();
        // look at the core dump!
}
}
