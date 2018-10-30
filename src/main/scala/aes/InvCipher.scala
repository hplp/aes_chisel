package aes

import chisel3._
import chisel3.util._

// implements AES_Decrypt
// change Nk=4 for AES128, NK=6 for AES192, Nk=8 for AES256
class InvCipher(Nk: Int) extends Module {
  require(Nk == 4 || Nk == 6 || Nk == 8)
  val KeyLength = Nk * Params.rows
  val Nr = Nk + 6 // 10, 12, 14 rounds
  val Nrplus1 = Nr + 1 // 10+1, 12+1, 14+1

  val io = IO(new Bundle {
    val ciphertext = Input(Vec(Params.StateLength, UInt(8.W)))
    val expandedKey = Input(Vec(Nrplus1, Vec(Params.StateLength, UInt(8.W))))
    val start = Input(Bool())
    val state_out = Output(Vec(Params.StateLength, UInt(8.W)))
    val state_out_valid = Output(Bool())
  })

  // Instantiate module objects
  val AddRoundKeyModule = AddRoundKey()
  val InvSubBytesModule = InvSubBytes()
  val InvShiftRowsModule = InvShiftRows()
  val InvMixColumnsModule = InvMixColumns()

  // Internal variables
  val initValues = Seq.fill(Params.StateLength) {
    0.U(8.W)
  }
  val state = RegInit(VecInit(initValues))
  val rounds = RegInit(0.U(4.W))

  // STM
  val sIdle :: sInitialAR :: sBusy :: Nil = Enum(3)
  val STM = RegInit(sIdle)

  switch(STM) {
    is(sIdle) {
      when(io.start) {
        STM := sInitialAR
      } // Start cipher
      rounds := 0.U
    }
    is(sInitialAR) {
      rounds := rounds + 1.U
      STM := sBusy
    }
    is(sBusy) {
      rounds := rounds + 1.U
      when(rounds === Nr.U) {
        STM := sIdle
      }
    }
  }

  // InvShiftRows state
  InvShiftRowsModule.io.state_in := state

  // InvSubBytes state
  InvSubBytesModule.io.state_in := InvShiftRowsModule.io.state_out

  // AddRoundKey state
  AddRoundKeyModule.io.state_in := Mux(STM === sInitialAR, io.ciphertext, InvSubBytesModule.io.state_out)
  AddRoundKeyModule.io.roundKey := io.expandedKey(Nr.U - rounds)

  // InvMixColumns state
  InvMixColumnsModule.io.state_in := AddRoundKeyModule.io.state_out

  state := Mux(STM =/= sIdle, Mux((rounds > 0.U) & (rounds < Nr.U), InvMixColumnsModule.io.state_out, AddRoundKeyModule.io.state_out), VecInit(initValues))

  // Set state_out_valid true when cipher ends
  io.state_out_valid := rounds === Nrplus1.U
  io.state_out := state

  // Debug statements
  printf("STM: %d, rounds: %d, valid: %d\n", STM, rounds, io.state_out_valid)
  printf("state: %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d\n", state(0), state(1), state(2), state(3), state(4),
    state(5), state(6), state(7), state(8), state(9), state(10), state(11), state(12), state(13), state(14), state(15))
}

object InvCipher {
  def apply(): InvCipher = Module(new InvCipher(4))
}