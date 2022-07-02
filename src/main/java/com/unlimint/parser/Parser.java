package com.unlimint.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unlimint.constant.ApplicationConstant;
import com.unlimint.exception.HeaderRowMissingException;
import com.unlimint.executor.ApplicationExecutor;
import com.unlimint.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class Parser {
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private ApplicationExecutor executor;

    //Counter for record entry in all files to obtain total records after parsed.
    public final AtomicLong idCounter = new AtomicLong(1);

    public String getFileType(Path path) {
        String[] split = path.getFileName().toString().split("\\.");
        return split[split.length - 1];
    }

    public void parseCsvFile(Path filePath) {
        int lineNumber = 2; //Since line number 1 is header row
        try (BufferedReader reader = getBufferReader(filePath)) {
            //Order Id,amount,currency,comment (input csv header)
            String[] header = splitStringBasedOnComma(reader.readLine(), true);
            Map<String, Integer> headerColumNumberMap = getHeaderColumNumberMap(header);
            String row;
            while ((row = reader.readLine()) != null) {
                String[] values = splitStringBasedOnComma(row, false);
                if (header.length == values.length) {
                    Order order = new Order();
                    order.setId(idCounter.getAndIncrement());
                    order.setFilename(filePath.getFileName().toString());
                    order.setResult("OK");
                    setOrderId(order, lineNumber, values[headerColumNumberMap.get(ApplicationConstant.orderId)]);
                    setOrderAmount(order, lineNumber, values[headerColumNumberMap.get(ApplicationConstant.amount)]);
                    setOrderCurrency(order, values[headerColumNumberMap.get(ApplicationConstant.currency)]);
                    setOrderComment(order, values[headerColumNumberMap.get(ApplicationConstant.comment)]);
                    order.setLine(lineNumber++);
                    //output data to stdout in json format in different thread
                    printOrderInJsonFormatInParallel(order);
                } else {
                    printSkippingInfo(lineNumber++, filePath.getFileName().toString());
                }
            }
        } catch (HeaderRowMissingException ex) {
            System.out.println("Skipping filename: " + filePath.getFileName() + ", as HEADER ROW is missing. ");
        } catch (NullPointerException ex) {
            System.out.println("Error! Received a null value, must be a String object to split.");
        } catch (FileNotFoundException ex) {
            System.out.println("Error reading or file not found with name: " + filePath.getFileName());
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void parseJsonFile(Path filePath) {
        int lineNumber = 1;
        try (BufferedReader reader = getBufferReader(filePath)) {
            String row;
            while ((row = reader.readLine()) != null) {
                //{"orderId":3, "amount":1.23, "currency": "USD", "comment": "order payment"} (input json format)
                JsonNode jsonNode = mapper.readTree(row);
                if (checkFieldCount(jsonNode)) {
                    Order order = new Order();
                    order.setId(idCounter.getAndIncrement());
                    order.setResult("Ok");
                    order.setFilename(filePath.getFileName().toString());
                    //converts row(in json format) into jsonTree object for easy read
                    setOrderId(order, lineNumber, stringValue(jsonNode.get("orderId")));
                    setOrderAmount(order, lineNumber, stringValue(jsonNode.get("amount")));
                    setOrderCurrency(order, stringValue(jsonNode.get("currency")));
                    setOrderComment(order, stringValue(jsonNode.get("comment")));
                    order.setLine(lineNumber++);
                    //output data to stdout in json format in different thread
                    printOrderInJsonFormatInParallel(order);
                } else {
                    printSkippingInfo(lineNumber++, filePath.getFileName().toString());
                }

            }
        } catch (FileNotFoundException ex) {
            System.out.println("Error reading or file not found with name: " + filePath.getFileName());
        } catch (IOException ex) {
            System.out.println("Invalid json format at line number: " + (lineNumber - 1) + " in " + filePath.getFileName() + " file");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void printOrderInJsonFormatInParallel(Order order) {
        //prints in parallel
        executor.addFuture(executor.getExecutor().submit(() ->
        {
            try {
                System.out.println(mapper.writeValueAsString(order));
            } catch (JsonProcessingException e) {
                System.out.println(e.getMessage());
            }
        }));

    }

    private void printSkippingInfo(Integer lineNumber, String filename) {
        System.out.println("Skipping line number: " + lineNumber + " in filename '" + filename + "', as it does not contain all the required value fields.");

    }

    private Map<String, Integer> getHeaderColumNumberMap(String[] header) {
        Map<String, Integer> headerColumNumberMap = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            headerColumNumberMap.put(header[i], i);
        }
        return headerColumNumberMap;
    }

    private void setOrderId(Order order, Integer lineNumber, String orderId) {
        try {
            order.setOrderId(Long.valueOf(orderId.trim()));
        } catch (NumberFormatException ignored) {
            String errorMessage = "Error on Line number: " + lineNumber + ". Value of 'Order Id' cannot be converted into number.";
            setOrderError(order, errorMessage);
        }
    }

    private void setOrderAmount(Order order, Integer lineNumber, String amount) {
        try {
            order.setAmount(Double.valueOf(amount.trim()));
        } catch (NumberFormatException ignored) {
            String errorMessage = "Error on Line number: " + lineNumber + ". Value of 'amount' cannot be converted into number.";
            setOrderError(order, errorMessage);
        }
    }

    private void setOrderError(Order order, String errorMessage) {
        if ("OK".equals(order.getResult())) {
            order.setResult(errorMessage);
        } else {
            order.setResult(order.getResult() + ".\n" + errorMessage);
        }
    }

    private void setOrderCurrency(Order order, String currency) {
        order.setCurrency(currency.trim());
    }

    private void setOrderComment(Order order, String comment) {
        order.setComment(comment.trim());
    }

    private boolean checkFieldCount(JsonNode jsonNode) {
        return jsonNode.get("orderId") != null && jsonNode.get("amount") != null && jsonNode.get("currency") != null && jsonNode.get("comment") != null;
    }

    private String stringValue(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }
        return jsonNode.asText();
    }

    private BufferedReader getBufferReader(Path filePath) throws FileNotFoundException {
        BufferedReader reader;
        try {
            reader = Files.newBufferedReader(filePath);
        } catch (IOException exception) {
            throw new FileNotFoundException();
        }
        return reader;
    }

    private String[] splitStringBasedOnComma(String value, boolean isHeaderRow) throws HeaderRowMissingException {
        if (value == null) {
            throw new NullPointerException();
        }
        String[] split = value.trim().split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        if (!isHeaderRow) {
            return split;
        }

        for (String field : split) {
            if (ApplicationConstant.headerRow.contains(field)) {
                return split;
            }
        }
        throw new HeaderRowMissingException();
    }
}
