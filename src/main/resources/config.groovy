/**
@file
@author Terry Ebdon
@brief This file configures the app.
*/

version = 'v3.0.1'
aTrim {
  start = 0.2
}
silenceRemove {
  startPeriods   = 1
  startSilence   = 0.5
  stopSilence    = 0.5
  startThreshold = '-26dB'
  startDuration  = 0
  stopDuration   = 1
  enabled        = true
}
