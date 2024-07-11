package net.ebdon.spottozara

class Installer {
  AntBuilder ant = new AntBuilder()

  void installFfmpeg() {
    ant.get (
      src:          "https://downloads.apache.org/ant/KEYS",
      dest:         "KEYS",
      verbose:      false,
      usetimestamp: true
    )
  }
}
