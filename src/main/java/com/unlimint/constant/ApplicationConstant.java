package com.unlimint.constant;

import java.util.Arrays;
import java.util.List;

public class ApplicationConstant {
    private ApplicationConstant() {
    }

    public static final String orderId = "Order Id";
    public static final String amount = "amount";
    public static final String currency = "currency";
    public static final String comment = "comment";

    public static final List<String> headerRow = Arrays.asList(orderId, amount, currency, comment);
}
