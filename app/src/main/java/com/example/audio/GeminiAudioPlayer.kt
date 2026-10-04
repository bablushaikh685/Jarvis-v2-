package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class GeminiAudioPlayer(private val context: Context) {

  private var mediaPlayer: MediaPlayer? = null
  private var audioTrack: AudioTrack? = null
  private var visualizerJob: Job? = null
  private var playbackJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.Main)

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying = _isPlaying.asStateFlow()

  private val _audioAmplitude = MutableStateFlow(0f)
  val audioAmplitude = _audioAmplitude.asStateFlow()

  private val isInterrupted = AtomicBoolean(false)
  private val lock = Any()

  /**
   * Plays base64 encoded audio string from Gemini (WAV, MP3, or PCM)
   */
  fun playBase64Audio(
    base64Data: String,
    mimeType: String = "audio/wav",
    onComplete: () -> Unit = {}
  ) {
    clearAudioBuffersAndHalt()

    try {
      val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
      if (audioBytes == null || audioBytes.isEmpty()) {
        onComplete()
        return
      }

      if (mimeType.contains("pcm") || mimeType.contains("raw")) {
        playPcmData(audioBytes, 24000, onComplete)
      } else {
        playMediaBytes(audioBytes, onComplete)
      }
    } catch (e: Exception) {
      clearAudioBuffersAndHalt()
      onComplete()
    }
  }

  private fun playMediaBytes(bytes: ByteArray, onComplete: () -> Unit) {
    try {
      val tempFile = File.createTempFile("gemini_voice", ".tmp", context.cacheDir).apply {
        deleteOnExit()
      }
      FileOutputStream(tempFile).use { it.write(bytes) }

      synchronized(lock) {
        isInterrupted.set(false)
        mediaPlayer = MediaPlayer().apply {
          setAudioAttributes(
            AudioAttributes.Builder()
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
              .build()
          )
          setDataSource(tempFile.absolutePath)
          prepare()
          setOnCompletionListener {
            tempFile.delete()
            clearAudioBuffersAndHalt()
            onComplete()
          }
          setOnErrorListener { _, _, _ ->
            tempFile.delete()
            clearAudioBuffersAndHalt()
            onComplete()
            true
          }
          start()
        }
      }

      _isPlaying.value = true
      startVisualizerSimulation()
    } catch (e: Exception) {
      clearAudioBuffersAndHalt()
      onComplete()
    }
  }

  /**
   * Plays PCM stream in responsive, interruptible chunks with instant buffer flush
   */
  fun playPcmData(pcmBytes: ByteArray, sampleRate: Int = 24000, onComplete: () -> Unit) {
    clearAudioBuffersAndHalt()

    val bufferSize = AudioTrack.getMinBufferSize(
      sampleRate,
      AudioFormat.CHANNEL_OUT_MONO,
      AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)

    synchronized(lock) {
      isInterrupted.set(false)
      audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(bufferSize)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

      audioTrack?.play()
    }

    _isPlaying.value = true
    startVisualizerSimulation()

    playbackJob = scope.launch(Dispatchers.IO) {
      try {
        val chunkSize = 2048
        var offset = 0
        while (isActive && !isInterrupted.get() && offset < pcmBytes.size) {
          val remaining = pcmBytes.size - offset
          val currentChunk = remaining.coerceAtMost(chunkSize)

          val written = synchronized(lock) {
            if (isInterrupted.get()) -1
            else audioTrack?.write(pcmBytes, offset, currentChunk) ?: -1
          }

          if (written <= 0) break
          offset += written
        }

        // Graceful completion check
        if (isActive && !isInterrupted.get()) {
          delay(80) // Let final PCM samples drain
        }
      } catch (_: Exception) {
      } finally {
        withContext(Dispatchers.Main) {
          clearAudioBuffersAndHalt()
          onComplete()
        }
      }
    }
  }

  private fun startVisualizerSimulation() {
    visualizerJob?.cancel()
    visualizerJob = scope.launch {
      while (isActive && _isPlaying.value) {
        val t = System.currentTimeMillis() % 1000 / 1000f
        val amp = 0.35f + 0.55f * kotlin.math.sin(t * 2 * kotlin.math.PI).toFloat().coerceAtLeast(0f)
        _audioAmplitude.value = amp
        delay(60)
      }
      _audioAmplitude.value = 0f
    }
  }

  /**
   * Robust State Synchronization & Buffer Clearing:
   * Immediately clears all hardware audio buffers, cancels coroutines, and halts playback.
   */
  fun clearAudioBuffersAndHalt() {
    isInterrupted.set(true)
    visualizerJob?.cancel()
    visualizerJob = null
    playbackJob?.cancel()
    playbackJob = null
    _audioAmplitude.value = 0f
    _isPlaying.value = false

    synchronized(lock) {
      // 1. Immediately pause and flush AudioTrack to clear lingering PCM audio buffer
      try {
        audioTrack?.apply {
          if (playState == AudioTrack.PLAYSTATE_PLAYING) {
            pause()
            flush() // Clears hardware queued samples immediately
          }
          stop()
          release()
        }
      } catch (_: Exception) {}
      audioTrack = null

      // 2. Clear and release MediaPlayer
      try {
        mediaPlayer?.apply {
          if (isPlaying) {
            pause()
            stop()
          }
          reset()
          release()
        }
      } catch (_: Exception) {}
      mediaPlayer = null
    }
  }

  /** Alias for clearAudioBuffersAndHalt */
  fun stopPlayback() = clearAudioBuffersAndHalt()
}
