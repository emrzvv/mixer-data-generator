package org.genarator;

import org.genarator.arango.ArangoApi;
import org.genarator.arango.ArangoDoc;
import org.genarator.config.ConfigProperties;
import org.genarator.generator.Generator;
import org.genarator.models.BitcoinAddressNode;
import org.genarator.models.BitcoinOutputEdge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Main {
    public static void importGraphNodesAndEdges(Logger log, ConfigProperties config, Generator generator, boolean clearDatabaseBeforeImport) {
        // connect arango
        ArangoApi aapi = new ArangoApi(log, config.getArangoHosts(), config.getArangoUser(), config.getArangoPassword());
        aapi.connect();
        aapi.selectDatabase(config.getArangoDbName());

        aapi.importDocsIfNotExistToCollection("btcAddress", new ArrayList<>(generator.getAddressNodes().values()), clearDatabaseBeforeImport);
        aapi.importDocsIfNotExistToCollection("btcTx", new ArrayList<>(generator.getTransactionNodes().values()), clearDatabaseBeforeImport);
        aapi.importDocsIfNotExistToCollection("btcBlock", List.of(generator.getBlockNode()), clearDatabaseBeforeImport);
        aapi.importDocsIfNotExistToCollection("btcIn", generator.getInOutEdges().values().stream()
                .filter(edge -> edge.get_from().startsWith("btcAddress")).collect(Collectors.toList()), clearDatabaseBeforeImport);
        aapi.importDocsIfNotExistToCollection("btcOut", generator.getInOutEdges().values().stream()
                .filter(edge -> edge.get_from().startsWith("btcTx")).collect(Collectors.toList()), clearDatabaseBeforeImport);
        aapi.importDocsIfNotExistToCollection("btcEdges", new ArrayList<>(generator.getBlockEdges().values()), clearDatabaseBeforeImport);
        log.info("imported data to database");
    }



    public static void main(String[] args) {
        ConfigProperties configProperties = new ConfigProperties();
        Generator generator = new Generator(configProperties);
        generator.generateDefaultCentralized();
        //generator.writeAll();
        Logger log = Logger.getLogger(Main.class.getName());

        importGraphNodesAndEdges(log, configProperties, generator, false);
        System.out.println(generator.getInOutEdges().values().stream().map(BitcoinOutputEdge::getSpentBtc).reduce(0L, Long::sum));
    }
}