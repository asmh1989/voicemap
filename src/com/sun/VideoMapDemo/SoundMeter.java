package com.sun.VideoMapDemo;

import java.io.File;
import java.io.IOException;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;

public  class SoundMeter implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
	static final private double EMA_FILTER = 0.6;

	private MediaRecorder mRecorder = null;
	MediaPlayer mPlayer = null;
	private double mEMA = 0.0;
	static final int BITRATE_AMR =  5900; // bits/sec
	static final int BITRATE_3GPP = 5900;

	static final String AUDIO_3GPP = "audio/3gpp";
	static final String AUDIO_AMR = "audio/amr";
	static final String AUDIO_ANY = "audio/*";
	static final String ANY_ANY = "*/*";

	private String mRequestedType = AUDIO_AMR;
	private String videoPath;

	public void start(String name) {
		videoPath = name;
		if (mRecorder == null) {
			mRecorder = new MediaRecorder();
			mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
			if (AUDIO_AMR.equals(mRequestedType)) {
				mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
			} else if (AUDIO_3GPP.equals(mRequestedType)) {
				mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

			}
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			mRecorder.setOutputFile(name);
			try {
				mRecorder.prepare();
				mRecorder.start();
				
				mEMA = 0.0;
			} catch (IllegalStateException e) {
				System.out.print(e.getMessage());
			} catch (IOException e) {
				System.out.print(e.getMessage());
			}

		}
	}

	public void stop() {
		if (mRecorder != null) {
			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;
		}
	}

	public void pause() {
		if (mRecorder != null) {
			mRecorder.stop();
		}
	}

	public void start() {
		if (mRecorder != null) {
			mRecorder.start();
		}
	}

	public double getAmplitude() {
		if (mRecorder != null)
			return (mRecorder.getMaxAmplitude() / 2000.0);
		else
			return 0;

	}

	public double getAmplitudeEMA() {
		double amp = getAmplitude();
		mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
		return mEMA;
	}

	public void startPlayback() {
		stop2();

		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(videoPath);
			mPlayer.setOnCompletionListener(this);
			mPlayer.setOnErrorListener(this);
			mPlayer.prepare();
			mPlayer.start();
		} catch (IllegalArgumentException e) {
			mPlayer = null;
			return;
		} catch (IOException e) {
			mPlayer = null;
			return;
		}

	}

	public void stopPlayback() {
		if (mPlayer == null) // we were not in playback
			return;

		mPlayer.stop();
		mPlayer.release();
		mPlayer = null;
	}

	public void stop2() {
		stopPlayback();
	}

	public boolean onError(MediaPlayer mp, int what, int extra) {
		stop2();
		return true;
	}

	public void onCompletion(MediaPlayer mp) {
		stop2();
	}
}
