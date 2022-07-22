package org.genarator.arango;

import com.arangodb.*;
import com.arangodb.entity.LoadBalancingStrategy;
import com.arangodb.mapping.ArangoJack;
import com.arangodb.model.DocumentImportOptions;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.logging.Logger;
@RequiredArgsConstructor
public class ArangoApi {
    @NonNull
    Logger log;
    @NonNull
    String arangoHosts;
    @NonNull
    String user;
    @NonNull
    String password;

    ArangoDB arangoDB;
    static final int DEFAULT_ARANGO_PORT = 8529;


    ArangoDatabase arangoDatabase;

    DocumentImportOptions optionswithIgnoreDublicate = new DocumentImportOptions();

    public void connect() {
        ArangoDB.Builder builder = new ArangoDB.Builder()
                .loadBalancingStrategy(LoadBalancingStrategy.ROUND_ROBIN)
                .acquireHostList(true)
                .serializer(new ArangoJack());
        // todo: add connection with arango config file (to set multiple hosts easier)


        if (user != null) {
            builder.user(user)
                    .password(password);
        }

        for (String host: arangoHosts.split(",")) {
            String[] hostAndPort = host.split(":");
            if (hostAndPort.length < 2) {
                builder.host(hostAndPort[0], DEFAULT_ARANGO_PORT);
            } else {
                builder.host(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
            }
        }
        arangoDB = builder.build();
    }
    public void createDatabase(String dbName) {
        arangoDB.createDatabase(dbName);
    }
    public void selectDatabase(String dbName) {
        arangoDatabase = arangoDB.db(DbName.of(dbName));
    }

    public Object getDocument(String dbName, String collectioName, String docKey, Class documentClass) {
        log.fine(String.format("get do with _key=%s from collection=%s from db=%s", dbName, collectioName, docKey));
        return arangoDB.db(DbName.of(dbName)).collection(collectioName).getDocument(docKey, documentClass);
    }


    public boolean hasDocument(String dbName, String collectioName, String docKey) {
        return arangoDB.db(DbName.of(dbName)).collection(collectioName).documentExists(docKey);
    }

    /**
     * upserts with empty update docs to collection with single aql query (for bulk load comparing to insertDocuments method)
     * @param collectionName
     * @param docs
     * @return true if insert was successful, false - if it failed (fail can also happen if docs are already in collection - see exception in logs)
     */
    public boolean importDocsIfNotExistToCollection(String collectionName, List<Object> docs) {
        docs.forEach(System.out::println);
        if (docs.size() == 0) {
            log.warning(String.format("try insert empty list to collection %s, will not insert anything", collectionName));
            return true;
        }
        try {
            arangoDatabase.collection(collectionName).importDocuments(docs, optionswithIgnoreDublicate);
            return true;
        } catch (ArangoDBException e) {
            log.severe(String.format("error importing docs %s to %s", docs.toString(), collectionName));
        }

        return false;
    }

}

