package aes

object Params {
  val Nb = 4
  val rows = 4
  val stt_lng = Nb * rows
  val Nk = 4 // 4, 6, 8
  val CipherKeyLenghth = Nk * rows
  val Nr = Nk + 6 // 10, 12, 14
  val Nrplus1 = Nk + 6 + 1 // 10+1, 12+1, 14+1
}