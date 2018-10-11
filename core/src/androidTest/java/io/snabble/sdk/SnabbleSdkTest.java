package io.snabble.sdk;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SnabbleSdkTest {
    protected Project project;
    protected Context context;
    protected static MockWebServer mockWebServer;

    protected static Buffer productDbBuffer;

    @BeforeClass
    public static void setupMockWebServer() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();

        mockWebServer = new MockWebServer();
        mockWebServer.start();

        final String metadataJson = IOUtils.toString(context.getAssets().open("metadata.json"), Charset.forName("UTF-8"));

        final Buffer product1Buffer = new Buffer();
        product1Buffer.readFrom(context.getAssets().open("product.json"));

        final Buffer product2Buffer = new Buffer();
        product2Buffer.readFrom(context.getAssets().open("product2.json"));

        final Buffer productListBuffer = new Buffer();
        productListBuffer.readFrom(context.getAssets().open("product_list.json"));

        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath().contains("/metadata/app")) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(metadataJson);
                } else if (request.getPath().contains("appdb")) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/vnd+sellfio.appdb+sqlite3")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(productDbBuffer);
                } else if (request.getPath().contains("/products/sku/online1")) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(product1Buffer);
                } else if (request.getPath().contains("/products/sku/online2")) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(product2Buffer);
                } else if (request.getPath().contains("/products/search/bySkus")) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(productListBuffer);
                } else if (request.getPath().contains("/products/")) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Cache-Control", "no-cache")
                            .setResponseCode(404);
                } else if (request.getPath().contains("token")) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody("{\"token\":\"\"," +
                                    "\"issuedAt\":" + (System.currentTimeMillis() / 1000) + "," +
                                    "\"expiresAt\":" + (System.currentTimeMillis() / 1000 + TimeUnit.HOURS.toSeconds(1))
                                    + "}")
                            .setResponseCode(200);
                }


                return new MockResponse().setResponseCode(404);
            }
        };

        mockWebServer.setDispatcher(dispatcher);
    }

    @AfterClass
    public static void closeMockWebServer() throws IOException {
        mockWebServer.close();
    }

    @Before
    public void setupSdk() throws Snabble.SnabbleException, IOException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        withDb("testDb.sqlite3");
    }

    public void withDb(String testDbName) throws IOException, Snabble.SnabbleException {
        withDb(testDbName, false);
    }

    public void withDb(String testDbName, boolean generateSearchIndex) throws IOException, Snabble.SnabbleException {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        prepareUpdateDb(testDbName);

        FileUtils.deleteQuietly(context.getFilesDir());
        FileUtils.deleteQuietly(new File(context.getFilesDir().getParentFile(), "/databases/"));

        final Snabble.Config config = new Snabble.Config();
        config.appId = "test";
        config.endpointBaseUrl = "http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort();
        config.secret = "asdf";
        config.generateSearchIndex = generateSearchIndex;

        Snabble snabble = Snabble.getInstance();
        snabble.setupBlocking((Application) context.getApplicationContext(), config);

        project = snabble.getProjects().get(0);
        project.getShoppingCart().clear();

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        project.getProductDatabase().update(new ProductDatabase.UpdateCallback() {
            @Override
            public void success() {
                countDownLatch.countDown();
            }

            @Override
            public void error() {
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void prepareUpdateDb(String assetPath) throws IOException {
        productDbBuffer = new Buffer();
        productDbBuffer.readFrom(context.getAssets().open(assetPath));
    }

    @After
    public void teardownSdk() {
        FileUtils.deleteQuietly(context.getFilesDir());
        FileUtils.deleteQuietly(new File(context.getFilesDir().getParentFile(), "/databases/"));
    }

    @Test
    public void testSdkInitialization() {
        String appDbUrl = project.getAppDbUrl();

        Assert.assertNotNull(appDbUrl);
        Assert.assertTrue(project.getProductDatabase().getRevisionId() > 0);
        Assert.assertTrue(project.getShops().length > 0);
    }
}