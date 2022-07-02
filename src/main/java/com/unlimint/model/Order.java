package com.unlimint.model;

import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Order {
    @EqualsAndHashCode.Include
    Long id;
    @EqualsAndHashCode.Include
    Long orderId;
    Double amount;
    String currency;
    String comment;
    String filename;
    Integer line;
    String result;
}
