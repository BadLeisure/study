package com.KeyFrameKit.AV.Base;

import android.os.Build;
import android.util.Size;

import androidx.annotation.RequiresApi;

import static com.KeyFrameKit.AV.Base.KFFrame.KFFrameType.KFFrameTexture;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class KFTextureFrame extends KFFrame {
    public int textureId = -1;
    public Size textureSize = new Size(0,0);
    public long nanoTime = 0;
    public boolean isOESTexture = false;
    public float[] textureMatrix = KFGLBase.KFIdentityMatrix();
    public float[] positionMatrix = KFGLBase.KFIdentityMatrix();
    public boolean isEnd = false;

    public KFTextureFrame(int texture,Size size,long nanoTimeStamp){
        super(KFFrameTexture);
        textureId = texture;
        textureSize = size;
        nanoTime = nanoTimeStamp;
    }

    public KFTextureFrame(int texture,Size size,long nanoTimeStamp, boolean isOES){
        super(KFFrameTexture);
        textureId = texture;
        textureSize = size;
        nanoTime = nanoTimeStamp;
        isOESTexture = isOES;
    }

    public KFTextureFrame(int texture,Size size,long nanoTimeStamp, boolean isOES,final float[] texMatrix){
        super(KFFrameTexture);
        textureId = texture;
        textureSize = size;
        nanoTime = nanoTimeStamp;
        isOESTexture = isOES;
        textureMatrix = texMatrix;
    }

    public KFTextureFrame(int texture,Size size,long nanoTimeStamp, boolean isOES,final float[] texMatrix,final float[] posMatrix){
        super(KFFrameTexture);
        textureId = texture;
        textureSize = size;
        nanoTime = nanoTimeStamp;
        isOESTexture = isOES;
        textureMatrix = texMatrix;
        positionMatrix = posMatrix;
    }

    public KFTextureFrame(KFTextureFrame inputFrame) {
        super(KFFrameTexture);
        textureId = inputFrame.textureId;
        textureSize = inputFrame.textureSize;
        nanoTime = inputFrame.nanoTime;
        isOESTexture = inputFrame.isOESTexture;
        textureMatrix = inputFrame.textureMatrix;
    }

    public KFFrameType frameType() {
        return KFFrameTexture;
    }

    public long usTime() {
        return nanoTime / 1000;
    }

    public long msTime() {
        return nanoTime / 1000000;
    }
}
