package com.KeyFrameKit.AV.Effect;
//
//  KFGLFrameBuffer
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.opengl.GLES20;
import android.os.Build;
import android.util.Size;

import androidx.annotation.RequiresApi;

import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glCheckFramebufferStatus;
import static android.opengl.GLES20.glDeleteFramebuffers;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetIntegerv;
import static android.opengl.GLES20.glPixelStorei;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glViewport;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class KFGLFrameBuffer {

    private int mTextureId = -1;// 纹理id
    private int mFboId = -1;// FBO（帧缓存） id
    private KFGLTextureAttributes mTextureAttributes = null;// 纹理格式描述
    private Size mSize;// 纹理对应的Size
    private int mLastFboId = -1;// 上一次绑定的FBO（帧缓存） id


    public KFGLFrameBuffer(Size size){
        mTextureAttributes = new KFGLTextureAttributes();
        mSize = size;
        // 创建纹理 帧缓存
        _setup();
    }

    public KFGLFrameBuffer(Size size,KFGLTextureAttributes textureAttributes){
        mTextureAttributes = textureAttributes != null ? textureAttributes : new KFGLTextureAttributes();
        mSize = size;
        // 创建纹理 帧缓存
        _setup();
    }

    public void release() {
        // 释放纹理 帧缓存
        if(mTextureId != -1){
            glDeleteTextures(1,  new int[] {mTextureId},0);
            mTextureId = -1;
        }

        if (mFboId != -1) {
            glDeleteFramebuffers(1, new int[] {mFboId},0);
            mFboId = -1;
        }
    }

    public Size getSize() {
        return mSize;
    }

    public int getTextureId() {
        return mTextureId;
    }

    public void bind() {
        // 绑定当前帧缓存、设置视口大小
        glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, new int[] {mLastFboId},0);
        if (mFboId != -1) {
            glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId);
            glViewport(0, 0, mSize.getWidth(), mSize.getHeight());
        }
    }

    public void unbind() {
        // 绑定恢复帧缓存
        glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mLastFboId);
    }

    private void _setup() {
        // 初始化纹理
        _setupTexture();
        // 初始化帧缓存
        _setupFrameBuffer();
        // 绑定纹理至帧缓存
        _bindTexture2FrameBuffer();
    }

    private void _setupTexture() {
        // 初始化纹理
        if(mTextureId == -1){
            int[] textures = new int[1];
            glGenTextures(1, textures, 0);
            mTextureId = textures[0];
            glActiveTexture(GLES20.GL_TEXTURE0);
            glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
            glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, mTextureAttributes.minFilter);
            glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, mTextureAttributes.magFilter);
            glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, mTextureAttributes.wrapS);
            glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, mTextureAttributes.wrapT);
            // 设置对其字节数
            if (mSize.getWidth() % 4 != 0) {
                glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
            }
            glTexImage2D(GLES20.GL_TEXTURE_2D, 0, mTextureAttributes.internalFormat, mSize.getWidth(), mSize.getHeight(), 0, mTextureAttributes.format, mTextureAttributes.type, null);
            glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }
    }

    private void _setupFrameBuffer() {
        // 初始化帧缓存
        if (mFboId == -1) {
            int[] fbos = new int[1];
            glGenFramebuffers(1, fbos,0);
            mFboId = fbos[0];
        }
    }

    private void _bindTexture2FrameBuffer() {
        // 绑定纹理至帧缓存，帧缓存可以挂载不同的附件，这里挂载纹理，渲染到帧缓存的数据会自动同步到纹理
        if (mFboId != -1 && mTextureId != -1 && mSize.getWidth() != 0 && mSize.getHeight() != 0) {
            glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId);
            glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mTextureId, 0);
            // 检查绑定状态
            if (glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("glCheckFramebufferStatus()");
            }
            glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
    }
}
