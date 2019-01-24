package io.snabble.sdk;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.snabble.sdk.utils.Logger;

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

    public static BigDecimal convert(BigDecimal value, Unit from, Unit to, int scale, RoundingMode rm) {
        if (from == to) return value;

        value = value.setScale(scale, rm);

        if (from == LITER && to == DECILITER) return value.divide(new BigDecimal(10), rm);
        if (from == DECILITER && to == LITER) return value.multiply(new BigDecimal(10));

        if (from == LITER && to == MILLILITER) return value.divide(new BigDecimal(1000), rm);
        if (from == MILLILITER && to == LITER) return value.multiply(new BigDecimal(1000));

        if (from == DECILITER && to == MILLILITER) return value.divide(new BigDecimal(100), rm);
        if (from == MILLILITER && to == DECILITER) return value.multiply(new BigDecimal(100));

        if (from == CUBIC_METER && to == CUBIC_CENTIMETER) return value.divide(new BigDecimal(1_000_000), rm);
        if (from == CUBIC_CENTIMETER && to == CUBIC_METER) return value.multiply(new BigDecimal(1_000_000));

        if (from == SQUARE_METER && to == SQUARE_CENTIMETER) return value.divide(new BigDecimal(10_000), rm);
        if (from == SQUARE_CENTIMETER && to == SQUARE_METER) return value.multiply(new BigDecimal(10_000));

        if (from == METER && to == CENTIMETER) return value.divide(new BigDecimal(100), rm);
        if (from == CENTIMETER && to == METER) return value.multiply(new BigDecimal(100));

        if (from == METER && to == MILLIMETER) return value.divide(new BigDecimal(1000), rm);
        if (from == MILLIMETER && to == METER) return value.multiply(new BigDecimal(1000));

        if (from == CENTIMETER && to == MILLIMETER) return value.divide(new BigDecimal(10), rm);
        if (from == MILLIMETER && to == CENTIMETER) return value.multiply(new BigDecimal(10));

        if (from == TONNE && to == KILOGRAM) return value.divide(new BigDecimal(1000), rm);
        if (from == KILOGRAM && to == TONNE) return value.multiply(new BigDecimal(1000));

        if (from == TONNE && to == GRAM) return value.divide(new BigDecimal(1_000_000), rm);
        if (from == GRAM && to == TONNE) return value.multiply(new BigDecimal(1_000_000));

        if (from == KILOGRAM && to == GRAM) return value.divide(new BigDecimal(1000), rm);
        if (from == GRAM && to == KILOGRAM) return value.multiply(new BigDecimal(1000));

        Logger.d("Unsupported conversion: %s -> %s", from.toString(), to.toString());

        return value;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public static boolean isMass(Unit unit) {
        return unit == KILOGRAM || unit == GRAM || unit == TONNE;
    }

    public static boolean isVolume(Unit unit) {
        return unit == MILLILITER || unit == DECILITER || unit == LITER;
    }

    public static boolean isArea(Unit unit) {
        return unit == SQUARE_CENTIMETER || unit == SQUARE_METER;
    }

    public static boolean isCapacity(Unit unit) {
        return unit == CUBIC_METER || unit == CUBIC_CENTIMETER;
    }
}
