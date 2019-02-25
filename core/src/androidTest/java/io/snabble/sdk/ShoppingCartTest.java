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
        Assert.assertTrue(simpleProduct1.cartItem().isMergeRequired());
        Assert.assertTrue(simpleProduct2.cartItem().isMergeRequired());
        Assert.assertTrue(simpleProduct3.cartItem().isMergeRequired());
        Assert.assertFalse(userWeighedProduct.cartItem().isMergeRequired());
        Assert.assertFalse(preWeighedProduct.cartItem().isMergeRequired());
        Assert.assertFalse(pieceProduct.cartItem().isMergeRequired());
        Assert.assertFalse(priceProduct.cartItem().isMergeRequired());
        Assert.assertFalse(zeroAmountProduct.cartItem().isMergeRequired());
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
        Assert.assertFalse(cart.get(0).isMergeRequired());
    }
}
