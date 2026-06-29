package example;

import java.util.ArrayList;
import java.util.List;

public class Repro {
    record Window(java.time.LocalTime start, java.time.LocalTime end) {}

    List<Window> f(List<Window> in) {
        List<Window> out = new ArrayList<Window>();
        in.stream().map(w -> new Window(w.start(), w.end())).forEach(out::add);
        return out;
    }
}