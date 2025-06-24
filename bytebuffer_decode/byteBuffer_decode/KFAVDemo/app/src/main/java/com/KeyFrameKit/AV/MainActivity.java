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
import android.media.MediaFormat;
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
import com.KeyFrameKit.AV.MediaCodec.KFByteBufferCodec;
import com.KeyFrameKit.AV.MediaCodec.KFMediaCodecInterface;
import com.KeyFrameKit.AV.MediaCodec.KFMediaCodecListener;
import com.KeyFrameKit.AV.MediaCodec.KFVideoSurfaceDecoder;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private KFMP4Demuxer mDemuxer;// 解封装器
    private KFDemuxerConfig mDemuxerConfig;// 解封装器配置
    private KFMediaCodecInterface mDecoder = null; //解码器
    private FileOutputStream mStream = null;

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

        // 解封装配置
        mDemuxerConfig = new KFDemuxerConfig();
        mDemuxerConfig.path = Environment.getExternalStorageDirectory().getPath() + "/2.mp4";
        mDemuxerConfig.demuxerType = KFMediaBase.KFMediaType.KFMediaVideo;
        if(mStream == null){
            try {
                mStream = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/test.yuv");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        FrameLayout.LayoutParams startParams = new FrameLayout.LayoutParams(200, 120);
        startParams.gravity = Gravity.CENTER_HORIZONTAL;
        Button startButton = new Button(this);
        startButton.setTextColor(Color.BLUE);
        startButton.setText("开始");
        startButton.setVisibility(View.VISIBLE);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 创建解封装
                if(mDemuxer == null){
                    mDemuxer = new KFMP4Demuxer(mDemuxerConfig,mDemuxerListener);
                    // 创建解码器
                    mDecoder = new KFByteBufferCodec();
                    mDecoder.setup(false,mDemuxer.videoMediaFormat(),mDecoderListener,null);

                    // 循环读取数据输入给解码器
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    ByteBuffer nextBuffer = mDemuxer.readVideoSampleData(bufferInfo);
                    while (nextBuffer != null){
                        mDecoder.processFrame(new KFBufferFrame(nextBuffer,bufferInfo));
                        nextBuffer = mDemuxer.readVideoSampleData(bufferInfo);
                    }
                    mDecoder.flush();
                    Log.i("KFDemuxer","complete");
                }
            }
        });
        addContentView(startButton, startParams);
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
        // 解码回调出粗
        public void onError(int error, String errorMsg) {

        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        // 解码后数据回调
        public void dataOnAvailable(KFFrame frame) {
            if(frame == null){
                return;
            }

            KFBufferFrame bufferFrame = (KFBufferFrame)frame;
            if(bufferFrame.buffer == null){
                return;
            }

            MediaFormat mediaFormat = mDecoder.getOutputMediaFormat();
            int width = mediaFormat.getInteger("width");
            int height = mediaFormat.getInteger("height");
            int cropLeft = mediaFormat.getInteger("crop-left");
            int cropRight = mediaFormat.getInteger("crop-right");
            int cropTop = mediaFormat.getInteger("crop-top");
            int cropBottom = mediaFormat.getInteger("crop-bottom");
            int colorFormat = mediaFormat.getInteger("color-format");//COLOR_FormatYUV420SemiPlanar

            // yuv数据存储本地
            try {
                byte[] dst = new byte[(int) (width*height*1.5)];
                bufferFrame.buffer.get(dst);
                mStream.write(dst);
            }  catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}