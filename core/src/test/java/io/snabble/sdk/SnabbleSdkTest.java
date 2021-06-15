package io.snabble.sdk;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import androidx.test.platform.app.InstrumentationRegistry;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.snabble.sdk.auth.AppUser;
import io.snabble.sdk.auth.AppUserAndToken;
import io.snabble.sdk.auth.Token;
import io.snabble.sdk.utils.GsonHolder;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

@RunWith(RobolectricTestRunner.class)
public class SnabbleSdkTest {
    protected Project project;
    protected Context context;
    protected static MockWebServer mockWebServer;

    protected static Buffer productDbBuffer;

    protected static Buffer loadBuffer(String name) throws IOException {
        final Buffer buffer = new Buffer();
        try(InputStream stream = buffer.getClass().getClassLoader().getResourceAsStream(name)) {
            buffer.readFrom(stream);
        }
        return buffer;
    }

    protected static String loadSql(String name) throws IOException {
        return loadBuffer(name + ".sql").readString(StandardCharsets.UTF_8);
    }

    protected InputStream getInputStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    @BeforeClass
    public static void setupMockWebServer() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        final Buffer metadataJson = loadBuffer("metadata.json");
        final Buffer product1Buffer = loadBuffer("product.json");
        final Buffer product1OtherPriceBuffer = loadBuffer("product_otherprice.json");
        final Buffer product2Buffer = loadBuffer("product2.json");

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
                            .addHeader("Content-Type", "application/vnd+snabble.appdb+sqlite3")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(productDbBuffer);
                } else if (request.getPath().contains("/resolvedProducts/sku/1?shopID=1774")) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(product1OtherPriceBuffer);
                } else if (request.getPath().contains("/resolvedProducts/sku/1")) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(product1Buffer);
                } else if (request.getPath().contains("/resolvedProducts/sku/2")) {
                    return new MockResponse()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(product2Buffer);
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
                } else if (request.getPath().contains("/users")) {
                    Token token = new Token("", "", System.currentTimeMillis() / 1000, System.currentTimeMillis() / 1000 + TimeUnit.HOURS.toSeconds(1));
                    AppUser appUser = new AppUser("user", "geheim");
                    AppUserAndToken appUserAndToken = new AppUserAndToken(token, appUser);
                    return new MockResponse()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(GsonHolder.get().toJson(appUserAndToken))
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
        withDb("test_1_25.sqlite3");
    }

    public void withDb(String testDbName) throws IOException, Snabble.SnabbleException {
        withDb(testDbName, false, null);
    }

    public void withDb(String testDbName, boolean generateSearchIndex, String[] initialSQL) throws IOException, Snabble.SnabbleException {
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
        config.initialSQL = initialSQL;

        Snabble snabble = Snabble.getInstance();
        snabble.setupBlocking((Application) context.getApplicationContext(), config);

        project = snabble.getProjects().get(0);
        project.getShoppingCart().clear();
        project.setCheckedInShop(null);

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
        productDbBuffer = loadBuffer(assetPath);
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