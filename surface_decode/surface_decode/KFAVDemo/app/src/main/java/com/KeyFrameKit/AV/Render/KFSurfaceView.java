package com.KeyFrameKit.AV.Render;
//
//  KFSurfaceView
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public class KFSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private KFRenderListener mListener = null;// 回调
    private SurfaceHolder mHolder = null;// surface的抽象接口

    public KFSurfaceView(Context context, KFRenderListener listener) {
        super(context);
        mListener = listener;
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        // Surface 创建
        mHolder = surfaceHolder;
        // 根据SurfaceHolder 创建 Surface
        if(mListener != null){
            mListener.surfaceCreate(surfaceHolder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width, int height) {
        // Surface 分辨率变更
        if(mListener != null){
            mListener.surfaceChanged(surfaceHolder.getSurface(),width,height);
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        // Surface 销毁
        if (mListener != null) {
            mListener.surfaceDestroy(surfaceHolder.getSurface());
        }
    }
}
