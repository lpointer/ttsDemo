package com.iflytek.mscv5plusdemo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.iflytek.speech.setting.TtsSettings;

public class TtsManager {
    private static String TAG = TtsDemo.class.getSimpleName();
    private static TtsManager mTtsManager;
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 默认云端发音人
    public static String voicerCloud = "xiaoyan";
    // 默认本地发音人
    public static String voicerLocal = "xiaoyan";

    public static String voicerXtts = "xiaoyan";
    // 云端发音人列表
    private String[] cloudVoicersEntries;
    private String[] cloudVoicersValue;

    // 本地发音人列表
    private String[] localVoicersEntries;
    private String[] localVoicersValue;

    // 增强版发音人列表
    private String[] xttsVoicersEntries;
    private String[] xttsVoicersValue;

    //缓冲进度
    private int mPercentForBuffering = 0;
    //播放进度
    private int mPercentForPlaying = 0;

    // 云端/本地选择按钮
    private RadioGroup mRadioGroup;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    private Toast mToast;
    private SharedPreferences mSharedPreferences;
    private Activity mActivity;

    public static TtsManager getInstance(Activity activity) {
        if (mTtsManager == null) {
            mTtsManager = new TtsManager(activity);
        }

        return mTtsManager;
    }

    private TtsManager(Activity activity) {
        mActivity = activity;
    }

    public void init() {

        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(mActivity, mTtsInitListener);

        // 云端发音人名称列表
        cloudVoicersEntries = mActivity.getResources().getStringArray(R.array.voicer_cloud_entries);
        cloudVoicersValue = mActivity.getResources().getStringArray(R.array.voicer_cloud_values);

        // 本地发音人名称列表
        localVoicersEntries = mActivity.getResources().getStringArray(R.array.voicer_local_entries);
        localVoicersValue = mActivity.getResources().getStringArray(R.array.voicer_local_values);

        // 增强版发音人名称列表
        xttsVoicersEntries = mActivity.getResources().getStringArray(R.array.voicer_xtts_entries);
        xttsVoicersValue = mActivity.getResources().getStringArray(R.array.voicer_xtts_values);

        mSharedPreferences = mActivity.getSharedPreferences(TtsSettings.PREFER_NAME, Activity.MODE_PRIVATE);
        setParam();
    }


    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");

            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };


    /**
     * 参数设置
     */
    private void setParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        //设置合成
        if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            //设置使用云端引擎
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            //设置发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicerCloud);

        } else if (mEngineType.equals(SpeechConstant.TYPE_LOCAL)) {
            //设置使用本地引擎
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            //设置发音人资源路径
            mTts.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath());
            //设置发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicerLocal);
        } else {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_XTTS);
            //设置发音人资源路径
            mTts.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath());
            //设置发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicerXtts);
        }
        //mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY,"1");//支持实时音频流抛出，仅在synthesizeToUri条件下支持
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, mSharedPreferences.getString("speed_preference", "50"));
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, mSharedPreferences.getString("pitch_preference", "50"));
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, mSharedPreferences.getString("volume_preference", "50"));
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, mSharedPreferences.getString("stream_preference", "3"));
        //	mTts.setParameter(SpeechConstant.STREAM_TYPE, AudioManager.STREAM_MUSIC+"");

        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH,
                mActivity.getExternalFilesDir("msc").getAbsolutePath() + "/tts.pcm");


    }

    //获取发音人资源路径
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        String type = "tts";
        if (mEngineType.equals(SpeechConstant.TYPE_XTTS)) {
            type = "xtts";
        }
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(mActivity, ResourceUtil.RESOURCE_TYPE.assets, type + "/common.jet"));
        tempBuffer.append(";");
        //发音人资源
        if (mEngineType.equals(SpeechConstant.TYPE_XTTS)) {
            tempBuffer.append(ResourceUtil.generateResourcePath(mActivity, ResourceUtil.RESOURCE_TYPE.assets, type + "/" + TtsDemo.voicerXtts + ".jet"));
        } else {
            tempBuffer.append(ResourceUtil.generateResourcePath(mActivity, ResourceUtil.RESOURCE_TYPE.assets, type + "/" + TtsDemo.voicerLocal + ".jet"));
        }

        return tempBuffer.toString();
    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            //showTip("开始播放");
            Log.d(TAG, "开始播放：" + System.currentTimeMillis());
        }

        @Override
        public void onSpeakPaused() {
            showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            showTip("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
            mPercentForBuffering = percent;
            showTip(String.format(mActivity.getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            mPercentForPlaying = percent;
            showTip(String.format(mActivity.getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                showTip("播放完成");
            } else {
                showTip(error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                String sid = obj.getString(SpeechEvent.KEY_EVENT_AUDIO_URL);
                Log.d(TAG, "session id =" + sid);
            }

            //实时音频流输出参考
			/*if (SpeechEvent.EVENT_TTS_BUFFER == eventType) {
				byte[] buf = obj.getByteArray(SpeechEvent.KEY_EVENT_TTS_BUFFER);
				Log.e("MscSpeechLog", "buf is =" + buf);
			}*/
        }
    };

    protected void onDestroy() {
        if (null != mTts) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
    }

    private void showTip(final String str) {
        mActivity.runOnUiThread(() -> {
            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(mActivity, str, Toast.LENGTH_SHORT);
            mToast.show();
        });
    }

    public void startLocalSpeaking(String tip) {
        mTts.startSpeaking(tip, new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {

            }

            @Override
            public void onBufferProgress(int i, int i1, int i2, String s) {

            }

            @Override
            public void onSpeakPaused() {

            }

            @Override
            public void onSpeakResumed() {

            }

            @Override
            public void onSpeakProgress(int i, int i1, int i2) {

            }

            @Override
            public void onCompleted(SpeechError speechError) {

            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        });
    }
}
