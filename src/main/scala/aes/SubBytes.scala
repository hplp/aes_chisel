package aes

import chisel3._

class SubBytes extends Module {
  val io = IO(new Bundle {
    val state_in = Input(UInt(8.W))
    val state_out = Output(UInt(8.W))
  })

  io.state_out := io.state_in
}