package com.example.rtc.media;

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.xiaoyu.open.RtcGlobalConfig;
import com.xiaoyu.open.audio.RtcAudioDataSource;
import com.xiaoyu.open.audio.RtcAudioRenderer;
import com.xiaoyu.open.audio.RtcAudioSampleInfo;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;


public class AudioRenderer implements RtcAudioRenderer {
    private static final Logger LOGGER = Logger.getLogger("AudioRendererSoft");
    private static final int AUDIO_PLAY_RETRY = 10086;
    private RtcAudioDataSource dataSource;
    private int streamType = Integer.MIN_VALUE;
    private RtcAudioSampleInfo pcmSampleInfo;
    private AudioTrack mAudioTracker;
    private Thread mAudioTrackingThread;
    private AtomicBoolean mIsAudioTracking = new AtomicBoolean(false);
    private AtomicBoolean mSpeakerMute = new AtomicBoolean(true);
    private boolean mEnableDump;
    private final boolean newPerCall;
    private Handler handler;
    private boolean onStartRender = false;

    public AudioRenderer(boolean newPerCall, boolean dump) {
        this.newPerCall = newPerCall;
        this.mEnableDump = dump;
        if (!newPerCall) {
            initAudioTrack();
        }
        handler = new Handler(msg -> {
            if (msg.what == AUDIO_PLAY_RETRY) {
                synchronized (AudioRenderer.this) {
                    if (onStartRender) {
                        unInitAudioTrack();
                        onStartRender(null);
                    } else {
                        LOGGER.info("retry play error");
                    }
                }
            }
            return false;
        });
    }

    @Override
    public void onCreate(RtcAudioDataSource dataSource) {
        this.dataSource = dataSource;
        LOGGER.info("onCreate");
    }

    @Override
    public void onStreamTypeChanged(int streamType) {
        LOGGER.info("onStreamTypeChanged: " + streamType);
        if (this.streamType != streamType) {
            this.streamType = streamType;
            if (!newPerCall) {
                unInitAudioTrack();
                initAudioTrack();
            }
        }
    }

    @Override
    public void onSampleInfoChanged(RtcAudioSampleInfo sampleInfo) {
        LOGGER.info("onSampleInfoChanged: " + sampleInfo);
        if (pcmSampleInfo == null || pcmSampleInfo.sampleRateInHz != sampleInfo.sampleRateInHz || pcmSampleInfo.channels != sampleInfo.channels) {
            this.pcmSampleInfo = sampleInfo;
            if (!newPerCall) {
                unInitAudioTrack();
                initAudioTrack();
            }
        }
    }

    private void unInitAudioTrack() {
        if (mAudioTracker == null) {
            return;
        }
        LOGGER.severe("unInitAudioTrack: newPerCall AudioTrack");
        try {
            mAudioTracker.stop();
        } catch (Exception ignored) {
        }
        try {
            mAudioTracker.release();
        } catch (Exception ignored) {
        }
        mAudioTracker = null;

    }

    private void initAudioTrack() {
        if (mAudioTracker != null || pcmSampleInfo == null) {
            return;
        }
        LOGGER.info("init AudioTrack enter, streamType=" + streamType + ", channels=" + pcmSampleInfo.channels);
        int audioFormat = pcmSampleInfo.channels < 2 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_IN_STEREO;
        int minBufferSize = AudioTrack.getMinBufferSize(pcmSampleInfo.sampleRateInHz, audioFormat, RtcAudioSampleInfo.ENCODING_PCM_16BIT);
        try {
            mAudioTracker = new AudioTrack(streamType, pcmSampleInfo.sampleRateInHz, audioFormat, RtcAudioSampleInfo.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
            LOGGER.info("init AudioTrack success, minBufferSize=" + minBufferSize + ", sampleRate=" + mAudioTracker.getSampleRate());
        } catch (Exception e) {
            LOGGER.severe("init AudioTrack failed, " + Log.getStackTraceString(e));
        }
        LOGGER.info("init AudioTrack exit");
    }

    private int readErrorCount;
    private final Object lockSourceId = new Object();
    private String mSourceId;

    @Override
    public synchronized void onStartRender(final String sourceId) {
        onStartRender = true;
        initAudioTrack();
        if (mAudioTracker == null) {
            LOGGER.severe("onStartRender failed, audioTracker is null");
            return;
        }
        synchronized (lockSourceId) {
            if (sourceId != null) {
                mSourceId = sourceId;
            }
        }
        LOGGER.info("onStartRender: sourceId=" + mSourceId);

        if (!mIsAudioTracking.compareAndSet(false, true)) {
            LOGGER.warning("onStartRender skip, isAudioTracking value is not expected");
            return;
        }
        readErrorCount = 0;
        mAudioTrackingThread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            long tid = mAudioTrackingThread.getId();
            LOGGER.info("tracking thread enter, id=" + tid);
            try {
                LOGGER.info("tracker play");
                mAudioTracker.play();
                LOGGER.info("tracker played");
            } catch (Exception e) {
                LOGGER.severe("tracker play failed, " + Log.getStackTraceString(e));
                mIsAudioTracking.set(false);
                handler.sendEmptyMessageAtTime(AUDIO_PLAY_RETRY, 3000);
                return;
            }
            byte[] audioBuffer = new byte[pcmSampleInfo.frameBufferSize];
            byte[] mutedAudioBuffer = new byte[pcmSampleInfo.frameBufferSize];
            long totalCount = 0;
            DataOutputStream pcmDumper = null;
            if (mEnableDump) {
                File pcmFile = new File(RtcGlobalConfig.getWorkDir(), "audio_play.pcm");
                if (pcmFile.exists()) {
                    if (!pcmFile.delete()) {
                        LOGGER.severe("delete " + pcmFile.getPath() + " fail");
                    }
                }
                try {
                    pcmDumper = new DataOutputStream(new FileOutputStream(pcmFile));
                } catch (Exception e) {
                    LOGGER.severe(Log.getStackTraceString(e));
                }
            }
            while (mIsAudioTracking.get()) {
                String playSourceId;
                synchronized (lockSourceId) {
                    playSourceId = mSourceId;
                }
                boolean muteSource = SOURCE_ID_LOCAL_MUTE.equals(playSourceId);
                long remainData = totalCount - mAudioTracker.getPlaybackHeadPosition() * 2;
                // don't consider wrap around case, should be after 27 hours.
                if (remainData < 0) {
                    LOGGER.warning("tracker something wrong, sourceId=" + playSourceId + ", totalCount=" + totalCount + ", remainData=" + remainData);
                    totalCount -= remainData;
                }

                boolean bResult = muteSource || (dataSource != null && dataSource.getAudioData(playSourceId, audioBuffer, pcmSampleInfo.frameBufferSize));
                if (!bResult) {
                    if (readErrorCount % 200 == 0) {
                        LOGGER.severe("render AudioData failed, sourceId=" + playSourceId);
                    }
                    SystemClock.sleep(5);
                    readErrorCount++;
                    continue;
                }
                if (pcmDumper != null) {
                    try {
                        pcmDumper.write(audioBuffer);
                    } catch (IOException e) {
                        LOGGER.severe(Log.getStackTraceString(e));
                    }
                }
                totalCount += pcmSampleInfo.frameBufferSize;
                int result = mAudioTracker.write(muteSource || mSpeakerMute.get() ? mutedAudioBuffer : audioBuffer, 0, pcmSampleInfo.frameBufferSize);
                if (result != pcmSampleInfo.frameBufferSize) {
                    LOGGER.warning("tracker write error, result=" + result);
                    SystemClock.sleep(5);
                }
                if (muteSource) {
                    SystemClock.sleep(pcmSampleInfo.frameDuration);
                }
            }
            LOGGER.info("tracking loop exit");
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
            if (newPerCall) {
                LOGGER.info("tracker stop");
                try {
                    mAudioTracker.stop();
                    LOGGER.info("tracker stopped");
                } catch (Exception e) {
                    LOGGER.warning("tracker stop failed, " + Log.getStackTraceString(e));
                }
                LOGGER.info("tracker release");
                try {
                    mAudioTracker.release();
                    LOGGER.info("tracker released");
                } catch (Exception e) {
                    LOGGER.severe("tracker release failed, " + Log.getStackTraceString(e));
                } finally {
                    mAudioTracker = null;
                }
            } else {
                LOGGER.info("tracker pause");
                try {
                    mAudioTracker.pause();
                    mAudioTracker.flush();
                    LOGGER.info("tracker paused");
                } catch (Exception e) {
                    LOGGER.warning("tracker pause failed, " + Log.getStackTraceString(e));
                }
            }
            LOGGER.info("tracking thread exit, id=" + tid);
        }, "rtc_track");

        mAudioTrackingThread.start();
    }

    @Override
    public synchronized void onStopRender() {
        LOGGER.info("onStopRender");
        handler.removeMessages(AUDIO_PLAY_RETRY);
        onStartRender = false;
        if (mAudioTracker == null) {
            LOGGER.info("onStopRender failed, audioTracker is null");
            return;
        }

        mIsAudioTracking.set(false);
        LOGGER.info("onStopRender.1");
        if (mAudioTrackingThread != null) {
            try {
                mAudioTrackingThread.join(2000);
            } catch (InterruptedException e) {
                LOGGER.severe("onStopRender, join failed, " + Log.getStackTraceString(e));
            } finally {
                mAudioTrackingThread.interrupt();
                mAudioTrackingThread = null;
            }
        }
        LOGGER.info("onStopRender.x");
    }

    @Override
    public void onSpeakerMuteChange(boolean mute) {
        if (mSpeakerMute.compareAndSet(!mute, mute)) {
            LOGGER.info("onSpeakerMuteChange: " + mute);
        }
    }

    @Override
    public void onDestroy() {
        onStopRender();
    }
}