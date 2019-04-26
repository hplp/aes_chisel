package aes

import chisel3._

// implements AES_Encrypt round transforms
class CipherRound(transform: String, SubBytes_SCD: Boolean) extends Module {
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
    val SubBytesModule = SubBytes(SubBytes_SCD)
    val ShiftRowsModule = ShiftRows()

    // SubBytes
    SubBytesModule.io.state_in := io.state_in
    // ShiftRows
    ShiftRowsModule.io.state_in := SubBytesModule.io.state_out
    // AddRoundKey
    AddRoundKeyModule.io.state_in := ShiftRowsModule.io.state_out
    AddRoundKeyModule.io.roundKey := io.roundKey
    // output
    io.state_out := AddRoundKeyModule.io.state_out

  } else if (transform == "CompleteRound") {

    // Instantiate module objects
    val AddRoundKeyModule = AddRoundKey()
    val SubBytesModule = SubBytes(SubBytes_SCD)
    val ShiftRowsModule = ShiftRows()
    val MixColumnsModule = MixColumns()

    // SubBytes
    SubBytesModule.io.state_in := io.state_in
    // ShiftRows
    ShiftRowsModule.io.state_in := SubBytesModule.io.state_out
    // MixColumns
    MixColumnsModule.io.state_in := ShiftRowsModule.io.state_out
    // AddRoundKey
    AddRoundKeyModule.io.state_in := MixColumnsModule.io.state_out
    AddRoundKeyModule.io.roundKey := io.roundKey
    // output
    io.state_out := AddRoundKeyModule.io.state_out

  }

}

object CipherRound {
  def apply(transform: String, SubBytes_SCD: Boolean): CipherRound = Module(new CipherRound(transform, SubBytes_SCD))
}