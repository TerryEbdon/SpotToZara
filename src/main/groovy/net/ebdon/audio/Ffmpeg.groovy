package net.ebdon.audio

import groovy.ant.AntBuilder
import org.apache.tools.ant.Project
import java.nio.file.Paths
import org.codehaus.groovy.tools.groovydoc.ClasspathResourceManager

/**
 * Use `ffmpeg` to process audio files.
 */
@groovy.util.logging.Log4j2
class Ffmpeg {
  static final String configFileName = 'config.groovy'

  final String logLevel   = '-loglevel error'
  final String q          = '"'
  final String currentDir = '.'

  final AntBuilder ant    = new AntBuilder()
  private Map config

  final String srStartPeriods
  final String srStartSilence
  final String srStopSilence
  final String srStopDuration
  final Boolean srEnabled

  Ffmpeg() {
    ant.project.buildListeners[0].messageOutputLevel = Project.MSG_WARN
    loadConfig()
    srStartPeriods = config.silenceRemove.startPeriods
    srStartSilence = config.silenceRemove.startSilence
    srStopSilence  = config.silenceRemove.stoptSilence
    srStopDuration = config.silenceRemove.stopDuration
    srEnabled      = config.silenceRemove.enabled
  
    log.info "srStartPeriods: $srStartPeriods"
    log.info "srStartSilence: $srStartSilence"
    log.info "srStopSilence:  $srStopSilence"
    log.info  "srStopDuration: $srStopDuration"
  }

  private void loadConfig() {
    log.info "Loading config from $configFileName"
    log.info 'Current folder is ' + new File('.').absolutePath
    log.debug 'package:  ' + getClass().packageName
    log.debug 'Class is: ' + getClass().name

    final File configFile = new File( configFileName )

    if ( configFile.exists() ) {
      log.info "Using custom configuration from file:\n  ${configFile.absolutePath}"
      println  "Using custom configuration from file:\n  ${configFile.absolutePath}"
      config = new ConfigSlurper().parse( configFile.toURI().toURL())
    } else {
      log.info 'Using default configuration.'
      println 'Using default configuration.'
      ClasspathResourceManager resourceManager = new ClasspathResourceManager()
      def configScript = resourceManager.getReader(configFileName)
      if ( configScript ) {
        final String scriptText = configScript.text
        log.debug scriptText
        config = new ConfigSlurper().parse( scriptText )
        log.trace "config: ${config}"
      } else {
        ant.fail "Couldn't load resource for configuration script."
      }
    }
   log.debug 'Resource loaded'
  }

  void trimSilence( List<String> trackList ) {
    if ( srEnabled ) {
      log.info 'Trimming silence from start and end of tracks.'
      trackList.each { String trackFileName ->
        trimAudio( trackFileName )
      } 
    } else {
      log.info 'Silence trimming is disabled.'
    }
  }

  String getBinPath() {
    String jarPath = Paths.get(
      this.class.protectionDomain.
        codeSource.location.toURI()
    )

    "${jarPath}\\..\\..\\bin"
  }

  void normalise( List<String> trackList ) {
    log.info 'Normalising tracks'
    trackList.each { String trackFileName ->
      normaliseAudio( trackFileName )
    }
  }

  /**
  * Normalise with single-pass
  * <a href="https://ffmpeg.org/ffmpeg-filters.html#loudnorm">loudnorm</a>
  * filter
  */
  void normaliseAudio( final String mp3FileName ) {
    final String integratedLoudnessTarget = '-13'
    final String filter = "loudnorm=I=$integratedLoudnessTarget"
    filterTrack( 'loudnorm', filter, mp3FileName)
  }

  void filterTrack( String filterName, String filter, String mp3FileName ) {
    final String task  = "-af $filter"

    runTask( filterName, task, mp3FileName )
  }

  void runTask( String taskName, String task, String trackFileName ) {
    log.info "${taskName}: $trackFileName"
    final String tempFileName   = "${taskName}_$trackFileName"
    final String replaceOutFile = '-y'
    final String input          = "-i $q$trackFileName$q"
    final String inFileArg      = "$replaceOutFile $input"
    final String outFileArg     = "$q$tempFileName$q"
    final String argsLine       = "$inFileArg $logLevel $task $outFileArg"
    log.debug argsLine

    ant.exec (
      dir               : currentDir,
      executable        : "${binPath}\\ffmpeg",
      outputproperty    : 'cmdOut',
      errorproperty     : 'cmdError',
      resultproperty    : 'cmdResult',
    ) {
      arg( line: argsLine )
    }

    final Map antProperties = ant.project.properties
    final int execRes       = antProperties.cmdResult.toInteger()
    final String execOut    = antProperties.cmdOut
    final String execErr    = antProperties.cmdError
    log.debug "$taskName execOut = $execOut"
    log.debug "$taskName execErr = $execErr"
    log.debug "$taskName execRes = $execRes"

    if ( execErr.empty ) {
      moveFile tempFileName, trackFileName
    } else {
      log.error "Task failed: $taskName"
      log.error execErr
      log.info "out: $execOut"
      log.info "result: $execRes"
    }
  }

  void trimAudio( final String mp3FileName ) {
    final String trimTrackArgs =
      "areverse,atrim=start=0.2,silenceremove=start_periods=$srStartPeriods:start_silence=0.1:start_threshold=$srStartSilence:stop_silence=$srStopSilence:stop_duration=$srStopDuration"
    final String filter = "$q$trimTrackArgs,$trimTrackArgs$q"

    filterTrack('trimSilence', filter, mp3FileName)
  }

  void applyTags( String mp3FileName, String artist, String title ) {
    final String md             = '-metadata'
    final String nameMd         = "artist=$q$artist$q"
    final String artistMd       = "$md $nameMd $md album_$nameMd"
    final String titleMd        = "$md title=$q$title$q"

    final String task = "$titleMd $artistMd"
    log.info "Tagging $mp3FileName"
    log.debug "task: $task"

    runTask('applyTags', task, mp3FileName)
  }

  void moveFile( final String fromFileName, final String toFileName ) {
    ant.move(
      file: fromFileName, tofile: toFileName,
      failonerror: true, verbose: false, overwrite: true, force:true
    )
  }
}
