package net.sourceforge.zbar.android.util;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.widget.Toast;

public class MyHttpClient {
	private static DefaultHttpClient httpClient;
	
	public static DefaultHttpClient newInstance(){
		if(httpClient == null){
			httpClient = new DefaultHttpClient();
		}
		
		return httpClient;
	}
}
