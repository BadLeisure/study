package com.KeyFrameKit.AV.MediaCodec;
//
//  KFVideoEncoderConfig
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.media.MediaCodecInfo;
import android.os.Build;
import android.util.Size;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class KFVideoEncoderConfig {
    public Size size = new Size(720,1280);
    public int bitrate = 4 * 1024 * 1024;
    public int fps = 30;
    public int gop = 30 * 4;
    public boolean isHEVC = false;
    public int profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline;
    public int profileLevel = MediaCodecInfo.CodecProfileLevel.AVCLevel1;

    public KFVideoEncoderConfig() {

    }
}
