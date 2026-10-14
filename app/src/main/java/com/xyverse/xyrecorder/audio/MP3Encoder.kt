package com.xyverse.xyrecorder.audio

/**
 * JNI wrapper for LAME MP3 encoder
 */
class MP3Encoder {
    
    companion object {
        init {
            System.loadLibrary("mp3lame_wrapper")
        }
    }
    
    /**
     * Initialize the LAME encoder
     * @param inSampleRate Input PCM sample rate (e.g., 44100, 48000)
     * @param outChannel Number of output channels (1 for mono, 2 for stereo)
     * @param outSampleRate Output MP3 sample rate
     * @param outBitrate Output MP3 bitrate in kbps (e.g., 128, 192, 320)
     * @param quality Quality setting (0=best, 9=worst, 2 recommended)
     */
    external fun nativeInit(
        inSampleRate: Int,
        outChannel: Int,
        outSampleRate: Int,
        outBitrate: Int,
        quality: Int
    )
    
    /**
     * Get required buffer size for MP3 encoding
     * @param pcmSamples Number of PCM samples to encode
     * @return Required buffer size in bytes
     */
    external fun nativeGetBufferSize(pcmSamples: Int): Int
    
    /**
     * Encode PCM data to MP3
     * @param pcmBuffer PCM data buffer (16-bit signed samples)
     * @param pcmSamples Number of samples to encode
     * @param mp3Buffer Output buffer for MP3 data
     * @return Number of bytes written to mp3Buffer, or -1 on error
     */
    external fun nativeEncode(
        pcmBuffer: ShortArray,
        pcmSamples: Int,
        mp3Buffer: ByteArray
    ): Int
    
    /**
     * Flush any remaining encoded data
     * @param mp3Buffer Output buffer for remaining MP3 data
     * @return Number of bytes written to mp3Buffer
     */
    external fun nativeFlush(mp3Buffer: ByteArray): Int
    
    /**
     * Close the encoder and free resources
     */
    external fun nativeClose()
    
    /**
     * Initialize encoder with default settings
     */
    fun init(
        sampleRate: Int = 44100,
        channels: Int = 1,
        bitRate: Int = 128
    ) {
        nativeInit(sampleRate, channels, sampleRate, bitRate, 2)
    }
    
    /**
     * Get buffer size needed for encoding
     */
    fun getBufferSize(samplesCount: Int): Int {
        return nativeGetBufferSize(samplesCount)
    }
    
    /**
     * Encode PCM to MP3
     */
    fun encode(pcmBuffer: ShortArray, samplesCount: Int, mp3Buffer: ByteArray): Int {
        return nativeEncode(pcmBuffer, samplesCount, mp3Buffer)
    }
    
    /**
     * Flush remaining data
     */
    fun flush(mp3Buffer: ByteArray): Int {
        return nativeFlush(mp3Buffer)
    }
    
    /**
     * Close encoder
     */
    fun close() {
        nativeClose()
    }
}
