package io.snabble.sdk;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleJsonCallback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

class ProductApi {
    private static class ApiProduct {
        private String sku;
        private String name;
        private String description;
        private String subtitle;
        private String taxCategory;
        private String depositProduct;
        private boolean outOfStock;
        private boolean deleted;
        private String imageUrl;
        private String productType;
        private String[] eans;
        private int price;
        private int discountedPrice;
        private String basePrice;
        private boolean saleStop;
        private ApiScannableCode[] codes;
        private Product.SaleRestriction saleRestriction = Product.SaleRestriction.NONE;
        private ApiWeighing weighing;
    }

    private static class ApiScannableCode {
        private String code;
        private String transmissionCode;
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

    private Project project;
    private OkHttpClient okHttpClient;
    private Handler handler;

    ProductApi(Project project) {
        this.project = project;
        this.okHttpClient = project.getOkHttpClient();
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void findBySku(String sku, final OnProductAvailableListener productAvailableListener) {
        if (productAvailableListener == null) {
            return;
        }

        String url = project.getProductBySkuUrl();
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
        url = appendShopId(url);

        get(url, productAvailableListener);
    }

    public void findBySkus(final String[] skus, final OnProductsAvailableListener productsAvailableListener) {
        if (productsAvailableListener == null) {
            return;
        }

        String url = project.getProductsBySkus();
        if (url == null) {
            Logger.e("Could not check for products online, no productsBySku url provided in metadata");
            productsAvailableListener.onError();
            return;
        }

        if (skus == null || skus.length == 0) {
            productsAvailableListener.onProductsAvailable(new Product[0], true);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(url);

        boolean first = true;

        for(String sku : skus) {
            if(first){
                sb.append('?');
                first = false;
            } else {
                sb.append('&');
            }

            sb.append("skus=");
            sb.append(sku);
        }

        url = sb.toString();
        url = appendShopId(url);

        final Request request = new Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(url))
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new SimpleJsonCallback<ApiProductGroup>(ApiProductGroup.class) {
            @Override
            public void success(ApiProductGroup apiProductGroup) {
                final CountDownLatch countDownLatch = new CountDownLatch(apiProductGroup.products.length);
                final Map<String, Product> products = new HashMap<>();
                final boolean[] error = new boolean[1];

                for(ApiProduct apiProduct : apiProductGroup.products) {
                    flattenProduct(apiProduct, new OnProductAvailableListener() {
                        @Override
                        public void onProductAvailable(Product product, boolean wasOnlineProduct) {
                            products.put(product.getSku(), product);

                            countDownLatch.countDown();
                            error[0] = false;
                        }

                        @Override
                        public void onProductNotFound() {
                            countDownLatch.countDown();
                            error[0] = false;
                        }

                        @Override
                        public void onError() {
                            countDownLatch.countDown();
                            error[0] = true;
                        }
                    });
                }

                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    ProductApi.this.error(productsAvailableListener);
                }

                if (error[0]) {
                    ProductApi.this.error(productsAvailableListener);
                } else {
                    List<Product> productList = new ArrayList<>();
                    for(String sku : skus) {
                        Product p = products.get(sku);
                        if (p != null) {
                            productList.add(p);
                        }
                    }

                    ProductApi.this.success(productsAvailableListener, productList.toArray(new Product[productList.size()]));
                }
            }

            @Override
            public void error(Throwable t) {
                ProductApi.this.error(productsAvailableListener);
            }
        });
    }

    public void findByCode(final String code, final OnProductAvailableListener productAvailableListener) {
        if (productAvailableListener == null) {
            return;
        }

        String url = project.getProductByCodeUrl();
        if (url == null) {
            Logger.e("Could not check product online, no productByCode url provided in metadata");
            productAvailableListener.onError();
            return;
        }

        if (code == null) {
            productAvailableListener.onProductNotFound();
            return;
        }

        url = url.replace("{responseCode}", code);
        url = appendShopId(url);

        get(url, productAvailableListener);
    }

    public void findByWeighItemId(String weighItemId, final OnProductAvailableListener productAvailableListener) {
        if (productAvailableListener == null) {
            return;
        }

        String url = project.getProductByWeighItemIdUrl();
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
        url = appendShopId(url);

        get(url, productAvailableListener);
    }

    private void getBundlesOfProduct(final String sku, final ApiProductGroupCallback callback) {
        String url = project.getBundlesOfProductUrl();
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
        url = appendShopId(url);

        final Request request = new Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(url))
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new SimpleJsonCallback<ApiProductGroup>(ApiProductGroup.class) {

            @Override
            public void success(ApiProductGroup apiProductGroup) {
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

                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                        callback.onError();
                    }

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
            }

            @Override
            public void error(Throwable t) {
                if (responseCode() == 404) {
                    callback.onProductsNotFound();
                } else {
                    callback.onError();
                }
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

        okHttpClient.newCall(request).enqueue(new SimpleJsonCallback<ApiProduct>(ApiProduct.class) {
            @Override
            public void success(ApiProduct apiProduct) {
                flattenProduct(apiProduct, productAvailableListener);
            }

            @Override
            public void error(Throwable t) {
                if (responseCode() == 404) {
                    notFound(productAvailableListener);
                } else {
                    ProductApi.this.error(productAvailableListener);
                }
            }
        });
    }

    private void flattenProduct(final ApiProduct apiProduct, final OnProductAvailableListener productAvailableListener) {
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

    private void success(final OnProductsAvailableListener onProductAvailableListener, final Product[] products) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                onProductAvailableListener.onProductsAvailable(products, true);
            }
        });
    }

    private void notFound(final OnProductsAvailableListener onProductAvailableListener) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                onProductAvailableListener.onProductsAvailable(new Product[0], true);
            }
        });
    }

    private void error(final OnProductsAvailableListener onProductAvailableListener) {
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

        if (apiProduct.codes != null) {
            for (ApiScannableCode apiScannableCode : apiProduct.codes) {
                if (apiScannableCode.code != null && apiScannableCode.transmissionCode != null) {
                    builder.addTransmissionCode(apiScannableCode.code, apiScannableCode.transmissionCode);
                }
            }
        }

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

    private String appendShopId(String url) {
        Shop shop = project.getCheckedInShop();
        if(shop != null) {
            url = url + "?shopID=" + shop.getId();
        }
        return url;
    }
}
