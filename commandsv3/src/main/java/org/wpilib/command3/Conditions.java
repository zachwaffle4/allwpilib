package org.wpilib.command3;

import java.util.function.BooleanSupplier;

public class Conditions {
    private Conditions() {
        throw new UnsupportedOperationException("This is a utility class!");
    }

    public static BooleanSupplier and(BooleanSupplier a, BooleanSupplier b) {
        return () -> a.getAsBoolean() && b.getAsBoolean();
    }

    public static BooleanSupplier or(BooleanSupplier a, BooleanSupplier b) {
        return () -> a.getAsBoolean() || b.getAsBoolean();
    }

    public static BooleanSupplier not(BooleanSupplier a) {
        return () -> !a.getAsBoolean();
    }
}
