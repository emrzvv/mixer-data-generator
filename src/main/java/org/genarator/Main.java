package org.genarator;

import org.genarator.arango.ArangoApi;
import org.genarator.arango.ArangoDoc;
import org.genarator.config.ConfigProperties;
import org.genarator.generator.Generator;
import org.genarator.models.BitcoinAddressNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Main {
    public static void importGraphNodesAndEdges(Logger log, ConfigProperties config, Generator generator) {
        // connect arango
        ArangoApi aapi = new ArangoApi(log, config.getArangoHosts(), config.getArangoUser(), config.getArangoPassword());
        aapi.connect();
        aapi.selectDatabase(config.getArangoDbName());
        System.out.println("CONNECTED");
        // todo: use nodes and edges to write to arango, for example:

        aapi.importDocsIfNotExistToCollection("btcAddress", new ArrayList<>(generator.getAddressNodes().values()));
        aapi.importDocsIfNotExistToCollection("btcTx", new ArrayList<>(generator.getTransactionNodes().values()));
        aapi.importDocsIfNotExistToCollection("btcBlock", List.of(generator.getBlockNode()));
        aapi.importDocsIfNotExistToCollection("btcIn", generator.getInOutEdges().values().stream()
                .filter(edge -> edge.get_from().startsWith("btcAddress")).collect(Collectors.toList()));
        aapi.importDocsIfNotExistToCollection("btcOut", generator.getInOutEdges().values().stream()
                .filter(edge -> edge.get_from().startsWith("btcTx")).collect(Collectors.toList()));
        aapi.importDocsIfNotExistToCollection("btcEdges", new ArrayList<>(generator.getBlockEdges().values()));
        System.out.println("IMPORTED");
    }



    public static void main(String[] args) {
        Generator generator = new Generator();
        generator.generateDefaultCentralized(29L, 5, null);
        //generator.writeAll();

        String prodHosts = "127.0.0.1:8529";
        String prodUser = "root";
        String prodPassword = "root";
        String prodDbName = "testDb";
        ConfigProperties configProperties = new ConfigProperties(
                prodHosts,
                prodUser,
                prodPassword,
                prodDbName
        );
        Logger log = Logger.getLogger(Main.class.getName());

        importGraphNodesAndEdges(log, configProperties, generator);
    }
}