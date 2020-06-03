package com.anprsystemsltd.anpr.android.demo.so.ol;

public class AnprOutput_32 extends AnprOutput {

    @Override
    void refreshFromBuffer(byte[] buffer) {

        this.buffer = buffer;
        structSize = parseInteger( 0);
        copyChars(charBuffer, 8, 12);
        numberOfChars = parseInteger(20);
        isValid = structSize == 232 && numberOfChars <= 12;
        confidence = parseInteger(28);
        plateX0 = parseInteger(80);
        plateX0 = parseInteger(84);
        plateX1 = parseInteger(88);
        plateX1 = parseInteger(92);
        plateX2 = parseInteger(96);
        plateX2 = parseInteger(100);
        plateX3 = parseInteger(104);
        plateX3 = parseInteger(108);
        plateWidth = parseInteger(112);
        plateHeight = parseInteger(116);
        country = parseInteger(156);
        avgCharHeight = parseInteger(168);
        syntaxWeight = parseInteger(172);
        syntaxCode = parseInteger(176);
        copyChars(syntaxName, 208, 8);
    }

    @Override
    int parseInteger(int offset) {
        return unsignedByte(buffer[offset]) +
                unsignedByte(buffer[offset + 1]) * 16 +
                unsignedByte(buffer[offset + 2]) * 256 +
                unsignedByte(buffer[offset + 3]) * 65535;
    }
}
