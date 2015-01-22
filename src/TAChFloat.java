// File: TAChFloat.java
// Author: K R Sloan
// Last Modified: 132 December 2014
// Purpose: FPU for TACh
public class TAChFloat
{
    private boolean negative;
    private int exponent; // signed
    private int mantissa; // unsigned
    private boolean underflow;
    private boolean overflow;
    private boolean divideByZero;

    public boolean getUnderflow() { return underflow; }
    public boolean getOverflow() { return overflow; }
    public boolean getDivideByZero() { return divideByZero; }

    public int getValue()
    {
	int exponentBits = (this.exponent+0x40) & 0x7f;
	int mantissaBits = this.mantissa & 0xffffff;
	int signBit = 0; if (this.negative) signBit = 1;
	int result =  (signBit << 31) | (exponentBits << 24) | mantissaBits;
	return result;
    }
    
    public TAChFloat(int value)
    {
	boolean negative = (value & 0x80000000) != 0;
	this.exponent = ((value & 0x7f000000) >> 24) - 0x40;
	this.mantissa = value & 0x00ffffff;
	if (negative) this.mantissa = -this.mantissa;
    }
    
    public TAChFloat(int exponent, long mantissa)
    {
	boolean negative = false;
	if (mantissa < 0) 
	    { 
		negative = true; 
		mantissa = -mantissa;
	    }
	while (mantissa > 0x00ffffff)
	    {
		exponent++;
		mantissa = mantissa >> 4;
	    }
	if (mantissa == 0)
	    {
		exponent = 0;
		underflow = true;
	    }
	if (this.exponent > 63)
	    {
		exponent = 63;
		mantissa = 0x00ffffff;
		overflow = true;
	    }
	if (mantissa > 0)
	    {
		while (mantissa < 0x00100000)
		    {
			exponent--;
			mantissa = mantissa << 4;
		    }
		if (exponent < -0x3f)
		    {
			exponent = -0x3f;
			mantissa = 0;
			underflow = true;
		    }
	    }
	this.negative = negative;
	this.exponent = exponent;
	this.mantissa = (int) mantissa;
    }
    
    public TAChFloat plus(TAChFloat B)
    {
	int expA = this.exponent;
	int expB = B.exponent;
	int mantA = this.mantissa;
	int mantB = B.mantissa;
	while (expA > expB) { expB++; mantB = mantB >> 4; }
	while (expA < expB) { expA++; mantA = mantA >> 4; }
	if (this.negative) mantA = -mantA;
	if (B.negative) mantB = -mantB;
	long mantC = mantA + mantB;
	TAChFloat C = new TAChFloat(expA, mantC);
	return C;
    }
    
    public TAChFloat minus(TAChFloat B)
    {
	int expA = this.exponent;
	int expB = B.exponent;
	int mantA = this.mantissa;
	int mantB = B.mantissa;
	while (expA > expB) { expB++; mantB = mantB >> 4; }
	while (expA < expB) { expA++; mantA = mantA >> 4; }
	if (this.negative) mantA = -mantA;
	if (B.negative) mantB = -mantB;
	long mantC = mantA - mantB;
	TAChFloat C = new TAChFloat(expA, mantC);
	return C;
    }
    
    public TAChFloat times(TAChFloat B)
    {
	int expA = this.exponent;
	int expB = B.exponent;
	int mantA = this.mantissa;
	int mantB = B.mantissa;
	if (this.negative) mantA = -mantA;
	if (B.negative) mantB = -mantB;
	int expC = expA + expB;
	long mantC = ((long)mantA * (long)mantB) >> 24;
	TAChFloat C = new TAChFloat(expC, mantC);
	return C;
    }
    
    public TAChFloat dividedBy(TAChFloat B)
    {
	int expA = this.exponent;
	int expB = B.exponent;
	int mantA = this.mantissa;
	int mantB = B.mantissa;
	if (this.negative) mantA = -mantA;
	if (B.negative) mantB = -mantB;
	int expC = expA - expB;
	long mantC = ((long)mantA / (long)mantB) << 24;
	TAChFloat C = new TAChFloat(expC, mantC);
	return C;
    }

    public static void main(String[] args)
    {
	TAChFloat ONE = new TAChFloat(0x41100000);
	System.out.printf("ONE = %x\n", ONE.getValue());

	TAChFloat TWO = new TAChFloat(0x41200000);
	System.out.printf("TWO = %x\n", TWO.getValue());

	TAChFloat THREE = ONE.plus(TWO);
	System.out.printf("ONE plus TWO = %x\n", THREE.getValue());

	TAChFloat MINUSONE = ONE.minus(TWO);
	System.out.printf("ONE minus TWO = %x\n", MINUSONE.getValue());


	TAChFloat FOUR = TWO.times(TWO);
	System.out.printf("TWO times TWO = %x\n", FOUR.getValue());

	TAChFloat DOS = FOUR.dividedBy(TWO);
	System.out.printf("FOUR divided by TWO = %x\n", DOS.getValue());
    }
}