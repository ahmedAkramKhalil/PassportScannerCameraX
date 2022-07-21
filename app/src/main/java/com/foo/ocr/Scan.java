package com.foo.ocr;

public class Scan {
    long startTime = 0 ;
    long endTime = 0 ;

    public Scan() {
    }
    // [7107 3704, 2964, 4021, 2896, 3780, 1862, 4282, 1250, 5284, 5827, 4188, 10453,12121, 3108, 3970, 1577, 4243, 6557, 8229, 8451, 3988, 5378, 3861, 6018]
    // [4979, 5138, 7399, 5467, 4053, 7423, 7403, 7559, 1504, 4479, 5511, 7964, 7749]

    @Override
    public String toString() {
        return "" + ( endTime - startTime )
                 ;
    }
}
