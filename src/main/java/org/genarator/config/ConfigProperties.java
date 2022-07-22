package org.genarator.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class ConfigProperties {
    String arangoHosts;
    String arangoUser;
    String arangoPassword;
    String arangoDbName;
}
