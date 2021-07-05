package com.example.rtc.media;

import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

import com.xiaoyu.open.RtcGlobalConfig;
import com.xiaoyu.open.audio.RtcAudioCapturer;
import com.xiaoyu.open.audio.RtcAudioDataSource;
import com.xiaoyu.open.audio.RtcAudioSampleInfo;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AudioCapture implements RtcAudioCapturer {

    private static final Logger LOGGER = Logger.getLogger("AudioCaptureSoft");

    private AudioRecord mRecorder;
    private Thread mRecordingThread;
    private AtomicBoolean mIsRecording = new AtomicBoolean(false);
    private RtcAudioSampleInfo pcmSampleInfo;
    private final Object lock = new Object();
    private int audioSource;

    private boolean mEnableDump;
    private final int CHANNEL_IN;

    public AudioCapture(int audioSource, int channel, boolean dump) {
        this.audioSource = audioSource;
        this.CHANNEL_IN = channel;
        this.mEnableDump = dump;
    }

    @Override
    public void onSampleInfoChanged(RtcAudioSampleInfo sampleInfo) {
        LOGGER.info("onSampleInfoChanged: " + sampleInfo);
        this.pcmSampleInfo = sampleInfo;
    }

    private RtcAudioDataSource dataSource;

    @Override
    public void onCreate(RtcAudioDataSource dataSource) {
        synchronized (lock) {
            this.dataSource = dataSource;
        }
    }

    @Override
    public void notifyAudioNoInput() {
        LOGGER.warning("notifyAudioNoInput");
        onStopCapture();
        onStartCapture(sourceId);
    }

    @Override
    public void onDestroy() {
        onStopCapture();
    }

    private String sourceId;

    @Override
    public void onStartCapture(final String sourceId) {
        this.sourceId = sourceId;
        if (mRecorder == null) {
            initRecorder();
        }

        if (mRecorder == null) {
            LOGGER.warning("onStartCapture failed, recorder is null");
            return;
        }

        if (!mIsRecording.compareAndSet(false, true)) { // 有一个作用 已经在recording 的时候,可以return, 防止在多跑一个线程 , 一定程度可以保证线程安全
            LOGGER.warning("onStartCapture failed, isRecording is not expected");
            return;
        }

        mRecordingThread = new Thread(() -> {
            LOGGER.info("onStartCapture: recorder thread enter, id=" + mRecordingThread.getId());
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            try {
                mRecorder.startRecording();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "startRecording error", e);
                LOGGER.info("onStartCapture: recorder thread exit");
                return;
            }

            LOGGER.info("onStartCapture: recorder is started");

            byte[] frameBuffer = new byte[pcmSampleInfo.frameBufferSize];
            DataOutputStream pcmDumper = null;
            if (mEnableDump) {
                File pcmFile = new File(RtcGlobalConfig.getWorkDir(), "audio_record.pcm");
                if (pcmFile.exists()) {
                    if (pcmFile.delete()) {
                        LOGGER.severe("delete " + pcmFile.getPath() + " fail");
                    }
                }
                try {
                    pcmDumper = new DataOutputStream(new FileOutputStream(pcmFile));
                } catch (Exception e) {
                    LOGGER.severe(Log.getStackTraceString(e));
                }
            }
            int readCount = 0;//每周期采样次数
            int maxCount = (int) (1000 * 1.2) / pcmSampleInfo.frameDuration; //每周期允许最大采样次数
            long startTime = 0;//每周期起始时间
            while (mIsRecording.get()) {
                int ret = mRecorder.read(frameBuffer, 0, frameBuffer.length);
                if (pcmDumper != null) {
                    try {
                        pcmDumper.write(frameBuffer);
                    } catch (IOException e) {
                        LOGGER.severe(Log.getStackTraceString(e));
                    }
                }
                if (AudioRecord.ERROR_INVALID_OPERATION != ret && AudioRecord.ERROR_BAD_VALUE != ret) {
                    dataSource.putAudioData(sourceId, frameBuffer, pcmSampleInfo);
                    readCount++;
                    long time = System.currentTimeMillis();
                    long diff = time - startTime;
                    if (diff / 1000 > 0) {//超过一个时间周期
                        startTime = time;
                        LOGGER.info("onStartCapture: read " + readCount + " frames");
                        readCount = 0;
                    } else if (readCount > maxCount) {//一个周期内超过最大采样次数
                        diff = 1000 - diff;
                        LOGGER.severe("onStartCapture: read to fast, sleep " + diff + "ms");
                        SystemClock.sleep(diff);
                    }
                } else {
                    LOGGER.severe("onStartCapture: recording thread read data error, ret=" + ret);
                    SystemClock.sleep(5);
                }
            }
            if (pcmDumper != null) {
                try {
                    pcmDumper.flush();
                } catch (IOException e) {
                    LOGGER.severe(Log.getStackTraceString(e));
                }
                try {
                    pcmDumper.close();
                } catch (IOException e) {
                    LOGGER.severe(Log.getStackTraceString(e));
                }
            }
            try {
                mRecorder.stop();
                mRecorder.release();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "stopRecording error", e);
            } finally {
                mRecorder = null;
                mIsRecording.set(false);
            }
            LOGGER.info("onStartCapture: recorder thread exit");
        }, "rtc_record");
        mRecordingThread.start();
    }

    @Override
    public void onStopCapture() {
        LOGGER.info("onStopCapture: enter");

        mIsRecording.set(false);

        if (mRecordingThread != null) {
            try {
                mRecordingThread.join();
            } catch (InterruptedException e) {
                LOGGER.warning("onStopCapture: recordingThread join failed, message is " + e.getMessage());
            } finally {
                mRecordingThread = null;
            }
        }
        LOGGER.info("onStopCapture: exit");
    }

    private void initRecorder() {
        LOGGER.info("initRecorder: audioSource=" + audioSource + ", sampleRate=" + pcmSampleInfo.sampleRateInHz);
        try {
            int minBufferSize = AudioRecord.getMinBufferSize(
                    pcmSampleInfo.sampleRateInHz,
                    CHANNEL_IN,
                    RtcAudioSampleInfo.ENCODING_PCM_16BIT);

            if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                LOGGER.warning("initRecorder: getMinBufferSize failed");
                return;
            }

            int bufferSize = pcmSampleInfo.frameBufferSize > minBufferSize ? pcmSampleInfo.frameBufferSize + minBufferSize : minBufferSize * 2;

            mRecorder = new AudioRecord(
                    audioSource,
                    pcmSampleInfo.sampleRateInHz,
                    CHANNEL_IN,
                    RtcAudioSampleInfo.ENCODING_PCM_16BIT,
                    bufferSize);

            LOGGER.info("initRecorder: recorderState=" + mRecorder.getState() + ", mFrameBufferSize=" + pcmSampleInfo.frameBufferSize + ", bufferSize=" + bufferSize);

            if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                LOGGER.severe("initRecorder: recorderState error");
                mRecorder.release();
                mRecorder = null;
            }
        } catch (Exception e) {
            LOGGER.severe(Log.getStackTraceString(e));
        }
    }
}