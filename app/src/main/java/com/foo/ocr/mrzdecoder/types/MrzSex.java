
package com.foo.ocr.mrzdecoder.types;

/***
 * MRZ sex.
 */
public enum MrzSex {

    Male('M'),
    Female('F'),
    Unspecified('X');

    /***
     * The MRZ character.
     */
    public final char mrz;
    
    private MrzSex(char mrz) {
        this.mrz = mrz;
    }
    
    public static MrzSex fromMrz(char sex) {
        switch (sex) {
            case 'M':
                return Male;
            case 'F':
                return Female;
            case '<':
            case 'X':
                return Unspecified;
            default:
                throw new RuntimeException("Invalid MRZ sex character: " + sex);
        }
    }
}
