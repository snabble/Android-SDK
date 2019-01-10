package io.snabble.sdk;

public class CustomerCardInfo {
    private String cardId;
    private boolean isRequired;

    CustomerCardInfo(String cardId, boolean isRequired) {
        this.cardId = cardId;
        this.isRequired = isRequired;
    }

    public String getCardId() {
        return cardId;
    }

    public boolean isRequired() {
        return isRequired;
    }
}
