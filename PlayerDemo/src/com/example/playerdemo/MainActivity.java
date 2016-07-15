package com.example.playerdemo;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnBufferingUpdateListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnCompletionListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnPreparedListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnVideoSizeChangedListener;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;

public class MainActivity extends Activity implements Callback,
		OnPreparedListener, OnVideoSizeChangedListener, OnClickListener,
		OnBufferingUpdateListener, OnCompletionListener {

	private SurfaceView mSurfaceView;
	private SurfaceHolder holder;
	private int mVideoWidth, mVideoHeight;
	private IjkMediaPlayer mMediaPlayer;
	protected int playerWidth, playerHeight;
	private ViewTreeObserver observer;
	private boolean isState;
	private View top, bottom;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);
		// 加载本地文件
		IjkMediaPlayer.loadLibrariesOnce(null);
		IjkMediaPlayer.native_profileBegin("libijkplayer.so");
		mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
		holder = mSurfaceView.getHolder();
		holder.addCallback(this);
		holder.setFormat(PixelFormat.RGBA_8888);
		initeView();
	}

	private void initeView() {
		observer = mSurfaceView.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@SuppressWarnings("deprecation")
			@Override
			public void onGlobalLayout() {
				// TODO 自动生成的方法存根
				mSurfaceView.getViewTreeObserver()
						.removeGlobalOnLayoutListener(this);
				playerWidth = mSurfaceView.getWidth();
				playerHeight = mSurfaceView.getHeight();
			}
		});
		findViewById(R.id.iv_fullscreen).setOnClickListener(this);
		top = findViewById(R.id.v_top);
		bottom = findViewById(R.id.v_bottom);
	}

	/**
	 * rtmp://tv.sznews.com:1935/live/live_240_mc43 深圳卫视
	 * rtmp://203.207.99.19:1935/live/zgjyt 湖南卫视 
	 * rtmp://60.174.36.89:1935/live/vod4 池州新闻 rtmp流
	 * http://fms.cntv.lxdns.com/live/flv/channel78.flv 厦门影视,flv流
	 * http://hlstest.imgo.tv/hva4/index_512k.m3u8 湖南经视m3u8流
	 * http://live.sxtv.com.cn/channels/shaoxing/news/flv:500k/live 绍兴新闻综合http流
	 * 经测试flv,rtmp,m3u8,http都支持,其他暂时没找到测试源
	 */
	private void playVideo() {
		doCleanUp();
		try {
			// 创建播放器，并初始化监听
			mMediaPlayer = new IjkMediaPlayer();
			mMediaPlayer
					.setDataSource("http://hlstest.imgo.tv/hva4/index_512k.m3u8"
							.trim());
			mMediaPlayer.setDisplay(holder);
			mMediaPlayer.prepareAsync();
			mMediaPlayer.setOnBufferingUpdateListener(this);
			mMediaPlayer.setOnCompletionListener(this);
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setOnVideoSizeChangedListener(this);
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
		} catch (Exception e) {
			Log.e("xf", "error: " + e.getMessage(), e);
		}
	}

	private void changeSurfaceSize() {
		// get screen size
		int dw = getWindowManager().getDefaultDisplay().getWidth();
		int dh = getWindowManager().getDefaultDisplay().getHeight();

		// calculate aspect ratio
		double ar = (double) mVideoWidth / (double) mVideoHeight;
		// calculate display aspect ratio
		double dar = (double) dw / (double) dh;

		if (dar < ar)
			dh = (int) (dw / ar);
		holder.setFixedSize(mVideoWidth, mVideoHeight);
		ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
		lp.width = dw;
		lp.height = dh;
		mSurfaceView.setLayoutParams(lp);
		mSurfaceView.invalidate();
	}

	private void doCleanUp() {
		mVideoWidth = 0;
		mVideoHeight = 0;
	}

	/**
	 * 释放资源
	 */
	private void releaseMediaPlayer() {
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseMediaPlayer();
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		playVideo();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {

	}

	@Override
	public void onVideoSizeChanged(IMediaPlayer mp, int width, int height,
			int sar_num, int sar_den) {
		if (width == 0 || height == 0) {
			Log.e("xf", "invalid video width(" + width + ") or height("
					+ height + ")");
			return;
		}
		mVideoWidth = playerWidth;
		mVideoHeight = playerHeight;
		startVideoPlayback();
	}

	private void startVideoPlayback() {
		Log.v("xf", "startVideoPlayback");
		holder.setFixedSize(mVideoWidth, mVideoHeight);
		mMediaPlayer.start();
	}

	@Override
	public void onPrepared(IMediaPlayer mp) {
		mMediaPlayer.start();
	}

	@Override
	public void onClick(View arg0) {
		isState = !isState;
		if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			top.setVisibility(View.GONE);
			bottom.setVisibility(View.GONE);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			top.setVisibility(View.VISIBLE);
			bottom.setVisibility(View.VISIBLE);
		}
		changeSurfaceSize();
	}

	@Override
	public void onCompletion(IMediaPlayer mp) {

	}

	@Override
	public void onBufferingUpdate(IMediaPlayer mp, int percent) {

	}
}
