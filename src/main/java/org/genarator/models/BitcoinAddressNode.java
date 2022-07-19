package org.genarator.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.io.Serializable;

@Getter @Setter @AllArgsConstructor @ToString
public class BitcoinAddressNode implements Serializable {
    // btc address
    private String _key;

    public BitcoinAddressNode(String txId, int outIndex, String address) {
        if (address != null && address.length() != 0) {
            _key = address;
        } else {
            // coinbase is still true because coinbase txs are only without inputs
            _key = txId + "_" + outIndex; // "";
        }
    }
}
