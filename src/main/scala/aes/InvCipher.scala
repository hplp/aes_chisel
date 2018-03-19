package aes

import chisel3._
import chisel3.util._

// implements AES_Decrypt
class InvCipher extends Module {
  val io = IO(new Bundle {
    val plaintext = Input(Vec(Params.stt_lng, UInt(8.W)))
    val expandedKey = Input(Vec(Params.Nrplus1, Vec(Params.stt_lng, UInt(8.W))))
    val start = Input(Bool())
    val state_out = Output(Vec(Params.stt_lng, UInt(8.W)))
    val state_out_valid = Output(Bool())
  })

  // Instantiate module objects
  val AddRoundKeyModule = AddRoundKey()
  val InvSubBytesModule = InvSubBytes()
  val InvShiftRowsModule = InvShiftRows()
  val InvMixColumnsModule = InvMixColumns()

  // Internal variables
  val initValues = Seq.fill(Params.stt_lng) { 0.U(8.W) }
  val state = RegInit(Vec(initValues))
  val rounds = RegInit(0.U(4.W))

  // STM
  val sIdle :: sInitialAR :: sBusy :: Nil = Enum(3)
  val STM = RegInit(sIdle)

  switch(STM) {
    is(sIdle) {
      when(io.start) { STM := sInitialAR } // Start cipher
      rounds := 0.U
    }
    is(sInitialAR) {
      rounds := rounds + 1.U
      STM := sBusy
    }
    is(sBusy) {
      rounds := rounds + 1.U
      when(rounds === Params.Nr.U) { STM := sIdle }
    }
  }

  // InvShiftRows state
  InvShiftRowsModule.io.state_in := state

  // InvSubBytes state
  InvSubBytesModule.io.state_in := InvShiftRowsModule.io.state_out

  // AddRoundKey state
  AddRoundKeyModule.io.state_in := Mux(STM === sInitialAR, io.plaintext, InvSubBytesModule.io.state_out)
  AddRoundKeyModule.io.roundKey := io.expandedKey(Params.Nr.U - rounds)

  // InvMixColumns state
  InvMixColumnsModule.io.state_in := AddRoundKeyModule.io.state_out

  state := Mux(STM != sIdle, Mux((rounds > 0.U) & (rounds < Params.Nr.U), InvMixColumnsModule.io.state_out, AddRoundKeyModule.io.state_out), Vec(initValues))

  // Set state_out_valid true when cipher ends
  io.state_out_valid := rounds === Params.Nrplus1.U
  io.state_out := state

  // Debug statements
  printf("STM: %d, rounds: %d, valid: %d\n", STM, rounds, io.state_out_valid)
  printf("state: %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d\n", state(0), state(1), state(2), state(3), state(4),
    state(5), state(6), state(7), state(8), state(9), state(10), state(11), state(12), state(13), state(14), state(15))
}