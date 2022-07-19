package org.genarator.models;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Getter @Setter @AllArgsConstructor @ToString @NoArgsConstructor
public class BitcoinBlockNode implements Serializable {
    private Integer blockHeight;
    // _key = blockHeight.toString()
    private String _key;
    private String blockHash;

    public BitcoinBlockNode(String blockHash, Integer blockHeight) {
        this.blockHash = blockHash;
        this.blockHeight = blockHeight;
        this._key = this.blockHeight.toString();
    }
}
