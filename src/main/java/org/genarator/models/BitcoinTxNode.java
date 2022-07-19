package org.genarator.models;

import lombok.Data;
import lombok.NonNull;
import lombok.ToString;

import java.io.Serializable;

@Data @ToString
public class BitcoinTxNode implements Serializable {
    @NonNull
    private String _key;
    @NonNull
    private long time;
}
