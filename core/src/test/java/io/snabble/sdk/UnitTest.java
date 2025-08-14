package io.snabble.sdk;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class UnitTest extends SnabbleSdkTest {

    @Test
    public void testUnits() {
        testConvert(100, Unit.CENTIMETER, Unit.METER, 1);
        testConvert(1, Unit.METER, Unit.CENTIMETER, 100);
        testConvert(100, Unit.MILLILITER, Unit.LITER, 0.1);
        testConvert(2, Unit.SQUARE_METER, Unit.SQUARE_CENTIMETER, 20_000);
        testConvert(2.5, Unit.SQUARE_METER, Unit.SQUARE_CENTIMETER, 25_000);
        testConvert(3000, Unit.KILOGRAM, Unit.TONNE, 3);
        testConvert(3, Unit.TONNE, Unit.KILOGRAM, 3000);
        testConvert(2, Unit.PRICE, Unit.PRICE, 2);
        testConvert(42, Unit.CENTIMETER, Unit.LITER, 42);
    }

    private void testConvert(double value, Unit from, Unit to, double expected) {
        Assert.assertEquals(Unit.convert(
                new BigDecimal(value), from, to).setScale(16, RoundingMode.HALF_UP),
                new BigDecimal(expected).setScale(16, RoundingMode.HALF_UP));
    }
}
