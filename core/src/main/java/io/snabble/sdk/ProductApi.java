package io.snabble.sdk;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

import io.snabble.sdk.utils.Logger;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class ProductApi {
    private static class ApiProduct {
        private String sku;
        private String name;
        private String description;
        private String subtitle;
        private int boost;
        private String taxCategory;
        private String depositProduct;
        private boolean outOfStock;
        private boolean deleted;
        private String imageUrl;
        private String productType;
        @SerializedName(value = "eans", alternate = "scannableCodes")
        private String[] eans;
        private int price;
        private int discountedPrice;
        private String basePrice;
        private boolean saleStop;
        private Product.SaleRestriction saleRestriction = Product.SaleRestriction.NONE;
        private ApiWeighing weighing;
    }

    private static class ApiWeighing {
        private String[] weighedItemIds;
        private boolean weighByCustomer;
        private String encodingUnit;
    }

    private static class ApiProductGroup {
        private ApiProduct[] products;
    }

    private interface ApiProductGroupCallback {
        void onProductsAvailable(Product[] products);

        void onProductsNotFound();

        void onError();
    }

    private Gson gson;
    private Project sdkInstance;
    private OkHttpClient okHttpClient;
    private Handler handler;

    ProductApi(Project sdkInstance) {
        this.sdkInstance = sdkInstance;
        this.okHttpClient = Snabble.getInstance().getOkHttpClient();
        this.gson = new GsonBuilder().create();
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void findBySku(String sku, final OnProductAvailableListener productAvailableListener) {
        if (productAvailableListener == null) {
            return;
        }

        String url = sdkInstance.getProductBySkuUrl();
        if (url == null) {
            Logger.e("Could not check product online, no productBySku url provided in metadata");
            productAvailableListener.onError();
            return;
        }

        if (sku == null) {
            productAvailableListener.onProductNotFound();
            return;
        }

        url = url.replace("{sku}", sku);

        get(url, productAvailableListener);
    }

    public void findByCode(String code, final OnProductAvailableListener productAvailableListener) {
        if (productAvailableListener == null) {
            return;
        }

        String url = sdkInstance.getProductByCodeUrl();
        if (url == null) {
            Logger.e("Could not check product online, no productByCode url provided in metadata");
            productAvailableListener.onError();
            return;
        }

        if (code == null) {
            productAvailableListener.onProductNotFound();
            return;
        }

        url = url.replace("{code}", code);

        get(url, productAvailableListener);
    }

    public void findByWeighItemId(String weighItemId, final OnProductAvailableListener productAvailableListener) {
        if (productAvailableListener == null) {
            return;
        }

        String url = sdkInstance.getProductByWeighItemIdUrl();
        if (url == null) {
            Logger.e("Could not check product online, no productByWeighItemId url provided in metadata");
            productAvailableListener.onError();
            return;
        }

        if (weighItemId == null) {
            productAvailableListener.onProductNotFound();
            return;
        }

        url = url.replace("{id}", weighItemId);

        get(url, productAvailableListener);
    }

    private void getBundlesOfProduct(final String sku, final ApiProductGroupCallback callback) {
        String url = sdkInstance.getBundlesOfProductUrl();
        if (url == null) {
            Logger.e("Could not check product bundles online, no bundlesOfProduct url provided in metadata");

            // return not found, so that normal product requests still work even when not bundle url is provided
            callback.onProductsNotFound();
            return;
        }

        if (sku == null) {
            callback.onProductsNotFound();
            return;
        }

        url = url.replace("{bundledSku}", sku);

        final Request request = new Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(url))
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    if (body == null) {
                        callback.onError();
                        return;
                    }

                    InputStream inputStream = body.byteStream();
                    String json = IOUtils.toString(inputStream, Charset.forName("UTF-8"));
                    inputStream.close();

                    try {
                        final ApiProductGroup apiProductGroup = gson.fromJson(json, ApiProductGroup.class);
                        if (apiProductGroup != null && apiProductGroup.products != null) {
                            final Product[] products = new Product[apiProductGroup.products.length];
                            final CountDownLatch countDownLatch = new CountDownLatch(apiProductGroup.products.length);
                            final boolean[] error = new boolean[1];

                            for (int i = 0; i < apiProductGroup.products.length; i++) {
                                final int index = i;
                                final ApiProduct apiProduct = apiProductGroup.products[i];

                                getDepositProduct(apiProduct, new OnProductAvailableListener() {
                                    @Override
                                    public void onProductAvailable(Product product, boolean wasOnlineProduct) {
                                        countDownLatch.countDown();
                                        products[index] = toProduct(apiProduct, product, null);
                                    }

                                    @Override
                                    public void onProductNotFound() {
                                        countDownLatch.countDown();
                                        error[1] = true;
                                    }

                                    @Override
                                    public void onError() {
                                        countDownLatch.countDown();
                                        error[1] = true;
                                    }
                                });
                            }

                            countDownLatch.await();

                            if (error[0]) {
                                callback.onError();
                            } else {
                                callback.onProductsAvailable(products);
                            }
                        } else {
                            if (apiProductGroup != null) {
                                callback.onProductsNotFound();
                            } else {
                                callback.onError();
                            }
                        }
                    } catch (JsonParseException e) {
                        callback.onError();
                    } catch (InterruptedException e) {
                        callback.onError();
                    }
                } else {
                    if (response.code() == 404) {
                        callback.onProductsNotFound();
                    } else {
                        callback.onError();
                    }
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError();
            }
        });
    }

    private void getDepositProduct(final ApiProduct apiProduct, final OnProductAvailableListener productAvailableListener) {
        if (apiProduct.depositProduct != null && !apiProduct.depositProduct.equals("")) {
            findBySku(apiProduct.depositProduct, productAvailableListener);
        } else {
            productAvailableListener.onProductAvailable(null, true);
        }
    }

    private void get(final String url, final OnProductAvailableListener productAvailableListener) {
        final Request request = new Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(url))
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(final Call call, final Response response) throws IOException {
                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    if (body == null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                productAvailableListener.onError();
                            }
                        });
                        return;
                    }

                    InputStream inputStream = body.byteStream();
                    String json = IOUtils.toString(inputStream, Charset.forName("UTF-8"));
                    inputStream.close();

                    try {
                        final ApiProduct apiProduct = gson.fromJson(json, ApiProduct.class);
                        getDepositProduct(apiProduct, new OnProductAvailableListener() {
                            @Override
                            public void onProductAvailable(final Product product, boolean wasOnlineProduct) {
                                getBundlesOfProduct(apiProduct.sku, new ApiProductGroupCallback() {
                                    @Override
                                    public void onProductsAvailable(final Product[] products) {
                                        Product p = toProduct(apiProduct, product, products);
                                        success(productAvailableListener, p);
                                    }

                                    @Override
                                    public void onProductsNotFound() {
                                        Product p = toProduct(apiProduct, product, null);
                                        success(productAvailableListener, p);
                                    }

                                    @Override
                                    public void onError() {
                                        error(productAvailableListener);
                                    }
                                });
                            }

                            @Override
                            public void onProductNotFound() {
                                notFound(productAvailableListener);
                            }

                            @Override
                            public void onError() {
                                error(productAvailableListener);
                            }
                        });
                    } catch (JsonParseException e) {
                        error(productAvailableListener);
                    }
                } else {
                    if (response.code() == 404) {
                        notFound(productAvailableListener);
                    } else {
                        error(productAvailableListener);
                    }
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                error(productAvailableListener);
            }
        });
    }

    private void success(final OnProductAvailableListener onProductAvailableListener, final Product product) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                onProductAvailableListener.onProductAvailable(product, true);
            }
        });
    }

    private void notFound(final OnProductAvailableListener onProductAvailableListener) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                onProductAvailableListener.onProductNotFound();
            }
        });
    }

    private void error(final OnProductAvailableListener onProductAvailableListener) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                onProductAvailableListener.onError();
            }
        });
    }

    private Product toProduct(ApiProduct apiProduct, Product depositProduct, Product[] bundleProducts) {
        Product.Builder builder = new Product.Builder()
                .setSku(apiProduct.sku)
                .setName(apiProduct.name)
                .setDescription(apiProduct.description)
                .setSubtitle(apiProduct.subtitle)
                .setBoost(apiProduct.boost)
                .setDepositProduct(depositProduct)
                .setBundleProducts(bundleProducts)
                .setIsDeposit("deposit".equals(apiProduct.productType))
                .setImageUrl(apiProduct.imageUrl)
                .setScannableCodes(apiProduct.eans)
                .setPrice(apiProduct.price)
                .setDiscountedPrice(apiProduct.discountedPrice)
                .setBasePrice(apiProduct.basePrice)
                .setSaleRestriction(apiProduct.saleRestriction)
                .setSaleStop(apiProduct.saleStop);

        if (apiProduct.weighing != null) {
            builder.setWeighedItemIds(apiProduct.weighing.weighedItemIds);

            if (apiProduct.weighing.weighByCustomer) {
                builder.setType(Product.Type.UserWeighed);
            } else {
                if ("piece".equals(apiProduct.weighing.encodingUnit)) {
                    builder.setType(Product.Type.Article);
                } else {
                    builder.setType(Product.Type.PreWeighed);
                }
            }
        } else {
            builder.setType(Product.Type.Article);
        }

        return builder.build();
    }
}
