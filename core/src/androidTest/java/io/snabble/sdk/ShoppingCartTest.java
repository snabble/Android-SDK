package io.snabble.sdk;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.snabble.sdk.codes.ScannedCode;

public class ShoppingCartTest extends SnabbleSdkTest {
    private ShoppingCart2 cart;
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
    }

    private void add(TestProduct testProduct) {
        cart.add(new ShoppingCart2.Item(testProduct.product, testProduct.scannedCode));
    }

    @Before
    public void setup() {
        cart = new ShoppingCart2(project);

        simpleProduct1 = new TestProduct(project.getProductDatabase().findBySku("1"), ScannedCode.parseDefault(project, "4008258510001"));
        simpleProduct2 = new TestProduct(project.getProductDatabase().findBySku("2"), ScannedCode.parseDefault(project, "0885580294533"));
        simpleProduct3 = new TestProduct(project.getProductDatabase().findBySku("3"), ScannedCode.parseDefault(project, "0885580466701"));

        userWeighedProduct = new TestProduct(project.getProductDatabase().findBySku("34"), ScannedCode.parseDefault(project, "23232327"));
        preWeighedProduct = new TestProduct(project.getProductDatabase().findBySku("34-b"), ScannedCode.parseDefault(project, "2423230001544"));
        pieceProduct = new TestProduct(project.getProductDatabase().findBySku("34-c"), ScannedCode.parseDefault(project, "2523232000061"));
        priceProduct = new TestProduct(project.getProductDatabase().findBySku("34-d"), ScannedCode.parseDefault(project, "2623237002494"));
        zeroAmountProduct = new TestProduct(project.getProductDatabase().findBySku("34-c"), ScannedCode.parseDefault(project, "2523230000001"));
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
        add(simpleProduct1);
        add(simpleProduct2);
        Assert.assertEquals(cart.size(), 2);
        add(simpleProduct1);
        Assert.assertEquals(cart.size(), 2);
    }

    @Test
    public void testNoMerge() {
        add(preWeighedProduct);
        Assert.assertEquals(cart.size(), 1);
        add(preWeighedProduct);
        Assert.assertEquals(cart.size(), 2);
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
        Assert.assertEquals(cart.getTotalPrice(), simpleProduct1.product.getDiscountedPrice());
        cart.get(0).setQuantity(2);
        Assert.assertEquals(cart.get(0).getTotalPrice(), simpleProduct1.product.getDiscountedPrice() * 2);
        Assert.assertEquals(cart.getTotalPrice(), simpleProduct1.product.getDiscountedPrice() * 2);

        cart.getTotalPrice()

        add(preWeighedProduct);
    }
}
