package io.snabble.sdk;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ProductDatabaseTest extends SnabbleSdkTest {

    @Test
    public void testAllPromotionsQuery() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product[] products = productDatabase.getDiscountedProducts();
        assertEquals(2, products.length);
        assertEquals(products[0].getSku(), "1");
        assertEquals(products[0].getName(), "Müllermilch Banane 0,4l");
        assertEquals(products[1].getSku(), "2");
        assertEquals(products[1].getName(), "Coca-Cola 1l");
    }

    @Test
    public void testBoostedPromotionsQuery() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product[] products = productDatabase.getBoostedProducts(2);
        assertEquals(2, products.length);
        assertEquals(products[0].getSku(), "2");
        assertEquals(products[1].getSku(), "1");

        products = productDatabase.getBoostedProducts(1);
        assertTrue(products.length == 1);
        assertEquals(products[0].getSku(), "2");
    }

    @Test
    public void testTextSearch() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_6.sqlite3");

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
    public void testTextSearchNoFTS() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_6_no_fts.sqlite3", true);

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
        Cursor cursor = productDatabase.searchByCode("402550", null);
        cursor.moveToFirst();
        Product product = productDatabase.productAtCursor(cursor);
        assertEquals(product.getSku(), "1");
        assertEquals(product.getName(), "Müllermilch Banane 0,4l");
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
                assertFalse(true);
            }
            set.add(p);
        }
        cursor.close();
    }

    @Test
    public void testFindByCode() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findByCode("4025500133627");
        Product product2 = productDatabase.findByCode("2");
        Product product3 = productDatabase.findByCode("000000000000004025500133627");

        assertEquals(product.getSku(), "1");
        assertEquals(product.getName(), "Müllermilch Banane 0,4l");
        assertEquals(product, product2);
        assertEquals(product.getScannableCodes().length, 2);
        assertEquals(product, product3);

        assertNull(productDatabase.findByCode("unknownCode"));
    }

    @Test
    public void testFindBySku() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findBySku("1");
        assertEquals(product.getSku(), "1");
        assertEquals(product.getName(), "Müllermilch Banane 0,4l");

        product = productDatabase.findBySku("123");
        assertNull(product);
    }

    @Test
    public void testSaleRestriction() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_6.sqlite3");

        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findBySku("1");
        assertEquals(product.getSaleRestriction(), Product.SaleRestriction.NONE);

        product = productDatabase.findBySku("37");
        assertEquals(product.getSaleRestriction(), Product.SaleRestriction.MIN_AGE_16);
    }

    @Test
    public void testSaleStop() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_6.sqlite3");

        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findBySku("1");
        assertEquals(product.getSaleStop(), false);

        product = productDatabase.findBySku("42");
        assertEquals(product.getSaleStop(), true);
    }

    @Test
    public void testFindBySkuOnline() {
        ProductDatabase productDatabase = project.getProductDatabase();
        final Product product = findBySkuBlocking(productDatabase, "online1");
        assertEquals(product.getSku(), "online1");
        assertArrayEquals(product.getScannableCodes(), new String[]{"0"});
        assertEquals(product.getTransmissionCode("0"), "0");

        final Product product2 = findBySkuBlocking(productDatabase, "online2");
        assertEquals(product2.getSku(), "online2");
        assertArrayEquals(product2.getScannableCodes(), new String[]{"1"});
        assertEquals(product2.getTransmissionCode("1"), "000001");

        assertNull(findBySkuBlocking(productDatabase, "unknownCode"));
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
    public void testFindByMultipleSkusOnline() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product[] products = findBySkusBlocking(productDatabase, new String[] {"online1", "online2"});

        assertEquals(2, products.length);

        assertEquals(products[0].getSku(), "online1");
        assertEquals(products[1].getSku(), "online2");

        products = findBySkusBlocking(productDatabase, new String[] {"online2", "not_there", "online1"});

        assertEquals(2, products.length);

        assertEquals(products[0].getSku(), "online2");
        assertEquals(products[1].getSku(), "online1");
    }

    private Product[] findBySkusBlocking(ProductDatabase productDatabase, String[] skus) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final Product[][] productArr = new Product[1][];

        productDatabase.findBySkusOnline(skus, new OnProductsAvailableListener() {
            @Override
            public void onProductsAvailable(Product[] products, boolean wasOnline) {
                productArr[0] = products;
                countDownLatch.countDown();
            }

            @Override
            public void onProductsNotFound() {
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
    public void testFindByWeighItemId() {
        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findByWeighItemId("2810540000000");
        assertEquals(product.getSku(), "3");
        assertEquals(product.getName(), "Äpfel");

        product = productDatabase.findByWeighItemId("123");
        assertNull(product);
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
    public void testApplyChangeSetInvalidDoesNotModifyDb() throws IOException {
        ProductDatabase productDatabase = project.getProductDatabase();
        String changeSet = "UPDATE prices SET discountedPrice=59 WHERE sku=1;\n\n" +
                "DELETE FROM pricesASDF WHER!E sku=3;\n\n" +
                "DELETE FROM products WHERE???? sku=3;\n\n" +
                "DELETE FROM weighIIDHFIDUFHUDSFtemIds WHERE sku=3;\n\n" +
                "DELETE FROM searchByName WHERE docid=3;";

        Product product = productDatabase.findBySku("1");
        int p = product.getDiscountedPrice();

        productDatabase.applyDeltaUpdate(new ByteArrayInputStream(changeSet.getBytes()));

        product = productDatabase.findBySku("1");
        assertEquals(p, product.getDiscountedPrice());
    }

    @Test
    public void testFullUpdate() throws IOException {
        ProductDatabase productDatabase = project.getProductDatabase();
        productDatabase.applyFullUpdate(context.getAssets().open("testUpdateDb.sqlite3"));

        Product product = productDatabase.findBySku("1");
        assertEquals("Nutella", product.getName());

        product = productDatabase.findBySku("2");
        assertNull(product);
    }

    @Test
    public void testFullUpdateDoesNotModifyOnCorruptedFile() throws IOException {
        ProductDatabase productDatabase = project.getProductDatabase();
        byte[] bytes = IOUtils.toByteArray(context.getAssets().open("testUpdateDb.sqlite3"));
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
        InputStream is = context.getResources().getAssets().open("testUpdateDb.sqlite3");
        File outputFile = context.getDatabasePath("testUpdateDb.sqlite3");
        FileOutputStream fos = new FileOutputStream(outputFile);
        IOUtils.copy(is, fos);

        SQLiteDatabase db = context.openOrCreateDatabase("testUpdateDb.sqlite3", Context.MODE_PRIVATE, null);
        db.execSQL("UPDATE metadata SET value = 2 WHERE metadata.key = 'schemaVersionMajor'");
        db.close();

        Product product = productDatabase.findBySku("1");
        String name = product.getName();

        productDatabase.applyFullUpdate(new FileInputStream(outputFile));

        product = productDatabase.findBySku("1");
        assertEquals(name, product.getName());
    }

    @Test
    public void testFindProductWithByEan8WhenScanningEan13() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_6.sqlite3");

        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findByCode("42276630");
        Assert.assertNotNull(product);

        String[] codes = product.getScannableCodes();
        Assert.assertTrue(ArrayUtils.contains(codes, "42276630"));

        Product product2 = productDatabase.findByCode("0000042276630");
        Assert.assertNotNull(product2);

        String[] codes2 = product2.getScannableCodes();
        Assert.assertTrue(ArrayUtils.contains(codes2, "42276630"));
    }

    @Test
    public void testFindProductWithEan13ByEan8() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_6.sqlite3");

        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findByCode("40084015");
        Assert.assertNotNull(product);

        String[] codes = product.getScannableCodes();
        Assert.assertTrue(ArrayUtils.contains(codes, "0000040084015"));
    }

    @Test
    public void testTransmissionCodeIsSameOnOldDbVersion() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_6.sqlite3");

        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findBySku("48");
        Assert.assertNotNull(product);

        Assert.assertEquals(product.getTransmissionCode(product.getScannableCodes()[0]), product.getScannableCodes()[0]);
    }

    @Test
    public void testTransmissionCode() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_11.sqlite3");

        ProductDatabase productDatabase = project.getProductDatabase();
        Product product = productDatabase.findBySku("48");
        Assert.assertNotNull(product);

        Assert.assertEquals(product.getTransmissionCode(product.getScannableCodes()[0]), "00000" + product.getScannableCodes()[0]);
    }

    @Test
    public void testRecoverFromFileCorruptions() throws IOException, Snabble.SnabbleException, InterruptedException {
        withDb("testDb_corrupt.sqlite3");
        Assert.assertNull(project.getProductDatabase().findBySku("1"));

        prepareUpdateDb("testDb.sqlite3");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        project.getProductDatabase().update(new ProductDatabase.UpdateCallback() {
            @Override
            public void success() {
                countDownLatch.countDown();
            }

            @Override
            public void error() {
                Assert.fail();
            }
        });

        countDownLatch.await();

        Product product = project.getProductDatabase().findBySku("1");

        Assert.assertNotNull(product);
        Assert.assertEquals("1", product.getSku());
    }
}
