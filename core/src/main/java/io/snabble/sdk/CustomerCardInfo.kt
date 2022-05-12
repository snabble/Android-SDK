package io.snabble.sdk;

public class CustomerCardInfo {
    private final String cardId;
    private final boolean isRequired;

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