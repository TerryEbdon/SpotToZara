package net.ebdon.spottozara

import groovy.xml.XmlSlurper
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.mp3.MP3AudioHeader
import java.util.logging.Logger
import java.util.logging.Level

class SpotToZara {
  final String m3uFileType  = '.m3u8'
  final String zaraFileType = '.lst'
  final String m3u8FileName
  final String zaraFileName
  def m3u8 = [:]

  public static main( args ) {
    if ( args.size() != 1 ) {
      println "Usage: SpotToZara {spotfile}"
    } else {
      // println "Processing ${args[0]}"
      new SpotToZara( args[0] ).run()
    }
  }

  SpotToZara( spotFileName ) {
    Boolean includesFileType = spotFileName.contains( m3uFileType ) 
    m3u8FileName = includesFileType ? spotFileName : spotFileName + m3uFileType
    zaraFileName = m3u8FileName - m3uFileType + zaraFileType
  }

  void run() {
    loadM3u8()
    fixMetadata()
    saveAsZaraPlayList()
  }

  void saveAsZaraPlayList() {
    File lst = new File( zaraFileName )
    if ( !lst.exists() ) {
      println "Creating ZaraRadio playlist: $zaraFileName"
      lst << "${m3u8.size()}\n"
      m3u8.each { trackNo, details ->
        details[0] = details[0] * 1000
        lst << details.join('\t')
        lst << '\r\n'
      }
    } else {
      println "ABORTING as Zara playlist already exists"
    }
  }

  void fixMetadata() {
    Logger.getLogger("org.jaudiotagger").setLevel(Level.WARNING)
    int fixedCount = 0
    m3u8.each { trackNo, details ->
      if ( details.first() < 1 ) {
        // println "Fixing track $trackNo with length ${details[0]}"
        String trackFileName = details[1]
        // println trackFileName
        File trackFile = new File( trackFileName ) 
        AudioFile audioFile = AudioFileIO.read( trackFile )

        MP3AudioHeader audioHeader = audioFile.getAudioHeader();
        String newLengthStr = audioHeader.getTrackLength();
        Long newlength = Long.parseLong( newLengthStr )
        Long mins = newlength / 60
        Long secs = newlength % 60 
        // println "New length: $newLengthStr = $mins mins, $secs secs"
        details[0]= newlength
        ++fixedCount
      }
    }
    println "Fixed $fixedCount track lengths."
  }

  void loadM3u8() {
    int lineNo = 0
    long trackLength
    // String fileName = /C:\portable\SpotDL\Love songs 3.m3u8/
    File file = new File( m3u8FileName )
    assert file.exists()
    println "Loading $m3u8FileName"
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
          ++trackNo
          // println "EXTINF  line: $lineNo, trackNo: $trackNo, ${line[0..10]}"
          assert lineNo > 1 && lineNo % 2 == 0
          trackLength = Long.parseLong( line.split(/(EXTINF:)|(,)/)[1] )
          break
        }
        default: { // file name
          // println "default line: $lineNo, trackNo: $trackNo, ${line[0..10]}"
          assert lineNo > 1 && lineNo % 2 == 1
          m3u8[trackNo] = [trackLength, line]
        }
      }
    }
  }
}
