package org.genarator.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class BitcoinNextEdge implements Serializable {
    // 'btcTx/{key}'
    private String _from;
    // 'btcTx/{key}'
    private String _to;
    // txId + "_" + outIndex
    private String _key;

    private String address;
    private Integer outIndex;
    private Long spentBtc;
}
