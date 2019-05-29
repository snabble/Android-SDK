package io.snabble.sdk;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.annotations.SerializedName;

import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleJsonCallback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

class ProductApi {
    private static class ApiProduct {
        String sku;
        ApiProductType productType;
        String name;
        String description;
        String subtitle;
        boolean weighByCustomer;
        String referenceUnit;
        String encodingUnit;
        String imageUrl;
        String scanMessage;
        ApiPrice price;
        boolean saleStop;
        ApiScannableCode[] codes;
        Product.SaleRestriction saleRestriction = Product.SaleRestriction.NONE;

        ApiProduct deposit;
        ApiProduct[] bundles;
    }

    private static class ApiPrice {
        int listPrice;
        int discountedPrice;
        int customerCardPrice;
        String basePrice;
    }

    private enum ApiProductType {
        @SerializedName("default")
        DEFAULT,
        @SerializedName("weighable")
        WEIGHABLE,
        @SerializedName("deposit")
        DEPOSIT
    }

    private static class ApiScannableCode {
        String code;
        String template;
        String transmissionCode;
        String encodingUnit;
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

        HttpUrl baseUrl = HttpUrl.parse(url);
        if (baseUrl == null) {
            Logger.e("Could not check product online, malformed url provided in metadata");
            productAvailableListener.onError();
            return;
        }

        HttpUrl.Builder builder = baseUrl.newBuilder();

        Shop shop = project.getCheckedInShop();
        if(shop != null) {
            builder.addQueryParameter("shopID", shop.getId());
        }

        get(builder.build(), productAvailableListener);
    }

    public void findByCode(final ScannedCode code, final OnProductAvailableListener productAvailableListener) {
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

        HttpUrl baseUrl = HttpUrl.parse(url);
        if (baseUrl == null) {
            Logger.e("Could not check product online, malformed url provided in metadata");
            productAvailableListener.onError();
            return;
        }

        HttpUrl.Builder builder = baseUrl.newBuilder()
                .addQueryParameter("code", code.getLookupCode())
                .addQueryParameter("template", code.getTemplateName());

        Shop shop = project.getCheckedInShop();
        if(shop != null) {
            builder.addQueryParameter("shopID", shop.getId());
        }

        get(builder.build(), productAvailableListener);
    }

    private void get(final HttpUrl url, final OnProductAvailableListener productAvailableListener) {
        final Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new SimpleJsonCallback<ApiProduct>(ApiProduct.class) {
            @Override
            public void success(ApiProduct apiProduct) {
                ProductApi.this.success(productAvailableListener, toProduct(apiProduct));
            }

            @Override
            public void error(Throwable t) {
                if (responseCode() == 404) {
                    ProductApi.this.notFound(productAvailableListener);
                } else {
                    ProductApi.this.error(productAvailableListener);
                }
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

    private Product[] toProducts(ApiProduct[] apiProducts) {
        if (apiProducts == null) {
            return null;
        }

        Product[] products = new Product[apiProducts.length];

        for (int i=0; i<apiProducts.length; i++) {
            products[i] = toProduct(apiProducts[i]);
        }

        return products;
    }

    private Product toProduct(ApiProduct apiProduct) {
        if (apiProduct == null) {
            return null;
        }

        Product.Builder builder = new Product.Builder()
                .setSku(apiProduct.sku)
                .setName(apiProduct.name)
                .setDescription(apiProduct.description)
                .setSubtitle(apiProduct.subtitle)
                .setDepositProduct(toProduct(apiProduct.deposit))
                .setBundleProducts(toProducts(apiProduct.bundles))
                .setIsDeposit(apiProduct.productType == ApiProductType.DEPOSIT)
                .setImageUrl(apiProduct.imageUrl)
                .setSaleRestriction(apiProduct.saleRestriction)
                .setSaleStop(apiProduct.saleStop);

        if (apiProduct.codes != null) {
            Product.Code[] codes = new Product.Code[apiProduct.codes.length];

            for (int i=0; i<codes.length; i++) {
                ApiScannableCode apiScannableCode = apiProduct.codes[i];

                Product.Code code = new Product.Code(apiScannableCode.code,
                        apiScannableCode.transmissionCode,
                        apiScannableCode.template,
                        Unit.fromString(apiScannableCode.encodingUnit));

                codes[i] = code;
            }

            builder.setScannableCodes(codes);
        }

        if (apiProduct.price != null) {
            builder.setPrice(apiProduct.price.listPrice);
            builder.setDiscountedPrice(apiProduct.price.discountedPrice);
            builder.setCustomerCardPrice(apiProduct.price.customerCardPrice);
            builder.setBasePrice(apiProduct.price.basePrice);
        }

        Unit referenceUnit = Unit.fromString(apiProduct.referenceUnit);
        builder.setReferenceUnit(referenceUnit);

        Unit encodingUnit = Unit.fromString(apiProduct.encodingUnit);
        builder.setEncodingUnit(encodingUnit);

        if (apiProduct.weighByCustomer) {
            builder.setType(Product.Type.UserWeighed);
        } else {
            if (apiProduct.productType == ApiProductType.WEIGHABLE) {
                if (referenceUnit == null || referenceUnit == Unit.PIECE) {
                    builder.setType(Product.Type.Article);
                } else {
                    builder.setType(Product.Type.PreWeighed);
                }
            } else {
                builder.setType(Product.Type.Article);
            }
        }

        return builder.build();
    }
}
