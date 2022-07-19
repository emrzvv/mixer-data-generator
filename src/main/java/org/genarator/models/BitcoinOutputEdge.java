package org.genarator.models;

import lombok.*;

import java.io.Serializable;

@Getter @Setter @AllArgsConstructor @ToString @NoArgsConstructor
public class BitcoinOutputEdge implements Serializable {
    // txId + '_' + outIndex
    private String _key;
    // output: txId arango id 'btcTx/{_key}'
    // input: address arango id 'btcAddress/{_key}'
    private String _from;

    // output: address arango id 'btcAddress/{_key}'
    // input: txId arango id 'btcTx/{_key}'
    private String _to;

    private Integer outIndex;
    private Long spentBtc;
    private Long time;
}
