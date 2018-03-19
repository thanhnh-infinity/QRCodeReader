/*
 * Basic no frills app which integrates the ZBar barcode scanner with
 * the camera.
 * 
 * Created by lisah0 on 2012-02-24
 */
package net.sourceforge.zbar.android.CameraTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;
import org.json.JSONObject;



import net.sourceforge.zbar.android.CameraTest.CameraPreview;
import net.sourceforge.zbar.android.util.MyHttpClient;
import net.sourceforge.zbar.android.util.SystemParameter;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.pm.ActivityInfo;

import android.os.Bundle;
import android.os.Handler;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.FrameLayout;
import android.widget.Button;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.AutoFocusCallback;

import android.hardware.Camera.Size;

import android.widget.TextView;

/* Import ZBar Class files */
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import net.sourceforge.zbar.Config;

public class CameraTestActivity extends Activity {  
	private Camera mCamera;
	private CameraPreview mPreview;
	private Handler autoFocusHandler;

	TextView scanText;
	Button scanButton;

	ImageScanner scanner;  
	
  
	private boolean barcodeScanned = false;
	private boolean previewing = true;

	String qr_code_id = "";

	static {
		System.loadLibrary("iconv");
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		autoFocusHandler = new Handler();
		mCamera = getCameraInstance();

		/* Instance barcode scanner */
		scanner = new ImageScanner();
		scanner.setConfig(0, Config.X_DENSITY, 3);
		scanner.setConfig(0, Config.Y_DENSITY, 3);

		mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
		FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
		preview.addView(mPreview);  

		scanText = (TextView) findViewById(R.id.scanText);

		scanButton = (Button) findViewById(R.id.ScanButton);

		scanButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (barcodeScanned) {
					try {
						barcodeScanned = false;
						scanText.setText("Đang Scanning và kết nối Server. Vui lòng chờ...");
						mCamera.setPreviewCallback(previewCb);
						mCamera.startPreview();
						previewing = true;
						mCamera.autoFocus(autoFocusCB);
					} catch (Exception $ex) {
						
					}
				}
			}
		});
	}

	public void onPause() {
		super.onPause();
		releaseCamera();
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open();
		} catch (Exception e) {
		}
		return c;
	}

	private void releaseCamera() {
		if (mCamera != null) {
			previewing = false;
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}
	}

	private Runnable doAutoFocus = new Runnable() {
		public void run() {
			if (previewing)
				mCamera.autoFocus(autoFocusCB);
		}
	};

	PreviewCallback previewCb = new PreviewCallback() {
		public void onPreviewFrame(byte[] data, Camera camera) {
			Camera.Parameters parameters = camera.getParameters();
			Size size = parameters.getPreviewSize();

			Image barcode = new Image(size.width, size.height, "Y800");
			barcode.setData(data);

			int result = scanner.scanImage(barcode);

			if (result != 0) {
				previewing = false;
				mCamera.setPreviewCallback(null);
				mCamera.stopPreview();

				SymbolSet syms = scanner.getResults();
				for (Symbol sym : syms) {
					qr_code_id = sym.getData();
					String cmsRequest = setUpCMSrequest(qr_code_id);
					String response = "";
					String errorCode = "Check mạng !";
					String errorReason = "";
					try {
						response = requestActiveQRCode(cmsRequest);
						JSONObject jSONObject = new JSONObject(response);

						boolean successObjJSON = jSONObject
								.getBoolean("success");
						
						
						if (successObjJSON) {
							AlertDialog.Builder builder = new AlertDialog.Builder(CameraTestActivity.this);
							builder.setTitle("THÀNH CÔNG");
							builder.setMessage("Khách mời " + qr_code_id + " đã được Actived THÀNH CÔNG");
							
							builder.setNegativeButton("OK",
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {
											barcodeScanned = false;
											scanText.setText("Đang Scanning và kết nối Server. Vui lòng chờ...");
											mCamera.setPreviewCallback(previewCb);
											mCamera.startPreview();
											previewing = true;
											mCamera.autoFocus(autoFocusCB);
										}
									});

							builder.show();
						} else {
							errorCode = jSONObject.getString("type");
							AlertDialog.Builder builder = new AlertDialog.Builder(CameraTestActivity.this);
							builder.setTitle("LỖI");
							errorReason = jSONObject.getString("reason") ;
							builder.setMessage(jSONObject.getString("reason") + ". ID : " + qr_code_id);
							
							builder.setNegativeButton("OK",
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {
											
										}
									});

							builder.show();
						}

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();  
					}

					scanText.setText(errorCode + ":" + errorReason);
					barcodeScanned = true;
				}
			}
		}

		private String setUpCMSrequest(String qr_code) {
			String cmsURL = "";
			cmsURL = SystemParameter.REQUEST_ACTIVE_QR_CODE;
			cmsURL = cmsURL.replace("%q", qr_code);
			cmsURL = cmsURL.replace("%t",
					String.valueOf(System.currentTimeMillis()));
			Log.i("Request", cmsURL);
			return cmsURL;

		}

		/**
		 * NGUYEN HAI THANH
		 * 
		 * @param cmsURL
		 * @return
		 * @throws IOException
		 */
		public String requestActiveQRCode(String cmsURL) throws IOException {  
			HttpClient httpClient = MyHttpClient.newInstance();
			HttpResponse response = httpClient.execute(new HttpGet(cmsURL));
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				out.close();
				String responseString = out.toString();
				return responseString;
			} else {
				// Closes the connection.
				response.getEntity().getContent().close();
				throw new IOException(statusLine.getReasonPhrase());
			}
		}
	};

	// Mimic continuous auto-focusing
	AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
		public void onAutoFocus(boolean success, Camera camera) {
			autoFocusHandler.postDelayed(doAutoFocus, 1000);
		}
	};
}
