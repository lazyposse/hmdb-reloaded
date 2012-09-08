package sample;

import java.util.List;
import java.util.ArrayList;

public class Graph {
    public Object content;
    public Graph  child;

    public static Graph newExample() {
        Graph a = new Graph();
        Graph b = new Graph();
        Graph c = new Graph();

        a.content = "A";
        b.content = "B";
        c.content = "C";

        a.child = b;
        b.child = c;
        c.child = a;

        return a;
    }
}
