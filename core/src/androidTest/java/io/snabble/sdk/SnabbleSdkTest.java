package io.snabble.sdk;

import android.app.Application;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

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
import java.nio.charset.StandardCharsets;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SnabbleSdkTest {
    protected static final String projectId = "test-b6024ba";
    protected static final String metadataUrl = "/api/" + projectId + "/metadata/app/android/test/1.0";
    protected static final String testDbName = "testDb.sqlite3";

    protected SnabbleSdk snabbleSdk;
    protected Context context;
    protected static MockWebServer mockWebServer;

    @BeforeClass
    public static void setupMockWebServer() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();

        mockWebServer = new MockWebServer();
        mockWebServer.start();

        final String metadataJson = IOUtils.toString(context.getAssets().open("metadata.json"), StandardCharsets.UTF_8);
        final Buffer productDbBuffer = new Buffer();
        productDbBuffer.readFrom(context.getAssets().open("testDb.sqlite3"));

        final Buffer product1Buffer = new Buffer();
        product1Buffer.readFrom(context.getAssets().open("product.json"));

        final Buffer product2Buffer = new Buffer();
        product2Buffer.readFrom(context.getAssets().open("product2.json"));

        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().equals(metadataUrl)) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(metadataJson);
                } else if (request.getPath().contains("/api/" + projectId + "/appdb")) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/vnd+sellfio.appdb+sqlite3")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(productDbBuffer);
                } else if (request.getPath().contains("/api/" + projectId + "/products/sku/online1")) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/vnd+sellfio.appdb+sqlite3")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(product1Buffer);
                } else if (request.getPath().contains("/api/" + projectId + "/products/sku/online2")) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/vnd+sellfio.appdb+sqlite3")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(product2Buffer);
                } else if (request.getPath().contains("/api/" + projectId + "/products/")) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/vnd+sellfio.appdb+sqlite3")
                            .addHeader("Cache-Control", "no-cache")
                            .setResponseCode(404);
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
    public void setupSdk() throws SnabbleSdk.SnabbleException, IOException, InterruptedException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        FileUtils.deleteQuietly(context.getFilesDir());
        FileUtils.deleteQuietly(new File(context.getFilesDir().getParentFile(), "/databases/"));

        final SnabbleSdk.Config config = new SnabbleSdk.Config();
        config.projectId = projectId;
        config.metadataUrl = metadataUrl;
        config.endpointBaseUrl = "http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort();
        config.productDbName = testDbName;

        snabbleSdk = SnabbleSdk.setupBlocking((Application) context.getApplicationContext(), config);
    }

    @After
    public void teardownSdk() throws IOException {
        FileUtils.deleteQuietly(context.getFilesDir());
        FileUtils.deleteQuietly(new File(context.getFilesDir().getParentFile(), "/databases/"));
    }

    @Test
    public void testSdkInitialization() throws IOException {
        String endpointBaseUrl = snabbleSdk.getEndpointBaseUrl();
        String metadataUrl = snabbleSdk.getMetadataUrl();
        String appDbUrl = snabbleSdk.getAppDbUrl();

        Assert.assertNotNull(endpointBaseUrl);
        Assert.assertNotNull(metadataUrl);
        Assert.assertNotNull(appDbUrl);
        Assert.assertTrue(snabbleSdk.getProductDatabase().getRevisionId() > 0);
        Assert.assertTrue(snabbleSdk.getShops().length > 0);
    }
}