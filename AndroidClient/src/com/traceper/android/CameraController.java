package com.traceper.android;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.traceper.R;
import com.traceper.android.interfaces.IAppService;
import com.traceper.android.services.AppService;

public class CameraController extends Activity implements SurfaceHolder.Callback{

	private Camera camera;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private boolean isPreviewRunning;
	private ProgressDialog pdialog = null; 
	private static final int UPLOAD_PHOTO = Menu.FIRST;
	private static final int TAKE_ANOTHER_PHOTO = Menu.FIRST + 1;
	private static final int BACK = Menu.FIRST + 2;
	private byte[] picture;
	private IAppService appService = null;
	private boolean pictureTaken = false;
	private Button takePictureButton = null;
	private NotificationManager mManager;
	private static int NOTIFICATION_ID = 0;
	private static final int DIALOG_ASK_TO_MAKE_IMAGE_PUBLIC = 0;	

	private Camera.PictureCallback mPictureCallbackRaw = new Camera.PictureCallback() {  
		public void onPictureTaken(byte[] data, Camera c) {  
			Log.e(getClass().getSimpleName(), "PICTURE CALLBACK RAW: " + data);  
		}  
	}; 
	private Camera.PictureCallback mPictureCallbackJpeg= new Camera.PictureCallback() { 

		public void onPictureTaken(byte[] data, Camera c) {
			picture = data;
			Log.e(getClass().getSimpleName(), "PICTURE CALLBACK JPEG: string.length = " + data.length);  
		}  
	};  

	private Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {  
		public void onShutter() {  
			Log.e(getClass().getSimpleName(), "SHUTTER CALLBACK");  
		}  
	};


	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {          
			appService = ((AppService.IMBinder)service).getService();    
		}
		public void onServiceDisconnected(ComponentName className) {          
			appService = null;
		}
	};

	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );


		setContentView(R.layout.camera_view);

		getWindow().setFormat(PixelFormat.TRANSLUCENT);

		surfaceView = (SurfaceView) findViewById(R.id.surface);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		takePictureButton = (Button) findViewById(R.id.takePictureButton);
		takePictureButton.bringToFront();
		takePictureButton.setOnClickListener(new View.OnClickListener() {			

			public void onClick(View arg0) {
				pictureTaken = true;
				camera.takePicture(mShutterCallback, mPictureCallbackRaw, mPictureCallbackJpeg);  
				openOptionsMenu();

			}
		});
		takePictureButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_camera,0,0,0);


	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		if (isPreviewRunning) {  
			camera.stopPreview();  
		}  

		try {
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
		}  

		camera.startPreview(); 

		isPreviewRunning = true; 
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		camera = Camera.open();	

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		camera.stopPreview();
		isPreviewRunning = false;
		camera.release();		
	}

	public boolean onCreateOptionsMenu(Menu menu){
		boolean result  = super.onCreateOptionsMenu(menu);
		if (pictureTaken == true) {
			menu.add(0, UPLOAD_PHOTO, 0, "Upload photo").setIcon(android.R.drawable.ic_menu_upload);
			menu.add(0, TAKE_ANOTHER_PHOTO, 1, "Take another photo").setIcon(android.R.drawable.ic_menu_camera);
		}
		menu.add(0, BACK, 2, "Back").setIcon(android.R.drawable.ic_menu_revert);

		return result;		
	}

	public boolean onPrepareOptionsMenu(Menu menu)
	{
		boolean result = super.onPrepareOptionsMenu(menu);
		MenuItem item = menu.findItem(UPLOAD_PHOTO);
		if (item == null && pictureTaken == true) {
			menu.add(0, UPLOAD_PHOTO, 0, "Upload photo").setIcon(android.R.drawable.ic_menu_upload);
			menu.add(0, TAKE_ANOTHER_PHOTO, 1, "Take another photo").setIcon(android.R.drawable.ic_menu_camera);
		}
		else if (item != null && pictureTaken == false){
			menu.removeItem(UPLOAD_PHOTO);
			menu.removeItem(TAKE_ANOTHER_PHOTO);
		}


		return result;		
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) 
		{
		case UPLOAD_PHOTO:	
			showDialog(DIALOG_ASK_TO_MAKE_IMAGE_PUBLIC);
			break;
		case TAKE_ANOTHER_PHOTO:
			pictureTaken = false;
			camera.startPreview();  
			break;
		case BACK:
			CameraController.this.finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)  
	{  
		if (keyCode == KeyEvent.KEYCODE_BACK) {  
			return super.onKeyDown(keyCode, event);  
		}  
		else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {  
			pictureTaken = true;
			camera.takePicture(mShutterCallback, mPictureCallbackRaw, mPictureCallbackJpeg);  
			this.openOptionsMenu();
			return true;  
		}  

		return false;  
	} 	

	protected void onResume(){
		super.onResume();
		bindService(new Intent(CameraController.this, AppService.class), mConnection , Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		unbindService(mConnection);
		super.onPause();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ASK_TO_MAKE_IMAGE_PUBLIC:
			final Dialog dialog = new Dialog(this);

			dialog.setContentView(R.layout.photo_description);
			dialog.setTitle(R.string.upload_photo);

			dialog.findViewById(R.id.sendPhotoButton).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					CheckBox isPublicCheckbox = (CheckBox)dialog.findViewById(R.id.isPublic);
					EditText desriptionEditText = (EditText)dialog.findViewById(R.id.photo_description);
					String description = desriptionEditText.getText().toString();
					if (description.length() > 0) {
						uploadImage(isPublicCheckbox.isChecked(), description);
						dialog.dismiss();
						CameraController.this.finish();
					}
					else {
					//	showDialog(DIALOG_ASK_TO_MAKE_IMAGE_PUBLIC);
						Toast.makeText(CameraController.this, R.string.please_enter_description, Toast.LENGTH_SHORT).show();
					}
					
				}
			});
			dialog.findViewById(R.id.cancelButton).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					dialog.dismiss();
					finish();
					
				}
			});
			
			return dialog;
		}

		return super.onCreateDialog(id);
	}

	private void uploadImage(final boolean publicImage, final String description){

		Thread uploadThread = new Thread(){
			@Override
			public void run() {
				
				appService.uploadImage(picture, publicImage, description);
				
				/*
				final String result = appService.sendImage(byteArrayOutputStream.toByteArray(), publicImage, description);
				{
					handler.post(new Runnable() {							
						@Override
						public void run() {
							String str;
							if (result.equals("1")) {
								str = getString(R.string.upload_succesfull);
							}
							else {
								str = getString(R.string.upload_failed);
							}
							notification.setLatestEventInfo(getApplicationContext(), getString(R.string.ApplicationName), str, contentIntent);
							mManager.notify(NOTIFICATION_ID, notification);
						}
					});
				}
				*/
				super.run();
			}
		};
		uploadThread.start();

	}
}
