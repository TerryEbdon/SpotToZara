package net.ebdon.spottozara

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.mp3.MP3AudioHeader
import java.util.logging.Logger
import java.util.logging.Level
import net.ebdon.audio.Ffmpeg
/**
 * Command Line Interface and playlist processing
 */
@SuppressWarnings('CatchException')
@groovy.util.logging.Log4j2
class SpotToZara {
  private static final Logger audioTagLogger = Logger.getLogger('org.jaudiotagger')
  final String m3u8FileType  = '.m3u8'
  final String m3uFileType   = '.m3u'
  final String zaraFileType  = '.lst'
  final long   unknownTrackLength = -99
  final String m3u8FileName
  final String m3uFileName
  final String zaraFileName

  Map m3u8 = [:]
  File playlist

  static void main( String[] args ) {
    final String path = args.last()
    Installer installer = new Installer(path)
    if (args.size() in 1..2) {
      switch (args.first()) {
        case 'install-ffmpeg': {
          installer.installFfmpeg()
          break
        }

        case 'install-spotdl': {
          installer.installSpotDL()
          break
        }

        default: {
          final String url = args.first()
          log.info  "Processing Spotify playlist $url"

          try {
            new SpotToZara( url ).run()
          } catch ( Exception ex ) {
            log.fatal ex
          }
        }
      }
    } else {
      log.error "Expecting 1 or 2 arguments but received ${args.size()}"
    }
  }

  SpotToZara( String spotFileName ) {
    Boolean includesFileType = spotFileName.contains( m3u8FileType )
    m3u8FileName = includesFileType ? spotFileName : spotFileName + m3u8FileType
    m3uFileName  = m3u8FileName - m3u8FileType + m3uFileType
    zaraFileName = m3u8FileName - m3u8FileType + zaraFileType
  }

  void run() {
    if (new Ffmpeg().configMatchesApp()) {
      loadM3u8()
      fixMetadata()
      trimSilence()
      normalise()
      saveAsZaraPlayList()
      saveAsM3uPlayList()
    }
  }

  void trimSilence() {
    log.info 'Trimming silence from start and end of tracks'
    new Ffmpeg().trimSilence( tracks )
  }

  void normalise() {
    log.info 'Normalising tracks'
    new Ffmpeg().normalise( tracks )
  }

  final List getTracks() {
    final String trackPathSeparator = '/' // work around for internal regex errors
    m3u8.collect { idx,track ->
      log.trace ">>$idx<${track.last()[-10..-1]}"
      String trackPath = track.last().replaceAll('\\\\',trackPathSeparator)
      trackPath.split(trackPathSeparator).last()
    }
  }

  void saveAsM3uPlayList() {
    if (m3u8.size() > 0 ) {
      File m3u = new File( m3uFileName )
      if (m3u.exists() == false) {
        log.info "Creating M3U playlist: $m3uFileName"
        m3u8.each { trackNo, details ->
          m3u << details[1]
          m3u << System.lineSeparator()
        }
        log.info "Created: $m3uFileName"
      } else {
        log.error 'M3U playlist already exists'
      }
    } else {
      log.debug 'm3u playlist not created as .m3u8 playlist is empty or missing'
    }
  }

  void saveAsZaraPlayList() {
    if (m3u8.size() > 0 ) {
      File lst = new File( zaraFileName )
      if (lst.exists() == false) {
        log.info "Creating ZaraRadio playlist: $zaraFileName"
        lst << "${m3u8.size()}"
        lst << System.lineSeparator()

        m3u8.each { trackNo, details ->
          details[0] = details[0] * 1000
          lst << details.join('\t')
          lst << System.lineSeparator()
        }
        log.info "Created: $zaraFileName"
      } else {
        log.fatal 'ABORTING as Zara playlist already exists'
      }
    } else {
      log.debug 'Zara playlist not created as .m3u8 playlist is empty or missing'
    }
  }

  void fixMetadata() {
    audioTagLogger.level = Level.WARNING
    int fixedCount = 0
    m3u8.each { trackNo, details ->
      if ( details.first() < 1 ) {
        log.debug "Fixing track $trackNo with length ${details[0]}"
        String trackFileName = details[1]
        log.debug trackFileName
        File trackFile = new File( trackFileName )
        AudioFile audioFile = AudioFileIO.read( trackFile )

        MP3AudioHeader audioHeader = audioFile.audioHeader
        String newLengthStr = audioHeader.trackLength
        Long newlength = Long.parseLong( newLengthStr )
        final long secsPerMin = 60
        Long mins = newlength / secsPerMin
        Long secs = newlength % secsPerMin
        log.debug "New length: $newLengthStr = $mins mins, $secs secs"
        details[0] = newlength
        ++fixedCount
      }
    }
    if (m3u8.size() > 0) {
      log.info "Fixed $fixedCount track lengths."
    } else {
      log.info 'Downloaded playlist is empty.'
    }
  }

  void loadM3u8() {
    playlist = new File( m3u8FileName )
    if ( playlist.exists() ) {
      processM3u8()
    } else {
      log.error "No such file ${m3u8FileName}"
    }
  }

  void processM3u8() {
    log.info "Loading: $m3u8FileName"
    long trackLength = unknownTrackLength
    int lineNo = 0
    int trackNo
    playlist.eachLine { line ->
      ++lineNo
      String[] bits = line.split(':')
      switch ( bits[0] ) {
        case '#EXTM3U': { // first line of file
          assert lineNo == 1
          break
        }
        case '#EXTINF': {    // length, artist - name
          log.trace "EXTINF  line: $lineNo, trackNo: $trackNo, ${line[0..10]}"
          trackLength = Long.parseLong( line.split(/(EXTINF:)|(,)/)[1] )
          break
        }
        default: { // file name
          ++trackNo
          log.trace "default line: $lineNo, trackNo: $trackNo, ${line[0..10]}"
          final String trackPath = new File( line ).absolutePath
          m3u8[trackNo] = [trackLength,trackPath]
          trackLength = unknownTrackLength
        }
      }
    }
  }
}
