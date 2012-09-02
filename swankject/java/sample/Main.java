package sample;

import sample.a.A;
import sample.b.B;

public class Main {
    public static final void main(String[] args) {
        A.a("The time is now: "+ new java.util.Date());
        B.b("The time is now: "+ new java.util.Date());
    }
}
