package org.lineageos.twelve.datasources.mediastore

import org.lineageos.twelve.datasources.mediastore.models.LyricsLine
import org.lineageos.twelve.models.Lyrics
import java.util.regex.Matcher
import java.util.regex.Pattern

class LrcParser {
    companion object {
        private val LYRIC_LINE_PATTERN: Pattern =
            Pattern.compile("""\[(\d{2}):(\d{2})\.(\d{2})\](.*)""")

        private fun parseLine(line: String): LyricsLine {
            val matcher = LYRIC_LINE_PATTERN.matcher(line)
            if (matcher.find()) {
                val minutes = matcher.group(1)?.toLong()!!
                val seconds = matcher.group(2)?.toLong()!!
                val hundredths = matcher.group(3)?.toLong()!!
                val text = matcher.group(4)?.trim { it <= ' ' }!!
                val timestampInMillis = (minutes * 60 * 1000) + (seconds * 1000) + (hundredths * 10)

                return LyricsLine(text, timestampInMillis)
            }
            return LyricsLine(line)
        }

        fun parseLyrics(lyrics: String): Lyrics{
            return Lyrics.Builder().apply {
                lyrics.lineSequence().forEach { line ->
                    parseLine(line).let{ lyricsLine ->
                        addLine(lyricsLine.text, lyricsLine.startTimeMs)
                    }
                }
            }.build()
        }

        fun isValidLrc(lyricsString: String): Boolean {
            if (lyricsString.isEmpty()) return false
            return run {
                lyricsString.lineSequence().forEach { line ->
                    if (line.startsWith('#')) return@forEach

                    val matcher: Matcher = LYRIC_LINE_PATTERN.matcher(line)
                    if (matcher.matches()) {
                        return@run true
                    }
                }
                return@run false
            }
        }
    }
}
