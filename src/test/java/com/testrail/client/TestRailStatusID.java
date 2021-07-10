package com.testrail.client;

/**
 * Test Rail Status IDs
 */
public class TestRailStatusID {
    public static int PASS = 1;
    public static int BLOCKED= 2;
    public static int UNTESTED= 3;
    public static int RETEST=4;
    public static int FAIL= 5;

    public static String getResult(int result) {
        switch(result) {
            case 1:
                return "PASS";
            case 2:
                return "BLOCKED";
            case 3:
                return "UNTESTED";
            case 4:
                return "RETEST";
            case 5:
                return "FAIL";
        }
        return null;
    }
}
