package aes

import chisel3._

// implements ShiftRows
class ShiftRows extends Module {
  val io = IO(new Bundle {
    val state_in = Input(Vec(Params.StateLength, UInt(8.W)))
    val state_out = Output(Vec(Params.StateLength, UInt(8.W)))
  })

  io.state_out(0) := io.state_in(0)
  io.state_out(1) := io.state_in(5)
  io.state_out(2) := io.state_in(10)
  io.state_out(3) := io.state_in(15)

  io.state_out(4) := io.state_in(4)
  io.state_out(5) := io.state_in(9)
  io.state_out(6) := io.state_in(14)
  io.state_out(7) := io.state_in(3)

  io.state_out(8) := io.state_in(8)
  io.state_out(9) := io.state_in(13)
  io.state_out(10) := io.state_in(2)
  io.state_out(11) := io.state_in(7)

  io.state_out(12) := io.state_in(12)
  io.state_out(13) := io.state_in(1)
  io.state_out(14) := io.state_in(6)
  io.state_out(15) := io.state_in(11)
}

object ShiftRows {
  def apply(): ShiftRows = Module(new ShiftRows())
}
