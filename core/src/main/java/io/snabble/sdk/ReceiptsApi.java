package io.snabble.sdk;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleJsonCallback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

class ReceiptsApi {
    private class ApiLink {
        public String href;
    }

    private class ApiReceipt {
        public ApiOrder[] orders;
    }

    private class ApiOrder {
        public String id;
        public String project;
        public String date;
        public String shopName;
        public int price;
        public Map<String, ApiLink> links;
    }

    public interface ReceiptUpdateCallback {
        void success(ReceiptInfo[] receiptInfos);
        void failure();
    }

    private final SimpleDateFormat simpleDateFormat;

    public ReceiptsApi() {
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

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
                           return;
                        }

                        Project project = projectsById.get(apiOrder.project);
                        if (project != null) {
                            PriceFormatter priceFormatter = new PriceFormatter(project);

                            try {
                                ReceiptInfo receiptInfo = new ReceiptInfo(
                                        apiOrder.id,
                                        project,
                                        simpleDateFormat.parse(apiOrder.date),
                                        snabble.absoluteUrl(apiLink.href),
                                        apiOrder.shopName,
                                        priceFormatter.format(apiOrder.price));

                                result.add(receiptInfo);
                            } catch (ParseException e) {
                                Logger.e("Could not parse ReceiptInfo: " + e.getMessage());
                            }
                        }
                    }

                    Collections.sort(result, new Comparator<ReceiptInfo>() {
                        @Override
                        public int compare(ReceiptInfo o1, ReceiptInfo o2) {
                            return -o1.getDate().compareTo(o2.getDate());
                        }
                    });

                    receiptUpdateCallback.success(result.toArray(new ReceiptInfo[result.size()]));
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
