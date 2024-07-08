package net.ebdon.spottozara

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.mp3.MP3AudioHeader
import java.util.logging.Logger
import java.util.logging.Level
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError

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

  public static main( args ) {
    if ( args.size() != 1 ) {
      println "Usage: SpotToZara {spotfile}"
    } else {
      log.info "Processing ${args[0]}"
      try {
        new SpotToZara( args[0] ).run()
      } catch ( Throwable thrown ) {
       log.fatal thrown
      }
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
  }

  void saveAsZaraPlayList() {
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
    log.info "Fixed $fixedCount track lengths."
    println "Fixed $fixedCount track lengths."
  }

  void loadM3u8() {
    int lineNo = 0
    long trackLength = -99
    File file = new File( m3u8FileName )
    assert file.exists()
    println "Loading: $m3u8FileName"
    int trackNo
    file.eachLine { line ->
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
