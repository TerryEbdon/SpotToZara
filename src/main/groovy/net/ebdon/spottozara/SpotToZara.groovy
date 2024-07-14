package net.ebdon.spottozara

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.mp3.MP3AudioHeader
import java.util.logging.Logger
import java.util.logging.Level

@groovy.util.logging.Log4j2
class SpotToZara {
  public static Logger audioTagLogger = Logger.getLogger('org.jaudiotagger')
  final String m3u8FileType  = '.m3u8'
  final String m3uFileType   = '.m3u'
  final String zaraFileType  = '.lst'
  final String m3u8FileName
  final String m3uFileName
  final String zaraFileName
  def m3u8 = [:]
  File playlist

  public static main( args ) {
    final String path = args.last()
    if (args.size() in 1..2) {
      switch (args.first()) {
        case 'install-ffmpeg': {
          Installer.installFfmpeg(path)
          break
        }

        case 'install-spotdl': {
          Installer.installSpotDL(path)
          break
        }

        default: {
          final String url = args.first()
          println   "downloading playlist $url"
          log.info  "downloading playlist $url"

          try {
            new SpotToZara( url ).run()
          } catch ( Throwable thrown ) {
            log.fatal thrown
          }
        }
      }
    } else {
      log.error "Expecting 1 or 2 arguments but received ${args.size()}"
    }
  }

  SpotToZara( spotFileName ) {
    Boolean includesFileType = spotFileName.contains( m3u8FileType ) 
    m3u8FileName = includesFileType ? spotFileName : spotFileName + m3u8FileType
    m3uFileName  = m3u8FileName - m3u8FileType + m3uFileType
    zaraFileName = m3u8FileName - m3u8FileType + zaraFileType
  }

  void run() {
    loadM3u8()
    fixMetadata()
    saveAsZaraPlayList()
    saveAsM3uPlayList()
  }

  void saveAsM3uPlayList() {
    if (m3u8.size() > 0 ) {
      File m3u = new File( m3uFileName )
      if ( !m3u.exists() ) {
        log.info "Creating M3U playlist: $m3uFileName"
        m3u8.each { trackNo, details ->
          m3u << details[1]
          m3u << '\r\n'
        }
        println "Created: $m3uFileName"
      } else {
        log.error "M3U playlist already exists"
      }
    } else {
      log.debug "m3u playlist not created as .m3u8 playlist is empty or missing"
    }
  }

  void saveAsZaraPlayList() {
    if (m3u8.size() > 0 ) {
      File lst = new File( zaraFileName )
      if ( !lst.exists() ) {
        log.info "Creating ZaraRadio playlist: $zaraFileName"
        lst << "${m3u8.size()}\n"
        m3u8.each { trackNo, details ->
          details[0] = details[0] * 1000
          lst << details.join('\t')
          lst << '\r\n'
        }
        println "Created: $zaraFileName"
      } else {
        log.fatal "ABORTING as Zara playlist already exists"
      }
    } else {
      log.debug "Zara playlist not created as .m3u8 playlist is empty or missing"
    }
  }

  void fixMetadata() {
    audioTagLogger.setLevel(Level.WARNING)
    int fixedCount = 0
    m3u8.each { trackNo, details ->
      if ( details.first() < 1 ) {
        log.debug "Fixing track $trackNo with length ${details[0]}"
        String trackFileName = details[1]
        log.debug trackFileName
        File trackFile = new File( trackFileName ) 
        AudioFile audioFile = AudioFileIO.read( trackFile )

        MP3AudioHeader audioHeader = audioFile.getAudioHeader();
        String newLengthStr = audioHeader.getTrackLength();
        Long newlength = Long.parseLong( newLengthStr )
        Long mins = newlength / 60
        Long secs = newlength % 60 
        log.debug "New length: $newLengthStr = $mins mins, $secs secs"
        details[0]= newlength
        ++fixedCount
      }
    }
    if (m3u8.size() > 0) {
      log.info "Fixed $fixedCount track lengths."
      println "Fixed $fixedCount track lengths."
    } else {
      log.info "No tracks downloaded"
      println  "No tracks downloaded"
    }
  }

  void loadM3u8() {
    playlist = new File( m3u8FileName )
    if ( playlist.exists() ) {
      processM3u8()
    } else {
      log.error "No such file: ${m3u8FileName}"
    }
  }

  void processM3u8() {
    println "Loading: $m3u8FileName"
    long trackLength = -99
    int lineNo = 0
    int trackNo
    playlist.eachLine { line ->
      ++lineNo
      def bits = line.split(':')
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
          trackLength = -99
        }
      }
    }
  }
}
