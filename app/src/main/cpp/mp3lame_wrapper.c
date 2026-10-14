#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>
#include "lame.h"

#define LOG_TAG "MP3Recorder"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global LAME encoder instance
lame_global_flags *lame_gfp = NULL;

// Initialize LAME encoder
JNIEXPORT void JNICALL
Java_com_xyverse_xyrecorder_audio_MP3Encoder_nativeInit(
    JNIEnv *env, jobject obj,
    jint inSamplerate, jint outChannel,
    jint outSamplerate, jint outBitrate, jint quality) {
    
    if (lame_gfp != NULL) {
        lame_close(lame_gfp);
        lame_gfp = NULL;
    }
    
    lame_gfp = lame_init();
    lame_set_in_samplerate(lame_gfp, inSamplerate);
    lame_set_out_samplerate(lame_gfp, outSamplerate);
    lame_set_num_channels(lame_gfp, outChannel);
    lame_set_brate(lame_gfp, outBitrate);
    lame_set_quality(lame_gfp, quality);
    lame_set_mode(lame_gfp, MONO);  // Mono for voice recording
    
    lame_init_params(lame_gfp);
    
    LOGI("LAME initialized: inRate=%d, outRate=%d, channels=%d, bitrate=%d",
         inSamplerate, outSamplerate, outChannel, outBitrate);
}

// Get buffer size needed for MP3 output
JNIEXPORT jint JNICALL
Java_com_xyverse_xyrecorder_audio_MP3Encoder_nativeGetBufferSize(
    JNIEnv *env, jobject obj, jint pcmSamples) {
    return (jint)(1.25 * pcmSamples + 7200);
}

// Encode PCM data to MP3
JNIEXPORT jint JNICALL
Java_com_xyverse_xyrecorder_audio_MP3Encoder_nativeEncode(
    JNIEnv *env, jobject obj,
    jshortArray pcmBuffer, jint pcmSamples,
    jbyteArray mp3Buffer) {
    
    jshort* pcmData = (*env)->GetShortArrayElements(env, pcmBuffer, 0);
    jbyte* mp3Data = (*env)->GetByteArrayElements(env, mp3Buffer, 0);
    
    jint mp3Size = lame_encode_buffer(
        lame_gfp,
        pcmData,  // left channel
        pcmData,  // right channel (same as left for mono)
        pcmSamples,
        (unsigned char*)mp3Data,
        (*env)->GetArrayLength(env, mp3Buffer)
    );
    
    (*env)->ReleaseShortArrayElements(env, pcmBuffer, pcmData, 0);
    (*env)->ReleaseByteArrayElements(env, mp3Buffer, mp3Data, 0);
    
    if (mp3Size < 0) {
        LOGE("MP3 encoding failed: %d", mp3Size);
        return -1;
    }
    
    return mp3Size;
}

// Flush remaining MP3 data
JNIEXPORT jint JNICALL
Java_com_xyverse_xyrecorder_audio_MP3Encoder_nativeFlush(
    JNIEnv *env, jobject obj,
    jbyteArray mp3Buffer) {
    
    jbyte* mp3Data = (*env)->GetByteArrayElements(env, mp3Buffer, 0);
    
    jint mp3Size = lame_encode_flush(
        lame_gfp,
        (unsigned char*)mp3Data,
        (*env)->GetArrayLength(env, mp3Buffer)
    );
    
    (*env)->ReleaseByteArrayElements(env, mp3Buffer, mp3Data, 0);
    
    return mp3Size;
}

// Close LAME encoder
JNIEXPORT void JNICALL
Java_com_xyverse_xyrecorder_audio_MP3Encoder_nativeClose(
    JNIEnv *env, jobject obj) {
    
    if (lame_gfp != NULL) {
        lame_close(lame_gfp);
        lame_gfp = NULL;
        LOGI("LAME closed");
    }
}
