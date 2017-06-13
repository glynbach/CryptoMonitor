package com.kieral.cryptomon.service.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtil {

    public static void main(String[] args) {
    	if (args != null && args.length != 3) {
    		System.out.println("Expecting argumens {secret, contents, file}");
    		System.exit(-1);
    	}
        try {
            SecretKey myDesKey = new SecretKeySpec(args[0].getBytes(), "AES");
            Cipher desCipher;
            desCipher = Cipher.getInstance("AES");
            InputStream text = new ByteArrayInputStream(args[1].getBytes("UTF-8"));
            StringBuffer sb = new StringBuffer();
            sb.append(text);
            desCipher.init(Cipher.ENCRYPT_MODE, myDesKey);
            CipherOutputStream os = new CipherOutputStream(new FileOutputStream(args[2]), desCipher);
            byte[] buffer = new byte[8192];
            int count;
            while ((count = text.read(buffer)) > 0)
            {
                os.write(buffer, 0, count);
            }
            os.close();
        } catch(Exception e) {
            e.printStackTrace();;
        }
    }
    
    public static String decryptValue(String key, File file) throws Exception {
        SecretKey myDesKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher desCipher;
        desCipher = Cipher.getInstance("AES");
        desCipher.init(Cipher.DECRYPT_MODE, myDesKey);
        CipherInputStream is = new CipherInputStream(new FileInputStream(file), desCipher);
        try {
	        ByteArrayOutputStream os = new ByteArrayOutputStream(); 
	        byte[] buffer = new byte[8192];
	        int count;
	        while ((count = is.read(buffer)) > 0)
	        {
	            os.write(buffer, 0, count);
	        }
	        return new String(os.toByteArray(), "UTF-8");
        } finally {
        	try {
        		is.close();
        	} catch (Exception e) {}
        }
    }
    
}
