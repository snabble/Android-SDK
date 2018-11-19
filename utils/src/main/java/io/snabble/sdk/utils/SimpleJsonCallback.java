package io.snabble.sdk.utils;


public abstract class SimpleJsonCallback<T> extends JsonCallback<T, T> {
    public static class SimpleJsonCallbackException extends Exception {
        public SimpleJsonCallbackException(String message) {
            super(message);
        }
    }

    public SimpleJsonCallback(Class<T> clazz) {
        super(clazz, clazz);
    }

    @Override
    protected void handleFailure(String body) {
        error(new SimpleJsonCallbackException(body));
    }

    @Override
    public void failure(T obj) {

    }
}
