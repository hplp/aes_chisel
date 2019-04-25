package aes

import chisel3._

// implements AES_Decrypt round transforms
class InvCipherRound(transform: String, InvSubBytes_SCD: Boolean) extends Module {
  require(transform == "AddRoundKeyOnly" || transform == "NoMixColumns" || transform == "CompleteRound")
  val io = IO(new Bundle {
    val state_in = Input(Vec(Params.StateLength, UInt(8.W)))
    val roundKey = Input(Vec(Params.StateLength, UInt(8.W)))
    val state_out = Output(Vec(Params.StateLength, UInt(8.W)))
  })

  // Transform sequences
  if (transform == "AddRoundKeyOnly") {

    // Instantiate module objects
    val AddRoundKeyModule = AddRoundKey()

    // AddRoundKey
    AddRoundKeyModule.io.state_in := io.state_in
    AddRoundKeyModule.io.roundKey := io.roundKey
    // output
    io.state_out := AddRoundKeyModule.io.state_out

  } else if (transform == "NoMixColumns") {

    // Instantiate module objects
    val AddRoundKeyModule = AddRoundKey()
    val InvSubBytesModule = InvSubBytes(InvSubBytes_SCD)
    val InvShiftRowsModule = InvShiftRows()

    // InvShiftRows
    InvShiftRowsModule.io.state_in := io.state_in
    // InvSubBytes
    InvSubBytesModule.io.state_in := InvShiftRowsModule.io.state_out
    // AddRoundKey
    AddRoundKeyModule.io.state_in := InvSubBytesModule.io.state_out
    AddRoundKeyModule.io.roundKey := io.roundKey
    // output
    io.state_out := AddRoundKeyModule.io.state_out

  } else if (transform == "CompleteRound") {

    // Instantiate module objects
    val AddRoundKeyModule = AddRoundKey()
    val InvSubBytesModule = InvSubBytes(InvSubBytes_SCD)
    val InvShiftRowsModule = InvShiftRows()
    val InvMixColumnsModule = InvMixColumns()

    // InvShiftRows
    InvShiftRowsModule.io.state_in := io.state_in
    // InvSubBytes
    InvSubBytesModule.io.state_in := InvShiftRowsModule.io.state_out
    // AddRoundKey
    AddRoundKeyModule.io.state_in := InvSubBytesModule.io.state_out
    AddRoundKeyModule.io.roundKey := io.roundKey
    // InvMixColumns
    InvMixColumnsModule.io.state_in := AddRoundKeyModule.io.state_out
    // output
    io.state_out := InvMixColumnsModule.io.state_out

  }

}