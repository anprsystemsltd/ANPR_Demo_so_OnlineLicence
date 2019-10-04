package com.anprsystemsltd.anpr.android.demo.so.ol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class DownloadAnprLicence implements Runnable {
    private String fileName = "/sdcard/anprlicense.txt";

    private String downloadAddress = "http://anprlicense.eu/android/android_get_licens.php";

    private String imei = "";

    private String errorString = "";

    private Handler handlerRet;

    private Context context;

    private String error = "";

    public DownloadAnprLicence(Context aContext, Handler aHandler) {
        context = aContext;
        handlerRet = aHandler;
    }



    public int CheckLicens() {
        int ret = 0;


        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        imei = telephonyManager.getDeviceId();

        File file = new File(fileName);
        if (file.exists() == true) {
            String cont = "";
            try {
                FileReader reader = new FileReader(file);
                char[] buffer = new char[1000];
                int len = 0;
                int h = 0;
                while (h != -1) {
                    h = reader.read(buffer);
                    cont = cont + new String(buffer);
                    if (h > -1) {
                        len = len + h;
                    }
                }
                reader.close();
                cont = cont.substring(0, len);
            }
            catch (Exception e) {
            }

            int posk = cont.indexOf("(licens)");
            int posv = cont.indexOf("(/licens)");
            if ((posk > -1) & (posv > -1)) {
                String licens = cont.substring(posk + 8, posv);
                posk = licens.indexOf("#");
                posv = licens.indexOf("#", posk + 1);
                String ltime = licens.substring(posk + 1, posv);

                Date cDate = new Date();
                String ttime = new SimpleDateFormat("yyyy-MM-dd").format(cDate);

                if (ttime.compareTo(ltime) < 0) {
                    ret = 1;
                }
            }


        }

        return ret;
    }





    @Override
    public void run() {
        error = "";

        String cmd = "";
        cmd = cmd + downloadAddress;

//		cmd = cmd + "?imei=" + imei;
//		String res = CallNet(cmd);

        Vector<String> params = new Vector<String>();
        params.add("imei=" + imei);
        String res = CallNet1(cmd, params);

        if (res != null) {
            int posk = res.indexOf("(message)");
            int posv = res.indexOf("(/message)");
            if ((posk > -1) & (posv > -1)) {
                int posrk = res.indexOf("(result)");
                int posrv = res.indexOf("(/result)");
                if ((posrk > -1) & (posrv > -1)) {
                    String result = res.substring(posrk + 8, posrv);
                    if (result.equals("OK"))
                    {
                        posk = res.indexOf("(licens)");
                        posv = res.indexOf("(/message)");
                        String licens = res.substring(posk, posv);
                        File file = new File(fileName);
                        if (file.exists()) {
                            file.delete();
                        }
                        try {
                            FileWriter writer = new FileWriter(file);
                            writer.write(licens);
                            writer.close();
                        }
                        catch (IOException e) {
                            error = "File I/O error.";
                        }

                    }
                    else {
                        error = result;
                    }
                }
            }
            else {
                error = "Licens data error.";
            }

        }
        else {
            error = "Communication error.";
        }

        Message msg = handlerRet.obtainMessage();
        msg.what = 1;
        msg.obj = error;
        handlerRet.sendMessage(msg);
    }












    private String CallNet(String aCallString) {
        String ret = "";

        InputStream is = null;
        StringBuilder sb = null;
        String result = null;



        try {
            HttpClient httpclient = new DefaultHttpClient();

            HttpGet httpget = new HttpGet(aCallString);

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        }
        catch(Exception e) {
            errorString = "Communication error!";
            ret = null;
            return null;
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is,"iso-8859-1"),8);
            sb = new StringBuilder();
            sb.append(reader.readLine() + "\n");
            String line="0";
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            result = sb.toString();
        }
        catch(Exception e) {
            errorString = "Protocol error!!";
            ret = null;
            return null;
        }


        return result;
    }



    private String CallNet1(String aPath, Vector<String> aParams) {
        String pth = aPath;
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        if (aParams != null) {
            for (int i = 0; i < aParams.size(); i++) {
                String p = aParams.elementAt(i);
                int pos = p.indexOf("=");
                String p1 = p.substring(0, pos);
                String p2 = p.substring(pos + 1);
                nameValuePairs.add(new BasicNameValuePair(p1, p2));
            }
        }
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost request = new HttpPost(pth);

            HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            int timeoutConnection = 20000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 20000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
            request.setParams(httpParameters);

            if (nameValuePairs != null) {
                request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            }

            HttpResponse response = client.execute(request);
            String ret =  EntityUtils.toString(response.getEntity());

            int deb = 0;
            deb++;

            return ret;
        }
        catch (Exception e) {
            int deb = 0;
            deb++;


            errorString = "Communication error!";
            return null;
        }

    }








}
