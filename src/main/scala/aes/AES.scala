package aes

import chisel3._
import chisel3.util._

// implements wrapper for AES cipher and inverse cipher
class AES extends Module {
  val io = IO(new Bundle {
    val AES_mode = Input(Bool()) // 00=cipher, 01=inverse cipher, later version: 10=key update
    val start = Input(Bool())
    //
    val input_text = Input(Vec(Params.stt_lng, UInt(8.W))) // plaintext, ciphertext or expanded key
    val expandedKey = Input(Vec(Params.Nrplus1, Vec(Params.stt_lng, UInt(8.W)))) // for now, send the expanded key
    //
    val output_text = Output(Vec(Params.stt_lng, UInt(8.W))) // ciphertext or plaintext
    val output_valid = Output(Bool())
  })

  // Instantiate module objects
  val CipherModule = Cipher()
  val InvCipherModule = InvCipher()

  // The input text can go to both the cipher and the inverse cipher for now
  CipherModule.io.plaintext := io.input_text
  InvCipherModule.io.ciphertext := io.input_text

  CipherModule.io.expandedKey := io.expandedKey
  InvCipherModule.io.expandedKey := io.expandedKey

  // The start is sent to either the cipher or the inverse cipher, based on AES_mode
  when(io.start) {
    when(!io.AES_mode) {
      CipherModule.io.start := io.start
      InvCipherModule.io.start := !io.start
    }.otherwise {
      CipherModule.io.start := !io.start
      InvCipherModule.io.start := io.start
    }
  }.otherwise {
    CipherModule.io.start := !io.start
    InvCipherModule.io.start := !io.start
  }

  // The state_out_valid determines which output to take
  when(CipherModule.io.state_out_valid) {
    io.output_valid := CipherModule.io.state_out_valid
    io.output_text := CipherModule.io.state_out
  }.elsewhen(InvCipherModule.io.state_out_valid) {
    io.output_valid := InvCipherModule.io.state_out_valid
    io.output_text := InvCipherModule.io.state_out
  }
}