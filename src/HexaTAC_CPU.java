// File: HexaTAC.java
// Author: J A Harris
// Last Modified: 12 February 2015
// Purpose: Basic CPU loader and ALU
import hexATAC.*;
public class HexaTAC_CPU implements Runnable {
private int PC;
private TACHMemory memory;
private int RUNNING;
private int opCode;
private int index;
private int instruction;
private int addressA;
private int addressB;
private int addressC;
private static int program1 = 0x10111000;     // 00  ADD 17,16,00

public HexaTAC_CPU(TACHMemory memory){
        this.memory = memory;
        this.PC = 0;
        this.RUNNING = 0;
        this.addressA = 0;
        this.addressB = 0;
        this.addressC = 0;
}
private void parse(){
        this.opCode = (instruction >> 24)&0xF8;
        this.index = (instruction >> 24) & 0x07;
        this.addressA = (instruction >>16) & 0x000000FF;
        this.addressB = (instruction >> 8) & 0x000000FF;
        this.addressC = (instruction) & 0x000000FF;
}
private void execute(){
        //indirect is off
        if (index ==0x0) {
                switch(this.opCode) {
                case 0x10: add(); break;
                case 0x20: sub(); break;
                case 0x30: mul(); break;
                case 0x40: div(); break;
                default: System.err.printf("opCode = %x\n",this.opCode);
                }
        }

}

private void fie(){
        while(RUNNING) {
                instruction = memory.fetch(PC);
                PC++;
                parse();
                execute();
        }
}
public void run(){
        RUNNING=1;
        fie();
}

private void add(){
        addressC = Integer.valueOf(
                String.valueOf(
                        Integer.parseInt(addressA, 16) + Integer.parseInt(addressB, 16)
                        ), 16);
}
private void sub(){
        addressC = Integer.valueOf(
                String.valueOf(
                        Integer.parseInt(addressA, 16) - Integer.parseInt(addressB, 16)
                        ), 16);
}
private void mul(){
        addressC = Integer.valueOf(
                String.valueOf(
                        Integer.parseInt(addressA, 16) * Integer.parseInt(addressB, 16)
                        ), 16);
}
private void div(){
        if (addressB > 0x0) {
                addressC = Integer.valueOf(
                        String.valueOf(
                                Integer.parseInt(addressA, 16) / Integer.parseInt(addressB, 16)
                                ), 16);
        }
}

public static void main(String[] args){
        TAChMemory tm = new TAChMemory();
        Thread t = new Thread(tm);
        t.start();
        tm.store(addr,program[addr]);
        System.err.printf("%s\n",tm);
        HexaTAC_CPU cpu = new HexaTAC_CPU(cpu);
        Thread tcpu = new Thread(cpu);
        tcpu.start();
}
}
