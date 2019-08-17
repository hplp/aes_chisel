package aes

import chisel3._
import chisel3.util._

// implements AddRoundKey
class AddRoundKey(Pipelined: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val state_in = Input(Vec(Params.StateLength, UInt(8.W)))
    val roundKey = Input(Vec(Params.StateLength, UInt(8.W)))
    val state_out = Output(Vec(Params.StateLength, UInt(8.W)))
  })

  for (i <- 0 until Params.StateLength) {
    if (Pipelined) {
      io.state_out(i) := ShiftRegister(io.state_in(i) ^ io.roundKey(i), 1)
    } else {
      io.state_out(i) := io.state_in(i) ^ io.roundKey(i)
    }
  }
}

object AddRoundKey {
  def apply(Pipelined: Boolean = false): AddRoundKey = Module(new AddRoundKey(Pipelined))
}