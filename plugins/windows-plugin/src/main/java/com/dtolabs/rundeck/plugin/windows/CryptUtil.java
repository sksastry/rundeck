/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * CryptUtil.java
 */

package com.dtolabs.rundeck.plugin.windows;

import java.io.BufferedReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class CryptUtil {
    
    private static byte[] doCipher(int mode, byte[] data, byte[] key)
      throws Exception{
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(mode, new SecretKeySpec(key, "AES"));       
        return cipher.doFinal(data);
    }
    
    public static String decryptString(String userName, String string, String keyFilePath)
      throws Exception {
    	String decryptedPassword
	      = new String(doCipher(Cipher.DECRYPT_MODE,
					   base64ToByte(string),
					   Base64.decodeFromFile(keyFilePath)));
      	String magicToken = "::" + userName;
      	if (decryptedPassword.endsWith(magicToken)) {
      		return decryptedPassword.substring(0, decryptedPassword.indexOf(magicToken));
      	}
		return null;
    }
    
    private static String encryptString(String userName, String string, byte[] key)
      throws Exception {
    	String magicToken = "::" + userName;
    	return byteToBase64(doCipher(Cipher.ENCRYPT_MODE,
								     (string + magicToken).getBytes(),
								     key));
    }
    
    private static byte[] getRandomKey() throws Exception {
        // Salt generation 64 bits long
        byte[] bKey = new byte[16];

        // Uses a secure Random not a simple Random
		SecureRandom.getInstance("SHA1PRNG").nextBytes(bKey);

		return bKey;
    }

    private static byte[] base64ToByte(String data) throws Exception {
		return Base64.decode(data);
    }

    private static String byteToBase64(byte[] data){
        return Base64.encodeBytes(data);
    }
    
    public static void main(String [] args) throws Throwable {
    	byte[] randomKey;
    	String keyFilePath = null;
    	File keyFile;
    	if (args.length != 3) {
    		System.err.println("[ERROR]: 3 arguments required: {key-file-path} {username} {password}");
    		System.exit(1);
    	} else if (!(keyFile = new File(keyFilePath = args[0])).isAbsolute()) {
    		System.err.println("[ERROR]: key-file-path must be an absolute file path.");
    		System.exit(1);
    	} else if (!keyFile.isFile()) {
        	randomKey = getRandomKey();
        	Base64.encodeToFile(randomKey, keyFilePath);
        	System.out.println("\n[INFO]: file: \"" + keyFilePath + "\" created.");
    	} else {  		
        	System.out.println("\n[INFO]: using existing key-file: \"" + keyFilePath + "\".");
    	}
    	
    	String userName = args[1];
    	String password = args[2];
    	randomKey = Base64.decodeFromFile(keyFilePath);
    	String encodedPassword = encryptString(userName, password, randomKey); 
    	System.out.println(
    	  String.format("\n[INFO]: place the following in your project \"resource.xml\" file:\n"
    	  		        + "<node ... key-file-path=\"%s\"\n"
    	  		        + "          username=\"%s\"\n"
    	  		        + "          password=\"%s\" .../>",
    			        keyFilePath, userName, encodedPassword));
    	//System.out.println(decryptString(userName, encodedPassword, keyFilePath));
    }

}
