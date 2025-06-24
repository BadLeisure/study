package com.KeyFrameKit.AV;
//
//  MainActivity
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.KeyFrameKit.AV.Base.KFBufferFrame;
import com.KeyFrameKit.AV.Base.KFFrame;
import com.KeyFrameKit.AV.Base.KFGLBase;
import com.KeyFrameKit.AV.Base.KFMediaBase;
import com.KeyFrameKit.AV.Base.KFTextureFrame;
import com.KeyFrameKit.AV.Demuxer.KFMP4Demuxer;
import com.KeyFrameKit.AV.Demuxer.KFDemuxerConfig;
import com.KeyFrameKit.AV.Demuxer.KFDemuxerListener;
import com.KeyFrameKit.AV.Effect.KFGLContext;
import com.KeyFrameKit.AV.MediaCodec.KFMediaCodecInterface;
import com.KeyFrameKit.AV.MediaCodec.KFMediaCodecListener;
import com.KeyFrameKit.AV.MediaCodec.KFVideoSurfaceDecoder;
import com.KeyFrameKit.AV.Render.KFRenderView;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private KFMP4Demuxer mDemuxer;// 解封装器
    private KFDemuxerConfig mDemuxerConfig;// 解封装器配置
    private KFMediaCodecInterface mDecoder = null;// 解码
    private KFRenderView mRenderView;// 渲染
    private KFGLContext mGLContext;// GL上下文
    private Timer mTimer;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 申请采集、存储权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this,
                    new String[] {Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }

        // 创建GL上下文
        mGLContext = new KFGLContext(null);
        // 创建渲染视图
        mRenderView = new KFRenderView(this,mGLContext.getContext());
        WindowManager windowManager = (WindowManager)this.getSystemService(this.WINDOW_SERVICE);
        Rect outRect = new Rect();
        windowManager.getDefaultDisplay().getRectSize(outRect);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(outRect.width(), outRect.height());
        addContentView(mRenderView,params);

        // 创建解封装器配置
        mDemuxerConfig = new KFDemuxerConfig();
        mDemuxerConfig.path = Environment.getExternalStorageDirectory().getPath() + "/2.mp4";
        mDemuxerConfig.demuxerType = KFMediaBase.KFMediaType.KFMediaVideo;

        FrameLayout.LayoutParams startParams = new FrameLayout.LayoutParams(200, 120);
        startParams.gravity = Gravity.CENTER_HORIZONTAL;
        Button startButton = new Button(this);
        startButton.setTextColor(Color.BLUE);
        startButton.setText("开始");
        startButton.setVisibility(View.VISIBLE);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 创建解封装与解码器
                if(mDemuxer == null){
                    mDemuxer = new KFMP4Demuxer(mDemuxerConfig,mDemuxerListener);
                    mDecoder = new KFVideoSurfaceDecoder();
                    mDecoder.setup(false, mDemuxer.videoMediaFormat(),mDecoderListener,mGLContext.getContext());
                    ((Button)view).setText("停止");
                }else{
                    mDemuxer.release();
                    mDemuxer = null;
                    mDecoder.release();
                    mDecoder = null;
                    ((Button)view).setText("开始");
                }
            }
        });
        addContentView(startButton, startParams);

        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // 根据Timer回调读取解封装数据给解码器
                if(mDemuxer != null){
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    ByteBuffer byteBuffer = mDemuxer.readVideoSampleData(bufferInfo);
                    if(byteBuffer != null){
                        KFBufferFrame frame = new KFBufferFrame();
                        frame.bufferInfo = bufferInfo;
                        frame.buffer = byteBuffer;
                        mDecoder.processFrame(frame);
                    }
                }
            }
        };
        timer.schedule(task,0,33);
    }

    private KFDemuxerListener mDemuxerListener = new KFDemuxerListener() {
        @Override
        // 解封装回调出错
        public void demuxerOnError(int error, String errorMsg) {
            Log.i("KFDemuxer","error" + error + "msg" + errorMsg);
        }
    };

    private KFMediaCodecListener mDecoderListener = new KFMediaCodecListener() {
        @Override
        // 解码回调出错
        public void onError(int error, String errorMsg) {

        }

        // 解码回调进行渲染
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void dataOnAvailable(KFFrame frame) {
            mRenderView.render((KFTextureFrame) frame);
        }
    };
}