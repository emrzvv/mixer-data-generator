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
    private long minDepositAmount = 0; // todo: set from config
    private long maxDepositAmount = Long.MAX_VALUE; // todo: set from config
    private int minCommission = 1; // todo: set from config
    private int maxCommission = 30; // todo: set from config
    private final ZoneId zoneId = ZoneId.systemDefault();
    private final Random random = new Random();
    private int maxOutputsAmount = 5; // todo: set from config
    private ArrayList<BitcoinTxNode> currentLayerTxs = new ArrayList<>();
    private ArrayList<BitcoinTxNode> prevLayerTxs = new ArrayList<>();
    private long btcAmount = 290000000; // todo: set from config
    private int commission = 10; // todo: set from config
    private int preferredHoursDelay = 48; // todo: set hours amount from config
    private int supportTransactionsAmount = 10; // todo: set transactions amount from config
    private long currentTime = getLocalTime();
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

        long commissionValue = toMixBtcAmount / 100L * commission;

        return values;
    }

    private ArrayList<Pair<BitcoinAddressNode, BitcoinOutputEdge>> generateRandomClusterWithdraws(int i, BitcoinTxNode transaction) {
        ArrayList<Pair<BitcoinAddressNode, BitcoinOutputEdge>> withdrawData = new ArrayList<>();
        int amount = random.nextInt(1, maxOutputsAmount);
        System.out.println("withdraw addresses: " + amount);
        long btcToSpend = btcAmount / 2;
        for (int j = 1; j <= amount; ++j) {
            BitcoinAddressNode withdrawAddress_i = new BitcoinAddressNode("withdraw_btc_address_" + j + "_" + i, j, Utils.generateBtcAddress());

            BitcoinOutputEdge output_i = new BitcoinOutputEdge(transaction.get_key() + "_output_" + j,
                    btcTx + transaction.get_key(),
                    btcAddress + withdrawAddress_i.get_key(),
                    j,
                    btcToSpend / amount,
                    getLocalTime());

            withdrawData.add(new Pair<>(withdrawAddress_i, output_i));
        }
        return withdrawData;
    }

    private void performDelay(int i) {
        if (preferredHoursDelay <= 1) return;
        if (i == supportTransactionsAmount) {
            currentTime += Duration.ofHours(preferredHoursDelay).toSeconds();
        }
        else {
            int hours = random.nextInt(1, preferredHoursDelay);
            preferredHoursDelay -= hours;
            currentTime += Duration.ofHours(hours).toSeconds();
            System.out.println("CURRENT DELAY: " + hours + "h");
        }
    }

    private BitcoinAddressNode generateCluster(int i, BitcoinAddressNode fromAddress) {
        BitcoinAddressNode toAddress = new BitcoinAddressNode("support_btc_address_" + i, 0, Utils.generateBtcAddress());

        performDelay(i);

        BitcoinTxNode transaction = new BitcoinTxNode("tx_" + fromAddress.get_key() + "_TO_" + toAddress.get_key(), currentTime);
        BitcoinOutputEdge input0 = new BitcoinOutputEdge(transaction.get_key() + "_input_0",
                btcAddress + fromAddress.get_key(),
                btcTx + transaction.get_key(),
                0,
                btcAmount,
                currentTime);

        addressNodes.put(fromAddress.get_key(), fromAddress);
        addressNodes.put(toAddress.get_key(), toAddress);
        transactionNodes.put(transaction.get_key(), transaction);
        inOutEdges.put(input0.get_key(), input0);

        generateRandomClusterWithdraws(i, transaction).forEach(pair -> {
            addressNodes.put(pair.left.get_key(), pair.left);
            inOutEdges.put(pair.right.get_key(), pair.right);
        });

        BitcoinOutputEdge output0 = new BitcoinOutputEdge(transaction.get_key() + "_output_0",
                btcTx + transaction.get_key(),
                btcAddress + toAddress.get_key(),
                0,
                btcAmount / 2,
                currentTime);
        inOutEdges.put(output0.get_key(), output0);
        btcAmount /= 2;

        currentLayerTxs.add(transaction);
        return toAddress;
    }

    private void generateBlockNodeAndBlockEdges() {
        blockNode = new BitcoinBlockNode(
                Utils.generateRandomHash(6),
                random.nextInt(1, Integer.MAX_VALUE)
        );

        int i = 0;
        for (var entry : transactionNodes.entrySet()) {
            BitcoinParentBlockEdge bpbe = new BitcoinParentBlockEdge(
                    "block_edge_" + i,
                    btcTx + entry.getValue().get_key(),
                    btcBlock + blockNode.getBlockHeight().toString()
            );

            blockEdges.put(bpbe.get_key(), bpbe);

            i++;
        }
    }

    private void generateStartCluster(ArrayList<BitcoinAddressNode> enterSupportNodes, long btcAmount, int commission) {
        BitcoinAddressNode startNode = new BitcoinAddressNode(null, 0, Utils.generateBtcAddress());
        BitcoinTxNode startTx = new BitcoinTxNode("start_tx_from_" + startNode.get_key(), getLocalTime());
        BitcoinOutputEdge startInput = new BitcoinOutputEdge(
                startTx.get_key() + "_input_0",
                btcAddress + startNode.get_key(),
                btcTx + startTx.get_key(),
                0,
                btcAmount,
                getLocalTime());
        addressNodes.put(startNode.get_key(), startNode);
        transactionNodes.put(startTx.get_key(), startTx);
        inOutEdges.put(startInput.get_key(), startInput);
        long spent = btcAmount - (btcAmount / 100 * commission);
        for (int j = 0; j < enterSupportNodes.size(); ++j) {
            BitcoinOutputEdge outputEdge = new BitcoinOutputEdge(
                    startTx.get_key() + "_output_0",
                    btcTx + startTx.get_key(),
                    btcAddress + enterSupportNodes.get(j).get_key(),
                    j,
                    spent / enterSupportNodes.size(),
                    getLocalTime()
            );
            inOutEdges.put(outputEdge.get_key(), outputEdge);
        }
    }

    public void generateDefaultCentralized() {
        long toMixBtcAmount = btcAmount;
        if ((toMixBtcAmount < minDepositAmount) ||
                (toMixBtcAmount > maxDepositAmount) ||
                (commission < minCommission) ||
                (commission > maxCommission)) {
            throw new IllegalArgumentException();
        }

        ArrayList<BitcoinAddressNode> enterSupportNodes = new ArrayList<>();
        int layersAmount = 1;
        btcAmount = toMixBtcAmount - (toMixBtcAmount / 100 * commission);
        for (int layer = 0; layer < layersAmount; ++layer) {
            ArrayList<BitcoinAddressNode> currentSupportAddresses = new ArrayList<>();
            BitcoinAddressNode fromAddress;
            fromAddress = new BitcoinAddressNode("support_btc_address_0", 0, Utils.generateBtcAddress());
            enterSupportNodes.add(fromAddress);
            currentSupportAddresses.add(fromAddress);
            prevLayerTxs.clear();
            prevLayerTxs.addAll(currentLayerTxs);
            currentLayerTxs.clear();
            for (int i = 1; i <= supportTransactionsAmount; ++i) {
                fromAddress = generateCluster(i, fromAddress);
                currentSupportAddresses.add(fromAddress);
            }
//            if (prevLayerTxs.isEmpty()) continue;
//            for (int i = 1; i < currentSupportAddresses.size(); ++i) {
//                BitcoinOutputEdge connectingInput = new BitcoinOutputEdge(
//                        prevLayerTxs.get(i - 1).get_key() + "_1",
//                        btcAddress + currentSupportAddresses.get(i),
//                        btcTx + prevLayerTxs.get(i - 1).get_key(),
//                        1,
//                        null,
//                        getLocalTime()
//                );
//                inOutEdges.put(connectingInput.get_key(), connectingInput);
//            }
        }
        generateStartCluster(enterSupportNodes, toMixBtcAmount, commission);


        generateBlockNodeAndBlockEdges();
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
