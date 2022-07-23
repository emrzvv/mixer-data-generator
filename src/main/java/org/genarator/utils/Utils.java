package org.genarator.utils;

import org.bitcoinj.core.Base58;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.util.Random;

public class Utils {
    public static class ArangoPrefix {
        public static final String btcAddress = "btcAddress/";
        public static final String btcTx = "btcTx/";
        public static final String btcBlock = "btcBlock/";
    }



    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    static private String adjustTo64(String s) {
        return switch (s.length()) {
            case 62 -> "00" + s;
            case 63 -> "0" + s;
            case 64 -> s;
            default -> throw new IllegalArgumentException("not a valid key: " + s);
        };
    }
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String generateRandomHash(int len) {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijk"
                +"lmnopqrstuvwxyz";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    public static String generateBtcAddress() {
        java.security.Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("EC", "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        try {
            keyGen.initialize(ecSpec);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        KeyPair kp = keyGen.generateKeyPair();
        PublicKey pub = kp.getPublic();
        PrivateKey pvt = kp.getPrivate();
        ECPrivateKey epvt = (ECPrivateKey)pvt;
        String sepvt = adjustTo64(epvt.getS().toString(16)).toUpperCase();
        // System.out.println("s[" + sepvt.length() + "]: " + sepvt);
        ECPublicKey epub = (ECPublicKey)pub;
        ECPoint pt = epub.getW();
        String sx = adjustTo64(pt.getAffineX().toString(16)).toUpperCase();
        String sy = adjustTo64(pt.getAffineY().toString(16)).toUpperCase();
        String bcPub = "04" + sx + sy;
        // System.out.println("bcPub: " + bcPub);
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] s1 = new byte[0];
        try {
            s1 = sha.digest(bcPub.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        //System.out.println("  sha: " + bytesToHex(s1).toUpperCase());
        MessageDigest rmd = null;
        try {
            rmd = MessageDigest.getInstance("RipeMD160", "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
        byte[] r1 = rmd.digest(s1);
        byte[] r2 = new byte[r1.length + 1];
        r2[0] = 0;
        for (int i = 0 ; i < r1.length ; i++) r2[i + 1] = r1[i];
        //System.out.println("  rmd: " + bytesToHex(r2).toUpperCase());
        byte[] s2 = sha.digest(r2);
        //System.out.println("  sha: " + bytesToHex(s2).toUpperCase());
        byte[] s3 = sha.digest(s2);
        //System.out.println("  sha: " + bytesToHex(s3).toUpperCase());
        byte[] a1 = new byte[25];
        for (int i = 0 ; i < r2.length ; i++) a1[i] = r2[i];
        for (int i = 0 ; i < 5 ; i++) a1[20 + i] = s3[i];
        return Base58.encode(a1);
    }
}
