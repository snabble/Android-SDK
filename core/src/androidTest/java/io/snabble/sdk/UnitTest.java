package io.snabble.sdk;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class UnitTest extends SnabbleSdkTest {
    @Test
    public void testUnitPrices() {
        ProductDatabase productDatabase = project.getProductDatabase();
        final Product test_ml_1 = productDatabase.findBySku("test-ml-1");
        final Product test_g_1 = productDatabase.findBySku("test-g-1");
        final Product test_cm_1 = productDatabase.findBySku("test-cm-1");

        Assert.assertEquals(139, test_ml_1.getPrice());
        Assert.assertEquals(139, test_ml_1.getPriceForQuantity(1000, null, RoundingMode.HALF_UP));
        Assert.assertEquals(60, test_ml_1.getPriceForQuantity(431, null, RoundingMode.HALF_UP));

        Assert.assertEquals(299, test_g_1.getPrice());
        Assert.assertEquals(299, test_g_1.getPriceForQuantity(1000, null, RoundingMode.HALF_UP));
        Assert.assertEquals(150, test_g_1.getPriceForQuantity(500, null, RoundingMode.HALF_UP));

        Assert.assertEquals(199, test_cm_1.getPrice());
        Assert.assertEquals(1990, test_cm_1.getPriceForQuantity(1000, null, RoundingMode.HALF_UP));
        Assert.assertEquals(498, test_cm_1.getPriceForQuantity(250, null, RoundingMode.HALF_UP));
        Assert.assertEquals(497, test_cm_1.getPriceForQuantity(250, null, RoundingMode.FLOOR));
    }

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
