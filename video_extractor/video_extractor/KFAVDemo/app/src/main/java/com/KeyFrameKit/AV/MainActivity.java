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
    private FileOutputStream mStream = null;

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
        mDemuxerConfig.demuxerType = KFMediaBase.KFMediaType.KFMediaVideo;
        if(mStream == null){
            try {
                mStream = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/test.h264");
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
                // 解封装创建
                if(mDemuxer == null){
                    mDemuxer = new KFMP4Demuxer(mDemuxerConfig,mDemuxerListener);

                    // 根据HEVC 分别获取vps sps pps等信息 hevc的vps sps pps等信息都保存在csd0里面
                    if(mDemuxer.isHEVC()){
                        try {
                            ByteBuffer extradata = mDemuxer.videoMediaFormat().getByteBuffer("csd-0");
                            byte[] extradataBytes = new byte[extradata.capacity()];
                            extradata.get(extradataBytes);
                            mStream.write(extradataBytes);
                        }  catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    //h264的sps保存在csd0里面，pps保存在csd1里面
                    else{
                        try {
                            ByteBuffer sps = mDemuxer.videoMediaFormat().getByteBuffer("csd-0");
                            byte[] spsBytes = new byte[sps.capacity()];
                            sps.get(spsBytes);
                            mStream.write(spsBytes);
                        }  catch (IOException e) {
                            e.printStackTrace();
                        }

                        try {
                            ByteBuffer pps = mDemuxer.videoMediaFormat().getByteBuffer("csd-1");
                            byte[] ppsBytes = new byte[pps.capacity()];
                            pps.get(ppsBytes);
                            mStream.write(ppsBytes);
                        }  catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    // 循环读取视频数据
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    ByteBuffer nextBuffer = mDemuxer.readVideoSampleData(bufferInfo);
                    while (nextBuffer != null){
                        try {
                            byte[] dst = new byte[bufferInfo.size];
                            //从当前 ByteBuffer 的 当前位置 开始，读取 dst.length 个字节到 dst 数组中，并将位置指针后移相应的字节数。
                            nextBuffer.get(dst);
                            mStream.write(dst);
                        }  catch (IOException e) {
                            e.printStackTrace();
                        }
                        nextBuffer = mDemuxer.readVideoSampleData(bufferInfo);
                    }
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
}