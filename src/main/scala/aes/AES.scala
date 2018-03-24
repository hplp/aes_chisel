package aes

import chisel3._
import chisel3.util._

// implements wrapper for AES cipher and inverse cipher
class AES extends Module {
  val io = IO(new Bundle {
    val   input_text = Input(Vec(Params.stt_lng, UInt(8.W))) // plaintext, ciphertext or expanded key
    val   input_oper = Input(Vec(Params.stt_lng, UInt(8.W))) // cipher, inverse cipher, or key update
    val  output_text = Output(Vec(Params.stt_lng, UInt(8.W))) // ciphertext or plaintext
    val output_valid = Output(Bool())
  })

}