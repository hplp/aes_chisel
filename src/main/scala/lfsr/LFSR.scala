package lfsr

import chisel3._
import chisel3.util.Cat

class LFSR extends Module {
  //declare input-output interface signals
  val io = IO(new Bundle {
    //clock and reset are default,
    //no other inputs necessary
    //lfsr_6 and lfsr_3r will have the random values
    val lfsr_6 = Output(UInt(6.W))
    val lfsr_3r = Output(UInt(3.W))
  })

  //declare the 6-bit register and initialize to 000001
  val D0123456 = RegInit(1.U(6.W)) //will init at reset

  //next clk value is XOR of 2 MSBs as LSB concatenated with left shift rest
  //example: 010010 => 100101 = Cat('10010','0^1')
  val nxt_D0123456 = Cat(D0123456(4, 0), D0123456(5) ^ D0123456(4))

  //update 6-bit register will happen in sync with clk
  D0123456 := nxt_D0123456

  //assign outputs
  io.lfsr_6 := D0123456
  //lfsr_3r in reverse order just for fun
  io.lfsr_3r := Cat(D0123456(1), D0123456(3), D0123456(5))
}

object LFSR {
  def apply(): LFSR = Module(new LFSR())
}