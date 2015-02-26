// File: TAChMemory.java
// Author: K R Sloan
// Last Modified: 21 December 2014
// Purpose: provide memory for TACh


import java.util.*;
import java.util.concurrent.*;
import java.awt.Color;


public class TAChMemory implements Runnable
{
    private int[] words;
    private boolean dirty;
    private Draw tube;
    private int bitMapCols = 64;
    private int bitMapRows = 256*32/bitMapCols;

    private double bitMapLeft       =  0.0;
    private double bitMapTop        =  0.5;
    private double bitMapStepRight  =  1.0 / bitMapCols;
    private double bitMapStepDown   = -0.5 / bitMapRows;

    private Color backgroundColor;
    private Color oneColor;
    private Color zeroColor;

    // repaint entire memory
    private void refresh()
    {
	for(int addr=0; addr<this.words.length; addr++)
	    {
		int word = this.words[addr];
		int bit = addr*32;
		int x = bit % bitMapCols;
		int y = bit / bitMapCols;
		double sX = bitMapLeft + x*bitMapStepRight;
		double sY = bitMapTop  + y*bitMapStepDown;
		for(int mask=1<<31;
		    mask != 0;
		    mask = ((mask >> 1) & 0x7fffffff), sX+=bitMapStepRight)
		    {
			boolean one = (word & mask) != 0;
			if (one) tube.setPenColor(this.oneColor);
			else tube.setPenColor(this.zeroColor);
			tube.pixel(sX,sY);
		    }
	    }
	tube.show();
    }

    private synchronized void setDirty(boolean dirty)
    {
	this.dirty = dirty;
    }

    public synchronized void store(int addr, int contents)
    {
	this.words[addr] = contents;
	this.dirty = true;
	//	refresh(addr);
    }

    public int fetch(int addr)
    {
	return this.words[addr];
    }

    private String sexString(int x, int n)
    {
	String[] digits
	    = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
	int rest = x;
	String result = "";
	for(int i = 0; i< n; i++)
	    {
		int lo = rest & 0x0f;
		rest = rest >> 4;
		result = digits[lo] + result;
	    }
	return result;
    }

    private char makePrintable(char ch)
    {
	if ((ch > 32) && (ch < 127)) return ch;
	return (' ');
    }

    private String charString(int x, int n)
    {
	int rest = x;
	String result = "";
	for(int i = 0; i< n; i++)
	    {
		char lo = makePrintable((char)(rest % 256));
		rest = rest / 256;
		result = String.valueOf((char)lo) + result;
	    }
	return result;
    }

    public String toString()
    {
	int wordsPerLine=8;
	String result = "";
	String sexPart = "";
	String charPart = "";
	for(int addr=0; addr<words.length;)
	    {
		sexPart += sexString(this.words[addr],8) + " ";
		charPart += charString(this.words[addr],4);
		addr++;
		if(0 == (addr%wordsPerLine))
		    {
			String addrPart
			    = sexString(wordsPerLine*((addr/wordsPerLine)-1),2);
			result+=addrPart+" "+sexPart+" *"+charPart+"*\n";
			sexPart = "";
			charPart = "";
		    }
	    }
	return result;
    }

    public String toString(int lo, int hi)
    {
	int startAddr = (lo/8)*8;
	int endAddr = 7 + (hi/8)*8;
	int wordsPerLine=8;
	String result = "";
	String sexPart = "";
	String charPart = "";
	for(int addr=startAddr; addr<=endAddr;)
	    {
		sexPart += sexString(this.words[addr],8) + " ";
		charPart += charString(this.words[addr],4);
		addr++;
		if(0 == (addr%wordsPerLine))
		    {
			String addrPart
			    = sexString(wordsPerLine*((addr/wordsPerLine)-1),2);
			result+=addrPart+" "+sexPart+" *"+charPart+"*\n";
			sexPart = "";
			charPart = "";
		    }
	    }
	return result;
    }

    public TAChMemory()
    {
	this.backgroundColor = new Color(10,64,10);
	this.oneColor = new Color(127,255,127);
	this.zeroColor = new Color(0,32,0);
	this.words = new int[256];
	for(int addr=0; addr<words.length; addr++) words[addr]=0;
	this.tube = null;
    }
    public void run()
    {
	this.tube = new Draw("TACh memory: "
			+ bitMapCols
			+ "x"
			+ bitMapRows
			+ " = "
			+ bitMapCols*bitMapRows
			+ " bits"
			);
	this.tube.setCanvasSize(bitMapCols*6,bitMapRows*6);
	this.tube.setXscale(-0.01, 1.01);
	this.tube.setYscale(-0.01, 0.51);
	this.tube.clear(this.backgroundColor);
	setDirty(true);
	while(true)
	    {
		if (this.dirty)
		    {
			setDirty(false);
			this.refresh();
		    }
	    }
    }

    public static void main(String[] args)
    {
	TAChMemory tm = new TAChMemory();
	Thread t = new Thread(tm);
	t.start();
	for(int addr=0; addr<256; addr++) tm.store(addr,addr);
	System.err.printf("%s\n",tm);
    }
}
