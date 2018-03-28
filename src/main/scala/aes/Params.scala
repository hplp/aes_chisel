package aes

object Params {
  val Nb = 4 // columns in state (in this standard)
  val rows = 4
  val StateLength = Nb * rows
  // Change Nk=4 for AES128, NK=6 for AES192, Nk=8 for AES256
  val Nk = 4 // 4, 6, 8 [32-bit words] columns in cipher key
  val KeyLength = Nk * rows
  val Nr = Nk + 6 // 10, 12, 14 rounds
  val Nrplus1 = Nr + 1 // 10+1, 12+1, 14+1
}