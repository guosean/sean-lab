import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;

/**
 * Created by guozhenbin on 2017/6/8.
 */
public class TestMain {

    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        String algorithm = "RSA";

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
        kpg.initialize(16384);
        System.out.println("init");
        long start = System.currentTimeMillis();
        KeyPair kp = kpg.generateKeyPair();
        System.out.println("key gen "+ (System.currentTimeMillis() - start));
        PrivateKey prk = kp.getPrivate();
        PublicKey puk = kp.getPublic();
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE,puk);
        String content = "test测试ddddd中国人的点点滴滴多多多多多";
        start = System.currentTimeMillis();
        byte[] bytes = cipher.doFinal(content.getBytes());
        System.out.println((System.currentTimeMillis()-start)+ " -- "+content.getBytes().length);

        System.out.println(bytes.length);
        start = System.currentTimeMillis();
        cipher.init(Cipher.DECRYPT_MODE,prk);
        byte[] deBytes = cipher.doFinal(bytes);

        System.out.println((System.currentTimeMillis() - start) + " "+new String(deBytes));
    }

}
