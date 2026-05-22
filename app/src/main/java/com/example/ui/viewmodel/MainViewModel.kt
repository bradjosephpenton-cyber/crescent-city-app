package com.example.ui.viewmodel

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BeatRepository
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var visualizerJob: Job? = null

    // Hardcoded high-resolution tracks derived directly from Crescent City Instrumentals catalog description
    val tracks = listOf(
        Track(
            id = "15257038",
            title = "You Are My Lady 2",
            durationText = "3:04",
            durationMs = 184000,
            description = "Standout hip-hop production featuring radiant gold brass elements, smooth R&B melodies, and driving trap hats. Signature New Orleans dancefloor energy.",
            streamUrl = "https://assets.mixkit.co/music/preview/mixkit-hip-hop-02-738.mp3",
            imageUrl = "https://cloudimages.soundclick.com/146/images/c/band/crescentcityinstrumentals.webp",
            datePublished = "May 19, 2026",
            price = 25.00,
            tags = listOf("Hip-Hop", "Smooth", "East Coast", "Radiant")
        ),
        Track(
            id = "15256644",
            title = "Cement",
            durationText = "2:15",
            durationMs = 135000,
            description = "Relentless in energy and raw in tone. Heavy New Orleans bounce rhythms blended with modern trap beats and sub-sub basslines.",
            streamUrl = "https://assets.mixkit.co/music/preview/mixkit-urban-hip-hop-744.mp3",
            imageUrl = "https://cloudimages.soundclick.com/420/pro/album/1/1337361_32382.webp",
            datePublished = "May 18, 2026",
            price = 25.00,
            tags = listOf("Bounce", "Trap", "West Coast", "Energy")
        ),
        Track(
            id = "15252894",
            title = "Way Of Life 504 Edition",
            durationText = "4:02",
            durationMs = 242000,
            description = "Slogan beat inspired by New Orleans' legendary 504 area code. Charted at #18 in Hip-Hop General. Produced in the classic vein of Hott Kizzle On The Track.",
            streamUrl = "https://assets.mixkit.co/music/preview/mixkit-complex-hip-hop-757.mp3",
            imageUrl = "https://cloudimages.soundclick.com/439/images/c/song/crescentcityinstrumentals%2Bwayoflife504edition2.webp",
            datePublished = "May 18, 2026",
            price = 25.00,
            tags = listOf("Charted", "504 Vibes", "New Orleans", "Brass")
        ),
        Track(
            id = "15256616",
            title = "Gutta Anthem (Mastered)",
            durationText = "3:12",
            durationMs = 192000,
            description = "A banger street anthem with relentless energy. High-pitch string loops, heavy 808 glide kicks, and industrial brass drops.",
            streamUrl = "https://assets.mixkit.co/music/preview/mixkit-street-hip-hop-759.mp3",
            imageUrl = "https://cloudimages.soundclick.com/208/pro/album/1/1337361_1225.webp",
            datePublished = "May 15, 2026",
            price = 25.00,
            tags = listOf("Street", "Banger", "Anthem", "Hard")
        ),
        Track(
            id = "15256416",
            title = "This Ain't Life Too",
            durationText = "2:45",
            durationMs = 165000,
            description = "Reflective, beautiful hip-hop orchestration. Rich organic pianos, soft woodwind samples, and soul vocal chops.",
            streamUrl = "https://assets.mixkit.co/music/preview/mixkit-morning-breeze-725.mp3",
            imageUrl = "https://cloudimages.soundclick.com/816/pro/album/1/1337361_21068.jpg",
            datePublished = "May 12, 2026",
            price = 25.00,
            tags = listOf("Deep", "Soulful", "Pianos", "Vocal Chops")
        )
    )

    // Room Database Observables
    val favoritesFlow: StateFlow<List<FavoriteTrack>>
    val estimatesFlow: StateFlow<List<SavedEstimate>>

    // Audio Playback Streams & States
    private val _currentTrack = MutableStateFlow<Track>(tracks[0])
    val currentTrack: StateFlow<Track> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f) // 0.0 to 1.0f
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _currentMillis = MutableStateFlow(0L)
    val currentMillis: StateFlow<Long> = _currentMillis.asStateFlow()

    private val _isLoadingTrack = MutableStateFlow(false)
    val isLoadingTrack: StateFlow<Boolean> = _isLoadingTrack.asStateFlow()

    // 8-Bar Waveform Simulation state
    private val _visualizerBars = MutableStateFlow(List(8) { 0.15f })
    val visualizerBars: StateFlow<List<Float>> = _visualizerBars.asStateFlow()

    init {
        val database = BeatDatabase.getDatabase(application)
        repository = BeatRepository(database.beatDao())

        favoritesFlow = repository.allFavorites
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        estimatesFlow = repository.allEstimates
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        setupMediaPlayer()
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnCompletionListener {
                playNextTrack()
            }
            setOnErrorListener { mp, what, extra ->
                Log.e("CrescentPlayer", "MediaPlayer Error: what=$what, extra=$extra")
                _isPlaying.value = false
                _isLoadingTrack.value = false
                stopJobs()
                true
            }
        }
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            try {
                if (_currentTrack.value.id == track.id && mediaPlayer != null) {
                    // Toggle play/pause for the same track
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                        _isPlaying.value = false
                        stopJobs()
                    } else {
                        mediaPlayer?.start()
                        _isPlaying.value = true
                        startProgressJob()
                        startVisualizerSimulation()
                    }
                    return@launch
                }

                // Prepare and load a new track
                _isLoadingTrack.value = true
                _isPlaying.value = false
                stopJobs()

                _currentTrack.value = track
                _playbackProgress.value = 0f
                _currentMillis.value = 0L

                mediaPlayer?.reset()
                mediaPlayer?.setDataSource(track.streamUrl)
                
                // Keep it non-blocking on main thread using prepareAsync
                mediaPlayer?.setOnPreparedListener { mp ->
                    _isLoadingTrack.value = false
                    mp.start()
                    _isPlaying.value = true
                    startProgressJob()
                    startVisualizerSimulation()
                }
                mediaPlayer?.prepareAsync()

            } catch (e: Exception) {
                _isLoadingTrack.value = false
                Log.e("CrescentPlayer", "Error playing track: ${e.message}")
            }
        }
    }

    fun togglePlayPause() {
        playTrack(_currentTrack.value)
    }

    fun seekToFraction(fraction: Float) {
        val player = mediaPlayer ?: return
        val current = _currentTrack.value
        if (_isPlaying.value || player.isPlaying) {
            val progressMillis = (fraction * player.duration).toInt()
            player.seekTo(progressMillis)
            _playbackProgress.value = fraction
            _currentMillis.value = progressMillis.toLong()
        }
    }

    fun playNextTrack() {
        val currentIndex = tracks.indexOfFirst { it.id == _currentTrack.value.id }
        if (currentIndex != -1) {
            val nextIndex = (currentIndex + 1) % tracks.size
            playTrack(tracks[nextIndex])
        }
    }

    fun playPreviousTrack() {
        val currentIndex = tracks.indexOfFirst { it.id == _currentTrack.value.id }
        if (currentIndex != -1) {
            val prevIndex = if (currentIndex - 1 < 0) tracks.size - 1 else currentIndex - 1
            playTrack(tracks[prevIndex])
        }
    }

    // Toggle favorite track in Room database
    fun toggleFavorite(trackId: String) {
        viewModelScope.launch {
            val list = favoritesFlow.value
            val isFavorite = list.any { it.id == trackId }
            if (isFavorite) {
                repository.removeFavorite(trackId)
            } else {
                repository.addFavorite(trackId)
            }
        }
    }

    // Licensing custom quotes / estimate management
    fun saveLicensingEstimate(
        trackId: String,
        trackName: String,
        licenseType: String,
        basePrice: Double,
        vocals: Boolean,
        radio: Boolean,
        musicVideo: Boolean,
        total: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val estimate = SavedEstimate(
                trackId = trackId,
                trackName = trackName,
                licenseType = licenseType,
                basePrice = basePrice,
                vocalRecordingAddon = vocals,
                radioPlacementsAddon = radio,
                musicVideoAddon = musicVideo,
                totalEstimate = total,
                notes = notes
            )
            repository.saveEstimate(estimate)
        }
    }

    fun deleteEstimate(estimate: SavedEstimate) {
        viewModelScope.launch {
            repository.deleteEstimate(estimate)
        }
    }

    private fun startProgressJob() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (_isPlaying.value) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    val duration = player.duration.toFloat()
                    if (duration > 0) {
                        val current = player.currentPosition.toFloat()
                        _playbackProgress.value = current / duration
                        _currentMillis.value = player.currentPosition.toLong()
                    }
                }
                delay(200)
            }
        }
    }

    private fun startVisualizerSimulation() {
        visualizerJob?.cancel()
        visualizerJob = viewModelScope.launch {
            while (_isPlaying.value) {
                // Fluctuates active audio visualizer bars smoothly
                _visualizerBars.value = List(8) {
                    Random.nextFloat().coerceIn(0.2f, 1.0f)
                }
                delay(120)
            }
        }
    }

    private fun stopJobs() {
        progressJob?.cancel()
        visualizerJob?.cancel()
        _visualizerBars.value = List(8) { 0.15f }
    }

    override fun onCleared() {
        super.onCleared()
        stopJobs()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
