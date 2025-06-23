package com.KeyFrameKit.AV.Effect;
//
//  KFSurfaceTexture
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

public class KFSurfaceTexture implements SurfaceTexture.OnFrameAvailableListener{
    private int mSurfaceTextureId = -1;// oes 纹理id
    private SurfaceTexture mSurfaceTexture = null;// 渲染TextureId的Surface
    private KFSurfaceTextureListener mListener = null;//回调

    public KFSurfaceTexture(KFSurfaceTextureListener listener) {
        mListener = listener;
        _setupSurfaceTexture();
    }

    public int getSurfaceTextureId() {
        return mSurfaceTextureId;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void release() {
        // 释放纹理id
        if(mSurfaceTextureId != -1){
            GLES20.glDeleteTextures(1,  new int[] {mSurfaceTextureId},0);
            mSurfaceTextureId = -1;
        }
    }

    private void _setupSurfaceTexture() {
        // 初始化OpenGL OES 纹理id
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mSurfaceTextureId = textures[0];
        mSurfaceTexture = new SurfaceTexture(mSurfaceTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mSurfaceTextureId);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // 回调数据
        if(mListener != null){
            mListener.onFrameAvailable(surfaceTexture);
        }
    }
}
