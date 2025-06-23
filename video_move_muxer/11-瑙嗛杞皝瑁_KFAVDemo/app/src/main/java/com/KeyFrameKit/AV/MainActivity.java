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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.KeyFrameKit.AV.Base.KFFrame;
import com.KeyFrameKit.AV.Base.KFGLBase;
import com.KeyFrameKit.AV.Base.KFMediaBase;
import com.KeyFrameKit.AV.Demuxer.KFMP4Demuxer;
import com.KeyFrameKit.AV.Demuxer.KFDemuxerConfig;
import com.KeyFrameKit.AV.Demuxer.KFDemuxerListener;
import com.KeyFrameKit.AV.Muxer.KFMP4Muxer;
import com.KeyFrameKit.AV.Muxer.KFMuxerConfig;
import com.KeyFrameKit.AV.Muxer.KFMuxerListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;

public class MainActivity extends AppCompatActivity {
    private KFMP4Demuxer mDemuxer;// 解封装器
    private KFDemuxerConfig mDemuxerConfig;// 解封装配置
    private KFMP4Muxer mMuxer;// 封装器
    private KFMuxerConfig mMuxerConfig;// 封装配置

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 申请采集 存储权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this,
                    new String[] {Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }

        // 解封装配置 控制仅输出视频
        mDemuxerConfig = new KFDemuxerConfig();
        mDemuxerConfig.path = Environment.getExternalStorageDirectory().getPath() + "/2.mp4";
        mDemuxerConfig.demuxerType = KFMediaBase.KFMediaType.KFMediaAV;

        // 创建封装配置
        mMuxerConfig = new KFMuxerConfig(Environment.getExternalStorageDirectory().getPath() + "/test.mp4");

        FrameLayout.LayoutParams startParams = new FrameLayout.LayoutParams(200, 120);
        startParams.gravity = Gravity.CENTER_HORIZONTAL;
        Button startButton = new Button(this);
        startButton.setTextColor(Color.BLUE);
        startButton.setText("开始");
        startButton.setVisibility(View.VISIBLE);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 创建解封装与封装
                if(mDemuxer == null){
                    mDemuxer = new KFMP4Demuxer(mDemuxerConfig,mDemuxerListener);
                    mMuxer = new KFMP4Muxer(mMuxerConfig,mMuxerListener);
                    mMuxer.start();
                    // 设置格式描述
                    mMuxer.setVideoMediaFormat(mDemuxer.videoMediaFormat());
                    mMuxer.setAudioMediaFormat(mDemuxer.audioMediaFormat());

                    // 循环读取音视频数据写入封装器
                    MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
                    ByteBuffer videoNextBuffer = mDemuxer.readVideoSampleData(videoBufferInfo);

                    MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
                    ByteBuffer audioNextBuffer = mDemuxer.readAudioSampleData(audioBufferInfo);
                    while (audioNextBuffer != null || videoNextBuffer != null){
                        if(audioNextBuffer != null){
                            mMuxer.writeSampleData(false,audioNextBuffer,audioBufferInfo);
                            audioNextBuffer = mDemuxer.readAudioSampleData(audioBufferInfo);
                        }

                        if(videoNextBuffer != null){
                            mMuxer.writeSampleData(true,videoNextBuffer,videoBufferInfo);
                            videoNextBuffer = mDemuxer.readVideoSampleData(videoBufferInfo);
                        }
                    }
                    mMuxer.stop();
                    Log.i("KFDemuxer","complete");
                }
            }
        });
        addContentView(startButton, startParams);
    }

    private KFDemuxerListener mDemuxerListener = new KFDemuxerListener() {
        @Override
        // 解封装出错回调
        public void demuxerOnError(int error, String errorMsg) {
            Log.i("KFDemuxer","error" + error + "msg" + errorMsg);
        }
    };

    private KFMuxerListener mMuxerListener = new KFMuxerListener() {
        @Override
        // 封装器出错回调
        public void muxerOnError(int error, String errorMsg) {
            Log.e("KFMuxer","error:" + error + "msg:" +errorMsg);
        }
    };
}