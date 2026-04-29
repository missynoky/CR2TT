package pl.uwb.cr2tt.utils;

public class Logger {

    private static boolean isVerbose = false;

    public static void init(boolean verbose){
        isVerbose = verbose;
    }

    public static void info(String message){
        if (isVerbose) {
            System.err.println("Diagnostic: " + message);
        }
    }

    public static void error(String message) {
        System.err.println("Error: " + message);
    }

    public static void warn(String message) {
        System.err.println("Warning: " + message);
    }
}
