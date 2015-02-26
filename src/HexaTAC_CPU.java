// File: HexaTAC.java
// Author: J A Harris
// Last Modified: 12 February 2015
// Purpose: Basic CPU loader and ALU

public class HexaTAC_CPU implements Runnable {
private int PC;
private TAChMemory memory;
private boolean RUNNING;
private int opCode;
private int index;
private int instruction;
private int addressA;
private int addressB;
private int addressC;
private int addressXA;
private int addressXB;
private int addressXC;
private static int program1 = 0x10111000;     // 00  ADD 17,16,00


//initialtized cpu
public HexaTAC_CPU(TAChMemory memory){
        this.memory = memory;
        this.PC = 0;
        this.RUNNING = false;
        this.addressA = 0;
        this.addressB = 0;
        this.addressC = 0;
}
//parse instruction into opcode index and addressess
private void parse(){
        this.opCode = (instruction >> 24)&0xF8;
        this.index = (instruction >> 24) & 0x07;
        this.addressA = (instruction >>16) & 0xFF;
        if (this.index & 0x4 ) this.addressA += this.addressXA;
        this.addressB = (instruction >> 8) & 0xFF;
        if (this.index & 0x2 ) this.addressB += this.addressXB;
        this.addressC = (instruction) & 0xFF;
        if (this.index & 0x1 ) this.addressC += this.addressXC;
}
//excute opcode
private void execute(){
        //indirect is off

                switch(this.opCode) {
                case 0x00: halt(); break;
                case 0x10: add(); break;
                case 0x20: sub(); break;
                case 0x30: mul(); break;
                case 0x40: div(); break;
                case 0x50:and();break;
                case 0x58:and();break;
                case 0x60:and();break;
                case 0x68:shl();break;
                case 0x70:shr();break;
                default: System.err.printf("opCode = %x\n",this.opCode);
                }

}

private void dump(){
    System.out.println(Integer.toHexString(addressA));
    System.out.println(Integer.toHexString(addressB));
    System.out.println(Integer.toHexString(addressC));
}
private void fie(){
        while(RUNNING) {
                instruction = memory.fetch(PC);
                PC++;
                parse();
                execute();
        }
}

private void halt(){
    RUNNING=false;
}

private void add(){
        addressC = addressA + addressB;
        dump();
}
private void sub(){
        addressC = addressA + addressB;
        dump();
}
private void mul(){
        addressC = addressA + addressB;
        dump();
}
private void div(){
        if (addressB > 0x0) {
            addressC = addressA + addressB;
            dump();
        }else{
            halt();
        }
}
private void and(){
    addressC = addressA && addressB;
    dump();
}
private void or(){
    addressC = addressA || addressB;
    dump();
}
private void xor(){
    addressC = addressA ^ addressB;
    dump();
}
private void shl(){
    addressC = addressA << addressB;
    dump();
}
private void shr(){
    addressC = addressA  >> addressB;
    dump();
}
public void run(){
    RUNNING=true;
    fie();
}

public static void main(String[] args){
        TAChMemory tm = new TAChMemory();
        Thread t = new Thread(tm);
        t.start();
        tm.store(0,program1);
        System.err.printf("%s\n",tm);
        HexaTAC_CPU cpu = new HexaTAC_CPU(tm);
        Thread tcpu = new Thread(cpu);
        tcpu.start();
}
}
