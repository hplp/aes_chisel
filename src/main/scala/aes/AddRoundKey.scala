package aes

import chisel3._

class AddRoundKey extends Module {
  val io = IO(new Bundle {
    val state_in = Input(Vec(16, UInt(8.W)))
    val roundKey = Input(Vec(16, UInt(8.W)))
    val state_out = Output(Vec(16, UInt(8.W)))
  })

  for (i <- 0 until 16) {
    io.state_out(i) := io.state_in(i) ^ io.roundKey(i)
  }
}

object AddRoundKey {
  def apply(): AddRoundKey = new AddRoundKey()
}