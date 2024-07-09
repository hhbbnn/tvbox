package com.github.tvbox.osc.bean.common;

public class OperationResult<T> {
    private final boolean success;
    private final T data;
    private final String errorMessage;

    private OperationResult(boolean success, T data, String errorMessage) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static <T> OperationResult<T> success() {
        return new OperationResult<>(true, null, null);
    }

    public static <T> OperationResult<T> success(T data) {
        return new OperationResult<>(true, data, null);
    }

    public static <T> OperationResult<T> failure() {
        return new OperationResult<>(false, null, null);
    }

    public static <T> OperationResult<T> failure(String errorMessage) {
        return new OperationResult<>(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        if (!success) {
            throw new IllegalStateException("操作失败，无法获取数据");
        }
        return data;
    }

    public String getErrorMessage() {
        if (success) {
            throw new IllegalStateException("操作成功，没有错误信息");
        }
        return errorMessage;
    }
}
