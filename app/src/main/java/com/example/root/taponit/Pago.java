package com.example.root.taponit;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.net.HttpURLConnection;


public class Pago {

    public Pago(){

    }

    public class FetchMPData extends AsyncTask<String, Void, Void> {

        private final String LOG_TAG = FetchMPData.class.getSimpleName();

        @Override
        protected Void doInBackground(String... params) {
            if(params.length == 0){
                //nothing to do
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String mpJsonStr = null;

            /*obtengo mi access token*/
            MP mp = new MP ("8345175340580712", "4BZspfgSwHIcopc3dbPlr2cmHXZMgeLS");
            try{
                String accessToken = mp.getAccessToken();

                System.out.println("ACESS TOKEN: " + accessToken);
            }catch (Exception e){
                e.printStackTrace();
            }





            return null;
        }
    }
}
