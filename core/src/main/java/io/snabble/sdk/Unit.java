package io.snabble.sdk;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import io.snabble.sdk.utils.Logger;

public enum Unit {
    MILLILITER("ml", "ml", Dimension.VOLUME),
    CENTILITER("cl", "cl", Dimension.VOLUME),
    DECILITER("dl", "dl", Dimension.VOLUME),
    LITER("l", "l", Dimension.VOLUME),

    CUBIC_METER("m3", "m³", Dimension.CAPACITY),
    CUBIC_CENTIMETER("cm3", "cm³", Dimension.CAPACITY),

    SQUARE_METER("m2", "m²", Dimension.AREA),
    SQUARE_METER_TENTH("m2e-1", "m2e-1", Dimension.AREA),
    SQUARE_DECIMETER("dm2", "dm²", Dimension.AREA),
    SQUARE_DECIMETER_TENTH("m2e-3", "m2e-3", Dimension.AREA),
    SQUARE_CENTIMETER("cm2", "cm²", Dimension.AREA),

    MILLIMETER("mm", "mm", Dimension.DISTANCE),
    CENTIMETER("cm", "cm", Dimension.DISTANCE),
    DECIMETER("dm", "dm", Dimension.DISTANCE),
    METER("m", "m", Dimension.DISTANCE),

    GRAM("g", "g", Dimension.MASS),
    DECAGRAM("dag", "dag", Dimension.MASS),
    HECTOGRAM("hg", "hg", Dimension.MASS),
    KILOGRAM("kg", "kg", Dimension.MASS),
    TONNE("t", "t", Dimension.MASS),

    PIECE("piece", "", null),
    PRICE("price", "", null);

    private String id;
    private String displayValue;
    private Dimension dimension;

    Unit(String id, String displayValue, Dimension dimension) {
        this.id = id;
        this.displayValue = displayValue;
        this.dimension = dimension;
    }

    public static Unit fromString(String value) {
        if (value != null) {
            for (Unit unit : values()) {
                if (unit.id.equals(value)) {
                    return unit;
                }
            }
        }

        return null;
    }

    public String getId() {
        return id;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public Unit getFractionalUnit(int decimal) {
        for (Conversion conversion : conversions) {
            if (conversion.from == this && conversion.factor == (int)Math.pow(10, decimal)) {
                return conversion.to;
            }
        }

        return null;
    }

    public static boolean hasDimension(Unit unit) {
        if (unit != null) {
            return unit.dimension != null;
        }

        return false;
    }

    private static class Conversion {
        Unit from;
        Unit to;
        int factor;
        int divisor;

        Conversion(Unit from, Unit to, int factor, int divisor) {
            this.from = from;
            this.to = to;
            this.factor = factor;
            this.divisor = divisor;
        }
    }

    private static List<Conversion> conversions = new ArrayList<>();

    private static void addConversion(Unit from, Unit to, int factor, int divisor) {
        conversions.add(new Conversion(from, to, factor, divisor));
        conversions.add(new Conversion(to, from, divisor, factor));
    }

    static {
        addConversion(LITER, DECILITER, 10, 1);
        addConversion(LITER, CENTILITER, 100, 1);
        addConversion(LITER, MILLILITER, 1000, 1);
        addConversion(DECILITER, MILLILITER, 100, 1);

        addConversion(CUBIC_METER, CUBIC_CENTIMETER, 1_000_000, 1);

        addConversion(SQUARE_METER, SQUARE_CENTIMETER, 10_000, 1);
        addConversion(SQUARE_METER, SQUARE_DECIMETER_TENTH, 1000, 1);
        addConversion(SQUARE_METER, SQUARE_DECIMETER, 100, 1);
        addConversion(SQUARE_METER, SQUARE_METER_TENTH, 10, 1);

        addConversion(METER, CENTIMETER, 100, 1);
        addConversion(METER, DECIMETER, 10, 1);
        addConversion(METER, MILLIMETER, 1000, 1);
        addConversion(CENTIMETER, MILLIMETER, 10, 1);

        addConversion(TONNE, KILOGRAM, 1000, 1);
        addConversion(TONNE, GRAM, 1_000_000, 1);
        addConversion(KILOGRAM, GRAM, 1000, 1);
        addConversion(KILOGRAM, DECAGRAM, 100, 1);
        addConversion(KILOGRAM, HECTOGRAM, 10, 1);
    }

    public static BigDecimal convert(BigDecimal value, Unit from, Unit to) {
        if (from == to) return value;

        for (Conversion conversion : conversions) {
            if (conversion.from == from && conversion.to == to) {
                return value.multiply(new BigDecimal(conversion.factor))
                        .divide(new BigDecimal(conversion.divisor), MathContext.UNLIMITED);
            }
        }

        Logger.d("Unsupported conversion: %s -> %s", from.toString(), to.toString());

        return value;
    }
}
