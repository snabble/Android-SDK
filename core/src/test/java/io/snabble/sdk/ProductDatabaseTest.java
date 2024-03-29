package io.snabble.sdk;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import io.snabble.sdk.codes.ScannedCode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

@RunWith(RobolectricTestRunner.class)
public class ProductDatabaseTest extends SnabbleSdkTest {
    @Test
    public void testAllPromotionsQuery() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product[] products = productDatabase.getDiscountedProducts();
        assertEquals(17, products.length);
    }

    @Test
    public void testTextSearchNoFTS() throws IOException, Snabble.SnabbleException {
        withDb("test_1_25.sqlite3", true, null);
        ProductDatabase productDatabase = project.getProductDatabase();
        Cursor cursor = productDatabase.searchByFoldedName("gold", null);
        cursor.moveToFirst();
        Product product = productDatabase.productAtCursor(cursor);
        assertEquals(product.getSku(), "31");
        assertEquals(product.getName(), "Goldbären 200g");
        cursor.close();
        cursor = productDatabase.searchByFoldedName("foo", null);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void testCodeSearch() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Cursor cursor = productDatabase.searchByCode("400825", null);
        cursor.moveToFirst();
        Product product = productDatabase.productAtCursor(cursor);
        assertEquals(product.getSku(), "1");
        assertEquals(product.getName(), "Chiasamen");
        cursor.close();

        cursor = productDatabase.searchByCode("02371231", null);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void testMultipleResultsAreDistinct() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Cursor cursor = productDatabase.searchByCode("5", null);
        Set<Product> set = new HashSet<>();
        while (cursor.moveToNext()) {
            Product p = productDatabase.productAtCursor(cursor);
            if (set.contains(p)) {
                fail();
            }
            set.add(p);
        }
        cursor.close();
    }

    @Test
    public void testFindByCode() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findByCode(ScannedCode.parse(project, "0885580466725").get(0));
        assertEquals(product.getSku(), "6");
        assertNull(productDatabase.findByCode(ScannedCode.parse(project, "unknownCode").get(0)));
    }

    @Test
    public void testFindBySku() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findBySku("1");
        assertEquals(product.getSku(), "1");
        assertEquals(product.getName(), "Chiasamen");

        product = productDatabase.findBySku("asdf123");
        assertNull(product);
    }

    @Test
    public void testSaleRestriction() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findBySku("1");
        assertEquals(product.getSaleRestriction(), Product.SaleRestriction.NONE);

        product = productDatabase.findBySku("37");
        assertEquals(product.getSaleRestriction(), Product.SaleRestriction.MIN_AGE_16);
    }

    @Test
    public void testSaleStop() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findBySku("1");
        assertFalse(product.getSaleStop());

        product = productDatabase.findBySku("42");
        assertTrue(product.getSaleStop());
    }

    @Test
    public void testFindBySkuOnline() throws Throwable {
        ProductDatabase productDatabase = project.getProductDatabase();
        final Product product = findBySkuBlocking(productDatabase, "1");
        assertEquals(product.getSku(), "1");
        containsCode(product, "4008258510001");
        assertNull(product.getTransmissionCode(project, "default", "0", 0));

        final Product product2 = findBySkuBlocking(productDatabase, "2");
        assertEquals(product2.getSku(), "2");
        containsCode(product2, "asdf123");
        assertEquals(product2.getTransmissionCode(project, "default", "asdf123", 0), "trans123");

        assertNull(findBySkuBlocking(productDatabase, "unknownCode"));
    }

    @Test
    public void testFindBySkuOnlineWithShopSpecificPrice() {
        ProductDatabase productDatabase = project.getProductDatabase();
        final Product product = findBySkuBlocking(productDatabase, "1");
        assertEquals(product.getSku(), "1");
        assertEquals(product.getListPrice(), 399);
        Snabble.getInstance().setCheckedInShop(project.getShops().get(1));
        final Product product2 = findBySkuBlocking(productDatabase, "1");
        assertEquals(product2.getSku(), "1");
        assertEquals(product2.getListPrice(), 299);
    }

    @Test
    public void testFindBySkuWithShopSpecificPrice() {
        ProductDatabase productDatabase = project.getProductDatabase();
        final Product product = productDatabase.findBySku("salfter-classic");
        assertEquals(product.getSku(), "salfter-classic");
        assertEquals(product.getListPrice(), 100);

        Snabble.getInstance().setCheckedInShop(project.getShops().get(3));
        final Product product2 = productDatabase.findBySku("salfter-classic");
        assertEquals(product2.getSku(), "salfter-classic");
        assertEquals(product2.getListPrice(), 200);
    }

    private Product findBySkuBlocking(ProductDatabase productDatabase, String sku) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final Product[] productArr = new Product[1];

        productDatabase.findBySkuOnline(sku, new OnProductAvailableListener() {
            @Override
            public void onProductAvailable(Product product, boolean wasOnlineProduct) {
                productArr[0] = product;
                countDownLatch.countDown();
            }

            @Override
            public void onProductNotFound() {
                productArr[0] = null;
                countDownLatch.countDown();
            }

            @Override
            public void onError() {
                productArr[0] = null;
                countDownLatch.countDown();
            }
        }, true);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return productArr[0];
    }

    @Test
    public void testFindByMultipleSkus() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product[] products = productDatabase.findBySkus(new String[] {"1", "2", "asdf1234"});

        assertEquals(products.length, 2);
        assertEquals(products[0].getSku(), "1");
        assertEquals(products[1].getSku(), "2");

        products = productDatabase.findBySkus(new String[] {"1"});

        assertEquals(products.length, 1);
        assertEquals(products[0].getSku(), "1");
    }

    @Test
    public void testApplyChangeSet() throws IOException {
        ProductDatabase productDatabase = project.getProductDatabase();
        String changeSet = "UPDATE prices SET discountedPrice=59 WHERE sku=1;\n\n" +
                "DELETE FROM prices WHERE sku=3;\n\n" +
                "DELETE FROM products WHERE sku=3;\n\n" +
                "DELETE FROM weighItemIds WHERE sku=3;";

        productDatabase.applyDeltaUpdate(new ByteArrayInputStream(changeSet.getBytes()));

        assertNull(productDatabase.findBySku("3"));

        Product product = productDatabase.findBySku("1");
        assertEquals(59, product.getDiscountedPrice());
    }

    @Test
    public void testApplyChangeSetWithNewLineAtEnd() throws IOException {
        ProductDatabase productDatabase = project.getProductDatabase();
        String changeSet = "UPDATE prices SET discountedPrice=59 WHERE sku=1;\n\n" +
                "DELETE FROM prices WHERE sku=3;\n\n" +
                "DELETE FROM products WHERE sku=3;\n\n" +
                "DELETE FROM weighItemIds WHERE sku=3;\n\n";

        productDatabase.applyDeltaUpdate(new ByteArrayInputStream(changeSet.getBytes()));

        assertNull(productDatabase.findBySku("3"));

        Product product = productDatabase.findBySku("1");
        assertEquals(59, product.getDiscountedPrice());
    }

    @Test
    public void testApplyChangeSetInvalidDoesNotModifyDb() {
        ProductDatabase productDatabase = project.getProductDatabase();
        String changeSet = "UPDATE prices SET discountedPrice=59 WHERE sku=1;\n\n" +
                "DELETE FROM pricesASDF WHER!E sku=3;\n\n" +
                "DELETE FROM products WHERE???? sku=3;\n\n" +
                "DELETE FROM weighIIDHFIDUFHUDSFtemIds WHERE sku=3;\n\n" +
                "DELETE FROM searchByName WHERE docid=3;";

        Product product = productDatabase.findBySku("1");
        int p = product.getListPrice();

        boolean exceptionThrown = false;
        try {
            productDatabase.applyDeltaUpdate(new ByteArrayInputStream(changeSet.getBytes()));
        } catch (IOException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);

        product = productDatabase.findBySku("1");
        assertEquals(p, product.getListPrice());
    }

    @Test
    public void testFullUpdate() throws IOException {
        ProductDatabase productDatabase = project.getProductDatabase();
        productDatabase.applyFullUpdate(getInputStream("update_1_25.sqlite3"));

        Product product = productDatabase.findBySku("1337");

        assertEquals(product.getListPrice(), 349);
        assertEquals(product.getName(), "UPDATE");

        product = productDatabase.findBySku("16");
        assertNull(product);
    }

    @Test
    public void testFullUpdateDoesNotModifyOnCorruptedFile() throws IOException {
        ProductDatabase productDatabase = project.getProductDatabase();
        byte[] bytes = IOUtils.toByteArray(getInputStream("update_1_25.sqlite3"));
        for (int i = 0; i < bytes.length; i++) {
            if (i % 4 == 0) {
                bytes[i] = 42;
            }
        }

        Product product = productDatabase.findBySku("1");
        String name = product.getName();

        productDatabase.applyFullUpdate(new ByteArrayInputStream(bytes));

        product = productDatabase.findBySku("1");
        assertEquals(name, product.getName());
    }

    @Test
    public void testFullUpdateDoesNotModifyOnWrongMajorVersion() throws IOException {
        ProductDatabase productDatabase = project.getProductDatabase();
        InputStream is = getInputStream("update_1_25.sqlite3");
        File outputFile = context.getDatabasePath("update_1_25.sqlite3");
        FileOutputStream fos = new FileOutputStream(outputFile);
        IOUtils.copy(is, fos);

        SQLiteDatabase db = context.openOrCreateDatabase("update_1_25.sqlite3", Context.MODE_PRIVATE, null);
        db.execSQL("UPDATE metadata SET value = 2 WHERE metadata.key = 'schemaVersionMajor'");
        db.close();

        Product product = productDatabase.findBySku("1");
        String name = product.getName();

        productDatabase.applyFullUpdate(new FileInputStream(outputFile));

        product = productDatabase.findBySku("1");
        assertEquals(name, product.getName());
    }

    @Test
    public void testFindProductWithByEan8WhenScanningEan13() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findByCode(ScannedCode.parse(project, "42276630").get(0));
        Assert.assertNotNull(product);

        containsCode(product, "42276630");

        Product product2 = productDatabase.findByCode(ScannedCode.parse(project, "0000042276630").get(0));
        Assert.assertNotNull(product2);

        containsCode(product2, "42276630");
    }

    @Test
    public void testFindProductWithEan13ByEan8() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findByCode(ScannedCode.parse(project, "42276630").get(0));
        Assert.assertNotNull(product);

        containsCode(product, "0000042276630");
    }

    private void containsCode(Product product, String code) {
        for (Product.Code pc : product.getScannableCodes()) {
            if (pc.lookupCode.equals(code)) {
                return;
            }
        }

        Assert.fail();
    }

    @Test
    public void testTransmissionCode() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findBySku("48");
        Assert.assertNotNull(product);

        Assert.assertEquals(product.getScannableCodes()[0].transmissionCode, "00000" + product.getScannableCodes()[0].lookupCode);
    }

    @Test
    public void testRecoverFromFileCorruptions() throws IOException, Snabble.SnabbleException, InterruptedException {
        withDb("corrupt.sqlite3");
        Assert.assertNotNull(project.getProductDatabase().findBySku("1"));

        prepareUpdateDb("test_1_25.sqlite3");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        boolean[] fail = new boolean[] {false};
        project.getProductDatabase().update(new ProductDatabase.UpdateCallback() {
            @Override
            public void success() {
                countDownLatch.countDown();
            }

            @Override
            public void error() {
                fail[0] = true;
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
        if(fail[0]) Assert.fail();

        Product product = project.getProductDatabase().findBySku("1");

        Assert.assertNotNull(product);
        Assert.assertEquals("1", product.getSku());
    }

    @Test
    public void testMultiplePricingCategories() throws IOException, Snabble.SnabbleException {
        withDb("test_1_25.sqlite3");
        Snabble.getInstance().setCheckedInShop(project.getShops().get(4));

        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findBySku("multiple-categories");
        Assert.assertNotNull(product);
        Assert.assertEquals(product.getListPrice(), 83);

        Snabble.getInstance().setCheckedInShop(project.getShops().get(2));

        productDatabase = project.getProductDatabase();
        product = productDatabase.findBySku("multiple-categories");
        Assert.assertNotNull(product);
        Assert.assertEquals(product.getListPrice(), 49);
    }

    @Test
    public void testTransmissionTemplates() throws IOException, Snabble.SnabbleException {
        String[] sql = loadSql("transmission_template").split("\n");
        withDb("test_1_25.sqlite3", false, Arrays.asList(sql));

        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findBySku("34-tt");
        Assert.assertNotNull(product);

        String transmissionCode = product.getTransmissionCode(project, "ean13_instore", "12345", 567);
        Assert.assertEquals("TEST_00567_12345", transmissionCode);
    }
}