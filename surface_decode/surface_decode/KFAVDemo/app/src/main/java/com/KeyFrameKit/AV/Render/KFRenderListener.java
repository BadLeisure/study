package com.KeyFrameKit.AV.Render;
//
//  KFRenderListener
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.view.Surface;

import androidx.annotation.NonNull;

public interface KFRenderListener {
    void surfaceCreate(@NonNull Surface surface);// 渲染缓存创建
    void surfaceChanged(@NonNull Surface surface, int width, int height);// 渲染缓存变更分辨率
    void surfaceDestroy(@NonNull Surface surface);// 渲染缓存销毁
}
