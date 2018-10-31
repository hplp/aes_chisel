package aes

import chisel3._

// implements wrapper for AES cipher and inverse cipher
// change Nk=4 for AES128, NK=6 for AES192, Nk=8 for AES256
class AES(Nk: Int) extends Module {
  require(Nk == 4 || Nk == 6 || Nk == 8)
  val KeyLength: Int = Nk * Params.rows
  val Nr: Int = Nk + 6 // 10, 12, 14 rounds
  val Nrplus1: Int = Nr + 1 // 10+1, 12+1, 14+1

  val io = IO(new Bundle {
    val AES_mode = Input(Bool()) // 00=cipher, 01=inverse cipher, later version: 10=key update
    val start = Input(Bool())
    //
    val input_text = Input(Vec(Params.StateLength, UInt(8.W))) // plaintext, ciphertext or expanded key
    val expandedKey = Input(Vec(Nrplus1, Vec(Params.StateLength, UInt(8.W)))) // for now, send the expanded key
    //
    val output_text = Output(Vec(Params.StateLength, UInt(8.W))) // ciphertext or plaintext
    val output_valid = Output(Bool())
  })

  // Instantiate module objects
  val CipherModule = Cipher(Nk)
  val InvCipherModule = InvCipher(Nk)
  //  val CipherModule = new Cipher(Nk)
  //  val InvCipherModule = new InvCipher(Nk)

  // Internal variables
  val initValues = Seq.fill(Params.StateLength) {
    0.U(8.W)
  }
  val output_valid = RegInit(VecInit(initValues))

  // The input text can go to both the cipher and the inverse cipher (for now)
  CipherModule.io.plaintext <> io.input_text
  CipherModule.io.expandedKey <> io.expandedKey
  InvCipherModule.io.ciphertext <> io.input_text
  InvCipherModule.io.expandedKey <> io.expandedKey

  // Cipher starts at (start=1 and AES_Mode=0)
  CipherModule.io.start := io.start && (!io.AES_mode)
  // Inverse Cipher starts at (start=1 and AES_Mode=1)
  InvCipherModule.io.start := io.start && (io.AES_mode)

  // AES output_valid can be the Cipher.output_valid OR InvCipher.output_valid
  io.output_valid := CipherModule.io.state_out_valid || InvCipherModule.io.state_out_valid
  // AES output can be managed using a Mux on the Cipher output and the InvCipher output
  io.output_text <> Mux(CipherModule.io.state_out_valid, CipherModule.io.state_out, InvCipherModule.io.state_out)
}