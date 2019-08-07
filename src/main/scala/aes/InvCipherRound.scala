package aes

import chisel3._
import chisel3.util._

// implements AES_Decrypt round transforms
class InvCipherRound(transform: String, InvSubBytes_SCD: Boolean) extends Module {
  require(transform == "AddRoundKeyOnly" || transform == "NoInvMixColumns" || transform == "CompleteRound")
  val io = IO(new Bundle {
    val input_valid = Input(Bool())
    val state_in = Input(Vec(Params.StateLength, UInt(8.W)))
    val roundKey = Input(Vec(Params.StateLength, UInt(8.W)))
    val state_out = Output(Vec(Params.StateLength, UInt(8.W)))
    val output_valid = Output(Bool())
  })

  // A well defined 'DontCare' or Initialization value
  val ZeroInit = Vec(Seq.fill(Params.StateLength)(0.U(8.W)))

  // Transform sequences
  if (transform == "AddRoundKeyOnly") {

    // Instantiate module objects
    val AddRoundKeyModule = AddRoundKey()

    // AddRoundKey
    when(io.input_valid) {
      AddRoundKeyModule.io.state_in := io.state_in
      AddRoundKeyModule.io.roundKey := io.roundKey
    }.otherwise {
      AddRoundKeyModule.io.state_in := ZeroInit
      AddRoundKeyModule.io.roundKey := ZeroInit
    }

    // output
    io.state_out := ShiftRegister(AddRoundKeyModule.io.state_out, 1)
    io.output_valid := ShiftRegister(io.input_valid, 1)

  } else if (transform == "NoInvMixColumns") {

    // Instantiate module objects
    val AddRoundKeyModule = AddRoundKey()
    val InvSubBytesModule = InvSubBytes(InvSubBytes_SCD)
    val InvShiftRowsModule = InvShiftRows()

    // InvShiftRows and AddRoundKeyModule roundKey
    when(io.input_valid) {
      InvShiftRowsModule.io.state_in := io.state_in
      AddRoundKeyModule.io.roundKey := io.roundKey
    }.otherwise {
      InvShiftRowsModule.io.state_in := ZeroInit
      AddRoundKeyModule.io.roundKey := ZeroInit
    }
    // InvSubBytes
    InvSubBytesModule.io.state_in := InvShiftRowsModule.io.state_out
    // AddRoundKey
    AddRoundKeyModule.io.state_in := InvSubBytesModule.io.state_out

    // output
    io.state_out := ShiftRegister(AddRoundKeyModule.io.state_out, 1)
    io.output_valid := ShiftRegister(io.input_valid, 1)

  } else if (transform == "CompleteRound") {

    // Instantiate module objects
    val AddRoundKeyModule = AddRoundKey()
    val InvSubBytesModule = InvSubBytes(InvSubBytes_SCD)
    val InvShiftRowsModule = InvShiftRows()
    val InvMixColumnsModule = InvMixColumns()

    // InvShiftRows and AddRoundKeyModule roundKey
    when(io.input_valid) {
      InvShiftRowsModule.io.state_in := io.state_in
      AddRoundKeyModule.io.roundKey := io.roundKey
    }.otherwise {
      InvShiftRowsModule.io.state_in := ZeroInit
      AddRoundKeyModule.io.roundKey := ZeroInit
    }
    // InvSubBytes
    InvSubBytesModule.io.state_in := InvShiftRowsModule.io.state_out
    // AddRoundKey
    AddRoundKeyModule.io.state_in := InvSubBytesModule.io.state_out
    // InvMixColumns
    InvMixColumnsModule.io.state_in := AddRoundKeyModule.io.state_out

    // output
    io.state_out := ShiftRegister(InvMixColumnsModule.io.state_out, 1)
    io.output_valid := ShiftRegister(io.input_valid, 1)

  }

}

object InvCipherRound {
  def apply(transform: String, SubBytes_SCD: Boolean): InvCipherRound = Module(new InvCipherRound(transform, SubBytes_SCD))
}