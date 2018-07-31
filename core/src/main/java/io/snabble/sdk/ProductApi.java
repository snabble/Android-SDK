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

    private Gson gson;
    private SnabbleSdk sdkInstance;
    private OkHttpClient okHttpClient;
    private Handler handler;

    ProductApi(SnabbleSdk sdkInstance) {
        this.sdkInstance = sdkInstance;
        this.okHttpClient = sdkInstance.getOkHttpClient();
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

    private void get(final String url, final OnProductAvailableListener productAvailableListener) {
        final Request request = new Request.Builder()
                .url(sdkInstance.absoluteUrl(url))
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
                        if (apiProduct.depositProduct != null && !apiProduct.depositProduct.equals("")) {
                            findBySku(apiProduct.depositProduct, new OnProductAvailableListener() {
                                @Override
                                public void onProductAvailable(final Product product, boolean wasOnlineProduct) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            productAvailableListener.onProductAvailable(toProduct(apiProduct, product), true);
                                        }
                                    });
                                }

                                @Override
                                public void onProductNotFound() {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            productAvailableListener.onProductNotFound();
                                        }
                                    });
                                }

                                @Override
                                public void onError() {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            productAvailableListener.onError();
                                        }
                                    });
                                }
                            });
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    productAvailableListener.onProductAvailable(toProduct(apiProduct, null), true);
                                }
                            });
                        }
                    } catch (JsonParseException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                productAvailableListener.onError();
                            }
                        });
                    }
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (response.code() == 404) {
                                productAvailableListener.onProductNotFound();
                            } else {
                                productAvailableListener.onError();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        productAvailableListener.onError();
                    }
                });
            }
        });
    }

    private Product toProduct(ApiProduct apiProduct, Product depositProduct) {
        Product.Builder builder = new Product.Builder()
                .setSku(apiProduct.sku)
                .setName(apiProduct.name)
                .setDescription(apiProduct.description)
                .setSubtitle(apiProduct.subtitle)
                .setBoost(apiProduct.boost)
                .setDepositProduct(depositProduct)
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
                if("piece".equals(apiProduct.weighing.encodingUnit)) {
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
