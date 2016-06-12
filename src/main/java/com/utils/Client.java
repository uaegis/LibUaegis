package com.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Created by Lehyu on 2016/5/16.
 */
public class Client {
    private static Socket server;
    private static OutputStream output;
    private static InputStreamReader input;

    private static void connectToServer() throws IOException {
        server = new Socket(Configuration.IP_ADDRESS, Configuration.PORT);
        output = server.getOutputStream();
        input = new InputStreamReader(server.getInputStream(), Configuration.ENCODE);
    }

    private static void close() throws IOException {
        server.close();
        input.close();
        output.close();
    }

    public static String doVerity(byte[] msg, byte[] signature, byte[] certByte){
        BufferedReader reader = null;
        try {
            connectToServer();
            reader =  new BufferedReader(new InputStreamReader(server.getInputStream(), Configuration.ENCODE));
            StringBuilder sb = new StringBuilder();
            String line;
            while (!(line=reader.readLine().trim()).equals(Configuration.SEND_KEY_DONE)){
                sb.append(line);
            }
            RSACoder coder = new RSACoder(sb.toString());
            sb.delete(0, sb.length());

            X509Certificate x509cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(certByte));
            PublicKey publicKey = x509cert.getPublicKey();



            String encodedMsg = coder.encrypt(msg);
            //String encodedMsg = coder.encrypt("this's a test!".getBytes());
            output.write((encodedMsg + "\n").getBytes());
            output.write((Configuration.SEND_MSG_DONE + "\n").getBytes());

            String encodedSign = coder.encrypt(signature);
            output.write((encodedSign + "\n").getBytes());
            output.write((Configuration.SEND_SIGN_DONE + "\n").getBytes());

            String encodedCert = coder.encrypt(certByte);
            output.write((encodedCert + "\n").getBytes());
            output.write((Configuration.SEND_CERT_DONE + "\n").getBytes());

            String result = new String(RSACoder.parseHexStr2Byte(reader.readLine()));
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != reader){
                    reader.close();
                }
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
