package io.snabble.sdk;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.utils.Dispatch;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleJsonCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PaymentOriginCandidateHelper {
    private final Project project;
    private List<PaymentOriginCandidateAvailableListener> listeners = new CopyOnWriteArrayList<>();
    private boolean isPolling;
    private Call call;
    private PaymentOriginCandidate paymentOriginCandidate;

    public PaymentOriginCandidateHelper(Project project) {
        this.project = project;
    }

    public void startPollingIfLinkIsAvailable(CheckoutApi.CheckoutProcessResponse checkoutProcessResponse) {
        if (checkoutProcessResponse.getOriginCandidateLink() == null) {
            return;
        }

        if (isPolling) {
            return;
        }

        isPolling = true;
        poll(checkoutProcessResponse);
    }

    private void poll(CheckoutApi.CheckoutProcessResponse checkoutProcessResponse) {
        if (!isPolling) {
            return;
        }

        Dispatch.background(() -> {
            Request request = new Request.Builder()
                    .get()
                    .url(Snabble.getInstance().absoluteUrl(checkoutProcessResponse.getOriginCandidateLink()))
                    .build();

            call = project.getOkHttpClient().newCall(request);
            call.enqueue(new SimpleJsonCallback<PaymentOriginCandidate>(PaymentOriginCandidate.class) {
                        @Override
                        public void success(PaymentOriginCandidate paymentOriginCandidate) {
                            if (paymentOriginCandidate.isValid()) {
                                paymentOriginCandidate.project = project;
                                PaymentOriginCandidateHelper.this.paymentOriginCandidate = paymentOriginCandidate;
                                notifyPaymentOriginCandidateAvailable(paymentOriginCandidate);
                                stopPolling();
                            } else {
                                poll(checkoutProcessResponse);
                            }
                        }

                        @Override
                        public void error(Throwable t) {
                            if (responseCode() >= 500) {
                                poll(checkoutProcessResponse);
                            }
                        }
                    });
        }, 1000);
    }

    public void stopPolling() {
        if (call != null) {
            call.cancel();
        }

        isPolling = false;
    }

    public void reset() {
        stopPolling();
        paymentOriginCandidate = null;
    }

    public Project getProject() {
        return project;
    }

    public PaymentOriginCandidate getPaymentOriginCandidate() {
        PaymentOriginCandidate candidate = paymentOriginCandidate;
        reset();
        return candidate;
    }

    public void addPaymentOriginCandidateAvailableListener(PaymentOriginCandidateAvailableListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removePaymentOriginCandidateAvailableListener(PaymentOriginCandidateAvailableListener listener) {
        listeners.remove(listener);
    }

    private void notifyPaymentOriginCandidateAvailable(PaymentOriginCandidate paymentOriginCandidate) {
        Dispatch.mainThread(() -> {
            for (PaymentOriginCandidateAvailableListener listener : listeners) {
                listener.onPaymentOriginCandidateAvailable(paymentOriginCandidate);
            }
        });
    }

    public static class PaymentOriginCandidate {
        private transient Project project;
        public String origin;
        public Map<String, CheckoutApi.Href> links;

        public void promote(PaymentCredentials paymentCredentials, PromoteResult result) {
            if (getPromoteLink() == null) {
                result.error();
            }

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("origin", paymentCredentials.getEncryptedData());

            Request request = new Request.Builder()
                    .post(RequestBody.create(MediaType.parse("application/json"), GsonHolder.get().toJson(jsonObject)))
                    .url(Snabble.getInstance().absoluteUrl(getPromoteLink()))
                    .build();

            project.getOkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    result.error();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() == 201) {
                        result.success();
                    } else {
                        result.error();
                    }
                }
            });
        }

        private String getPromoteLink() {
            CheckoutApi.Href link = links.get("promote");
            if (link != null && link.href != null) {
                return link.href;
            }
            return null;
        }

        private boolean isValid() {
            return origin != null && getPromoteLink() != null;
        }
    }

    public interface PromoteResult {
        void success();
        void error();
    }

    public interface PaymentOriginCandidateAvailableListener {
        void onPaymentOriginCandidateAvailable(PaymentOriginCandidate paymentOriginCandidate);
    }
}
