package org.genarator;

import org.genarator.generator.Generator;

public class Main {
    public static void main(String[] args) {
        Generator generator = new Generator();
        generator.generateDefaultCentralized(29L, 5, null);
        generator.writeAll();
    }
}