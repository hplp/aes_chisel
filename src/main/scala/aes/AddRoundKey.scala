package aes

import chisel3._

// implements AddRoundKey
class AddRoundKey extends Module {
  val io = IO(new Bundle {
    val state_in = Input(Vec(Params.StateLength, UInt(8.W)))
    val roundKey = Input(Vec(Params.StateLength, UInt(8.W)))
    val state_out = Output(Vec(Params.StateLength, UInt(8.W)))
  })

  for (i <- 0 until Params.StateLength) {
    io.state_out(i) := io.state_in(i) ^ io.roundKey(i)
  }
}

object AddRoundKey {
  def apply(): AddRoundKey = Module(new AddRoundKey())
}