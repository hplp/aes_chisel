package aes

import chisel3._
import chisel3.util._

// implements wrapper for AES cipher and inverse cipher
class AES extends Module {
  val io = IO(new Bundle {
    val AES_mode = Input(Bool()) // 00=cipher, 01=inverse cipher, later version: 10=key update
    val start = Input(Bool())
    //
    val input_text = Input(Vec(Params.StateLength, UInt(8.W))) // plaintext, ciphertext or expanded key
    val expandedKey = Input(Vec(Params.Nrplus1, Vec(Params.StateLength, UInt(8.W)))) // for now, send the expanded key
    //
    val output_text = Output(Vec(Params.StateLength, UInt(8.W))) // ciphertext or plaintext
    val output_valid = Output(Bool())
  })

  // Instantiate module objects
  val CipherModule = Cipher()
  val InvCipherModule = InvCipher()

  // Internal variables
  val initValues = Seq.fill(Params.StateLength) { 0.U(8.W) }
  val output_valid = RegInit(Vec(initValues))

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