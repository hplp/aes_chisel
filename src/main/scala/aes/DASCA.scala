package aes

import chisel3._

class DASCA extends Module {
  val io = IO(new Bundle {
    val plaintext = Input(Vec(Params.StateLength, UInt(8.W))) // plaintext
    val roundKey = Input(Vec(Params.StateLength, UInt(8.W))) // roundKey
    val ciphertext = Output(Vec(Params.StateLength, UInt(8.W))) // ciphertext
  })

  printf("plaintext: %x %x %x %x %x %x %x %x %x %x %x %x %x %x %x %x\n", io.plaintext(0), io.plaintext(1), io.plaintext(2), io.plaintext(3), io.plaintext(4), io.plaintext(5), io.plaintext(6), io.plaintext(7), io.plaintext(8), io.plaintext(9), io.plaintext(10), io.plaintext(11), io.plaintext(12), io.plaintext(13), io.plaintext(14), io.plaintext(15))

  // Instantiate module objects
  val AddRoundKeyModule = AddRoundKey()
  val SubBytesModule = SubBytes()

  // AddRoundKey whitening transform
  AddRoundKeyModule.io.state_in := io.plaintext
  AddRoundKeyModule.io.roundKey := io.roundKey

  // SubBytes transform
  SubBytesModule.io.state_in := AddRoundKeyModule.io.state_out

  // done!
  io.ciphertext := SubBytesModule.io.state_out
}

object DASCA {
  def apply(): DASCA = Module(new DASCA)
}