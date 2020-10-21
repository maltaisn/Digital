/*
 * Copyright (c) 2018 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.core;

import junit.framework.TestCase;

public class IntFormatTest extends TestCase {

    public void testHex() throws Exception {
        assertEquals("0x1", IntFormat.hex.formatToView(new Value(1, 1)));
        assertEquals("0x1", IntFormat.hex.formatToView(new Value(1, 2)));
        assertEquals("0x1", IntFormat.hex.formatToView(new Value(1, 3)));
        assertEquals("0x1", IntFormat.hex.formatToView(new Value(1, 4)));
        assertEquals("0xF", IntFormat.hex.formatToView(new Value(-1, 4)));
        assertEquals("0x01", IntFormat.hex.formatToView(new Value(1, 5)));
        assertEquals("0x1F", IntFormat.hex.formatToView(new Value(-1, 5)));
        assertEquals("0xFFF", IntFormat.hex.formatToView(new Value(-1, 12)));
        assertEquals("0x1FFF", IntFormat.hex.formatToView(new Value(-1, 13)));
        assertEquals("0x3FFF", IntFormat.hex.formatToView(new Value(-1, 14)));
        assertEquals("0x7FFF", IntFormat.hex.formatToView(new Value(-1, 15)));
        assertEquals("0xFFFF", IntFormat.hex.formatToView(new Value(-1, 16)));
        assertEquals("0xFEDCBA9876543210", IntFormat.hex.formatToView(new Value(0xFEDCBA9876543210L, 64)));
    }

    public void testBin() {
        assertEquals("0b1", IntFormat.bin.formatToView(new Value(1, 1)));
        assertEquals("0b01", IntFormat.bin.formatToView(new Value(1, 2)));
        assertEquals("0b001", IntFormat.bin.formatToView(new Value(1, 3)));
        assertEquals("0b111", IntFormat.bin.formatToView(new Value(-1, 3)));
        assertEquals("0b1111111111111111111111111111111111111111111111111111111111111111", IntFormat.bin.formatToView(new Value(-1, 64)));
        assertEquals("0b1000111111111111111111111111111111111111111111111111111111111111", IntFormat.bin.formatToView(new Value(0x8fffffffffffffffL, 64)));
    }

    public void testDec() throws Exception {
        assertEquals("3", IntFormat.dec.formatToView(new Value(-1, 2)));
        assertEquals("-1", IntFormat.decSigned.formatToView(new Value(-1, 2)));
    }

    public void testDef() throws Exception {
        assertEquals("3", IntFormat.def.formatToView(new Value(3, 64)));
        assertEquals("0x113", IntFormat.def.formatToView(new Value(0x113, 64)));
        assertEquals("1A3", IntFormat.def.formatToView(new Value(0x1A3, 64)));
        assertEquals("FFFFFFFFFFFFFFFF", IntFormat.def.formatToView(new Value(-1, 64)));
    }

    public void testFloat32() throws Exception {
        assertEquals("32f", IntFormat.floatp.formatToView(new Value(Float.floatToIntBits(32.0f), 32)));
        assertEquals("-1f", IntFormat.floatp.formatToView(new Value(Float.floatToIntBits(-1f), 32)));
        assertEquals("5.6e-4f", IntFormat.floatp.formatToView(new Value(Float.floatToIntBits(5.6e-4f), 32)));
        assertEquals("NaNf", IntFormat.floatp.formatToView(new Value(Float.floatToIntBits(Float.NaN), 32)));
        assertEquals("inff", IntFormat.floatp.formatToView(new Value(Float.floatToIntBits(Float.POSITIVE_INFINITY), 32)));
        assertEquals("-inff", IntFormat.floatp.formatToView(new Value(Float.floatToIntBits(Float.NEGATIVE_INFINITY), 32)));
    }

    public void testFloat64() throws Exception {
        assertEquals("32.0", IntFormat.floatp.formatToView(new Value(Double.doubleToLongBits(32.0), 64)));
        assertEquals("-1.0", IntFormat.floatp.formatToView(new Value(Double.doubleToLongBits(-1.0), 64)));
        assertEquals("5.6e-4", IntFormat.floatp.formatToView(new Value(Double.doubleToLongBits(5.6e-4), 64)));
        assertEquals("NaN", IntFormat.floatp.formatToView(new Value(Double.doubleToLongBits(Double.NaN), 64)));
        assertEquals("inf", IntFormat.floatp.formatToView(new Value(Double.doubleToLongBits(Double.POSITIVE_INFINITY), 64)));
        assertEquals("-inf", IntFormat.floatp.formatToView(new Value(Double.doubleToLongBits(Double.NEGATIVE_INFINITY), 64)));
    }

    public void testIsFloat32Literal() {
        assertTrue(IntFormat.isFloat32Literal("nanf"));
        assertTrue(IntFormat.isFloat32Literal("inff"));
        assertTrue(IntFormat.isFloat32Literal("-inff"));
        assertTrue(IntFormat.isFloat32Literal("1.0f"));
        assertTrue(IntFormat.isFloat32Literal("1.f"));
        assertTrue(IntFormat.isFloat32Literal("1f"));
        assertTrue(IntFormat.isFloat32Literal("1e0f"));
        assertTrue(IntFormat.isFloat32Literal("-1.0e-9f"));
        assertFalse(IntFormat.isFloat32Literal("0x1f"));
        assertFalse(IntFormat.isFloat32Literal("0x1e9"));
        assertFalse(IntFormat.isFloat32Literal("nan"));
        assertFalse(IntFormat.isFloat32Literal("inf"));
    }

    public void testIsFloat64Literal() {
        assertTrue(IntFormat.isFloat64Literal("nan"));
        assertTrue(IntFormat.isFloat64Literal("inf"));
        assertTrue(IntFormat.isFloat64Literal("-inf"));
        assertTrue(IntFormat.isFloat64Literal("1.0"));
        assertTrue(IntFormat.isFloat64Literal("1."));
        assertTrue(IntFormat.isFloat64Literal("1e0"));
        assertTrue(IntFormat.isFloat64Literal("-1.0e-9"));
        assertFalse(IntFormat.isFloat64Literal("0x1e9"));
        assertFalse(IntFormat.isFloat64Literal("nanf"));
        assertFalse(IntFormat.isFloat64Literal("inff"));
    }

    /**
     * Ensures that it is possible to convert a string representation obtained by {@link IntFormat#formatToEdit(Value)}
     * back to the same value by {@link Bits#decode(String)}
     */
    public void testBitDecodeConstraint() throws Bits.NumberFormatException {
        for (IntFormat f : IntFormat.values()) {
            switch (f) {
                case ascii:
                    checkConstraint(f, tableAscii); // ascii supports only 16 bit
                    break;
                case floatp:
                    checkConstraint(f, tableFloat); // avoids NaN canonicalization
                    break;
                default:
                    checkConstraint(f, table);
                    break;
            }
        }
    }

    private static final Value[] table = new Value[]{
            new Value(1, 2),
            new Value(-1, 2),
            new Value(1, 64),
            new Value(10, 8),
            new Value(17, 8),
            new Value(-1, 64),
            new Value(0x4fffffffffffffffL, 63),
            new Value(0x8fffffffffffffffL, 64),
    };

    private static final Value[] tableAscii = new Value[]{
            new Value(65, 8),
            new Value(65, 8),
            new Value(1000, 16),
            new Value(-1, 7),
            new Value(-1, 7),
    };

    private static final Value[] tableFloat = new Value[]{
            new Value(Float.floatToIntBits(32f), 32),
            new Value(Float.floatToIntBits(Float.NaN), 32),
            new Value(Float.floatToIntBits(Float.NEGATIVE_INFINITY), 32),
            new Value(Double.doubleToLongBits(5.3e-8), 64),
            new Value(Double.doubleToLongBits(0.3), 64),
    };

    private void checkConstraint(IntFormat format, Value[] table) throws Bits.NumberFormatException {
        for (Value val : table) {
            final String str = format.formatToEdit(val);
            final Value conv = new Value(Bits.decode(str), val.getBits());
            assertTrue(format.name() + ":" + val + " != " + conv, val.isEqual(conv));
        }
    }

    public void testStrLen() {
        assertEquals(6,IntFormat.hex.strLen(16));
        assertEquals(6,IntFormat.hex.strLen(15));
        assertEquals(6,IntFormat.hex.strLen(14));
        assertEquals(6,IntFormat.hex.strLen(13));
        assertEquals(5,IntFormat.hex.strLen(12));

        assertEquals(18,IntFormat.bin.strLen(16));
        assertEquals(17,IntFormat.bin.strLen(15));
        assertEquals(16,IntFormat.bin.strLen(14));

        assertEquals(3,IntFormat.dec.strLen(8));
        assertEquals(3,IntFormat.dec.strLen(9));
        assertEquals(4,IntFormat.dec.strLen(10));
        assertEquals(19, IntFormat.dec.strLen(60));
        assertEquals(19, IntFormat.dec.strLen(61));
        assertEquals(19, IntFormat.dec.strLen(62));
        assertEquals(19, IntFormat.dec.strLen(63));
        assertEquals(20, IntFormat.dec.strLen(64));

        assertEquals(4,IntFormat.decSigned.strLen(8));
        assertEquals(4,IntFormat.decSigned.strLen(9));
        assertEquals(4,IntFormat.decSigned.strLen(10));
        assertEquals(5,IntFormat.decSigned.strLen(11));
        assertEquals(20, IntFormat.decSigned.strLen(62));
        assertEquals(20, IntFormat.decSigned.strLen(63));
        assertEquals(20, IntFormat.decSigned.strLen(64));

        assertEquals(4,IntFormat.oct.strLen(4));
        assertEquals(4,IntFormat.oct.strLen(5));
        assertEquals(4,IntFormat.oct.strLen(6));
        assertEquals(5,IntFormat.oct.strLen(7));
        assertEquals(5,IntFormat.oct.strLen(8));
        assertEquals(5,IntFormat.oct.strLen(9));
        assertEquals(6,IntFormat.oct.strLen(10));

    }
}
