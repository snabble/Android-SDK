package io.snabble.sdk;

import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleJsonCallback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Class for interfacing with the snabble Receipts API
 */
public class ReceiptsApi {
    /** Receipts POJO **/
    public static class ApiReceipt {
        public ApiOrder[] orders;
    }

    /** Order POJO **/
    public static class ApiOrder {
        public String id;
        public String project;
        public String date;
        @SerializedName("shopID")
        public String shopId;
        public String shopName;
        public int price;
        public Map<String, ApiLink> links;
    }

    /** Link POJO **/
    public static class ApiLink {
        public String href;
    }

    /**
     * Interface for getting the raw receipts response
     */
    public interface RawReceiptUpdateCallback {
        void success(ApiReceipt receipts);
        void failure();
    }

    /**
     * Interface for getting the a parsed receipts response
     */
    public interface ReceiptUpdateCallback {
        void success(ReceiptInfo[] receiptInfos);
        void failure();
    }

    private final SimpleDateFormat simpleDateFormat;

    public ReceiptsApi() {
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Fetch receipts from the backend, providing the raw json document as a POJO
     */
    public void getRaw(final RawReceiptUpdateCallback rawReceiptUpdateCallback) {
        final Snabble snabble = Snabble.getInstance();
        String url = snabble.getReceiptsUrl();

        if (url != null) {
            Request request = new Request.Builder()
                    .get()
                    .url(url)
                    .build();

            OkHttpClient okHttpClient = snabble.getProjects().get(0).getOkHttpClient();
            okHttpClient.newCall(request).enqueue(new SimpleJsonCallback<ApiReceipt>(ApiReceipt.class) {
                @Override
                public void success(ApiReceipt apiReceipt) {
                    rawReceiptUpdateCallback.success(apiReceipt);
                }

                @Override
                public void error(Throwable t) {
                    rawReceiptUpdateCallback.failure();
                }
            });
        } else {
            rawReceiptUpdateCallback.failure();
        }
    }

    /**
     * Fetch receipts from the backend, parsed as a list of {@link ReceiptInfo}
     */
    public void get(final ReceiptUpdateCallback receiptUpdateCallback) {
        final Snabble snabble = Snabble.getInstance();
        String url = snabble.getReceiptsUrl();

        if (url != null) {
            Request request = new Request.Builder()
                    .get()
                    .url(url)
                    .build();

            if (snabble.getProjects().size() == 0) {
                receiptUpdateCallback.failure();
                return;
            }

            OkHttpClient okHttpClient = snabble.getProjects().get(0).getOkHttpClient();
            okHttpClient.newCall(request).enqueue(new SimpleJsonCallback<ApiReceipt>(ApiReceipt.class) {
                @Override
                public void success(ApiReceipt apiReceipt) {
                    List<Project> projects = snabble.getProjects();
                    HashMap<String, Project> projectsById = new HashMap<>();
                    for (Project project : projects) {
                        projectsById.put(project.getId(), project);
                    }

                    List<ReceiptInfo> result = new ArrayList<>();

                    for (ApiOrder apiOrder : apiReceipt.orders) {
                        ApiLink apiLink = apiOrder.links.get("receipt");
                        if (apiLink == null) {
                            continue;
                        }

                        Project project = projectsById.get(apiOrder.project);
                        if (project != null) {
                            PriceFormatter priceFormatter = project.getPriceFormatter();

                            try {
                                String url = apiLink.href;
                                if (url == null || url.equals("")) {
                                    url = null;
                                }

                                ReceiptInfo receiptInfo = new ReceiptInfo(
                                        apiOrder.id,
                                        apiOrder.project,
                                        simpleDateFormat.parse(apiOrder.date).getTime(),
                                        url != null ? snabble.absoluteUrl(url) : null,
                                        apiOrder.shopName,
                                        priceFormatter.format(apiOrder.price));

                                result.add(receiptInfo);
                            } catch (ParseException e) {
                                Logger.e("Could not parse ReceiptInfo: " + e.getMessage());
                            }
                        }
                    }

                    Collections.sort(result, (o1, o2) -> {
                        Date date1 = new Date(o1.timestamp);
                        Date date2 = new Date(o2.timestamp);
                        return -date1.compareTo(date2);
                    });

                    receiptUpdateCallback.success(result.toArray(new ReceiptInfo[0]));
                }

                @Override
                public void error(Throwable t) {
                    receiptUpdateCallback.failure();
                }
            });
        } else {
            receiptUpdateCallback.failure();
        }
    }
}