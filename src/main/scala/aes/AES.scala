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
    val input_text = Input(Vec(Params.StateLength, UInt(8.W))) // plaintext, ciphertext
    val roundKey = Input(Vec(Params.StateLength, UInt(8.W))) // single roundKey
    //
    val output_text = Output(Vec(Params.StateLength, UInt(8.W))) // ciphertext or plaintext
    val output_valid = Output(Bool())
  })

  // Instantiate module objects
  val CipherModule = Cipher(Nk)
  val InvCipherModule = InvCipher(Nk)

  // Create a synchronous-read, synchronous-write memory block big enough for any key length
  // A roundKey is 16 bytes, and 1+(10/12/14) of them are needed
//  val address = Wire(UInt(4.W))
//  val dataIn = Wire(UInt((Params.StateLength * 8).W))
//  val dataOut = Wire(UInt((Params.StateLength * 8).W))
//  val enable = Wire(Bool())
//  val expandedKeyMem = SyncReadMem(16, UInt((Params.StateLength * 8).W))

  // Internal variables
  val initValues = Seq.fill(Params.StateLength) {
    0.U(8.W)
  }
  val output_valid = RegInit(VecInit(initValues))

  // The input text can go to both the cipher and the inverse cipher (for now)
  CipherModule.io.plaintext <> io.input_text
  CipherModule.io.roundKey <> io.roundKey
  InvCipherModule.io.ciphertext <> io.input_text
  InvCipherModule.io.roundKey <> io.roundKey

  // Cipher starts at (start=1 and AES_Mode=0)
  CipherModule.io.start := io.start && (!io.AES_mode)
  // Inverse Cipher starts at (start=1 and AES_Mode=1)
  InvCipherModule.io.start := io.start && (io.AES_mode)

  // AES output_valid can be the Cipher.output_valid OR InvCipher.output_valid
  io.output_valid := CipherModule.io.state_out_valid || InvCipherModule.io.state_out_valid
  // AES output can be managed using a Mux on the Cipher output and the InvCipher output
  io.output_text <> Mux(CipherModule.io.state_out_valid, CipherModule.io.state_out, InvCipherModule.io.state_out)
}

object AES {
  def apply(Nk: Int): AES = Module(new AES(Nk))
}