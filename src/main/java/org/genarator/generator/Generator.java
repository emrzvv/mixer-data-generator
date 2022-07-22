package org.genarator.generator;

import org.genarator.models.*;
import org.genarator.utils.Utils;
import org.genarator.utils.Pair;
import static org.genarator.utils.Utils.ArangoPrefix.*;

import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class Generator {
    // _key -> graph dao
    private Map<String, BitcoinOutputEdge> inOutEdges;
    private Map<String, BitcoinAddressNode> addressNodes;
    private Map<String, BitcoinTxNode> transactionNodes;
    private Map<String, BitcoinParentBlockEdge> blockEdges;
    private BitcoinBlockNode blockNode;
    private long minDepositAmount = 0;
    private long maxDepositAmount = Long.MAX_VALUE;
    private int minCommission = 1;
    private int maxCommission = 30;
    private final ZoneId zoneId = ZoneId.systemDefault();
    private final Random random = new Random();
    private int maxOutputsAmount = 5; // todo: needs to be set by user. or make generation process with config only
    private ArrayList<BitcoinTxNode> currentLayerTxs = new ArrayList<>();
    private ArrayList<BitcoinTxNode> prevLayerTxs = new ArrayList<>();

    public Generator() {
        this.blockNode = null;
        this.inOutEdges = new HashMap<>();
        this.addressNodes = new HashMap<>();
        this.transactionNodes = new HashMap<>();
        this.blockEdges = new HashMap<>();
    }

    public Generator(Map<String, BitcoinOutputEdge> inOutEdges,
                     Map<String, BitcoinAddressNode> addressNodes,
                     Map<String, BitcoinTxNode> transactionNodes,
                     Map<String, BitcoinParentBlockEdge> blockEdges) {
        this.inOutEdges = inOutEdges;
        this.addressNodes = addressNodes;
        this.transactionNodes = transactionNodes;
        this.blockEdges = blockEdges;
    }

    public Map<String, BitcoinOutputEdge> getInOutEdges() {
        return inOutEdges;
    }

    public Map<String, BitcoinAddressNode> getAddressNodes() {
        return addressNodes;
    }

    public Map<String, BitcoinTxNode> getTransactionNodes() {
        return transactionNodes;
    }

    public Map<String, BitcoinParentBlockEdge> getBlockEdges() {
        return blockEdges;
    }

    public BitcoinBlockNode getBlockNode() {
        return blockNode;
    }

    private long getLocalTime() {
        return LocalDateTime.now().atZone(zoneId).toEpochSecond();
    }


    private String generateBtcAddress() {
        return "btc_address_" + (random.nextLong() & 0xffffffffL);
    }


    public void generateSimpleExample() {
        BitcoinTxNode tx1 = new BitcoinTxNode("tx_1_key", getLocalTime());
        BitcoinTxNode tx2 = new BitcoinTxNode("tx_2_key", getLocalTime());
        BitcoinTxNode tx3 = new BitcoinTxNode("tx_3_key", getLocalTime());

        // txId???
        BitcoinAddressNode addressNode1 = new BitcoinAddressNode(tx1.get_key(), 1, generateBtcAddress());
        BitcoinAddressNode addressNode2 = new BitcoinAddressNode(tx2.get_key(), 2, generateBtcAddress());
        BitcoinAddressNode addressNode3 = new BitcoinAddressNode(tx3.get_key(), 3, generateBtcAddress());

        BitcoinOutputEdge input1 = new BitcoinOutputEdge("input_key_1_" + tx1.get_key(), "btcAddress/" + addressNode1.get_key(), "btcTx/" + tx1.get_key(), 1, 12345L, getLocalTime());
        BitcoinOutputEdge input2 = new BitcoinOutputEdge("input_key_2_" + tx2.get_key(), "btcAddress/" + addressNode2.get_key(), "btcTx/" + tx2.get_key(), 1, 54321L, getLocalTime());

        BitcoinOutputEdge output1 = new BitcoinOutputEdge("output_key_1_" + tx1.get_key() + "_0", "btcTx/" + tx1.get_key(), "btcAddress/" + addressNode3.get_key(), 1, 20000L, getLocalTime());
        BitcoinOutputEdge output2 = new BitcoinOutputEdge("output_key_2_" + tx2.get_key() + "_0", "btcTx/" + tx1.get_key(), "btcAddress/" + addressNode3.get_key(), 1, 20000L, getLocalTime());

        System.out.println(tx1.toString());
        System.out.println(addressNode1.toString());
        System.out.println(addressNode2.toString());
        System.out.println(addressNode3.toString());
        System.out.println(input1);
        System.out.println(input2);
        System.out.println(output1);
        System.out.println(output2);
    }

    public void generateCoinJoin() {}

    private Deque<Long> expandBtcToPowersOfTwo(long toMixBtcAmount, int commission) {
        Deque<Long> values = new ArrayDeque<>();
        long value = toMixBtcAmount;
        for (long i = 0; value > 0; ++i) {
            if (value % 2 == 1) {
                if (i == 0) values.addLast(1L);
                else values.addLast(2L << (i - 1));
            }
            value /= 2;
        }

        long comissionValue = toMixBtcAmount / 100L * commission;

        return values;
    }

    private ArrayList<Pair<BitcoinAddressNode, BitcoinOutputEdge>> generateRandomClusterWithdraws(int i, BitcoinTxNode transaction)
            throws InvalidAlgorithmParameterException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchProviderException {
        ArrayList<Pair<BitcoinAddressNode, BitcoinOutputEdge>> withdrawData = new ArrayList<>();
        int amount = random.nextInt(1, maxOutputsAmount);
        System.out.println("withdraw addresses: " + amount);
        for (int j = 1; j <= amount; ++j) {
            BitcoinAddressNode withdrawAddress_i = new BitcoinAddressNode("withdraw_btc_address_" + j + "_" + i, j, Utils.generateBtcAddress());
            BitcoinOutputEdge output_i = new BitcoinOutputEdge(transaction.get_key() + "_output_" + j,
                    btcTx + transaction.get_key(),
                    btcAddress + withdrawAddress_i.get_key(),
                    0,
                    null,
                    getLocalTime());
            withdrawData.add(new Pair<>(withdrawAddress_i, output_i));
        }
        return withdrawData;
    }

    private BitcoinAddressNode generateCluster(int i, BitcoinAddressNode fromAddress) throws InvalidAlgorithmParameterException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchProviderException {
        BitcoinAddressNode toAddress = new BitcoinAddressNode("support_btc_address_" + i, 0, Utils.generateBtcAddress());

        BitcoinTxNode transaction = new BitcoinTxNode("tx_" + fromAddress.get_key() + "_TO_" + toAddress.get_key(), getLocalTime());
        BitcoinOutputEdge input0 = new BitcoinOutputEdge(transaction.get_key() + "_input_0",
                btcAddress + fromAddress.get_key(),
                btcTx + transaction.get_key(),
                0,
                null,
                getLocalTime());
        BitcoinOutputEdge output0 = new BitcoinOutputEdge(transaction.get_key() + "_output_0",
                btcTx + transaction.get_key(),
                btcAddress + toAddress.get_key(),
                0,
                null,
                getLocalTime());


        addressNodes.put(fromAddress.get_key(), fromAddress);
        addressNodes.put(toAddress.get_key(), toAddress);
        transactionNodes.put(transaction.get_key(), transaction);
        inOutEdges.put(input0.get_key(), input0);
        inOutEdges.put(output0.get_key(), output0);

        generateRandomClusterWithdraws(i, transaction).forEach(pair -> {
            addressNodes.put(pair.left.get_key(), pair.left);
            inOutEdges.put(pair.right.get_key(), pair.right);
        });

        currentLayerTxs.add(transaction);
        return toAddress;
    }

    private void setBtcValues() {

    }

    public void generateDefaultCentralized(long toMixBtcAmount, int commission, Duration preferredDelay) {
        if ((minDepositAmount <= toMixBtcAmount && toMixBtcAmount <= maxDepositAmount)
                && (minCommission <= commission && commission <= maxCommission)) {
            // Deque<Long> expandedToMixBtc = expandBtcToPowersOfTwo(toMixBtcAmount, commission);
            // expandedToMixBtc.forEach(System.out::println);
            ArrayList<BitcoinAddressNode> enterSupportNodes = new ArrayList<>();
            int supportNodesAmount = 10; // per layer
            int layersAmount = 1;
            for (int layer = 0; layer < layersAmount; ++layer) {
                ArrayList<BitcoinAddressNode> currentSupportAddresses = new ArrayList<>();
                BitcoinAddressNode fromAddress;
                try {
                    fromAddress = new BitcoinAddressNode("support_btc_address_0", 0, Utils.generateBtcAddress());
                    enterSupportNodes.add(fromAddress);
                    currentSupportAddresses.add(fromAddress);
                } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | UnsupportedEncodingException |
                         NoSuchProviderException e) {
                    throw new RuntimeException(e);
                }
                prevLayerTxs.clear();
                prevLayerTxs.addAll(currentLayerTxs);
                currentLayerTxs.clear();
                for (int i = 1; i <= supportNodesAmount; ++i) {
                    try {
                        fromAddress = generateCluster(i, fromAddress);
                        currentSupportAddresses.add(fromAddress);
                    } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException |
                             UnsupportedEncodingException | NoSuchProviderException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (prevLayerTxs.isEmpty()) continue;
                for (int i = 1; i < currentSupportAddresses.size(); ++i) {
                    BitcoinOutputEdge connectingInput = new BitcoinOutputEdge(
                            prevLayerTxs.get(i - 1).get_key() + "_1",
                            btcAddress + currentSupportAddresses.get(i),
                            btcTx + prevLayerTxs.get(i - 1).get_key(),
                            1,
                            null,
                            getLocalTime()
                    );
                    inOutEdges.put(connectingInput.get_key(), connectingInput);
                }

                supportNodesAmount /= 2;
            }

            try {
                BitcoinAddressNode startNode = new BitcoinAddressNode(null, 0, Utils.generateBtcAddress());
                BitcoinTxNode startTx = new BitcoinTxNode("start_tx_from_" + startNode.get_key(), getLocalTime());
                BitcoinOutputEdge startInput = new BitcoinOutputEdge(
                        startTx.get_key() + "_0",
                        btcAddress + startNode.get_key(),
                        btcTx + startTx.get_key(),
                        0,
                        null,
                        getLocalTime());
                addressNodes.put(startNode.get_key(), startNode);
                transactionNodes.put(startTx.get_key(), startTx);
                inOutEdges.put(startInput.get_key(), startInput);
                for (int j = 0; j < enterSupportNodes.size(); ++j) {
                    BitcoinOutputEdge outputEdge = new BitcoinOutputEdge(
                            startTx.get_key() + "_0",
                            btcTx + startTx.get_key(),
                            btcAddress + enterSupportNodes.get(j).get_key(),
                            j,
                            null,
                            getLocalTime()
                    );
                    inOutEdges.put(outputEdge.get_key(), outputEdge);
                }
            } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | UnsupportedEncodingException |
                     NoSuchProviderException e) {
                throw new RuntimeException(e);
            }
        } else throw new IllegalArgumentException();

        blockNode = new BitcoinBlockNode(
                Utils.generateRandomHash(6),
                random.nextInt(1, Integer.MAX_VALUE)
        );

        int i = 0;
        for (var entry : transactionNodes.entrySet()) {
            BitcoinParentBlockEdge bpbe = new BitcoinParentBlockEdge(
                    "block_edge_" + i,
                    "btcTx/" + entry.getValue().get_key(),
                    "btcBlock/" + blockNode.getBlockHeight().toString()
            );

            blockEdges.put(bpbe.get_key(), bpbe);

            i++;
        }
    }

    private <K, V> void writeDownHashMapData(Map<K, V> data, String folder) {
        for (var entry: data.entrySet()) {
            try (FileOutputStream fos = new FileOutputStream( folder + entry.getKey() + ".dat");
                 ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(entry.getValue());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void writeAll() {
        ArrayList<String> folderNames = new ArrayList<>(
                Arrays.asList(
                        "transactions",
                        "addresses",
                        "inoutedges",
                        "blockedges",
                        "blocknodes"));

        String path = "./src/main/resources/";
        for (String folderName: folderNames) {
            File folder = new File(path + folderName);
            if (!folder.exists()) {
                folder.mkdirs();
            }
        }

        writeDownHashMapData(transactionNodes, "./src/main/resources/transactions/");
        writeDownHashMapData(addressNodes, "./src/main/resources/addresses/");
        writeDownHashMapData(inOutEdges, "./src/main/resources/inoutedges/");
        writeDownHashMapData(blockEdges, "./src/main/resources/blockedges/");

        try (FileOutputStream fos = new FileOutputStream( "./src/main/resources/blocknodes/" + blockNode.get_key() +  ".dat");
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(blockNode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
