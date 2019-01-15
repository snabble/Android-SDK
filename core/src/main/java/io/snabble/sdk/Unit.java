package io.snabble.sdk;

public enum Unit {
    MILLILITER("ml"),
    DECILITER("dl"),
    LITER("l"),
    CUBIC_METER("m³"),
    CUBIC_CENTIMETER("cm³"),
    SQUARE_METER("m²"),
    SQUARE_CENTIMETER("cm²"),
    MILLIMETER("mm"),
    CENTIMETER("cm"),
    METER("m"),
    KILOGRAM("kg"),
    GRAM("g"),
    TONNE("t"),
    PIECE(""),
    PRICE("");

    private String displayValue;

    Unit(String displayValue) {
        this.displayValue = displayValue;
    }

    public static Unit fromString(String value) {
        switch (value) {
            case "ml": return MILLILITER;
            case "dl": return DECILITER;
            case "l": return LITER;
            case "m3": return CUBIC_METER;
            case "cm3": return CUBIC_CENTIMETER;
            case "m2": return SQUARE_METER;
            case "cm2": return SQUARE_CENTIMETER;
            case "mm": return MILLIMETER;
            case "cm": return CENTIMETER;
            case "m": return METER;
            case "kg": return KILOGRAM;
            case "g": return GRAM;
            case "t": return TONNE;
            case "piece": return PIECE;
            case "price": return PRICE;
        }

        return null;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public boolean isMass() {
        return this == KILOGRAM || this == GRAM || this == TONNE;
    }

    public boolean isVolume() {
        return this == MILLILITER || this == DECILITER || this == LITER;
    }

    public boolean isArea() {
        return this == SQUARE_CENTIMETER || this == SQUARE_METER;
    }

    public boolean isCapacity() {
        return this == CUBIC_METER || this == CUBIC_CENTIMETER;
    }
}
