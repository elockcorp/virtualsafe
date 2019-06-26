/* ###_VIRTUALSAFE_CHANGE_TRACKING_START_###
U2FsdGVkX19SehIxYWkKOtC9yoX5X4M9irr/M6Im1zWegkBGoRW4hotM445487QE
wkEx3jCh+WovP7lQAz3Dzoyrzz2Lik2w52u2M+K5udAXcUjWFOlOniTkJNJCxvY5
yOUd9SVVWM4nMg8pNEFYfw==
###_VIRTUALSAFE_CHANGE_TRACKING_END_### */
/*
 * Referenced from:
 * http://stackoverflow.com/questions/24338108/java-encrypt-string-with-existing-public-key-file
 */

package org.cryptomator.ui.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PKI {

    private static final Logger LOG = LoggerFactory.getLogger(PKI.class);

    public PKI() {
    }

    public byte[] readFileBytes(String filename) throws IOException
    {
        Path path = Paths.get(filename);
        return Files.readAllBytes(path);        
    }

    public PublicKey readPublicKey(String filename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
    {
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(readFileBytes(filename));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(publicSpec);       
    }

    public PrivateKey readPrivateKey(String filename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
    {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(readFileBytes(filename));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);     
    }

    public byte[] encrypt(PublicKey key, byte[] plaintext) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");   
        cipher.init(Cipher.ENCRYPT_MODE, key);  
        return cipher.doFinal(plaintext);
    }

    public byte[] decrypt(PrivateKey key, byte[] ciphertext) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");   
        cipher.init(Cipher.DECRYPT_MODE, key);  
        return cipher.doFinal(ciphertext);
    }

    public void encryptWithEmbeddedPublicKey(String pubkeyfile, SecureCharSequence passphrase, Path outfile) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        try
        {
            PublicKey publicKey = readPublicKey(pubkeyfile);

            byte[] bytes = new byte[passphrase.length()];
            for(int i = 0; i < passphrase.length(); i++)
                bytes[i] = (byte) passphrase.charAt(i);

            byte[] secret = encrypt(publicKey, bytes);
            Files.write(outfile, secret, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (Exception e)
        {
            LOG.error("Failed to encrypt and save passphrase with public key.", e);
        }
    }

    public void decryptWithPrivateKey(String privatekeyfile, String infile, String outfile) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        try
        {
            PrivateKey privateKey = readPrivateKey(privatekeyfile);
            Path outfilepath = Paths.get(outfile);

            byte[] recovered_message = decrypt(privateKey, readFileBytes(infile));
            Files.write(outfilepath, recovered_message, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (Exception e)
        {
            LOG.error("Failed to decrypt encrypted passphrase with private key.", e);
        }
    }
}
