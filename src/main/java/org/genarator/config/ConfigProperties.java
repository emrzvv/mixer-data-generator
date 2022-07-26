package org.genarator.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Random;

@Data @AllArgsConstructor
public class ConfigProperties {
    private Config config;
    String arangoHosts;
    String arangoUser;
    String arangoPassword;
    String arangoDbName;
    Long minDepositAmount;
    Long maxDepositAmount;
    Integer minCommission;
    Integer maxCommission;
    Integer maxOutputsAmount;
    Long btcAmount;
    Integer commission;
    Integer preferredHoursDelay;
    Integer supportTransactionsAmount;

    Random random = new Random();
    public ConfigProperties() {

        config = ConfigFactory.load();
        arangoHosts = config.getString("database.arangoHosts");
        arangoUser = config.getString("database.arangoUser");
        arangoPassword = config.getString("database.arangoPassword");
        arangoDbName = config.getString("database.arangoDbName");
        minDepositAmount = config.getLong("generator.minDepositAmount");
        maxDepositAmount = config.getLong("generator.maxDepositAmount");
        minCommission = config.getInt("generator.minCommission");
        maxCommission = config.getInt("generator.maxCommission");
        maxOutputsAmount = config.getInt("generator.maxOutputsAmount");
        btcAmount = config.getLong("generator.btcAmount");
        // able to set static commission
        commission = random.nextInt(minCommission, maxCommission);
        // commission = config.getInt("generator.commission");
        preferredHoursDelay = config.getInt("generator.preferredHoursDelay");
        supportTransactionsAmount = config.getInt("generator.supportTransactionsAmount");
    }
}
