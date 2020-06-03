package com.anprsystemsltd.anpr.android.demo.so.ol;

abstract class AnprOutput {

    public boolean isValid = false;

    protected byte[] buffer;

    public int structSize;
    public char[] charBuffer = new char[12];
    public int numberOfChars;
    public int confidence;
    public int plateX0, plateY0;
    public int plateX1, plateY1;
    public int plateX2, plateY2;
    public int plateX3, plateY3;
    public int plateWidth;
    public int plateHeight;
    public int country;
    public int avgCharHeight;
    public int syntaxWeight;
    public int syntaxCode;
    public char[] syntaxName = new char[8];



    protected int unsignedByte(byte b) {
        return b & 0xff;
    }

    protected void copyChars(char[] chars, int offset, int count) {
        for (int i = 0; i < count; i++) {
            chars[i] = (char)buffer[offset + i];
        }
    }

    abstract int parseInteger(int offset);

    abstract void refreshFromBuffer(byte[] buffer);
}
