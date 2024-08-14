package io.snabble.sdk;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import io.snabble.sdk.utils.Logger;

/**
 * Enum describing different units and thier conversions to each other
 */
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

    PIECE("piece", "", Dimension.COUNT),
    PRICE("price", "", Dimension.AMOUNT);

    private final String id;
    private final String displayValue;
    private final Dimension dimension;

    Unit(String id, String displayValue, Dimension dimension) {
        this.id = id;
        this.displayValue = displayValue;
        this.dimension = dimension;
    }

    /**
     * Returns a unit from it's string representation.
     * <p>
     * E.g. "mm" -> Unit.MILLIMETER
     */
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

    /**
     * Returns the units unique identifier.
     * <p>
     * E.g. "mm" -> Unit.MILLIMETER
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the display value of the unit
     */
    public String getDisplayValue() {
        return displayValue;
    }

    /**
     * Gets the dimension (group of units).
     * <p>
     * E.g. Dimension.DISTANCE for MILLIMETER, CENTIMETER and so on.
     */
    public Dimension getDimension() {
        return dimension;
    }

    /**
     * Gets the fractional unit of a given decimal place.
     * <p>
     * E.g. 0.002m -> 2mm
     */
    public Unit getFractionalUnit(int decimal) {
        if (decimal == 0) {
            return this;
        }

        for (Conversion conversion : conversions) {
            if (conversion.from == this && conversion.factor == (int) Math.pow(10, decimal)) {
                return conversion.to;
            }
        }

        return null;
    }

    /**
     * Gets the smallest possible unit of a dimension group
     */
    public Unit getSmallestUnit() {
        switch (this) {
            case MILLILITER:
            case CENTILITER:
            case DECILITER:
            case LITER:
                return Unit.MILLILITER;
            case CUBIC_METER:
            case CUBIC_CENTIMETER:
            case MILLIMETER:
            case CENTIMETER:
            case DECIMETER:
            case METER:
                return Unit.MILLIMETER;
            case SQUARE_METER:
            case SQUARE_METER_TENTH:
            case SQUARE_DECIMETER:
            case SQUARE_DECIMETER_TENTH:
            case SQUARE_CENTIMETER:
                return Unit.SQUARE_CENTIMETER;
            case GRAM:
            case DECAGRAM:
            case HECTOGRAM:
            case KILOGRAM:
            case TONNE:
                return Unit.GRAM;
            default:
                return null;
        }
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

    private static final List<Conversion> conversions = new ArrayList<>();

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

    /**
     * Converts a given value from one unit to another.
     * <p>
     * Returns the same value if a conversion is not possible. (e.g. KILOGRAM -> MILLIMETER)
     */
    public static @NonNull BigDecimal convert(@NonNull BigDecimal value, @NonNull Unit from, @NonNull Unit to) {
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
