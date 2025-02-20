package net.ebdon.spottozara

import groovy.ant.AntBuilder
import org.apache.tools.ant.Project

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
  static final String ffmpegFile    = "$downloadDir$ffmpegZip"

  static final String spotDlRepo    = "$github/spotDL/spotify-downloader"
  static final String spotDlversion = '4.2.5'
  static final String spotDlExe     = "spotdl-${spotDlversion}-win32.exe"
  static final String spotDlLastest = "releases/download/v${spotDlversion}"
  static final String spotDlUrl     = "$spotDlRepo/$spotDlLastest/$spotDlExe"
  static final String spotDlFile    = "$downloadDir/$spotDlExe"

  static final AntBuilder ant = new AntBuilder()

  final String installPath

  Installer(final String installPath) {
    this.installPath = installPath
    ant.project.buildListeners[0].messageOutputLevel = Project.MSG_WARN
  }

  void installSpotDL() {
    log.info  "Downloading: $spotDlExe"
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
      log.debug 'spotDl downloaded'
      log.info "Copying into: $installPath"
      ant.copy(
         file:  spotDlFile,
         todir: installPath,
         flatten: true
      )
      log.debug 'spotDl copied'
    } else {
      log.error spotDlDownloadFail
      ant.fail  spotDlDownloadFail
    }
  }

  void installFfmpeg() {
    log.trace "Exists before: ${new File(ffmpegFile).exists()}"
    log.trace "Path exists: ${new File(installPath).exists()}"

    assert new File(installPath).exists()

    log.info  "Downloading $ffmpegZip"
    ant.get (
      src:          ffmpegUrl,
      dest:         downloadDir,
      verbose:      false,
      usetimestamp: true,
    )
    if (new File(ffmpegFile).exists()) {
      log.debug 'ffmpeg downloaded'
      log.info  "Unzipping into: $installPath"
      ant.unzip(
         src:  ffmpegFile,
         dest: installPath,
      ) {
        patternset {
          include name: '**/*.exe'
        }
        mapper type: 'flatten'
      }
      log.debug 'ffmpeg unzipped'
    } else {
      log.error ffmpegDownloadFail
      ant.fail  ffmpegDownloadFail
    }
  }
}
