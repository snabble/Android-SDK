package io.snabble.sdk;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.snabble.sdk.codes.ScannedCode;

public class ShoppingCartTest extends SnabbleSdkTest {
    private ShoppingCart cart;
    private TestProduct simpleProduct1;
    private TestProduct simpleProduct2;
    private TestProduct simpleProduct3;
    private TestProduct userWeighedProduct;
    private TestProduct preWeighedProduct;
    private TestProduct pieceProduct;
    private TestProduct priceProduct;
    private TestProduct zeroAmountProduct;

    private class TestProduct {
        Product product;
        ScannedCode scannedCode;

        public TestProduct(Product product, ScannedCode scannedCode) {
            this.product = product;
            this.scannedCode = scannedCode;
        }

        public ShoppingCart.Item cartItem() {
            return cart.newItem(product, scannedCode);
        }
    }

    private void add(TestProduct testProduct) {
        cart.add(testProduct.cartItem());
    }

    private ScannedCode code(String code, String templateName) {
        List<ScannedCode> codes = ScannedCode.parse(project, code);
        for (ScannedCode scannedCode : codes) {
            if (scannedCode.getTemplateName().equals(templateName)) {
                return scannedCode;
            }
        }

        return codes.get(0);
    }

    @Before
    public void setup() {
        cart = new ShoppingCart(project);

        simpleProduct1 = new TestProduct(project.getProductDatabase().findBySku("1"), code("4008258510001", "default"));
        simpleProduct2 = new TestProduct(project.getProductDatabase().findBySku("2"), code("0885580294533", "default"));
        simpleProduct3 = new TestProduct(project.getProductDatabase().findBySku("3"), code("0885580466701", "default"));

        userWeighedProduct = new TestProduct(project.getProductDatabase().findBySku("34"), code("23232327", "default"));
        preWeighedProduct = new TestProduct(project.getProductDatabase().findBySku("34-b"), code("2423230001544", "ean13_instore_chk"));
        pieceProduct = new TestProduct(project.getProductDatabase().findBySku("34-c"), code("2523232000061", "ean13_instore_chk"));
        priceProduct = new TestProduct(project.getProductDatabase().findBySku("34-d"), code("2623237002494", "ean13_instore_chk"));
        zeroAmountProduct = new TestProduct(project.getProductDatabase().findBySku("34-c"), code("2523230000001", "ean13_instore_chk"));
    }

    @Test
    public void testShoppingCart() {
        add(simpleProduct1);

        Assert.assertEquals(cart.size(), 1);
        Assert.assertEquals(cart.get(0).getProduct(), simpleProduct1.product);
        Assert.assertEquals(cart.get(0).getScannedCode(), simpleProduct1.scannedCode);
    }

    @Test
    public void testMerge() {
        Assert.assertTrue(simpleProduct1.cartItem().isMergeable());
        Assert.assertTrue(simpleProduct2.cartItem().isMergeable());
        Assert.assertTrue(simpleProduct3.cartItem().isMergeable());
        Assert.assertFalse(userWeighedProduct.cartItem().isMergeable());
        Assert.assertFalse(preWeighedProduct.cartItem().isMergeable());
        Assert.assertFalse(pieceProduct.cartItem().isMergeable());
        Assert.assertFalse(priceProduct.cartItem().isMergeable());
        Assert.assertFalse(zeroAmountProduct.cartItem().isMergeable());
    }

    @Test
    public void testEditable() {
        add(simpleProduct1);
        Assert.assertTrue(cart.get(0).isEditable());
    }

    @Test
    public void testNotEditable() {
        add(priceProduct);
        add(preWeighedProduct);
        add(pieceProduct);
        Assert.assertFalse(cart.get(0).isEditable());
        Assert.assertFalse(cart.get(1).isEditable());
        Assert.assertFalse(cart.get(2).isEditable());
    }

    @Test
    public void testTotalPrice() {
        add(simpleProduct1);
        Assert.assertEquals(cart.get(0).getTotalPrice(), simpleProduct1.product.getDiscountedPrice());

        cart.get(0).setQuantity(2);
        Assert.assertEquals(cart.get(0).getTotalPrice(), simpleProduct1.product.getDiscountedPrice() * 2);

        add(preWeighedProduct);
        Assert.assertEquals(cart.get(0).getTotalPrice(), 47);

        add(pieceProduct);
        Assert.assertEquals(cart.get(0).getTotalPrice(), pieceProduct.product.getDiscountedPrice() * 6);

        add(priceProduct);
        Assert.assertEquals(cart.get(0).getTotalPrice(), priceProduct.scannedCode.getEmbeddedData());
    }

    @Test
    public void testZeroAmountProduct() {
        add(zeroAmountProduct);
        Assert.assertEquals(cart.size(), 1);

        add(zeroAmountProduct);
        Assert.assertEquals(cart.size(), 2);

        Assert.assertTrue(cart.get(0).isEditable());
        Assert.assertFalse(cart.get(0).isMergeable());
    }

    @Test
    public void testPriceText() {
        ShoppingCart.Item item = simpleProduct1.cartItem();
        assertStrEquals(item.getPriceText(), " 3,99 €");
        assertStrEquals(item.getQuantityText(), "1");
        assertStrEquals(item.getFullPriceText(), "3,99 €");

        item = simpleProduct1.cartItem();
        item.setQuantity(2);
        assertStrEquals(item.getPriceText(), " * 3,99 € = 7,98 €");
        assertStrEquals(item.getQuantityText(), "2");
        assertStrEquals(item.getFullPriceText(), "2 * 3,99 € = 7,98 €");

        item = preWeighedProduct.cartItem();
        assertStrEquals(item.getPriceText(), " * 2,99 € / kg = 0,47 €");
        assertStrEquals(item.getQuantityText(), "154g");
        assertStrEquals(item.getFullPriceText(), "154g * 2,99 € / kg = 0,47 €");

        item = userWeighedProduct.cartItem();
        item.setQuantity(500);
        assertStrEquals(item.getPriceText(), " * 2,99 € / kg = 1,50 €");
        assertStrEquals(item.getQuantityText(), "500g");
        assertStrEquals(item.getFullPriceText(), "500g * 2,99 € / kg = 1,50 €");

        item = userWeighedProduct.cartItem();
        item.setQuantity(0);
        assertStrEquals(item.getPriceText(), " 2,99 € / kg");
        assertStrEquals(item.getFullPriceText(), "2,99 € / kg");

        item = pieceProduct.cartItem();
        assertStrEquals(item.getPriceText(), " * 0,49 € = 2,94 €");
        assertStrEquals(item.getQuantityText(), "6");
        assertStrEquals(item.getFullPriceText(), "6 * 0,49 € = 2,94 €");

        item = priceProduct.cartItem();
        assertStrEquals(item.getPriceText(), " 2,49 €");
        assertStrEquals(item.getQuantityText(), "1");
        assertStrEquals(item.getFullPriceText(), "2,49 €");

        item = zeroAmountProduct.cartItem();
        item.setQuantity(4);
        assertStrEquals(item.getPriceText(), " * 0,49 € = 1,96 €");
        assertStrEquals(item.getQuantityText(), "4");
        assertStrEquals(item.getFullPriceText(), "4 * 0,49 € = 1,96 €");
    }

    public void assertStrEquals(String a, String b) {
        Assert.assertEquals(replaceNoBreakSpace(a), replaceNoBreakSpace(b));
    }

    public String replaceNoBreakSpace(String in) {
        return in.replace(" ", " "); // those spaces are different unicode points
    }
}
