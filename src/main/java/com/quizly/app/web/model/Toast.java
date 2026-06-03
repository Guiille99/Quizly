package com.quizly.app.web.model;

public class Toast {
    private String message;
    private String type; // success, error, warning, info

    public Toast() {
    }

    public Toast(String message, String type) {
        this.message = message;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public static Toast success(String msg) {
        return new Toast(msg, "success");
    }

    public static Toast error(String msg) {
        return new Toast(msg, "error");
    }

    public static Toast info(String msg) {
        return new Toast(msg, "info");
    }

    public static Toast warning(String msg) {
        return new Toast(msg, "warning");
    }
}
