package net.ebdon.spottozara

import groovy.ant.AntBuilder

/**
 * Bootstrap class that downloads dependencies.
 */
@groovy.util.logging.Log4j2
class Installer {
  static final String ffmpegDownloadFail = 'ffmpeg download failed.'
  static final String spotDlDownloadFail = 'spotDl download failed.'

  static final String downloadDir   = System.getProperty('java.io.tmpdir')
  static final String github        = 'https://github.com'

  static final String ffmpegRepo    = "$github/BtbN/FFmpeg-Builds"
  static final String ffmpegZip     = 'ffmpeg-master-latest-win64-lgpl.zip'
  static final String ffmpegLastest = 'releases/download/latest'
  static final String ffmpegUrl     = "$ffmpegRepo/$ffmpegLastest/$ffmpegZip"
  static final String ffmpegFile    = "$downloadDir/$ffmpegZip"

  static final String spotDlRepo    = "$github/spotDL/spotify-downloader"
  static final String spotDlversion = '4.2.5'
  static final String spotDlExe     = "spotdl-${spotDlversion}-win32.exe"
  static final String spotDlLastest = "releases/download/v${spotDlversion}"
  static final String spotDlUrl     = "$spotDlRepo/$spotDlLastest/$spotDlExe"
  static final String spotDlFile    = "$downloadDir/$spotDlExe"

  static final AntBuilder ant = new AntBuilder()

  static void installSpotDL(final String installPath) {
    log.info  "Installing spotDl from: $spotDlUrl"
    log.info  "Installing spotDl into: $installPath"
    log.trace "Exists before: ${new File(spotDlFile).exists()}"
    log.trace "Path exists: ${new File(installPath).exists()}"

    assert new File(installPath).exists()

    ant.get (
      src:          spotDlUrl,
      dest:         downloadDir,
      verbose:      false,
      usetimestamp: true
    )

    if (new File(spotDlFile).exists()) {
      log.info 'spotDl downloaded'
      ant.copy(
         file:  spotDlFile,
         todir: installPath,
         flatten: true
      )
      log.info 'spotDl installed'
    } else {
      log.error spotDlDownloadFail
      ant.fail  spotDlDownloadFail
    }
  }

  static void installFfmpeg(final String installPath) {
    log.info  "Installing ffmpeg from: $ffmpegUrl"
    log.info  "Installing ffmpeg into: $installPath"
    log.trace "Exists before: ${new File(ffmpegFile).exists()}"
    log.trace "Path exists: ${new File(installPath).exists()}"

    assert new File(installPath).exists()

    ant.get (
      src:          ffmpegUrl,
      dest:         downloadDir,
      verbose:      false,
      usetimestamp: true

    )
    if (new File(ffmpegFile).exists()) {
      log.info 'ffmpeg downloaded'
      ant.unzip(
         src:  ffmpegFile,
         dest: installPath,
      ) {
        patternset {
          include name: '**/*.exe'
        }
        mapper type: 'flatten'
      }
      log.info 'ffmpeg unzipped'
    } else {
      log.error ffmpegDownloadFail
      ant.fail  ffmpegDownloadFail
    }
  }
}
