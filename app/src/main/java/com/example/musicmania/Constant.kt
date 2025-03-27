package com.example.musicmania

class Constant {
    companion object {
        const val CHANNEL_ID = "MusicServiceChannel"
        const val NOTIFICATION_ID = 2

        const val ACTION_PLAY_PAUSE = "MUSIC_PLAY_PAUSE"
        const val ACTION_NEXT = "MUSIC_NEXT"
        const val ACTION_PREVIOUS = "MUSIC_PREVIOUS"
        const val ACTION_INIT_SERVICE = "INIT_SERVICE"
        const val ACTION_SEEK = "MUSIC_SEEK"

        const val BROADCAST_PLAYBACK_STATE = "com.example.musicmania.service.PLAYBACK_STATE"
        const val BROADCAST_PROGRESS = "com.example.musicmania.service.PROGRESS"
        const val TAG = "SongListBottomSheetFragment"
    }

}