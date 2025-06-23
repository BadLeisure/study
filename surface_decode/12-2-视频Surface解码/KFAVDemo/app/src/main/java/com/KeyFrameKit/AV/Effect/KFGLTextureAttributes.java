package com.KeyFrameKit.AV.Effect;
//
//  KFGLTextureAttributes
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.opengl.GLES20;

public class KFGLTextureAttributes {

    public int minFilter = GLES20.GL_LINEAR;
    public int magFilter = GLES20.GL_LINEAR;
    public int wrapS = GLES20.GL_CLAMP_TO_EDGE;
    public int wrapT = GLES20.GL_CLAMP_TO_EDGE;
    public int internalFormat = GLES20.GL_RGBA;
    public int format = GLES20.GL_RGBA;
    public int type = GLES20.GL_UNSIGNED_BYTE;

    public KFGLTextureAttributes() {

    }
}
