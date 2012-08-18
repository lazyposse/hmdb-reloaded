package sample;

import sample.a.A;
import sample.b.B;

public class Main {
    public static final void main(String[] args) {
        prn("Main ...");
        prn("    Calling A.a() ...");
        A.a();
        prn("    Calling A.a() DONE");
        prn("    Calling B.b() ...");
        B.b();
        prn("    Calling B.b() DONE");
        prn("Main DONE");
    }

    private static void prn(String msg) {
        System.out.println(msg);
    }
}
