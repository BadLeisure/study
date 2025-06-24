package com.KeyFrameKit.AV.MediaCodec;
//
//  KFVideoSurfaceDecoder
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.KeyFrameKit.AV.Base.KFBufferFrame;
import com.KeyFrameKit.AV.Base.KFFrame;
import com.KeyFrameKit.AV.Base.KFGLBase;
import com.KeyFrameKit.AV.Base.KFTextureFrame;
import com.KeyFrameKit.AV.Effect.KFGLContext;
import com.KeyFrameKit.AV.Effect.KFGLFilter;
import com.KeyFrameKit.AV.Effect.KFSurfaceTexture;
import com.KeyFrameKit.AV.Effect.KFSurfaceTextureListener;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class KFVideoSurfaceDecoder implements  KFMediaCodecInterface {
    private static final String TAG = "KFVideoSurfaceDecoder";
    private KFMediaCodecListener mListener = null;// 回调
    private MediaCodec mDecoder = null;// 解码器
    private ByteBuffer[] mInputBuffers;// 解码器输入缓存
    private MediaFormat mInputMediaFormat = null;// 输入格式描述
    private MediaFormat mOutMediaFormat = null;// 输出格式描述
    private KFGLContext mEGLContext = null;// OpenGL上下文
    private KFSurfaceTexture mSurfaceTexture = null;// 纹理缓存
    private Surface mSurface = null;// 纹理缓存 对应 Surface
    private KFGLFilter mOESConvert2DFilter;///< 特效

    private long mLastInputPts = 0;// 输入数据最后一帧时间戳
    private List<KFBufferFrame> mList = new ArrayList<>();
    private ReentrantLock mListLock = new ReentrantLock(true);

    private HandlerThread mDecoderThread = null;// 解码线程
    private Handler mDecoderHandler = null;
    private HandlerThread mRenderThread = null;// 渲染线程
    private Handler mRenderHandler = null;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());// 主线程

    public KFVideoSurfaceDecoder() {

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setup(boolean isEncoder,MediaFormat mediaFormat,KFMediaCodecListener listener, EGLContext eglShareContext) {
        mInputMediaFormat = mediaFormat;
        mListener = listener;

        // 创建解码线程
        mDecoderThread = new HandlerThread("KFVideoSurfaceDecoderThread");
        mDecoderThread.start();
        mDecoderHandler = new Handler((mDecoderThread.getLooper()));

        // 创建渲染线程
        mRenderThread = new HandlerThread("KFVideoSurfaceRenderThread");
        mRenderThread.start();
        mRenderHandler = new Handler((mRenderThread.getLooper()));

        mDecoderHandler.post(()->{
            if(mInputMediaFormat == null){
                _callBackError(KFMediaCodecInterfaceErrorParams,"mInputMediaFormat null");
                return;
            }

            // 创建OpenGL 上下文、纹理缓存、纹理缓存Surface、OES转2D数据
            mEGLContext = new KFGLContext(eglShareContext);
            mEGLContext.bind();
            mSurfaceTexture = new KFSurfaceTexture(mSurfaceTextureListener);
            mSurfaceTexture.getSurfaceTexture().setDefaultBufferSize(mInputMediaFormat.getInteger(MediaFormat.KEY_WIDTH),mInputMediaFormat.getInteger(MediaFormat.KEY_HEIGHT));
            mSurface = new Surface(mSurfaceTexture.getSurfaceTexture());
            mOESConvert2DFilter = new KFGLFilter(false, KFGLBase.defaultVertexShader,KFGLBase.oesFragmentShader);
            mEGLContext.unbind();

            _setupDecoder();
        });
    }

    @Override
    public MediaFormat getOutputMediaFormat() {
        return mOutMediaFormat;
    }

    @Override
    public MediaFormat getInputMediaFormat() {
        return mInputMediaFormat;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void release() {
        mDecoderHandler.post(()-> {
            // 释放解码器、GL上下文、数据缓存、SurfaceTexture
            if(mDecoder != null){
                try {
                    mDecoder.stop();
                    mDecoder.release();
                } catch (Exception e) {
                    Log.e(TAG, "release: " + e.toString());
                }
                mDecoder = null;
            }

            mEGLContext.bind();
            if(mSurfaceTexture != null){
                mSurfaceTexture.release();
                mSurfaceTexture = null;
            }
            if(mSurface != null){
                mSurface.release();
                mSurface = null;
            }
            if(mOESConvert2DFilter != null){
                mOESConvert2DFilter.release();
                mOESConvert2DFilter = null;
            }
            mEGLContext.unbind();

            if(mEGLContext != null){
                mEGLContext.release();
                mEGLContext = null;
            }

            mListLock.lock();
            mList.clear();
            mListLock.unlock();

            mDecoderThread.quit();
            mRenderThread.quit();
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void flush() {
        mDecoderHandler.post(()-> {
            // 刷新解码器缓冲区
            if(mDecoder == null){
                return;
            }

            try {
                mDecoder.flush();
            } catch (Exception e) {
                Log.e(TAG, "flush" + e);
            }

            mListLock.lock();
            mList.clear();
            mListLock.unlock();
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int processFrame(KFFrame inputFrame) {
        if(inputFrame == null){
            return KFMediaCodeProcessParams;
        }

        KFBufferFrame frame = (KFBufferFrame)inputFrame;
        if(frame.buffer ==null || frame.bufferInfo == null || frame.bufferInfo.size == 0){
            return KFMediaCodeProcessParams;
        }

        // 外层数据进入缓存
        _appendFrame(frame);

        mDecoderHandler.post(()-> {
            if(mDecoder == null){
                return;
            }

            // 缓存获取数据，尽量多的输入给解码器
            mListLock.lock();
            int mListSize = mList.size();
            mListLock.unlock();
            while (mListSize > 0){
                mListLock.lock();
                KFBufferFrame packet = mList.get(0);
                mListLock.unlock();

                int bufferIndex;
                try {
                    // 获取解码器输入缓存下标
                    bufferIndex = mDecoder.dequeueInputBuffer(10 * 1000);
                } catch (Exception e) {
                    Log.e(TAG, "dequeueInputBuffer" + e);
                    return;
                }

                if(bufferIndex >= 0){
                    // 填充数据
                    mInputBuffers[bufferIndex].clear();
                    mInputBuffers[bufferIndex].put(packet.buffer);
                    mInputBuffers[bufferIndex].flip();
                    try {
                        // 数据塞入解码器
                        mDecoder.queueInputBuffer(bufferIndex, 0, packet.bufferInfo.size, packet.bufferInfo.presentationTimeUs, packet.bufferInfo.flags);
                    } catch (Exception e) {
                        Log.e(TAG, "queueInputBuffer" + e);
                        return;
                    }

                    mLastInputPts = packet.bufferInfo.presentationTimeUs;
                    mListLock.lock();
                    mList.remove(0);
                    mListSize = mList.size();
                    mListLock.unlock();
                }else{
                    break;
                }
            }

            // 从解码器拉取尽量多的数据出来
            long outputDts = -1;
            MediaCodec.BufferInfo outputBufferInfo = new MediaCodec.BufferInfo();
            while (outputDts < mLastInputPts) {
                int bufferIndex;
                try {
                    // 获取解码器输出缓存下标
                    bufferIndex = mDecoder.dequeueOutputBuffer(outputBufferInfo, 10 * 1000);
                } catch (Exception e) {
                    Log.e(TAG, "dequeueOutputBuffer" + e);
                    return;
                }

                if(bufferIndex >= 0){
                    // 释放缓存，第二个参数必须设置位true，这样数据刷新到指定surface
                    mDecoder.releaseOutputBuffer(bufferIndex,true);
                }else{
                    if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        mOutMediaFormat = mDecoder.getOutputFormat();
                    }
                    break;
                }
            }
        });

        return KFMediaCodeProcessSuccess;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void _appendFrame(KFBufferFrame frame) {
        // 添加数据到缓存List
        KFBufferFrame packet = new KFBufferFrame();

        ByteBuffer newBuffer = ByteBuffer.allocateDirect(frame.bufferInfo.size);
        newBuffer.put(frame.buffer).position(0);
        MediaCodec.BufferInfo newInfo = new MediaCodec.BufferInfo();
        newInfo.size = frame.bufferInfo.size;
        newInfo.flags = frame.bufferInfo.flags;
        newInfo.presentationTimeUs = frame.bufferInfo.presentationTimeUs;
        packet.buffer = newBuffer;
        packet.bufferInfo = newInfo;

        mListLock.lock();
        mList.add(packet);
        mListLock.unlock();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean _setupDecoder() {
        // 初始化解码器
        try {
            // 根据输入格式描述创建解码器
            String mimetype = mInputMediaFormat.getString(MediaFormat.KEY_MIME);
            mDecoder = MediaCodec.createDecoderByType(mimetype);
        }catch (Exception e) {
            Log.e(TAG, "createDecoderByType" + e);
            _callBackError(KFMediaCodecInterfaceErrorCreate,e.getMessage());
            return false;
        }

        try {
            // 配置位Surface 解码模式
            mDecoder.configure(mInputMediaFormat, mSurface, null, 0);
        }catch (Exception e) {
            Log.e(TAG, "configure" + e);
            _callBackError(KFMediaCodecInterfaceErrorConfigure,e.getMessage());
            return false;
        }

        try {
            // 启动解码器
            mDecoder.start();
            // 获取解码器输入缓存
            mInputBuffers = mDecoder.getInputBuffers();
        }catch (Exception e) {
            Log.e(TAG, "start" +  e );
            _callBackError(KFMediaCodecInterfaceErrorStart,e.getMessage());
            return false;
        }

        return true;
    }

    private void _callBackError(int error, String errorMsg){
        // 错误回调
        if(mListener != null){
            mMainHandler.post(()->{
                mListener.onError(error,TAG + errorMsg);
            });
        }
    }

    private KFSurfaceTextureListener mSurfaceTextureListener = new KFSurfaceTextureListener() {
        // SurfaceTexture 数据回调
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mRenderHandler.post(() -> {
                mEGLContext.bind();
                mSurfaceTexture.getSurfaceTexture().updateTexImage();
                if(mListener != null){
                    int width = mInputMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                    int height = mInputMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    int rotation = (mInputMediaFormat.getInteger(MediaFormat.KEY_ROTATION) + 360) % 360;
                    int rotationWidth = (rotation % 360 == 90 || rotation % 360 == 270) ? height : width;
                    int rotationHeight = (rotation % 360 == 90 || rotation % 360 == 270) ? width : height;
                    KFTextureFrame frame = new KFTextureFrame(mSurfaceTexture.getSurfaceTextureId(),new Size(rotationWidth,rotationHeight),mSurfaceTexture.getSurfaceTexture().getTimestamp() * 1000,true);
                    mSurfaceTexture.getSurfaceTexture().getTransformMatrix(frame.textureMatrix);
                    // OES 数据转换2D
                    KFFrame convertFrame = mOESConvert2DFilter.render(frame);
                    mListener.dataOnAvailable(convertFrame);
                }
                mEGLContext.unbind();
            });
        }
    };
}
