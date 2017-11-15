package aes

import chisel3._
import chisel3.util._

class Cipher extends Module {
  val io = IO(new Bundle {
    val plaintext = Input(Vec(16, UInt(8.W)))
    val expandedKey = Input(Vec(11, Vec(16, UInt(8.W))))
    val start = Input(Bool())
    val state_out = Output(Vec(16, UInt(8.W)))
    val state_out_valid = Output(Bool())
  })

  // Instantiate module objects
  val AddRoundKeyModule = AddRoundKey()
  val SubBytesModule = SubBytes()
  val ShiftRowsModule = ShiftRows()
  val MixColumnsModule = MixColumns()

  // Internal variables
  val initValues = Seq.fill(16) { 0.U(8.W) }
  val state = RegInit(Vec(initValues))
  val nxt_state = RegInit(Vec(initValues))
  val rounds = RegInit(0.U(4.W))
  val step_cnt = RegInit(0.U(2.W))

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
      when(rounds === 10.U) { STM := sIdle }
    }
  }

  // AddRoundKey state
  AddRoundKeyModule.io.state_in := Mux(STM === sInitialAR, io.plaintext,
    Mux(rounds === 10.U, ShiftRowsModule.io.state_out, MixColumnsModule.io.state_out))
  AddRoundKeyModule.io.roundKey := io.expandedKey(rounds)

  // SubBytes state
  SubBytesModule.io.state_in := state

  // ShiftRows state
  ShiftRowsModule.io.state_in := SubBytesModule.io.state_out

  // MixColumns state
  MixColumnsModule.io.state_in := ShiftRowsModule.io.state_out

  state := Mux(STM != sIdle, AddRoundKeyModule.io.state_out, Vec(initValues))
  //nxt_state := state

  // Set state_out_valid true when cipher ends
  io.state_out_valid := rounds === 11.U
  io.state_out := state

  // Debug statements
  printf("STM: %d, rounds: %d, valid: %d\n", STM, rounds, io.state_out_valid)
  printf("state: %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d\n", state(0), state(1), state(2), state(3), state(4),
    state(5), state(6), state(7), state(8), state(9), state(10), state(11), state(12), state(13), state(14), state(15))
  //printf("nxt_state: %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d\n", nxt_state(0), nxt_state(1), nxt_state(2), nxt_state(3),
  //nxt_state(4), nxt_state(5), nxt_state(6), nxt_state(7), nxt_state(8), nxt_state(9), nxt_state(10), nxt_state(11), nxt_state(12), nxt_state(13), nxt_state(14), nxt_state(15))
}