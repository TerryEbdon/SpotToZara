package net.ebdon.spottozara

import groovy.ant.AntBuilder

class Installer {
  static final String downloadDir   = System.getProperty('java.io.tmpdir')

  static final String ffmpegRepo    = 'https://github.com/BtbN/FFmpeg-Builds'
  static final String ffmpegZip     = 'ffmpeg-master-latest-win64-gpl-shared.zip'
  static final String ffmpegLastest = 'releases/download/latest'
  static final String ffmpegUrl     = "$ffmpegRepo/$ffmpegLastest/$ffmpegZip"
  static final String ffmpegFile    = "$downloadDir/$ffmpegZip"

  static final AntBuilder ant = new AntBuilder()

  static void installFfmpeg(final String installPath) {

    println 'Installing ffmpeg'
    println "from: $ffmpegUrl"
    println "into: $installPath"
    println "Exists before: ${new File(ffmpegFile).exists()}"
    println "Path exists: ${new File(installPath).exists()}"
    assert new File(installPath).exists()

    ant.get (
      src:          ffmpegUrl,
      dest:         downloadDir,
      verbose:      false,
      usetimestamp: true
    )
    println "Exists after: ${new File(ffmpegFile).exists()}"
    if (new File(ffmpegFile).exists()) {
      ant.unzip(
         src: ffmpegFile,
         dest: installPath
      ) {
        patternset {
          include name: '**/*.exe'
          include name: '**/*.dll'
        }
        mapper type: 'flatten'
      }
    } else {
      ant.fail "ffmpeg download failed."
    }
  }
}
