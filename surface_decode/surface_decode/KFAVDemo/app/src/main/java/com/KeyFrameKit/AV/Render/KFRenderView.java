package com.KeyFrameKit.AV.Render;
//
//  KFRenderView
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.content.Context;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.KeyFrameKit.AV.Base.KFFrame;
import com.KeyFrameKit.AV.Base.KFGLBase;
import com.KeyFrameKit.AV.Base.KFTextureFrame;
import com.KeyFrameKit.AV.Effect.KFGLContext;
import com.KeyFrameKit.AV.Effect.KFGLFilter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class KFRenderView extends ViewGroup {
    private KFGLContext mEGLContext = null;// OpenGL上下文
    private KFGLFilter mFilter = null;// 特效渲染到指定Surface
    private EGLContext mShareContext = null;// 共享上下文
    private View mRenderView = null;// 渲染视图基类
    private int mSurfaceWidth = 0;// 渲染缓存宽
    private int mSurfaceHeight = 0;// 渲染缓存高
    private FloatBuffer mSquareVerticesBuffer = null;// 自定义顶点
    private KFRenderMode mRenderMode = KFRenderMode.KFRenderModeFill;// 自适应模式 黑边 比例填冲
    private boolean mSurfaceChanged = false;// 渲染缓存是否变更
    private Size mLastRenderSize = new Size(0,0);// 标记上次渲染Size

    public enum KFRenderMode {
        KFRenderStretch,// 拉伸满-可能变形
        KFRenderModeFit,// 黑边
        KFRenderModeFill// 比例填充
    };

    public KFRenderView(Context context, EGLContext eglContext){
        super(context);
        mShareContext = eglContext;// 共享上下文
        _setupSquareVertices();// 初始化顶点

        boolean isSurfaceView  = false;// TextureView 与 SurfaceView 开关
        if(isSurfaceView){
            mRenderView = new KFSurfaceView(context, mListener);
        }else{
            mRenderView = new KFTextureView(context, mListener);
        }

        this.addView(mRenderView);// 添加视图到父视图
    }

    public void release() {
        // 释放GL上下文、特效
        if(mEGLContext != null){
            mEGLContext.bind();
            if(mFilter != null){
                mFilter.release();
                mFilter = null;
            }
            mEGLContext.unbind();

            mEGLContext.release();
            mEGLContext = null;
        }
    }

    public void render(KFTextureFrame inputFrame){
        if(inputFrame == null){
            return;
        }

        //输入纹理使用自定义特效渲染到View 的 Surface上
        if(mEGLContext != null && mFilter != null){
            boolean frameResolutionChanged = inputFrame.textureSize.getWidth() != mLastRenderSize.getWidth() || inputFrame.textureSize.getHeight() != mLastRenderSize.getHeight();
            // 渲染缓存变更或者视图大小变更重新设置顶点
            if(mSurfaceChanged || frameResolutionChanged){
                _recalculateVertices(inputFrame.textureSize);
                mSurfaceChanged = false;
                mLastRenderSize = inputFrame.textureSize;
            }

            // 渲染到指定Surface
            mEGLContext.bind();
            mFilter.setSquareVerticesBuffer(mSquareVerticesBuffer);
            GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
            mFilter.render(inputFrame);
            mEGLContext.swapBuffers();
            mEGLContext.unbind();
        }
    }

    private KFRenderListener mListener = new KFRenderListener() {
        @Override
        // 渲染缓存创建
        public void surfaceCreate(@NonNull Surface surface) {
            mEGLContext = new KFGLContext(mShareContext,surface);
            // 初始化特效
            mEGLContext.bind();
            _setupFilter();
            mEGLContext.unbind();
        }

        @Override
        // 渲染缓存变更
        public void surfaceChanged(@NonNull Surface surface, int width, int height) {
            mSurfaceWidth = width;
            mSurfaceHeight = height;
            mSurfaceChanged = true;
            // 设置GL上下文Surface
            mEGLContext.bind();
            mEGLContext.setSurface(surface);
            mEGLContext.unbind();
        }

        @Override
        public void surfaceDestroy(@NonNull Surface surface) {

        }
    };

    private void _setupFilter() {
        // 初始化特效
        if(mFilter == null){
            mFilter = new KFGLFilter(true, KFGLBase.defaultVertexShader,KFGLBase.defaultFragmentShader);
        }
    }

    private void _setupSquareVertices() {
        // 初始化顶点缓存
        final float squareVertices[] = {
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f,  1.0f,
                1.0f,  1.0f,
        };

        ByteBuffer squareVerticesByteBuffer = ByteBuffer.allocateDirect(4 * squareVertices.length);
        squareVerticesByteBuffer.order(ByteOrder.nativeOrder());
        mSquareVerticesBuffer = squareVerticesByteBuffer.asFloatBuffer();
        mSquareVerticesBuffer.put(squareVertices);
        mSquareVerticesBuffer.position(0);
    }

    private void _recalculateVertices(Size inputImageSize){
        // 按照适应模式创建顶点
        if(mSurfaceWidth == 0 || mSurfaceHeight == 0){
            return;
        }

        Size renderSize = new Size(mSurfaceWidth,mSurfaceHeight);
        float heightScaling = 1, widthScaling = 1;
        Size insetSize = new Size(0,0);
        float inputAspectRatio = (float) inputImageSize.getWidth() / (float)inputImageSize.getHeight();
        float outputAspectRatio = (float)renderSize.getWidth() / (float)renderSize.getHeight();
        boolean isAutomaticHeight = inputAspectRatio <= outputAspectRatio ? false : true;

        if (isAutomaticHeight) {
            float insetSizeHeight = (float)inputImageSize.getHeight() / ((float)inputImageSize.getWidth() / (float)renderSize.getWidth());
            insetSize = new Size(renderSize.getWidth(),(int)insetSizeHeight);
        } else {
            float insetSizeWidth = (float)inputImageSize.getWidth() / ((float)inputImageSize.getHeight() / (float)renderSize.getHeight());
            insetSize = new Size((int)insetSizeWidth,renderSize.getHeight());
        }

        switch (mRenderMode) {
            case KFRenderStretch: {
                widthScaling = 1;
                heightScaling = 1;
            }; break;
            case KFRenderModeFit: {
                widthScaling = (float)insetSize.getWidth() / (float)renderSize.getWidth();
                heightScaling = (float)insetSize.getHeight() / (float)renderSize.getHeight();
            }; break;
            case KFRenderModeFill: {
                widthScaling = (float) renderSize.getHeight() / (float)insetSize.getHeight();
                heightScaling = (float)renderSize.getWidth() / (float)insetSize.getWidth();
            }; break;
        }

        final float squareVertices[] = {
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f,  1.0f,
                1.0f,  1.0f,
        };

        final float customVertices[] = {
                -widthScaling, -heightScaling,
                widthScaling, -heightScaling,
                -widthScaling,  heightScaling,
                widthScaling,  heightScaling,
        };
        ByteBuffer squareVerticesByteBuffer = ByteBuffer.allocateDirect(4 * customVertices.length);
        squareVerticesByteBuffer.order(ByteOrder.nativeOrder());
        mSquareVerticesBuffer = squareVerticesByteBuffer.asFloatBuffer();
        mSquareVerticesBuffer.put(customVertices);
        mSquareVerticesBuffer.position(0);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // 视图变更Size
        this.mRenderView.layout(left,top,right,bottom);
    }
}
