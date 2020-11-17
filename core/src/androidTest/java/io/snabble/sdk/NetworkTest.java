package io.snabble.sdk;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NetworkTest extends SnabbleSdkTest {
    @Test
    public void testLetsEncrypt() throws IOException {
        OkHttpClient okHttpClient = project.getOkHttpClient();

        Response response = okHttpClient.newCall(new Request.Builder().url("https://valid-isrgrootx1.letsencrypt.org/robots.txt").build()).execute();
        Assert.assertTrue(response.code() == 404 || response.code() == 200);
        response = okHttpClient.newCall(new Request.Builder().url("https://google.com/robots.txt").build()).execute();
        Assert.assertTrue(response.code() == 404 || response.code() == 200);
    }
}
